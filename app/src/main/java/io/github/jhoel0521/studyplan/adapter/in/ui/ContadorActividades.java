package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Optional;
import java.util.concurrent.Executor;

import io.github.jhoel0521.studyplan.application.port.in.configuracion.ControlarSesionBloqueo;
import io.github.jhoel0521.studyplan.application.port.in.tecnicas.RegistrarBloqueoPantalla;
import io.github.jhoel0521.studyplan.application.port.in.tecnicas.RegistrarDesbloqueo;
import io.github.jhoel0521.studyplan.application.port.in.tecnicas.RegistrarSalidaApp;
import io.github.jhoel0521.studyplan.domain.readmodel.SalidaRegistrada;

/**
 * Cuenta las Activities iniciadas para saber cuándo la app pasa a segundo plano y vuelve a primer plano. Un cambio
 * de configuración (girar la pantalla) detiene y reinicia la Activity, pero no cuenta como salir de la app.
 *
 * <p>Cuando la última Activity se detiene con la pantalla apagada ({@link PowerManager#isInteractive()} es
 * {@code false}) no es una salida: se registra el bloqueo de pantalla; al volver a primer plano se registra el
 * desbloqueo.
 *
 * <p>Una salida de la app no se registra al instante: tiene una ventana de gracia de 1 minuto. Si el estudiante
 * vuelve antes no pasa nada; si no, se registra con la hora real en que salió ({@link SalidaConGracia}).
 *
 * <p>Implementa: HU-04 (pedir autenticación al volver a primer plano), HU-40 (registrar salida de
 * la app si existe una sesión {@code EN_CURSO}) y HU-50 (bloqueo y desbloqueo de pantalla).
 */
public class ContadorActividades implements Application.ActivityLifecycleCallbacks {

    private final ControlarSesionBloqueo sesion;
    private final RegistrarSalidaApp registrarSalida;
    private final RegistrarBloqueoPantalla registrarBloqueo;
    private final RegistrarDesbloqueo registrarDesbloqueo;
    private final Executor ejecutor;
    private final Context appContext;
    /**
     * Tiempo que se espera tras detenerse la última Activity antes de decidir entre bloqueo y salida: en algunos
     * teléfonos (como los Xiaomi con HyperOS) el sistema apaga la pantalla un instante después de detener la
     * Activity, y decidir en ese momento tomaba el bloqueo por una salida.
     */
    static final long ESPERA_DECISION_MS = 1_200L;

    /** Ventana de gracia: salir y volver antes de este tiempo no cuenta como salida (HU-40, RN-23). */
    static final long GRACIA_SALIDA_MS = 60_000L;

    private final SalidaConGracia salidaConGracia;
    private volatile int iniciadas;
    private boolean salioDeLaApp;
    private volatile boolean pendienteEvaluarRecuperacion = true;

    public ContadorActividades(ControlarSesionBloqueo sesion) {
        this(sesion, null, null, null);
    }

    public ContadorActividades(
            ControlarSesionBloqueo sesion,
            RegistrarSalidaApp registrarSalida,
            Executor ejecutor,
            Context appContext) {
        this(sesion, registrarSalida, null, null, ejecutor, appContext);
    }

    public ContadorActividades(
            ControlarSesionBloqueo sesion,
            RegistrarSalidaApp registrarSalida,
            RegistrarBloqueoPantalla registrarBloqueo,
            RegistrarDesbloqueo registrarDesbloqueo,
            Executor ejecutor,
            Context appContext) {
        this.sesion = sesion;
        this.registrarSalida = registrarSalida;
        this.registrarBloqueo = registrarBloqueo;
        this.registrarDesbloqueo = registrarDesbloqueo;
        this.ejecutor = ejecutor;
        this.appContext = appContext == null ? null : appContext.getApplicationContext();
        this.salidaConGracia = new SalidaConGracia(
                System::currentTimeMillis,
                new ProgramadorRetardadoHandler(),
                this::pantallaApagada,
                new SalidaConGracia.Destino() {
                    @Override
                    public void registrarBloqueo() {
                        alRegistrarBloqueo();
                    }

                    @Override
                    public void confirmarSalida(long instanteSalidaMs, boolean avisar) {
                        alConfirmarSalida(instanteSalidaMs, avisar);
                    }
                },
                ESPERA_DECISION_MS,
                GRACIA_SALIDA_MS);
    }

