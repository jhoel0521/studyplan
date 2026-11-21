package io.github.jhoel0521.studyplan.adapter.in.ui.reportes;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.application.port.out.system.ResultadoExportacion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.enumeration.FormatoExportacionReporte;
import io.github.jhoel0521.studyplan.domain.valueobject.PeriodoEstadisticas;
import io.github.jhoel0521.studyplan.domain.readmodel.ReporteEstudio;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.ActividadProtegida;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.GrupoBotones;

/**
 * Pantalla de Reportes por periodo semanal o mensual con exportación y compartición en PDF/CSV
 * ({@code HU-32}, {@code HU-33}, {@code RNF-06}, {@code RNF-10}, {@code RNF-12}).
 */
public class ReportesActivity extends ActividadProtegida {

    public static final String EXTRA_TIPO_PERIODO = "extra_tipo_periodo";
    public static final String EXTRA_FECHA_FOCO = "extra_fecha_foco";

    private ReportesViewModel modelo;
    private MaterialButtonToggleGroup grupoPeriodo;
    private TextView textoRango;
    private TextView textoTotalPlanificadas;
    private TextView textoTotalReales;
    private TextView textoTotalCumplimiento;
    private TextView textoTotalNoRealizadas;
    private MaterialButton botonExportarPdf;
    private MaterialButton botonExportarCsv;
    private TextView textoSinDatos;
    private RecyclerView listaMaterias;
    private ReportesAdapter adaptador;

    private ReporteEstudio reporteActual;
    private boolean exportandoActual;

    public static Intent crearIntent(
            Context context, PeriodoEstadisticas.Tipo tipo, Fecha fechaFoco) {
        Intent intent = new Intent(context, ReportesActivity.class);
        if (tipo != null) {
            intent.putExtra(EXTRA_TIPO_PERIODO, tipo.name());
        }
        if (fechaFoco != null) {
            intent.putExtra(EXTRA_FECHA_FOCO, fechaFoco.iso());
        }
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes);
        aplicarInsets();

        MaterialToolbar barra = findViewById(R.id.barraSuperior);
        barra.setNavigationOnClickListener(this::onVolverClick);

        grupoPeriodo = findViewById(R.id.grupoPeriodoReportes);
        GrupoBotones.ajustarEnUnaLinea(grupoPeriodo);
        textoRango = findViewById(R.id.textoRangoReportes);
        textoTotalPlanificadas = findViewById(R.id.textoTotalPlanificadasReportes);
        textoTotalReales = findViewById(R.id.textoTotalRealesReportes);
        textoTotalCumplimiento = findViewById(R.id.textoTotalCumplimientoReportes);
        textoTotalNoRealizadas = findViewById(R.id.textoTotalNoRealizadasReportes);
        botonExportarPdf = findViewById(R.id.botonExportarPdf);
        botonExportarCsv = findViewById(R.id.botonExportarCsv);
        textoSinDatos = findViewById(R.id.textoSinDatosReportes);
        listaMaterias = findViewById(R.id.listaMateriasReportes);

        adaptador = new ReportesAdapter();
        listaMaterias.setLayoutManager(new LinearLayoutManager(this));
        listaMaterias.setAdapter(adaptador);

        modelo = viewModel(ReportesViewModel.class);
        modelo.inicializarSiFalta(
                parsearTipo(getIntent().getStringExtra(EXTRA_TIPO_PERIODO)),
                parsearFecha(getIntent().getStringExtra(EXTRA_FECHA_FOCO)));

        grupoPeriodo.addOnButtonCheckedListener(this::onPeriodoCambiado);
        findViewById(R.id.btnPeriodoAnteriorReportes).setOnClickListener(this::onAnteriorClick);
        findViewById(R.id.btnPeriodoSiguienteReportes).setOnClickListener(this::onSiguienteClick);
        botonExportarPdf.setOnClickListener(this::onExportarPdfClick);
        botonExportarCsv.setOnClickListener(this::onExportarCsvClick);

