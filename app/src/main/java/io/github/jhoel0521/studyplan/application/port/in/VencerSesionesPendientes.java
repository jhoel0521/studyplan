package io.github.jhoel0521.studyplan.application.port.in;

/**
 * Puerto de entrada (caso de uso): transicionar automáticamente sesiones pendientes vencidas a
 * {@code NO_REALIZADA} respetando las sesiones {@code EN_CURSO} (HU-13, RN-08, RN-29).
 */
public interface VencerSesionesPendientes {

    /** @return cantidad de sesiones vencidas en esta ejecución */
    int ejecutar();
}
