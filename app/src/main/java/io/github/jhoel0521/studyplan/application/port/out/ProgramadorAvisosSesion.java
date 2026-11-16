package io.github.jhoel0521.studyplan.application.port.out;

/**
 * Puerto de salida para programar y cancelar avisos puntuales de hitos durante una sesión en curso
 * (HU-23, HU-42, HU-43, RN-25, RN-27).
 */
public interface ProgramadorAvisosSesion {

    enum TipoHito {
        CAMBIO_FASE_POMODORO,
        FIN_PLANIFICADO,
        INVASION_SESION
    }

    void programarSiguienteHito(
            long sesionId, TipoHito tipo, long instanteDisparoMilis, String nombreMateria);

    void cancelarAvisos(long sesionId);
}
