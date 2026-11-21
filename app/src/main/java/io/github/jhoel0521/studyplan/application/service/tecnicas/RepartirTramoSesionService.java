package io.github.jhoel0521.studyplan.application.service.tecnicas;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.tecnicas.RepartirTramoSesion;
import io.github.jhoel0521.studyplan.application.port.out.ControladorNoMolestar;
import io.github.jhoel0521.studyplan.application.port.out.EventoEstudioRepository;
import io.github.jhoel0521.studyplan.application.port.out.MarcadorSalidaApp;
import io.github.jhoel0521.studyplan.application.port.out.ProgramadorAvisosSesion;
import io.github.jhoel0521.studyplan.application.port.out.ProgramadorRecordatorios;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.application.port.out.TramoDiarioRepository;
import io.github.jhoel0521.studyplan.domain.service.CalculadorRepartoTramo;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.service.RepartidorPorDia;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.readmodel.ResultadoReparto;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio;

/**
 * Reparte un tramo continuo entre la sesión en curso y la siguiente sesión invadida completando
 * ambas en una única transacción atómica (HU-44, RN-28).
 */
public final class RepartirTramoSesionService implements RepartirTramoSesion {

    private final SesionRepository sesiones;
    private final EventoEstudioRepository eventos;
    private final ControladorNoMolestar noMolestar;
    private final ProgramadorAvisosSesion programador;
    private final MarcadorSalidaApp marcadorSalida;
    private final TramoDiarioRepository tramos;
    private final Reloj reloj;
    private final ProgramadorRecordatorios programadorRecordatorios;

    public RepartirTramoSesionService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            ControladorNoMolestar noMolestar,
            ProgramadorAvisosSesion programador,
            MarcadorSalidaApp marcadorSalida,
            Reloj reloj) {
        this(sesiones, eventos, noMolestar, programador, marcadorSalida, null, reloj, null);
    }

    public RepartirTramoSesionService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            ControladorNoMolestar noMolestar,
            ProgramadorAvisosSesion programador,
            MarcadorSalidaApp marcadorSalida,
            TramoDiarioRepository tramos,
            Reloj reloj) {
        this(sesiones, eventos, noMolestar, programador, marcadorSalida, tramos, reloj, null);
    }

    public RepartirTramoSesionService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            ControladorNoMolestar noMolestar,
            ProgramadorAvisosSesion programador,
            MarcadorSalidaApp marcadorSalida,
            TramoDiarioRepository tramos,
            Reloj reloj,
            ProgramadorRecordatorios programadorRecordatorios) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.noMolestar = Objects.requireNonNull(noMolestar, "noMolestar");
        this.programador = Objects.requireNonNull(programador, "programador");
        this.marcadorSalida = Objects.requireNonNull(marcadorSalida, "marcadorSalida");
        this.tramos = tramos;
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.programadorRecordatorios = programadorRecordatorios;
    }

    @Override
    public Resultado<ResultadoReparto> ejecutar(
            long sesionActualId, long sesionSiguienteId, long corteMilis) {
        Optional<SesionEstudio> actualOpt = sesiones.buscarPorId(sesionActualId);
        Optional<SesionEstudio> sigOpt = sesiones.buscarPorId(sesionSiguienteId);
        if (!actualOpt.isPresent() || !sigOpt.isPresent()) {
            return Resultado.error(ErrorValidacion.SESION_NO_ENCONTRADA);
        }

        SesionEstudio actual = actualOpt.get();
        SesionEstudio siguiente = sigOpt.get();
        long ahoraMs = reloj.ahoraMilis();
        List<EventoEstudio> lista = new ArrayList<>(eventos.listarPorSesion(sesionActualId));

        Resultado<ResultadoReparto> repartoRes = CalculadorRepartoTramo.repartir(
                actual.tecnica(),
                actual.configuracionPomodoroSnapshot(),
                actual.duracionPlanificadaMinutos(),
                lista,
                ahoraMs,
                corteMilis);
        if (!repartoRes.esValido()) {
            return repartoRes;
        }
        ResultadoReparto reparto = repartoRes.valor();

        Resultado<SesionEstudio> primeraRes = actual.detener(reparto.minutosPrimeraSesion());
        if (!primeraRes.esValido()) {
            return Resultado.error(primeraRes.error());
        }
        Resultado<SesionEstudio> sigEnCurso = siguiente.iniciar(siguiente.tecnica(), null);
        if (!sigEnCurso.esValido()) {
            return Resultado.error(sigEnCurso.error());
        }
        Resultado<SesionEstudio> segundaRes = sigEnCurso.valor().detener(reparto.minutosSegundaSesion());
        if (!segundaRes.esValido()) {
            return Resultado.error(segundaRes.error());
        }

        EventoEstudio finEv = eventos.guardar(EventoEstudio.crear(
                sesionActualId,
                TipoEventoEstudio.FIN,
                ahoraMs,
                reloj.ahoraInstante().desfaseMinutos(),
                "").valor());
        sesiones.completarReparto(primeraRes.valor(), segundaRes.valor());
        if (tramos != null) {
            List<EventoEstudio> hastaCorte = new ArrayList<>();
            for (EventoEstudio ev : lista) {
                if (ev.instanteMilis() <= corteMilis) {
                    hastaCorte.add(ev);
                }
            }
            java.util.Map<Fecha, Integer> tramosPrimera = RepartidorPorDia.repartir(
                    primeraRes.valor(), hastaCorte, corteMilis, reparto.minutosPrimeraSesion());
            tramos.guardarTramos(sesionActualId, tramosPrimera);

            int desfase = reloj.ahoraInstante().desfaseMinutos();
            EventoEstudio inicioCorte = EventoEstudio.crear(
                    sesionSiguienteId, TipoEventoEstudio.INICIO, corteMilis, desfase, "").valor();
            java.util.Map<Fecha, Integer> tramosSegunda = RepartidorPorDia.repartir(
                    segundaRes.valor(),
                    java.util.Arrays.asList(inicioCorte, finEv),
                    ahoraMs,
                    reparto.minutosSegundaSesion());
            tramos.guardarTramos(sesionSiguienteId, tramosSegunda);
        }
        programador.cancelarAvisos(sesionActualId);
        if (programadorRecordatorios != null) {
            programadorRecordatorios.cancelar(sesionSiguienteId);
        }
        noMolestar.restaurarSiAplica();
        marcadorSalida.limpiar();

        return repartoRes;
    }
}
