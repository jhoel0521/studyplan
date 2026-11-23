package io.github.jhoel0521.studyplan.application.service.configuracion;

import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.configuracion.BorrarDatos;
import io.github.jhoel0521.studyplan.application.port.out.persistence.AlmacenDatos;
import io.github.jhoel0521.studyplan.application.port.out.persistence.ConfiguracionRespaldoRepository;
import io.github.jhoel0521.studyplan.application.port.out.system.AlmacenCredencialRespaldo;
import io.github.jhoel0521.studyplan.application.port.out.system.ClienteGoogleDrive;
import io.github.jhoel0521.studyplan.application.port.out.system.ControladorNoMolestar;
import io.github.jhoel0521.studyplan.application.port.out.system.ExportadorReporte;
import io.github.jhoel0521.studyplan.application.port.out.system.MarcadorSalidaApp;
import io.github.jhoel0521.studyplan.application.port.out.system.ProgramadorRecordatorios;
import io.github.jhoel0521.studyplan.application.port.out.system.ProgramadorRespaldo;
import io.github.jhoel0521.studyplan.domain.enumeration.AlcanceBorrado;
import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;

/**
 * Implementa {@link BorrarDatos}: vacía el almacén local y deja la sesión sin bloqueo, porque tras el borrado los
 * ajustes vuelven a sus valores por defecto (bloqueo inactivo). Con alcance {@code DRIVE} o {@code AMBOS} borra antes
 * el respaldo de Drive; si Drive falla no toca nada del teléfono. Al borrar el teléfono también se cancela el respaldo
 * automático y se olvidan la cuenta y la contraseña guardada.
 *
 * <p>Implementa: HU-38 y RN-21.
 */
public final class BorrarDatosService implements BorrarDatos {

    private final AlmacenDatos almacen;
    private final SesionBloqueo sesion;
    private final MarcadorSalidaApp marcadorSalida;
    private final ControladorNoMolestar noMolestar;
    private final ExportadorReporte exportadorReporte;
    private final ProgramadorRecordatorios programadorRecordatorios;
    private final ClienteGoogleDrive drive;
    private final ConfiguracionRespaldoRepository configuracionRespaldo;
    private final ProgramadorRespaldo programadorRespaldo;
    private final AlmacenCredencialRespaldo credencialRespaldo;

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
        this(almacen, sesion, marcadorSalida, noMolestar, exportadorReporte, programadorRecordatorios,
                null, null, null, null);
    }

    public BorrarDatosService(
            AlmacenDatos almacen,
            SesionBloqueo sesion,
            MarcadorSalidaApp marcadorSalida,
            ControladorNoMolestar noMolestar,
            ExportadorReporte exportadorReporte,
            ProgramadorRecordatorios programadorRecordatorios,
            ClienteGoogleDrive drive,
            ConfiguracionRespaldoRepository configuracionRespaldo,
            ProgramadorRespaldo programadorRespaldo,
            AlmacenCredencialRespaldo credencialRespaldo) {
        this.almacen = almacen;
        this.sesion = sesion;
        this.marcadorSalida = marcadorSalida;
        this.noMolestar = noMolestar;
        this.exportadorReporte = exportadorReporte;
        this.programadorRecordatorios = programadorRecordatorios;
        this.drive = drive;
        this.configuracionRespaldo = configuracionRespaldo;
        this.programadorRespaldo = programadorRespaldo;
        this.credencialRespaldo = credencialRespaldo;
    }

    @Override
    public ResultadoRespaldo ejecutar(AlcanceBorrado alcance) {
        Objects.requireNonNull(alcance);
        if (alcance == AlcanceBorrado.DRIVE || alcance == AlcanceBorrado.AMBOS) {
            ResultadoRespaldo enDrive = borrarEnDrive();
            if (enDrive != ResultadoRespaldo.OK) {
                return enDrive;
            }
        }
        if (alcance == AlcanceBorrado.TELEFONO || alcance == AlcanceBorrado.AMBOS) {
            borrarTelefono();
        }
        return ResultadoRespaldo.OK;
    }

    private ResultadoRespaldo borrarEnDrive() {
        if (drive == null) {
            return ResultadoRespaldo.SIN_CUENTA;
        }
        ResultadoRespaldo resultado = drive.eliminar();
        if (resultado == ResultadoRespaldo.OK && configuracionRespaldo != null) {
            configuracionRespaldo.guardarUltimoRespaldo(null);
        }
        return resultado;
    }

    private void borrarTelefono() {
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
        if (programadorRespaldo != null) {
            programadorRespaldo.cancelar();
        }
        if (credencialRespaldo != null) {
            credencialRespaldo.borrar();
        }
        if (configuracionRespaldo != null) {
            configuracionRespaldo.limpiar();
        }
    }
}
