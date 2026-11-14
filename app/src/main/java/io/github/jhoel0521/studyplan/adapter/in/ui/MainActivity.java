package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import android.widget.TextView;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.ResumenInicio;

/**
 * Pantalla de Inicio y punto de entrada de la app: saludo, progreso de la semana (por ahora {@code 0 / meta}) y
 * acceso a Ajustes. La protección (primer uso y bloqueo) la resuelve {@link ActividadProtegida}.
 *
 * <p>Implementa: HU-02 (versión parcial), HU-01 y HU-04.
 */
public class MainActivity extends ActividadProtegida {

    private InicioViewModel modelo;
    private TextView saludo;
    private TextView textoProgreso;
    private LinearProgressIndicator barraProgreso;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        aplicarInsets();
        enlazarVistas();
        enlazarBarraInferior(DestinoPrincipal.INICIO);
        modelo = viewModel(InicioViewModel.class);
        modelo.resumen().observe(this, this::onResumen);
    }

    private void enlazarVistas() {
        MaterialToolbar barraSuperior = findViewById(R.id.barraSuperior);
        saludo = findViewById(R.id.saludo);
        textoProgreso = findViewById(R.id.textoProgreso);
        barraProgreso = findViewById(R.id.barraProgreso);
        barraSuperior.setOnMenuItemClickListener(this::onAjustesClick);
    }

    @Override
    protected void alEstarListo() {
        modelo.cargar();
    }

    private void onResumen(ResumenInicio resumen) {
        saludo.setText(getString(R.string.inicio_saludo, resumen.nombre()));
        textoProgreso.setText(getString(R.string.inicio_progreso,
                FormatoHoras.texto(resumen.horasRealizadas()), FormatoHoras.texto(resumen.metaSemanalHoras())));
        barraProgreso.setProgress(resumen.porcentajeProgreso());
    }

    private boolean onAjustesClick(MenuItem opcion) {
        if (opcion.getItemId() == R.id.accion_ajustes) {
            startActivity(new Intent(this, AjustesActivity.class));
            return true;
        }
        return false;
    }
}
