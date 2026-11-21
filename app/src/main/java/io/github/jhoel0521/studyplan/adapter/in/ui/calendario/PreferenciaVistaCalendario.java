package io.github.jhoel0521.studyplan.adapter.in.ui.calendario;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Objects;

import io.github.jhoel0521.studyplan.domain.enumeration.VistaCalendario;

/**
 * Persiste localmente la última {@link VistaCalendario} elegida por el estudiante en preferencias
 * privadas de interfaz, iniciando en {@link VistaCalendario#SEMANA} la primera vez (HU-16 a HU-18).
 */
public final class PreferenciaVistaCalendario {

    private static final String ARCHIVO_PREFERENCIAS = "preferencias_calendario_ui";
    private static final String CLAVE_ULTIMA_VISTA = "ultima_vista";

    interface Almacen {
        String leer(String clave, String defecto);

        void guardar(String clave, String valor);
    }

    private final Almacen almacen;

    public PreferenciaVistaCalendario(Context contexto) {
        SharedPreferences prefs = Objects.requireNonNull(contexto, "contexto")
                .getApplicationContext()
                .getSharedPreferences(ARCHIVO_PREFERENCIAS, Context.MODE_PRIVATE);
        this.almacen = new Almacen() {
            @Override
            public String leer(String clave, String defecto) {
                return prefs.getString(clave, defecto);
            }

            @Override
            public void guardar(String clave, String valor) {
                prefs.edit().putString(clave, valor).apply();
            }
        };
    }

    PreferenciaVistaCalendario(Almacen almacen) {
        this.almacen = Objects.requireNonNull(almacen, "almacen");
    }

    /** Fábrica en memoria para pruebas unitarias sin {@link Context} Android. */
    public static PreferenciaVistaCalendario enMemoria() {
        return new PreferenciaVistaCalendario(new Almacen() {
            private String valor;

            @Override
            public String leer(String clave, String defecto) {
                return valor == null ? defecto : valor;
            }

            @Override
            public void guardar(String clave, String nuevoValor) {
                this.valor = nuevoValor;
            }
        });
    }

    public VistaCalendario leer() {
        String guardado = almacen.leer(CLAVE_ULTIMA_VISTA, VistaCalendario.POR_DEFECTO.name());
        if (guardado == null) {
            return VistaCalendario.POR_DEFECTO;
        }
        try {
            return VistaCalendario.valueOf(guardado);
        } catch (IllegalArgumentException e) {
            return VistaCalendario.POR_DEFECTO;
        }
    }

    public void guardar(VistaCalendario vista) {
        if (vista != null) {
            almacen.guardar(CLAVE_ULTIMA_VISTA, vista.name());
        }
    }
}
