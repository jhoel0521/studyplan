package io.github.jhoel0521.studyplan.adapter.in.ui.comun;

import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.widget.TextViewCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.shape.AbsoluteCornerSize;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.CornerSize;
import com.google.android.material.shape.RelativeCornerSize;
import com.google.android.material.shape.ShapeAppearanceModel;

/**
 * Une botones en un grupo, como el {@code btn-group} de Bootstrap: solo el primero redondea sus esquinas izquierdas y
 * solo el último las derechas; si queda uno solo, se redondea por completo.
 *
 * <p>Implementa: HU-06 a HU-08 y HU-46 (presentación de las acciones del formulario de materia).
 */
public final class GrupoBotones {

    private GrupoBotones() {
    }

    /**
     * Da forma a los botones visibles, en el orden en que se pasan.
     *
     * @param visibles botones que se muestran en el grupo, de izquierda a derecha
     */
    public static void unir(@NonNull MaterialButton... visibles) {
        for (int i = 0; i < visibles.length; i++) {
            boolean primero = i == 0;
            boolean ultimo = i == visibles.length - 1;
            CornerSize izquierda = primero ? new RelativeCornerSize(0.5f) : new AbsoluteCornerSize(0);
            CornerSize derecha = ultimo ? new RelativeCornerSize(0.5f) : new AbsoluteCornerSize(0);
            ShapeAppearanceModel forma = ShapeAppearanceModel.builder()
                    .setTopLeftCorner(CornerFamily.ROUNDED, izquierda)
                    .setBottomLeftCorner(CornerFamily.ROUNDED, izquierda)
                    .setTopRightCorner(CornerFamily.ROUNDED, derecha)
                    .setBottomRightCorner(CornerFamily.ROUNDED, derecha)
                    .build();
            visibles[i].setShapeAppearanceModel(forma);
        }
    }

    /**
     * Deja cada botón de un grupo alternable en una sola línea y reduce su letra de 16 a 8 sp si la etiqueta no
     * cabe, para que todos los botones conserven la misma altura (design.md, decisión 17; HU-03, escenario «Texto
     * ampliado»). {@link MaterialButtonToggleGroup} ya fija una línea con «…» al añadir los botones, así que el
     * autoajuste se aplica desde código después de inflar el layout; «…» queda solo como último recurso.
     *
     * @param grupo grupo alternable ya inflado
     */
    public static void ajustarEnUnaLinea(@NonNull MaterialButtonToggleGroup grupo) {
        for (int i = 0; i < grupo.getChildCount(); i++) {
            View hijo = grupo.getChildAt(i);
            if (hijo instanceof MaterialButton) {
                MaterialButton boton = (MaterialButton) hijo;
                boton.setMaxLines(1);
                TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                        boton, 8, 16, 1, TypedValue.COMPLEX_UNIT_SP);
            }
        }
    }
}
