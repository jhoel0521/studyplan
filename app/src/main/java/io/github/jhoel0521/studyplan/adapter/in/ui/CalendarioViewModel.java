package io.github.jhoel0521.studyplan.adapter.in.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Objects;
import java.util.concurrent.Executor;

import io.github.jhoel0521.studyplan.application.port.in.calendario.ConsultarCalendario;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.domain.readmodel.DatosCalendario;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.FiltroCalendario;
import io.github.jhoel0521.studyplan.domain.valueobject.PeriodoCalendario;
import io.github.jhoel0521.studyplan.domain.enumeration.VistaCalendario;

/**
 * ViewModel del calendario de estudio (MVVM, HU-16 a HU-21, RNF-02, RNF-10).
 *
 * <p>Conserva el periodo activo y los filtros ante rotación y ejecuta todas las consultas fuera del
 * hilo principal.
 */
public class CalendarioViewModel extends ViewModel {

    private final ConsultarCalendario consultarCalendario;
    private final Reloj reloj;
    private final PreferenciaVistaCalendario preferenciaVista;
    private final Executor ejecutor;

    private final MutableLiveData<DatosCalendario> datos = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);

    private PeriodoCalendario periodoActual;
    private FiltroCalendario filtroActual = FiltroCalendario.porDefecto();

    public CalendarioViewModel(
            ConsultarCalendario consultarCalendario,
            Reloj reloj,
            PreferenciaVistaCalendario preferenciaVista,
            Executor ejecutor) {
        this.consultarCalendario = Objects.requireNonNull(consultarCalendario, "consultarCalendario");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.preferenciaVista = Objects.requireNonNull(preferenciaVista, "preferenciaVista");
        this.ejecutor = Objects.requireNonNull(ejecutor, "ejecutor");
    }

    public LiveData<DatosCalendario> datos() {
        return datos;
    }

    public LiveData<Boolean> cargando() {
        return cargando;
    }

    /** Carga inicial (o recarga si ya existe un periodo en memoria tras rotación o edición). */
    public void cargar() {
        ejecutarActualizacion(() -> {
            if (periodoActual == null) {
                Fecha hoy = reloj.hoy();
                VistaCalendario vistaInicial = preferenciaVista.leer();
                periodoActual = PeriodoCalendario.de(vistaInicial, hoy, hoy);
            }
        });
    }

    public void cambiarVista(VistaCalendario nuevaVista) {
        if (nuevaVista == null) {
            return;
        }
        ejecutarActualizacion(() -> {
            asegurarPeriodoInicial();
            Fecha hoy = reloj.hoy();
            periodoActual = periodoActual.cambiarVista(nuevaVista, hoy);
            preferenciaVista.guardar(nuevaVista);
        });
    }

    public void anterior() {
        ejecutarActualizacion(() -> {
            asegurarPeriodoInicial();
            periodoActual = periodoActual.anterior();
        });
    }

    public void siguiente() {
        ejecutarActualizacion(() -> {
            asegurarPeriodoInicial();
            periodoActual = periodoActual.siguiente();
        });
    }

    public void irAHoy() {
        ejecutarActualizacion(() -> {
            asegurarPeriodoInicial();
            periodoActual = periodoActual.irAHoy(reloj.hoy());
        });
    }

    public void seleccionarDia(Fecha fecha) {
        if (fecha == null) {
            return;
        }
        ejecutarActualizacion(() -> {
            asegurarPeriodoInicial();
            periodoActual = periodoActual.seleccionarDia(fecha);
        });
    }

    public void aplicarFiltro(FiltroCalendario nuevoFiltro) {
        ejecutarActualizacion(() -> {
            asegurarPeriodoInicial();
            filtroActual = nuevoFiltro == null ? FiltroCalendario.porDefecto() : nuevoFiltro;
        });
    }

    public void restablecerFiltro() {
        aplicarFiltro(FiltroCalendario.porDefecto());
    }

    private void asegurarPeriodoInicial() {
        if (periodoActual == null) {
            Fecha hoy = reloj.hoy();
            periodoActual = PeriodoCalendario.de(preferenciaVista.leer(), hoy, hoy);
        }
    }

    private void ejecutarActualizacion(Runnable mutacion) {
        cargando.postValue(true);
        ejecutor.execute(() -> {
            mutacion.run();
            DatosCalendario resultado = consultarCalendario.ejecutar(periodoActual, filtroActual);
            datos.postValue(resultado);
            cargando.postValue(false);
        });
    }
}
