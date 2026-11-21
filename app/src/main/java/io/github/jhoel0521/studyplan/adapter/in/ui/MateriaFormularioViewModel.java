package io.github.jhoel0521.studyplan.adapter.in.ui;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import io.github.jhoel0521.studyplan.application.port.in.materias.ArchivarMateria;
import io.github.jhoel0521.studyplan.application.port.in.materias.ConsultarMateria;
import io.github.jhoel0521.studyplan.application.port.in.materias.DesarchivarMateria;
import io.github.jhoel0521.studyplan.application.port.in.materias.EditarMateria;
import io.github.jhoel0521.studyplan.application.port.in.materias.EliminarMateria;
import io.github.jhoel0521.studyplan.application.port.in.materias.RegistrarMateria;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Estado del formulario de materia: cargar una existente, registrar o editar, archivar, desarchivar y eliminar (esta
 * última con una confirmación previa modelada como estado).
 *
 * <p>Implementa: HU-06 (registrar), HU-07 (editar), HU-08 (archivar y eliminar), HU-46 (desarchivar) y RNF-14.
 */
public class MateriaFormularioViewModel extends ViewModel {

    private final RegistrarMateria registrar;
    private final EditarMateria editar;
    private final ConsultarMateria consultar;
    private final ArchivarMateria archivar;
    private final DesarchivarMateria desarchivar;
    private final EliminarMateria eliminar;
    private final Executor ejecutor;
    private final MutableLiveData<Materia> materia = new MutableLiveData<>();
    private final MutableLiveData<EventoMateria> evento = new MutableLiveData<>();
    private final MutableLiveData<Boolean> confirmandoEliminar = new MutableLiveData<>(false);
    private final AtomicBoolean enCurso = new AtomicBoolean(false);

    public MateriaFormularioViewModel(RegistrarMateria registrar, EditarMateria editar, ConsultarMateria consultar,
                                      ArchivarMateria archivar, DesarchivarMateria desarchivar,
                                      EliminarMateria eliminar, Executor ejecutor) {
        this.registrar = registrar;
        this.editar = editar;
        this.consultar = consultar;
        this.archivar = archivar;
        this.desarchivar = desarchivar;
        this.eliminar = eliminar;
        this.ejecutor = ejecutor;
    }

    /** La materia cargada para editar (nula mientras no se cargue o si no existe). */
    public LiveData<Materia> materia() {
        return materia;
    }

    /** El último resultado de una acción; nulo si ya se atendió. */
    public LiveData<EventoMateria> evento() {
        return evento;
    }

    /** {@code true} mientras se muestra la confirmación de eliminar. */
    public LiveData<Boolean> confirmandoEliminar() {
        return confirmandoEliminar;
    }

    public void cargar(long id) {
        ejecutor.execute(() -> consultar.ejecutar(id).ifPresent(materia::postValue));
    }

    /** Registra una materia nueva (si {@code id} es nulo) o edita la existente. */
    public void guardar(Long id, String nombre, int color, String docente) {
        guardar(id, nombre, color, docente, "");
    }

    /** Registra o edita una materia con meta semanal opcional ({@code HU-26}, {@code RN-14}). */
    public void guardar(Long id, String nombre, int color, String docente, String metaTexto) {
        Double metaHoras = null;
        String limpio = metaTexto == null ? "" : metaTexto.trim();
        if (!limpio.isEmpty()) {
            try {
                metaHoras = Double.parseDouble(limpio.replace(',', '.'));
            } catch (NumberFormatException e) {
                evento.setValue(EventoMateria.error(
                        io.github.jhoel0521.studyplan.domain.result.ErrorValidacion.META_FUERA_DE_RANGO));
                return;
            }
        }
        final Double metaFinal = metaHoras;
        ejecutar(() -> {
            Resultado<Materia> r = id == null
                    ? registrar.ejecutar(nombre, color, docente, metaFinal)
                    : editar.ejecutar(id, nombre, color, docente, metaFinal);
            publicar(r, EventoMateria.Tipo.GUARDADA);
        });
    }

    public void archivar(long id) {
        ejecutar(() -> publicar(archivar.ejecutar(id), EventoMateria.Tipo.ARCHIVADA));
    }

    public void desarchivar(long id) {
        ejecutar(() -> publicar(desarchivar.ejecutar(id), EventoMateria.Tipo.DESARCHIVADA));
    }

    /** El estudiante pulsó «Eliminar»: se pide confirmación y todavía no se borra nada. */
    public void pedirEliminar() {
        confirmandoEliminar.setValue(true);
    }

    public void cancelarEliminar() {
        confirmandoEliminar.setValue(false);
    }

    public void confirmarEliminar(long id) {
        confirmandoEliminar.setValue(false);
        ejecutar(() -> publicar(eliminar.ejecutar(id), EventoMateria.Tipo.ELIMINADA));
    }

    /** Marca el evento como atendido para que una rotación no lo repita. */
    public void consumirEvento() {
        evento.setValue(null);
    }

    /**
     * Ejecuta una acción en segundo plano solo si no hay otra en curso: un doble toque en «Guardar» no registra dos
     * veces ni pierde el evento de éxito (con {@code postValue} el segundo resultado pisaría al primero).
     */
    private void ejecutar(Runnable accion) {
        if (!enCurso.compareAndSet(false, true)) {
            return;
        }
        ejecutor.execute(() -> {
            try {
                accion.run();
            } finally {
                enCurso.set(false);
            }
        });
    }

    private void publicar(Resultado<Materia> resultado, EventoMateria.Tipo exito) {
        evento.postValue(resultado.esValido() ? EventoMateria.de(exito) : EventoMateria.error(resultado.error()));
    }
}
