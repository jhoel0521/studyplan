package io.github.jhoel0521.studyplan.domain.service;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Pruebas de las lecturas de mapas que no desempaquetan nulos (RNF-15; usadas por HU-29 a HU-32, HU-39 y HU-45).
 */
public class MapasTest {

    @Test
    public void enteroDevuelveElValorOCeroSiFaltaOEsNulo() {
        Map<String, Integer> mapa = new HashMap<>();
        mapa.put("a", 7);
        mapa.put("nulo", null);

        assertEquals(7, Mapas.entero(mapa, "a"));
        assertEquals(0, Mapas.entero(mapa, "falta"));
        assertEquals(0, Mapas.entero(mapa, "nulo"));
    }

    @Test
    public void largoDevuelveElValorOCeroSiFaltaOEsNulo() {
        Map<Long, Long> mapa = new HashMap<>();
        mapa.put(1L, 90_000L);
        mapa.put(2L, null);

        assertEquals(90_000L, Mapas.largo(mapa, 1L));
        assertEquals(0L, Mapas.largo(mapa, 3L));
        assertEquals(0L, Mapas.largo(mapa, 2L));
    }
}
