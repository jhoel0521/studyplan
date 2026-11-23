package io.github.jhoel0521.studyplan.domain.service;

import static org.junit.Assert.assertEquals;

import io.github.jhoel0521.studyplan.domain.readmodel.InstantaneaRespaldo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Pruebas de {@link NormalizadorRespaldo}: ajustes del dispositivo por defecto y sesiones «En curso» a «Pendiente».
 *
 * <p>Verifica: HU-37.
 */
public class NormalizadorRespaldoTest {

    private static Map<String, Object> fila(Object... paresClaveValor) {
        Map<String, Object> fila = new LinkedHashMap<>();
        for (int i = 0; i < paresClaveValor.length; i += 2) {
            fila.put((String) paresClaveValor[i], paresClaveValor[i + 1]);
        }
        return fila;
    }

    @Test
    public void normalizaAjustesYSesionesSinTocarLoDemasNiLaOriginal() {
        Map<String, List<Map<String, Object>>> tablas = new LinkedHashMap<>();
        tablas.put("ajustes", new ArrayList<>(Collections.singletonList(
                fila("id", 1L, "bloqueoActivo", 1L, "no_molestar_global", 1L, "tema", "OSCURO"))));
        List<Map<String, Object>> sesiones = new ArrayList<>();
        sesiones.add(fila("id", 1L, "estado", "EN_CURSO"));
        sesiones.add(fila("id", 2L, "estado", "COMPLETADA"));
        tablas.put("sesion_estudio", sesiones);
        tablas.put("materia", new ArrayList<>(Collections.singletonList(fila("id", 1L, "nombre", "General"))));
        InstantaneaRespaldo original = new InstantaneaRespaldo(1, 8, 5L, tablas);

        InstantaneaRespaldo normalizada = NormalizadorRespaldo.normalizar(original);

        assertEquals(0L, normalizada.filas("ajustes").get(0).get("bloqueoActivo"));
        assertEquals(0L, normalizada.filas("ajustes").get(0).get("no_molestar_global"));
        assertEquals("OSCURO", normalizada.filas("ajustes").get(0).get("tema"));
        assertEquals("PENDIENTE", normalizada.filas("sesion_estudio").get(0).get("estado"));
        assertEquals("COMPLETADA", normalizada.filas("sesion_estudio").get(1).get("estado"));
        assertEquals(original.filas("materia"), normalizada.filas("materia"));
        assertEquals(1L, original.filas("ajustes").get(0).get("bloqueoActivo"));
        assertEquals("EN_CURSO", original.filas("sesion_estudio").get(0).get("estado"));
        assertEquals(5L, normalizada.fechaExportacionUtcMilis());
    }

    @Test
    public void unRespaldoSinAjustesNiSesionesSePasaSinCambios() {
        InstantaneaRespaldo vacia = new InstantaneaRespaldo(1, 8, 0L, new LinkedHashMap<String, List<Map<String, Object>>>());

        InstantaneaRespaldo normalizada = NormalizadorRespaldo.normalizar(vacia);

        assertEquals(vacia.tablas(), normalizada.tablas());
        assertEquals(1, normalizada.versionFormato());
    }
}
