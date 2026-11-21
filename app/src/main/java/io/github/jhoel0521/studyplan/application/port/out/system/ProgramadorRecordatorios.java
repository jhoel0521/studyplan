package io.github.jhoel0521.studyplan.application.port.out.system;

/**
 * Puerto de salida segregado para programar y cancelar avisos previos de sesiones en estado
 * {@code PENDIENTE}, así como limpiar las notificaciones de recordatorio activas ({@code HU-34}).
 */
public interface ProgramadorRecordatorios {

    /** Programa el aviso previo de {@code sesionId} para {@code instanteDisparoUtcMilis}. */
    void programar(long sesionId, long instanteDisparoUtcMilis);

    /** Cancela la alarma programada y retira cualquier notificación activa de {@code sesionId}. */
    void cancelar(long sesionId);

    /** Retira todas las notificaciones activas de recordatorio del sistema ({@code HU-38}). */
    void cancelarTodasNotificaciones();
}
