package io.github.jhoel0521.studyplan.adapter.in.ui.materias;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import java.util.List;
import java.util.Objects;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.github.jhoel0521.studyplan.application.service.materias.ArchivarMateriaService;
import io.github.jhoel0521.studyplan.application.service.materias.ListarMateriasService;
import io.github.jhoel0521.studyplan.application.service.materias.RegistrarMateriaService;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;

/**
 * Pruebas del ViewModel de la lista de materias con ejecución instantánea.
 *
 * <p>Verifica: HU-09 (Lista y Lista vacía, parcial: horas en cero) y HU-08 (sección «Archivadas» plegable).
 */
public class ListaMateriasViewModelTest {

    private static final int COLOR = 0xFF1E88E5;

    @Rule
    public InstantTaskExecutorRule reglaInstantanea = new InstantTaskExecutorRule();

    private FakeMateriaRepository repositorio;
    private RegistrarMateriaService registrar;
    private ListaMateriasViewModel modelo;

    @Before
    public void preparar() {
        repositorio = new FakeMateriaRepository();
        registrar = new RegistrarMateriaService(repositorio);
        modelo = new ListaMateriasViewModel(
                new ListarMateriasService(repositorio, new FakeSesionRepository()),
                Runnable::run);
    }

    private List<FilaMateria> filas() {
        return Objects.requireNonNull(modelo.filas().getValue());
    }

    @Test
    public void sinMateriasPublicaListaVaciaYSinFilas() {
        modelo.cargar();
        assertEquals(Boolean.TRUE, modelo.vacia().getValue());
        assertTrue(filas().isEmpty());
    }

    @Test
    public void lasActivasSalenEnOrdenAlfabeticoConSusHorasEnCero() {
        registrar.ejecutar("Química", COLOR, null);
        registrar.ejecutar("Álgebra", COLOR, "Dra. Ruiz");
        modelo.cargar();
        assertEquals(Boolean.FALSE, modelo.vacia().getValue());
        assertEquals(2, filas().size());
        assertEquals("Álgebra", filas().get(0).materia().nombre());
        assertEquals(FilaMateria.Tipo.ACTIVA, filas().get(0).tipo());
        assertEquals(0, filas().get(0).horasRealizadas(), 0);
        assertEquals("Química", filas().get(1).materia().nombre());
    }

    @Test
    public void lasArchivadasAparecenSoloComoEncabezadoPlegadoConSuCantidad() {
        long id = registrar.ejecutar("Física", COLOR, null).valor().id();
        registrar.ejecutar("Biología", COLOR, null);
        new ArchivarMateriaService(repositorio).ejecutar(id);
        modelo.cargar();
        assertEquals(2, filas().size());
        FilaMateria encabezado = filas().get(1);
        assertEquals(FilaMateria.Tipo.ENCABEZADO_ARCHIVADAS, encabezado.tipo());
        assertEquals(1, encabezado.cantidadArchivadas());
        assertFalse(encabezado.expandida());
    }

    @Test
    public void alternarElEncabezadoDespliegaYPliegaLasArchivadas() {
        long id = registrar.ejecutar("Física", COLOR, null).valor().id();
        new ArchivarMateriaService(repositorio).ejecutar(id);
        modelo.cargar();
        modelo.alternarArchivadas();
        assertEquals(2, filas().size());
        assertEquals(FilaMateria.Tipo.ARCHIVADA, filas().get(1).tipo());
        assertEquals("Física", filas().get(1).materia().nombre());
        modelo.alternarArchivadas();
        assertEquals(1, filas().size());
    }

    @Test
    public void unaListaSoloConArchivadasNoEstaVacia() {
        long id = registrar.ejecutar("Física", COLOR, null).valor().id();
        new ArchivarMateriaService(repositorio).ejecutar(id);
        modelo.cargar();
        assertEquals(Boolean.FALSE, modelo.vacia().getValue());
    }
}
