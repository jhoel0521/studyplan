package io.github.jhoel0521.studyplan.application.service;

import java.util.List;
import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.sesiones.RegistrarSesionPasada;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.MetaSemanalRepository;
import io.github.jhoel0521.studyplan.application.port.out.PerfilRepository;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.application.port.out.TramoDiarioRepository;
import io.github.jhoel0521.studyplan.domain.readmodel.ConflictoHorario;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.readmodel.MetaAlcanzada;
import io.github.jhoel0521.studyplan.domain.service.RepartidorPorDia;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.readmodel.ResultadoSesion;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;

/**
 * Implementa {@link RegistrarSesionPasada}: valida materia activa, ventana de 1 a 3 días (RN-06),
 * horario (RN-04), duración real (RN-26), cruce horario (RN-05) y detección de metas alcanzadas
 * (RN-16), guardando en estado {@code COMPLETADA_TARDIA} (HU-11, HU-28).
 */
public final class RegistrarSesionPasadaService implements RegistrarSesionPasada {

    private final SesionRepository sesiones;
    private final MateriaRepository materias;
    private final TramoDiarioRepository tramos;
    private final MetaSemanalRepository metas;
    private final PerfilRepository perfiles;
    private final Reloj reloj;

    public RegistrarSesionPasadaService(
            SesionRepository sesiones, MateriaRepository materias, Reloj reloj) {
        this(sesiones, materias, null, null, null, reloj);
    }

    public RegistrarSesionPasadaService(
            SesionRepository sesiones,
            MateriaRepository materias,
            TramoDiarioRepository tramos,
            Reloj reloj) {
        this(sesiones, materias, tramos, null, null, reloj);
    }

    public RegistrarSesionPasadaService(
            SesionRepository sesiones,
            MateriaRepository materias,
            TramoDiarioRepository tramos,
            MetaSemanalRepository metas,
            PerfilRepository perfiles,
            Reloj reloj) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.materias = Objects.requireNonNull(materias, "materias");
        this.tramos = tramos;
        this.metas = metas;
        this.perfiles = perfiles;
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public ResultadoSesion ejecutar(
            long materiaId,
            Fecha fecha,
            Hora horaInicio,
            Hora horaFin,
            int duracionRealMinutos,
            String notas) {
        Resultado<Materia> materia = ValidadorSesiones.obtenerMateriaActiva(materias, materiaId);
        if (!materia.esValido()) {
            return ResultadoSesion.error(materia.error());
        }
        int desfaseMinutos = reloj.ahoraInstante().desfaseMinutos();
        Resultado<SesionEstudio> resultado = SesionEstudio.registrarPasada(
                materiaId,
                fecha,
                reloj.hoy(),
                horaInicio,
                horaFin,
                desfaseMinutos,
                duracionRealMinutos,
                notas);
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
        DetectorFelicitacionMetas.InstantaneaAntes antes =
                DetectorFelicitacionMetas.capturarAntes(tramos, metas, reloj);
        SesionEstudio guardada = sesiones.guardar(candidata);
        if (tramos != null) {
            tramos.guardarTramos(
                    guardada.id(),
                    RepartidorPorDia.repartir(guardada, null, 0L, duracionRealMinutos));
        }
        List<MetaAlcanzada> alcanzadas = DetectorFelicitacionMetas.detectarYMarcar(
                antes, metas, perfiles, materias, tramos);
        return ResultadoSesion.ok(guardada, alcanzadas);
    }
}
