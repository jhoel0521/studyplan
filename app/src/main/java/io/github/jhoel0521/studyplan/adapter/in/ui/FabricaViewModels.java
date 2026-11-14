package io.github.jhoel0521.studyplan.adapter.in.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

/**
 * Crea los ViewModel con los casos de uso que expone {@link StudyPlanApplication} (inyección manual).
 *
 * <p>Implementa: HU-01, HU-02, HU-03, HU-04 y HU-38 (conecta las pantallas con el núcleo).
 */
public class FabricaViewModels implements ViewModelProvider.Factory {

    private final StudyPlanApplication app;

    public FabricaViewModels(StudyPlanApplication app) {
        this.app = app;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> clase) {
        if (clase.isAssignableFrom(PerfilInicialViewModel.class)) {
            return (T) new PerfilInicialViewModel(app.configurarPerfil(), app.ejecutor());
        }
        if (clase.isAssignableFrom(InicioViewModel.class)) {
            return (T) new InicioViewModel(app.obtenerResumenInicio(), app.ejecutor());
        }
        if (clase.isAssignableFrom(ProteccionViewModel.class)) {
            return (T) new ProteccionViewModel(app.consultarEstadoArranque(), app.ejecutor());
        }
        if (clase.isAssignableFrom(AjustesViewModel.class)) {
            return (T) new AjustesViewModel(app.consultarAjustes(), app.configurarApariencia(),
                    app.configurarBloqueo(), app.ejecutor());
        }
        if (clase.isAssignableFrom(BorrarDatosViewModel.class)) {
            return (T) new BorrarDatosViewModel(app.borrarDatos(), app.ejecutor());
        }
        throw new IllegalArgumentException("ViewModel desconocido: " + clase.getName());
    }
}
