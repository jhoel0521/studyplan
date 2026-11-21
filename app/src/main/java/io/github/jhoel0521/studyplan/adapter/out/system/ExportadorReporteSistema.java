package io.github.jhoel0521.studyplan.adapter.out.system;

import android.app.LocaleManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.LocaleList;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.application.port.out.system.ExportadorReporte;
import io.github.jhoel0521.studyplan.application.port.out.system.ResultadoExportacion;
import io.github.jhoel0521.studyplan.domain.readmodel.DesgloseMateriaPeriodo;
import io.github.jhoel0521.studyplan.domain.enumeration.FormatoExportacionReporte;
import io.github.jhoel0521.studyplan.domain.service.GeneradorCsvReporte;
import io.github.jhoel0521.studyplan.domain.valueobject.PeriodoEstadisticas;
import io.github.jhoel0521.studyplan.domain.readmodel.ReporteEstudio;

/**
 * Adaptador de salida en infraestructura Android que materializa reportes PDF nativos A4
 * ({@link PdfDocument}) y CSV UTF-8 con BOM dentro de la caché privada expuesta vía
 * {@link FileProvider} ({@code HU-33}, {@code HU-38}, {@code RNF-07}, {@code RNF-08}).
 */
public final class ExportadorReporteSistema implements ExportadorReporte {

    private static final String CARPETA_REPORTES = "reportes";
    private static final int MAX_ARCHIVOS_RETENIDOS = 3;

    private static final int ANCHO_A4 = 595;
    private static final int ALTO_A4 = 842;
    private static final float MARGEN_X = 40f;
    private static final float MARGEN_SUPERIOR = 44f;
    private static final float MARGEN_INFERIOR = 42f;
    private static final float ANCHO_CONTENIDO = ANCHO_A4 - (2f * MARGEN_X);
    private static final float ALTURA_PIE = 22f;
    private static final float LIMITE_TABLA_Y = ALTO_A4 - MARGEN_INFERIOR - ALTURA_PIE;
    private static final float ALTURA_CABECERA_TABLA = 26f;
    private static final float ALTURA_FILA_TOTAL = 28f;
    private static final float ALTURA_MINIMA_FILA = 24f;

    private static final float ANCHO_COL_MATERIA = 230f;
    private static final float X_FIN_PLAN = 345f;
    private static final float X_FIN_REAL = 415f;
    private static final float X_FIN_CUMPL = 485f;
    private static final float X_FIN_NO_REAL = ANCHO_A4 - MARGEN_X;

    private static final int COLOR_TINTA_SECUNDARIA = Color.rgb(66, 66, 66);
    private static final int COLOR_FONDO_CABECERA = Color.rgb(236, 239, 241);
    private static final int COLOR_BORDE = Color.rgb(207, 216, 220);
    private static final int COLOR_FILA_CEBRA = Color.rgb(250, 250, 250);
    private static final int COLOR_FILA_TOTAL = Color.rgb(227, 242, 253);

    private final Context context;
    private final File directorioReportes;
    private final Object bloqueoCache = new Object();

    public ExportadorReporteSistema(Context context) {
        Context appContext = Objects.requireNonNull(context, "context").getApplicationContext();
        this.context = appContext;
        this.directorioReportes = new File(appContext.getCacheDir(), CARPETA_REPORTES);
    }

    ExportadorReporteSistema(Context context, File directorioReportes) {
        this.context = Objects.requireNonNull(context, "context").getApplicationContext();
        this.directorioReportes =
                Objects.requireNonNull(directorioReportes, "directorioReportes");
    }

