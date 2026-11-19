package io.github.jhoel0521.studyplan.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.AlcanceBorrado;
import io.github.jhoel0521.studyplan.fakes.FakeAlmacenDatos;
import io.github.jhoel0521.studyplan.fakes.FakeExportadorReporte;
import io.github.jhoel0521.studyplan.fakes.FakeProgramadorRecordatorios;

/**
 * Pruebas del borrado de datos con alcance «teléfono». Que los ajustes vuelvan a sus valores por defecto lo comprueba
 * la prueba del adaptador de Room (tarea 3.2), porque lo hace el almacén real al quedar vacío.
 *
 * <p>Verifica: HU-38, HU-34 y RN-21.
 */
public class BorrarDatosServiceTest {

    private FakeAlmacenDatos almacen;
    private SesionBloqueo sesion;
    private FakeExportadorReporte exportador;
    private FakeProgramadorRecordatorios recordatorios;
    private BorrarDatosService servicio;

    @Before
    public void preparar() {
        almacen = new FakeAlmacenDatos();
        sesion = new SesionBloqueo();
        exportador = new FakeExportadorReporte();
        recordatorios = new FakeProgramadorRecordatorios();
        servicio = new BorrarDatosService(
                almacen, sesion, null, null, exportador, recordatorios);
    }

    @Test
    public void borrarElTelefonoVaciaElAlmacenYLosTemporalesDeReportesUnaSolaVez() {
        recordatorios.programar(42L, 1_000_000L);
        servicio.ejecutar(AlcanceBorrado.TELEFONO);
        assertEquals(1, almacen.borrados);
        assertEquals(1, exportador.limpiezas);
        assertEquals(1, recordatorios.limpiezasTotales());
        assertEquals(0, recordatorios.cantidadProgramadas());
    }

    @Test
    public void trasBorrarLaSesionQuedaSinBloqueoAunqueEstuvieraActivo() {
        sesion.alCambiarBloqueo(true);
        servicio.ejecutar(AlcanceBorrado.TELEFONO);
        assertFalse(sesion.estaBloqueada());
        sesion.alVolverAPrimerPlano();
        assertFalse(sesion.estaBloqueada());
    }

    @Test(expected = NullPointerException.class)
    public void sinAlcanceNoSeBorraNada() {
        servicio.ejecutar(null);
    }
}
