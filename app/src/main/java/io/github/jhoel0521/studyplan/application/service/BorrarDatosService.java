package io.github.jhoel0521.studyplan.application.service;

import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.BorrarDatos;
import io.github.jhoel0521.studyplan.application.port.out.AlmacenDatos;
import io.github.jhoel0521.studyplan.application.port.out.ControladorNoMolestar;
import io.github.jhoel0521.studyplan.application.port.out.ExportadorReporte;
import io.github.jhoel0521.studyplan.application.port.out.MarcadorSalidaApp;
import io.github.jhoel0521.studyplan.application.port.out.ProgramadorRecordatorios;
import io.github.jhoel0521.studyplan.domain.enumeration.AlcanceBorrado;

/**
 * Implementa {@link BorrarDatos}: vacía el almacén local y deja la sesión sin bloqueo, porque tras el borrado los
 * ajustes vuelven a sus valores por defecto (bloqueo inactivo).
 *
 * <p>Implementa: HU-38 y RN-21 (parcial: solo el alcance «teléfono»).
 */
public final class BorrarDatosService implements BorrarDatos {

    private final AlmacenDatos almacen;
    private final SesionBloqueo sesion;
    private final MarcadorSalidaApp marcadorSalida;
    private final ControladorNoMolestar noMolestar;
    private final ExportadorReporte exportadorReporte;
    private final ProgramadorRecordatorios programadorRecordatorios;

    public BorrarDatosService(AlmacenDatos almacen, SesionBloqueo sesion) {
        this(almacen, sesion, null, null, null, null);
    }

    public BorrarDatosService(
            AlmacenDatos almacen,
            SesionBloqueo sesion,
            MarcadorSalidaApp marcadorSalida,
            ControladorNoMolestar noMolestar) {
        this(almacen, sesion, marcadorSalida, noMolestar, null, null);
    }

    public BorrarDatosService(
            AlmacenDatos almacen,
            SesionBloqueo sesion,
            MarcadorSalidaApp marcadorSalida,
            ControladorNoMolestar noMolestar,
            ExportadorReporte exportadorReporte) {
        this(almacen, sesion, marcadorSalida, noMolestar, exportadorReporte, null);
    }

    public BorrarDatosService(
            AlmacenDatos almacen,
            SesionBloqueo sesion,
            MarcadorSalidaApp marcadorSalida,
            ControladorNoMolestar noMolestar,
            ExportadorReporte exportadorReporte,
            ProgramadorRecordatorios programadorRecordatorios) {
        this.almacen = almacen;
        this.sesion = sesion;
        this.marcadorSalida = marcadorSalida;
        this.noMolestar = noMolestar;
        this.exportadorReporte = exportadorReporte;
        this.programadorRecordatorios = programadorRecordatorios;
    }

    @Override
    public void ejecutar(AlcanceBorrado alcance) {
        Objects.requireNonNull(alcance);
        if (alcance == AlcanceBorrado.TELEFONO) {
            almacen.borrarTodo();
            if (programadorRecordatorios != null) {
                programadorRecordatorios.cancelarTodasNotificaciones();
            }
            sesion.alCambiarBloqueo(false);
            if (marcadorSalida != null) {
                marcadorSalida.limpiar();
            }
            if (noMolestar != null) {
                noMolestar.restaurarSiAplica();
            }
            if (exportadorReporte != null) {
                exportadorReporte.limpiarTemporales();
            }
        }
    }
}
