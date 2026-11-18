package io.github.jhoel0521.studyplan.adapter.out.system;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.out.MarcadorSalidaApp;

/**
 * Persiste de forma ligera en {@link SharedPreferences} la salida de una sesión en curso para
 * recuperación tras cierre o reinicio (HU-40, HU-41, RN-23, RN-29).
 */
public final class MarcadorSalidaAppPrefs implements MarcadorSalidaApp {

    private static final String PREFS = "marcador_salida_app_prefs";
    private static final String CLAVE_SESION_ID = "sesion_activa_id";
    private static final String CLAVE_INSTANTE = "instante_salida_milis";
    private static final String CLAVE_SESION_DESCANSO_ID = "sesion_descanso_id";
    private static final String CLAVE_FIN_DESCANSO = "fin_descanso_milis";

    private final SharedPreferences prefs;

    public MarcadorSalidaAppPrefs(Context contexto) {
        this.prefs = Objects.requireNonNull(contexto, "contexto")
                .getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public void marcarSalida(long sesionId, long instanteSalidaMilis) {
        prefs.edit()
                .putLong(CLAVE_SESION_ID, sesionId)
                .putLong(CLAVE_INSTANTE, instanteSalidaMilis)
                .commit();
    }

    @Override
    public Long obtenerSesionActiva() {
        if (!prefs.contains(CLAVE_SESION_ID)) {
            return null;
        }
        return prefs.getLong(CLAVE_SESION_ID, -1L);
    }

    @Override
    public Long obtenerInstanteSalida() {
        if (!prefs.contains(CLAVE_INSTANTE)) {
            return null;
        }
        return prefs.getLong(CLAVE_INSTANTE, -1L);
    }

    @Override
    public void marcarFinDescansoPomodoro(long sesionId, long finDescansoMilis) {
        prefs.edit()
                .putLong(CLAVE_SESION_DESCANSO_ID, sesionId)
                .putLong(CLAVE_FIN_DESCANSO, finDescansoMilis)
                .commit();
    }

    @Override
    public Long obtenerSesionDescansoPomodoro() {
        if (!prefs.contains(CLAVE_SESION_DESCANSO_ID)) {
            return null;
        }
        return prefs.getLong(CLAVE_SESION_DESCANSO_ID, -1L);
    }

    @Override
    public Long obtenerFinDescansoPomodoro() {
        if (!prefs.contains(CLAVE_FIN_DESCANSO)) {
            return null;
        }
        return prefs.getLong(CLAVE_FIN_DESCANSO, -1L);
    }

    @Override
    public void limpiarFinDescansoPomodoro() {
        prefs.edit()
                .remove(CLAVE_SESION_DESCANSO_ID)
                .remove(CLAVE_FIN_DESCANSO)
                .apply();
    }

    @Override
    public void limpiar() {
        prefs.edit()
                .remove(CLAVE_SESION_ID)
                .remove(CLAVE_INSTANTE)
                .remove(CLAVE_SESION_DESCANSO_ID)
                .remove(CLAVE_FIN_DESCANSO)
                .apply();
    }
}
