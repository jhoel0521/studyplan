package io.github.jhoel0521.studyplan.domain.readmodel;

import java.util.Objects;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;

/**
 * Estado completo de la sesión en curso para la pantalla {@code SesionEnCursoActivity}
 * (HU-22 a HU-24, HU-42, HU-43).
 */
public final class ResumenEjecucionSesion {

    private final SesionEstudio sesion;
    private final String nombreMateria;
    private final int colorMateria;
    private final long milisEfectivos;
    private final boolean pausada;
    private final boolean noMolestarActivo;
    private final EstadoPomodoroCalculado estadoPomodoro;
    private final boolean mostrarExtensionPomodoro;
    private final boolean mostrarFinPlanificado;
    private final CandidataInvasion candidataInvasion;
    private final boolean salidaPendienteResolver;

    public ResumenEjecucionSesion(
            SesionEstudio sesion,
            String nombreMateria,
            int colorMateria,
            long milisEfectivos,
            boolean pausada,
            boolean noMolestarActivo,
            EstadoPomodoroCalculado estadoPomodoro,
            boolean mostrarExtensionPomodoro,
            boolean mostrarFinPlanificado,
            CandidataInvasion candidataInvasion) {
        this(
                sesion,
                nombreMateria,
                colorMateria,
                milisEfectivos,
                pausada,
                noMolestarActivo,
                estadoPomodoro,
                mostrarExtensionPomodoro,
                mostrarFinPlanificado,
                candidataInvasion,
                false);
    }

    public ResumenEjecucionSesion(
            SesionEstudio sesion,
            String nombreMateria,
            int colorMateria,
            long milisEfectivos,
            boolean pausada,
            boolean noMolestarActivo,
            EstadoPomodoroCalculado estadoPomodoro,
            boolean mostrarExtensionPomodoro,
            boolean mostrarFinPlanificado,
            CandidataInvasion candidataInvasion,
            boolean salidaPendienteResolver) {
        this.sesion = Objects.requireNonNull(sesion, "sesion");
        this.nombreMateria = Objects.requireNonNull(nombreMateria, "nombreMateria");
        this.colorMateria = colorMateria;
        this.milisEfectivos = Math.max(0L, milisEfectivos);
        this.pausada = pausada;
        this.noMolestarActivo = noMolestarActivo;
        this.estadoPomodoro = estadoPomodoro;
        this.mostrarExtensionPomodoro = mostrarExtensionPomodoro;
        this.mostrarFinPlanificado = mostrarFinPlanificado;
        this.candidataInvasion = candidataInvasion;
        this.salidaPendienteResolver = salidaPendienteResolver;
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

    public long milisEfectivos() {
        return milisEfectivos;
    }

    public boolean pausada() {
        return pausada;
    }

    public boolean noMolestarActivo() {
        return noMolestarActivo;
    }

    public EstadoPomodoroCalculado estadoPomodoro() {
        return estadoPomodoro;
    }

    public boolean mostrarExtensionPomodoro() {
        return mostrarExtensionPomodoro;
    }

    public boolean mostrarFinPlanificado() {
        return mostrarFinPlanificado;
    }

    public CandidataInvasion candidataInvasion() {
        return candidataInvasion;
    }

    public boolean salidaPendienteResolver() {
        return salidaPendienteResolver;
    }
}
