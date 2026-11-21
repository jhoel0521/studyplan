package io.github.jhoel0521.studyplan.application.service.comun;

import java.util.List;

import io.github.jhoel0521.studyplan.application.port.out.persistence.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.persistence.SesionRepository;
import io.github.jhoel0521.studyplan.domain.readmodel.ConflictoHorario;
import io.github.jhoel0521.studyplan.domain.readmodel.DetalleSesionInicio;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.valueobject.Instante;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;

/**
 * Utilidad interna compartida por los servicios de sesiones para validar materia activa, detectar
 * cruces horarios (RN-05) y construir elementos de lectura sin duplicar lógica.
 */
public final class ValidadorSesiones {

    private ValidadorSesiones() {
    }

    public static Resultado<Materia> obtenerMateriaActiva(MateriaRepository materias, long materiaId) {
        if (materiaId <= 0) {
            return Resultado.error(ErrorValidacion.MATERIA_SESION_OBLIGATORIA);
        }
        Materia materia = materias.obtener(materiaId).orElse(null);
        if (materia == null) {
            return Resultado.error(ErrorValidacion.MATERIA_NO_ENCONTRADA);
        }
        if (materia.archivada()) {
            return Resultado.error(ErrorValidacion.MATERIA_INACTIVA);
        }
        return Resultado.ok(materia);
    }

    public static ConflictoHorario buscarConflictoHorario(
            SesionRepository sesiones,
            MateriaRepository materias,
            Instante inicio,
            Instante fin,
            Long idExcluido) {
        List<SesionEstudio> existentes =
                sesiones.listarPorRangoUtc(inicio.utcMilis(), fin.utcMilis());
        for (SesionEstudio existente : existentes) {
            if (idExcluido != null && idExcluido.equals(existente.id())) {
                continue;
            }
            if (existente.seSolapaCon(inicio, fin)) {
                String nombreMateria = materias.obtener(existente.materiaId())
                        .map(Materia::nombre)
                        .orElse("");
                long sesionId = existente.id() == null ? 0L : existente.id();
                return new ConflictoHorario(
                        sesionId,
                        existente.materiaId(),
                        nombreMateria,
                        existente.fecha(),
                        existente.horaInicio(),
                        existente.horaFin());
            }
        }
        return null;
    }

    public static DetalleSesionInicio aDetalleInicio(SesionEstudio sesion, Materia materia) {
        String nombreMateria = materia == null ? "" : materia.nombre();
        int colorMateria = materia == null ? 0 : materia.color();
        long sesionId = sesion.id() == null ? 0L : sesion.id();
        return new DetalleSesionInicio(
                sesionId,
                sesion.materiaId(),
                nombreMateria,
                colorMateria,
                sesion.fecha(),
                sesion.horaInicio(),
                sesion.horaFin(),
                sesion.duracionPlanificadaMinutos(),
                sesion.duracionRealMinutos(),
                sesion.codigoEstado());
    }
}
