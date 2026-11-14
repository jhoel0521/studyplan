package io.github.jhoel0521.studyplan.adapter.in.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import io.github.jhoel0521.studyplan.application.port.in.ListarMaterias;
import io.github.jhoel0521.studyplan.domain.ListadoMaterias;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.ResumenMateria;

/**
 * Estado de la pantalla de materias: las filas (activas, encabezado de archivadas y, si está desplegado, las
 * archivadas) y si no existe ninguna materia.
 *
 * <p>Implementa: HU-09 (Lista y Lista vacía) y HU-08 (sección «Archivadas»).
 */
public class ListaMateriasViewModel extends ViewModel {

    private final ListarMaterias listarMaterias;
    private final Executor ejecutor;
    private final MutableLiveData<List<FilaMateria>> filas = new MutableLiveData<>();
    private final MutableLiveData<Boolean> vacia = new MutableLiveData<>();
    private volatile ListadoMaterias ultimo;
    /** Solo se lee y se escribe en el ejecutor de un solo hilo (cargar y alternar), por eso no necesita ser volátil. */
    private boolean archivadasDesplegadas;

    public ListaMateriasViewModel(ListarMaterias listarMaterias, Executor ejecutor) {
        this.listarMaterias = listarMaterias;
        this.ejecutor = ejecutor;
    }

    public LiveData<List<FilaMateria>> filas() {
        return filas;
    }

    /** {@code true} cuando no existe ninguna materia, ni activa ni archivada. */
    public LiveData<Boolean> vacia() {
        return vacia;
    }

    /** Lee las materias en segundo plano y publica las filas. */
    public void cargar() {
        ejecutor.execute(() -> {
            ListadoMaterias listado = listarMaterias.ejecutar();
            ultimo = listado;
            vacia.postValue(listado.estaVacio());
            filas.postValue(construirFilas(listado, archivadasDesplegadas));
        });
    }

    /** Despliega o pliega la sección de archivadas. */
    public void alternarArchivadas() {
        // Se hace en el mismo ejecutor que cargar(): así no se pisan un toque y una lectura a la vez.
        ejecutor.execute(() -> {
            archivadasDesplegadas = !archivadasDesplegadas;
            ListadoMaterias listado = ultimo;
            if (listado != null) {
                filas.postValue(construirFilas(listado, archivadasDesplegadas));
            }
        });
    }

    private static List<FilaMateria> construirFilas(ListadoMaterias listado, boolean desplegadas) {
        List<FilaMateria> resultado = new ArrayList<>();
        for (ResumenMateria activa : listado.activas()) {
            resultado.add(FilaMateria.activa(activa));
        }
        if (!listado.archivadas().isEmpty()) {
            resultado.add(FilaMateria.encabezadoArchivadas(listado.archivadas().size(), desplegadas));
            if (desplegadas) {
                for (Materia archivada : listado.archivadas()) {
                    resultado.add(FilaMateria.archivada(archivada));
                }
            }
        }
        return resultado;
    }
}
