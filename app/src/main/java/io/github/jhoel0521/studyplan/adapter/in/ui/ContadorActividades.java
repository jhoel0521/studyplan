package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Optional;
import java.util.concurrent.Executor;

import io.github.jhoel0521.studyplan.application.port.in.ControlarSesionBloqueo;
import io.github.jhoel0521.studyplan.application.port.in.RegistrarBloqueoPantalla;
import io.github.jhoel0521.studyplan.application.port.in.RegistrarDesbloqueo;
import io.github.jhoel0521.studyplan.application.port.in.RegistrarSalidaApp;
import io.github.jhoel0521.studyplan.domain.SalidaRegistrada;

/**
 * Cuenta las Activities iniciadas para saber cuándo la app pasa a segundo plano y vuelve a primer plano. Un cambio
 * de configuración (girar la pantalla) detiene y reinicia la Activity, pero no cuenta como salir de la app.
 *
 * <p>Cuando la última Activity se detiene con la pantalla apagada ({@link PowerManager#isInteractive()} es
 * {@code false}) no es una salida: se registra el bloqueo de pantalla; al volver a primer plano se registra el
 * desbloqueo.
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

    private final Handler manejador = new Handler(Looper.getMainLooper());
    private Runnable decisionPendiente;
    private int iniciadas;
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
        cancelarDecisionPendiente();
        salioDeLaApp = false;
        pendienteEvaluarRecuperacion = false;
    }

    @Override
    public void onActivityStarted(@NonNull Activity actividad) {
        cancelarDecisionPendiente();
        if (iniciadas == 0 && salioDeLaApp) {
            sesion.alVolverAPrimerPlano();
            salioDeLaApp = false;
            pendienteEvaluarRecuperacion = true;
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
            programarDecision();
        }
    }

    /** Espera {@link #ESPERA_DECISION_MS} y decide entonces entre bloqueo de pantalla y salida de la app. */
    private void programarDecision() {
        cancelarDecisionPendiente();
        decisionPendiente = this::decidirBloqueoOSalida;
        manejador.postDelayed(decisionPendiente, ESPERA_DECISION_MS);
    }

    private void cancelarDecisionPendiente() {
        if (decisionPendiente != null) {
            manejador.removeCallbacks(decisionPendiente);
            decisionPendiente = null;
        }
    }

    /**
     * Si ninguna Activity volvió a iniciarse: con la pantalla apagada es un bloqueo (HU-50); con la pantalla
     * encendida es una salida de la app (HU-40).
     */
    private void decidirBloqueoOSalida() {
        decisionPendiente = null;
        if (iniciadas > 0) {
            return;
        }
        if (pantallaApagada() && registrarBloqueo != null && ejecutor != null) {
            ejecutor.execute(registrarBloqueo::ejecutar);
        } else if (registrarSalida != null && ejecutor != null && appContext != null) {
            ejecutor.execute(() -> {
                Optional<SalidaRegistrada> salida = registrarSalida.ejecutar();
                if (salida.isPresent()) {
                    NotificadorSesion.mostrarAvisoRetorno(
                            appContext, salida.get().sesionId(), salida.get().nombreMateria());
                    NotificadorSesion.refrescar(appContext, salida.get().sesionId());
                }
            });
        }
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
