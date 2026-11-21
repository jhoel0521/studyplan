package io.github.jhoel0521.studyplan.adapter.in.ui.materias;

/**
 * Ajusta el color libre de una materia para usarlo solo como acento (barra de 4 dp, punto de 10 dp o chip
 * al 15 %) sobre una superficie del tema, claro u oscuro.
 *
 * <p>Java puro, sin {@code android.*}: trabaja con enteros ARGB y aritmética sRGB/HSL, para probarlo en la JVM.
 * Quien lo usa resuelve la superficie con {@code MaterialColors.getColor(vista, colorSurface)} del
 * {@code Context} actual, así el resultado sigue válido al rotar o al cambiar de tema.
 *
 * <p>Implementa: HU-03 (escenarios «Color de materia solo como acento» y «Color de materia legible»), RNF-06
 * (contraste de elementos no textuales ≥ 3:1, WCAG 1.4.11).
 */
public final class ColorMateria {

    /** Contraste mínimo de un elemento gráfico contra su fondo (WCAG 1.4.11). */
    public static final double CONTRASTE_MINIMO = 3.0;

    private static final double PASO_LUMINOSIDAD = 0.02;

    private ColorMateria() {
    }

    /**
     * Devuelve el mismo color si ya contrasta ≥ 3:1 con la superficie; si no, el mismo tono más oscuro (sobre
     * una superficie clara) o más claro (sobre una oscura) hasta cumplirlo. El resultado es opaco.
     */
    public static int paraSuperficie(int argb, int superficieArgb) {
        int color = opaco(argb);
        if (contraste(color, superficieArgb) >= CONTRASTE_MINIMO) {
            return color;
        }
        boolean oscurecer = luminancia(superficieArgb) > 0.5;
        double[] hsl = aHsl(color);
        while (true) {
            hsl[2] = oscurecer ? Math.max(0.0, hsl[2] - PASO_LUMINOSIDAD) : Math.min(1.0, hsl[2] + PASO_LUMINOSIDAD);
            int candidato = deHsl(hsl);
            if (contraste(candidato, superficieArgb) >= CONTRASTE_MINIMO || hsl[2] <= 0.0 || hsl[2] >= 1.0) {
                return candidato;
            }
        }
    }

    /** Relleno opaco del chip: el color de la materia con la opacidad {@code alfa} sobre la superficie. */
    public static int conAlfa(int argb, float alfa, int superficieArgb) {
        double a = Math.max(0f, Math.min(1f, alfa));
        int r = mezclar((argb >> 16) & 0xFF, (superficieArgb >> 16) & 0xFF, a);
        int g = mezclar((argb >> 8) & 0xFF, (superficieArgb >> 8) & 0xFF, a);
        int b = mezclar(argb & 0xFF, superficieArgb & 0xFF, a);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Relación de contraste WCAG 2.x entre dos colores opacos. */
    public static double contraste(int a, int b) {
        double la = luminancia(a);
        double lb = luminancia(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    /** Luminancia relativa WCAG 2.x de un color (se ignora el canal alfa). */
    public static double luminancia(int argb) {
        return 0.2126 * lineal((argb >> 16) & 0xFF)
                + 0.7152 * lineal((argb >> 8) & 0xFF)
                + 0.0722 * lineal(argb & 0xFF);
    }

    private static double lineal(int canal) {
        double c = canal / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static int mezclar(int frente, int fondo, double alfa) {
        return (int) Math.round(frente * alfa + fondo * (1 - alfa));
    }

    private static int opaco(int argb) {
        return 0xFF000000 | (argb & 0x00FFFFFF);
    }

    private static double[] aHsl(int argb) {
        double r = ((argb >> 16) & 0xFF) / 255.0;
        double g = ((argb >> 8) & 0xFF) / 255.0;
        double b = (argb & 0xFF) / 255.0;
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double l = (max + min) / 2;
        double h = 0;
        double s = 0;
        double d = max - min;
        if (d > 0) {
            s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
            if (max == r) {
                h = ((g - b) / d + (g < b ? 6 : 0)) / 6;
            } else if (max == g) {
                h = ((b - r) / d + 2) / 6;
            } else {
                h = ((r - g) / d + 4) / 6;
            }
        }
        return new double[] {h, s, l};
    }

    private static int deHsl(double[] hsl) {
        double h = hsl[0];
        double s = hsl[1];
        double l = hsl[2];
        double r;
        double g;
        double b;
        if (s == 0) {
            r = l;
            g = l;
            b = l;
        } else {
            double q = l < 0.5 ? l * (1 + s) : l + s - l * s;
            double p = 2 * l - q;
            r = tono(p, q, h + 1.0 / 3);
            g = tono(p, q, h);
            b = tono(p, q, h - 1.0 / 3);
        }
        return 0xFF000000 | (canal(r) << 16) | (canal(g) << 8) | canal(b);
    }

    private static double tono(double p, double q, double t) {
        if (t < 0) {
            t += 1;
        }
        if (t > 1) {
            t -= 1;
        }
        if (t < 1.0 / 6) {
            return p + (q - p) * 6 * t;
        }
        if (t < 1.0 / 2) {
            return q;
        }
        if (t < 2.0 / 3) {
            return p + (q - p) * (2.0 / 3 - t) * 6;
        }
        return p;
    }

    private static int canal(double valor) {
        return (int) Math.round(Math.max(0, Math.min(1, valor)) * 255);
    }
}
