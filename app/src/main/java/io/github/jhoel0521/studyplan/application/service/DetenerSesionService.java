package io.github.jhoel0521.studyplan.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.DetenerSesion;
import io.github.jhoel0521.studyplan.application.port.out.ControladorNoMolestar;
import io.github.jhoel0521.studyplan.application.port.out.EventoEstudioRepository;
import io.github.jhoel0521.studyplan.application.port.out.MarcadorSalidaApp;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.MetaSemanalRepository;
import io.github.jhoel0521.studyplan.application.port.out.PerfilRepository;
import io.github.jhoel0521.studyplan.application.port.out.ProgramadorAvisosSesion;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.application.port.out.TramoDiarioRepository;
import io.github.jhoel0521.studyplan.domain.CalculadorTiempoReal;
import io.github.jhoel0521.studyplan.domain.CierreSesion;
import io.github.jhoel0521.studyplan.domain.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.MetaAlcanzada;
import io.github.jhoel0521.studyplan.domain.RepartidorPorDia;
import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.TipoEventoEstudio;

/**
 * Detiene una sesión {@code EN_CURSO}, registra el evento {@code FIN}, materializa la duración
 * real en minutos (>= 0), restaura avisos y No molestar, y detecta metas semanales alcanzadas
 * (HU-22, HU-24, HU-28, RN-07, RN-16, RN-18, RN-26).
 */
public final class DetenerSesionService implements DetenerSesion {

    private final SesionRepository sesiones;
    private final EventoEstudioRepository eventos;
    private final ControladorNoMolestar noMolestar;
    private final ProgramadorAvisosSesion programador;
    private final MarcadorSalidaApp marcadorSalida;
    private final TramoDiarioRepository tramos;
    private final MetaSemanalRepository metas;
    private final PerfilRepository perfiles;
    private final MateriaRepository materias;
    private final Reloj reloj;

    public DetenerSesionService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            ControladorNoMolestar noMolestar,
            ProgramadorAvisosSesion programador,
            MarcadorSalidaApp marcadorSalida,
            Reloj reloj) {
        this(sesiones, eventos, noMolestar, programador, marcadorSalida, null, null, null, null, reloj);
    }

    public DetenerSesionService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            ControladorNoMolestar noMolestar,
            ProgramadorAvisosSesion programador,
            MarcadorSalidaApp marcadorSalida,
            TramoDiarioRepository tramos,
            Reloj reloj) {
        this(sesiones, eventos, noMolestar, programador, marcadorSalida, tramos, null, null, null, reloj);
    }

    public DetenerSesionService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            ControladorNoMolestar noMolestar,
            ProgramadorAvisosSesion programador,
            MarcadorSalidaApp marcadorSalida,
            TramoDiarioRepository tramos,
            MetaSemanalRepository metas,
            PerfilRepository perfiles,
            MateriaRepository materias,
            Reloj reloj) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.noMolestar = Objects.requireNonNull(noMolestar, "noMolestar");
        this.programador = Objects.requireNonNull(programador, "programador");
        this.marcadorSalida = Objects.requireNonNull(marcadorSalida, "marcadorSalida");
        this.tramos = tramos;
        this.metas = metas;
        this.perfiles = perfiles;
        this.materias = materias;
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public Resultado<SesionEstudio> ejecutar(long sesionId) {
        Resultado<CierreSesion> res = ejecutarConMetas(sesionId);
        return res.esValido() ? Resultado.ok(res.valor().sesion()) : Resultado.error(res.error());
    }

    @Override
    public Resultado<CierreSesion> ejecutarConMetas(long sesionId) {
        Optional<SesionEstudio> encontrada = sesiones.buscarPorId(sesionId);
        if (!encontrada.isPresent()) {
            return Resultado.error(ErrorValidacion.SESION_NO_ENCONTRADA);
        }
        SesionEstudio sesion = encontrada.get();
        long ahoraMs = reloj.ahoraMilis();
        List<EventoEstudio> lista = new ArrayList<>(eventos.listarPorSesion(sesionId));

        EventoEstudio finEv = EventoEstudio.crear(
                sesionId,
                TipoEventoEstudio.FIN,
                ahoraMs,
                reloj.fechaDe(ahoraMs),
                reloj.horaDe(ahoraMs),
                "").valor();
        lista.add(finEv);

        int minutos = CalculadorTiempoReal.calcularMinutosReales(
                sesion.tecnica(),
                sesion.configuracionPomodoroSnapshot(),
                sesion.duracionPlanificadaMinutos(),
                lista,
                ahoraMs);

        Resultado<SesionEstudio> detenidaRes = sesion.detener(minutos);
        if (!detenidaRes.esValido()) {
            return Resultado.error(detenidaRes.error());
        }

        DetectorFelicitacionMetas.InstantaneaAntes antes =
                DetectorFelicitacionMetas.capturarAntes(tramos, metas, reloj);
        eventos.guardar(finEv);
        sesiones.actualizar(detenidaRes.valor());
        if (tramos != null) {
            tramos.guardarTramos(
                    sesionId,
                    RepartidorPorDia.repartir(detenidaRes.valor(), lista, ahoraMs, minutos));
        }
        List<MetaAlcanzada> alcanzadas = DetectorFelicitacionMetas.detectarYMarcar(
                antes, metas, perfiles, materias, tramos);
        programador.cancelarAvisos(sesionId);
        noMolestar.restaurarSiAplica();
        marcadorSalida.limpiar();

        return Resultado.ok(new CierreSesion(detenidaRes.valor(), alcanzadas));
    }
}
