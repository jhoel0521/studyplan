package io.github.jhoel0521.studyplan.domain.readmodel;

import java.util.Objects;
import io.github.jhoel0521.studyplan.domain.enumeration.TopicoEventoEstudio;

/**
 * Resumen agrupado de cantidad y duración acumulada para un {@link TopicoEventoEstudio}
 * ({@code HU-45}, {@code RN-30}).
 */
public final class ResumenTopico {

    private final TopicoEventoEstudio topico;
    private final int cantidad;
    private final int minutos;

    public ResumenTopico(TopicoEventoEstudio topico, int cantidad, int minutos) {
        this.topico = Objects.requireNonNull(topico, "topico");
        this.cantidad = Math.max(0, cantidad);
        this.minutos = Math.max(0, minutos);
    }

    public TopicoEventoEstudio topico() {
        return topico;
    }

    public int cantidad() {
        return cantidad;
    }

    public int minutos() {
        return minutos;
    }
}