    @Override
    public ResultadoExportacion exportar(
            ReporteEstudio reporte, FormatoExportacionReporte formato) {
        if (reporte == null || reporte.esSinDatos()) {
            return ResultadoExportacion.error(ResultadoExportacion.Error.SIN_DATOS);
        }
        Objects.requireNonNull(formato, "formato");

        Locale idiomaActivo = resolverIdiomaActivo(context);
        Context ctxIdioma = contextoParaIdioma(context, idiomaActivo);

        synchronized (bloqueoCache) {
            if (!asegurarDirectorio()) {
                return ResultadoExportacion.error(ResultadoExportacion.Error.ERROR_ESCRITURA);
            }

            File archivo = crearArchivoUnico(reporte, formato);
            try {
                if (formato == FormatoExportacionReporte.CSV) {
                    escribirCsv(archivo, reporte, ctxIdioma, idiomaActivo);
                } else {
                    escribirPdf(archivo, reporte, ctxIdioma, idiomaActivo);
                }
                retenerUltimosArchivos(archivo);
                String authority = context.getPackageName() + ".fileprovider";
                return ResultadoExportacion.ok(
                        FileProvider.getUriForFile(context, authority, archivo).toString());
            } catch (IOException | IllegalArgumentException | SecurityException e) {
                if (archivo.exists()) {
                    archivo.delete();
                }
                return ResultadoExportacion.error(ResultadoExportacion.Error.ERROR_ESCRITURA);
            }
        }
    }

    @Override
    public void limpiarTemporales() {
        synchronized (bloqueoCache) {
            if (!directorioReportes.exists() || !directorioReportes.isDirectory()) {
                return;
            }
            File[] archivos = directorioReportes.listFiles();
            if (archivos == null) {
                return;
            }
            for (File archivo : archivos) {
                if (archivo.isFile()) {
                    archivo.delete();
                }
            }
        }
    }

    private boolean asegurarDirectorio() {
        if (directorioReportes.isDirectory()) {
            return true;
        }
        if (directorioReportes.exists()) {
            return false;
        }
        return directorioReportes.mkdirs() || directorioReportes.isDirectory();
    }

    private File crearArchivoUnico(ReporteEstudio reporte, FormatoExportacionReporte formato) {
        String ext = formato == FormatoExportacionReporte.CSV ? "csv" : "pdf";
        String inicio = sanitizarNombre(reporte.periodo().inicio().iso());
        String fin = sanitizarNombre(reporte.periodo().fin().iso());
        String base = "studyplan_reporte_" + inicio + "_" + fin + "_" + System.currentTimeMillis();
        File candidato = new File(directorioReportes, base + "." + ext);
        int sufijo = 1;
        while (candidato.exists()) {
            candidato = new File(directorioReportes, base + "_" + sufijo + "." + ext);
            sufijo++;
        }
        return candidato;
    }

