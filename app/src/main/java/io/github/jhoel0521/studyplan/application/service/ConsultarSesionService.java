package io.github.jhoel0521.studyplan.application.service;

import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.ConsultarSesion;
import io.github.jhoel0521.studyplan.application.port.in.VencerSesionesPendientes;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.domain.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.DetalleSesion;
import io.github.jhoel0521.studyplan.domain.EstadoSesion;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;

/**
 * Implementa {@link ConsultarSesion}: actualiza sesiones vencidas, carga materia y capacidades del
 * estado actual y resuelve la sesión destino si está {@code REPROGRAMADA}
 * (HU-12, HU-13, HU-14, HU-15, RN-07, RN-09, RN-10, RN-12).
 */
public final class ConsultarSesionService implements ConsultarSesion {

    private final SesionRepository sesiones;
    private final MateriaRepository materias;
    private final Reloj reloj;
    private final VencerSesionesPendientes vencerPendientes;

    public ConsultarSesionService(
            SesionRepository sesiones,
            MateriaRepository materias,
            Reloj reloj,
            VencerSesionesPendientes vencerPendientes) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.materias = Objects.requireNonNull(materias, "materias");
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

        SesionEstudio sesionDestino = null;
        if (estado.codigo() == CodigoEstadoSesion.REPROGRAMADA && sesion.id() != null) {
            sesionDestino = sesiones.buscarReprogramadaHijaDe(sesion.id()).orElse(null);
        }
        return Optional.of(new DetalleSesion(
                sesion,
                nombreMateria,
                colorMateria,
                puedeCompletar,
                puedeRegistrarTardio,
                puedeReprogramar,
                puedeEliminar,
                fueraDePlazoTardio,
                sesionDestino));
    }
}
