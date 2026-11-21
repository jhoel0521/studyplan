package io.github.jhoel0521.studyplan.adapter.in.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

import io.github.jhoel0521.studyplan.application.port.in.metas.ConsultarProgresoMetas;
import io.github.jhoel0521.studyplan.domain.readmodel.ProgresoMeta;

/**
 * Estado de la pantalla de historial semanal de metas ({@code HU-27}, {@code RN-31}, {@code RN-33}).
 */
public class HistorialMetasViewModel extends ViewModel {

    private final ConsultarProgresoMetas consultar;
    private final Executor ejecutor;
    private final MutableLiveData<List<ProgresoMeta>> historial = new MutableLiveData<>();

    public HistorialMetasViewModel(ConsultarProgresoMetas consultar, Executor ejecutor) {
        this.consultar = Objects.requireNonNull(consultar, "consultar");
        this.ejecutor = Objects.requireNonNull(ejecutor, "ejecutor");
    }

    public LiveData<List<ProgresoMeta>> historial() {
        return historial;
    }

    public void cargar() {
        ejecutor.execute(() -> historial.postValue(consultar.historialMetas()));
    }
}
