package io.github.jhoel0521.studyplan.domain;

import java.util.Objects;

/**
 * Bloque posicionado en la rejilla horaria de las vistas {@code Semana} y {@code Día}
 * (HU-17, HU-18, RN-05, RN-09).
 */
public final class BloqueHorarioCalendario {

    private final ItemSesionCalendario item;
    private final int diaColumna;
    private final int minutoInicio;
    private final int minutoFin;
    private final int indiceSolapamiento;
    private final int totalSolapamientos;

    public BloqueHorarioCalendario(
            ItemSesionCalendario item,
            int diaColumna,
            int minutoInicio,
            int minutoFin,
            int indiceSolapamiento,
            int totalSolapamientos) {
        this.item = Objects.requireNonNull(item, "item");
        this.diaColumna = diaColumna;
        this.minutoInicio = minutoInicio;
        this.minutoFin = minutoFin;
        this.indiceSolapamiento = indiceSolapamiento;
        this.totalSolapamientos = totalSolapamientos;
    }

    public ItemSesionCalendario item() {
        return item;
    }

    /** Columna del día: {@code 0..6} (lunes..domingo) en Semana o {@code 0} en Día. */
    public int diaColumna() {
        return diaColumna;
    }

    public int minutoInicio() {
        return minutoInicio;
    }

    public int minutoFin() {
        return minutoFin;
    }

    public int indiceSolapamiento() {
        return indiceSolapamiento;
    }

    public int totalSolapamientos() {
        return totalSolapamientos;
    }
}
