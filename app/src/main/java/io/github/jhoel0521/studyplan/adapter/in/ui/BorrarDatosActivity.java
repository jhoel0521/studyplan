package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.Idioma;
import io.github.jhoel0521.studyplan.domain.Tema;

/**
 * Pantalla de privacidad: borra todos los datos del teléfono con una doble confirmación. Al terminar restablece tema
 * e idioma y vuelve al primer uso con la pila de pantallas limpia.
 *
 * <p>Implementa: HU-38 (alcance «teléfono») y RN-21.
 */
public class BorrarDatosActivity extends ActividadProtegida {

    private BorrarDatosViewModel modelo;
    private MaterialButton botonBorrar;
    private AlertDialog dialogoAbierto;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_borrar_datos);
        aplicarInsets();
        enlazarVistas();
        modelo = viewModel(BorrarDatosViewModel.class);
        modelo.paso().observe(this, this::onPaso);
    }

    private void enlazarVistas() {
        MaterialToolbar barraSuperior = findViewById(R.id.barraSuperior);
        botonBorrar = findViewById(R.id.botonBorrarTelefono);
        barraSuperior.setNavigationOnClickListener(this::onVolverClick);
        botonBorrar.setOnClickListener(this::onBorrarClick);
    }

    @Override
    protected void onDestroy() {
        cerrarDialogo();
        super.onDestroy();
    }

    private void onVolverClick(View vista) {
        finish();
    }

    private void onBorrarClick(View vista) {
        modelo.pedirBorrado();
    }

    private void onPaso(BorrarDatosViewModel.Paso paso) {
        cerrarDialogo();
        botonBorrar.setEnabled(paso == BorrarDatosViewModel.Paso.NINGUNO);
        switch (paso) {
            case PRIMERA_CONFIRMACION:
                mostrarDialogo(R.string.borrar_primera_confirmacion, R.string.borrar_continuar,
                        this::onPrimeraConfirmada);
                break;
            case SEGUNDA_CONFIRMACION:
                mostrarDialogo(R.string.borrar_segunda_confirmacion, R.string.borrar_confirmar,
                        this::onSegundaConfirmada);
                break;
            case BORRADO:
                Intent primerUso = new Intent(this, PerfilInicialActivity.class);
                primerUso.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(primerUso);
                modelo.cancelar();
                finish();
                AplicadorApariencia.aplicarTema(Tema.SISTEMA);
                AplicadorApariencia.aplicarIdioma(Idioma.ES);
                break;
            default:
                break;
        }
    }

    private void mostrarDialogo(int mensaje, int textoConfirmar, DialogInterface.OnClickListener alConfirmar) {
        dialogoAbierto = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.borrar_titulo_dialogo)
                .setMessage(mensaje)
                .setPositiveButton(textoConfirmar, alConfirmar)
                .setNegativeButton(R.string.cancelar, this::onCancelarClick)
                .setOnCancelListener(this::onDialogoCancelado)
                .show();
    }

    private void onPrimeraConfirmada(DialogInterface dialogo, int boton) {
        modelo.confirmarPrimera();
    }

    private void onSegundaConfirmada(DialogInterface dialogo, int boton) {
        modelo.confirmarSegunda();
    }

    private void onCancelarClick(DialogInterface dialogo, int boton) {
        modelo.cancelar();
    }

    private void onDialogoCancelado(DialogInterface dialogo) {
        modelo.cancelar();
    }

    private void cerrarDialogo() {
        if (dialogoAbierto != null) {
            dialogoAbierto.setOnDismissListener(null);
            dialogoAbierto.dismiss();
            dialogoAbierto = null;
        }
    }
}
