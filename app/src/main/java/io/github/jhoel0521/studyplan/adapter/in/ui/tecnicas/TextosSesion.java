package io.github.jhoel0521.studyplan.adapter.in.ui.tecnicas;

import android.content.Context;

import java.util.Locale;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.enumeration.FasePomodoro;

/**
 * Textos de la sesión En curso que comparten la pantalla y la notificación (HU-23, HU-49).
 */
final class TextosSesion {

    private TextosSesion() {
    }

    /** «Trabajo · Ciclo 2/4», «Descanso corto · Ciclo 2/4», etc. */
    static String fase(Context contexto, FasePomodoro fase, int ciclo, int objetivo) {
        switch (fase) {
            case DESCANSO_CORTO:
                return contexto.getString(R.string.sesion_en_curso_fase_descanso_corto, ciclo, objetivo);
            case DESCANSO_LARGO:
                return contexto.getString(R.string.sesion_en_curso_fase_descanso_largo, ciclo, objetivo);
            case TIEMPO_EXTRA_LIBRE:
                return contexto.getString(R.string.sesion_en_curso_fase_extra);
            case TRABAJO:
            default:
                return contexto.getString(R.string.sesion_en_curso_fase_trabajo, ciclo, objetivo);
        }
    }

    /** Formato {@code hh:mm:ss}. */
    static String hms(long milis) {
        long segTotal = Math.max(0L, milis / 1000L);
        long h = segTotal / 3600L;
        long m = (segTotal % 3600L) / 60L;
        long s = segTotal % 60L;
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s);
    }
}
