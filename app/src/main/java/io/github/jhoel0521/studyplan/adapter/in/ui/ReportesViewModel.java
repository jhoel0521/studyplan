package io.github.jhoel0521.studyplan.adapter.in.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import io.github.jhoel0521.studyplan.application.port.in.GenerarReporteEstudio;
import io.github.jhoel0521.studyplan.application.port.out.ExportadorReporte;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.ResultadoExportacion;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.FormatoExportacionReporte;
import io.github.jhoel0521.studyplan.domain.PeriodoEstadisticas;
import io.github.jhoel0521.studyplan.domain.ReporteEstudio;

/**
 * ViewModel de la pantalla de Reportes ({@code HU-32}, {@code HU-33}, {@code RNF-10}): gestiona el
 * periodo visible con inicialización idempotente, descarta respuestas obsoletas ante navegación
 * rápida y coordina exportaciones PDF/CSV sin doble pulsación.
 */
public class ReportesViewModel extends ViewModel {

    /** Evento único con el identificador compartible y formato generado. */
    public static final class EventoExportacion {
        private final String uriContenido;
        private final FormatoExportacionReporte formato;

        public EventoExportacion(String uriContenido, FormatoExportacionReporte formato) {
            this.uriContenido = Objects.requireNonNull(uriContenido, "uriContenido");
            this.formato = Objects.requireNonNull(formato, "formato");
        }

        public String uriContenido() {
            return uriContenido;
        }

        public FormatoExportacionReporte formato() {
            return formato;
        }
    }

    private final GenerarReporteEstudio generarReporte;
    private final ExportadorReporte exportador;
    private final Reloj reloj;
    private final Executor ejecutor;

    private final MutableLiveData<PeriodoEstadisticas> periodo = new MutableLiveData<>();
    private final MutableLiveData<ReporteEstudio> reporte = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> exportando = new MutableLiveData<>(false);
    private final MutableLiveData<EventoExportacion> exportacion = new MutableLiveData<>();
    private final MutableLiveData<ResultadoExportacion.Error> errorExportacion =
            new MutableLiveData<>();

    private final AtomicLong versionCarga = new AtomicLong();
    private final Object bloqueoResultado = new Object();
    private boolean inicializado;

    public ReportesViewModel(
            GenerarReporteEstudio generarReporte,
            ExportadorReporte exportador,
            Reloj reloj,
            Executor ejecutor) {
        this.generarReporte = Objects.requireNonNull(generarReporte, "generarReporte");
        this.exportador = Objects.requireNonNull(exportador, "exportador");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.ejecutor = Objects.requireNonNull(ejecutor, "ejecutor");
    }

    public LiveData<PeriodoEstadisticas> periodo() {
        return periodo;
    }

    public LiveData<ReporteEstudio> reporte() {
        return reporte;
    }

    public LiveData<Boolean> cargando() {
        return cargando;
    }

    public LiveData<Boolean> exportando() {
        return exportando;
    }

    public LiveData<EventoExportacion> exportacion() {
        return exportacion;
    }

    public LiveData<ResultadoExportacion.Error> errorExportacion() {
        return errorExportacion;
    }

    /**
     * Fija el periodo inicial la primera vez que se abre la pantalla, ignorando reintentos tras
     * rotación ({@code RNF-10}).
     */
    public void inicializarSiFalta(PeriodoEstadisticas.Tipo tipoInicial, Fecha fechaFocoInicial) {
        if (inicializado) {
            return;
        }
        PeriodoEstadisticas.Tipo tipo =
                tipoInicial != null ? tipoInicial : PeriodoEstadisticas.Tipo.SEMANA;
        Fecha foco = fechaFocoInicial != null ? fechaFocoInicial : reloj.hoy();
        periodo.setValue(PeriodoEstadisticas.de(tipo, foco));
        inicializado = true;
    }

    public void cargar() {
        inicializarSiFalta(null, null);
        lanzarCarga(periodo.getValue());
    }

    public void cambiarTipo(PeriodoEstadisticas.Tipo nuevoTipo) {
        if (nuevoTipo == null) {
            return;
        }
        inicializarSiFalta(null, null);
        PeriodoEstadisticas actual = periodo.getValue();
        if (actual == null || actual.tipo() == nuevoTipo) {
            return;
        }
        PeriodoEstadisticas actualizado = actual.conTipo(nuevoTipo);
        periodo.setValue(actualizado);
        lanzarCarga(actualizado);
    }

    public void periodoAnterior() {
        inicializarSiFalta(null, null);
        PeriodoEstadisticas actual = periodo.getValue();
        if (actual == null) {
            return;
        }
        PeriodoEstadisticas anterior = actual.anterior();
        periodo.setValue(anterior);
        lanzarCarga(anterior);
    }

    public void periodoSiguiente() {
        inicializarSiFalta(null, null);
        PeriodoEstadisticas actual = periodo.getValue();
        if (actual == null) {
            return;
        }
        PeriodoEstadisticas siguiente = actual.siguiente();
        periodo.setValue(siguiente);
        lanzarCarga(siguiente);
    }

    public void exportarPdf() {
        exportar(FormatoExportacionReporte.PDF);
    }

    public void exportarCsv() {
        exportar(FormatoExportacionReporte.CSV);
    }

    public void exportar(FormatoExportacionReporte formato) {
        Objects.requireNonNull(formato, "formato");
        if (Boolean.TRUE.equals(exportando.getValue())) {
            return;
        }
        ReporteEstudio actual = reporte.getValue();
        if (actual == null || actual.esSinDatos()) {
            errorExportacion.setValue(ResultadoExportacion.Error.SIN_DATOS);
            return;
        }
        long versionEnCurso = versionCarga.get();
        exportando.setValue(true);
        ejecutor.execute(() -> {
            ResultadoExportacion resultado = exportador.exportar(actual, formato);
            synchronized (bloqueoResultado) {
                exportando.postValue(false);
                if (versionEnCurso != versionCarga.get()) {
                    return;
                }
                if (resultado.esExitoso()) {
                    exportacion.postValue(
                            new EventoExportacion(resultado.uriContenido(), formato));
                } else {
                    errorExportacion.postValue(resultado.error());
                }
            }
        });
    }

    public void consumirExportacion() {
        exportacion.setValue(null);
    }

    public void consumirError() {
        errorExportacion.setValue(null);
    }

    private void lanzarCarga(PeriodoEstadisticas objetivo) {
        if (objetivo == null) {
            return;
        }
        long version = versionCarga.incrementAndGet();
        cargando.setValue(true);
        ejecutor.execute(() -> {
            if (version != versionCarga.get()) {
                return;
            }
            ReporteEstudio generado = generarReporte.ejecutar(objetivo);
            synchronized (bloqueoResultado) {
                if (version == versionCarga.get()) {
                    reporte.postValue(generado);
                    cargando.postValue(false);
                }
            }
        });
    }
}
