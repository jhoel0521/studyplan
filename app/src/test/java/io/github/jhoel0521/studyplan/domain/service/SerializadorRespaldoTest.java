package io.github.jhoel0521.studyplan.domain.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.github.jhoel0521.studyplan.domain.readmodel.InstantaneaRespaldo;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Pruebas unitarias de {@link SerializadorRespaldo}: ida y vuelta de las siete tablas del respaldo (perfil, ajustes,
 * materia, sesion_estudio, evento_estudio, tramo_diario, meta_semanal) y rechazo de archivos inválidos.
 *
 * <p>Implementa: HU-35 y HU-37.
 */
public class SerializadorRespaldoTest {

    private static Map<String, Object> fila(Object... paresClaveValor) {
        Map<String, Object> fila = new LinkedHashMap<>();
        for (int i = 0; i < paresClaveValor.length; i += 2) {
            fila.put((String) paresClaveValor[i], paresClaveValor[i + 1]);
        }
        return fila;
    }

    private static InstantaneaRespaldo ejemplo() {
        Map<String, List<Map<String, Object>>> tablas = new LinkedHashMap<>();
        tablas.put("perfil", lista(fila("id", 1L, "nombre", "Ana \"la\" López\n", "metaSemanalHoras", 12.5)));
        tablas.put("ajustes", lista(fila("id", 1L, "tema", "OSCURO", "bloqueoActivo", 1L, "recordatorio_minutos", 15L)));
        tablas.put("materia", lista(
                fila("id", 1L, "nombre", "General", "color", -10453621L),
                fila("id", 2L, "nombre", "Cálculo ∑ 日本", "color", 4278190080L)));
        tablas.put("sesion_estudio", lista(fila(
                "id", 7L, "materia_id", 2L, "plan_fin_utc_milis", null, "notas", "tab\there\\", "estado", "PENDIENTE")));
        tablas.put("evento_estudio", lista(fila("id", 1L, "sesion_id", 7L, "instante_utc_milis", 1_700_000_000_000L)));
        tablas.put("tramo_diario", lista(fila("id", 1L, "fecha", "2026-11-23", "minutos", 25L)));
        tablas.put("meta_semanal", lista(fila("id", 1L, "semana_inicio", "2026-11-23", "meta_horas", 6.0)));
        return new InstantaneaRespaldo(1, 8, 1_700_000_000_123L, tablas);
    }

    @SafeVarargs
    private static List<Map<String, Object>> lista(Map<String, Object>... filas) {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Map<String, Object> f : filas) {
            lista.add(f);
        }
        return lista;
    }

    @Test
    public void idaYVueltaConservaLasSieteTablasYLosTiposDeValor() {
        InstantaneaRespaldo original = ejemplo();

        InstantaneaRespaldo leida = SerializadorRespaldo.deserializar(SerializadorRespaldo.serializar(original));

        assertEquals(1, leida.versionFormato());
        assertEquals(8, leida.versionEsquema());
        assertEquals(1_700_000_000_123L, leida.fechaExportacionUtcMilis());
        assertEquals(original.tablas().keySet(), leida.tablas().keySet());
        assertEquals(original.tablas(), leida.tablas());
        assertEquals("Ana \"la\" López\n", leida.filas("perfil").get(0).get("nombre"));
        assertEquals(12.5, (Double) leida.filas("perfil").get(0).get("metaSemanalHoras"), 0.0);
        assertNull(leida.filas("sesion_estudio").get(0).get("plan_fin_utc_milis"));
        assertEquals("Cálculo ∑ 日本", leida.filas("materia").get(1).get("nombre"));
    }

    @Test
    public void unaTablaVaciaSobreviveALaIdaYVuelta() {
        Map<String, List<Map<String, Object>>> tablas = new LinkedHashMap<>();
        tablas.put("evento_estudio", new ArrayList<Map<String, Object>>());
        InstantaneaRespaldo leida = SerializadorRespaldo.deserializar(
                SerializadorRespaldo.serializar(new InstantaneaRespaldo(1, 8, 0L, tablas)));

        assertTrue(leida.tablas().containsKey("evento_estudio"));
        assertTrue(leida.filas("evento_estudio").isEmpty());
        assertTrue(leida.filas("tabla_inexistente").isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaUnArchivoQueNoEsJson() {
        SerializadorRespaldo.deserializar("esto no es json".getBytes(StandardCharsets.UTF_8));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaUnJsonQueNoEsUnRespaldoDeStudyPlan() {
        SerializadorRespaldo.deserializar("{\"formato\":\"OTRA\",\"tablas\":{}}".getBytes(StandardCharsets.UTF_8));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaUnArchivoTruncado() {
        byte[] completo = SerializadorRespaldo.serializar(ejemplo());
        SerializadorRespaldo.deserializar(java.util.Arrays.copyOf(completo, completo.length / 2));
    }
}
