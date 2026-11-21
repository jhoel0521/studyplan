package io.github.jhoel0521.studyplan.domain.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.enumeration.FormatoExportacionReporte;
import io.github.jhoel0521.studyplan.domain.readmodel.DesgloseMateriaPeriodo;
import io.github.jhoel0521.studyplan.domain.readmodel.ReporteEstudio;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.PeriodoEstadisticas;

/**
 * Pruebas unitarias de {@link GeneradorCsvReporte} y {@link FormatoExportacionReporte}
 * ({@code HU-33}).
 */
public final class GeneradorCsvReporteTest {

    private Locale localePrevio;

    @Before
    public void guardarLocale() {
        localePrevio = Locale.getDefault();
    }

    @After
    public void restaurarLocale() {
        Locale.setDefault(localePrevio);
    }

    @Test
    public void generaUtf8ConBomCrlfDecimalesPuntoYEscapeRfcYCwe1236EnEsBoYUs() {
        PeriodoEstadisticas semana = PeriodoEstadisticas.semanal(Fecha.de(2026, 11, 18).valor());

        DesgloseMateriaPeriodo filaFormula = new DesgloseMateriaPeriodo(
                1L, "=cmd|'/C'!A0", false, 0xFF1565C0, false, 90, 50.0, 60, 0);
        DesgloseMateriaPeriodo filaComasYComillas = new DesgloseMateriaPeriodo(
                2L, "Diseño, \"Español\" y Ñ", false, 0xFFE53935, true, 90, 50.0, 0, 2);
        DesgloseMateriaPeriodo filaEspaciosFormula = new DesgloseMateriaPeriodo(
                3L, "  +SUM(A1:A2)", false, 0xFF43A047, false, 0, 0.0, 30, 1);
        DesgloseMateriaPeriodo filaTab = new DesgloseMateriaPeriodo(
                4L, "\t@IMPORTXML()", false, 0xFF8E24AA, false, 0, 0.0, 0, 1);
        DesgloseMateriaPeriodo filaGeneral = new DesgloseMateriaPeriodo(
                5L, Materia.NOMBRE_GENERAL, true, 0xFF607D8B, false, 30, 0.0, 60, 0);

        ReporteEstudio reporte = new ReporteEstudio(
                semana,
                123456L,
                Arrays.asList(
                        filaFormula,
                        filaComasYComillas,
                        filaEspaciosFormula,
                        filaTab,
                        filaGeneral));

        Locale.setDefault(Locale.forLanguageTag("es-BO"));
        byte[] bytesEs = GeneradorCsvReporte.generar(reporte, Locale.forLanguageTag("es"));

        assertTrue(bytesEs.length > 3);
        assertEquals((byte) 0xEF, bytesEs[0]);
        assertEquals((byte) 0xBB, bytesEs[1]);
        assertEquals((byte) 0xBF, bytesEs[2]);

        String csvEs = new String(bytesEs, 3, bytesEs.length - 3, StandardCharsets.UTF_8);
        assertTrue(csvEs.endsWith("\r\n"));
        assertFalse(csvEs.replace("\r\n", "").contains("\n"));

        String[] lineas = csvEs.split("\r\n");
        assertEquals(7, lineas.length);
        assertEquals(
                "Periodo,Materia,Horas planificadas (h),Horas reales (h),"
                        + "Cumplimiento (%),Sesiones no realizadas",
                lineas[0]);
        assertEquals("2026-11-16 \u2013 2026-11-22,'=cmd|'/C'!A0,1.0,1.5,150,0", lineas[1]);
        assertEquals(
                "2026-11-16 \u2013 2026-11-22,\"Diseño, \"\"Español\"\" y Ñ (Archivada)\","
                        + "0.0,1.5,\u2014,2",
                lineas[2]);
        assertEquals("2026-11-16 \u2013 2026-11-22,'  +SUM(A1:A2),0.5,0.0,0,1", lineas[3]);
        assertEquals("2026-11-16 \u2013 2026-11-22,'\t@IMPORTXML(),0.0,0.0,\u2014,1", lineas[4]);
        assertEquals(
                "2026-11-16 \u2013 2026-11-22,Estudios varios,1.0,0.5,50,0", lineas[5]);
        assertEquals("2026-11-16 \u2013 2026-11-22,Total,2.5,3.5,140,4", lineas[6]);

        Locale.setDefault(Locale.US);
        byte[] bytesEn = GeneradorCsvReporte.generar(reporte, Locale.US);
        String csvEn = new String(bytesEn, 3, bytesEn.length - 3, StandardCharsets.UTF_8);
        String[] lineasEn = csvEn.split("\r\n");
        assertEquals(
                "Period,Subject,Planned hours (h),Real hours (h),Compliance (%),Missed sessions",
                lineasEn[0]);
        assertTrue(lineasEn[2].contains("(Archived)"));
        assertTrue(lineasEn[5].contains(",Miscellaneous study,"));
    }

    @Test
    public void rechazaReporteSinDatos() {
        PeriodoEstadisticas semana = PeriodoEstadisticas.semanal(Fecha.de(2026, 11, 18).valor());
        ReporteEstudio vacio = new ReporteEstudio(semana, 0L, Collections.emptyList());
        assertThrows(
                IllegalArgumentException.class,
                () -> GeneradorCsvReporte.generar(vacio, Locale.ENGLISH));
        assertEquals(2, FormatoExportacionReporte.values().length);
    }
}
