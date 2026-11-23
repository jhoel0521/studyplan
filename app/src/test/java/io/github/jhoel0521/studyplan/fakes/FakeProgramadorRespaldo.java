package io.github.jhoel0521.studyplan.fakes;

import io.github.jhoel0521.studyplan.application.port.out.system.ProgramadorRespaldo;
import io.github.jhoel0521.studyplan.domain.enumeration.FrecuenciaRespaldo;

/**
 * Programador del respaldo periódico falso: recuerda la frecuencia programada.
 *
 * <p>Apoya las pruebas de: HU-35.
 */
public final class FakeProgramadorRespaldo implements ProgramadorRespaldo {

    /** Frecuencia programada, o {@code null} si no hay tarea. */
    public FrecuenciaRespaldo programada;
    public int cancelaciones;

    @Override
    public void programar(FrecuenciaRespaldo frecuencia) {
        programada = frecuencia;
    }

    @Override
    public void cancelar() {
        programada = null;
        cancelaciones++;
    }
}