    private void retenerUltimosArchivos(File recienCreado) {
        File[] archivos = directorioReportes.listFiles(File::isFile);
        if (archivos == null || archivos.length <= MAX_ARCHIVOS_RETENIDOS) {
            return;
        }
        Arrays.sort(archivos, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        int sobrantes = archivos.length - MAX_ARCHIVOS_RETENIDOS;
        for (File candidato : archivos) {
            if (sobrantes <= 0) {
                break;
            }
            if (!candidato.equals(recienCreado) && candidato.delete()) {
                sobrantes--;
            }
        }
    }

    private void escribirCsv(
            File destino, ReporteEstudio reporte, Context ctxIdioma, Locale idiomaActivo)
            throws IOException {
        byte[] contenido = GeneradorCsvReporte.generar(
                reporte,
                idiomaActivo,
                ctxIdioma.getString(R.string.materia_general_nombre),
                ctxIdioma.getString(R.string.estadisticas_materia_archivada));
        try (FileOutputStream out = new FileOutputStream(destino)) {
            out.write(contenido);
            out.flush();
        }
    }

    private void escribirPdf(
            File destino, ReporteEstudio reporte, Context ctxIdioma, Locale idiomaActivo)
            throws IOException {
        EstilosPdf estilos = new EstilosPdf();
        List<FilaMedida> filas = medirFilas(reporte, ctxIdioma, estilos.celdaTexto);

        float yInicioPag1 = MARGEN_SUPERIOR + 76f + ALTURA_CABECERA_TABLA;
        float yInicioPagSig = MARGEN_SUPERIOR + 26f + ALTURA_CABECERA_TABLA;
        List<List<FilaMedida>> paginas = paginar(filas, yInicioPag1, yInicioPagSig);

        String etiquetaTipo = ctxIdioma.getString(
                reporte.periodo().tipo() == PeriodoEstadisticas.Tipo.MES
                        ? R.string.estadisticas_mes
                        : R.string.estadisticas_semana);
        String rango = reporte.periodo().inicio().iso() + " \u2013 "
                + reporte.periodo().fin().iso();
        String fechaGenerado = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM, DateFormat.SHORT, idiomaActivo)
                .format(new Date(reporte.generadoEnMilis()));

        PdfDocument document = new PdfDocument();
        try {
            int totalPaginas = paginas.size();
            for (int indice = 0; indice < totalPaginas; indice++) {
                PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(
                        ANCHO_A4, ALTO_A4, indice + 1).create();
                PdfDocument.Page pagina = document.startPage(info);
                Canvas canvas = pagina.getCanvas();
                canvas.drawColor(Color.WHITE);

                float y;
                if (indice == 0) {
                    y = dibujarEncabezadoPrincipal(
                            canvas, ctxIdioma, estilos, etiquetaTipo, rango, fechaGenerado);
                } else {
                    y = dibujarEncabezadoContinuacion(canvas, ctxIdioma, estilos, rango);
                }

                y = dibujarCabeceraTabla(canvas, ctxIdioma, estilos, y);

                List<FilaMedida> filasPagina = paginas.get(indice);
                for (int i = 0; i < filasPagina.size(); i++) {
                    y = dibujarFilaMateria(
                            canvas, estilos, idiomaActivo, filasPagina.get(i), y, i % 2 == 1);
                }

                if (indice == totalPaginas - 1) {
                    dibujarFilaTotal(canvas, ctxIdioma, estilos, idiomaActivo, reporte, y);
                }

                dibujarPiePagina(canvas, ctxIdioma, estilos, indice + 1, totalPaginas);
                document.finishPage(pagina);
            }

            try (FileOutputStream out = new FileOutputStream(destino)) {
                document.writeTo(out);
                out.flush();
            }
        } finally {
            document.close();
        }
    }

    private List<FilaMedida> medirFilas(
            ReporteEstudio reporte, Context ctxIdioma, TextPaint paintTexto) {
        String nombreGeneral = ctxIdioma.getString(R.string.materia_general_nombre);
        String etiquetaArchivada = ctxIdioma.getString(R.string.estadisticas_materia_archivada);
        List<FilaMedida> medidas = new ArrayList<>(reporte.materias().size());
        int anchoTexto = Math.round(ANCHO_COL_MATERIA - 10f);

        for (DesgloseMateriaPeriodo item : reporte.materias()) {
            String nombre = item.esGeneral() ? nombreGeneral : item.nombreMateria();
            if (item.archivada()) {
                nombre = nombre + " (" + etiquetaArchivada + ")";
            }
            if (nombre == null) {
                nombre = "";
            }
            StaticLayout layoutNombre = StaticLayout.Builder.obtain(
                            nombre, 0, nombre.length(), paintTexto, anchoTexto)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setMaxLines(2)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build();
            float alto = Math.max(ALTURA_MINIMA_FILA, layoutNombre.getHeight() + 10f);
            medidas.add(new FilaMedida(item, layoutNombre, alto));
        }
        return medidas;
    }

