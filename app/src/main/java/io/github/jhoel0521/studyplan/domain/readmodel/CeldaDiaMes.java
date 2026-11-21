package io.github.jhoel0521.studyplan.domain.readmodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;

/**
 * Celda inmutable de un día dentro de la cuadrícula mensual de 7 columnas (HU-16, RN-13).
 */
public final class CeldaDiaMes {

    private final Fecha fecha;
    private final boolean delMesActual;
    private final boolean esHoy;
    private final boolean esSeleccionado;
    private final List<Integer> coloresMaterias;
    private final int minutosTotalesDia;
    private final int cantidadSesiones;

    public CeldaDiaMes(
            Fecha fecha,
            boolean delMesActual,
            boolean esHoy,
            boolean esSeleccionado,
            List<Integer> coloresMaterias,
            int minutosTotalesDia,
            int cantidadSesiones) {
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        this.delMesActual = delMesActual;
        this.esHoy = esHoy;
        this.esSeleccionado = esSeleccionado;
        this.coloresMaterias = coloresMaterias == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(coloresMaterias));
        this.minutosTotalesDia = minutosTotalesDia;
        this.cantidadSesiones = cantidadSesiones;
    }

    public Fecha fecha() {
        return fecha;
    }

    public boolean delMesActual() {
        return delMesActual;
    }

    public boolean esHoy() {
        return esHoy;
    }

    public boolean esSeleccionado() {
        return esSeleccionado;
    }

    public List<Integer> coloresMaterias() {
        return coloresMaterias;
    }

    public int minutosTotalesDia() {
        return minutosTotalesDia;
    }

    public int cantidadSesiones() {
        return cantidadSesiones;
    }
}