    public boolean consumirEvaluacionRecuperacion() {
        boolean valor = pendienteEvaluarRecuperacion;
        pendienteEvaluarRecuperacion = false;
        return valor;
    }

    public void solicitarEvaluacionRecuperacion() {
        pendienteEvaluarRecuperacion = true;
    }

    /** Restablece el estado de primer plano entre pruebas instrumentadas. */
    public void reiniciarParaPruebas() {
        salidaConGracia.reiniciar();
        salioDeLaApp = false;
        pendienteEvaluarRecuperacion = false;
    }

    /** Acorta la ventana de gracia para las pruebas instrumentadas; {@link #reiniciarParaPruebas()} la restablece. */
    public void fijarGraciaParaPruebas(long graciaMs) {
        salidaConGracia.fijarGraciaMs(graciaMs);
    }

    @Override
    public void onActivityStarted(@NonNull Activity actividad) {
        if (iniciadas == 0 && salioDeLaApp) {
            sesion.alVolverAPrimerPlano();
            salioDeLaApp = false;
            SalidaConGracia.Retorno retorno = salidaConGracia.actividadIniciada();
            // Volver dentro de la gracia no escribió ningún evento: pedir la recuperación abriría una pregunta falsa.
            if (retorno != SalidaConGracia.Retorno.DENTRO_DE_GRACIA) {
                pendienteEvaluarRecuperacion = true;
            }
            if (registrarDesbloqueo != null && ejecutor != null && appContext != null) {
                ejecutor.execute(() -> {
                    registrarDesbloqueo.ejecutar();
                    // Vuelve a mostrar la notificación de la sesión si el sistema la descartó (HU-49).
                    NotificadorSesion.reconciliar(appContext);
                });
            }
        }
        iniciadas++;
    }

    @Override
    public void onActivityStopped(@NonNull Activity actividad) {
        iniciadas--;
        if (iniciadas == 0 && !actividad.isChangingConfigurations()) {
            salioDeLaApp = true;
            salidaConGracia.ultimaActividadDetenida();
        }
    }

    /** La pantalla se apagó con una pantalla de StudyPlan en primer plano: es un bloqueo (HU-50). */
    private void alRegistrarBloqueo() {
        if (registrarBloqueo != null && ejecutor != null) {
            ejecutor.execute(registrarBloqueo::ejecutar);
        }
    }

    /** Pasó la ventana de gracia fuera de la app: se registra la salida con la hora real (HU-40). */
    private void alConfirmarSalida(long instanteSalidaMs, boolean avisar) {
        if (registrarSalida == null || ejecutor == null || appContext == null) {
            return;
        }
        ejecutor.execute(() -> {
            Optional<SalidaRegistrada> salida = registrarSalida.ejecutar(instanteSalidaMs);
            // Si el estudiante ya volvió mientras se escribía el evento, no se le avisa.
            if (salida.isPresent() && avisar && iniciadas == 0) {
                NotificadorSesion.mostrarAvisoRetorno(
                        appContext, salida.get().sesionId(), salida.get().nombreMateria());
            }
            if (salida.isPresent()) {
                NotificadorSesion.refrescar(appContext, salida.get().sesionId());
            }
        });
    }

    /** Indica si la pantalla está apagada: la app estaba en primer plano y el sistema la bloqueó (HU-50). */
    private boolean pantallaApagada() {
        if (appContext == null) {
            return false;
        }
        PowerManager energia = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
        return energia != null && !energia.isInteractive();
    }

    @Override
    public void onActivityCreated(@NonNull Activity actividad, @Nullable Bundle estado) {
    }

    @Override
    public void onActivityResumed(@NonNull Activity actividad) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity actividad) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity actividad, @NonNull Bundle estado) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity actividad) {
    }
}