    private List<List<FilaMedida>> paginar(
            List<FilaMedida> filas, float yInicioPag1, float yInicioPagSig) {
        List<List<FilaMedida>> paginas = new ArrayList<>();
        List<FilaMedida> actual = new ArrayList<>();
        float y = yInicioPag1;

        for (FilaMedida fila : filas) {
            if (!actual.isEmpty() && y + fila.alto > LIMITE_TABLA_Y) {
                paginas.add(actual);
                actual = new ArrayList<>();
                y = yInicioPagSig;
            }
            actual.add(fila);
            y += fila.alto;
        }

        if (y + ALTURA_FILA_TOTAL > LIMITE_TABLA_Y) {
            List<FilaMedida> paginaTotal = new ArrayList<>();
            if (actual.size() > 1) {
                // Evita dejar la fila Total huérfana trasladando la última materia con ella
                FilaMedida ultima = actual.remove(actual.size() - 1);
                paginaTotal.add(ultima);
            }
            paginas.add(actual);
            paginas.add(paginaTotal);
        } else {
            paginas.add(actual);
        }
        return paginas;
    }

    private float dibujarEncabezadoPrincipal(
            Canvas canvas,
            Context ctxIdioma,
            EstilosPdf estilos,
            String etiquetaTipo,
            String rango,
            String fechaGenerado) {
        float y = MARGEN_SUPERIOR + 16f;
        canvas.drawText(ctxIdioma.getString(R.string.reportes_pdf_titulo), MARGEN_X, y, estilos.titulo);
        y += 20f;
        canvas.drawText(
                ctxIdioma.getString(R.string.reportes_pdf_periodo, etiquetaTipo, rango),
                MARGEN_X,
                y,
                estilos.subtitulo);
        y += 16f;
        canvas.drawText(
                ctxIdioma.getString(R.string.reportes_pdf_generado, fechaGenerado),
                MARGEN_X,
                y,
                estilos.metadato);
        y += 12f;
        canvas.drawLine(MARGEN_X, y, ANCHO_A4 - MARGEN_X, y, estilos.linea);
        return y + 12f;
    }

    private float dibujarEncabezadoContinuacion(
            Canvas canvas, Context ctxIdioma, EstilosPdf estilos, String rango) {
        float y = MARGEN_SUPERIOR + 12f;
        String texto = ctxIdioma.getString(R.string.reportes_pdf_titulo) + " — " + rango;
        canvas.drawText(texto, MARGEN_X, y, estilos.metadato);
        return y + 14f;
    }

    private float dibujarCabeceraTabla(
            Canvas canvas, Context ctxIdioma, EstilosPdf estilos, float y) {
        canvas.drawRect(
                MARGEN_X, y, ANCHO_A4 - MARGEN_X, y + ALTURA_CABECERA_TABLA, estilos.fondoCabecera);
        float yBaseTexto = y + 17f;
        canvas.drawText(
                ctxIdioma.getString(R.string.reportes_pdf_col_materia),
                MARGEN_X + 6f,
                yBaseTexto,
                estilos.cabeceraIzq);
        canvas.drawText(
                ctxIdioma.getString(R.string.reportes_pdf_col_plan),
                X_FIN_PLAN - 4f,
                yBaseTexto,
                estilos.cabeceraDer);
        canvas.drawText(
                ctxIdioma.getString(R.string.reportes_pdf_col_real),
                X_FIN_REAL - 4f,
                yBaseTexto,
                estilos.cabeceraDer);
        canvas.drawText(
                ctxIdioma.getString(R.string.reportes_pdf_col_cumpl),
                X_FIN_CUMPL - 4f,
                yBaseTexto,
                estilos.cabeceraDer);
        canvas.drawText(
                ctxIdioma.getString(R.string.reportes_pdf_col_no_real),
                X_FIN_NO_REAL - 6f,
                yBaseTexto,
                estilos.cabeceraDer);
        float finY = y + ALTURA_CABECERA_TABLA;
        canvas.drawLine(MARGEN_X, finY, ANCHO_A4 - MARGEN_X, finY, estilos.linea);
        return finY;
    }

