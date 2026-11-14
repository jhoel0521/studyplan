package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.widget.ImageViewCompat;

import com.google.android.material.color.MaterialColors;

import java.util.EnumMap;
import java.util.Map;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.CodigoEstadoSesion;

/**
 * Mapeo visual uniforme de cada {@link CodigoEstadoSesion} a su icono, etiqueta textual y color
 * semántico de tema Material 3, cumpliendo RNF-06 (ningún estado depende únicamente del color).
 *
 * <p>Implementa: HU-02, HU-10 a HU-15 y RNF-06.
 */
final class EstadoSesionVisual {

    private static final class Descriptor {
        @DrawableRes final int icono;
        @StringRes final int etiqueta;
        @AttrRes final int atributoColor;

        Descriptor(@DrawableRes int icono, @StringRes int etiqueta, @AttrRes int atributoColor) {
            this.icono = icono;
            this.etiqueta = etiqueta;
            this.atributoColor = atributoColor;
        }
    }

    private static final Map<CodigoEstadoSesion, Descriptor> DESCRIPTORES =
            new EnumMap<>(CodigoEstadoSesion.class);

    static {
        DESCRIPTORES.put(CodigoEstadoSesion.PENDIENTE, new Descriptor(
                R.drawable.ic_estado_pendiente,
                R.string.estado_sesion_pendiente,
                com.google.android.material.R.attr.colorPrimary));
        DESCRIPTORES.put(CodigoEstadoSesion.EN_CURSO, new Descriptor(
                R.drawable.ic_estado_en_curso,
                R.string.estado_sesion_en_curso,
                com.google.android.material.R.attr.colorTertiary));
        DESCRIPTORES.put(CodigoEstadoSesion.NO_REALIZADA, new Descriptor(
                R.drawable.ic_estado_no_realizada,
                R.string.estado_sesion_no_realizada,
                com.google.android.material.R.attr.colorError));
        DESCRIPTORES.put(CodigoEstadoSesion.COMPLETADA, new Descriptor(
                R.drawable.ic_estado_completada,
                R.string.estado_sesion_completada,
                com.google.android.material.R.attr.colorPrimary));
        DESCRIPTORES.put(CodigoEstadoSesion.COMPLETADA_TARDIA, new Descriptor(
                R.drawable.ic_estado_completada_tardia,
                R.string.estado_sesion_completada_tardia,
                com.google.android.material.R.attr.colorSecondary));
        DESCRIPTORES.put(CodigoEstadoSesion.REPROGRAMADA, new Descriptor(
                R.drawable.ic_estado_reprogramada,
                R.string.estado_sesion_reprogramada,
                com.google.android.material.R.attr.colorOnSurfaceVariant));
    }

    private EstadoSesionVisual() {
    }

    static void aplicar(
            CodigoEstadoSesion estado, ImageView vistaIcono, TextView vistaEtiqueta) {
        Descriptor d = DESCRIPTORES.get(estado);
        if (d == null) {
            return;
        }
        Context contexto = vistaEtiqueta.getContext();
        int color = MaterialColors.getColor(vistaEtiqueta, d.atributoColor);
        vistaIcono.setImageResource(d.icono);
        ImageViewCompat.setImageTintList(vistaIcono, ColorStateList.valueOf(color));
        vistaEtiqueta.setText(contexto.getString(d.etiqueta));
        vistaEtiqueta.setTextColor(color);
    }

    @StringRes
    static int etiquetaDe(CodigoEstadoSesion estado) {
        Descriptor d = DESCRIPTORES.get(estado);
        return d == null ? R.string.estado_sesion_no_realizada : d.etiqueta;
    }
}
