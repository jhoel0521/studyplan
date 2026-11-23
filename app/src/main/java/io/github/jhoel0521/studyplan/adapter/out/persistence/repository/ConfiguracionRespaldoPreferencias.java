package io.github.jhoel0521.studyplan.adapter.out.persistence.repository;

import android.content.Context;
import android.content.SharedPreferences;

import io.github.jhoel0521.studyplan.application.port.out.persistence.ConfiguracionRespaldoRepository;
import io.github.jhoel0521.studyplan.domain.enumeration.FrecuenciaRespaldo;
import io.github.jhoel0521.studyplan.domain.readmodel.ConfiguracionRespaldo;
import io.github.jhoel0521.studyplan.domain.readmodel.MetadatosRespaldo;
import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;

/**
 * Adaptador de salida: guarda en {@code SharedPreferences} la configuración local del respaldo (cuenta, frecuencia,
 * si hay contraseña, último respaldo y último resultado). Nunca guarda la contraseña. Vive fuera de la base de Room a
 * propósito: así el borrado y la restauración de las tablas no cambian la cuenta vinculada. Sin valores guardados es
 * una instalación nueva: sin cuenta y con frecuencia {@code NUNCA}.
 *
 * <p>Implementa: HU-35, HU-36 y RN-19.
 */
public final class ConfiguracionRespaldoPreferencias implements ConfiguracionRespaldoRepository {

    private static final String PREFS = "studyplan_respaldo_config";
    private static final String CORREO = "correo";
    private static final String FRECUENCIA = "frecuencia";
    private static final String CON_CONTRASENA = "con_contrasena";
    private static final String FECHA = "ultimo_fecha";
    private static final String TAMANO = "ultimo_tamano";
    private static final String CIFRADO = "ultimo_cifrado";
    private static final String RESULTADO = "ultimo_resultado";

    private final SharedPreferences preferencias;

    public ConfiguracionRespaldoPreferencias(Context contexto) {
        this.preferencias = contexto.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public ConfiguracionRespaldo leer() {
        MetadatosRespaldo metadatos = preferencias.contains(FECHA)
                ? new MetadatosRespaldo(
                        preferencias.getLong(FECHA, 0L),
                        preferencias.getLong(TAMANO, 0L),
                        preferencias.getBoolean(CIFRADO, false))
                : null;
        return new ConfiguracionRespaldo(
                preferencias.getString(CORREO, null),
                leerEnum(FrecuenciaRespaldo.class, preferencias.getString(FRECUENCIA, null), FrecuenciaRespaldo.NUNCA),
                preferencias.getBoolean(CON_CONTRASENA, false),
                metadatos,
                leerEnum(ResultadoRespaldo.class, preferencias.getString(RESULTADO, null), null));
    }

    @Override
    public void guardarCorreo(String correo) {
        preferencias.edit().putString(CORREO, correo).apply();
    }

    @Override
    public void guardarFrecuencia(FrecuenciaRespaldo frecuencia) {
        preferencias.edit().putString(FRECUENCIA, frecuencia.name()).apply();
    }

    @Override
    public void guardarConContrasena(boolean conContrasena) {
        preferencias.edit().putBoolean(CON_CONTRASENA, conContrasena).apply();
    }

    @Override
    public void guardarUltimoRespaldo(MetadatosRespaldo metadatos) {
        SharedPreferences.Editor editor = preferencias.edit();
        if (metadatos == null) {
            editor.remove(FECHA).remove(TAMANO).remove(CIFRADO);
        } else {
            editor.putLong(FECHA, metadatos.fechaUtcMilis())
                    .putLong(TAMANO, metadatos.tamanoBytes())
                    .putBoolean(CIFRADO, metadatos.cifrado());
        }
        editor.apply();
    }

    @Override
    public void guardarUltimoResultado(ResultadoRespaldo resultado) {
        preferencias.edit().putString(RESULTADO, resultado.name()).apply();
    }

    @Override
    public void limpiar() {
        preferencias.edit().clear().apply();
    }

    private static <E extends Enum<E>> E leerEnum(Class<E> tipo, String nombre, E porDefecto) {
        if (nombre == null) {
            return porDefecto;
        }
        try {
            return Enum.valueOf(tipo, nombre);
        } catch (IllegalArgumentException e) {
            return porDefecto;
        }
    }
}
