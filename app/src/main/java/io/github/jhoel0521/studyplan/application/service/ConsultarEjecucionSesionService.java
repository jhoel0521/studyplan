package io.github.jhoel0521.studyplan.application.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.ConsultarEjecucionSesion;
import io.github.jhoel0521.studyplan.application.port.out.AjustesRepository;
import io.github.jhoel0521.studyplan.application.port.out.ControladorNoMolestar;
import io.github.jhoel0521.studyplan.application.port.out.EventoEstudioRepository;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.domain.Ajustes;
import io.github.jhoel0521.studyplan.domain.CalculadorPomodoro;
import io.github.jhoel0521.studyplan.domain.CalculadorTiempoReal;
import io.github.jhoel0521.studyplan.domain.CandidataInvasion;
import io.github.jhoel0521.studyplan.domain.ConfiguracionPomodoro;
import io.github.jhoel0521.studyplan.domain.EstadoPomodoroCalculado;
import io.github.jhoel0521.studyplan.domain.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.ResumenEjecucionSesion;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.TecnicaEstudio;
import io.github.jhoel0521.studyplan.domain.TipoEventoEstudio;

/**
 * Calcula en vivo la proyección {@link ResumenEjecucionSesion} para la UI de ejecución
 * (HU-22, HU-23, HU-24, HU-42, HU-43).
 */
public final class ConsultarEjecucionSesionService implements ConsultarEjecucionSesion {

    private final SesionRepository sesiones;
    private final EventoEstudioRepository eventos;
    private final MateriaRepository materias;
    private final AjustesRepository ajustesRepo;
    private final ControladorNoMolestar noMolestar;
    private final Reloj reloj;

    public ConsultarEjecucionSesionService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            MateriaRepository materias,
            AjustesRepository ajustesRepo,
            ControladorNoMolestar noMolestar,
            Reloj reloj) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.materias = Objects.requireNonNull(materias, "materias");
        this.ajustesRepo = Objects.requireNonNull(ajustesRepo, "ajustesRepo");
        this.noMolestar = Objects.requireNonNull(noMolestar, "noMolestar");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public Optional<ResumenEjecucionSesion> ejecutar(long sesionId) {
        Optional<SesionEstudio> encontrada = sesiones.buscarPorId(sesionId);
        if (!encontrada.isPresent()) {
            return Optional.empty();
        }
        SesionEstudio sesion = encontrada.get();
        Materia materia = materias.obtener(sesion.materiaId()).orElse(null);
        String nombreMateria = materia == null ? "" : materia.nombre();
        int colorMateria = materia == null ? 0 : materia.color();

        long ahoraMs = reloj.ahoraMilis();
        List<EventoEstudio> lista = eventos.listarPorSesion(sesionId);
        long milisEfectivos = CalculadorTiempoReal.calcularMilisEfectivos(
                sesion.tecnica(),
                sesion.configuracionPomodoroSnapshot(),
                sesion.duracionPlanificadaMinutos(),
                lista,
                ahoraMs);
        boolean pausada = CalculadorTiempoReal.estaPausadaActualmente(lista);

        Ajustes ajustes = ajustesRepo.obtener();
        boolean dndActivo = !pausada
                && sesion.debeActivarNoMolestar(ajustes.noMolestarGlobal())
                && noMolestar.tienePermiso();

        EstadoPomodoroCalculado estadoPom = null;
        boolean mostrarExtPom = false;
        boolean enTiempoExtra = EjecucionHitos.contieneTipo(lista, TipoEventoEstudio.TIEMPO_EXTRA);
        if (sesion.tecnica() == TecnicaEstudio.POMODORO) {
            ConfiguracionPomodoro cfg = sesion.configuracionPomodoroSnapshot();
            if (cfg != null) {
                long dentroApp = CalculadorTiempoReal.calcularMilisDentroDeApp(lista, ahoraMs);
                int objetivo = CalculadorPomodoro.pomodorosQueCabenEnPlan(
                        sesion.duracionPlanificadaMinutos(), cfg);
                estadoPom = CalculadorPomodoro.calcularEstado(dentroApp, cfg, objetivo, enTiempoExtra);
                mostrarExtPom = estadoPom.ciclosPlanificadosCompletados() && !enTiempoExtra;
            }
        }

        long finPlanMs = sesion.tienePlan()
                ? reloj.instanteDe(sesion.fecha(), sesion.horaFin())
                : Long.MAX_VALUE;
        boolean mostrarFinPlan = sesion.tienePlan()
                && ahoraMs >= finPlanMs
                && !EjecucionHitos.contieneTipo(lista, TipoEventoEstudio.FIN_PLANIFICADO)
                && !enTiempoExtra;
        boolean salidaPendiente = CalculadorTiempoReal.tieneSalidaSinResolver(lista);

        CandidataInvasion invasion = EjecucionHitos.detectarInvasion(
                sesion, ahoraMs, sesiones, materias, reloj);

        return Optional.of(new ResumenEjecucionSesion(
                sesion,
                nombreMateria,
                colorMateria,
                milisEfectivos,
                pausada,
                dndActivo,
                estadoPom,
                mostrarExtPom,
                mostrarFinPlan,
                invasion,
                salidaPendiente));
    }
}
