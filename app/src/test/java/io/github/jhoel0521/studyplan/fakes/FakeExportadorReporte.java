package io.github.jhoel0521.studyplan.fakes;

import io.github.jhoel0521.studyplan.application.port.out.ExportadorReporte;
import io.github.jhoel0521.studyplan.application.port.out.ResultadoExportacion;
import io.github.jhoel0521.studyplan.domain.FormatoExportacionReporte;
import io.github.jhoel0521.studyplan.domain.ReporteEstudio;

/**
 * Doble de prueba en memoria para {@link ExportadorReporte} ({@code HU-33}, {@code HU-38}).
 */
public final class FakeExportadorReporte implements ExportadorReporte {

    public int exportaciones;
    public int limpiezas;
    public ReporteEstudio ultimoReporte;
    public FormatoExportacionReporte ultimoFormato;
    public ResultadoExportacion.Error errorForzado;
    public String uriForzada = "content://io.github.jhoel0521.studyplan.fileprovider/reportes/test";

    @Override
    public ResultadoExportacion exportar(
            ReporteEstudio reporte, FormatoExportacionReporte formato) {
        exportaciones++;
        ultimoReporte = reporte;
        ultimoFormato = formato;
        if (errorForzado != null) {
            return ResultadoExportacion.error(errorForzado);
        }
        if (reporte == null || reporte.esSinDatos()) {
            return ResultadoExportacion.error(ResultadoExportacion.Error.SIN_DATOS);
        }
        String extension = formato == FormatoExportacionReporte.CSV ? ".csv" : ".pdf";
        return ResultadoExportacion.ok(uriForzada + extension);
    }

    @Override
    public void limpiarTemporales() {
        limpiezas++;
    }
}
