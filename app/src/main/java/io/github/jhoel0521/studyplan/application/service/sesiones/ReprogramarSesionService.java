package io.github.jhoel0521.studyplan.application.service.sesiones;

import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.sesiones.ReprogramarSesion;
import io.github.jhoel0521.studyplan.application.port.out.persistence.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.system.ProgramadorRecordatorios;
import io.github.jhoel0521.studyplan.application.port.out.system.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.persistence.SesionRepository;
import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.domain.readmodel.ConflictoHorario;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.readmodel.ResultadoSesion;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.application.service.comun.ValidadorSesiones;

/**
 * Implementa {@link ReprogramarSesion}: pasa la sesión original a {@code REPROGRAMADA}, cancela su
 * recordatorio previo y crea una nueva sesión {@code PENDIENTE} vinculada mediante
 * {@code sesionOrigenId} en una operación atómica (HU-14, HU-34, RN-04, RN-05, RN-07, RN-09).
 */
public final class ReprogramarSesionService implements ReprogramarSesion {

    private final SesionRepository sesiones;
    private final MateriaRepository materias;
    private final Reloj reloj;
    private final ProgramadorRecordatorios programadorRecordatorios;

    public ReprogramarSesionService(
            SesionRepository sesiones, MateriaRepository materias, Reloj reloj) {
        this(sesiones, materias, reloj, null);
    }

    public ReprogramarSesionService(
            SesionRepository sesiones,
            MateriaRepository materias,
            Reloj reloj,
            ProgramadorRecordatorios programadorRecordatorios) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.materias = Objects.requireNonNull(materias, "materias");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.programadorRecordatorios = programadorRecordatorios;
    }

    @Override
    public ResultadoSesion ejecutar(
            long sesionId, Fecha nuevaFecha, Hora nuevaHoraInicio, Hora nuevaHoraFin, String notas) {
        SesionEstudio original = sesiones.buscarPorId(sesionId).orElse(null);
        if (original == null) {
            return ResultadoSesion.error(ErrorValidacion.SESION_NO_ENCONTRADA);
        }
        return reprogramarCon(
                original,
                nuevaFecha,
                nuevaHoraInicio,
                nuevaHoraFin,
                notas,
                original.antelacionRecordatorio());
    }

    @Override
    public ResultadoSesion ejecutar(
            long sesionId,
            Fecha nuevaFecha,
            Hora nuevaHoraInicio,
            Hora nuevaHoraFin,
            String notas,
            AntelacionRecordatorio nuevaAntelacion) {
        SesionEstudio original = sesiones.buscarPorId(sesionId).orElse(null);
        if (original == null) {
            return ResultadoSesion.error(ErrorValidacion.SESION_NO_ENCONTRADA);
        }
        return reprogramarCon(
                original,
                nuevaFecha,
                nuevaHoraInicio,
                nuevaHoraFin,
                notas,
                nuevaAntelacion == null
                        ? AntelacionRecordatorio.SIN_RECORDATORIO
                        : nuevaAntelacion);
    }

    private ResultadoSesion reprogramarCon(
            SesionEstudio original,
            Fecha nuevaFecha,
            Hora nuevaHoraInicio,
            Hora nuevaHoraFin,
            String notas,
            AntelacionRecordatorio antelacion) {
        Resultado<SesionEstudio> originalReprogramada = original.marcarReprogramada();
        if (!originalReprogramada.esValido()) {
            return ResultadoSesion.error(originalReprogramada.error());
        }
        String notasFinales = notas == null ? original.notas() : notas;
        int desfaseMinutos = reloj.ahoraInstante().desfaseMinutos();
        Resultado<SesionEstudio> nuevaPendiente = SesionEstudio.planificar(
                original.materiaId(),
                nuevaFecha,
                reloj.hoy(),
                nuevaHoraInicio,
                nuevaHoraFin,
                desfaseMinutos,
                notasFinales,
                original.id(),
                antelacion);
        if (!nuevaPendiente.esValido()) {
            return ResultadoSesion.error(nuevaPendiente.error());
        }
        SesionEstudio candidata = nuevaPendiente.valor();
        ConflictoHorario conflicto = ValidadorSesiones.buscarConflictoHorario(
                sesiones,
                materias,
                candidata.inicioInstante(),
                candidata.finInstante(),
                original.id());
        if (conflicto != null) {
            return ResultadoSesion.conConflicto(conflicto);
        }
        SesionEstudio creada =
                sesiones.reprogramar(originalReprogramada.valor(), candidata);
        if (programadorRecordatorios != null) {
            if (original.id() != null) {
                programadorRecordatorios.cancelar(original.id());
            }
            Long disparo = creada.instanteRecordatorioUtcMilis();
            if (creada.id() != null && disparo != null && disparo > reloj.ahoraMilis()) {
                programadorRecordatorios.programar(creada.id(), disparo);
            }
        }
        return ResultadoSesion.ok(creada);
    }
}
