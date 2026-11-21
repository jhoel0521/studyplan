package io.github.jhoel0521.studyplan.adapter.out.system;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.out.system.ControladorNoMolestar;

/**
 * Adaptador de sistema para controlar el modo No molestar del dispositivo de forma segura
 * (HU-24, RN-18, RNF-18).
 */
public final class ControladorNoMolestarSistema implements ControladorNoMolestar {

    private static final String PREFS = "preferencias_no_molestar_sistema";
    private static final String CLAVE_FILTRO_PREVIO = "filtro_previo";
    private static final int SIN_FILTRO_GUARDADO = -1;

    private final NotificationManager notificationManager;
    private final SharedPreferences prefs;

    public ControladorNoMolestarSistema(Context contexto) {
        Context appCtx = Objects.requireNonNull(contexto, "contexto").getApplicationContext();
        this.notificationManager =
                (NotificationManager) appCtx.getSystemService(Context.NOTIFICATION_SERVICE);
        this.prefs = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public boolean tienePermiso() {
        if (notificationManager == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return notificationManager.isNotificationPolicyAccessGranted();
        }
        return true;
    }

    @Override
    public void aplicar(boolean activar) {
        if (!tienePermiso()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activar) {
                if (!prefs.contains(CLAVE_FILTRO_PREVIO)) {
                    int previo = notificationManager.getCurrentInterruptionFilter();
                    prefs.edit().putInt(CLAVE_FILTRO_PREVIO, previo).apply();
                }
                notificationManager.setInterruptionFilter(
                        NotificationManager.INTERRUPTION_FILTER_PRIORITY);
            } else {
                restaurarSiAplica();
            }
        }
    }

    @Override
    public void restaurarSiAplica() {
        int previo = prefs.getInt(CLAVE_FILTRO_PREVIO, SIN_FILTRO_GUARDADO);
        if (previo == SIN_FILTRO_GUARDADO) {
            return;
        }
        prefs.edit().remove(CLAVE_FILTRO_PREVIO).apply();
        if (!tienePermiso()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.setInterruptionFilter(previo);
        }
    }
}
