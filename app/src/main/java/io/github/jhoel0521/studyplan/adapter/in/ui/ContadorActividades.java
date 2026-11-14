package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.jhoel0521.studyplan.application.port.in.ControlarSesionBloqueo;

/**
 * Cuenta las Activities iniciadas para saber cuándo la app pasa a segundo plano y vuelve a primer plano. Un cambio
 * de configuración (girar la pantalla) detiene y reinicia la Activity, pero no cuenta como salir de la app.
 *
 * <p>Implementa: HU-04 (pedir autenticación al volver a primer plano).
 */
public class ContadorActividades implements Application.ActivityLifecycleCallbacks {

    private final ControlarSesionBloqueo sesion;
    private int iniciadas;
    private boolean salioDeLaApp;

    public ContadorActividades(ControlarSesionBloqueo sesion) {
        this.sesion = sesion;
    }

    @Override
    public void onActivityStarted(@NonNull Activity actividad) {
        if (iniciadas == 0 && salioDeLaApp) {
            sesion.alVolverAPrimerPlano();
            salioDeLaApp = false;
        }
        iniciadas++;
    }

    @Override
    public void onActivityStopped(@NonNull Activity actividad) {
        iniciadas--;
        if (iniciadas == 0 && !actividad.isChangingConfigurations()) {
            salioDeLaApp = true;
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
