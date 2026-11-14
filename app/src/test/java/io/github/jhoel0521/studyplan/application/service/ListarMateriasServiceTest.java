package io.github.jhoel0521.studyplan.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.ListadoMaterias;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;

/**
 * Pruebas de la lista de materias: orden alfabético, lista vacía, archivadas aparte y horas
 * acumuladas reales por materia según RN-11.
 *
 * <p>Verifica: HU-09, HU-08 y RN-11.
 */
public class ListarMateriasServiceTest {

    private static final int COLOR = 0xFF607D8B;
    private static final Fecha HOY = Fecha.de(2026, 11, 14).valor();

    private FakeMateriaRepository repositorio;
    private FakeSesionRepository sesiones;
    private RegistrarMateriaService registrar;
    private ListarMateriasService servicio;

    @Before
    public void preparar() {
        repositorio = new FakeMateriaRepository();
        sesiones = new FakeSesionRepository();
        registrar = new RegistrarMateriaService(repositorio);
        servicio = new ListarMateriasService(repositorio, sesiones);
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
    public void sumaSoloHorasRealesDeCompletadasYCompletadasTardiasSegunRn11() {
        long fisicaId = registrar.ejecutar("Física", COLOR, null).valor().id();
        sesiones.guardar(SesionEstudio.restaurar(
                null, fisicaId, HOY, Hora.de(8, 0).valor(), Hora.de(9, 0).valor(),
                60, 90, CodigoEstadoSesion.COMPLETADA, null, null));
        sesiones.guardar(SesionEstudio.restaurar(
                null, fisicaId, HOY.sumarDias(-1), Hora.de(8, 0).valor(), Hora.de(9, 0).valor(),
                60, 30, CodigoEstadoSesion.COMPLETADA_TARDIA, null, null));
        // Estas NO deben sumar horas reales
        sesiones.guardar(SesionEstudio.restaurar(
                null, fisicaId, HOY, Hora.de(10, 0).valor(), Hora.de(12, 0).valor(),
                120, null, CodigoEstadoSesion.PENDIENTE, null, null));
        sesiones.guardar(SesionEstudio.restaurar(
                null, fisicaId, HOY.sumarDias(-2), Hora.de(10, 0).valor(), Hora.de(12, 0).valor(),
                120, null, CodigoEstadoSesion.NO_REALIZADA, null, null));
        sesiones.guardar(SesionEstudio.restaurar(
                null, fisicaId, HOY.sumarDias(-1), Hora.de(10, 0).valor(), Hora.de(12, 0).valor(),
                120, 120, CodigoEstadoSesion.REPROGRAMADA, null, null));

        assertEquals(2.0, servicio.ejecutar().activas().get(0).horasRealizadas(), 0.001);
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
