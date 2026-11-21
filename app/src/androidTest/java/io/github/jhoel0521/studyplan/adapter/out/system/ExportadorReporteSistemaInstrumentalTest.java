package io.github.jhoel0521.studyplan.adapter.out.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.LocaleManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.LocaleList;
import android.os.ParcelFileDescriptor;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import io.github.jhoel0521.studyplan.application.port.out.system.ResultadoExportacion;
import io.github.jhoel0521.studyplan.domain.readmodel.DesgloseMateriaPeriodo;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.enumeration.FormatoExportacionReporte;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.valueobject.PeriodoEstadisticas;
import io.github.jhoel0521.studyplan.domain.readmodel.ReporteEstudio;

/**
 * Pruebas instrumentadas de {@link ExportadorReporteSistema} y {@code FileProvider}: CSV con BOM
 * UTF-8 e idiomas {@code es}/{@code en}, PDF multipágina validado con {@link PdfRenderer}, errores
 * de escritura y limpieza de caché temporal ({@code HU-33}, {@code HU-38}, {@code RNF-08}).
 */
@RunWith(AndroidJUnit4.class)
public final class ExportadorReporteSistemaInstrumentalTest {

    private Context context;
    private ExportadorReporteSistema exportador;

    @Before
    public void preparar() {
        context = ApplicationProvider.getApplicationContext();
        exportador = new ExportadorReporteSistema(context);
        exportador.limpiarTemporales();
        aplicarIdioma("es");
    }

    @After
    public void limpiar() {
        exportador.limpiarTemporales();
        aplicarIdioma("es");
    }

    @Test
    public void generaCsvEnEspanolEInglesYResuelveContentUriViaFileProvider() throws Exception {
        PeriodoEstadisticas semana = PeriodoEstadisticas.semanal(Fecha.de(2026, 11, 18).valor());
        DesgloseMateriaPeriodo general = new DesgloseMateriaPeriodo(
                1L, Materia.NOMBRE_GENERAL, true, 0xFF607D8B, false, 60, 50.0, 120, 0);
        DesgloseMateriaPeriodo archivada = new DesgloseMateriaPeriodo(
                2L, "Química Orgánica", false, 0xFF1565C0, true, 60, 50.0, 0, 1);
        ReporteEstudio reporte = new ReporteEstudio(
                semana, 1_795_000_000_000L, java.util.Arrays.asList(general, archivada));

        ResultadoExportacion resEs = exportador.exportar(reporte, FormatoExportacionReporte.CSV);
        assertTrue(resEs.esExitoso());
        Uri uriEs = Uri.parse(resEs.uriContenido());
        assertEquals("content", uriEs.getScheme());
        assertEquals(context.getPackageName() + ".fileprovider", uriEs.getAuthority());

        byte[] bytesEs = leerBytes(uriEs);
        assertEquals((byte) 0xEF, bytesEs[0]);
        assertEquals((byte) 0xBB, bytesEs[1]);
        assertEquals((byte) 0xBF, bytesEs[2]);
        String textoEs = new String(bytesEs, 3, bytesEs.length - 3, StandardCharsets.UTF_8);
        assertTrue(textoEs.startsWith("Periodo,Materia,"));
        assertTrue(textoEs.contains(",Estudios varios,"));
        assertTrue(textoEs.contains("Química Orgánica (Archivada)"));
        assertTrue(textoEs.contains("\r\n"));

        aplicarIdioma("en");
        ResultadoExportacion resEn = exportador.exportar(reporte, FormatoExportacionReporte.CSV);
        assertTrue(resEn.esExitoso());
        byte[] bytesEn = leerBytes(Uri.parse(resEn.uriContenido()));
        String textoEn = new String(bytesEn, 3, bytesEn.length - 3, StandardCharsets.UTF_8);
        assertTrue(textoEn.startsWith("Period,Subject,"));
        assertTrue(textoEn.contains(",Miscellaneous study,"));
        assertTrue(textoEn.contains("Química Orgánica (Archived)"));
    }

