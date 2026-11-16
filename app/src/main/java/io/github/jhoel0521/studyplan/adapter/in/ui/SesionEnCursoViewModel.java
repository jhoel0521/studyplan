package io.github.jhoel0521.studyplan.adapter.in.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;

import io.github.jhoel0521.studyplan.application.port.in.ConsultarEjecucionSesion;
import io.github.jhoel0521.studyplan.application.port.in.DecidirFinPlanificadoOInvasion;
import io.github.jhoel0521.studyplan.application.port.in.DetenerSesion;
import io.github.jhoel0521.studyplan.application.port.in.PausarReanudarSesion;
import io.github.jhoel0521.studyplan.domain.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.ResumenEjecucionSesion;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;

/**
 * ViewModel de la pantalla {@link SesionEnCursoActivity} (HU-22 a HU-24, HU-42, HU-43, RNF-02).
 */
public class SesionEnCursoViewModel extends ViewModel {

    private final ConsultarEjecucionSesion consultar;
    private final PausarReanudarSesion pausarReanudar;
    private final DetenerSesion detener;
    private final DecidirFinPlanificadoOInvasion decidir;
    private final Executor ejecutor;

    private final MutableLiveData<ResumenEjecucionSesion> resumen = new MutableLiveData<>();
    private final MutableLiveData<Boolean> finalizada = new MutableLiveData<>(false);
    private final MutableLiveData<Long> nuevaSesionActiva = new MutableLiveData<>();

    public SesionEnCursoViewModel(
            ConsultarEjecucionSesion consultar,
            PausarReanudarSesion pausarReanudar,
            DetenerSesion detener,
            DecidirFinPlanificadoOInvasion decidir,
            Executor ejecutor) {
        this.consultar = Objects.requireNonNull(consultar, "consultar");
        this.pausarReanudar = Objects.requireNonNull(pausarReanudar, "pausarReanudar");
        this.detener = Objects.requireNonNull(detener, "detener");
        this.decidir = Objects.requireNonNull(decidir, "decidir");
        this.ejecutor = Objects.requireNonNull(ejecutor, "ejecutor");
    }

    public LiveData<ResumenEjecucionSesion> resumen() {
        return resumen;
    }

    public LiveData<Boolean> finalizada() {
        return finalizada;
    }

    public LiveData<Long> nuevaSesionActiva() {
        return nuevaSesionActiva;
    }

    public void cargar(long sesionId) {
        if (sesionId <= 0) {
            finalizada.postValue(true);
            return;
        }
        ejecutor.execute(() -> recargar(sesionId));
    }

    public void pausarOReanudar(long sesionId) {
        ejecutor.execute(() -> {
            pausarReanudar.ejecutar(sesionId);
            recargar(sesionId);
        });
    }

    public void detener(long sesionId) {
        ejecutor.execute(() -> {
            Resultado<SesionEstudio> r = detener.ejecutar(sesionId);
            if (r.esValido()) {
                finalizada.postValue(true);
            }
        });
    }

    public void decidir(
            long sesionId,
            DecidirFinPlanificadoOInvasion.DecisionFinOInvasion decision,
            Long siguienteId) {
        ejecutor.execute(() -> {
            Resultado<SesionEstudio> r = decidir.ejecutar(sesionId, decision, siguienteId);
            if (!r.esValido()) {
                return;
            }
            if (decision == DecidirFinPlanificadoOInvasion.DecisionFinOInvasion.TERMINE) {
                finalizada.postValue(true);
            } else if (decision == DecidirFinPlanificadoOInvasion.DecisionFinOInvasion.CAMBIAR_A_SIGUIENTE
                    && r.valor().id() != null) {
                nuevaSesionActiva.postValue(r.valor().id());
            } else {
                recargar(sesionId);
            }
        });
    }

    private void recargar(long sesionId) {
        Optional<ResumenEjecucionSesion> opt = consultar.ejecutar(sesionId);
        if (!opt.isPresent()
                || opt.get().sesion().codigoEstado() != CodigoEstadoSesion.EN_CURSO) {
            finalizada.postValue(true);
            return;
        }
        resumen.postValue(opt.get());
    }
}
