package io.github.jhoel0521.studyplan.application.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.RegistrarSalidaApp;
import io.github.jhoel0521.studyplan.application.port.out.EventoEstudioRepository;
import io.github.jhoel0521.studyplan.application.port.out.MarcadorSalidaApp;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.domain.CalculadorTiempoReal;
import io.github.jhoel0521.studyplan.domain.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.TipoEventoEstudio;

/**
 * Registra el evento {@code SALIDA_APP} y la marca ligera cuando el estudiante sale de la app
 * teniendo una sesión {@code EN_CURSO} no pausada (HU-40, RN-23).
 */
public final class RegistrarSalidaAppService implements RegistrarSalidaApp {

    private final SesionRepository sesiones;
    private final EventoEstudioRepository eventos;
    private final MarcadorSalidaApp marcadorSalida;
    private final Reloj reloj;

    public RegistrarSalidaAppService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            MarcadorSalidaApp marcadorSalida,
            Reloj reloj) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.marcadorSalida = Objects.requireNonNull(marcadorSalida, "marcadorSalida");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public Optional<Long> ejecutar() {
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
        long ahoraMs = reloj.ahoraMilis();
        eventos.guardar(EventoEstudio.crear(
                activa.id(),
                TipoEventoEstudio.SALIDA_APP,
                ahoraMs,
                reloj.fechaDe(ahoraMs),
                reloj.horaDe(ahoraMs),
                "").valor());
        marcadorSalida.marcarSalida(activa.id(), ahoraMs);
        return Optional.of(activa.id());
    }
}
