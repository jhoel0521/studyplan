package io.github.jhoel0521.studyplan.adapter.in.ui.configuracion;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.entity.Perfil;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.ActividadBase;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.ProcesoAutorizacionDrive;

/**
 * Pantalla del primer uso: pide el nombre y la meta semanal, o permite restaurar una copia de Google Drive en un
 * teléfono nuevo. No pide autenticación, porque sin perfil no hay datos que proteger.
 *
 * <p>Implementa: HU-01 (Configurar perfil inicial) y HU-37 (Restaurar desde Drive en un teléfono nuevo).
 */
public class PerfilInicialActivity extends ActividadBase implements ProcesoAutorizacionDrive.Escucha {

    private PerfilInicialViewModel modelo;
    private RespaldoViewModel modeloRespaldo;
    private ProcesoAutorizacionDrive autorizacion;
    private EditText campoDialogo;
    private MaterialButton botonRestaurar;
    private TextInputLayout contenedorNombre;
    private TextInputLayout contenedorMeta;
    private TextInputEditText campoNombre;
    private TextInputEditText campoMeta;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_inicial);
        aplicarInsets();
        enlazarVistas();
        autorizacion = new ProcesoAutorizacionDrive(this, this);
        modelo = viewModel(PerfilInicialViewModel.class);
        modelo.resultado().observe(this, this::onResultado);
        modeloRespaldo = viewModel(RespaldoViewModel.class);
        modeloRespaldo.ocupado().observe(this, this::onOcupado);
        modeloRespaldo.evento().observe(this, this::onEventoRespaldo);
    }

    private void enlazarVistas() {
        contenedorNombre = findViewById(R.id.contenedorNombre);
        contenedorMeta = findViewById(R.id.contenedorMeta);
        campoNombre = findViewById(R.id.campoNombre);
        campoMeta = findViewById(R.id.campoMeta);
        MaterialButton botonGuardar = findViewById(R.id.botonGuardar);
        botonGuardar.setOnClickListener(this::onGuardarClick);
        botonRestaurar = findViewById(R.id.botonRestaurarDrive);
        botonRestaurar.setOnClickListener(this::onRestaurarClick);
    }

    private void onGuardarClick(View vista) {
        contenedorNombre.setError(null);
        contenedorMeta.setError(null);
        modelo.guardar(textoDe(campoNombre), textoDe(campoMeta));
    }

    private void onResultado(@Nullable Resultado<Perfil> resultado) {
        if (resultado == null) {
            return;
        }
        modelo.consumirResultado();
        if (resultado.esValido()) {
            irAInicio();
            return;
        }
        if (resultado.error() == ErrorValidacion.NOMBRE_VACIO) {
            contenedorNombre.setError(getString(R.string.error_nombre_vacio));
        } else {
            contenedorMeta.setError(getString(R.string.error_meta_fuera_de_rango));
        }
    }

    // ------------------------------------------------------------------------------------------ restaurar (HU-37)

    private void onRestaurarClick(View vista) {
        autorizacion.iniciar();
    }

    @Override
    public void alAutorizar(String correo) {
        modeloRespaldo.restaurarConCuentaNueva(correo);
    }

    @Override
    public void alFallar() {
        Toast.makeText(this, R.string.respaldo_err_autorizacion_fallida, Toast.LENGTH_LONG).show();
    }

    private void onOcupado(@Nullable Boolean ocupado) {
        botonRestaurar.setEnabled(!Boolean.TRUE.equals(ocupado));
    }

    private void onEventoRespaldo(@Nullable RespaldoViewModel.Evento evento) {
        if (evento == null) {
            return;
        }
        modeloRespaldo.consumirEvento();
        if (evento.tipo == RespaldoViewModel.Tipo.RESTAURADO) {
            Toast.makeText(this, R.string.respaldo_ok_restaurado, Toast.LENGTH_LONG).show();
            irAInicio();
        } else if (evento.resultado == ResultadoRespaldo.CONTRASENA_REQUERIDA
                || evento.resultado == ResultadoRespaldo.CONTRASENA_INCORRECTA) {
            pedirContrasena(evento.resultado);
        } else {
            Toast.makeText(this, RespaldoActivity.mensajeDe(evento.resultado), Toast.LENGTH_LONG).show();
        }
    }

    private void pedirContrasena(ResultadoRespaldo motivo) {
        campoDialogo = new EditText(this);
        campoDialogo.setHint(R.string.respaldo_contrasena_hint);
        campoDialogo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        campoDialogo.setMinHeight(getResources().getDimensionPixelSize(R.dimen.button_height));
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.respaldo_restaurar_titulo)
                .setMessage(motivo == ResultadoRespaldo.CONTRASENA_INCORRECTA
                        ? R.string.respaldo_err_contrasena_incorrecta : R.string.respaldo_pedir_contrasena)
                .setView(campoDialogo)
                .setPositiveButton(R.string.respaldo_restaurar_continuar, this::onContrasenaEscrita)
                .setNegativeButton(R.string.cancelar, this::onContrasenaCancelada)
                .show();
    }

    private void onContrasenaEscrita(DialogInterface dialogo, int boton) {
        modeloRespaldo.restaurar(campoDialogo.getText().toString().toCharArray());
    }

    /** Sin contraseña no hay restauración: se olvida la cuenta para dejar la app como recién instalada. */
    private void onContrasenaCancelada(DialogInterface dialogo, int boton) {
        modeloRespaldo.desvincular();
    }

    private void irAInicio() {
        Intent inicio = new Intent(this, MainActivity.class);
        inicio.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(inicio);
        finish();
    }

    private static String textoDe(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString();
    }
}
