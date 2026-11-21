package io.github.jhoel0521.studyplan.domain.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.readmodel.DesgloseMateriaPeriodo;
import io.github.jhoel0521.studyplan.domain.readmodel.ReporteEstudio;
import io.github.jhoel0521.studyplan.domain.valueobject.PeriodoEstadisticas;

/**
 * Generador puro de reportes en formato CSV UTF-8 con prefijo BOM, estándar RFC 4180 (delimitador
 * {@code ,}, fin de línea {@code CRLF}, decimales con punto {@code .}) y neutralización ante
 * inyección de fórmulas ({@code CWE-1236}) ({@code HU-33}).
 */
public final class GeneradorCsvReporte {

    private static final String CRLF = "\r\n";
    private static final String GUION_LARGO = "\u2014";
    private static final String RANGO_SEPARADOR = " \u2013 ";
    private static final String CABECERAS_ES =
            "Periodo,Materia,Horas planificadas (h),Horas reales (h),"
                    + "Cumplimiento (%),Sesiones no realizadas";
    private static final String CABECERAS_EN =
            "Period,Subject,Planned hours (h),Real hours (h),"
                    + "Compliance (%),Missed sessions";

    private GeneradorCsvReporte() {
    }

    /**
     * Genera el contenido CSV de {@code reporte} en español o inglés según {@code idioma}.
     *
     * @throws IllegalArgumentException si {@code reporte.esSinDatos()} es verdadero.
     */
    public static byte[] generar(ReporteEstudio reporte, Locale idioma) {
        boolean ingles = esIngles(idioma);
        return generar(
                reporte,
                idioma,
                ingles ? "Miscellaneous study" : Materia.NOMBRE_GENERAL,
                ingles ? "Archived" : "Archivada");
    }

    /**
     * Genera el contenido CSV de {@code reporte} usando etiquetas localizadas explícitas para la
     * materia general y las materias archivadas.
     *
     * @throws IllegalArgumentException si {@code reporte.esSinDatos()} es verdadero.
     */
    public static byte[] generar(
            ReporteEstudio reporte,
            Locale idioma,
            String nombreGeneral,
            String etiquetaArchivada) {
        Objects.requireNonNull(reporte, "reporte");
        if (reporte.esSinDatos()) {
            throw new IllegalArgumentException("No hay datos disponibles para exportar el reporte.");
        }

        boolean ingles = esIngles(idioma);
        String textoGeneral = nombreGeneral != null && !nombreGeneral.trim().isEmpty()
                ? nombreGeneral
                : (ingles ? "Miscellaneous study" : Materia.NOMBRE_GENERAL);
        String textoArchivada = etiquetaArchivada != null && !etiquetaArchivada.trim().isEmpty()
                ? etiquetaArchivada
                : (ingles ? "Archived" : "Archivada");

        String rango = formatearRango(reporte.periodo());
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append(ingles ? CABECERAS_EN : CABECERAS_ES).append(CRLF);

        for (DesgloseMateriaPeriodo fila : reporte.materias()) {
            String nombre = fila.esGeneral() ? textoGeneral : fila.nombreMateria();
            if (fila.archivada()) {
                nombre = nombre + " (" + textoArchivada + ")";
            }
            agregarFila(
                    sb,
                    rango,
                    nombre,
                    fila.minutosPlanificadosDeSesiones() / 60.0,
                    fila.horasReales(),
                    fila.cumplimientoPlanificado(),
                    fila.sesionesNoRealizadas());
        }

        agregarFila(
                sb,
                rango,
                "Total",
                reporte.horasPlanificadasTotales(),
                reporte.horasRealesTotales(),
                reporte.cumplimientoTotal(),
                reporte.sesionesNoRealizadasTotales());

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void agregarFila(
            StringBuilder sb,
            String rango,
            String nombreMateria,
            double horasPlan,
            double horasReal,
            Double cumplimiento,
            int noRealizadas) {
        sb.append(escaparCsv(rango)).append(',');
        sb.append(escaparCsv(sanitizarFormula(nombreMateria))).append(',');
        sb.append(formatearHoras(horasPlan)).append(',');
        sb.append(formatearHoras(horasReal)).append(',');
        sb.append(formatearCumplimiento(cumplimiento)).append(',');
        sb.append(Math.max(0, noRealizadas)).append(CRLF);
    }

    private static String formatearRango(PeriodoEstadisticas periodo) {
        return periodo.inicio().iso() + RANGO_SEPARADOR + periodo.fin().iso();
    }

    private static String formatearHoras(double horas) {
        return String.format(Locale.ROOT, "%.1f", horas);
    }

    private static String formatearCumplimiento(Double cumplimiento) {
        if (cumplimiento == null) {
            return GUION_LARGO;
        }
        return String.valueOf(Math.round(cumplimiento));
    }

    private static String sanitizarFormula(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        char primero = texto.charAt(0);
        if (esPrefijoPeligroso(primero)) {
            return "'" + texto;
        }
        int indice = 0;
        int longitud = texto.length();
        while (indice < longitud && texto.charAt(indice) <= ' ') {
            if (esPrefijoPeligroso(texto.charAt(indice))) {
                return "'" + texto;
            }
            indice++;
        }
        if (indice < longitud && esPrefijoPeligroso(texto.charAt(indice))) {
            return "'" + texto;
        }
        return texto;
    }

    private static boolean esPrefijoPeligroso(char c) {
        return c == '=' || c == '+' || c == '-' || c == '@' || c == '\t' || c == '\r';
    }

    private static String escaparCsv(String celda) {
        String seguro = celda == null ? "" : celda;
        boolean requiereComillas = false;
        for (int i = 0; i < seguro.length(); i++) {
            char c = seguro.charAt(i);
            if (c == ',' || c == '"' || c == '\r' || c == '\n') {
                requiereComillas = true;
                break;
            }
        }
        if (!requiereComillas) {
            return seguro;
        }
        return '"' + seguro.replace("\"", "\"\"") + '"';
    }

    private static boolean esIngles(Locale idioma) {
        return idioma != null && "en".equalsIgnoreCase(idioma.getLanguage());
    }
}