        modelo.periodo().observe(this, this::onPeriodo);
        modelo.reporte().observe(this, this::onReporte);
        modelo.exportando().observe(this, this::onEstadoExportando);
        modelo.exportacion().observe(this, this::onEventoExportacion);
        modelo.errorExportacion().observe(this, this::onErrorExportacion);
    }

    @Override
    protected void alEstarListo() {
        modelo.cargar();
    }

    private void onVolverClick(View v) {
        finish();
    }

    private void onPeriodoCambiado(
            MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
        if (!isChecked) {
            return;
        }
        if (checkedId == R.id.btnPeriodoMesReportes) {
            modelo.cambiarTipo(PeriodoEstadisticas.Tipo.MES);
        } else {
            modelo.cambiarTipo(PeriodoEstadisticas.Tipo.SEMANA);
        }
    }

    private void onAnteriorClick(View v) {
        modelo.periodoAnterior();
    }

    private void onSiguienteClick(View v) {
        modelo.periodoSiguiente();
    }

    private void onExportarPdfClick(View v) {
        modelo.exportarPdf();
    }

    private void onExportarCsvClick(View v) {
        modelo.exportarCsv();
    }

    private void onPeriodo(@Nullable PeriodoEstadisticas p) {
        if (p == null) {
            return;
        }
        int idObjetivo = p.tipo() == PeriodoEstadisticas.Tipo.MES
                ? R.id.btnPeriodoMesReportes
                : R.id.btnPeriodoSemanaReportes;
        if (grupoPeriodo.getCheckedButtonId() != idObjetivo) {
            grupoPeriodo.check(idObjetivo);
        }
        textoRango.setText(String.format(
                Locale.getDefault(), "%s \u2013 %s", p.inicio().toString(), p.fin().toString()));
    }

    private void onReporte(@Nullable ReporteEstudio reporte) {
        reporteActual = reporte;
        if (reporte == null) {
            actualizarEstadoBotonesExportar();
            return;
        }

        String plan = String.format(Locale.getDefault(), "%.1f", reporte.horasPlanificadasTotales());
        String real = String.format(Locale.getDefault(), "%.1f", reporte.horasRealesTotales());
        textoTotalPlanificadas.setText(getString(R.string.estadisticas_total_horas, plan));
        textoTotalReales.setText(getString(R.string.estadisticas_total_horas, real));

        Double cumpl = reporte.cumplimientoTotal();
        textoTotalCumplimiento.setText(cumpl == null
                ? getString(R.string.estadisticas_sin_datos_metrica)
                : getString(R.string.reportes_cumplimiento_porcentaje, Math.round(cumpl)));
        textoTotalNoRealizadas.setText(String.valueOf(reporte.sesionesNoRealizadasTotales()));

        boolean sinDatos = reporte.esSinDatos();
        textoSinDatos.setVisibility(sinDatos ? View.VISIBLE : View.GONE);
        listaMaterias.setVisibility(sinDatos ? View.GONE : View.VISIBLE);
        adaptador.actualizar(reporte.materias());
        actualizarEstadoBotonesExportar();
    }

    private void onEstadoExportando(@Nullable Boolean exportando) {
        exportandoActual = Boolean.TRUE.equals(exportando);
        actualizarEstadoBotonesExportar();
    }

    private void actualizarEstadoBotonesExportar() {
        boolean habilitado = reporteActual != null
                && !reporteActual.esSinDatos()
                && !exportandoActual;
        botonExportarPdf.setEnabled(habilitado);
        botonExportarCsv.setEnabled(habilitado);
    }

    private void onEventoExportacion(@Nullable ReportesViewModel.EventoExportacion evento) {
        if (evento == null) {
            return;
        }
        modelo.consumirExportacion();
        Uri uri = Uri.parse(evento.uriContenido());
        String mime = evento.formato() == FormatoExportacionReporte.CSV
                ? "text/csv"
                : "application/pdf";
        Intent compartir = new Intent(Intent.ACTION_SEND);
        compartir.setType(mime);
        compartir.putExtra(Intent.EXTRA_STREAM, uri);
        compartir.setClipData(ClipData.newRawUri("", uri));
        compartir.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooser = Intent.createChooser(
                compartir, getString(R.string.reportes_compartir_titulo));
        try {
            startActivity(chooser);
        } catch (ActivityNotFoundException e) {
            Snackbar.make(
                    findViewById(R.id.main),
                    R.string.reportes_error_sin_app,
                    Snackbar.LENGTH_LONG).show();
        }
    }

    private void onErrorExportacion(@Nullable ResultadoExportacion.Error error) {
        if (error == null) {
            return;
        }
        modelo.consumirError();
        int mensajeId = error == ResultadoExportacion.Error.SIN_DATOS
                ? R.string.reportes_error_sin_datos
                : R.string.reportes_error_escritura;
        Snackbar.make(findViewById(R.id.main), mensajeId, Snackbar.LENGTH_LONG).show();
    }

    @Nullable
    private static PeriodoEstadisticas.Tipo parsearTipo(@Nullable String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        try {
            return PeriodoEstadisticas.Tipo.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    private static Fecha parsearFecha(@Nullable String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        Resultado<Fecha> resultado = Fecha.parsear(valor);
        return resultado.esValido() ? resultado.valor() : null;
    }
}
