package io.github.jhoel0521.studyplan.adapter.in.ui.materias;

import java.util.Locale;

/**
 * Convierte entre texto hexadecimal y color ARGB opaco. Acepta {@code RRGGBB}, {@code RGB} y las dos formas con
 * {@code #} al inicio.
 *
 * <p>Implementa: HU-06 y HU-07 (el estudiante elige cualquier color, no solo los de una lista).
 */
final class ColorHex {

    private ColorHex() {
    }

    /** @return el color opaco, o {@code null} si el texto no es un hexadecimal de 3 o 6 dígitos */
    static Integer parsear(String texto) {
        String hex = texto == null ? "" : texto.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() == 3) {
            StringBuilder ampliado = new StringBuilder();
            for (char c : hex.toCharArray()) {
                ampliado.append(c).append(c);
            }
            hex = ampliado.toString();
        }
        if (hex.length() != 6) {
            return null;
        }
        try {
            return 0xFF000000 | Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** @return los seis dígitos hexadecimales del color, sin {@code #} y en mayúsculas */
    static String formatear(int color) {
        return String.format(Locale.ROOT, "%06X", color & 0xFFFFFF);
    }
}
