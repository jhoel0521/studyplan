package io.github.jhoel0521.studyplan.domain.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.github.jhoel0521.studyplan.domain.entity.MetaSemanal;
import io.github.jhoel0521.studyplan.domain.readmodel.MetaAlcanzada;
import io.github.jhoel0521.studyplan.domain.readmodel.ProgresoMeta;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;

/**
 * Pruebas unitarias de {@link MetaSemanal} y {@link CalculadorProgresoMetas} ({@code HU-25} a
 * {@code HU-28}, {@code RN-11}, {@code RN-14}, {@code RN-16}, {@code RN-33}).
 */
public class CalculadorProgresoMetasTest {

    private static final Fecha LUNES = Fecha.de(2026, 11, 16).valor();

    @Test
    public void metasIndependientesPermitenCumplirMateriaSinCumplirTotalRn14() {
        ProgresoMeta matematicas = CalculadorProgresoMetas.calcularProgreso(
                LUNES, 1L, "Matemáticas", 8 * 60, 6.0, false);
        ProgresoMeta total = CalculadorProgresoMetas.calcularProgreso(
                LUNES, null, "", 8 * 60, 14.0, false);

        assertTrue(matematicas.cumplida());
        assertEquals(100, matematicas.porcentaje());
        assertFalse(total.cumplida());
        assertEquals(57, total.porcentaje());
    }

    @Test
    public void detectaVariasMetasAlcanzadasEnUnTramoYExcluyeYaFelicitadasRn16() {
        MetaSemanal metaMat = MetaSemanal.crear(LUNES, 1L, 4.0).valor().conId(10L);
        MetaSemanal metaInf = MetaSemanal.crear(LUNES, 2L, 2.0).valor().conId(11L).marcarFelicitada();
        MetaSemanal metaTotal = MetaSemanal.crear(LUNES, null, 6.0).valor().conId(12L);

        Map<Long, Integer> antes = new HashMap<>();
        antes.put(1L, 210); // 3.5 h
        antes.put(2L, 120); // 2.0 h ya cumplida y felicitada

        Map<Long, Integer> despues = new HashMap<>();
        despues.put(1L, 270); // 4.5 h -> cruza 4.0 h
        despues.put(2L, 150); // 2.5 h -> ya felicitada, no se repite

        Map<Long, String> nombres = new HashMap<>();
        nombres.put(1L, "Matemáticas");
        nombres.put(2L, "Informática");

        List<MetaAlcanzada> alcanzadas = CalculadorProgresoMetas.detectarMetasAlcanzadas(
                Arrays.asList(metaMat, metaInf, metaTotal),
                antes,
                despues,
                330, // 5.5 h antes
                420, // 7.0 h después -> cruza 6.0 h
                nombres);

        assertEquals(2, alcanzadas.size());
        assertEquals(Long.valueOf(1L), alcanzadas.get(0).materiaId());
        assertEquals("Matemáticas", alcanzadas.get(0).nombreMateria());
        assertTrue(alcanzadas.get(1).esMetaTotal());
    }

    @Test
    public void snapshotPasadoEsInmutableAlCambiarMetaActualRn33() {
        MetaSemanal pasada = MetaSemanal.crear(LUNES.sumarDias(-7), 1L, 6.0).valor();
        ProgresoMeta progresoPasado = CalculadorProgresoMetas.calcularProgreso(
                pasada.semanaInicio(), 1L, "Matemáticas", 360, pasada.horasObjetivo(), false);
        assertTrue(progresoPasado.cumplida());

        MetaSemanal actual = MetaSemanal.crear(LUNES, 1L, 10.0).valor();
        ProgresoMeta progresoActual = CalculadorProgresoMetas.calcularProgreso(
                actual.semanaInicio(), 1L, "Matemáticas", 360, actual.horasObjetivo(), false);
        assertFalse(progresoActual.cumplida());
    }

    @Test
    public void metaSemanalValidaLimitesRn14() {
        assertEquals(ErrorValidacion.META_FUERA_DE_RANGO,
                MetaSemanal.crear(LUNES, null, 0.0).error());
        assertEquals(ErrorValidacion.META_FUERA_DE_RANGO,
                MetaSemanal.crear(LUNES, null, 169.0).error());
        assertEquals(Collections.emptyList(),
                CalculadorProgresoMetas.detectarMetasAlcanzadas(
                        Collections.emptyList(), null, null, 0, 0, null));
    }
}
