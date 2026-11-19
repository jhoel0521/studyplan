package io.github.jhoel0521.studyplan.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.AlcanceBorrado;
import io.github.jhoel0521.studyplan.fakes.FakeAlmacenDatos;
import io.github.jhoel0521.studyplan.fakes.FakeExportadorReporte;

/**
 * Pruebas del borrado de datos con alcance «teléfono». Que los ajustes vuelvan a sus valores por defecto lo comprueba
 * la prueba del adaptador de Room (tarea 3.2), porque lo hace el almacén real al quedar vacío.
 *
 * <p>Verifica: HU-38 y RN-21.
 */
public class BorrarDatosServiceTest {

    private FakeAlmacenDatos almacen;
    private SesionBloqueo sesion;
    private FakeExportadorReporte exportador;
    private BorrarDatosService servicio;

    @Before
    public void preparar() {
        almacen = new FakeAlmacenDatos();
        sesion = new SesionBloqueo();
        exportador = new FakeExportadorReporte();
        servicio = new BorrarDatosService(almacen, sesion, null, null, exportador);
    }

    @Test
    public void borrarElTelefonoVaciaElAlmacenYLosTemporalesDeReportesUnaSolaVez() {
        servicio.ejecutar(AlcanceBorrado.TELEFONO);
        assertEquals(1, almacen.borrados);
        assertEquals(1, exportador.limpiezas);
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
