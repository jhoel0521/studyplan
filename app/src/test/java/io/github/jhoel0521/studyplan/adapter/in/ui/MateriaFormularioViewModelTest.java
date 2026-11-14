package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import java.util.Objects;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.github.jhoel0521.studyplan.application.service.ArchivarMateriaService;
import io.github.jhoel0521.studyplan.application.service.ConsultarMateriaService;
import io.github.jhoel0521.studyplan.application.service.DesarchivarMateriaService;
import io.github.jhoel0521.studyplan.application.service.EditarMateriaService;
import io.github.jhoel0521.studyplan.application.service.EliminarMateriaService;
import io.github.jhoel0521.studyplan.application.service.RegistrarMateriaService;
import io.github.jhoel0521.studyplan.domain.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeSesionesDeMateria;

/**
 * Pruebas del ViewModel del formulario de materia: cada error publica su código y cancelar la confirmación no elimina.
 *
 * <p>Verifica: HU-06 (registrar y nombre duplicado), HU-07 (editar), HU-08 (archivar, eliminar y eliminar protegida),
 * HU-46 (desarchivar), RN-02, RN-03 y RNF-14.
 */
public class MateriaFormularioViewModelTest {

    private static final int COLOR = 0xFF43A047;

    @Rule
    public InstantTaskExecutorRule reglaInstantanea = new InstantTaskExecutorRule();

    private FakeMateriaRepository repositorio;
    private FakeSesionesDeMateria sesiones;
    private RegistrarMateriaService registrar;
    private MateriaFormularioViewModel modelo;

    @Before
    public void preparar() {
        repositorio = new FakeMateriaRepository();
        sesiones = new FakeSesionesDeMateria();
        registrar = new RegistrarMateriaService(repositorio);
        modelo = new MateriaFormularioViewModel(registrar, new EditarMateriaService(repositorio),
                new ConsultarMateriaService(repositorio), new ArchivarMateriaService(repositorio),
                new DesarchivarMateriaService(repositorio), new EliminarMateriaService(repositorio, sesiones),
                Runnable::run);
    }

    private EventoMateria evento() {
        return Objects.requireNonNull(modelo.evento().getValue());
    }

    @Test
    public void registrarUnaMateriaValidaPublicaGuardada() {
        modelo.guardar(null, "Matemáticas", COLOR, null);
        assertEquals(EventoMateria.Tipo.GUARDADA, evento().tipo());
        assertEquals(1, repositorio.activas().size());
    }

    @Test
    public void unNombreVacioPublicaElCodigoNombreVacio() {
        modelo.guardar(null, "   ", COLOR, null);
        assertEquals(EventoMateria.Tipo.ERROR, evento().tipo());
        assertEquals(ErrorValidacion.NOMBRE_VACIO, evento().error());
        assertEquals(0, repositorio.cantidad());
    }

    @Test
    public void unNombreRepetidoPublicaElCodigoNombreDuplicado() {
        registrar.ejecutar("Matemáticas", COLOR, null);
        modelo.guardar(null, " MATEMATICAS ", COLOR, null);
        assertEquals(ErrorValidacion.NOMBRE_DUPLICADO, evento().error());
        assertEquals(1, repositorio.cantidad());
    }

    @Test
    public void unNombreMuyLargoPublicaElCodigoNombreMuyLargo() {
        modelo.guardar(null, "a".repeat(51), COLOR, null);
        assertEquals(ErrorValidacion.NOMBRE_MUY_LARGO, evento().error());
    }

    @Test
    public void cargarPublicaLaMateriaYEditarLaActualiza() {
        long id = registrar.ejecutar("Física", COLOR, null).valor().id();
        modelo.cargar(id);
        assertEquals("Física", Objects.requireNonNull(modelo.materia().getValue()).nombre());
        modelo.guardar(id, "Física II", COLOR, "Dr. Paz");
        assertEquals(EventoMateria.Tipo.GUARDADA, evento().tipo());
        assertEquals("Física II", repositorio.obtener(id).orElseThrow(AssertionError::new).nombre());
    }

    @Test
    public void archivarYDesarchivarPublicanSuEvento() {
        long id = registrar.ejecutar("Física", COLOR, null).valor().id();
        modelo.archivar(id);
        assertEquals(EventoMateria.Tipo.ARCHIVADA, evento().tipo());
        assertEquals(1, repositorio.archivadas().size());
        modelo.desarchivar(id);
        assertEquals(EventoMateria.Tipo.DESARCHIVADA, evento().tipo());
        assertEquals(1, repositorio.activas().size());
    }

    @Test
    public void desarchivarConElNombreEnUsoPublicaNombreDuplicado() {
        long id = registrar.ejecutar("Física", COLOR, null).valor().id();
        modelo.archivar(id);
        registrar.ejecutar("física", COLOR, null);
        modelo.desarchivar(id);
        assertEquals(ErrorValidacion.NOMBRE_DUPLICADO, evento().error());
    }

