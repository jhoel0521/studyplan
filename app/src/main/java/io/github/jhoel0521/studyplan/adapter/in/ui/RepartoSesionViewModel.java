package io.github.jhoel0521.studyplan.adapter.in.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;

import io.github.jhoel0521.studyplan.application.port.in.sesiones.ConsultarSesion;
import io.github.jhoel0521.studyplan.application.port.in.tecnicas.RepartirTramoSesion;
import io.github.jhoel0521.studyplan.application.port.out.EventoEstudioRepository;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.domain.service.CalculadorRepartoTramo;
import io.github.jhoel0521.studyplan.domain.service.CalculadorTiempoReal;
import io.github.jhoel0521.studyplan.domain.readmodel.DetalleSesion;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.valueobject.IntervaloEfectivo;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.readmodel.ResultadoReparto;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;

/**
 * ViewModel de {@link RepartoSesionActivity} ({@code W14}, HU-44, RN-28).
 */
public class RepartoSesionViewModel extends ViewModel {

    private static final long CINCO_MIN_MS = 5L * 60_000L;

    public static final class EstadoReparto {
        public final String nombrePrimera;
        public final String nombreSegunda;
        public final int colorPrimera;
        public final int colorSegunda;
        public final ResultadoReparto reparto;

        public EstadoReparto(
                String nombrePrimera,
                String nombreSegunda,
                int colorPrimera,
                int colorSegunda,
                ResultadoReparto reparto) {
            this.nombrePrimera = nombrePrimera;
            this.nombreSegunda = nombreSegunda;
            this.colorPrimera = colorPrimera;
            this.colorSegunda = colorSegunda;
            this.reparto = reparto;
        }
    }

    private final ConsultarSesion consultarSesion;
    private final EventoEstudioRepository eventosRepo;
    private final RepartirTramoSesion repartirTramo;
    private final Reloj reloj;
    private final Executor ejecutor;

    private final MutableLiveData<EstadoReparto> estado = new MutableLiveData<>();
    private final MutableLiveData<ErrorValidacion> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> guardado = new MutableLiveData<>(false);

    private long sesionActualId;
    private long sesionSiguienteId;
    private SesionEstudio sesionActual;
    private String nombrePrimera = "";
    private String nombreSegunda = "";
    private int colorPrimera;
    private int colorSegunda;
    private List<EventoEstudio> eventos;
    private long ahoraCargaMs;
    private long corteActualMs;

    public RepartoSesionViewModel(
            ConsultarSesion consultarSesion,
            EventoEstudioRepository eventosRepo,
            RepartirTramoSesion repartirTramo,
            Reloj reloj,
            Executor ejecutor) {
        this.consultarSesion = Objects.requireNonNull(consultarSesion, "consultarSesion");
        this.eventosRepo = Objects.requireNonNull(eventosRepo, "eventosRepo");
        this.repartirTramo = Objects.requireNonNull(repartirTramo, "repartirTramo");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.ejecutor = Objects.requireNonNull(ejecutor, "ejecutor");
    }

    public LiveData<EstadoReparto> estado() {
        return estado;
    }

    public LiveData<ErrorValidacion> error() {
        return error;
    }

    public LiveData<Boolean> guardado() {
        return guardado;
    }

    public void cargar(long actualId, long siguienteId) {
        if (actualId <= 0 || siguienteId <= 0) {
            error.postValue(ErrorValidacion.SESION_NO_ENCONTRADA);
            return;
        }
        this.sesionActualId = actualId;
        this.sesionSiguienteId = siguienteId;
        ejecutor.execute(() -> {
            Optional<DetalleSesion> det1 = consultarSesion.ejecutar(actualId);
            Optional<DetalleSesion> det2 = consultarSesion.ejecutar(siguienteId);
            if (!det1.isPresent() || !det2.isPresent()) {
                error.postValue(ErrorValidacion.SESION_NO_ENCONTRADA);
                return;
            }
            sesionActual = det1.get().sesion();
            nombrePrimera = det1.get().nombreMateria();
            colorPrimera = det1.get().colorMateria();
            nombreSegunda = det2.get().nombreMateria();
            colorSegunda = det2.get().colorMateria();
            eventos = eventosRepo.listarPorSesion(actualId);
            ahoraCargaMs = reloj.ahoraMilis();

            List<IntervaloEfectivo> ivs = CalculadorTiempoReal.reconstruirIntervalos(
                    eventos, ahoraCargaMs);
            if (ivs.isEmpty()) {
                error.postValue(ErrorValidacion.CORTE_REPARTO_INVALIDO);
                return;
            }
            long inicio = ivs.get(0).inicioMilis();
            long fin = ivs.get(ivs.size() - 1).finMilis();
            long finPlan = reloj.instanteDe(sesionActual.fecha(), sesionActual.horaFin());
            corteActualMs = (finPlan > inicio && finPlan < fin)
                    ? finPlan
                    : inicio + (fin - inicio) / 2L;
            publicarPreview();
        });
    }

    public void fijarCorteMilis(long nuevoCorteMs) {
        this.corteActualMs = nuevoCorteMs;
        publicarPreview();
    }

    public void desplazarCorteMinutos(int deltaMinutos) {
        if (estado.getValue() == null) {
            return;
        }
        ResultadoReparto rep = estado.getValue().reparto;
        long propuesto = corteActualMs + deltaMinutos * 60_000L;
        long min = rep.inicioTramoMilis() + CINCO_MIN_MS;
        long max = rep.finTramoMilis() - CINCO_MIN_MS;
        if (min > max) {
            min = rep.inicioTramoMilis() + 60_000L;
            max = rep.finTramoMilis() - 60_000L;
        }
        corteActualMs = Math.max(min, Math.min(max, propuesto));
        publicarPreview();
    }

    public void confirmar() {
        ejecutor.execute(() -> {
            Resultado<ResultadoReparto> res = repartirTramo.ejecutar(
                    sesionActualId, sesionSiguienteId, corteActualMs);
            if (res.esValido()) {
                error.postValue(null);
                guardado.postValue(true);
            } else {
                error.postValue(res.error());
            }
        });
    }

    private void publicarPreview() {
        if (sesionActual == null || eventos == null) {
            return;
        }
        Resultado<ResultadoReparto> res = CalculadorRepartoTramo.repartir(
                sesionActual.tecnica(),
                sesionActual.configuracionPomodoroSnapshot(),
                sesionActual.duracionPlanificadaMinutos(),
                eventos,
                ahoraCargaMs,
                corteActualMs);
        if (res.esValido()) {
            error.postValue(null);
            estado.postValue(new EstadoReparto(
                    nombrePrimera, nombreSegunda, colorPrimera, colorSegunda, res.valor()));
        } else {
            error.postValue(res.error());
        }
    }
}
