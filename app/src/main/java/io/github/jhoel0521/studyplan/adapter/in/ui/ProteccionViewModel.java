package io.github.jhoel0521.studyplan.adapter.in.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.Executor;

import io.github.jhoel0521.studyplan.application.port.in.ConsultarEstadoArranque;
import io.github.jhoel0521.studyplan.domain.enumeration.EstadoArranque;

/**
 * Pregunta qué debe mostrar la app al arrancar o volver a primer plano: primer uso, pantalla de bloqueo o la
 * pantalla pedida. La usan todas las Activities protegidas.
 *
 * <p>Implementa: HU-01 (el primer uso no pide autenticación) y HU-04 (autenticar al abrir y al volver).
 */
public class ProteccionViewModel extends ViewModel {

    private final ConsultarEstadoArranque consultarEstado;
    private final Executor ejecutor;
    private final MutableLiveData<EstadoArranque> estado = new MutableLiveData<>();

    public ProteccionViewModel(ConsultarEstadoArranque consultarEstado, Executor ejecutor) {
        this.consultarEstado = consultarEstado;
        this.ejecutor = ejecutor;
    }

    public LiveData<EstadoArranque> estado() {
        return estado;
    }

    /** Consulta el estado en segundo plano y lo publica. */
    public void comprobar() {
        ejecutor.execute(() -> estado.postValue(consultarEstado.ejecutar()));
    }

    /** Marca el estado como atendido, para que una rotación o un regreso no abra la misma pantalla otra vez. */
    public void consumirEstado() {
        estado.setValue(null);
    }
}
