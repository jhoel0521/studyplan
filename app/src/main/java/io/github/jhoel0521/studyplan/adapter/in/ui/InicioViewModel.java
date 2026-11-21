package io.github.jhoel0521.studyplan.adapter.in.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import io.github.jhoel0521.studyplan.application.port.in.DefinirMetaSemanal;
import io.github.jhoel0521.studyplan.application.port.in.EstudiarAhora;
import io.github.jhoel0521.studyplan.application.port.in.IniciarSesion;
import io.github.jhoel0521.studyplan.application.port.in.ListarMaterias;
import io.github.jhoel0521.studyplan.application.port.in.ObtenerResumenInicio;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.entity.MetaSemanal;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.readmodel.ResumenInicio;
import io.github.jhoel0521.studyplan.domain.readmodel.ResumenMateria;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;

/**
 * Estado de la pantalla de Inicio: saludo, progreso semanal real, próxima sesión pendiente,
 * sesiones del día actual, definición rápida de meta semanal y arranque «Estudiar ahora».
 *
 * <p>Implementa: HU-02, HU-25, HU-47.
 */
public class InicioViewModel extends ViewModel {

    private final ObtenerResumenInicio obtenerResumen;
    private final ListarMaterias listarMaterias;
    private final DefinirMetaSemanal definirMeta;
    private final EstudiarAhora estudiarAhora;
    private final IniciarSesion iniciarSesion;
    private final Executor ejecutor;
    private final MutableLiveData<ResumenInicio> resumen = new MutableLiveData<>();
    private final MutableLiveData<Boolean> hayMateriasActivas = new MutableLiveData<>(false);
    private final MutableLiveData<List<Materia>> materiasSelector = new MutableLiveData<>();
    private final MutableLiveData<Boolean> metaActualizada = new MutableLiveData<>();
    private final MutableLiveData<ErrorValidacion> errorAccion = new MutableLiveData<>();
    private final MutableLiveData<Long> sesionIniciadaId = new MutableLiveData<>();

    public InicioViewModel(
            ObtenerResumenInicio obtenerResumen,
            ListarMaterias listarMaterias,
            Executor ejecutor) {
        this(obtenerResumen, listarMaterias, null, null, null, ejecutor);
    }

    public InicioViewModel(
            ObtenerResumenInicio obtenerResumen,
            ListarMaterias listarMaterias,
            DefinirMetaSemanal definirMeta,
            EstudiarAhora estudiarAhora,
            IniciarSesion iniciarSesion,
            Executor ejecutor) {
        this.obtenerResumen = obtenerResumen;
        this.listarMaterias = listarMaterias;
        this.definirMeta = definirMeta;
        this.estudiarAhora = estudiarAhora;
        this.iniciarSesion = iniciarSesion;
        this.ejecutor = ejecutor;
    }

    public LiveData<ResumenInicio> resumen() {
        return resumen;
    }

    public LiveData<Boolean> hayMateriasActivas() {
        return hayMateriasActivas;
    }

    public LiveData<List<Materia>> materiasSelector() {
        return materiasSelector;
    }

    public LiveData<Boolean> metaActualizada() {
        return metaActualizada;
    }

    public LiveData<ErrorValidacion> errorAccion() {
        return errorAccion;
    }

    public LiveData<Long> sesionIniciadaId() {
        return sesionIniciadaId;
    }

    /** Lee el resumen y el estado de materias activas en segundo plano; sin perfil no publica resumen. */
    public void cargar() {
        ejecutor.execute(() -> obtenerResumen.ejecutar().ifPresent(r -> {
            List<ResumenMateria> activas = listarMaterias.ejecutar().activas();
            List<Materia> listaMaterias = new ArrayList<>(activas.size());
            boolean tieneNoGenerales = false;
            for (ResumenMateria rm : activas) {
                listaMaterias.add(rm.materia());
                if (!rm.materia().esGeneral()) {
                    tieneNoGenerales = true;
                }
            }
            hayMateriasActivas.postValue(tieneNoGenerales);
            materiasSelector.postValue(listaMaterias);
            resumen.postValue(r);
        }));
    }

    public void definirMetaSemana(String textoHoras) {
        if (definirMeta == null) {
            return;
        }
        double horas;
        try {
            horas = Double.parseDouble(textoHoras == null ? "" : textoHoras.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            errorAccion.setValue(ErrorValidacion.META_FUERA_DE_RANGO);
            return;
        }
        ejecutor.execute(() -> {
            Resultado<MetaSemanal> res = definirMeta.ejecutar(horas);
            if (!res.esValido()) {
                errorAccion.postValue(res.error());
                return;
            }
            metaActualizada.postValue(true);
            cargar();
        });
    }

    public void iniciarEstudiarAhora(Long materiaId, TecnicaEstudio tecnica) {
        if (estudiarAhora == null) {
            return;
        }
        ejecutor.execute(() -> {
            Resultado<SesionEstudio> res = estudiarAhora.ejecutar(
                    materiaId, tecnica, PreferenciaNoMolestar.HEREDAR_GLOBAL);
            if (!res.esValido()) {
                errorAccion.postValue(res.error());
            } else {
                sesionIniciadaId.postValue(res.valor().id());
            }
        });
    }

    public void iniciarEstudiarAhoraConNuevaMateria(
            String nombre, int color, TecnicaEstudio tecnica) {
        if (estudiarAhora == null) {
            return;
        }
        ejecutor.execute(() -> {
            Resultado<SesionEstudio> res = estudiarAhora.ejecutarConNuevaMateria(
                    nombre, color, tecnica, PreferenciaNoMolestar.HEREDAR_GLOBAL);
            if (!res.esValido()) {
                errorAccion.postValue(res.error());
            } else {
                sesionIniciadaId.postValue(res.valor().id());
            }
        });
    }

    public void iniciarSesionPlanificada(long sesionId, TecnicaEstudio tecnica) {
        if (iniciarSesion == null) {
            return;
        }
        ejecutor.execute(() -> {
            Resultado<SesionEstudio> res = iniciarSesion.ejecutar(
                    sesionId, tecnica, PreferenciaNoMolestar.HEREDAR_GLOBAL);
            if (!res.esValido()) {
                errorAccion.postValue(res.error());
            } else {
                sesionIniciadaId.postValue(res.valor().id());
            }
        });
    }

    public void consumirEventos() {
        metaActualizada.setValue(null);
        errorAccion.setValue(null);
        sesionIniciadaId.setValue(null);
    }
}
