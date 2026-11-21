package io.github.jhoel0521.studyplan.application.service.tecnicas;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.tecnicas.RegistrarBloqueoPantalla;
import io.github.jhoel0521.studyplan.application.port.out.EventoEstudioRepository;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.domain.service.CalculadorTiempoReal;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio;

/**
 * Registra {@code BLOQUEO_PANTALLA} cuando la pantalla se apaga con la app en primer plano y hay
 * una sesión {@code EN_CURSO} sin salida pendiente. No se duplica si el último evento ya es un
 * bloqueo (HU-50, HU-40, RN-23).
 */
public final class RegistrarBloqueoPantallaService implements RegistrarBloqueoPantalla {

    private final SesionRepository sesiones;
    private final EventoEstudioRepository eventos;
    private final Reloj reloj;

    public RegistrarBloqueoPantallaService(
            SesionRepository sesiones, EventoEstudioRepository eventos, Reloj reloj) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public Optional<EventoEstudio> ejecutar() {
        Optional<SesionEstudio> activa = sesiones.buscarEnCurso();
        if (!activa.isPresent()) {
            return Optional.empty();
        }
        List<EventoEstudio> lista = eventos.listarPorSesion(activa.get().id());
        if (CalculadorTiempoReal.tieneSalidaSinResolver(lista)) {
            return Optional.empty();
        }
        if (!lista.isEmpty()
                && lista.get(lista.size() - 1).tipo() == TipoEventoEstudio.BLOQUEO_PANTALLA) {
            return Optional.empty();
        }
        long ahoraMs = reloj.ahoraMilis();
        return Optional.of(eventos.guardar(EventoEstudio.crear(
                activa.get().id(),
                TipoEventoEstudio.BLOQUEO_PANTALLA,
                ahoraMs,
                reloj.fechaDe(ahoraMs),
                reloj.horaDe(ahoraMs),
                "").valor()));
    }
}