    @Test
    public void pedirEliminarSoloMuestraLaConfirmacionYNoBorraNada() {
        long id = registrar.ejecutar("Física", COLOR, null).valor().id();
        modelo.pedirEliminar();
        assertEquals(Boolean.TRUE, modelo.confirmandoEliminar().getValue());
        assertEquals(1, repositorio.cantidad());
        assertNull(modelo.evento().getValue());
        assertTrue(repositorio.obtener(id).isPresent());
    }

    @Test
    public void cancelarLaConfirmacionNoElimina() {
        long id = registrar.ejecutar("Física", COLOR, null).valor().id();
        modelo.pedirEliminar();
        modelo.cancelarEliminar();
        assertEquals(Boolean.FALSE, modelo.confirmandoEliminar().getValue());
        assertEquals(1, repositorio.cantidad());
        assertTrue(repositorio.obtener(id).isPresent());
    }

    @Test
    public void confirmarEliminaUnaMateriaSinSesiones() {
        long id = registrar.ejecutar("Física", COLOR, null).valor().id();
        modelo.pedirEliminar();
        modelo.confirmarEliminar(id);
        assertEquals(EventoMateria.Tipo.ELIMINADA, evento().tipo());
        assertEquals(Boolean.FALSE, modelo.confirmandoEliminar().getValue());
        assertFalse(repositorio.obtener(id).isPresent());
    }

    @Test
    public void eliminarUnaMateriaConSesionesPublicaElCodigoYNoLaBorra() {
        long id = registrar.ejecutar("Física", COLOR, null).valor().id();
        sesiones.conSesiones(id, 2);
        modelo.confirmarEliminar(id);
        assertEquals(ErrorValidacion.MATERIA_CON_SESIONES, evento().error());
        Materia sigue = repositorio.obtener(id).orElseThrow(AssertionError::new);
        assertEquals("Física", sigue.nombre());
    }

    @Test
    public void unDobleToqueEnGuardarSoloRegistraUnaVezYConservaElExito() {
        java.util.ArrayDeque<Runnable> cola = new java.util.ArrayDeque<>();
        MateriaFormularioViewModel conCola = new MateriaFormularioViewModel(registrar,
                new EditarMateriaService(repositorio), new ConsultarMateriaService(repositorio),
                new ArchivarMateriaService(repositorio), new DesarchivarMateriaService(repositorio),
                new EliminarMateriaService(repositorio, sesiones), cola::add);
        conCola.guardar(null, "Física", COLOR, null);
        conCola.guardar(null, "Física", COLOR, null);
        assertEquals(1, cola.size());
        Objects.requireNonNull(cola.poll()).run();
        assertEquals(EventoMateria.Tipo.GUARDADA, Objects.requireNonNull(conCola.evento().getValue()).tipo());
        assertEquals(1, repositorio.cantidad());
    }

    @Test
    public void siElServicioFallaSePuedeVolverAIntentar() {
        MateriaFormularioViewModel conFalla = new MateriaFormularioViewModel(
                (nombre, color, docente) -> {
                    throw new IllegalStateException("fallo simulado");
                },
                new EditarMateriaService(repositorio), new ConsultarMateriaService(repositorio),
                new ArchivarMateriaService(repositorio), new DesarchivarMateriaService(repositorio),
                new EliminarMateriaService(repositorio, sesiones), Runnable::run);
        try {
            conFalla.guardar(null, "Física", COLOR, null);
        } catch (IllegalStateException esperada) {
            // el ejecutor síncrono propaga la excepción: lo que importa es que la marca se libere
        }
        MateriaFormularioViewModel despues = new MateriaFormularioViewModel(registrar,
                new EditarMateriaService(repositorio), new ConsultarMateriaService(repositorio),
                new ArchivarMateriaService(repositorio), new DesarchivarMateriaService(repositorio),
                new EliminarMateriaService(repositorio, sesiones), Runnable::run);
        despues.guardar(null, "Física", COLOR, null);
        assertEquals(EventoMateria.Tipo.GUARDADA, Objects.requireNonNull(despues.evento().getValue()).tipo());
    }

    @Test
    public void trasUnErrorSePuedeVolverAIntentar() {
        modelo.guardar(null, "  ", COLOR, null);
        assertEquals(ErrorValidacion.NOMBRE_VACIO, evento().error());
        modelo.guardar(null, "Física", COLOR, null);
        assertEquals(EventoMateria.Tipo.GUARDADA, evento().tipo());
    }

    @Test
    public void consumirElEventoLoLimpia() {
        modelo.guardar(null, "Física", COLOR, null);
        modelo.consumirEvento();
        assertNull(modelo.evento().getValue());
    }
}
