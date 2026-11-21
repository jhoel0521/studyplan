package io.github.jhoel0521.studyplan.adapter.in.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.Executor;

import io.github.jhoel0521.studyplan.application.port.in.estadisticas.ConsultarEstadisticas;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.domain.readmodel.EstadisticasPeriodo;
import io.github.jhoel0521.studyplan.domain.valueobject.PeriodoEstadisticas;

/**
 * ViewModel para la pantalla de Estadísticas ({@code HU-29}, {@code HU-30}, {@code HU-31},
 * {@code HU-45}).
 */
public class EstadisticasViewModel extends ViewModel {

    public enum Subvista {
        RESUMEN,
        CONCENTRACION
    }

    private final ConsultarEstadisticas consultar;
    private final Executor ejecutor;
    private final MutableLiveData<PeriodoEstadisticas> periodo = new MutableLiveData<>();
    private final MutableLiveData<Subvista> subvista = new MutableLiveData<>(Subvista.RESUMEN);
    private final MutableLiveData<EstadisticasPeriodo> estadisticas = new MutableLiveData<>();
    private PeriodoEstadisticas periodoActual;

    public EstadisticasViewModel(
            ConsultarEstadisticas consultar,
            Reloj reloj,
            Executor ejecutor) {
        this.consultar = consultar;
        this.ejecutor = ejecutor;
        this.periodoActual = PeriodoEstadisticas.semanal(reloj.hoy());
        this.periodo.setValue(periodoActual);
    }

    public LiveData<PeriodoEstadisticas> periodo() {
        return periodo;
    }

    public LiveData<Subvista> subvista() {
        return subvista;
    }

    public LiveData<EstadisticasPeriodo> estadisticas() {
        return estadisticas;
    }

    public void cargar() {
        PeriodoEstadisticas objetivo = periodoActual;
        ejecutor.execute(() -> {
            EstadisticasPeriodo res = consultar.ejecutar(objetivo);
            estadisticas.postValue(res);
        });
    }

    public void cambiarTipo(PeriodoEstadisticas.Tipo tipo) {
        if (tipo == null || tipo == periodoActual.tipo()) {
            return;
        }
        periodoActual = periodoActual.conTipo(tipo);
        periodo.setValue(periodoActual);
        cargar();
    }

    public void periodoAnterior() {
        periodoActual = periodoActual.anterior();
        periodo.setValue(periodoActual);
        cargar();
    }

    public void periodoSiguiente() {
        periodoActual = periodoActual.siguiente();
        periodo.setValue(periodoActual);
        cargar();
    }

    public void cambiarSubvista(Subvista nuevaSubvista) {
        if (nuevaSubvista != null && nuevaSubvista != subvista.getValue()) {
            subvista.setValue(nuevaSubvista);
        }
    }
}
