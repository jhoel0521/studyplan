package io.github.jhoel0521.studyplan.application.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.RegistrarSalidaApp;
import io.github.jhoel0521.studyplan.application.port.out.EventoEstudioRepository;
import io.github.jhoel0521.studyplan.application.port.out.MarcadorSalidaApp;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.domain.service.CalculadorPomodoro;
import io.github.jhoel0521.studyplan.domain.service.CalculadorTiempoReal;
import io.github.jhoel0521.studyplan.domain.valueobject.ConfiguracionPomodoro;
import io.github.jhoel0521.studyplan.domain.readmodel.EstadoPomodoroCalculado;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.readmodel.SalidaRegistrada;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio;

/**
 * Registra el evento {@code SALIDA_APP} y la marca ligera cuando el estudiante sale de la app
 * teniendo una sesión {@code EN_CURSO} no pausada ni en descanso Pomodoro (HU-40, RN-23).
 */
public final class RegistrarSalidaAppService implements RegistrarSalidaApp {

    private final SesionRepository sesiones;
    private final EventoEstudioRepository eventos;
    private final MateriaRepository materias;
    private final MarcadorSalidaApp marcadorSalida;
    private final Reloj reloj;

    public RegistrarSalidaAppService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            MarcadorSalidaApp marcadorSalida,
            Reloj reloj) {
        this(sesiones, eventos, null, marcadorSalida, reloj);
    }

    public RegistrarSalidaAppService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            MateriaRepository materias,
            MarcadorSalidaApp marcadorSalida,
            Reloj reloj) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.materias = materias;
        this.marcadorSalida = Objects.requireNonNull(marcadorSalida, "marcadorSalida");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public Optional<SalidaRegistrada> ejecutar(long instanteSalidaMs) {
        Optional<SesionEstudio> activaOpt = sesiones.buscarEnCurso();
        if (!activaOpt.isPresent()) {
            return Optional.empty();
        }
        SesionEstudio activa = activaOpt.get();
        List<EventoEstudio> lista = eventos.listarPorSesion(activa.id());
        if (CalculadorTiempoReal.estaPausadaActualmente(lista)
                || CalculadorTiempoReal.tieneSalidaSinResolver(lista)) {
            return Optional.empty();
        }

        // Si ya hay un evento posterior a la salida (por ejemplo pausar y reanudar durante la gracia), registrar
        // la salida en el pasado rompería el orden cronológico de los eventos.
        if (!lista.isEmpty() && lista.get(lista.size() - 1).instanteMilis() > instanteSalidaMs) {
            return Optional.empty();
        }

        if (activa.tecnica() == TecnicaEstudio.POMODORO) {
            ConfiguracionPomodoro cfg = activa.configuracionPomodoroSnapshot();
            if (cfg != null) {
                long dentroApp = CalculadorTiempoReal.calcularMilisDentroDeApp(lista, instanteSalidaMs);
                int objetivo = CalculadorPomodoro.pomodorosQueCabenEnPlan(
                        activa.duracionPlanificadaMinutos(), cfg);
                boolean extra = EjecucionHitos.contieneTipo(lista, TipoEventoEstudio.TIEMPO_EXTRA);
                EstadoPomodoroCalculado estadoPom =
                        CalculadorPomodoro.calcularEstado(dentroApp, cfg, objetivo, extra);
                if (CalculadorPomodoro.estaEnDescanso(estadoPom.faseActual())) {
                    long finDescansoMs = CalculadorPomodoro.calcularFinDescansoProyectado(
                            instanteSalidaMs, estadoPom);
                    marcadorSalida.marcarFinDescansoPomodoro(activa.id(), finDescansoMs);
                    return Optional.empty();
                }
            }
        }

        eventos.guardar(EventoEstudio.crear(
                activa.id(),
                TipoEventoEstudio.SALIDA_APP,
                instanteSalidaMs,
                reloj.fechaDe(instanteSalidaMs),
                reloj.horaDe(instanteSalidaMs),
                "").valor());
        marcadorSalida.marcarSalida(activa.id(), instanteSalidaMs);
        String nombreMateria = materias == null
                ? ""
                : materias.obtener(activa.materiaId()).map(Materia::nombre).orElse("");
        return Optional.of(new SalidaRegistrada(activa.id(), nombreMateria));
    }
}
