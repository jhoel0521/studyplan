package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.Objects;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;

/**
 * Selector de materia compartido entre «Estudiar ahora» ({@code HU-47}) y «Cambiar de materia»
 * ({@code HU-48}): muestra materias activas, «Estudios varios», técnica y creación rápida.
 */
final class SelectorEstudioBottomSheet {

    interface Listener {
        void alElegirMateria(@Nullable Long materiaId, @Nullable Materia materia, TecnicaEstudio tecnica);

        void alCrearNuevaMateria(String nombre, int color, TecnicaEstudio tecnica);
    }

    private SelectorEstudioBottomSheet() {
    }

    static BottomSheetDialog mostrar(
            Context contexto,
            int tituloRes,
            List<Materia> materias,
            TecnicaEstudio tecnicaInicial,
            Listener listener) {
        Objects.requireNonNull(listener, "listener");
        BottomSheetDialog dialogo = new BottomSheetDialog(contexto);
        View vista = LayoutInflater.from(contexto)
                .inflate(R.layout.bottom_sheet_selector_estudio, null, false);
        dialogo.setContentView(vista);

        TextView titulo = vista.findViewById(R.id.tituloSelectorEstudio);
        titulo.setText(tituloRes);

        MaterialButton opLibre = vista.findViewById(R.id.opcionTecnicaLibre);
        MaterialButton opPomodoro = vista.findViewById(R.id.opcionTecnicaPomodoro);
        GrupoBotones.ajustarEnUnaLinea(vista.findViewById(R.id.grupoTecnicaSelector));
        if (tecnicaInicial == TecnicaEstudio.POMODORO) {
            opPomodoro.setChecked(true);
        } else {
            opLibre.setChecked(true);
        }

        LinearLayout lista = vista.findViewById(R.id.contenedorListaMateriasSelector);
        LayoutInflater inflador = LayoutInflater.from(contexto);
        MaterialButton btnComenzarSelector = vista.findViewById(R.id.botonComenzarSelector);
        // Acción de la fila elegida: tocar una fila solo la marca; «Comenzar» la ejecuta (HU-47, decisión 20).
        Runnable[] eleccion = {null};
        boolean incluyoGeneral = false;
        if (materias != null) {
            for (Materia m : materias) {
                if (m.esGeneral()) {
                    incluyoGeneral = true;
                }
                agregarFilaMateria(inflador, lista, m.esGeneral()
                        ? contexto.getString(R.string.materia_general_nombre)
                        : m.nombre(), m.color(), btnComenzarSelector, eleccion, () -> {
                    TecnicaEstudio tecnica = opPomodoro.isChecked()
                            ? TecnicaEstudio.POMODORO : TecnicaEstudio.LIBRE;
                    dialogo.dismiss();
                    listener.alElegirMateria(m.id(), m, tecnica);
                });
            }
        }
        if (!incluyoGeneral) {
            int colorGeneral = ContextCompat.getColor(contexto, R.color.materia_azul);
            agregarFilaMateria(
                    inflador,
                    lista,
                    contexto.getString(R.string.materia_general_nombre),
                    colorGeneral,
                    btnComenzarSelector,
                    eleccion,
                    () -> {
                        TecnicaEstudio tecnica = opPomodoro.isChecked()
                                ? TecnicaEstudio.POMODORO : TecnicaEstudio.LIBRE;
                        dialogo.dismiss();
                        listener.alElegirMateria(null, null, tecnica);
                    });
        }
        btnComenzarSelector.setOnClickListener(v -> {
            if (eleccion[0] != null) {
                eleccion[0].run();
            }
        });

        MaterialButton btnNueva = vista.findViewById(R.id.botonMostrarNuevaMateria);
        View panelNueva = vista.findViewById(R.id.panelNuevaMateriaSelector);
        TextInputLayout contNombre = vista.findViewById(R.id.contenedorNombreNuevaMateria);
        TextInputEditText campoNombre = vista.findViewById(R.id.campoNombreNuevaMateria);
        MaterialButton btnComenzar = vista.findViewById(R.id.botonComenzarNuevaMateria);

        btnNueva.setOnClickListener(v -> {
            panelNueva.setVisibility(View.VISIBLE);
            btnNueva.setVisibility(View.GONE);
            // El panel trae su propio «Comenzar»: no se muestran dos acciones primarias a la vez.
            btnComenzarSelector.setVisibility(View.GONE);
            campoNombre.requestFocus();
        });

        int colorPorDefecto = ContextCompat.getColor(contexto, R.color.materia_azul);
        btnComenzar.setOnClickListener(v -> {
            String nombre = campoNombre.getText() == null ? "" : campoNombre.getText().toString().trim();
            if (nombre.isEmpty()) {
                contNombre.setError(contexto.getString(R.string.error_materia_nombre_vacio));
                return;
            }
            contNombre.setError(null);
            TecnicaEstudio tecnica = opPomodoro.isChecked()
                    ? TecnicaEstudio.POMODORO : TecnicaEstudio.LIBRE;
            dialogo.dismiss();
            listener.alCrearNuevaMateria(nombre, colorPorDefecto, tecnica);
        });

        dialogo.show();
        return dialogo;
    }

    private static void agregarFilaMateria(
            LayoutInflater inflador,
            LinearLayout contenedor,
            String nombreVisible,
            int color,
            MaterialButton botonComenzar,
            Runnable[] eleccion,
            Runnable alComenzar) {
        View fila = inflador.inflate(R.layout.item_selector_materia, contenedor, false);
        ImageView icono = fila.findViewById(R.id.colorSelectorMateria);
        TextView texto = fila.findViewById(R.id.nombreSelectorMateria);
        AcentoMateria.punto(icono, color);
        texto.setText(nombreVisible);
        ViewCompat.setAccessibilityDelegate(fila, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(
                    @NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(RadioButton.class.getName());
                info.setCheckable(true);
                info.setChecked(host.isActivated());
            }
        });
        fila.setOnClickListener(v -> {
            for (int i = 0; i < contenedor.getChildCount(); i++) {
                marcar(contenedor.getChildAt(i), false);
            }
            marcar(fila, true);
            eleccion[0] = alComenzar;
            botonComenzar.setEnabled(true);
        });
        contenedor.addView(fila);
    }

    private static void marcar(View fila, boolean elegida) {
        fila.setActivated(elegida);
        RadioButton radio = fila.findViewById(R.id.radioSelectorMateria);
        if (radio != null) {
            radio.setChecked(elegida);
        }
    }
}
