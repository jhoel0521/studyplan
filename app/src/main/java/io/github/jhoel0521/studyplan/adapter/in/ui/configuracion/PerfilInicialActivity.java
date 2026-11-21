package io.github.jhoel0521.studyplan.adapter.in.ui.configuracion;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.entity.Perfil;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.ActividadBase;

/**
 * Pantalla del primer uso: pide el nombre y la meta semanal. No pide autenticación, porque sin perfil no hay datos
 * que proteger.
 *
 * <p>Implementa: HU-01 (Configurar perfil inicial).
 */
public class PerfilInicialActivity extends ActividadBase {

    private PerfilInicialViewModel modelo;
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
        modelo = viewModel(PerfilInicialViewModel.class);
        modelo.resultado().observe(this, this::onResultado);
    }

    private void enlazarVistas() {
        contenedorNombre = findViewById(R.id.contenedorNombre);
        contenedorMeta = findViewById(R.id.contenedorMeta);
        campoNombre = findViewById(R.id.campoNombre);
        campoMeta = findViewById(R.id.campoMeta);
        MaterialButton botonGuardar = findViewById(R.id.botonGuardar);
        botonGuardar.setOnClickListener(this::onGuardarClick);
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
            Intent inicio = new Intent(this, MainActivity.class);
            inicio.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(inicio);
            finish();
            return;
        }
        if (resultado.error() == ErrorValidacion.NOMBRE_VACIO) {
            contenedorNombre.setError(getString(R.string.error_nombre_vacio));
        } else {
            contenedorMeta.setError(getString(R.string.error_meta_fuera_de_rango));
        }
    }

    private static String textoDe(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString();
    }
}
