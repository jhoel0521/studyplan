package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.TecnicaEstudio;

/**
 * Puerto de entrada: iniciar una sesión de estudio sin plan previo («Estudiar ahora») sobre una
 * materia existente, la materia general {@code Estudios varios} o una materia nueva creada al vuelo
 * ({@code HU-47}, {@code RN-07}, {@code RN-32}).
 */
public interface EstudiarAhora {

    /**
     * Inicia una sesión sin plan sobre {@code materiaId} (o sobre la materia general si es
     * {@code null}).
     */
    Resultado<SesionEstudio> ejecutar(
            Long materiaId, TecnicaEstudio tecnica, PreferenciaNoMolestar preferenciaNoMolestar);

    /** Crea una nueva materia al vuelo y arranca de inmediato una sesión sin plan sobre ella. */
    Resultado<SesionEstudio> ejecutarConNuevaMateria(
            String nombreMateria,
            int colorMateria,
            TecnicaEstudio tecnica,
            PreferenciaNoMolestar preferenciaNoMolestar);
}
