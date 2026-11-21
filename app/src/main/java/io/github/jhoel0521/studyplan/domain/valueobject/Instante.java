package io.github.jhoel0521.studyplan.domain.valueobject;

import java.util.Locale;
import java.util.Objects;

/**
 * Value Object inmutable que representa un instante absoluto en milisegundos UTC junto con el
 * desfase horario local (en minutos) vigente en el momento en que se originó (HU-02, HU-10,
 * HU-17, HU-18, HU-21, HU-39, RN-04, RN-05, RN-08, RN-31).
 *
 * <p>Implementado en Java puro sin {@code java.time} para mantener compatibilidad con API 24
 * ({@code RNF-17}).
 */
public final class Instante implements Comparable<Instante> {

    public static final long MILIS_POR_MINUTO = 60_000L;
    public static final long MILIS_POR_DIA = 86_400_000L;
    public static final int DESFASE_MINIMO_MINUTOS = -12 * 60;
    public static final int DESFASE_MAXIMO_MINUTOS = 14 * 60;

    private final long utcMilis;
    private final int desfaseMinutos;

    public Instante(long utcMilis, int desfaseMinutos) {
        if (desfaseMinutos < DESFASE_MINIMO_MINUTOS || desfaseMinutos > DESFASE_MAXIMO_MINUTOS) {
            throw new IllegalArgumentException("Desfase horario fuera de rango: " + desfaseMinutos);
        }
        this.utcMilis = utcMilis;
        this.desfaseMinutos = desfaseMinutos;
    }

    public static Instante de(long utcMilis, int desfaseMinutos) {
        return new Instante(utcMilis, desfaseMinutos);
    }

    /** Construye un instante a partir de fecha y hora locales y el desfase en minutos. */
    public static Instante desdeFechaYHoraLocal(Fecha fecha, Hora hora, int desfaseMinutos) {
        Objects.requireNonNull(fecha, "fecha");
        Objects.requireNonNull(hora, "hora");
        long localMilis = fecha.diasDesdeEpoca() * MILIS_POR_DIA
                + hora.minutosDesdeMedianoche() * MILIS_POR_MINUTO;
        long utcMilis = localMilis - desfaseMinutos * MILIS_POR_MINUTO;
        return new Instante(utcMilis, desfaseMinutos);
    }

    public long utcMilis() {
        return utcMilis;
    }

    public int desfaseMinutos() {
        return desfaseMinutos;
    }

    public long localMilis() {
        return utcMilis + desfaseMinutos * MILIS_POR_MINUTO;
    }

    /** Fecha calendario local asociada a este instante usando su desfase guardado. */
    public Fecha fechaLocal() {
        long diasEpoca = Math.floorDiv(localMilis(), MILIS_POR_DIA);
        return Fecha.desdeDiasEpoca(diasEpoca);
    }

    /** Hora local ({@code 00:00..23:59}) asociada a este instante usando su desfase guardado. */
    public Hora horaLocal() {
        long milisEnDia = Math.floorMod(localMilis(), MILIS_POR_DIA);
        int minutos = (int) (milisEnDia / MILIS_POR_MINUTO);
        return Hora.desdeMinutos(minutos).valor();
    }

    /** Instante correspondiente a las {@code 00:00:00.000} del día local de este instante. */
    public Instante inicioDelDiaLocal() {
        long diasEpoca = Math.floorDiv(localMilis(), MILIS_POR_DIA);
        long utcInicio = diasEpoca * MILIS_POR_DIA - desfaseMinutos * MILIS_POR_MINUTO;
        return new Instante(utcInicio, desfaseMinutos);
    }

    /** Devuelve un nuevo instante desplazado {@code minutos} conservando el mismo desfase. */
    public Instante sumarMinutos(long minutos) {
        return new Instante(utcMilis + minutos * MILIS_POR_MINUTO, desfaseMinutos);
    }

    /** Devuelve un nuevo instante con otro desfase horario para el mismo momento UTC. */
    public Instante conDesfase(int nuevoDesfaseMinutos) {
        return new Instante(utcMilis, nuevoDesfaseMinutos);
    }

    public boolean esAnteriorA(Instante otro) {
        return utcMilis < otro.utcMilis;
    }

    public boolean esPosteriorA(Instante otro) {
        return utcMilis > otro.utcMilis;
    }

    public boolean esIgualEnTiempoA(Instante otro) {
        return utcMilis == otro.utcMilis;
    }

    /** Representación legible del desfase horario, por ejemplo {@code UTC−04:00} o {@code UTC+01:00}. */
    public String desfaseTexto() {
        String signo = desfaseMinutos < 0 ? "\u2212" : "+";
        int abs = Math.abs(desfaseMinutos);
        int horas = abs / 60;
        int mins = abs % 60;
        return String.format(Locale.US, "UTC%s%02d:%02d", signo, horas, mins);
    }

    @Override
    public int compareTo(Instante otro) {
        return Long.compare(utcMilis, otro.utcMilis);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Instante)) {
            return false;
        }
        Instante otro = (Instante) o;
        return utcMilis == otro.utcMilis && desfaseMinutos == otro.desfaseMinutos;
    }

    @Override
    public int hashCode() {
        return Objects.hash(utcMilis, desfaseMinutos);
    }

    @Override
    public String toString() {
        return fechaLocal().iso() + " " + horaLocal().texto() + " (" + desfaseTexto() + ")";
    }
}
