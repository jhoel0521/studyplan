package io.github.jhoel0521.studyplan.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Datos del resumen en la pantalla de Inicio: progreso semanal real, próxima sesión pendiente y
 * sesiones de hoy.
 *
 * <p>Implementa: HU-02, RN-11, RN-13 y RN-15.
 */
public final class ResumenInicio {

    private final String nombre;
    private final double horasRealizadas;
    private final double metaSemanalHoras;
    private final Fecha hoy;
    private final DetalleSesionInicio proximaSesion;
    private final List<DetalleSesionInicio> sesionesDeHoy;

    public ResumenInicio(String nombre, double horasRealizadas, double metaSemanalHoras) {
        this(nombre, horasRealizadas, metaSemanalHoras, null, null, Collections.emptyList());
    }

    public ResumenInicio(
            String nombre,
            double horasRealizadas,
            double metaSemanalHoras,
            Fecha hoy,
            DetalleSesionInicio proximaSesion,
            List<DetalleSesionInicio> sesionesDeHoy) {
        this.nombre = nombre;
        this.horasRealizadas = horasRealizadas;
        this.metaSemanalHoras = metaSemanalHoras;
        this.hoy = hoy;
        this.proximaSesion = proximaSesion;
        this.sesionesDeHoy = sesionesDeHoy == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(sesionesDeHoy));
    }

    public String nombre() {
        return nombre;
    }

    public double horasRealizadas() {
        return horasRealizadas;
    }

    public double metaSemanalHoras() {
        return metaSemanalHoras;
    }

    public Fecha hoy() {
        return hoy;
    }

    public DetalleSesionInicio proximaSesion() {
        return proximaSesion;
    }

    public List<DetalleSesionInicio> sesionesDeHoy() {
        return sesionesDeHoy;
    }

    /** @return el avance de la meta como porcentaje entero entre 0 y 100 */
    public int porcentajeProgreso() {
        if (metaSemanalHoras <= 0) {
            return 0;
        }
        int p = (int) Math.round((horasRealizadas / metaSemanalHoras) * 100.0);
        return Math.max(0, Math.min(100, p));
    }
}
