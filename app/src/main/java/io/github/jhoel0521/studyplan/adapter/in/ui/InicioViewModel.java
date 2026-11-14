package io.github.jhoel0521.studyplan.adapter.in.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.Executor;

import io.github.jhoel0521.studyplan.application.port.in.ListarMaterias;
import io.github.jhoel0521.studyplan.application.port.in.ObtenerResumenInicio;
import io.github.jhoel0521.studyplan.domain.ResumenInicio;

/**
 * Estado de la pantalla de Inicio: saludo, progreso semanal real, próxima sesión pendiente,
 * sesiones del día actual y disponibilidad de materias activas para planificar.
 *
 * <p>Implementa: HU-02.
 */
public class InicioViewModel extends ViewModel {

    private final ObtenerResumenInicio obtenerResumen;
    private final ListarMaterias listarMaterias;
    private final Executor ejecutor;
    private final MutableLiveData<ResumenInicio> resumen = new MutableLiveData<>();
    private final MutableLiveData<Boolean> hayMateriasActivas = new MutableLiveData<>(false);

    public InicioViewModel(
            ObtenerResumenInicio obtenerResumen,
            ListarMaterias listarMaterias,
            Executor ejecutor) {
        this.obtenerResumen = obtenerResumen;
        this.listarMaterias = listarMaterias;
        this.ejecutor = ejecutor;
    }

    public LiveData<ResumenInicio> resumen() {
        return resumen;
    }

    public LiveData<Boolean> hayMateriasActivas() {
        return hayMateriasActivas;
    }

    /** Lee el resumen y el estado de materias activas en segundo plano; sin perfil no publica resumen. */
    public void cargar() {
        ejecutor.execute(() -> obtenerResumen.ejecutar().ifPresent(r -> {
            boolean activas = !listarMaterias.ejecutar().activas().isEmpty();
            hayMateriasActivas.postValue(activas);
            resumen.postValue(r);
        }));
    }
}
