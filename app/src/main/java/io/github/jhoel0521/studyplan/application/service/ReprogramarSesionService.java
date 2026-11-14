package io.github.jhoel0521.studyplan.application.service;

import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.ReprogramarSesion;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.domain.ConflictoHorario;
import io.github.jhoel0521.studyplan.domain.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.ResultadoSesion;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;

/**
 * Implementa {@link ReprogramarSesion}: pasa la sesión original a {@code REPROGRAMADA} y crea una
 * nueva sesión {@code PENDIENTE} vinculada mediante {@code sesionOrigenId} en una operación
 * atómica (HU-14, RN-04, RN-05, RN-07, RN-09).
 */
public final class ReprogramarSesionService implements ReprogramarSesion {

    private final SesionRepository sesiones;
    private final MateriaRepository materias;
    private final Reloj reloj;

    public ReprogramarSesionService(
            SesionRepository sesiones, MateriaRepository materias, Reloj reloj) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.materias = Objects.requireNonNull(materias, "materias");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public ResultadoSesion ejecutar(
            long sesionId, Fecha nuevaFecha, Hora nuevaHoraInicio, Hora nuevaHoraFin, String notas) {
        SesionEstudio original = sesiones.buscarPorId(sesionId).orElse(null);
        if (original == null) {
            return ResultadoSesion.error(ErrorValidacion.SESION_NO_ENCONTRADA);
        }
        Resultado<SesionEstudio> originalReprogramada = original.marcarReprogramada();
        if (!originalReprogramada.esValido()) {
            return ResultadoSesion.error(originalReprogramada.error());
        }
        String notasFinales = notas == null ? original.notas() : notas;
        Resultado<SesionEstudio> nuevaPendiente = SesionEstudio.planificar(
                original.materiaId(),
                nuevaFecha,
                reloj.hoy(),
                nuevaHoraInicio,
                nuevaHoraFin,
                notasFinales,
                original.id());
        if (!nuevaPendiente.esValido()) {
            return ResultadoSesion.error(nuevaPendiente.error());
        }
        ConflictoHorario conflicto = ValidadorSesiones.buscarConflictoHorario(
                sesiones, materias, nuevaFecha, nuevaHoraInicio, nuevaHoraFin, original.id());
        if (conflicto != null) {
            return ResultadoSesion.conConflicto(conflicto);
        }
        SesionEstudio creada =
                sesiones.reprogramar(originalReprogramada.valor(), nuevaPendiente.valor());
        return ResultadoSesion.ok(creada);
    }
}
