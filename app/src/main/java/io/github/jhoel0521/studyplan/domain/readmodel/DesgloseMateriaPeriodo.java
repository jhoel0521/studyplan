package io.github.jhoel0521.studyplan.domain.readmodel;

import java.util.Objects;

/**
 * Desglose de tiempo estudiado y cumplimiento por materia en un periodo, compartido como base para
 * Estadísticas ({@code HU-30}) y Reportes ({@code HU-32}).
 */
public final class DesgloseMateriaPeriodo {

    private final long materiaId;
    private final String nombreMateria;
    private final boolean esGeneral;
    private final int colorMateria;
    private final boolean archivada;
    private final int minutosReales;
    private final double porcentajeTiempo;
    private final int minutosPlanificadosDeSesiones;
    private final int sesionesNoRealizadas;

    public DesgloseMateriaPeriodo(
            long materiaId,
            String nombreMateria,
            boolean esGeneral,
            int colorMateria,
            boolean archivada,
            int minutosReales,
            double porcentajeTiempo,
            int minutosPlanificadosDeSesiones,
            int sesionesNoRealizadas) {
        this.materiaId = materiaId;
        this.nombreMateria = Objects.requireNonNull(nombreMateria, "nombreMateria");
        this.esGeneral = esGeneral;
        this.colorMateria = colorMateria;
        this.archivada = archivada;
        this.minutosReales = Math.max(0, minutosReales);
        this.porcentajeTiempo = Math.max(0.0, porcentajeTiempo);
        this.minutosPlanificadosDeSesiones = Math.max(0, minutosPlanificadosDeSesiones);
        this.sesionesNoRealizadas = Math.max(0, sesionesNoRealizadas);
    }

    public long materiaId() {
        return materiaId;
    }

    public String nombreMateria() {
        return nombreMateria;
    }

    public boolean esGeneral() {
        return esGeneral;
    }

    public int colorMateria() {
        return colorMateria;
    }

    public boolean archivada() {
        return archivada;
    }

    public int minutosReales() {
        return minutosReales;
    }

    public double horasReales() {
        return minutosReales / 60.0;
    }

    public double porcentajeTiempo() {
        return porcentajeTiempo;
    }

    public int minutosPlanificadosDeSesiones() {
        return minutosPlanificadosDeSesiones;
    }

    public int sesionesNoRealizadas() {
        return sesionesNoRealizadas;
    }

    /** Porcentaje real frente a planificado ({@code null} si no hubo minutos planificados). */
    public Double cumplimientoPlanificado() {
        if (minutosPlanificadosDeSesiones <= 0) {
            return null;
        }
        return minutosReales * 100.0 / minutosPlanificadosDeSesiones;
    }
}
