package io.github.jhoel0521.studyplan.domain;

import java.util.Objects;

/**
 * Diagnóstico de una sesión {@code EN_CURSO} al regresar a la app tras una salida o cierre
 * abrupto (HU-40, HU-41, RN-23, RN-24, RN-29).
 */
public final class DiagnosticoRegresoApp {

    private final SesionEstudio sesionEnCurso;
    private final String nombreMateria;
    private final int colorMateria;
    private final long instanteSalidaMilis;
    private final long instanteLimiteMaxMilis;
    private final boolean esRecuperacionCierreAbrupto;
    private final boolean vencidaPorTresDias;

    public DiagnosticoRegresoApp(
            SesionEstudio sesionEnCurso,
            String nombreMateria,
            int colorMateria,
            long instanteSalidaMilis,
            long instanteLimiteMaxMilis,
            boolean esRecuperacionCierreAbrupto,
            boolean vencidaPorTresDias) {
        this.sesionEnCurso = Objects.requireNonNull(sesionEnCurso, "sesionEnCurso");
        this.nombreMateria = Objects.requireNonNull(nombreMateria, "nombreMateria");
        this.colorMateria = colorMateria;
        this.instanteSalidaMilis = instanteSalidaMilis;
        this.instanteLimiteMaxMilis = instanteLimiteMaxMilis;
        this.esRecuperacionCierreAbrupto = esRecuperacionCierreAbrupto;
        this.vencidaPorTresDias = vencidaPorTresDias;
    }

    public SesionEstudio sesionEnCurso() {
        return sesionEnCurso;
    }

    public String nombreMateria() {
        return nombreMateria;
    }

    public int colorMateria() {
        return colorMateria;
    }

    public long instanteSalidaMilis() {
        return instanteSalidaMilis;
    }

    public long instanteLimiteMaxMilis() {
        return instanteLimiteMaxMilis;
    }

    public boolean esRecuperacionCierreAbrupto() {
        return esRecuperacionCierreAbrupto;
    }

    public boolean vencidaPorTresDias() {
        return vencidaPorTresDias;
    }
}
