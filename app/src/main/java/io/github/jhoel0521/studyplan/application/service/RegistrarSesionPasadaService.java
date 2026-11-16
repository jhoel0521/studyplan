package io.github.jhoel0521.studyplan.application.service;

import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.RegistrarSesionPasada;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.application.port.out.TramoDiarioRepository;
import io.github.jhoel0521.studyplan.domain.ConflictoHorario;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.RepartidorPorDia;
import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.ResultadoSesion;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;

/**
 * Implementa {@link RegistrarSesionPasada}: valida materia activa, ventana de 1 a 3 días (RN-06),
 * horario (RN-04), duración real (RN-26) y cruce horario (RN-05) guardando en estado
 * {@code COMPLETADA_TARDIA} (HU-11).
 */
public final class RegistrarSesionPasadaService implements RegistrarSesionPasada {

    private final SesionRepository sesiones;
    private final MateriaRepository materias;
    private final TramoDiarioRepository tramos;
    private final Reloj reloj;

    public RegistrarSesionPasadaService(
            SesionRepository sesiones, MateriaRepository materias, Reloj reloj) {
        this(sesiones, materias, null, reloj);
    }

    public RegistrarSesionPasadaService(
            SesionRepository sesiones,
            MateriaRepository materias,
            TramoDiarioRepository tramos,
            Reloj reloj) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.materias = Objects.requireNonNull(materias, "materias");
        this.tramos = tramos;
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
        SesionEstudio guardada = sesiones.guardar(candidata);
        if (tramos != null) {
            tramos.guardarTramos(
                    guardada.id(),
                    RepartidorPorDia.repartir(guardada, null, 0L, duracionRealMinutos));
        }
        return ResultadoSesion.ok(guardada);
    }
}
