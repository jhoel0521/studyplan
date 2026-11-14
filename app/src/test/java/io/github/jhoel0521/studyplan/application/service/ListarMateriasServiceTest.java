package io.github.jhoel0521.studyplan.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.ListadoMaterias;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;

/**
 * Pruebas de la lista de materias: orden alfabético, lista vacía, archivadas aparte y horas en cero.
 *
 * <p>Verifica: HU-09 (Lista y Lista vacía, parcial: las horas realizadas son cero hasta que existan sesiones) y
 * HU-08 (archivadas).
 */
public class ListarMateriasServiceTest {

    private static final int COLOR = 0xFF607D8B;

    private FakeMateriaRepository repositorio;
    private RegistrarMateriaService registrar;
    private ListarMateriasService servicio;

    @Before
    public void preparar() {
        repositorio = new FakeMateriaRepository();
        registrar = new RegistrarMateriaService(repositorio);
        servicio = new ListarMateriasService(repositorio);
    }

    @Test
    public void sinMateriasLaListaEstaVacia() {
        assertTrue(servicio.ejecutar().estaVacio());
    }

    @Test
    public void lasActivasSalenEnOrdenAlfabeticoSinDistinguirAcentosNiMayusculas() {
        registrar.ejecutar("química", COLOR, null);
        registrar.ejecutar("Álgebra", COLOR, null);
        registrar.ejecutar("Biología", COLOR, null);
        registrar.ejecutar("Estadística", COLOR, null);
        ListadoMaterias listado = servicio.ejecutar();
        assertEquals("Álgebra", listado.activas().get(0).materia().nombre());
        assertEquals("Biología", listado.activas().get(1).materia().nombre());
        assertEquals("Estadística", listado.activas().get(2).materia().nombre());
        assertEquals("química", listado.activas().get(3).materia().nombre());
    }

    @Test
    public void lasHorasRealizadasSonCeroMientrasNoHayaSesiones() {
        registrar.ejecutar("Física", COLOR, null);
        assertEquals(0, servicio.ejecutar().activas().get(0).horasRealizadas(), 0);
    }

    @Test
    public void lasArchivadasSalenAparteYTambienOrdenadas() {
        long fisica = registrar.ejecutar("Física", COLOR, null).valor().id();
        long algebra = registrar.ejecutar("Álgebra", COLOR, null).valor().id();
        registrar.ejecutar("Biología", COLOR, null);
        new ArchivarMateriaService(repositorio).ejecutar(fisica);
        new ArchivarMateriaService(repositorio).ejecutar(algebra);
        ListadoMaterias listado = servicio.ejecutar();
        assertEquals(1, listado.activas().size());
        assertEquals(2, listado.archivadas().size());
        assertEquals("Álgebra", listado.archivadas().get(0).nombre());
        assertFalse(listado.estaVacio());
    }
}
