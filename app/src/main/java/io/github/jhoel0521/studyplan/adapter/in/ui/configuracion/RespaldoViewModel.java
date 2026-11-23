package io.github.jhoel0521.studyplan.adapter.in.ui.configuracion;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.Executor;

import io.github.jhoel0521.studyplan.application.port.in.configuracion.ConfigurarRespaldo;
import io.github.jhoel0521.studyplan.application.port.in.configuracion.RespaldarAhora;
import io.github.jhoel0521.studyplan.application.port.in.configuracion.RestaurarRespaldo;
import io.github.jhoel0521.studyplan.domain.enumeration.FrecuenciaRespaldo;
import io.github.jhoel0521.studyplan.domain.readmodel.ConfiguracionRespaldo;
import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;

/**
 * Estado de la pantalla Copia de seguridad y de «Restaurar desde Google Drive» en el primer uso. Todo el trabajo
 * corre en el hilo en segundo plano; la pantalla solo observa la configuración, si hay una operación en curso y el
 * último evento.
 *
 * <p>Implementa: HU-35, HU-36 y HU-37.
 */
public class RespaldoViewModel extends ViewModel {

    /** Qué terminó. */
    public enum Tipo { VINCULADA, DESVINCULADA, CONTRASENA_GUARDADA, RESPALDADO, RESTAURADO, ERROR }

    /** Un suceso de una sola vez para la pantalla (mostrar mensaje, navegar). */
    public static final class Evento {
        public final Tipo tipo;
        public final ResultadoRespaldo resultado;

        Evento(Tipo tipo, ResultadoRespaldo resultado) {
            this.tipo = tipo;
            this.resultado = resultado;
        }
    }

    private final ConfigurarRespaldo configurar;
    private final RespaldarAhora respaldarAhora;
    private final RestaurarRespaldo restaurarRespaldo;
    private final Executor ejecutor;
    private final MutableLiveData<ConfiguracionRespaldo> configuracion = new MutableLiveData<>();
    private final MutableLiveData<Boolean> ocupado = new MutableLiveData<>(false);
    private final MutableLiveData<Evento> evento = new MutableLiveData<>();

    public RespaldoViewModel(
            ConfigurarRespaldo configurar,
            RespaldarAhora respaldarAhora,
            RestaurarRespaldo restaurarRespaldo,
            Executor ejecutor) {
        this.configurar = configurar;
        this.respaldarAhora = respaldarAhora;
        this.restaurarRespaldo = restaurarRespaldo;
        this.ejecutor = ejecutor;
    }

    public LiveData<ConfiguracionRespaldo> configuracion() {
        return configuracion;
    }

    public LiveData<Boolean> ocupado() {
        return ocupado;
    }

    public LiveData<Evento> evento() {
        return evento;
    }

    public void consumirEvento() {
        evento.setValue(null);
    }

    /** Lee la configuración actual. */
    public void cargar() {
        ejecutor.execute(() -> configuracion.postValue(configurar.consultar()));
    }

    /** La cuenta ya autorizó el acceso a Drive: se guarda su correo. */
    public void vincular(String correo) {
        ejecutor.execute(() -> {
            configurar.vincularCuenta(correo);
            publicar(Tipo.VINCULADA, ResultadoRespaldo.OK);
        });
    }

    public void desvincular() {
        ejecutor.execute(() -> {
            configurar.desvincular();
            publicar(Tipo.DESVINCULADA, ResultadoRespaldo.OK);
        });
    }

    public void cambiarFrecuencia(FrecuenciaRespaldo frecuencia) {
        ejecutor.execute(() -> {
            ResultadoRespaldo resultado = configurar.cambiarFrecuencia(frecuencia);
            publicar(resultado == ResultadoRespaldo.OK ? null : Tipo.ERROR, resultado);
        });
    }

    public void definirContrasena(char[] contrasena) {
        ejecutor.execute(() -> {
            ResultadoRespaldo resultado = configurar.definirContrasena(contrasena);
            publicar(resultado == ResultadoRespaldo.OK ? Tipo.CONTRASENA_GUARDADA : Tipo.ERROR, resultado);
        });
    }

    public void quitarContrasena() {
        ejecutor.execute(() -> {
            configurar.quitarContrasena();
            publicar(Tipo.CONTRASENA_GUARDADA, ResultadoRespaldo.OK);
        });
    }

    public void respaldarAhora() {
        ocupado.setValue(true);
        ejecutor.execute(() -> {
            ResultadoRespaldo resultado = respaldarAhora.ejecutar();
            ocupado.postValue(false);
            publicar(resultado == ResultadoRespaldo.OK ? Tipo.RESPALDADO : Tipo.ERROR, resultado);
        });
    }

    /** Restaura con la cuenta ya vinculada; {@code contrasena} es {@code null} si el respaldo no pide ninguna. */
    public void restaurar(char[] contrasena) {
        ocupado.setValue(true);
        ejecutor.execute(() -> {
            ResultadoRespaldo resultado = restaurarRespaldo.ejecutar(contrasena);
            ocupado.postValue(false);
            publicar(resultado == ResultadoRespaldo.OK ? Tipo.RESTAURADO : Tipo.ERROR, resultado);
        });
    }

    /**
     * Primer uso en un teléfono nuevo: vincula la cuenta recién autorizada y restaura. Si no hay respaldo o falla por
     * algo que no sea la contraseña, la cuenta se desvincula para dejar la app como recién instalada.
     */
    public void restaurarConCuentaNueva(String correo) {
        ocupado.setValue(true);
        ejecutor.execute(() -> {
            configurar.vincularCuenta(correo);
            ResultadoRespaldo resultado = restaurarRespaldo.ejecutar(null);
            boolean pideContrasena = resultado == ResultadoRespaldo.CONTRASENA_REQUERIDA
                    || resultado == ResultadoRespaldo.CONTRASENA_INCORRECTA;
            if (resultado != ResultadoRespaldo.OK && !pideContrasena) {
                configurar.desvincular();
            }
            ocupado.postValue(false);
            publicar(resultado == ResultadoRespaldo.OK ? Tipo.RESTAURADO : Tipo.ERROR, resultado);
        });
    }

    private void publicar(Tipo tipo, ResultadoRespaldo resultado) {
        configuracion.postValue(configurar.consultar());
        if (tipo != null) {
            evento.postValue(new Evento(tipo, resultado));
        }
    }
}
