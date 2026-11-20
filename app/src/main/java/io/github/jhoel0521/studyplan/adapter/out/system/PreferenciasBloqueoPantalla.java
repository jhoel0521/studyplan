package io.github.jhoel0521.studyplan.adapter.out.system;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Preferencia de interfaz «No volver a preguntar» del candado de la sesión (HU-50). Es una
 * preferencia de la pantalla, no un ajuste de dominio, como {@code MarcadorSalidaAppPrefs}.
 */
public final class PreferenciasBloqueoPantalla {

    public static final String NOMBRE = "preferencias_bloqueo_pantalla";
    private static final String CLAVE_NO_PREGUNTAR = "no_volver_a_preguntar";

    private final SharedPreferences preferencias;

    public PreferenciasBloqueoPantalla(Context contexto) {
        this.preferencias = contexto.getApplicationContext()
                .getSharedPreferences(NOMBRE, Context.MODE_PRIVATE);
    }

    public boolean noVolverAPreguntar() {
        return preferencias.getBoolean(CLAVE_NO_PREGUNTAR, false);
    }

    public void guardarNoVolverAPreguntar(boolean valor) {
        preferencias.edit().putBoolean(CLAVE_NO_PREGUNTAR, valor).apply();
    }
}
