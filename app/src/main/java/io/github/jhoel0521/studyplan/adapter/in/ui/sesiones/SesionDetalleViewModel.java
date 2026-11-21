package io.github.jhoel0521.studyplan.adapter.in.ui.sesiones;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.jhoel0521.studyplan.application.port.in.sesiones.CompletarSesion;
import io.github.jhoel0521.studyplan.application.port.in.sesiones.ConsultarSesion;
import io.github.jhoel0521.studyplan.application.port.in.sesiones.EliminarSesion;
import io.github.jhoel0521.studyplan.application.port.in.tecnicas.IniciarSesion;
import io.github.jhoel0521.studyplan.application.port.in.sesiones.RegistrarSesionTardia;
import io.github.jhoel0521.studyplan.domain.readmodel.DetalleSesion;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;

/**
 * Estado de la pantalla de detalle de una sesión: lectura enriquecida, inicio en vivo, acciones
 * según máquina de estados y confirmación de borrado resiliente a rotación.
 *
 * <p>Implementa: HU-12, HU-13, HU-14, HU-15, HU-22 a HU-24, HU-39 y RNF-10.
 */
public class SesionDetalleViewModel extends ViewModel {

    private final ConsultarSesion consultarSesion;
    private final CompletarSesion completarSesion;
    private final RegistrarSesionTardia registrarSesionTardia;
    private final EliminarSesion eliminarSesion;
    private final IniciarSesion iniciarSesion;
    private final Executor ejecutor;

    private final MutableLiveData<DetalleSesion> detalle = new MutableLiveData<>();
    private final MutableLiveData<Boolean> noEncontrada = new MutableLiveData<>(false);
    private final MutableLiveData<EventoSesion> evento = new MutableLiveData<>();
    private final MutableLiveData<Boolean> confirmandoEliminar = new MutableLiveData<>(false);
    private final AtomicBoolean enCurso = new AtomicBoolean(false);

    public SesionDetalleViewModel(
            ConsultarSesion consultarSesion,
            CompletarSesion completarSesion,
            RegistrarSesionTardia registrarSesionTardia,
            EliminarSesion eliminarSesion,
            Executor ejecutor) {
        this(consultarSesion, completarSesion, registrarSesionTardia, eliminarSesion, null, ejecutor);
    }

    public SesionDetalleViewModel(
            ConsultarSesion consultarSesion,
            CompletarSesion completarSesion,
            RegistrarSesionTardia registrarSesionTardia,
            EliminarSesion eliminarSesion,
            IniciarSesion iniciarSesion,
            Executor ejecutor) {
        this.consultarSesion = consultarSesion;
        this.completarSesion = completarSesion;
        this.registrarSesionTardia = registrarSesionTardia;
        this.eliminarSesion = eliminarSesion;
        this.iniciarSesion = iniciarSesion;
        this.ejecutor = ejecutor;
    }

    public LiveData<DetalleSesion> detalle() {
        return detalle;
    }

    public LiveData<Boolean> noEncontrada() {
        return noEncontrada;
    }

    public LiveData<EventoSesion> evento() {
        return evento;
    }

    public LiveData<Boolean> confirmandoEliminar() {
        return confirmandoEliminar;
    }

    /** Consulta y publica el detalle completo de la sesión indicada. */
    public void cargar(long sesionId) {
        if (sesionId <= 0) {
            noEncontrada.postValue(true);
            return;
        }
        ejecutor.execute(() -> recargar(sesionId));
    }

    /** Inicia la sesión en vivo con la técnica y preferencia No molestar seleccionadas (HU-22 a HU-24). */
    public void iniciar(
            long sesionId, TecnicaEstudio tecnica, PreferenciaNoMolestar preferenciaNoMolestar) {
        if (iniciarSesion == null) {
            return;
        }
        ejecutarConCandado(() -> {
            Resultado<SesionEstudio> r = iniciarSesion.ejecutar(
                    sesionId, tecnica, preferenciaNoMolestar);
            if (r.esValido()) {
                recargar(sesionId);
                evento.postValue(EventoSesion.ok(EventoSesion.Tipo.INICIADA, sesionId));
            } else {
                evento.postValue(EventoSesion.error(r.error()));
            }
        });
    }

    /** Marca la sesión como completada en el mismo día con la duración real ingresada (HU-12). */
    public void completar(long sesionId, String duracionRealTxt) {
        int minutos = parsearEntero(duracionRealTxt);
        ejecutarConCandado(() -> {
            Resultado<SesionEstudio> r = completarSesion.ejecutar(sesionId, minutos);
            if (r.esValido()) {
                recargar(sesionId);
                evento.postValue(EventoSesion.ok(EventoSesion.Tipo.COMPLETADA, sesionId));
            } else {
                evento.postValue(EventoSesion.error(r.error()));
            }
        });
    }

    /** Registra la sesión como completada tardíamente dentro de los 3 días siguientes (HU-13). */
    public void registrarTardio(long sesionId, String duracionRealTxt) {
        int minutos = parsearEntero(duracionRealTxt);
        ejecutarConCandado(() -> {
            Resultado<SesionEstudio> r = registrarSesionTardia.ejecutar(sesionId, minutos);
            if (r.esValido()) {
                recargar(sesionId);
                evento.postValue(EventoSesion.ok(EventoSesion.Tipo.TARDIA_REGISTRADA, sesionId));
            } else {
                evento.postValue(EventoSesion.error(r.error()));
            }
        });
    }

    public void pedirEliminar() {
        confirmandoEliminar.setValue(true);
    }

    public void cancelarEliminar() {
        confirmandoEliminar.setValue(false);
    }

    /** Elimina una sesión en estado Pendiente tras confirmación del estudiante (HU-15). */
    public void confirmarEliminar(long sesionId) {
        confirmandoEliminar.setValue(false);
        ejecutarConCandado(() -> {
            Resultado<SesionEstudio> r = eliminarSesion.ejecutar(sesionId);
            if (r.esValido()) {
                evento.postValue(EventoSesion.ok(EventoSesion.Tipo.ELIMINADA, sesionId));
            } else {
                evento.postValue(EventoSesion.error(r.error()));
            }
        });
    }

    public void consumirEvento() {
        evento.setValue(null);
    }

    private void recargar(long sesionId) {
        Optional<DetalleSesion> encontrado = consultarSesion.ejecutar(sesionId);
        if (encontrado.isPresent()) {
            noEncontrada.postValue(false);
            detalle.postValue(encontrado.get());
        } else {
            noEncontrada.postValue(true);
        }
    }

    private void ejecutarConCandado(Runnable tarea) {
        if (!enCurso.compareAndSet(false, true)) {
            return;
        }
        ejecutor.execute(() -> {
            try {
                tarea.run();
            } finally {
                enCurso.set(false);
            }
        });
    }

    private static int parsearEntero(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
