package io.github.jhoel0521.studyplan.application.service;

import java.util.List;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.ProgramadorAvisosSesion;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.domain.CalculadorPomodoro;
import io.github.jhoel0521.studyplan.domain.CalculadorTiempoReal;
import io.github.jhoel0521.studyplan.domain.CandidataInvasion;
import io.github.jhoel0521.studyplan.domain.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.ConfiguracionPomodoro;
import io.github.jhoel0521.studyplan.domain.EstadoPomodoroCalculado;
import io.github.jhoel0521.studyplan.domain.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.TecnicaEstudio;
import io.github.jhoel0521.studyplan.domain.TipoEventoEstudio;

/**
 * Helper compartido en {@code application.service} para localizar sesiones invadidas y programar el
 * próximo hito crítico de una sesión en curso (HU-23, HU-42, HU-43, RN-25, RN-27).
 */
final class EjecucionHitos {

    private EjecucionHitos() {
    }

    static Optional<SesionEstudio> buscarSiguientePendiente(
            SesionEstudio activa, SesionRepository sesiones) {
        List<SesionEstudio> delDia = sesiones.listarPorFecha(activa.fecha());
        for (SesionEstudio candidata : delDia) {
            if (candidata.codigoEstado() == CodigoEstadoSesion.PENDIENTE
                    && !candidata.id().equals(activa.id())
                    && candidata.horaInicio().compareTo(activa.horaInicio()) >= 0) {
                return Optional.of(candidata);
            }
        }
        return Optional.empty();
    }

    static CandidataInvasion detectarInvasion(
            SesionEstudio activa,
            long ahoraMilis,
            SesionRepository sesiones,
            MateriaRepository materias,
            Reloj reloj) {
        List<SesionEstudio> delDia = sesiones.listarPorFecha(activa.fecha());
        for (SesionEstudio candidata : delDia) {
            if (candidata.codigoEstado() != CodigoEstadoSesion.PENDIENTE
                    || candidata.id().equals(activa.id())) {
                continue;
            }
            if (candidata.horaInicio().minutosDesdeMedianoche()
                    < activa.horaInicio().minutosDesdeMedianoche()) {
                continue;
            }
            long inicioCandMilis = reloj.instanteDe(candidata.fecha(), candidata.horaInicio());
            if (ahoraMilis >= inicioCandMilis) {
                Materia m = materias.obtener(candidata.materiaId()).orElse(null);
                String nombre = m == null ? "" : m.nombre();
                int color = m == null ? 0 : m.color();
                return new CandidataInvasion(
                        candidata.id(), nombre, color, candidata.horaInicio(), candidata.horaFin());
            }
        }
        return null;
    }

    static void programarProximoHito(
            SesionEstudio sesion,
            String nombreMateria,
            List<EventoEstudio> eventos,
            long ahoraMilis,
            SesionRepository sesiones,
            ProgramadorAvisosSesion programador,
            Reloj reloj) {
        programador.cancelarAvisos(sesion.id());
        if (CalculadorTiempoReal.estaPausadaActualmente(eventos)
                || CalculadorTiempoReal.tieneSalidaSinResolver(eventos)) {
            return;
        }

        long mejorInstante = Long.MAX_VALUE;
        ProgramadorAvisosSesion.TipoHito mejorTipo = null;

        if (sesion.tienePlan()) {
            long finPlanMilis = sesion.planFinUtcMilis();
            if (finPlanMilis > ahoraMilis && !contieneTipo(eventos, TipoEventoEstudio.FIN_PLANIFICADO)) {
                mejorInstante = finPlanMilis;
                mejorTipo = ProgramadorAvisosSesion.TipoHito.FIN_PLANIFICADO;
            }
        }

        if (sesion.tecnica() == TecnicaEstudio.POMODORO) {
            ConfiguracionPomodoro cfg = sesion.configuracionPomodoroSnapshot();
            if (cfg != null) {
                long dentroApp = CalculadorTiempoReal.calcularMilisDentroDeApp(eventos, ahoraMilis);
                int objetivo = CalculadorPomodoro.pomodorosQueCabenEnPlan(
                        sesion.duracionPlanificadaMinutos(), cfg);
                boolean extra = contieneTipo(eventos, TipoEventoEstudio.TIEMPO_EXTRA);
                EstadoPomodoroCalculado pom = CalculadorPomodoro.calcularEstado(
                        dentroApp, cfg, objetivo, extra);
                if (pom.milisRestantesEnFase() > 0) {
                    long cambioMilis = ahoraMilis + pom.milisRestantesEnFase();
                    if (cambioMilis < mejorInstante) {
                        mejorInstante = cambioMilis;
                        mejorTipo = ProgramadorAvisosSesion.TipoHito.CAMBIO_FASE_POMODORO;
                    }
                }
            }
        }

        List<SesionEstudio> delDia = sesiones.listarPorFecha(sesion.fecha());
        for (SesionEstudio cand : delDia) {
            if (cand.codigoEstado() == CodigoEstadoSesion.PENDIENTE
                    && !cand.id().equals(sesion.id())
                    && cand.horaInicio().minutosDesdeMedianoche()
                            >= sesion.horaInicio().minutosDesdeMedianoche()) {
                long inicioCand = reloj.instanteDe(cand.fecha(), cand.horaInicio());
                if (inicioCand > ahoraMilis && inicioCand < mejorInstante) {
                    mejorInstante = inicioCand;
                    mejorTipo = ProgramadorAvisosSesion.TipoHito.INVASION_SESION;
                }
                break;
            }
        }

        if (mejorTipo != null) {
            programador.programarSiguienteHito(
                    sesion.id(), mejorTipo, mejorInstante, nombreMateria);
        }
    }

    static boolean contieneTipo(List<EventoEstudio> eventos, TipoEventoEstudio tipo) {
        if (eventos == null) {
            return false;
        }
        for (EventoEstudio e : eventos) {
            if (e.tipo() == tipo) {
                return true;
            }
        }
        return false;
    }
}
