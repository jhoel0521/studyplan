package io.github.jhoel0521.studyplan.adapter.in.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

/**
 * Crea los ViewModel con los casos de uso que expone {@link StudyPlanApplication} (inyección manual).
 *
 * <p>Implementa: HU-01 a HU-04, HU-06 a HU-15, HU-38 y HU-46 (conecta las pantallas con el núcleo).
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
            return (T) new InicioViewModel(
                    app.obtenerResumenInicio(), app.listarMaterias(), app.ejecutor());
        }
        if (clase.isAssignableFrom(CalendarioViewModel.class)) {
            return (T) new CalendarioViewModel(
                    app.consultarCalendario(),
                    app.reloj(),
                    app.preferenciaVistaCalendario(),
                    app.ejecutor());
        }
        if (clase.isAssignableFrom(SesionFormularioViewModel.class)) {
            return (T) new SesionFormularioViewModel(
                    app.planificarSesion(),
                    app.registrarSesionPasada(),
                    app.reprogramarSesion(),
                    app.listarMaterias(),
                    app.consultarSesion(),
                    app.reloj(),
                    app.ejecutor());
        }
        if (clase.isAssignableFrom(SesionDetalleViewModel.class)) {
            return (T) new SesionDetalleViewModel(
                    app.consultarSesion(),
                    app.completarSesion(),
                    app.registrarSesionTardia(),
                    app.eliminarSesion(),
                    app.iniciarSesion(),
                    app.ejecutor());
        }
        if (clase.isAssignableFrom(SesionEnCursoViewModel.class)) {
            return (T) new SesionEnCursoViewModel(
                    app.consultarEjecucionSesion(),
                    app.pausarReanudarSesion(),
                    app.detenerSesion(),
                    app.decidirFinPlanificadoOInvasion(),
                    app.ejecutor());
        }
        if (clase.isAssignableFrom(RepartoSesionViewModel.class)) {
            return (T) new RepartoSesionViewModel(
                    app.consultarSesion(),
                    app.eventoEstudioRepository(),
                    app.repartirTramoSesion(),
                    app.reloj(),
                    app.ejecutor());
        }
        if (clase.isAssignableFrom(ProteccionViewModel.class)) {
            return (T) new ProteccionViewModel(app.consultarEstadoArranque(), app.ejecutor());
        }
        if (clase.isAssignableFrom(AjustesViewModel.class)) {
            return (T) new AjustesViewModel(app.consultarAjustes(), app.configurarApariencia(),
                    app.configurarBloqueo(), app.configurarAjustesEstudio(), app.ejecutor());
        }
        if (clase.isAssignableFrom(BorrarDatosViewModel.class)) {
            return (T) new BorrarDatosViewModel(app.borrarDatos(), app.ejecutor());
        }
        if (clase.isAssignableFrom(ListaMateriasViewModel.class)) {
            return (T) new ListaMateriasViewModel(app.listarMaterias(), app.ejecutor());
        }
        if (clase.isAssignableFrom(MateriaFormularioViewModel.class)) {
            return (T) new MateriaFormularioViewModel(app.registrarMateria(), app.editarMateria(),
                    app.consultarMateria(), app.archivarMateria(), app.desarchivarMateria(), app.eliminarMateria(),
                    app.ejecutor());
        }
        throw new IllegalArgumentException("ViewModel desconocido: " + clase.getName());
    }
}
