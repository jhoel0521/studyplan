package io.github.jhoel0521.studyplan.application.service.configuracion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.github.jhoel0521.studyplan.domain.enumeration.AlcanceBorrado;
import io.github.jhoel0521.studyplan.domain.enumeration.FrecuenciaRespaldo;
import io.github.jhoel0521.studyplan.domain.readmodel.MetadatosRespaldo;
import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;
import io.github.jhoel0521.studyplan.fakes.FakeAlmacenCredencialRespaldo;
import io.github.jhoel0521.studyplan.fakes.FakeAlmacenDatos;
import io.github.jhoel0521.studyplan.fakes.FakeClienteGoogleDrive;
import io.github.jhoel0521.studyplan.fakes.FakeConfiguracionRespaldoRepository;
import io.github.jhoel0521.studyplan.fakes.FakeProgramadorRespaldo;

import org.junit.Before;
import org.junit.Test;

/**
 * Pruebas del borrado selectivo: solo Drive, teléfono y Drive, y fallo de Drive sin tocar el teléfono.
 *
 * <p>Verifica: HU-38 y RN-21.
 */
public class BorrarDatosAlcanceDriveTest {

    private FakeAlmacenDatos almacen;
    private FakeClienteGoogleDrive drive;
    private FakeConfiguracionRespaldoRepository configuracion;
    private FakeAlmacenCredencialRespaldo credencial;
    private FakeProgramadorRespaldo programador;
    private BorrarDatosService servicio;

    @Before
    public void preparar() {
        almacen = new FakeAlmacenDatos();
        drive = new FakeClienteGoogleDrive();
        drive.archivo = new byte[] {1, 2, 3};
        configuracion = new FakeConfiguracionRespaldoRepository();
        configuracion.guardarCorreo("ana@example.com");
        configuracion.guardarFrecuencia(FrecuenciaRespaldo.SEMANAL);
        configuracion.guardarConContrasena(true);
        configuracion.guardarUltimoRespaldo(new MetadatosRespaldo(1L, 3L, true));
        credencial = new FakeAlmacenCredencialRespaldo();
        credencial.guardar("secreta".toCharArray());
        programador = new FakeProgramadorRespaldo();
        programador.programar(FrecuenciaRespaldo.SEMANAL);
        servicio = new BorrarDatosService(
                almacen, new SesionBloqueo(), null, null, null, null, drive, configuracion, programador, credencial);
    }

    @Test
    public void borrarSoloDriveEliminaElRespaldoYDejaIntactoElTelefono() {
        assertEquals(ResultadoRespaldo.OK, servicio.ejecutar(AlcanceBorrado.DRIVE));

        assertNull(drive.archivo);
        assertEquals(0, almacen.borrados);
        assertNull(configuracion.leer().ultimoRespaldo());
        assertNotNull(configuracion.leer().correo());
        assertNotNull(credencial.obtener());
    }

    @Test
    public void borrarAmbosEliminaDriveYElTelefonoYOlvidaCuentaContrasenaYTarea() {
        assertEquals(ResultadoRespaldo.OK, servicio.ejecutar(AlcanceBorrado.AMBOS));

        assertNull(drive.archivo);
        assertEquals(1, almacen.borrados);
        assertFalse(configuracion.leer().hayCuenta());
        assertEquals(FrecuenciaRespaldo.NUNCA, configuracion.leer().frecuencia());
        assertNull(credencial.obtener());
        assertNull(programador.programada);
    }

    @Test
    public void borrarSoloElTelefonoDejaElRespaldoEnDriveYCancelaElRespaldoAutomatico() {
        assertEquals(ResultadoRespaldo.OK, servicio.ejecutar(AlcanceBorrado.TELEFONO));

        assertNotNull(drive.archivo);
        assertEquals(1, almacen.borrados);
        assertNull(programador.programada);
        assertNull(credencial.obtener());
    }

    @Test
    public void siDriveFallaConAmbosNoSeBorraNadaDelTelefono() {
        drive.fallo = ResultadoRespaldo.SIN_CONEXION;

        assertEquals(ResultadoRespaldo.SIN_CONEXION, servicio.ejecutar(AlcanceBorrado.AMBOS));

        assertEquals(0, almacen.borrados);
        assertNotNull(configuracion.leer().correo());
        assertEquals(0, drive.eliminaciones);
    }

    @Test
    public void sinClienteDeDriveElBorradoDeDriveDevuelveSinCuenta() {
        BorrarDatosService sinDrive = new BorrarDatosService(almacen, new SesionBloqueo());

        assertEquals(ResultadoRespaldo.SIN_CUENTA, sinDrive.ejecutar(AlcanceBorrado.AMBOS));
        assertEquals(0, almacen.borrados);
    }
}
