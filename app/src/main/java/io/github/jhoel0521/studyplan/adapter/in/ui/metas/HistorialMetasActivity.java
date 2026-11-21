package io.github.jhoel0521.studyplan.adapter.in.ui.metas;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.readmodel.ProgresoMeta;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.ActividadProtegida;

/**
 * Pantalla de historial semanal de metas ({@code HU-27}, {@code RN-31}, {@code RN-33}).
 */
public class HistorialMetasActivity extends ActividadProtegida {

    private HistorialMetasViewModel modelo;
    private HistorialMetasAdapter adaptador;
    private TextView estadoVacio;
    private RecyclerView lista;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial_metas);
        aplicarInsets();

        MaterialToolbar barra = findViewById(R.id.barraSuperior);
        barra.setNavigationOnClickListener(this::onVolverClick);
        estadoVacio = findViewById(R.id.estadoVacioHistorial);
        lista = findViewById(R.id.listaHistorialMetas);
        adaptador = new HistorialMetasAdapter();
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adaptador);

        modelo = viewModel(HistorialMetasViewModel.class);
        modelo.historial().observe(this, this::onHistorial);
    }

    @Override
    protected void alEstarListo() {
        modelo.cargar();
    }

    private void onHistorial(@Nullable List<ProgresoMeta> items) {
        boolean vacio = items == null || items.isEmpty();
        estadoVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        lista.setVisibility(vacio ? View.GONE : View.VISIBLE);
        adaptador.submitList(items);
    }

    private void onVolverClick(View vista) {
        finish();
    }
}
