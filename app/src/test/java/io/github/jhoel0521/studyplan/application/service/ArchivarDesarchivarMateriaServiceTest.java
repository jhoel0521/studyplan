package io.github.jhoel0521.studyplan.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;

/**
 * Pruebas de archivar y desarchivar una materia.
 *
 * <p>Verifica: HU-08 (Archivar), HU-46 (Desarchivar y Nombre ya en uso) y RN-02.
 */
public class ArchivarDesarchivarMateriaServiceTest {

    private static final int COLOR = 0xFFFF9800;

    private FakeMateriaRepository repositorio;
    private RegistrarMateriaService registrar;
    private ArchivarMateriaService archivar;
    private DesarchivarMateriaService desarchivar;

    @Before
    public void preparar() {
        repositorio = new FakeMateriaRepository();
        registrar = new RegistrarMateriaService(repositorio);
        archivar = new ArchivarMateriaService(repositorio);
        desarchivar = new DesarchivarMateriaService(repositorio);
    }

    @Test
    public void archivarLaSacaDeLasActivasYLaDejaEnLasArchivadas() {
        long id = registrar.ejecutar("Física", COLOR, null).valor().id();
        assertTrue(archivar.ejecutar(id).valor().archivada());
        assertEquals(0, repositorio.activas().size());
        assertEquals(1, repositorio.archivadas().size());
    }

    @Test
    public void archivarUnaYaArchivadaNoFalla() {
        long id = registrar.ejecutar("Física", COLOR, null).valor().id();
        archivar.ejecutar(id);
        assertTrue(archivar.ejecutar(id).esValido());
        assertEquals(1, repositorio.archivadas().size());
    }

    @Test
    public void archivarUnaInexistenteDevuelveNoEncontrada() {
        assertEquals(ErrorValidacion.MATERIA_NO_ENCONTRADA, archivar.ejecutar(99).error());
    }

    @Test
    public void desarchivarLaDevuelveALasActivas() {
        long id = registrar.ejecutar("Física", COLOR, null).valor().id();
        archivar.ejecutar(id);
        assertFalse(desarchivar.ejecutar(id).valor().archivada());
        assertEquals(1, repositorio.activas().size());
        assertEquals(0, repositorio.archivadas().size());
    }

    @Test
    public void desarchivarConElNombreEnUsoPorUnaActivaSeRechaza() {
        long id = registrar.ejecutar("Física", COLOR, null).valor().id();
        archivar.ejecutar(id);
        registrar.ejecutar("física", COLOR, null);
        assertEquals(ErrorValidacion.NOMBRE_DUPLICADO, desarchivar.ejecutar(id).error());
        assertEquals(1, repositorio.archivadas().size());
    }

    @Test
    public void desarchivarUnaActivaDevuelveYaActiva() {
        long id = registrar.ejecutar("Física", COLOR, null).valor().id();
        assertEquals(ErrorValidacion.MATERIA_YA_ACTIVA, desarchivar.ejecutar(id).error());
    }

    @Test
    public void desarchivarUnaInexistenteDevuelveNoEncontrada() {
        assertEquals(ErrorValidacion.MATERIA_NO_ENCONTRADA, desarchivar.ejecutar(99).error());
    }
}
