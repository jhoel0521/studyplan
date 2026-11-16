package io.github.jhoel0521.studyplan.application.service;

import java.util.List;
import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.VencerSesionesPendientes;
import io.github.jhoel0521.studyplan.application.port.out.ControladorNoMolestar;
import io.github.jhoel0521.studyplan.application.port.out.EventoEstudioRepository;
import io.github.jhoel0521.studyplan.application.port.out.ProgramadorAvisosSesion;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.application.port.out.TramoDiarioRepository;
import io.github.jhoel0521.studyplan.domain.CalculadorTiempoReal;
import io.github.jhoel0521.studyplan.domain.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.RepartidorPorDia;
import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.TipoEventoEstudio;

/**
 * Implementa {@link VencerSesionesPendientes}: pasa a {@code NO_REALIZADA} toda sesión
 * {@code PENDIENTE} cuyo fin planificado sea anterior a hoy sin tocar sesiones {@code EN_CURSO}
 * activas, y cierra las {@code EN_CURSO} con más de 3 días sin actividad (HU-13, HU-41, RN-08,
 * RN-29, RN-31).
 */
public final class VencerSesionesPendientesService implements VencerSesionesPendientes {

    private final SesionRepository sesiones;
    private final EventoEstudioRepository eventos;
    private final ControladorNoMolestar noMolestar;
    private final ProgramadorAvisosSesion programador;
    private final TramoDiarioRepository tramos;
    private final Reloj reloj;

    public VencerSesionesPendientesService(SesionRepository sesiones, Reloj reloj) {
        this(sesiones, null, null, null, null, reloj);
    }

    public VencerSesionesPendientesService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            ControladorNoMolestar noMolestar,
            ProgramadorAvisosSesion programador,
            Reloj reloj) {
        this(sesiones, eventos, noMolestar, programador, null, reloj);
    }

    public VencerSesionesPendientesService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            ControladorNoMolestar noMolestar,
            ProgramadorAvisosSesion programador,
            TramoDiarioRepository tramos,
            Reloj reloj) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.eventos = eventos;
        this.noMolestar = noMolestar;
        this.programador = programador;
        this.tramos = tramos;
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public int ejecutar() {
        Fecha hoy = reloj.hoy();
        List<SesionEstudio> candidatas = sesiones.listarPendientesAnterioresA(hoy);
        int vencidas = 0;
        for (SesionEstudio sesion : candidatas) {
            if (!sesion.fechaFinLocal().esAnteriorA(hoy)) {
                continue;
            }
            Resultado<SesionEstudio> transicion = sesion.vencer();
            if (transicion.esValido()) {
                sesiones.actualizar(transicion.valor());
                vencidas++;
            }
        }

        if (eventos != null) {
            long ahoraMs = reloj.ahoraMilis();
            int desfase = reloj.ahoraInstante().desfaseMinutos();
            for (SesionEstudio activa : sesiones.listarEnCurso()) {
                List<EventoEstudio> lista = eventos.listarPorSesion(activa.id());
                Long ultimaMarca = CalculadorTiempoReal.obtenerUltimoInstanteDentroDeApp(lista);
                Long salida = CalculadorTiempoReal.obtenerInstanteSalidaSinResolver(lista);
                long ref = salida != null ? salida : (ultimaMarca != null ? ultimaMarca : ahoraMs);
                if (ahoraMs - ref > EvaluarRecuperacionSesionService.TRES_DIAS_MILIS) {
                    long cierreMs = ultimaMarca != null ? ultimaMarca : ref;
                    int minutos = CalculadorTiempoReal.calcularMinutosReales(
                            activa.tecnica(),
                            activa.configuracionPomodoroSnapshot(),
                            activa.duracionPlanificadaMinutos(),
                            lista,
                            cierreMs);
                    Resultado<SesionEstudio> cerrada = activa.cerrarPorPlazoVencido(minutos);
                    if (cerrada.esValido()) {
                        EventoEstudio finEv = eventos.guardar(EventoEstudio.crear(
                                activa.id(),
                                TipoEventoEstudio.FIN,
                                cierreMs,
                                desfase,
                                "").valor());
                        sesiones.actualizar(cerrada.valor());
                        if (tramos != null) {
                            List<EventoEstudio> conFin = new java.util.ArrayList<>(lista);
                            conFin.add(finEv);
                            tramos.guardarTramos(
                                    activa.id(),
                                    RepartidorPorDia.repartir(
                                            cerrada.valor(), conFin, cierreMs, minutos));
                        }
                        if (programador != null) {
                            programador.cancelarAvisos(activa.id());
                        }
                        if (noMolestar != null) {
                            noMolestar.restaurarSiAplica();
                        }
                        vencidas++;
                    }
                }
            }
        }
        return vencidas;
    }
}
