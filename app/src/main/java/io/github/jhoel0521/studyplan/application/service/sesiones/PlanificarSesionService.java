package io.github.jhoel0521.studyplan.application.service.sesiones;

import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.sesiones.PlanificarSesion;
import io.github.jhoel0521.studyplan.application.port.out.persistence.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.system.ProgramadorRecordatorios;
import io.github.jhoel0521.studyplan.application.port.out.system.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.persistence.SesionRepository;
import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.domain.readmodel.ConflictoHorario;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.readmodel.ResultadoSesion;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.application.service.comun.ValidadorSesiones;

/**
 * Implementa {@link PlanificarSesion}: valida materia activa, fecha, horario (RN-04) y cruce
 * horario (RN-05) antes de persistir la sesión en estado {@code PENDIENTE} y programar su
 * recordatorio previo si aplica (HU-10, HU-34).
 */
public final class PlanificarSesionService implements PlanificarSesion {

    private final SesionRepository sesiones;
    private final MateriaRepository materias;
    private final Reloj reloj;
    private final ProgramadorRecordatorios programadorRecordatorios;

    public PlanificarSesionService(
            SesionRepository sesiones, MateriaRepository materias, Reloj reloj) {
        this(sesiones, materias, reloj, null);
    }

    public PlanificarSesionService(
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
            long materiaId, Fecha fecha, Hora horaInicio, Hora horaFin, String notas) {
        return ejecutar(
                materiaId,
                fecha,
                horaInicio,
                horaFin,
                notas,
                AntelacionRecordatorio.SIN_RECORDATORIO);
    }

    @Override
    public ResultadoSesion ejecutar(
            long materiaId,
            Fecha fecha,
            Hora horaInicio,
            Hora horaFin,
            String notas,
            AntelacionRecordatorio antelacion) {
        Resultado<Materia> materia = ValidadorSesiones.obtenerMateriaActiva(materias, materiaId);
        if (!materia.esValido()) {
            return ResultadoSesion.error(materia.error());
        }
        int desfaseMinutos = reloj.ahoraInstante().desfaseMinutos();
        Resultado<SesionEstudio> resultado = SesionEstudio.planificar(
                materiaId,
                fecha,
                reloj.hoy(),
                horaInicio,
                horaFin,
                desfaseMinutos,
                notas,
                null,
                antelacion);
        if (!resultado.esValido()) {
            return ResultadoSesion.error(resultado.error());
        }
        SesionEstudio candidata = resultado.valor();
        ConflictoHorario conflicto = ValidadorSesiones.buscarConflictoHorario(
                sesiones,
                materias,
                candidata.inicioInstante(),
                candidata.finInstante(),
                null);
        if (conflicto != null) {
            return ResultadoSesion.conConflicto(conflicto);
        }
        SesionEstudio guardada = sesiones.guardar(candidata);
        Long disparo = guardada.instanteRecordatorioUtcMilis();
        if (programadorRecordatorios != null
                && guardada.id() != null
                && disparo != null
                && disparo > reloj.ahoraMilis()) {
            programadorRecordatorios.programar(guardada.id(), disparo);
        }
        return ResultadoSesion.ok(guardada);
    }
}
