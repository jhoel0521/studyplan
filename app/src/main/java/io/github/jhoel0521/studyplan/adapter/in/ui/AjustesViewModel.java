package io.github.jhoel0521.studyplan.adapter.in.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.Executor;

import io.github.jhoel0521.studyplan.application.port.in.ConfigurarApariencia;
import io.github.jhoel0521.studyplan.application.port.in.ConfigurarBloqueo;
import io.github.jhoel0521.studyplan.application.port.in.ConsultarAjustes;
import io.github.jhoel0521.studyplan.domain.Ajustes;
import io.github.jhoel0521.studyplan.domain.Idioma;
import io.github.jhoel0521.studyplan.domain.Tema;

/**
 * Estado de la pantalla de ajustes: tema, idioma y bloqueo.
 *
 * <p>Implementa: HU-03 (tema e idioma) y HU-04 (activar el bloqueo, con aviso si el teléfono no tiene método).
 */
public class AjustesViewModel extends ViewModel {

    private final ConsultarAjustes consultarAjustes;
    private final ConfigurarApariencia apariencia;
    private final ConfigurarBloqueo bloqueo;
    private final Executor ejecutor;
    private final MutableLiveData<Ajustes> ajustes = new MutableLiveData<>();
    private final MutableLiveData<Boolean> avisoSinBloqueo = new MutableLiveData<>();

    public AjustesViewModel(ConsultarAjustes consultarAjustes, ConfigurarApariencia apariencia,
                            ConfigurarBloqueo bloqueo, Executor ejecutor) {
        this.consultarAjustes = consultarAjustes;
        this.apariencia = apariencia;
        this.bloqueo = bloqueo;
        this.ejecutor = ejecutor;
    }

    public LiveData<Ajustes> ajustes() {
        return ajustes;
    }

    /** Se publica {@code true} cuando se intentó activar el bloqueo y el teléfono no tiene método de bloqueo. */
    public LiveData<Boolean> avisoSinBloqueo() {
        return avisoSinBloqueo;
    }

    public void cargar() {
        ejecutor.execute(() -> ajustes.postValue(consultarAjustes.ejecutar()));
    }

    public void elegirTema(Tema tema) {
        ejecutor.execute(() -> {
            apariencia.cambiarTema(tema);
            ajustes.postValue(consultarAjustes.ejecutar());
        });
    }

    public void elegirIdioma(Idioma idioma) {
        ejecutor.execute(() -> {
            apariencia.cambiarIdioma(idioma);
            ajustes.postValue(consultarAjustes.ejecutar());
        });
    }

    public void cambiarBloqueo(boolean activar) {
        ejecutor.execute(() -> {
            if (activar) {
                if (bloqueo.activar() == ConfigurarBloqueo.ResultadoActivacion.SIN_METODO_DE_BLOQUEO) {
                    avisoSinBloqueo.postValue(true);
                }
            } else {
                bloqueo.desactivar();
            }
            ajustes.postValue(consultarAjustes.ejecutar());
        });
    }

    public void consumirAviso() {
        avisoSinBloqueo.setValue(null);
    }
}
