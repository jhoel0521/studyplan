package io.github.jhoel0521.studyplan.adapter.in.ui;

import androidx.annotation.StringRes;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.ErrorValidacion;

/**
 * Traduce los códigos de error del dominio al texto de {@code strings.xml} (RNF-08: el dominio no devuelve textos).
 *
 * <p>Implementa: HU-06 a HU-08 y HU-46 (mensajes de las materias, RN-02 y RN-03).
 */
final class MensajesDeError {

    private MensajesDeError() {
    }

    @StringRes
    static int paraMateria(ErrorValidacion error) {
        switch (error) {
            case NOMBRE_VACIO:
                return R.string.error_materia_nombre_vacio;
            case NOMBRE_MUY_LARGO:
                return R.string.error_materia_nombre_largo;
            case NOMBRE_DUPLICADO:
                return R.string.error_materia_duplicada;
            case MATERIA_CON_SESIONES:
                return R.string.error_materia_con_sesiones;
            case MATERIA_YA_ACTIVA:
                return R.string.error_materia_ya_activa;
            default:
                return R.string.error_materia_no_encontrada;
        }
    }
}
