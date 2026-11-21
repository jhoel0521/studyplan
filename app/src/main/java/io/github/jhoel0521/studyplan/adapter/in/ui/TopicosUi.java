package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.Context;

import androidx.annotation.StringRes;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.enumeration.TopicoEventoEstudio;

/**
 * Mapeo compartido de {@link TopicoEventoEstudio} a etiquetas localizadas y construcción de chips
 * de selección única ({@code HU-22}, {@code HU-39}, {@code HU-41}, {@code HU-45}, {@code RNF-08}).
 */
public final class TopicosUi {

    private static final Map<TopicoEventoEstudio, Integer> ETIQUETAS =
            new EnumMap<>(TopicoEventoEstudio.class);

    static {
        ETIQUETAS.put(TopicoEventoEstudio.PDF_MOODLE, R.string.topico_pdf_moodle);
        ETIQUETAS.put(TopicoEventoEstudio.LIBRO_APUNTES, R.string.topico_libro_apuntes);
        ETIQUETAS.put(TopicoEventoEstudio.GRUPO_CLASE, R.string.topico_grupo_clase);
        ETIQUETAS.put(TopicoEventoEstudio.DESCANSO_COMIDA, R.string.topico_descanso_comida);
        ETIQUETAS.put(TopicoEventoEstudio.LLAMADA_MENSAJE, R.string.topico_llamada_mensaje);
        ETIQUETAS.put(TopicoEventoEstudio.OTRA_APP, R.string.topico_otra_app);
        ETIQUETAS.put(TopicoEventoEstudio.OTRO, R.string.topico_otro);
        ETIQUETAS.put(TopicoEventoEstudio.SIN_CLASIFICAR, R.string.topico_sin_clasificar);
    }

    private TopicosUi() {
    }

    @StringRes
    public static int etiquetaRes(TopicoEventoEstudio topico) {
        Integer res = ETIQUETAS.get(topico);
        return res != null ? res : R.string.topico_sin_clasificar;
    }

    public static String etiqueta(Context context, TopicoEventoEstudio topico) {
        return context.getString(etiquetaRes(topico));
    }

    public static List<TopicoEventoEstudio> topicosParaEstudioExterno() {
        List<TopicoEventoEstudio> lista = new ArrayList<>();
        for (TopicoEventoEstudio t : TopicoEventoEstudio.values()) {
            if (t.esParaEstudioExterno()) {
                lista.add(t);
            }
        }
        return lista;
    }

    public static List<TopicoEventoEstudio> topicosParaInterrupcion() {
        List<TopicoEventoEstudio> lista = new ArrayList<>();
        for (TopicoEventoEstudio t : TopicoEventoEstudio.values()) {
            if (t.esParaInterrupcion()) {
                lista.add(t);
            }
        }
        return lista;
    }

    /**
     * Pobla un {@link ChipGroup} con selección única y selecciona {@code porDefecto}.
     */
    public static void poblarChips(
            Context context,
            ChipGroup grupo,
            List<TopicoEventoEstudio> opciones,
            TopicoEventoEstudio porDefecto) {
        grupo.removeAllViews();
        grupo.setSingleSelection(true);
        grupo.setSelectionRequired(true);
        for (TopicoEventoEstudio topico : opciones) {
            Chip chip = new Chip(context);
            chip.setText(etiqueta(context, topico));
            chip.setTag(topico);
            chip.setCheckable(true);
            chip.setEnsureMinTouchTargetSize(true);
            if (topico == porDefecto) {
                chip.setChecked(true);
            }
            grupo.addView(chip);
        }
    }

    /**
     * Devuelve el tópico seleccionado actualmente en el {@link ChipGroup} o {@code SIN_CLASIFICAR}.
     */
    public static TopicoEventoEstudio obtenerSeleccionado(ChipGroup grupo) {
        int id = grupo.getCheckedChipId();
        if (id != android.view.View.NO_ID) {
            android.view.View vista = grupo.findViewById(id);
            if (vista != null && vista.getTag() instanceof TopicoEventoEstudio) {
                return (TopicoEventoEstudio) vista.getTag();
            }
        }
        return TopicoEventoEstudio.SIN_CLASIFICAR;
    }
}
