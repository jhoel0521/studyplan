package io.github.jhoel0521.studyplan.domain.service;

import java.util.Map;

/**
 * Lecturas de mapas de acumuladores sin desempaquetar un valor que podría ser nulo (RNF-15). Los mapas de estadísticas
 * (por materia, por tópico) guardan {@code Integer} y {@code Long}; {@link Map#getOrDefault} devuelve el objeto y
 * desempaquetarlo directamente avisa de un posible {@code NullPointerException} aunque nunca se guarden nulos.
 *
 * <p>Usado por {@link CalculadorEstadisticas} (HU-29 a HU-32 y HU-45), {@link CalculadorTiempoReal} (HU-39) y por
 * los servicios que acumulan minutos por materia (HU-07, HU-26 y HU-27).
 */
public final class Mapas {

    private Mapas() {
    }

    /** Valor entero asociado a la clave, o 0 si no hay entrada o el valor es nulo. */
    public static <K> int entero(Map<K, Integer> mapa, K clave) {
        Integer valor = mapa.get(clave);
        return valor == null ? 0 : valor;
    }

    /** Valor largo asociado a la clave, o 0 si no hay entrada o el valor es nulo. */
    public static <K> long largo(Map<K, Long> mapa, K clave) {
        Long valor = mapa.get(clave);
        return valor == null ? 0L : valor;
    }
}
