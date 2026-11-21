package io.github.jhoel0521.studyplan.application.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.tecnicas.DecidirFinPlanificadoOInvasion;
import io.github.jhoel0521.studyplan.application.port.in.tecnicas.DetenerSesion;
import io.github.jhoel0521.studyplan.application.port.out.AjustesRepository;
import io.github.jhoel0521.studyplan.application.port.out.ControladorNoMolestar;
import io.github.jhoel0521.studyplan.application.port.out.EventoEstudioRepository;
import io.github.jhoel0521.studyplan.application.port.out.MarcadorSalidaApp;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.ProgramadorAvisosSesion;
import io.github.jhoel0521.studyplan.application.port.out.ProgramadorRecordatorios;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.application.port.out.TramoDiarioRepository;
import io.github.jhoel0521.studyplan.domain.entity.Ajustes;
import io.github.jhoel0521.studyplan.domain.service.CalculadorTiempoReal;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.service.RepartidorPorDia;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio;

/**
 * Resuelve las decisiones contextuales durante {@code EN_CURSO}: continuar, extender Pomodoro,
 * pasar a tiempo extra libre, terminar o cambiar a la siguiente sesión (HU-23, HU-42, HU-43,
 * RN-25, RN-27).
 */
public final class DecidirFinPlanificadoOInvasionService implements DecidirFinPlanificadoOInvasion {

    private final SesionRepository sesiones;
    private final EventoEstudioRepository eventos;
    private final MateriaRepository materias;
    private final AjustesRepository ajustesRepo;
    private final ControladorNoMolestar noMolestar;
    private final ProgramadorAvisosSesion programador;
    private final MarcadorSalidaApp marcadorSalida;
    private final DetenerSesion detenerSesion;
    private final TramoDiarioRepository tramos;
    private final Reloj reloj;
    private final ProgramadorRecordatorios programadorRecordatorios;

