package io.github.jhoel0521.studyplan.application.service;

import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.PlanificarSesion;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.domain.ConflictoHorario;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.ResultadoSesion;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;

/**
 * Implementa {@link PlanificarSesion}: valida materia activa, fecha, horario (RN-04) y cruce
 * horario (RN-05) antes de persistir la sesión en estado {@code PENDIENTE} (HU-10).
 */
public final class PlanificarSesionService implements PlanificarSesion {

    private final SesionRepository sesiones;
    private final MateriaRepository materias;
    private final Reloj reloj;

    public PlanificarSesionService(
            SesionRepository sesiones, MateriaRepository materias, Reloj reloj) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.materias = Objects.requireNonNull(materias, "materias");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public ResultadoSesion ejecutar(
            long materiaId, Fecha fecha, Hora horaInicio, Hora horaFin, String notas) {
        Resultado<Materia> materia = ValidadorSesiones.obtenerMateriaActiva(materias, materiaId);
        if (!materia.esValido()) {
            return ResultadoSesion.error(materia.error());
        }
        int desfaseMinutos = reloj.ahoraInstante().desfaseMinutos();
        Resultado<SesionEstudio> resultado = SesionEstudio.planificar(
                materiaId, fecha, reloj.hoy(), horaInicio, horaFin, desfaseMinutos, notas, null);
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
        return ResultadoSesion.ok(sesiones.guardar(candidata));
    }
}