    private float dibujarFilaMateria(
            Canvas canvas,
            EstilosPdf estilos,
            Locale idiomaActivo,
            FilaMedida fila,
            float y,
            boolean alternada) {
        if (alternada) {
            canvas.drawRect(
                    MARGEN_X, y, ANCHO_A4 - MARGEN_X, y + fila.alto, estilos.fondoCebra);
        }
        canvas.save();
        canvas.translate(MARGEN_X + 6f, y + 5f);
        fila.layoutNombre.draw(canvas);
        canvas.restore();

        DesgloseMateriaPeriodo item = fila.item;
        float yNum = y + 16f;
        canvas.drawText(
                formatearHoras(idiomaActivo, item.minutosPlanificadosDeSesiones() / 60.0),
                X_FIN_PLAN - 4f,
                yNum,
                estilos.numeroDer);
        canvas.drawText(
                formatearHoras(idiomaActivo, item.horasReales()),
                X_FIN_REAL - 4f,
                yNum,
                estilos.numeroDer);
        canvas.drawText(
                formatearCumplimiento(item.cumplimientoPlanificado()),
                X_FIN_CUMPL - 4f,
                yNum,
                estilos.numeroDer);
        canvas.drawText(
                String.valueOf(item.sesionesNoRealizadas()),
                X_FIN_NO_REAL - 6f,
                yNum,
                estilos.numeroDer);

        float finY = y + fila.alto;
        canvas.drawLine(MARGEN_X, finY, ANCHO_A4 - MARGEN_X, finY, estilos.linea);
        return finY;
    }

    private void dibujarFilaTotal(
            Canvas canvas,
            Context ctxIdioma,
            EstilosPdf estilos,
            Locale idiomaActivo,
            ReporteEstudio reporte,
            float y) {
        float finY = y + ALTURA_FILA_TOTAL;
        canvas.drawRect(MARGEN_X, y, ANCHO_A4 - MARGEN_X, finY, estilos.fondoTotal);
        canvas.drawLine(MARGEN_X, y, ANCHO_A4 - MARGEN_X, y, estilos.lineaFuerte);

        float yBase = y + 18f;
        canvas.drawText(
                ctxIdioma.getString(R.string.reportes_pdf_total),
                MARGEN_X + 6f,
                yBase,
                estilos.totalIzq);
        canvas.drawText(
                formatearHoras(idiomaActivo, reporte.horasPlanificadasTotales()),
                X_FIN_PLAN - 4f,
                yBase,
                estilos.totalDer);
        canvas.drawText(
                formatearHoras(idiomaActivo, reporte.horasRealesTotales()),
                X_FIN_REAL - 4f,
                yBase,
                estilos.totalDer);
        canvas.drawText(
                formatearCumplimiento(reporte.cumplimientoTotal()),
                X_FIN_CUMPL - 4f,
                yBase,
                estilos.totalDer);
        canvas.drawText(
                String.valueOf(reporte.sesionesNoRealizadasTotales()),
                X_FIN_NO_REAL - 6f,
                yBase,
                estilos.totalDer);
        canvas.drawLine(MARGEN_X, finY, ANCHO_A4 - MARGEN_X, finY, estilos.lineaFuerte);
    }

    private void dibujarPiePagina(
            Canvas canvas,
            Context ctxIdioma,
            EstilosPdf estilos,
            int numPagina,
            int totalPaginas) {
        String pie = ctxIdioma.getString(R.string.reportes_pdf_pagina, numPagina, totalPaginas);
        canvas.drawText(pie, ANCHO_A4 - MARGEN_X, ALTO_A4 - 22f, estilos.pieDer);
    }

    private static String formatearHoras(Locale idioma, double horas) {
        return String.format(idioma, "%.1f", horas);
    }

    private static String formatearCumplimiento(Double cumplimiento) {
        if (cumplimiento == null) {
            return "\u2014";
        }
        return Math.round(cumplimiento) + " %";
    }

