package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Optional;
import java.util.concurrent.Executor;

import io.github.jhoel0521.studyplan.application.port.in.ControlarSesionBloqueo;
import io.github.jhoel0521.studyplan.application.port.in.RegistrarSalidaApp;

/**
 * Cuenta las Activities iniciadas para saber cuándo la app pasa a segundo plano y vuelve a primer plano. Un cambio
 * de configuración (girar la pantalla) detiene y reinicia la Activity, pero no cuenta como salir de la app.
 *
 * <p>Implementa: HU-04 (pedir autenticación al volver a primer plano) y HU-40 (registrar salida de
 * la app si existe una sesión {@code EN_CURSO}).
 */
public class ContadorActividades implements Application.ActivityLifecycleCallbacks {

    private final ControlarSesionBloqueo sesion;
    private final RegistrarSalidaApp registrarSalida;
    private final Executor ejecutor;
    private final Context appContext;
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
        this.sesion = sesion;
        this.registrarSalida = registrarSalida;
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

    @Override
    public void onActivityStarted(@NonNull Activity actividad) {
        if (iniciadas == 0 && salioDeLaApp) {
            sesion.alVolverAPrimerPlano();
            salioDeLaApp = false;
            pendienteEvaluarRecuperacion = true;
        }
        iniciadas++;
    }

    @Override
    public void onActivityStopped(@NonNull Activity actividad) {
        iniciadas--;
        if (iniciadas == 0 && !actividad.isChangingConfigurations()) {
            salioDeLaApp = true;
            if (registrarSalida != null && ejecutor != null && appContext != null) {
                ejecutor.execute(() -> {
                    Optional<Long> activa = registrarSalida.ejecutar();
                    if (activa.isPresent()) {
                        NotificadorSesion.mostrarAvisoRetorno(appContext, activa.get(), "");
                    }
                });
            }
        }
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
