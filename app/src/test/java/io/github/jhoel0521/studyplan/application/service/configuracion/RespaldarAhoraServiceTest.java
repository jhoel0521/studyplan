package io.github.jhoel0521.studyplan.application.service.configuracion;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;
import io.github.jhoel0521.studyplan.domain.service.SerializadorRespaldo;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.fakes.FakeAlmacenCredencialRespaldo;
import io.github.jhoel0521.studyplan.fakes.FakeAlmacenDatos;
import io.github.jhoel0521.studyplan.fakes.FakeCifradorRespaldo;
import io.github.jhoel0521.studyplan.fakes.FakeClienteGoogleDrive;
import io.github.jhoel0521.studyplan.fakes.FakeConfiguracionRespaldoRepository;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;

import org.junit.Before;
import org.junit.Test;

/**
 * Pruebas de {@link RespaldarAhoraService}: respaldo manual, cifrado con contraseña, sin cuenta, sin conexión y
 * autorización caducada.
 *
 * <p>Verifica: HU-35 y HU-36.
 */
public class RespaldarAhoraServiceTest {

    private FakeAlmacenDatos almacen;
    private FakeClienteGoogleDrive drive;
    private FakeCifradorRespaldo cifrador;
    private FakeAlmacenCredencialRespaldo credencial;
    private FakeConfiguracionRespaldoRepository configuracion;
    private FakeReloj reloj;
    private RespaldarAhoraService servicio;

    @Before
    public void preparar() {
        almacen = new FakeAlmacenDatos();
        drive = new FakeClienteGoogleDrive();
        cifrador = new FakeCifradorRespaldo();
        credencial = new FakeAlmacenCredencialRespaldo();
        configuracion = new FakeConfiguracionRespaldoRepository();
        reloj = new FakeReloj(Fecha.de(2026, 11, 23).valor(), Hora.de(9, 0).valor());
        servicio = new RespaldarAhoraService(almacen, drive, cifrador, credencial, configuracion, reloj);
    }

    @Test
    public void sinCuentaVinculadaNoSubeNadaYLoRegistra() {
        assertEquals(ResultadoRespaldo.SIN_CUENTA, servicio.ejecutar());

        assertNull(drive.archivo);
        assertEquals(ResultadoRespaldo.SIN_CUENTA, configuracion.leer().ultimoResultado());
        assertTrue(configuracion.leer().ultimoIntentoFallo());
    }

    @Test
    public void respaldoManualSubeLaInstantaneaEnClaroYGuardaFechaYTamano() {
        configuracion.guardarCorreo("ana@example.com");

        assertEquals(ResultadoRespaldo.OK, servicio.ejecutar());

        assertNotNull(drive.archivo);
        assertFalse(cifrador.estaCifrado(drive.archivo));
        byte[] datos = cifrador.desempaquetar(drive.archivo, null).get();
        assertEquals("Ana", SerializadorRespaldo.deserializar(datos).filas("perfil").get(0).get("nombre"));
        assertEquals(drive.archivo.length, configuracion.leer().ultimoRespaldo().tamanoBytes());
        assertEquals(reloj.ahoraMilis(), configuracion.leer().ultimoRespaldo().fechaUtcMilis());
        assertFalse(configuracion.leer().ultimoRespaldo().cifrado());
        assertFalse(configuracion.leer().ultimoIntentoFallo());
    }

    @Test
    public void conContrasenaElArchivoSubidoEstaCifradoYNoContieneLosDatosEnClaro() {
        configuracion.guardarCorreo("ana@example.com");
        configuracion.guardarConContrasena(true);
        credencial.guardar("secreta".toCharArray());

        assertEquals(ResultadoRespaldo.OK, servicio.ejecutar());

        assertTrue(cifrador.estaCifrado(drive.archivo));
        assertTrue(configuracion.leer().ultimoRespaldo().cifrado());
        assertArrayEquals(
                SerializadorRespaldo.serializar(almacen.exportarTodo(reloj.ahoraMilis())),
                cifrador.desempaquetar(drive.archivo, "secreta".toCharArray()).get());
    }

    @Test
    public void siLaContrasenaActivadaNoSePuedeRecuperarNoSubeNadaEnClaro() {
        configuracion.guardarCorreo("ana@example.com");
        configuracion.guardarConContrasena(true);

        assertEquals(ResultadoRespaldo.CONTRASENA_REQUERIDA, servicio.ejecutar());

        assertNull(drive.archivo);
    }

    @Test
    public void sinConexionDevuelveElFalloParaQueElTrabajoSeReintenteYNoActualizaElUltimoRespaldo() {
        configuracion.guardarCorreo("ana@example.com");
        drive.fallo = ResultadoRespaldo.SIN_CONEXION;

        assertEquals(ResultadoRespaldo.SIN_CONEXION, servicio.ejecutar());

        assertNull(configuracion.leer().ultimoRespaldo());
        assertEquals(0, almacen.borrados);
    }

    @Test
    public void conLaAutorizacionCaducadaElFalloQuedaVisibleParaVolverAAutorizar() {
        configuracion.guardarCorreo("ana@example.com");
        drive.fallo = ResultadoRespaldo.AUTORIZACION_REQUERIDA;

        servicio.ejecutar();

        assertTrue(configuracion.leer().ultimoIntentoFallo());
        assertEquals(ResultadoRespaldo.AUTORIZACION_REQUERIDA, configuracion.leer().ultimoResultado());
    }

    @Test
    public void unRespaldoBuenoDespuesDeUnFalloLimpiaElAvisoDeFallo() {
        configuracion.guardarCorreo("ana@example.com");
        drive.fallo = ResultadoRespaldo.ERROR_DRIVE;
        servicio.ejecutar();
        assertTrue(configuracion.leer().ultimoIntentoFallo());

        drive.fallo = ResultadoRespaldo.OK;
        servicio.ejecutar();

        assertFalse(configuracion.leer().ultimoIntentoFallo());
    }
}
