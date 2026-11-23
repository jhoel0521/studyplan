package io.github.jhoel0521.studyplan.domain.enumeration;

/**
 * Cada cuánto se respalda automáticamente en Google Drive. El respaldo nace apagado ({@link #NUNCA}) y lo activa el
 * estudiante.
 *
 * <p>Implementa: HU-35 y RN-19.
 */
public enum FrecuenciaRespaldo {
    /** Sin respaldo automático ni manual programado; es el valor inicial. */
    NUNCA(0),
    /** Una vez al día. */
    DIARIA(1),
    /** Una vez por semana. */
    SEMANAL(7),
    /** Una vez al mes (30 días). */
    MENSUAL(30),
    /** Solo cuando el estudiante pulsa «Respaldar ahora». */
    SOLO_MANUAL(0);

    private final int periodoDias;

    FrecuenciaRespaldo(int periodoDias) {
        this.periodoDias = periodoDias;
    }

    /** Días entre respaldos automáticos; 0 si no hay respaldo automático. */
    public int periodoDias() {
        return periodoDias;
    }

    /** {@code true} si hay que programar una tarea periódica. */
    public boolean esAutomatica() {
        return periodoDias > 0;
    }
}
