package io.github.jhoel0521.studyplan.adapter.in.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.Executor;

import io.github.jhoel0521.studyplan.application.port.in.ConfigurarAjustesEstudio;
import io.github.jhoel0521.studyplan.application.port.in.ConfigurarApariencia;
import io.github.jhoel0521.studyplan.application.port.in.ConfigurarBloqueo;
import io.github.jhoel0521.studyplan.application.port.in.ConsultarAjustes;
import io.github.jhoel0521.studyplan.domain.Ajustes;
import io.github.jhoel0521.studyplan.domain.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.Idioma;
import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.Tema;

/**
 * Estado de la pantalla de ajustes: tema, idioma, bloqueo, No molestar y Pomodoro.
 *
 * <p>Implementa: HU-03, HU-04, HU-23, HU-24, RN-17 y RN-18.
 */
public class AjustesViewModel extends ViewModel {

    private final ConsultarAjustes consultarAjustes;
    private final ConfigurarApariencia apariencia;
    private final ConfigurarBloqueo bloqueo;
    private final ConfigurarAjustesEstudio configurarEstudio;
    private final Executor ejecutor;
    private final MutableLiveData<Ajustes> ajustes = new MutableLiveData<>();
    private final MutableLiveData<Boolean> avisoSinBloqueo = new MutableLiveData<>();
    private final MutableLiveData<Boolean> pomodoroGuardado = new MutableLiveData<>();
    private final MutableLiveData<ErrorValidacion> errorPomodoro = new MutableLiveData<>();

    public AjustesViewModel(ConsultarAjustes consultarAjustes, ConfigurarApariencia apariencia,
                            ConfigurarBloqueo bloqueo, Executor ejecutor) {
        this(consultarAjustes, apariencia, bloqueo, null, ejecutor);
    }

    public AjustesViewModel(ConsultarAjustes consultarAjustes, ConfigurarApariencia apariencia,
                            ConfigurarBloqueo bloqueo, ConfigurarAjustesEstudio configurarEstudio,
                            Executor ejecutor) {
        this.consultarAjustes = consultarAjustes;
        this.apariencia = apariencia;
        this.bloqueo = bloqueo;
        this.configurarEstudio = configurarEstudio;
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

    public LiveData<Boolean> pomodoroGuardado() {
        return pomodoroGuardado;
    }

    public LiveData<ErrorValidacion> errorPomodoro() {
        return errorPomodoro;
    }

    public void guardarEstudio(
            boolean noMolestarGlobal,
            int trabajo,
            int corto,
            int largo,
            int ciclos,
            boolean notificarGuardado) {
        if (configurarEstudio == null) {
            return;
        }
        ejecutor.execute(() -> {
            Resultado<Ajustes> res = configurarEstudio.ejecutar(
                    noMolestarGlobal, trabajo, corto, largo, ciclos);
            if (!res.esValido()) {
                errorPomodoro.postValue(res.error());
                return;
            }
            errorPomodoro.postValue(null);
            ajustes.postValue(res.valor());
            if (notificarGuardado) {
                pomodoroGuardado.postValue(true);
            }
        });
    }

    public void consumirPomodoroGuardado() {
        pomodoroGuardado.setValue(null);
    }
}
