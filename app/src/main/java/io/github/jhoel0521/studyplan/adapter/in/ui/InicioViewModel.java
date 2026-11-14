package io.github.jhoel0521.studyplan.adapter.in.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.Executor;

import io.github.jhoel0521.studyplan.application.port.in.ObtenerResumenInicio;
import io.github.jhoel0521.studyplan.domain.ResumenInicio;

/**
 * Estado de la pantalla de Inicio: el saludo y el progreso de la semana.
 *
 * <p>Implementa: HU-02 (versión parcial: saludo y progreso {@code 0 / meta}).
 */
public class InicioViewModel extends ViewModel {

    private final ObtenerResumenInicio obtenerResumen;
    private final Executor ejecutor;
    private final MutableLiveData<ResumenInicio> resumen = new MutableLiveData<>();

    public InicioViewModel(ObtenerResumenInicio obtenerResumen, Executor ejecutor) {
        this.obtenerResumen = obtenerResumen;
        this.ejecutor = ejecutor;
    }

    public LiveData<ResumenInicio> resumen() {
        return resumen;
    }

    /** Lee el resumen en segundo plano; si no hay perfil no publica nada. */
    public void cargar() {
        ejecutor.execute(() -> obtenerResumen.ejecutar().ifPresent(resumen::postValue));
    }
}
