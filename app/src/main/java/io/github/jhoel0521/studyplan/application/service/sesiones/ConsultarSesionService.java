package io.github.jhoel0521.studyplan.application.service.sesiones;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.sesiones.ConsultarSesion;
import io.github.jhoel0521.studyplan.application.port.in.sesiones.VencerSesionesPendientes;
import io.github.jhoel0521.studyplan.application.port.out.persistence.EventoEstudioRepository;
import io.github.jhoel0521.studyplan.application.port.out.persistence.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.system.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.persistence.SesionRepository;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.readmodel.DetalleSesion;
import io.github.jhoel0521.studyplan.domain.state.EstadoSesion;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;

/**
 * Implementa {@link ConsultarSesion}: actualiza sesiones vencidas, carga materia, eventos de
 * estudio y capacidades del estado actual y resuelve la sesión destino si está {@code REPROGRAMADA}
 * (HU-12, HU-13, HU-14, HU-15, HU-22, HU-39, RN-07, RN-09, RN-10, RN-12).
 */
public final class ConsultarSesionService implements ConsultarSesion {

    private final SesionRepository sesiones;
    private final MateriaRepository materias;
    private final EventoEstudioRepository eventos;
    private final Reloj reloj;
    private final VencerSesionesPendientes vencerPendientes;

    public ConsultarSesionService(
            SesionRepository sesiones,
            MateriaRepository materias,
            Reloj reloj,
            VencerSesionesPendientes vencerPendientes) {
        this(sesiones, materias, null, reloj, vencerPendientes);
    }

    public ConsultarSesionService(
            SesionRepository sesiones,
            MateriaRepository materias,
            EventoEstudioRepository eventos,
            Reloj reloj,
            VencerSesionesPendientes vencerPendientes) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.materias = Objects.requireNonNull(materias, "materias");
        this.eventos = eventos;
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.vencerPendientes = Objects.requireNonNull(vencerPendientes, "vencerPendientes");
    }

    @Override
    public Optional<DetalleSesion> ejecutar(long sesionId) {
        vencerPendientes.ejecutar();
        SesionEstudio sesion = sesiones.buscarPorId(sesionId).orElse(null);
        if (sesion == null) {
            return Optional.empty();
        }
        Materia materia = materias.obtener(sesion.materiaId()).orElse(null);
        String nombreMateria = materia == null ? "" : materia.nombre();
        int colorMateria = materia == null ? 0 : materia.color();

        Fecha hoy = reloj.hoy();
        int diasAntiguedad = sesion.fecha().diasHasta(hoy);
        EstadoSesion estado = sesion.estado();
        boolean puedeCompletar = estado.puedeCompletar(sesion.fecha(), hoy);
        boolean puedeRegistrarTardio = estado.puedeRegistrarTardio(diasAntiguedad);
        boolean puedeReprogramar = estado.puedeReprogramar();
        boolean puedeEliminar = estado.puedeEliminar();
        boolean fueraDePlazoTardio =
                estado.codigo() == CodigoEstadoSesion.NO_REALIZADA && !puedeRegistrarTardio;
        boolean puedeIniciar =
                estado.codigo() == CodigoEstadoSesion.PENDIENTE
                        && (sesion.fecha().equals(hoy)
                                || sesion.estaEnVentanaRecordatorio(reloj.ahoraMilis()));

        SesionEstudio sesionDestino = null;
        if (estado.codigo() == CodigoEstadoSesion.REPROGRAMADA && sesion.id() != null) {
            sesionDestino = sesiones.buscarReprogramadaHijaDe(sesion.id()).orElse(null);
        }
        List<EventoEstudio> listaEventos = eventos == null
                ? Collections.emptyList()
                : eventos.listarPorSesion(sesionId);
        return Optional.of(new DetalleSesion(
                sesion,
                nombreMateria,
                colorMateria,
                puedeCompletar,
                puedeRegistrarTardio,
                puedeReprogramar,
                puedeEliminar,
                fueraDePlazoTardio,
                sesionDestino,
                listaEventos,
                puedeIniciar));
    }
}
