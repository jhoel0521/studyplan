package io.github.jhoel0521.studyplan.adapter.in.ui;

import java.util.Locale;

/**
 * Da formato a las horas para mostrarlas: sin decimales si el valor es entero (20) y con uno si no (12,5).
 *
 * <p>Implementa: HU-02 (progreso {@code horas / meta}).
 */
final class FormatoHoras {

    private FormatoHoras() {
    }

    static String texto(double horas) {
        if (horas == Math.rint(horas)) {
            return String.format(Locale.getDefault(), "%d", (long) horas);
        }
        return String.format(Locale.getDefault(), "%.1f", horas);
    }
}