    @Test
    public void generaPdfMultipaginaConCuarentaMateriasValidoParaPdfRendererYLimpieza()
            throws Exception {
        PeriodoEstadisticas mes = PeriodoEstadisticas.mensual(Fecha.de(2026, 11, 18).valor());
        List<DesgloseMateriaPeriodo> filas = new ArrayList<>();
        for (int i = 1; i <= 42; i++) {
            String nombreLargo = "Materia extensa número " + i
                    + " con título académico muy largo para ajuste";
            filas.add(new DesgloseMateriaPeriodo(
                    i, nombreLargo, false, 0xFF1565C0, false, 90, 2.0, 60, i % 3));
        }
        ReporteEstudio reporte = new ReporteEstudio(mes, 1_795_000_000_000L, filas);

        ResultadoExportacion resultado = exportador.exportar(
                reporte, FormatoExportacionReporte.PDF);
        assertTrue(resultado.esExitoso());

        Uri uriPdf = Uri.parse(resultado.uriContenido());
        byte[] pdfBytes = leerBytes(uriPdf);
        assertTrue(pdfBytes.length > 100);
        assertEquals("%PDF-", new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII));

        try (ParcelFileDescriptor pfd = context.getContentResolver()
                .openFileDescriptor(uriPdf, "r")) {
            assertNotNull(pfd);
            try (PdfRenderer renderer = new PdfRenderer(pfd)) {
                assertTrue(
                        "Esperaba al menos 2 páginas con 42 materias, obtuvo "
                                + renderer.getPageCount(),
                        renderer.getPageCount() >= 2);
                try (PdfRenderer.Page pagina1 = renderer.openPage(0)) {
                    assertEquals(595, pagina1.getWidth());
                    assertEquals(842, pagina1.getHeight());
                }
            }
        }

        // Retención máxima de 3 archivos tras múltiples exportaciones
        exportador.exportar(reporte, FormatoExportacionReporte.CSV);
        exportador.exportar(reporte, FormatoExportacionReporte.PDF);
        exportador.exportar(reporte, FormatoExportacionReporte.CSV);
        File carpeta = new File(context.getCacheDir(), "reportes");
        File[] restantes = carpeta.listFiles(File::isFile);
        assertNotNull(restantes);
        assertTrue(restantes.length <= 3);

        exportador.limpiarTemporales();
        File[] trasLimpiar = carpeta.listFiles(File::isFile);
        assertEquals(0, trasLimpiar == null ? 0 : trasLimpiar.length);
    }

    @Test
    public void retornaErrorSiNoHayDatosOOcurreErrorDeEscritura() throws Exception {
        PeriodoEstadisticas semana = PeriodoEstadisticas.semanal(Fecha.de(2026, 11, 18).valor());
        ReporteEstudio vacio = new ReporteEstudio(semana, 0L, Collections.emptyList());
        ResultadoExportacion sinDatos = exportador.exportar(vacio, FormatoExportacionReporte.PDF);
        assertFalse(sinDatos.esExitoso());
        assertEquals(ResultadoExportacion.Error.SIN_DATOS, sinDatos.error());

        File archivoQueBloqueaCarpeta = new File(context.getCacheDir(), "reportes_bloqueo_test");
        assertTrue(archivoQueBloqueaCarpeta.exists() || archivoQueBloqueaCarpeta.createNewFile());
        try {
            ExportadorReporteSistema exportadorBloqueado =
                    new ExportadorReporteSistema(context, archivoQueBloqueaCarpeta);
            ReporteEstudio conDato = new ReporteEstudio(
                    semana,
                    10L,
                    Collections.singletonList(new DesgloseMateriaPeriodo(
                            1L, "Física", false, 0xFF1565C0, false, 60, 100.0, 60, 0)));
            ResultadoExportacion falloIo = exportadorBloqueado.exportar(
                    conDato, FormatoExportacionReporte.PDF);
            assertFalse(falloIo.esExitoso());
            assertEquals(ResultadoExportacion.Error.ERROR_ESCRITURA, falloIo.error());
        } finally {
            archivoQueBloqueaCarpeta.delete();
        }
    }

    private byte[] leerBytes(Uri uri) throws Exception {
        try (InputStream in = context.getContentResolver().openInputStream(uri);
                ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            assertNotNull(in);
            byte[] chunk = new byte[4096];
            int leidos;
            while ((leidos = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, leidos);
            }
            return buffer.toByteArray();
        }
    }

    private void aplicarIdioma(String tag) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                LocaleManager manager = context.getSystemService(LocaleManager.class);
                if (manager != null) {
                    manager.setApplicationLocales(LocaleList.forLanguageTags(tag));
                }
            }
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag));
        });
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Configuration config = new Configuration(context.getResources().getConfiguration());
            config.setLocale(Locale.forLanguageTag(tag));
            exportador = new ExportadorReporteSistema(context.createConfigurationContext(config));
        }
    }
}
