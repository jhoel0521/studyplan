package io.github.jhoel0521.studyplan.domain.readmodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Copia inmutable de todos los datos locales en un instante: cada tabla es una lista de filas y cada fila un mapa de
 * columna a valor ({@code Long}, {@code Double}, {@code String}, {@code Boolean} o {@code null}). Así el respaldo no
 * depende de las entidades de Room y sigue siendo legible cuando se agrega una columna.
 *
 * <p>Implementa: HU-35 y HU-37.
 */
public final class InstantaneaRespaldo {

    /** Versión del formato del archivo de respaldo que esta app escribe y entiende. */
    public static final int VERSION_FORMATO_ACTUAL = 1;

    private final int versionFormato;
    private final int versionEsquema;
    private final long fechaExportacionUtcMilis;
    private final Map<String, List<Map<String, Object>>> tablas;

    public InstantaneaRespaldo(
            int versionFormato,
            int versionEsquema,
            long fechaExportacionUtcMilis,
            Map<String, List<Map<String, Object>>> tablas) {
        this.versionFormato = versionFormato;
        this.versionEsquema = versionEsquema;
        this.fechaExportacionUtcMilis = fechaExportacionUtcMilis;
        this.tablas = copiaInmutable(Objects.requireNonNull(tablas));
    }

    private static Map<String, List<Map<String, Object>>> copiaInmutable(
            Map<String, List<Map<String, Object>>> origen) {
        Map<String, List<Map<String, Object>>> copia = new LinkedHashMap<>();
        for (Map.Entry<String, List<Map<String, Object>>> tabla : origen.entrySet()) {
            List<Map<String, Object>> filas = new ArrayList<>();
            for (Map<String, Object> fila : tabla.getValue()) {
                filas.add(Collections.unmodifiableMap(new LinkedHashMap<>(fila)));
            }
            copia.put(tabla.getKey(), Collections.unmodifiableList(filas));
        }
        return Collections.unmodifiableMap(copia);
    }

    public int versionFormato() {
        return versionFormato;
    }

    /** Versión del esquema de Room con la que se exportó; solo informativa. */
    public int versionEsquema() {
        return versionEsquema;
    }

    public long fechaExportacionUtcMilis() {
        return fechaExportacionUtcMilis;
    }

    /** Tablas en el orden en que se exportaron (el orden respeta las claves foráneas). */
    public Map<String, List<Map<String, Object>>> tablas() {
        return tablas;
    }

    /** Filas de una tabla, o lista vacía si el respaldo no la trae. */
    public List<Map<String, Object>> filas(String tabla) {
        List<Map<String, Object>> filas = tablas.get(tabla);
        return filas == null ? Collections.<Map<String, Object>>emptyList() : filas;
    }

    /** Copia de esta instantánea con otras tablas (para normalizar antes de restaurar). */
    public InstantaneaRespaldo conTablas(Map<String, List<Map<String, Object>>> nuevas) {
        return new InstantaneaRespaldo(versionFormato, versionEsquema, fechaExportacionUtcMilis, nuevas);
    }
}
