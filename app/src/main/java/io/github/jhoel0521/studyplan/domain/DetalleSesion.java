package io.github.jhoel0521.studyplan.domain;

import java.util.Objects;

/**
 * Modelo de lectura completo de una sesión para su pantalla de detalle, incluyendo datos de su
 * materia, capacidades permitidas por {@link EstadoSesion} y enlace de reprogramación
 * (HU-12, HU-13, HU-14, HU-15, RN-07, RN-09, RN-10, RN-12).
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
        this.sesion = Objects.requireNonNull(sesion, "sesion");
        this.nombreMateria = Objects.requireNonNull(nombreMateria, "nombreMateria");
        this.colorMateria = colorMateria;
        this.puedeCompletar = puedeCompletar;
        this.puedeRegistrarTardio = puedeRegistrarTardio;
        this.puedeReprogramar = puedeReprogramar;
        this.puedeEliminar = puedeEliminar;
        this.noRealizadaFueraDePlazoTardio = noRealizadaFueraDePlazoTardio;
        this.sesionDestino = sesionDestino;
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
}
