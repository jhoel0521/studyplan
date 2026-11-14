package io.github.jhoel0521.studyplan.adapter.in.ui;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
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
final class GrupoBotones {

    private GrupoBotones() {
    }

    /**
     * Da forma a los botones visibles, en el orden en que se pasan.
     *
     * @param visibles botones que se muestran en el grupo, de izquierda a derecha
     */
    static void unir(@NonNull MaterialButton... visibles) {
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
}
