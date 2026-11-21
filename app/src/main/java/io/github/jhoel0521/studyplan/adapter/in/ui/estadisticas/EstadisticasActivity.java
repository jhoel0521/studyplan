package io.github.jhoel0521.studyplan.adapter.in.ui.estadisticas;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.core.widget.TextViewCompat;

import com.google.android.material.color.MaterialColors;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.readmodel.ConcentracionPeriodo;
import io.github.jhoel0521.studyplan.domain.readmodel.DesgloseMateriaPeriodo;
import io.github.jhoel0521.studyplan.domain.readmodel.EstadisticasPeriodo;
import io.github.jhoel0521.studyplan.domain.valueobject.PeriodoEstadisticas;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.AcentoMateria;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.ActividadProtegida;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.DestinoPrincipal;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.GrupoBotones;
import io.github.jhoel0521.studyplan.adapter.in.ui.reportes.ReportesActivity;

/**
 * Pantalla de Estadísticas ({@code HU-29}, {@code HU-30}, {@code HU-31}, {@code HU-45}).
 */
public class EstadisticasActivity extends ActividadProtegida {

    private EstadisticasViewModel modelo;
    private MaterialButtonToggleGroup grupoPeriodo;
    private MaterialButtonToggleGroup grupoSubvista;
    private TextView textoRango;
    private LinearLayout contenedorResumen;
    private LinearLayout contenedorConcentracion;

    private TextView textoTotalHoras;
    private TextView textoVariacion;
    private PieChart graficoDistribucion;
    private TextView textoSinHoras;
    private EstadisticasAdapter.MateriasAdapter adaptadorMaterias;
    private TextView textoCumplimientoValor;
    private TextView textoCumplimientoDesglose;
    private TextView textoPendientesAviso;

