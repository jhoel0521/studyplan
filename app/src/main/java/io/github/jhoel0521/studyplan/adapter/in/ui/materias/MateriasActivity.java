package io.github.jhoel0521.studyplan.adapter.in.ui.materias;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.ActividadProtegida;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.DestinoPrincipal;

/**
 * Pestaña Materias: lista alfabética de las materias activas con color, nombre, docente y horas, sección plegable de
 * archivadas y botón «+» para registrar una. Tocar una materia abre el formulario de edición.
 *
 * <p>Implementa: HU-09 (Lista y Lista vacía) y HU-08 (archivadas); HU-06 y HU-07 desde el formulario.
 */
public class MateriasActivity extends ActividadProtegida {

    private ListaMateriasViewModel modelo;
    private MateriasAdapter adaptador;
    private TextView textoVacio;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_materias);
        aplicarInsets();
        enlazarVistas();
        enlazarBarraInferior(DestinoPrincipal.MATERIAS);
        modelo = viewModel(ListaMateriasViewModel.class);
        modelo.filas().observe(this, this::onFilas);
        modelo.vacia().observe(this, this::onVacia);
    }

    private void enlazarVistas() {
        textoVacio = findViewById(R.id.textoMateriasVacio);
        adaptador = new MateriasAdapter(this::onMateriaClick, this::onArchivadasClick);
        RecyclerView lista = findViewById(R.id.listaMaterias);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adaptador);
        FloatingActionButton agregar = findViewById(R.id.botonAgregarMateria);
        agregar.setOnClickListener(this::onAgregarClick);
    }

    @Override
    protected void alEstarListo() {
        modelo.cargar();
    }

    private void onFilas(List<FilaMateria> filas) {
        adaptador.submitList(filas);
    }

    private void onVacia(Boolean vacia) {
        textoVacio.setVisibility(Boolean.TRUE.equals(vacia) ? View.VISIBLE : View.GONE);
    }

    private void onAgregarClick(View vista) {
        startActivity(new Intent(this, MateriaFormularioActivity.class));
    }

    private void onMateriaClick(Materia materia) {
        Intent intento = new Intent(this, MateriaFormularioActivity.class);
        intento.putExtra(MateriaFormularioActivity.EXTRA_ID, (long) materia.id());
        startActivity(intento);
    }

    private void onArchivadasClick() {
        modelo.alternarArchivadas();
    }
}
