package io.github.jhoel0521.studyplan.adapter.in.ui.configuracion;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.Executor;

import io.github.jhoel0521.studyplan.application.port.in.configuracion.BorrarDatos;
import io.github.jhoel0521.studyplan.domain.enumeration.AlcanceBorrado;
import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;

/**
 * Estado del borrado de datos, con doble confirmación modelada como dos pasos y el alcance elegido (teléfono, Drive o
 * ambos). Solo borra al confirmar el segundo.
 *
 * <p>Implementa: HU-38, RN-21 y RNF-13.
 */
public class BorrarDatosViewModel extends ViewModel {

    /** Pasos de la pantalla. */
    public enum Paso {
        NINGUNO, PRIMERA_CONFIRMACION, SEGUNDA_CONFIRMACION, BORRANDO,
        /** Se borró el teléfono (solo o con Drive): hay que volver al primer uso. */
        BORRADO,
        /** Se borró solo el respaldo de Drive: el teléfono sigue como estaba. */
        BORRADO_DRIVE,
        /** Drive falló; no se borró nada del teléfono. */
        ERROR
    }

    private final BorrarDatos borrarDatos;
    private final Executor ejecutor;
    private final MutableLiveData<Paso> paso = new MutableLiveData<>(Paso.NINGUNO);
    private AlcanceBorrado alcance = AlcanceBorrado.TELEFONO;
    private volatile ResultadoRespaldo ultimoError;

    public BorrarDatosViewModel(BorrarDatos borrarDatos, Executor ejecutor) {
        this.borrarDatos = borrarDatos;
        this.ejecutor = ejecutor;
    }

    public LiveData<Paso> paso() {
        return paso;
    }

    public AlcanceBorrado alcance() {
        return alcance;
    }

    /** Qué se borrará; por defecto, el teléfono. */
    public void elegirAlcance(AlcanceBorrado nuevo) {
        alcance = nuevo;
    }

    /** El motivo del último {@link Paso#ERROR}. */
    public ResultadoRespaldo ultimoError() {
        return ultimoError;
    }

    /** El estudiante pulsó «Borrar». */
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
        final AlcanceBorrado elegido = alcance;
        ejecutor.execute(() -> {
            ResultadoRespaldo resultado = borrarDatos.ejecutar(elegido);
            if (resultado != ResultadoRespaldo.OK) {
                ultimoError = resultado;
                paso.postValue(Paso.ERROR);
            } else if (elegido == AlcanceBorrado.DRIVE) {
                paso.postValue(Paso.BORRADO_DRIVE);
            } else {
                paso.postValue(Paso.BORRADO);
            }
        });
    }
}
