package io.github.jhoel0521.studyplan.adapter.in.ui.tecnicas;

import android.app.Activity;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.adapter.out.system.BloqueoPantallaSistema;
import io.github.jhoel0521.studyplan.adapter.out.system.PreferenciasBloqueoPantalla;

/**
 * Flujo del candado de la sesión: confirmación con «No volver a preguntar», explicación y solicitud del
 * permiso de administrador del dispositivo la primera vez, y bloqueo de la pantalla (HU-50, RNF-18). Si el
 * estudiante rechaza el permiso no se bloquea y la sesión sigue igual. Debe crearse en {@code onCreate},
 * porque registra un lanzador de resultados.
 */
final class FlujoBloqueoPantalla {

    private final AppCompatActivity actividad;
    private final BloqueoPantallaSistema sistema;
    private final PreferenciasBloqueoPantalla preferencias;
    private final ActivityResultLauncher<Intent> lanzadorPermiso;

    FlujoBloqueoPantalla(AppCompatActivity actividad) {
        this.actividad = actividad;
        this.sistema = new BloqueoPantallaSistema(actividad);
        this.preferencias = new PreferenciasBloqueoPantalla(actividad);
        this.lanzadorPermiso = actividad.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                resultado -> alVolverDelPermiso(resultado.getResultCode()));
    }

    /** Se llama al tocar el candado. */
    void iniciar() {
        if (preferencias.noVolverAPreguntar()) {
            continuar();
        } else {
            mostrarConfirmacion();
        }
    }

    private void mostrarConfirmacion() {
        LinearLayout contenido = new LinearLayout(actividad);
        contenido.setOrientation(LinearLayout.VERTICAL);
        int margen = (int) (24 * actividad.getResources().getDisplayMetrics().density);
        contenido.setPadding(margen, margen / 2, margen, 0);

        TextView mensaje = new TextView(actividad);
        mensaje.setText(R.string.bloqueo_confirmar_mensaje);
        mensaje.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        contenido.addView(mensaje, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        MaterialCheckBox casilla = new MaterialCheckBox(actividad);
        casilla.setText(R.string.bloqueo_no_volver_a_preguntar);
        contenido.addView(casilla, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        new MaterialAlertDialogBuilder(actividad)
                .setTitle(R.string.bloqueo_confirmar_titulo)
                .setView(contenido)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.bloqueo_confirmar_bloquear, (dialogo, boton) -> {
                    if (casilla.isChecked()) {
                        preferencias.guardarNoVolverAPreguntar(true);
                    }
                    continuar();
                })
                .show();
    }

    private void continuar() {
        if (sistema.estaActivo()) {
            sistema.bloquear();
        } else {
            explicarPermiso();
        }
    }

    private void explicarPermiso() {
        String explicacion = actividad.getString(R.string.bloqueo_pantalla_explicacion);
        new MaterialAlertDialogBuilder(actividad)
                .setTitle(R.string.bloqueo_permiso_titulo)
                .setMessage(explicacion)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.bloqueo_permiso_continuar, (dialogo, boton) ->
                        lanzadorPermiso.launch(sistema.intentSolicitarPermiso(explicacion)))
                .show();
    }

    private void alVolverDelPermiso(int codigoResultado) {
        if (codigoResultado == Activity.RESULT_OK && sistema.estaActivo()) {
            sistema.bloquear();
        }
    }
}