    public DecidirFinPlanificadoOInvasionService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            MateriaRepository materias,
            AjustesRepository ajustesRepo,
            ControladorNoMolestar noMolestar,
            ProgramadorAvisosSesion programador,
            MarcadorSalidaApp marcadorSalida,
            DetenerSesion detenerSesion,
            Reloj reloj) {
        this(
                sesiones,
                eventos,
                materias,
                ajustesRepo,
                noMolestar,
                programador,
                marcadorSalida,
                detenerSesion,
                null,
                reloj,
                null);
    }

    public DecidirFinPlanificadoOInvasionService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            MateriaRepository materias,
            AjustesRepository ajustesRepo,
            ControladorNoMolestar noMolestar,
            ProgramadorAvisosSesion programador,
            MarcadorSalidaApp marcadorSalida,
            DetenerSesion detenerSesion,
            TramoDiarioRepository tramos,
            Reloj reloj) {
        this(
                sesiones,
                eventos,
                materias,
                ajustesRepo,
                noMolestar,
                programador,
                marcadorSalida,
                detenerSesion,
                tramos,
                reloj,
                null);
    }

    public DecidirFinPlanificadoOInvasionService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            MateriaRepository materias,
            AjustesRepository ajustesRepo,
            ControladorNoMolestar noMolestar,
            ProgramadorAvisosSesion programador,
            MarcadorSalidaApp marcadorSalida,
            DetenerSesion detenerSesion,
            TramoDiarioRepository tramos,
            Reloj reloj,
            ProgramadorRecordatorios programadorRecordatorios) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.materias = Objects.requireNonNull(materias, "materias");
        this.ajustesRepo = Objects.requireNonNull(ajustesRepo, "ajustesRepo");
        this.noMolestar = Objects.requireNonNull(noMolestar, "noMolestar");
        this.programador = Objects.requireNonNull(programador, "programador");
        this.marcadorSalida = Objects.requireNonNull(marcadorSalida, "marcadorSalida");
        this.detenerSesion = Objects.requireNonNull(detenerSesion, "detenerSesion");
        this.tramos = tramos;
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.programadorRecordatorios = programadorRecordatorios;
    }

    @Override
    public Resultado<SesionEstudio> ejecutar(
            long sesionActualId, DecisionFinOInvasion decision, Long siguienteSesionId) {
        Objects.requireNonNull(decision, "decision");
        if (decision == DecisionFinOInvasion.TERMINE) {
            return detenerSesion.ejecutar(sesionActualId);
        }

        Optional<SesionEstudio> actualOpt = sesiones.buscarPorId(sesionActualId);
        if (!actualOpt.isPresent()) {
            return Resultado.error(ErrorValidacion.SESION_NO_ENCONTRADA);
        }
        SesionEstudio actual = actualOpt.get();
        long ahoraMs = reloj.ahoraMilis();
        List<EventoEstudio> lista = new ArrayList<>(eventos.listarPorSesion(sesionActualId));

        if (decision == DecisionFinOInvasion.CAMBIAR_A_SIGUIENTE) {
            return cambiarASiguiente(actual, lista, ahoraMs, siguienteSesionId);
        }

        if (decision == DecisionFinOInvasion.TIEMPO_EXTRA_LIBRE) {
            if (!EjecucionHitos.contieneTipo(lista, TipoEventoEstudio.TIEMPO_EXTRA)) {
                EventoEstudio extra = eventos.guardar(EventoEstudio.crear(
                        sesionActualId,
                        TipoEventoEstudio.TIEMPO_EXTRA,
                        ahoraMs,
                        reloj.fechaDe(ahoraMs),
                        reloj.horaDe(ahoraMs),
                        "").valor());
                lista.add(extra);
            }
        } else {
            if (!EjecucionHitos.contieneTipo(lista, TipoEventoEstudio.FIN_PLANIFICADO)) {
                EventoEstudio finPlan = eventos.guardar(EventoEstudio.crear(
                        sesionActualId,
                        TipoEventoEstudio.FIN_PLANIFICADO,
                        ahoraMs,
                        reloj.fechaDe(ahoraMs),
                        reloj.horaDe(ahoraMs),
                        "").valor());
                lista.add(finPlan);
            }
        }

        String nombreMateria = materias.obtener(actual.materiaId())
                .map(Materia::nombre)
                .orElse("");
        EjecucionHitos.programarProximoHito(
                actual, nombreMateria, lista, ahoraMs, sesiones, programador, reloj);
        return Resultado.ok(actual);
    }

    private Resultado<SesionEstudio> cambiarASiguiente(
            SesionEstudio actual,
            List<EventoEstudio> lista,
            long ahoraMs,
            Long siguienteSesionId) {
        if (siguienteSesionId == null) {
            return Resultado.error(ErrorValidacion.SESION_NO_ENCONTRADA);
        }
        Optional<SesionEstudio> sigOpt = sesiones.buscarPorId(siguienteSesionId);
        if (!sigOpt.isPresent()) {
            return Resultado.error(ErrorValidacion.SESION_NO_ENCONTRADA);
        }

        EventoEstudio cambioEv = EventoEstudio.crear(
                actual.id(),
                TipoEventoEstudio.CAMBIO_SESION,
                ahoraMs,
                reloj.fechaDe(ahoraMs),
                reloj.horaDe(ahoraMs),
                "").valor();
        lista.add(cambioEv);

        int minutos = CalculadorTiempoReal.calcularMinutosReales(
                actual.tecnica(),
                actual.configuracionPomodoroSnapshot(),
                actual.duracionPlanificadaMinutos(),
                lista,
                ahoraMs);
        Resultado<SesionEstudio> actualCerrada = actual.cambiarASiguiente(minutos);
        if (!actualCerrada.esValido()) {
            return actualCerrada;
        }

        Ajustes ajustes = ajustesRepo.obtener();
        Resultado<SesionEstudio> sigIniciadaRes = sigOpt.get().iniciar(
                sigOpt.get().tecnica(), ajustes.pomodoro());
        if (!sigIniciadaRes.esValido()) {
            return sigIniciadaRes;
        }
        SesionEstudio sigIniciada = sigIniciadaRes.valor();

        eventos.guardar(cambioEv);
        sesiones.actualizar(actualCerrada.valor());
        if (tramos != null) {
            tramos.guardarTramos(
                    actual.id(),
                    RepartidorPorDia.repartir(actualCerrada.valor(), lista, ahoraMs, minutos));
        }
        programador.cancelarAvisos(actual.id());
        noMolestar.restaurarSiAplica();
        marcadorSalida.limpiar();

        sesiones.actualizar(sigIniciada);
        if (programadorRecordatorios != null && sigIniciada.id() != null) {
            programadorRecordatorios.cancelar(sigIniciada.id());
        }
        EventoEstudio inicioSig = eventos.guardar(EventoEstudio.crear(
                sigIniciada.id(),
                TipoEventoEstudio.INICIO,
                ahoraMs,
                reloj.fechaDe(ahoraMs),
                reloj.horaDe(ahoraMs),
                "").valor());

        if (sigIniciada.debeActivarNoMolestar(ajustes.noMolestarGlobal())
                && noMolestar.tienePermiso()) {
            noMolestar.aplicar(true);
        }

        String nombreSig = materias.obtener(sigIniciada.materiaId())
                .map(Materia::nombre)
                .orElse("");
        EjecucionHitos.programarProximoHito(
                sigIniciada,
                nombreSig,
                Collections.singletonList(inicioSig),
                ahoraMs,
                sesiones,
                programador,
                reloj);
        return Resultado.ok(sigIniciada);
    }
}
