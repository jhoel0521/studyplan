package io.github.jhoel0521.studyplan.domain.readmodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.state.EstadoSesion;

/**
 * Modelo de lectura completo de una sesión para su pantalla de detalle, incluyendo datos de su
 * materia, capacidades permitidas por {@link EstadoSesion}, eventos de estudio y enlace de
 * reprogramación (HU-12, HU-13, HU-14, HU-15, HU-39, RN-07, RN-09, RN-10, RN-12).
 */
public final class DetalleSesion {

    private final SesionEstudio sesion;
    private final String nombreMateria;
    private final int colorMateria;
    private final boolean puedeCompletar;
    private final boolean puedeRegistrarTardio;
    private final boolean puedeReprogramar;
    private final boolean puedeEliminar;
    private final boolean noRealizadaFueraDePlazoTardio;
    private final SesionEstudio sesionDestino;
    private final List<EventoEstudio> eventos;
    private final boolean puedeIniciar;

    public DetalleSesion(
            SesionEstudio sesion,
            String nombreMateria,
            int colorMateria,
            boolean puedeCompletar,
            boolean puedeRegistrarTardio,
            boolean puedeReprogramar,
            boolean puedeEliminar,
            boolean noRealizadaFueraDePlazoTardio,
            SesionEstudio sesionDestino) {
        this(
                sesion,
                nombreMateria,
                colorMateria,
                puedeCompletar,
                puedeRegistrarTardio,
                puedeReprogramar,
                puedeEliminar,
                noRealizadaFueraDePlazoTardio,
                sesionDestino,
                Collections.emptyList(),
                false);
    }

    public DetalleSesion(
            SesionEstudio sesion,
            String nombreMateria,
            int colorMateria,
            boolean puedeCompletar,
            boolean puedeRegistrarTardio,
            boolean puedeReprogramar,
            boolean puedeEliminar,
            boolean noRealizadaFueraDePlazoTardio,
            SesionEstudio sesionDestino,
            List<EventoEstudio> eventos,
            boolean puedeIniciar) {
        this.sesion = Objects.requireNonNull(sesion, "sesion");
        this.nombreMateria = Objects.requireNonNull(nombreMateria, "nombreMateria");
        this.colorMateria = colorMateria;
        this.puedeCompletar = puedeCompletar;
        this.puedeRegistrarTardio = puedeRegistrarTardio;
        this.puedeReprogramar = puedeReprogramar;
        this.puedeEliminar = puedeEliminar;
        this.noRealizadaFueraDePlazoTardio = noRealizadaFueraDePlazoTardio;
        this.sesionDestino = sesionDestino;
        this.eventos = eventos == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(eventos));
        this.puedeIniciar = puedeIniciar;
    }

    public SesionEstudio sesion() {
        return sesion;
    }

    public String nombreMateria() {
        return nombreMateria;
    }

    public int colorMateria() {
        return colorMateria;
    }

    public boolean puedeCompletar() {
        return puedeCompletar;
    }

    public boolean puedeRegistrarTardio() {
        return puedeRegistrarTardio;
    }

    public boolean puedeReprogramar() {
        return puedeReprogramar;
    }

    public boolean puedeEliminar() {
        return puedeEliminar;
    }

    public boolean noRealizadaFueraDePlazoTardio() {
        return noRealizadaFueraDePlazoTardio;
    }

    public SesionEstudio sesionDestino() {
        return sesionDestino;
    }

    public List<EventoEstudio> eventos() {
        return eventos;
    }

    public boolean puedeIniciar() {
        return puedeIniciar;
    }
}
