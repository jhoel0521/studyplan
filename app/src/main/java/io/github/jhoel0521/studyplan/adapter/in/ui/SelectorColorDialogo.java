package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorListener;
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar;

import java.util.function.IntConsumer;

import io.github.jhoel0521.studyplan.R;

/**
 * Modal para elegir cualquier color, como el {@code input type="color"} de HTML: un selector de matiz y saturación para
 * arrastrar, una barra de brillo, un campo hexadecimal, la vista previa y ocho colores rápidos como atajos. El color
 * resultante siempre es opaco.
 *
 * <p>Implementa: HU-06 y HU-07 (el estudiante elige el color de la materia). Usa la librería ColorPickerView solo para
 * las vistas del selector.
 */
public final class SelectorColorDialogo {

    private final Context contexto;
    private final IntConsumer alElegir;
    private int colorActual;
    private boolean sincronizando;

    private AlertDialog dialogo;
    private ColorPickerView selector;
    private ImageView vistaPrevia;
    private TextInputLayout contenedorHex;
    private TextInputEditText campoHex;
    private GridLayout atajos;

    /**
     * @param colorInicial color con el que se abre el modal
     * @param alElegir se llama con el color opaco elegido cuando el estudiante pulsa «Elegir»
     */
    public SelectorColorDialogo(Context contexto, int colorInicial, IntConsumer alElegir) {
        this.contexto = contexto;
        this.colorActual = 0xFF000000 | colorInicial;
        this.alElegir = alElegir;
    }

    /** Muestra el modal. */
    public void mostrar() {
        View vista = LayoutInflater.from(contexto).inflate(R.layout.dialogo_color, null);
        enlazarVistas(vista);
        prepararAtajos();
        dialogo = new MaterialAlertDialogBuilder(contexto)
                .setTitle(R.string.materia_color_titulo)
                .setView(vista)
                .setPositiveButton(R.string.materia_color_elegir, null)
                .setNegativeButton(R.string.cancelar, null)
                .show();
        // Se reemplaza el listener para que un hexadecimal incompleto no cierre el modal.
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(this::onElegirClick);
        selector.post(this::seleccionarColorInicial);
    }

    /** Cierra el modal si está abierto (por ejemplo, al destruirse la pantalla). */
    public void cerrar() {
        if (dialogo != null) {
            dialogo.dismiss();
            dialogo = null;
        }
    }

    private void enlazarVistas(View vista) {
        selector = vista.findViewById(R.id.selectorColor);
        BrightnessSlideBar barraBrillo = vista.findViewById(R.id.barraBrillo);
        vistaPrevia = vista.findViewById(R.id.vistaPreviaDialogo);
        contenedorHex = vista.findViewById(R.id.contenedorColorHex);
        campoHex = vista.findViewById(R.id.campoColorHex);
        atajos = vista.findViewById(R.id.atajosColores);
        selector.attachBrightnessSlider(barraBrillo);
        selector.setColorListener((ColorListener) this::onColorDelSelector);
        campoHex.addTextChangedListener(new AlCambiarHex());
    }

    private void prepararAtajos() {
        // TypedArray solo es AutoCloseable desde la API 31 y el proyecto usa minSdk 24: se cierra con recycle() en finally.
        //noinspection resource,AutoCloseableResource
        TypedArray colores = contexto.getResources().obtainTypedArray(R.array.paleta_materias);
        try {
            LayoutInflater inflador = LayoutInflater.from(contexto);
            for (int i = 0; i < colores.length(); i++) {
                int color = 0xFF000000 | colores.getColor(i, 0);
                View muestra = inflador.inflate(R.layout.item_color, atajos, false);
                ImageView circulo = muestra.findViewById(R.id.colorCirculo);
                circulo.setColorFilter(color);
                muestra.setTag(color);
                muestra.setContentDescription(contexto.getString(R.string.materia_color_n, i + 1));
                muestra.setOnClickListener(this::onAtajoClick);
                atajos.addView(muestra);
            }
        } finally {
            colores.recycle();
        }
    }

    private void seleccionarColorInicial() {
        aplicar(colorActual, true, true);
    }

    /**
     * Aplica un color a todo el modal.
     *
     * @param actualizarSelector {@code false} cuando el cambio viene de arrastrar en el propio selector
     * @param actualizarCampo {@code false} cuando el cambio viene de escribir en el campo hexadecimal
     */
    private void aplicar(int color, boolean actualizarSelector, boolean actualizarCampo) {
        colorActual = 0xFF000000 | color;
        sincronizando = true;
        vistaPrevia.setColorFilter(colorActual);
        if (actualizarCampo) {
            campoHex.setText(ColorHex.formatear(colorActual));
        }
        if (actualizarSelector) {
            try {
                selector.selectByHsvColor(colorActual);
            } catch (IllegalAccessException e) {
                // La paleta por defecto es de matiz y saturación, así que no ocurre; si ocurriera, el campo y la vista
                // previa siguen mostrando el color.
            }
        }
        contenedorHex.setError(null);
        marcarAtajo();
        sincronizando = false;
    }

    private void marcarAtajo() {
        for (int i = 0; i < atajos.getChildCount(); i++) {
            View muestra = atajos.getChildAt(i);
            boolean elegida = ((Integer) muestra.getTag()) == colorActual;
            muestra.setSelected(elegida);
            muestra.findViewById(R.id.colorCheck).setVisibility(elegida ? View.VISIBLE : View.GONE);
        }
    }

    private void onColorDelSelector(int color, boolean delUsuario) {
        if (sincronizando || !delUsuario) {
            return;
        }
        aplicar(color, false, true);
    }

    private void onAtajoClick(View muestra) {
        aplicar((Integer) muestra.getTag(), true, true);
    }

    private void onElegirClick(View vista) {
        Integer escrito = ColorHex.parsear(campoHex.getText() == null ? "" : campoHex.getText().toString());
        if (escrito == null) {
            contenedorHex.setError(contexto.getString(R.string.error_color_hex));
            return;
        }
        alElegir.accept(colorActual);
        cerrar();
    }

    /** Escribir en el campo hexadecimal mueve el selector en cuanto el texto es válido. */
    private final class AlCambiarHex implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence texto, int inicio, int cuenta, int despues) {
        }

        @Override
        public void onTextChanged(CharSequence texto, int inicio, int antes, int cuenta) {
        }

        @Override
        public void afterTextChanged(@NonNull Editable texto) {
            if (sincronizando) {
                return;
            }
            Integer color = ColorHex.parsear(texto.toString());
            if (color != null) {
                aplicar(color, true, false);
            }
        }
    }
}