    private TextView textoIndiceConcentracion;
    private TextView textoInterrupciones;
    private TextView textoPuntualidad;
    private TextView textoRespetoTiempo;
    private TextView textoTiempoFuera;
    private TextView textoEstudioExternoResumen;
    private EstadisticasAdapter.TopicosAdapter adaptadorEstudioExterno;
    private TextView textoSinTopicos;
    private EstadisticasAdapter.TopicosAdapter adaptadorTopicos;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_estadisticas);
        aplicarInsets();
        enlazarBarraInferior(DestinoPrincipal.ESTADISTICAS);
        enlazarVistas();

        modelo = viewModel(EstadisticasViewModel.class);
        modelo.periodo().observe(this, this::onPeriodo);
        modelo.subvista().observe(this, this::onSubvista);
        modelo.estadisticas().observe(this, this::onEstadisticas);
    }

    @Override
    protected void alEstarListo() {
        modelo.cargar();
    }

    private void enlazarVistas() {
        grupoPeriodo = findViewById(R.id.grupoPeriodoEstadisticas);
        grupoSubvista = findViewById(R.id.grupoSubvistaEstadisticas);
        GrupoBotones.ajustarEnUnaLinea(grupoPeriodo);
        GrupoBotones.ajustarEnUnaLinea(grupoSubvista);
        textoRango = findViewById(R.id.textoRangoEstadisticas);
        contenedorResumen = findViewById(R.id.contenedorResumenEstadisticas);
        contenedorConcentracion = findViewById(R.id.contenedorConcentracionEstadisticas);

        textoTotalHoras = findViewById(R.id.textoTotalHorasEstadisticas);
        textoVariacion = findViewById(R.id.textoVariacionEstadisticas);
        graficoDistribucion = findViewById(R.id.graficoDistribucionMaterias);
        textoSinHoras = findViewById(R.id.textoSinHorasEstadisticas);
        RecyclerView listaMaterias = findViewById(R.id.listaDistribucionMaterias);
        listaMaterias.setLayoutManager(new LinearLayoutManager(this));
        adaptadorMaterias = new EstadisticasAdapter.MateriasAdapter();
        listaMaterias.setAdapter(adaptadorMaterias);

        textoCumplimientoValor = findViewById(R.id.textoCumplimientoValor);
        textoCumplimientoDesglose = findViewById(R.id.textoCumplimientoDesglose);
        textoPendientesAviso = findViewById(R.id.textoPendientesAviso);

        textoIndiceConcentracion = findViewById(R.id.textoIndiceConcentracion);
        textoInterrupciones = findViewById(R.id.textoInterrupciones);
        textoPuntualidad = findViewById(R.id.textoPuntualidad);
        textoRespetoTiempo = findViewById(R.id.textoRespetoTiempo);
        textoTiempoFuera = findViewById(R.id.textoTiempoFuera);
        textoEstudioExternoResumen = findViewById(R.id.textoEstudioExternoResumen);
        RecyclerView listaEstudioExterno = findViewById(R.id.listaEstudioExternoTopicos);
        listaEstudioExterno.setLayoutManager(new LinearLayoutManager(this));
        adaptadorEstudioExterno = new EstadisticasAdapter.TopicosAdapter();
        listaEstudioExterno.setAdapter(adaptadorEstudioExterno);

        textoSinTopicos = findViewById(R.id.textoSinTopicos);
        RecyclerView listaTopicos = findViewById(R.id.listaTopicosInterrupcion);
        listaTopicos.setLayoutManager(new LinearLayoutManager(this));
        adaptadorTopicos = new EstadisticasAdapter.TopicosAdapter();
        listaTopicos.setAdapter(adaptadorTopicos);

        configurarGraficoDona();

        grupoPeriodo.addOnButtonCheckedListener(this::onPeriodoCambiado);
        grupoSubvista.addOnButtonCheckedListener(this::onSubvistaCambiada);
        findViewById(R.id.btnPeriodoAnteriorEstadisticas).setOnClickListener(this::onAnteriorClick);
        findViewById(R.id.btnPeriodoSiguienteEstadisticas).setOnClickListener(this::onSiguienteClick);
        findViewById(R.id.botonAbrirReportes).setOnClickListener(this::onAbrirReportesClick);
    }

    private void configurarGraficoDona() {
        graficoDistribucion.setUsePercentValues(true);
        graficoDistribucion.getDescription().setEnabled(false);
        graficoDistribucion.getLegend().setEnabled(false);
        graficoDistribucion.setDrawEntryLabels(false);
        graficoDistribucion.setDrawHoleEnabled(true);
        graficoDistribucion.setHoleRadius(55f);
        graficoDistribucion.setTransparentCircleRadius(60f);
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, tv, true)) {
            graficoDistribucion.setHoleColor(tv.data);
        }
    }

    private void onPeriodoCambiado(
            MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
        if (!isChecked) {
            return;
        }
        if (checkedId == R.id.btnPeriodoMes) {
            modelo.cambiarTipo(PeriodoEstadisticas.Tipo.MES);
        } else {
            modelo.cambiarTipo(PeriodoEstadisticas.Tipo.SEMANA);
        }
    }

    private void onSubvistaCambiada(
            MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
        if (!isChecked) {
            return;
        }
        if (checkedId == R.id.btnSubvistaConcentracion) {
            modelo.cambiarSubvista(EstadisticasViewModel.Subvista.CONCENTRACION);
        } else {
            modelo.cambiarSubvista(EstadisticasViewModel.Subvista.RESUMEN);
        }
    }

    private void onAnteriorClick(View v) {
        modelo.periodoAnterior();
    }

    private void onSiguienteClick(View v) {
        modelo.periodoSiguiente();
    }

    private void onAbrirReportesClick(View v) {
        PeriodoEstadisticas actual = modelo.periodo().getValue();
        if (actual != null) {
            startActivity(ReportesActivity.crearIntent(this, actual.tipo(), actual.fechaFoco()));
        } else {
            startActivity(new Intent(this, ReportesActivity.class));
        }
    }

    private void onPeriodo(@Nullable PeriodoEstadisticas p) {
        if (p == null) {
            return;
        }
        textoRango.setText(String.format(
                Locale.getDefault(), "%s – %s", p.inicio().toString(), p.fin().toString()));
    }

    private void onSubvista(@Nullable EstadisticasViewModel.Subvista subvista) {
        boolean esConcentracion = subvista == EstadisticasViewModel.Subvista.CONCENTRACION;
        contenedorResumen.setVisibility(esConcentracion ? View.GONE : View.VISIBLE);
        contenedorConcentracion.setVisibility(esConcentracion ? View.VISIBLE : View.GONE);
    }

    private void onEstadisticas(@Nullable EstadisticasPeriodo e) {
        if (e == null) {
            return;
        }
        renderizarResumen(e);
        renderizarConcentracion(e.concentracion());
    }

    private void renderizarResumen(EstadisticasPeriodo e) {
        String horasTxt = String.format(Locale.getDefault(), "%.1f", e.horasRealesTotales());
        textoTotalHoras.setText(getString(R.string.estadisticas_total_horas, horasTxt));
        textoVariacion.setText(formatearVariacion(e));

        List<DesgloseMateriaPeriodo> materias = e.distribucionMaterias();
        if (materias.isEmpty() || e.minutosRealesTotales() == 0) {
            graficoDistribucion.setVisibility(View.GONE);
            textoSinHoras.setVisibility(View.VISIBLE);
            adaptadorMaterias.actualizar(java.util.Collections.emptyList());
        } else {
            graficoDistribucion.setVisibility(View.VISIBLE);
            textoSinHoras.setVisibility(View.GONE);
            List<PieEntry> entradas = new ArrayList<>();
            List<Integer> colores = new ArrayList<>();
            for (DesgloseMateriaPeriodo m : materias) {
                if (m.minutosReales() > 0) {
                    entradas.add(new PieEntry((float) m.minutosReales(), m.nombreMateria()));
                    // Porción con el color de la materia ajustado a la superficie (design.md, decisión 6).
                    colores.add(AcentoMateria.ajustado(graficoDistribucion, m.colorMateria()));
                }
            }
            PieDataSet set = new PieDataSet(entradas, "");
            set.setColors(colores);
            set.setSliceSpace(2f);
            set.setDrawValues(false);
            graficoDistribucion.setData(new PieData(set));
            graficoDistribucion.invalidate();
            adaptadorMaterias.actualizar(materias);
        }

        if (e.tasaCumplimientoPorcentaje() == null) {
            // Estado vacío en texto de cuerpo, no en el tamaño del porcentaje (design.md, decisión 9).
            TextViewCompat.setTextAppearance(textoCumplimientoValor, R.style.TextAppearance_StudyPlan_BodyMedium);
            textoCumplimientoValor.setTextColor(MaterialColors.getColor(
                    textoCumplimientoValor, com.google.android.material.R.attr.colorOnSurfaceVariant));
            textoCumplimientoValor.setText(R.string.estadisticas_cumplimiento_vacio);
        } else {
            int cumplidas = e.totalCumplidas();
            int evaluables = e.totalCerradas();
            int pct = (int) Math.round(e.tasaCumplimientoPorcentaje());
            TextViewCompat.setTextAppearance(textoCumplimientoValor, R.style.TextAppearance_StudyPlan_HeadlineSmall);
            textoCumplimientoValor.setTextColor(MaterialColors.getColor(
                    textoCumplimientoValor, com.google.android.material.R.attr.colorOnSurface));
            textoCumplimientoValor.setText(
                    getString(R.string.estadisticas_cumplimiento_valor, pct, cumplidas, evaluables));
        }
        textoCumplimientoDesglose.setText(getString(
                R.string.estadisticas_cumplimiento_desglose,
                e.completadas(),
                e.completadasTardias(),
                e.noRealizadas(),
                e.reprogramadas()));
        if (e.pendientesOVigentes() > 0) {
            textoPendientesAviso.setVisibility(View.VISIBLE);
            textoPendientesAviso.setText(
                    getString(R.string.estadisticas_pendientes_aviso, e.pendientesOVigentes()));
        } else {
            textoPendientesAviso.setVisibility(View.GONE);
        }
    }

    private String formatearVariacion(EstadisticasPeriodo e) {
        boolean semana = e.periodo().tipo() == PeriodoEstadisticas.Tipo.SEMANA;
        Double var = e.variacionPorcentaje();
        if (var == null) {
            return getString(R.string.estadisticas_sin_datos_metrica);
        }
        int pct = (int) Math.round(Math.abs(var));
        if (pct == 0) {
            return getString(semana
                    ? R.string.estadisticas_variacion_igual_semana
                    : R.string.estadisticas_variacion_igual_mes);
        }
        if (var > 0) {
            return getString(semana
                    ? R.string.estadisticas_variacion_sube_semana
                    : R.string.estadisticas_variacion_sube_mes, pct);
        }
        return getString(semana
                ? R.string.estadisticas_variacion_baja_semana
                : R.string.estadisticas_variacion_baja_mes, pct);
    }

    private void renderizarConcentracion(ConcentracionPeriodo c) {
        String sinDatos = getString(R.string.estadisticas_sin_datos_metrica);
        textoIndiceConcentracion.setText(c.indiceConcentracionPorcentaje() == null
                ? sinDatos
                : String.format(Locale.getDefault(), "%d %%",
                Math.round(c.indiceConcentracionPorcentaje())));
        String interrHora = String.format(Locale.getDefault(), "%.1f", c.interrupcionesPorHora());
        textoInterrupciones.setText(getString(
                R.string.estadisticas_metrica_etiqueta,
                getString(R.string.estadisticas_interrupciones),
                getString(R.string.estadisticas_interrupciones_valor, interrHora, c.totalInterrupciones())));
        String puntTxt = c.puntualidadPromedioMinutos() == null
                ? sinDatos
                : getString(R.string.estadisticas_puntualidad_valor,
                String.format(Locale.getDefault(), "%.1f", c.puntualidadPromedioMinutos()));
        textoPuntualidad.setText(getString(
                R.string.estadisticas_metrica_etiqueta,
                getString(R.string.estadisticas_puntualidad),
                puntTxt));
        String respTxt = c.respetoDelTiempoPorcentaje() == null
                ? sinDatos
                : String.format(Locale.getDefault(), "%d %%",
                Math.round(c.respetoDelTiempoPorcentaje()));
        textoRespetoTiempo.setText(getString(
                R.string.estadisticas_metrica_etiqueta,
                getString(R.string.estadisticas_respeto_tiempo),
                respTxt));
        textoTiempoFuera.setText(getString(
                R.string.estadisticas_metrica_etiqueta,
                getString(R.string.estadisticas_tiempo_fuera),
                getString(R.string.estadisticas_tiempo_fuera_valor, c.tiempoFueraAppMinutos())));

        textoEstudioExternoResumen.setText(getString(
                R.string.estadisticas_estudio_externo_resumen,
                c.estudioFueraAppMinutos(),
                c.estudioFueraSinClasificarMinutos()));
        adaptadorEstudioExterno.actualizar(c.estudioFueraPorTopico());

        if (c.resumenTopicosInterrupciones().isEmpty()) {
            textoSinTopicos.setVisibility(View.VISIBLE);
            adaptadorTopicos.actualizar(java.util.Collections.emptyList());
        } else {
            textoSinTopicos.setVisibility(View.GONE);
            adaptadorTopicos.actualizar(c.resumenTopicosInterrupciones());
        }
    }
}
