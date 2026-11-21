package io.github.jhoel0521.studyplan.domain.state;

/**
 * Códigos persistibles de los seis estados posibles de una sesión de estudio (RN-07).
 *
 * <p>Implementa HU-10, HU-11, HU-12, HU-13, HU-14, HU-15 y RN-07.
 */
public enum CodigoEstadoSesion {
    /** Sesión planificada aún pendiente de ejecutarse o vencer. */
    PENDIENTE,
    /** Sesión actualmente en ejecución con cronómetro o Pomodoro. */
    EN_CURSO,
    /** Sesión pendiente cuya fecha venció sin completarse (RN-08). */
    NO_REALIZADA,
    /** Sesión completada en su día o al finalizar su ejecución (HU-12, RN-07). */
    COMPLETADA,
    /** Sesión registrada o completada dentro de la ventana tardía de 3 días (RN-06, RN-10). */
    COMPLETADA_TARDIA,
    /** Sesión reemplazada por otra fecha u horario conservando vínculo de origen (RN-09). */
    REPROGRAMADA
}
