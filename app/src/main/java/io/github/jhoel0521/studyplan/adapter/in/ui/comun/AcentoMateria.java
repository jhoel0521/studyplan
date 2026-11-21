package io.github.jhoel0521.studyplan.adapter.in.ui.comun;

import android.content.res.ColorStateList;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.google.android.material.color.MaterialColors;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.adapter.in.ui.materias.ColorMateria;

/**
 * Pinta el color de una materia solo como acento (barra de 4 dp, punto de 10 dp o chip al 15 %) sobre la superficie
 * del tema actual, ajustado con {@link ColorMateria} para que contraste ≥ 3:1 en claro y oscuro. La superficie se
 * resuelve del {@code Context} de la vista, así que sigue válida tras rotar o cambiar de tema.
 *
 * <p>Implementa: HU-03 (escenarios «Color de materia solo como acento» y «Color de materia legible»), RNF-06.
 */
public final class AcentoMateria {

    private AcentoMateria() {
    }

    /** Color de la materia ajustado a la superficie del tema de {@code vista}. */
    @ColorInt
    public static int ajustado(@NonNull View vista, @ColorInt int colorMateria) {
        return ColorMateria.paraSuperficie(colorMateria, superficie(vista));
    }

    /** Barra de 4 dp: su fondo ({@code fondo_barra_materia}) se tiñe con el color ajustado. */
    public static void barra(@NonNull View barra, @ColorInt int colorMateria) {
        ViewCompat.setBackgroundTintList(barra, ColorStateList.valueOf(ajustado(barra, colorMateria)));
    }

    /** Punto de 10 dp ({@code ic_circulo}) teñido con el color ajustado. */
    public static void punto(@NonNull ImageView punto, @ColorInt int colorMateria) {
        punto.setColorFilter(ajustado(punto, colorMateria));
    }

    /** Chip ({@code fondo_chip_materia}) con el color de la materia al 15 % sobre la superficie; el texto sigue en colorOnSurface. */
    public static void chip(@NonNull View chip, @ColorInt int colorMateria) {
        float alfa = ResourcesCompat.getFloat(chip.getResources(), R.dimen.chip_materia_alpha);
        int relleno = ColorMateria.conAlfa(colorMateria, alfa, superficie(chip));
        ViewCompat.setBackgroundTintList(chip, ColorStateList.valueOf(relleno));
    }

    /** Anuncia una tarjeta clicable como botón a TalkBack (sin {@code accessibilityClassName}, que exige API 28). */
    public static void comoBoton(@NonNull View vista) {
        ViewCompat.setAccessibilityDelegate(vista, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(
                    @NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(Button.class.getName());
            }
        });
    }

    @ColorInt
    private static int superficie(@NonNull View vista) {
        return MaterialColors.getColor(vista, com.google.android.material.R.attr.colorSurface);
    }
}
