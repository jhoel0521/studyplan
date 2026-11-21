package io.github.jhoel0521.studyplan.adapter.in.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.Executor;

import io.github.jhoel0521.studyplan.application.port.in.configuracion.ConfigurarPerfil;
import io.github.jhoel0521.studyplan.domain.entity.Perfil;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Estado de la pantalla de perfil inicial. Llama al caso de uso fuera del hilo principal y publica el resultado:
 * un error de validación (por ejemplo {@code NOMBRE_VACIO}) o el perfil guardado.
 *
 * <p>Implementa: HU-01 y RNF-14.
 */
public class PerfilInicialViewModel extends ViewModel {

    private final ConfigurarPerfil configurarPerfil;
    private final Executor ejecutor;
    private final MutableLiveData<Resultado<Perfil>> resultado = new MutableLiveData<>();

    public PerfilInicialViewModel(ConfigurarPerfil configurarPerfil, Executor ejecutor) {
        this.configurarPerfil = configurarPerfil;
        this.ejecutor = ejecutor;
    }

    /** @return el último resultado de guardar; nulo si todavía no se intentó o ya se consumió */
    public LiveData<Resultado<Perfil>> resultado() {
        return resultado;
    }

    /**
     * Intenta guardar el perfil.
     *
     * @param nombre texto escrito por el estudiante
     * @param metaTexto horas escritas por el estudiante; si no es un número se trata como fuera de rango
     */
    public void guardar(String nombre, String metaTexto) {
        double meta = leerNumero(metaTexto);
        ejecutor.execute(() -> resultado.postValue(configurarPerfil.ejecutar(nombre, meta)));
    }

    /** Marca el resultado como atendido, para que una rotación no lo repita. */
    public void consumirResultado() {
        resultado.setValue(null);
    }

    private static double leerNumero(String texto) {
        try {
            return Double.parseDouble(texto == null ? "" : texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
