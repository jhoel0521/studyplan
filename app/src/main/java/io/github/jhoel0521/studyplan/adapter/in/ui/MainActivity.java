package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.DetalleSesionInicio;
import io.github.jhoel0521.studyplan.domain.ResumenInicio;

/**
 * Pantalla de Inicio y punto de entrada de la app: saludo, progreso real de la semana, acción
 * rápida para nueva sesión, tarjeta de próxima sesión pendiente y lista de sesiones del día actual.
 * La protección (primer uso y bloqueo) la resuelve {@link ActividadProtegida}.
 *
 * <p>Implementa: HU-02, HU-01, HU-04 y RNF-06.
 */
public class MainActivity extends ActividadProtegida {

    private InicioViewModel modelo;
    private TextView saludo;
    private TextView textoProgreso;
    private LinearProgressIndicator barraProgreso;
    private MaterialButton botonNuevaSesion;
    private MaterialCardView tarjetaProximaSesion;
    private ImageView colorMateriaProxima;
    private TextView nombreMateriaProxima;
    private TextView horarioProxima;
    private ImageView iconoEstadoProxima;
    private TextView etiquetaEstadoProxima;
    private TextView estadoVacio;
    private RecyclerView listaSesionesHoy;
    private SesionInicioAdapter adaptadorSesionesHoy;

    @Nullable
    private ResumenInicio ultimoResumen;
    private boolean hayMateriasActivas;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        aplicarInsets();
        enlazarVistas();
        enlazarBarraInferior(DestinoPrincipal.INICIO);
        modelo = viewModel(InicioViewModel.class);
        modelo.resumen().observe(this, this::onResumen);
        modelo.hayMateriasActivas().observe(this, this::onHayMateriasActivas);
    }

    private void enlazarVistas() {
        MaterialToolbar barraSuperior = findViewById(R.id.barraSuperior);
        saludo = findViewById(R.id.saludo);
        textoProgreso = findViewById(R.id.textoProgreso);
        barraProgreso = findViewById(R.id.barraProgreso);
        botonNuevaSesion = findViewById(R.id.botonNuevaSesion);
        tarjetaProximaSesion = findViewById(R.id.tarjetaProximaSesion);
        colorMateriaProxima = findViewById(R.id.colorMateriaProxima);
        nombreMateriaProxima = findViewById(R.id.nombreMateriaProxima);
        horarioProxima = findViewById(R.id.horarioProxima);
        iconoEstadoProxima = findViewById(R.id.iconoEstadoProxima);
        etiquetaEstadoProxima = findViewById(R.id.etiquetaEstadoProxima);
        estadoVacio = findViewById(R.id.estadoVacio);
        listaSesionesHoy = findViewById(R.id.listaSesionesHoy);

        adaptadorSesionesHoy = new SesionInicioAdapter(this::onSesionHoyClick);
        listaSesionesHoy.setLayoutManager(new LinearLayoutManager(this));
        listaSesionesHoy.setAdapter(adaptadorSesionesHoy);

        barraSuperior.setOnMenuItemClickListener(this::onAjustesClick);
        botonNuevaSesion.setOnClickListener(this::onNuevaSesionClick);
        tarjetaProximaSesion.setOnClickListener(this::onProximaSesionClick);
    }

    @Override
    protected void alEstarListo() {
        modelo.cargar();
    }

    private void onHayMateriasActivas(@Nullable Boolean activas) {
        hayMateriasActivas = Boolean.TRUE.equals(activas);
    }

    private void onResumen(ResumenInicio resumen) {
        ultimoResumen = resumen;
        saludo.setText(getString(R.string.inicio_saludo, resumen.nombre()));
        textoProgreso.setText(getString(R.string.inicio_progreso,
                FormatoHoras.texto(resumen.horasRealizadas()), FormatoHoras.texto(resumen.metaSemanalHoras())));
        barraProgreso.setProgress(resumen.porcentajeProgreso());

        DetalleSesionInicio proxima = resumen.proximaSesion();
        if (proxima != null) {
            colorMateriaProxima.setColorFilter(proxima.colorMateria());
            nombreMateriaProxima.setText(proxima.nombreMateria());
            horarioProxima.setText(FormatoSesion.rangoYDuracion(this, proxima, true));
            EstadoSesionVisual.aplicar(proxima.estado(), iconoEstadoProxima, etiquetaEstadoProxima);
            tarjetaProximaSesion.setVisibility(View.VISIBLE);
        } else {
            tarjetaProximaSesion.setVisibility(View.GONE);
        }

        boolean sinSesionesHoy = resumen.sesionesDeHoy().isEmpty();
        estadoVacio.setVisibility(sinSesionesHoy ? View.VISIBLE : View.GONE);
        listaSesionesHoy.setVisibility(sinSesionesHoy ? View.GONE : View.VISIBLE);
        adaptadorSesionesHoy.submitList(resumen.sesionesDeHoy());
    }

    private void onNuevaSesionClick(View vista) {
        if (!hayMateriasActivas) {
            Snackbar.make(
                            findViewById(R.id.main),
                            R.string.inicio_sin_materias_aviso,
                            Snackbar.LENGTH_LONG)
                    .setAction(R.string.inicio_ir_materias, this::onIrAMateriasClick)
                    .show();
            return;
        }
        Intent intento = new Intent(this, SesionFormularioActivity.class);
        intento.putExtra(SesionFormularioActivity.EXTRA_MODO, SesionFormularioActivity.MODO_PLANIFICAR);
        if (ultimoResumen != null && ultimoResumen.hoy() != null) {
            intento.putExtra(SesionFormularioActivity.EXTRA_FECHA, ultimoResumen.hoy().iso());
        }
        startActivity(intento);
    }

    private void onIrAMateriasClick(View vista) {
        startActivity(new Intent(this, MateriasActivity.class));
    }

    private void onProximaSesionClick(View vista) {
        if (ultimoResumen != null && ultimoResumen.proximaSesion() != null) {
            abrirDetalleSesion(ultimoResumen.proximaSesion().sesionId());
        }
    }

    private void onSesionHoyClick(DetalleSesionInicio sesion) {
        abrirDetalleSesion(sesion.sesionId());
    }

    private void abrirDetalleSesion(long sesionId) {
        Intent intento = new Intent(this, SesionDetalleActivity.class);
        intento.putExtra(SesionDetalleActivity.EXTRA_SESION_ID, sesionId);
        startActivity(intento);
    }

    private boolean onAjustesClick(MenuItem opcion) {
        if (opcion.getItemId() == R.id.accion_ajustes) {
            startActivity(new Intent(this, AjustesActivity.class));
            return true;
        }
        return false;
    }
}
