package io.github.jhoel0521.studyplan.domain.service;

import io.github.jhoel0521.studyplan.domain.readmodel.InstantaneaRespaldo;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Prepara una instantánea para restaurarla en este teléfono: los ajustes que dependen del dispositivo (bloqueo con
 * huella o PIN, No molestar) vuelven a su valor por defecto y ninguna sesión queda «En curso» (vuelve a «Pendiente»),
 * porque su cronómetro era del otro teléfono.
 *
 * <p>Implementa: HU-37 (escenario «Restauración consistente»).
 */
public final class NormalizadorRespaldo {

    private static final String TABLA_AJUSTES = "ajustes";
    private static final String TABLA_SESIONES = "sesion_estudio";

    private NormalizadorRespaldo() {
    }

    /** Devuelve una copia normalizada; no modifica la instantánea recibida. */
    public static InstantaneaRespaldo normalizar(InstantaneaRespaldo original) {
        Map<String, List<Map<String, Object>>> tablas = new LinkedHashMap<>();
        for (Map.Entry<String, List<Map<String, Object>>> tabla : original.tablas().entrySet()) {
            List<Map<String, Object>> filas = new ArrayList<>();
            for (Map<String, Object> fila : tabla.getValue()) {
                filas.add(normalizarFila(tabla.getKey(), fila));
            }
            tablas.put(tabla.getKey(), filas);
        }
        return original.conTablas(tablas);
    }

    private static Map<String, Object> normalizarFila(String tabla, Map<String, Object> fila) {
        Map<String, Object> copia = new LinkedHashMap<>(fila);
        if (TABLA_AJUSTES.equals(tabla)) {
            ponerSiExiste(copia, "bloqueoActivo", 0L);
            ponerSiExiste(copia, "no_molestar_global", 0L);
        } else if (TABLA_SESIONES.equals(tabla)
                && CodigoEstadoSesion.EN_CURSO.name().equals(copia.get("estado"))) {
            copia.put("estado", CodigoEstadoSesion.PENDIENTE.name());
        }
        return copia;
    }

    private static void ponerSiExiste(Map<String, Object> fila, String columna, Object valor) {
        if (fila.containsKey(columna)) {
            fila.put(columna, valor);
        }
    }
}
