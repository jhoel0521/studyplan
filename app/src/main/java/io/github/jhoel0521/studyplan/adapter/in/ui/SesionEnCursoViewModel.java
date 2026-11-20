package io.github.jhoel0521.studyplan.adapter.in.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;

import io.github.jhoel0521.studyplan.application.port.in.AlternarNoMolestarSesion;
import io.github.jhoel0521.studyplan.application.port.in.CambiarDeMateria;
import io.github.jhoel0521.studyplan.application.port.in.ConsultarEjecucionSesion;
import io.github.jhoel0521.studyplan.application.port.in.DecidirFinPlanificadoOInvasion;
import io.github.jhoel0521.studyplan.application.port.in.DetenerSesion;
import io.github.jhoel0521.studyplan.application.port.in.IniciarSesion;
import io.github.jhoel0521.studyplan.application.port.in.ListarMaterias;
import io.github.jhoel0521.studyplan.application.port.in.PausarReanudarSesion;
import io.github.jhoel0521.studyplan.domain.CierreSesion;
import io.github.jhoel0521.studyplan.domain.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.MetaAlcanzada;
import io.github.jhoel0521.studyplan.domain.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.ResumenEjecucionSesion;
import io.github.jhoel0521.studyplan.domain.ResumenMateria;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.TecnicaEstudio;

/**
 * ViewModel de la pantalla {@link SesionEnCursoActivity} (HU-22 a HU-24, HU-28, HU-34, HU-42,
 * HU-43, HU-48, RN-16, RNF-02).
 */
public class SesionEnCursoViewModel extends ViewModel {

    private final ConsultarEjecucionSesion consultar;
    private final PausarReanudarSesion pausarReanudar;
    private final DetenerSesion detener;
    private final DecidirFinPlanificadoOInvasion decidir;
    private final CambiarDeMateria cambiarDeMateria;
    private final ListarMaterias listarMaterias;
    private final IniciarSesion iniciarSesion;
    private final Executor ejecutor;
    private AlternarNoMolestarSesion alternarNoMolestar;

    private final MutableLiveData<ResumenEjecucionSesion> resumen = new MutableLiveData<>();
    private final MutableLiveData<Boolean> finalizada = new MutableLiveData<>(false);
    private final MutableLiveData<Long> nuevaSesionActiva = new MutableLiveData<>();
    private final MutableLiveData<List<MetaAlcanzada>> metasAlcanzadas = new MutableLiveData<>();
    private final MutableLiveData<List<Materia>> materiasSelector = new MutableLiveData<>();
    private final MutableLiveData<ErrorValidacion> errorInicioDirecto = new MutableLiveData<>();
    private final MutableLiveData<ErrorValidacion> errorNoMolestar = new MutableLiveData<>();

    public SesionEnCursoViewModel(
            ConsultarEjecucionSesion consultar,
            PausarReanudarSesion pausarReanudar,
            DetenerSesion detener,
            DecidirFinPlanificadoOInvasion decidir,
            Executor ejecutor) {
        this(consultar, pausarReanudar, detener, decidir, null, null, null, ejecutor);
    }

    public SesionEnCursoViewModel(
            ConsultarEjecucionSesion consultar,
            PausarReanudarSesion pausarReanudar,
            DetenerSesion detener,
            DecidirFinPlanificadoOInvasion decidir,
            CambiarDeMateria cambiarDeMateria,
            ListarMaterias listarMaterias,
            Executor ejecutor) {
        this(
                consultar,
                pausarReanudar,
                detener,
                decidir,
                cambiarDeMateria,
                listarMaterias,
                null,
                ejecutor);
    }

    public SesionEnCursoViewModel(
            ConsultarEjecucionSesion consultar,
            PausarReanudarSesion pausarReanudar,
            DetenerSesion detener,
            DecidirFinPlanificadoOInvasion decidir,
            CambiarDeMateria cambiarDeMateria,
            ListarMaterias listarMaterias,
            IniciarSesion iniciarSesion,
            Executor ejecutor) {
        this.consultar = Objects.requireNonNull(consultar, "consultar");
        this.pausarReanudar = Objects.requireNonNull(pausarReanudar, "pausarReanudar");
        this.detener = Objects.requireNonNull(detener, "detener");
        this.decidir = Objects.requireNonNull(decidir, "decidir");
        this.cambiarDeMateria = cambiarDeMateria;
        this.listarMaterias = listarMaterias;
        this.iniciarSesion = iniciarSesion;
        this.ejecutor = Objects.requireNonNull(ejecutor, "ejecutor");
    }

    /** Conecta el caso de uso que activa o desactiva No molestar en la sesión (HU-24). */
    public SesionEnCursoViewModel conAlternarNoMolestar(AlternarNoMolestarSesion alternar) {
        this.alternarNoMolestar = alternar;
        return this;
    }

