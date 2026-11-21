package io.github.jhoel0521.studyplan.application.port.out.system;

import io.github.jhoel0521.studyplan.domain.enumeration.FormatoExportacionReporte;
import io.github.jhoel0521.studyplan.domain.readmodel.ReporteEstudio;

/**
 * Puerto de salida para generar archivos exportables de un reporte en caché privada y limpiar los
 * archivos temporales al borrar datos del dispositivo ({@code HU-33}, {@code HU-38}).
 */
public interface ExportadorReporte {

    ResultadoExportacion exportar(ReporteEstudio reporte, FormatoExportacionReporte formato);

    void limpiarTemporales();
}
