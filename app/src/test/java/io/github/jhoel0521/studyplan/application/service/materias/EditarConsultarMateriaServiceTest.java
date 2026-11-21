package io.github.jhoel0521.studyplan.application.service.materias;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;

/**
 * Pruebas de editar y consultar una materia.
 *
 * <p>Verifica: HU-07 (Edición, conservando el id), RN-02 (renombrar a un nombre activo existente) y RNF-14.
 */
public class EditarConsultarMateriaServiceTest {

    private static final int COLOR = 0xFF9C27B0;

    private FakeMateriaRepository repositorio;
    private EditarMateriaService editar;
    private ConsultarMateriaService consultar;
    private long idFisica;

    @Before
    public void preparar() {
        repositorio = new FakeMateriaRepository();
        editar = new EditarMateriaService(repositorio);
        consultar = new ConsultarMateriaService(repositorio);
        RegistrarMateriaService registrar = new RegistrarMateriaService(repositorio);
        idFisica = registrar.ejecutar("Física", COLOR, null).valor().id();
        registrar.ejecutar("Química", COLOR, null);
    }

    @Test
    public void editarCambiaNombreColorYDocenteConservandoElId() {
        Materia editada = editar.ejecutar(idFisica, "Física II", 0xFF000000, "Dr. Paz").valor();
        assertEquals(Long.valueOf(idFisica), editada.id());
        Materia guardada = consultar.ejecutar(idFisica).orElseThrow(AssertionError::new);
        assertEquals("Física II", guardada.nombre());
        assertEquals(0xFF000000, guardada.color());
        assertEquals("Dr. Paz", guardada.docente());
    }

    @Test
    public void guardarElMismoNombreConOtrasMayusculasNoChocaConsigoMisma() {
        assertTrue(editar.ejecutar(idFisica, "FÍSICA", COLOR, null).esValido());
    }

    @Test
    public void renombrarAUnNombreDeOtraActivaSeRechaza() {
        assertEquals(ErrorValidacion.NOMBRE_DUPLICADO, editar.ejecutar(idFisica, " quimica ", COLOR, null).error());
        assertEquals("Física", consultar.ejecutar(idFisica).orElseThrow(AssertionError::new).nombre());
    }

    @Test
    public void editarUnaMateriaInexistenteDevuelveNoEncontrada() {
        assertEquals(ErrorValidacion.MATERIA_NO_ENCONTRADA, editar.ejecutar(99, "X", COLOR, null).error());
    }

    @Test
    public void editarConNombreInvalidoNoGuardaNada() {
        int antes = repositorio.guardados;
        assertEquals(ErrorValidacion.NOMBRE_VACIO, editar.ejecutar(idFisica, "  ", COLOR, null).error());
        assertEquals(antes, repositorio.guardados);
    }

    @Test
    public void unaArchivadaPuedeRepetirElNombreDeUnaActiva() {
        long idArchivada = repositorio.guardar(
                Materia.crear("Historia", COLOR, null).valor().conArchivada(true)).id();
        assertTrue(editar.ejecutar(idArchivada, "Química", COLOR, null).esValido());
    }

    @Test
    public void consultarUnaMateriaInexistenteDevuelveVacio() {
        assertFalse(consultar.ejecutar(99).isPresent());
    }
}
