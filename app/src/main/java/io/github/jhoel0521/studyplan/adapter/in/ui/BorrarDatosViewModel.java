package io.github.jhoel0521.studyplan.adapter.in.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.Executor;

import io.github.jhoel0521.studyplan.application.port.in.configuracion.BorrarDatos;
import io.github.jhoel0521.studyplan.domain.enumeration.AlcanceBorrado;

/**
 * Estado del borrado de datos, con doble confirmación modelada como dos pasos. Solo borra al confirmar el segundo.
 *
 * <p>Implementa: HU-38 (alcance «teléfono»), RN-21 y RNF-13.
 */
public class BorrarDatosViewModel extends ViewModel {

    /** Pasos de la pantalla. */
    public enum Paso { NINGUNO, PRIMERA_CONFIRMACION, SEGUNDA_CONFIRMACION, BORRANDO, BORRADO }

    private final BorrarDatos borrarDatos;
    private final Executor ejecutor;
    private final MutableLiveData<Paso> paso = new MutableLiveData<>(Paso.NINGUNO);

    public BorrarDatosViewModel(BorrarDatos borrarDatos, Executor ejecutor) {
        this.borrarDatos = borrarDatos;
        this.ejecutor = ejecutor;
    }

    public LiveData<Paso> paso() {
        return paso;
    }

    /** El estudiante pulsó «Borrar datos del teléfono». */
    public void pedirBorrado() {
        paso.setValue(Paso.PRIMERA_CONFIRMACION);
    }

    public void confirmarPrimera() {
        paso.setValue(Paso.SEGUNDA_CONFIRMACION);
    }

    /** Cancelar en cualquier paso vuelve al inicio y no borra nada. */
    public void cancelar() {
        paso.setValue(Paso.NINGUNO);
    }

    public void confirmarSegunda() {
        paso.setValue(Paso.BORRANDO);
        ejecutor.execute(() -> {
            borrarDatos.ejecutar(AlcanceBorrado.TELEFONO);
            paso.postValue(Paso.BORRADO);
        });
    }
}