    private static Locale resolverIdiomaActivo(Context base) {
        LocaleListCompat localesApp = AppCompatDelegate.getApplicationLocales();
        if (!localesApp.isEmpty() && localesApp.get(0) != null) {
            return localesApp.get(0);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LocaleManager manager = base.getSystemService(LocaleManager.class);
            if (manager != null) {
                LocaleList lista = manager.getApplicationLocales();
                if (!lista.isEmpty() && lista.get(0) != null) {
                    return lista.get(0);
                }
            }
        }
        Locale primero = ConfigurationCompat.getLocales(
                base.getResources().getConfiguration()).get(0);
        return primero != null ? primero : Locale.getDefault();
    }

    private static Context contextoParaIdioma(Context base, Locale idioma) {
        if (idioma == null) {
            return base;
        }
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(idioma);
        return base.createConfigurationContext(config);
    }

    private static String sanitizarNombre(String valor) {
        if (valor == null) {
            return "periodo";
        }
        return valor.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private static final class FilaMedida {
        final DesgloseMateriaPeriodo item;
        final StaticLayout layoutNombre;
        final float alto;

        FilaMedida(DesgloseMateriaPeriodo item, StaticLayout layoutNombre, float alto) {
            this.item = item;
            this.layoutNombre = layoutNombre;
            this.alto = alto;
        }
    }

    private static final class EstilosPdf {
        final Paint titulo = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint subtitulo = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint metadato = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint cabeceraIzq = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint cabeceraDer = new Paint(Paint.ANTI_ALIAS_FLAG);
        final TextPaint celdaTexto = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        final Paint numeroDer = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint totalIzq = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint totalDer = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint pieDer = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint linea = new Paint();
        final Paint lineaFuerte = new Paint();
        final Paint fondoCabecera = new Paint();
        final Paint fondoCebra = new Paint();
        final Paint fondoTotal = new Paint();

        EstilosPdf() {
            titulo.setColor(Color.BLACK);
            titulo.setTextSize(16f);
            titulo.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            subtitulo.setColor(Color.BLACK);
            subtitulo.setTextSize(11f);

            metadato.setColor(COLOR_TINTA_SECUNDARIA);
            metadato.setTextSize(9.5f);

            cabeceraIzq.setColor(Color.BLACK);
            cabeceraIzq.setTextSize(10f);
            cabeceraIzq.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            cabeceraDer.setColor(Color.BLACK);
            cabeceraDer.setTextSize(10f);
            cabeceraDer.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            cabeceraDer.setTextAlign(Paint.Align.RIGHT);

            celdaTexto.setColor(Color.BLACK);
            celdaTexto.setTextSize(10f);

            numeroDer.setColor(Color.BLACK);
            numeroDer.setTextSize(10f);
            numeroDer.setTextAlign(Paint.Align.RIGHT);

            totalIzq.setColor(Color.BLACK);
            totalIzq.setTextSize(10.5f);
            totalIzq.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            totalDer.setColor(Color.BLACK);
            totalDer.setTextSize(10.5f);
            totalDer.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            totalDer.setTextAlign(Paint.Align.RIGHT);

            pieDer.setColor(COLOR_TINTA_SECUNDARIA);
            pieDer.setTextSize(9f);
            pieDer.setTextAlign(Paint.Align.RIGHT);

            linea.setColor(COLOR_BORDE);
            linea.setStrokeWidth(0.8f);

            lineaFuerte.setColor(COLOR_TINTA_SECUNDARIA);
            lineaFuerte.setStrokeWidth(1.1f);

            fondoCabecera.setColor(COLOR_FONDO_CABECERA);
            fondoCabecera.setStyle(Paint.Style.FILL);

            fondoCebra.setColor(COLOR_FILA_CEBRA);
            fondoCebra.setStyle(Paint.Style.FILL);

            fondoTotal.setColor(COLOR_FILA_TOTAL);
            fondoTotal.setStyle(Paint.Style.FILL);
        }
    }
}
