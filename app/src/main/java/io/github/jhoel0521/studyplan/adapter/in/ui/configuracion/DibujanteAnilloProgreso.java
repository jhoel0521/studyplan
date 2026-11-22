package io.github.jhoel0521.studyplan.adapter.in.ui.configuracion;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Dibuja el anillo de progreso del widget en un {@link Bitmap}, porque {@code RemoteViews} no admite vistas propias.
 *
 * <p>Implementa: HU-05.
 */
final class DibujanteAnilloProgreso {

    /** Tope del lado en píxeles, para no pasar el límite de memoria de las {@code RemoteViews}. */
    private static final int LADO_MAXIMO_PX = 256;

    private DibujanteAnilloProgreso() {
    }

    /**
     * @param ladoPx lado del cuadrado en píxeles
     * @param porcentaje avance entre 0 y 100
     * @param colorPista color del aro de fondo
     * @param colorProgreso color del arco de avance
     */
    static Bitmap anillo(int ladoPx, int porcentaje, int colorPista, int colorProgreso) {
        int lado = Math.max(16, Math.min(ladoPx, LADO_MAXIMO_PX));
        Bitmap bitmap = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888);
        Canvas lienzo = new Canvas(bitmap);
        float grosor = lado * 0.12f;
        float margen = grosor / 2f + 1f;
        RectF caja = new RectF(margen, margen, lado - margen, lado - margen);

        Paint pincel = new Paint(Paint.ANTI_ALIAS_FLAG);
        pincel.setStyle(Paint.Style.STROKE);
        pincel.setStrokeWidth(grosor);
        pincel.setColor(colorPista);
        lienzo.drawOval(caja, pincel);

        int p = Math.max(0, Math.min(100, porcentaje));
        if (p > 0) {
            pincel.setColor(colorProgreso);
            pincel.setStrokeCap(Paint.Cap.ROUND);
            lienzo.drawArc(caja, -90f, 360f * p / 100f, false, pincel);
        }
        return bitmap;
    }
}
