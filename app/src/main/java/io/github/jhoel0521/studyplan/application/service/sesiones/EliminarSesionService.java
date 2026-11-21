package io.github.jhoel0521.studyplan.application.service.sesiones;

import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.sesiones.EliminarSesion;
import io.github.jhoel0521.studyplan.application.port.out.system.ProgramadorRecordatorios;
import io.github.jhoel0521.studyplan.application.port.out.persistence.SesionRepository;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;

/**
 * Implementa {@link EliminarSesion}: permite borrar únicamente sesiones en estado
 * {@code PENDIENTE} según RN-12 y cancela cualquier recordatorio asociado (HU-15, HU-34).
 */
public final class EliminarSesionService implements EliminarSesion {

    private final SesionRepository sesiones;
    private final ProgramadorRecordatorios programadorRecordatorios;

    public EliminarSesionService(SesionRepository sesiones) {
        this(sesiones, null);
    }

    public EliminarSesionService(
            SesionRepository sesiones, ProgramadorRecordatorios programadorRecordatorios) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.programadorRecordatorios = programadorRecordatorios;
    }

    @Override
    public Resultado<SesionEstudio> ejecutar(long sesionId) {
        SesionEstudio sesion = sesiones.buscarPorId(sesionId).orElse(null);
        if (sesion == null) {
            return Resultado.error(ErrorValidacion.SESION_NO_ENCONTRADA);
        }
        Resultado<SesionEstudio> validacion = sesion.validarEliminacion();
        if (!validacion.esValido()) {
            return validacion;
        }
        sesiones.eliminarPorId(sesionId);
        if (programadorRecordatorios != null) {
            programadorRecordatorios.cancelar(sesionId);
        }
        return Resultado.ok(sesion);
    }
}