    public LiveData<ErrorValidacion> errorNoMolestar() {
        return errorNoMolestar;
    }

    public void consumirErrorNoMolestar() {
        errorNoMolestar.setValue(null);
    }

    /** Activa o desactiva No molestar solo para esta sesión (HU-24, RN-18). */
    public void alternarNoMolestar(long sesionId) {
        if (alternarNoMolestar == null) {
            return;
        }
        ejecutor.execute(() -> {
            Resultado<Boolean> resultado = alternarNoMolestar.ejecutar(sesionId);
            if (!resultado.esValido()) {
                errorNoMolestar.postValue(resultado.error());
            }
            recargar(sesionId);
        });
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

    public LiveData<List<MetaAlcanzada>> metasAlcanzadas() {
        return metasAlcanzadas;
    }

    public LiveData<List<Materia>> materiasSelector() {
        return materiasSelector;
    }

    public LiveData<ErrorValidacion> errorInicioDirecto() {
        return errorInicioDirecto;
    }

    public void consumirErrorInicioDirecto() {
        errorInicioDirecto.setValue(null);
    }

    public void cargar(long sesionId) {
        if (sesionId <= 0) {
            finalizada.postValue(true);
            return;
        }
        ejecutor.execute(() -> recargar(sesionId));
    }

    /**
     * Inicia directamente una sesión {@code PENDIENTE} desde el botón «Iniciar» de un recordatorio
     * con técnica {@link TecnicaEstudio#LIBRE} y preferencia {@link PreferenciaNoMolestar#HEREDAR_GLOBAL},
     * o la reabre si ya está {@code EN_CURSO} ({@code HU-34}).
     */
    public void cargarOIniciarDesdeRecordatorio(long sesionId) {
        if (sesionId <= 0) {
            finalizada.postValue(true);
            return;
        }
        ejecutor.execute(() -> {
            Optional<ResumenEjecucionSesion> actual = consultar.ejecutar(sesionId);
            if (actual.isPresent()
                    && actual.get().sesion().codigoEstado() == CodigoEstadoSesion.EN_CURSO) {
                resumen.postValue(actual.get());
                return;
            }
            if (iniciarSesion == null) {
                recargar(sesionId);
                return;
            }
            Resultado<SesionEstudio> res = iniciarSesion.ejecutar(
                    sesionId, TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL);
            if (res.esValido()) {
                recargar(sesionId);
            } else {
                errorInicioDirecto.postValue(res.error());
            }
        });
    }

    public void cargarMateriasParaCambio() {
        if (listarMaterias == null) {
            return;
        }
        ejecutor.execute(() -> {
            List<ResumenMateria> activas = listarMaterias.ejecutar().activas();
            List<Materia> lista = new ArrayList<>(activas.size());
            for (ResumenMateria rm : activas) {
                lista.add(rm.materia());
            }
            materiasSelector.postValue(lista);
        });
    }

    public void cambiarDeMateria(long sesionId, Long nuevaMateriaId, TecnicaEstudio tecnica) {
        if (cambiarDeMateria == null) {
            return;
        }
        ejecutor.execute(() -> {
            Resultado<SesionEstudio> r = cambiarDeMateria.ejecutar(sesionId, nuevaMateriaId, tecnica);
            if (r.esValido() && r.valor().id() != null) {
                nuevaSesionActiva.postValue(r.valor().id());
            }
        });
    }

    public void cambiarACreada(
            long sesionId, String nombreMateria, int colorMateria, TecnicaEstudio tecnica) {
        if (cambiarDeMateria == null) {
            return;
        }
        ejecutor.execute(() -> {
            Resultado<SesionEstudio> r = cambiarDeMateria.ejecutarConNuevaMateria(
                    sesionId, nombreMateria, colorMateria, tecnica);
            if (r.esValido() && r.valor().id() != null) {
                nuevaSesionActiva.postValue(r.valor().id());
            }
        });
    }

    public void pausarOReanudar(long sesionId) {
        ejecutor.execute(() -> {
            pausarReanudar.ejecutar(sesionId);
            recargar(sesionId);
        });
    }

    public void reanudarConTopico(
            long sesionId,
            io.github.jhoel0521.studyplan.domain.TopicoEventoEstudio topico,
            String nota) {
        ejecutor.execute(() -> {
            pausarReanudar.ejecutar(sesionId, topico, nota);
            recargar(sesionId);
        });
    }

    public void detener(long sesionId) {
        ejecutor.execute(() -> {
            Resultado<CierreSesion> r = detener.ejecutarConMetas(sesionId);
            if (r.esValido()) {
                List<MetaAlcanzada> alcanzadas = r.valor().metasAlcanzadas();
                if (!alcanzadas.isEmpty()) {
                    metasAlcanzadas.postValue(alcanzadas);
                } else {
                    finalizada.postValue(true);
                }
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
