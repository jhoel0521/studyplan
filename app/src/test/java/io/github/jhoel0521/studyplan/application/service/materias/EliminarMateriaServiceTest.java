package io.github.jhoel0521.studyplan.application.service.materias;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeSesionesDeMateria;

/**
 * Pruebas de eliminar una materia: con sesiones no se elimina, solo se archiva.
 *
 * <p>Verifica: HU-08 (Eliminar protegida y Eliminar sin historial) y RN-03.
 */
public class EliminarMateriaServiceTest {

    private static final int COLOR = 0xFFE91E63;

    private FakeMateriaRepository repositorio;
    private FakeSesionesDeMateria sesiones;
    private EliminarMateriaService servicio;
    private long id;

    @Before
    public void preparar() {
        repositorio = new FakeMateriaRepository();
        sesiones = new FakeSesionesDeMateria();
        servicio = new EliminarMateriaService(repositorio, sesiones);
        id = new RegistrarMateriaService(repositorio).ejecutar("Física", COLOR, null).valor().id();
    }

    @Test
    public void unaMateriaSinSesionesSeEliminaDeFormaDefinitiva() {
        assertTrue(servicio.ejecutar(id).esValido());
        assertEquals(0, repositorio.cantidad());
    }

    @Test
    public void unaMateriaConSesionesNoSeEliminaYSeSugiereArchivar() {
        sesiones.conSesiones(id, 3);
        assertEquals(ErrorValidacion.MATERIA_CON_SESIONES, servicio.ejecutar(id).error());
        assertEquals(1, repositorio.cantidad());
    }

    @Test
    public void eliminarUnaInexistenteDevuelveNoEncontrada() {
        assertEquals(ErrorValidacion.MATERIA_NO_ENCONTRADA, servicio.ejecutar(99).error());
    }
}
