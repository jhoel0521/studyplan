package io.github.jhoel0521.studyplan.domain;

/**
 * Modos de visualización del calendario de estudio (HU-16, HU-17, HU-18).
 */
public enum VistaCalendario {
    /** Línea de tiempo diaria de 24 horas (HU-18). */
    DIA,
    /** Horario semanal de lunes a domingo (HU-17, RN-13). */
    SEMANA,
    /** Cuadrícula mensual de 7 columnas con detalle inferior del día (HU-16, RN-13). */
    MES;

    /** Vista inicial por defecto cuando aún no existe preferencia guardada. */
    public static final VistaCalendario POR_DEFECTO = SEMANA;
}
