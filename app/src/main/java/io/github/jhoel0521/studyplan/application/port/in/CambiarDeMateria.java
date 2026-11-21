package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;

/**
 * Puerto de entrada: cerrar la sesión en curso y arrancar de inmediato otra sesión sobre otra
 * materia sin reiniciar ni perder el tiempo ya estudiado ({@code HU-48}).
 */
public interface CambiarDeMateria {

    /**
     * Cambia a {@code nuevaMateriaId} (o a la materia general si es {@code null}). Si
     * {@code tecnicaNueva} es {@code null}, conserva la técnica de la sesión actual.
     */
    Resultado<SesionEstudio> ejecutar(
            long sesionActualId, Long nuevaMateriaId, TecnicaEstudio tecnicaNueva);

    /** Crea una nueva materia al vuelo y cambia la sesión en curso hacia ella. */
    Resultado<SesionEstudio> ejecutarConNuevaMateria(
            long sesionActualId,
            String nombreMateria,
            int colorMateria,
            TecnicaEstudio tecnicaNueva);
}
