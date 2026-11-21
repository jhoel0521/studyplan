package io.github.jhoel0521.studyplan.domain.valueobject;

import java.util.Objects;
import io.github.jhoel0521.studyplan.domain.enumeration.VistaCalendario;

/**
 * Periodo de consulta para Estadísticas ({@code SEMANA} lunes–domingo o {@code MES} completo),
 * delegando los límites visibles en {@link PeriodoCalendario} ({@code HU-29}, {@code HU-30},
 * {@code RN-13}).
 */
public final class PeriodoEstadisticas {

    public enum Tipo {
        SEMANA,
        MES
    }

    private final Tipo tipo;
    private final PeriodoCalendario periodoCalendario;

    private PeriodoEstadisticas(Tipo tipo, Fecha fechaFoco) {
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(fechaFoco, "fechaFoco");
        VistaCalendario vista = tipo == Tipo.SEMANA ? VistaCalendario.SEMANA : VistaCalendario.MES;
        this.periodoCalendario = PeriodoCalendario.de(vista, fechaFoco, fechaFoco);
    }

    public static PeriodoEstadisticas semanal(Fecha fechaEnSemana) {
        return new PeriodoEstadisticas(Tipo.SEMANA, fechaEnSemana);
    }

    public static PeriodoEstadisticas mensual(Fecha fechaEnMes) {
        return new PeriodoEstadisticas(Tipo.MES, fechaEnMes);
    }

    public static PeriodoEstadisticas de(Tipo tipo, Fecha fechaFoco) {
        return new PeriodoEstadisticas(tipo, fechaFoco);
    }

    public Tipo tipo() {
        return tipo;
    }

    public Fecha fechaFoco() {
        return periodoCalendario.fechaFoco();
    }

    public Fecha inicio() {
        return periodoCalendario.rangoVisibleInicio();
    }

    public Fecha fin() {
        return periodoCalendario.rangoVisibleFin();
    }

    public PeriodoEstadisticas conTipo(Tipo nuevoTipo) {
        return new PeriodoEstadisticas(nuevoTipo, fechaFoco());
    }

    public PeriodoEstadisticas anterior() {
        if (tipo == Tipo.SEMANA) {
            return new PeriodoEstadisticas(Tipo.SEMANA, inicio().sumarDias(-7));
        }
        Fecha primero = inicio();
        int mes = primero.mes() - 1;
        int anio = primero.anio();
        if (mes < 1) {
            mes = 12;
            anio--;
        }
        return new PeriodoEstadisticas(Tipo.MES, Fecha.de(anio, mes, 1).valor());
    }

    public PeriodoEstadisticas siguiente() {
        if (tipo == Tipo.SEMANA) {
            return new PeriodoEstadisticas(Tipo.SEMANA, inicio().sumarDias(7));
        }
        Fecha primero = inicio();
        int mes = primero.mes() + 1;
        int anio = primero.anio();
        if (mes > 12) {
            mes = 1;
            anio++;
        }
        return new PeriodoEstadisticas(Tipo.MES, Fecha.de(anio, mes, 1).valor());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PeriodoEstadisticas)) {
            return false;
        }
        PeriodoEstadisticas that = (PeriodoEstadisticas) o;
        return tipo == that.tipo
                && inicio().equals(that.inicio())
                && fin().equals(that.fin());
    }

    @Override
    public int hashCode() {
        return Objects.hash(tipo, inicio(), fin());
    }
}
