package io.github.jhoel0521.studyplan.application.service.configuracion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.github.jhoel0521.studyplan.application.port.in.recordatorios.ReprogramarRecordatoriosPendientes;
import io.github.jhoel0521.studyplan.domain.readmodel.AvisoRecordatorioPendiente;
import io.github.jhoel0521.studyplan.domain.readmodel.InstantaneaRespaldo;
import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;
import io.github.jhoel0521.studyplan.domain.service.SerializadorRespaldo;
import io.github.jhoel0521.studyplan.fakes.FakeAlmacenDatos;
import io.github.jhoel0521.studyplan.fakes.FakeCifradorRespaldo;
import io.github.jhoel0521.studyplan.fakes.FakeClienteGoogleDrive;
import io.github.jhoel0521.studyplan.fakes.FakeConfiguracionRespaldoRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * Pruebas de {@link RestaurarRespaldoService}: restaurar, contraseña correcta e incorrecta, sin respaldo, versión más
 * nueva, archivo dañado, sin conexión y normalización tras restaurar.
 *
 * <p>Verifica: HU-37 y HU-36.
 */
public class RestaurarRespaldoServiceTest {

    private FakeAlmacenDatos almacen;
    private FakeClienteGoogleDrive drive;
    private FakeCifradorRespaldo cifrador;
    private FakeConfiguracionRespaldoRepository configuracion;
    private int reprogramaciones;
    private RestaurarRespaldoService servicio;

    @Before
    public void preparar() {
        almacen = new FakeAlmacenDatos();
        drive = new FakeClienteGoogleDrive();
        cifrador = new FakeCifradorRespaldo();
        configuracion = new FakeConfiguracionRespaldoRepository();
        configuracion.guardarCorreo("ana@example.com");
        ReprogramarRecordatoriosPendientes reprogramar = () -> {
            reprogramaciones++;
            return Collections.<AvisoRecordatorioPendiente>emptyList();
        };
        servicio = new RestaurarRespaldoService(almacen, drive, cifrador, configuracion, reprogramar);
    }

    private static Map<String, Object> fila(Object... paresClaveValor) {
        Map<String, Object> fila = new LinkedHashMap<>();
        for (int i = 0; i < paresClaveValor.length; i += 2) {
            fila.put((String) paresClaveValor[i], paresClaveValor[i + 1]);
        }
        return fila;
    }

    private static InstantaneaRespaldo respaldoDeOtroTelefono(int versionFormato) {
        Map<String, List<Map<String, Object>>> tablas = new LinkedHashMap<>();
        tablas.put("perfil", new ArrayList<>(Collections.singletonList(fila("id", 1L, "nombre", "Luis"))));
        tablas.put("ajustes", new ArrayList<>(Collections.singletonList(
                fila("id", 1L, "bloqueoActivo", 1L, "no_molestar_global", 1L, "tema", "OSCURO"))));
        tablas.put("sesion_estudio", new ArrayList<>(Collections.singletonList(
                fila("id", 5L, "estado", "EN_CURSO"))));
        return new InstantaneaRespaldo(versionFormato, 8, 123L, tablas);
    }

    private void subirRespaldo(InstantaneaRespaldo instantanea, char[] contrasena) {
        drive.archivo = cifrador.empaquetar(SerializadorRespaldo.serializar(instantanea), contrasena);
    }

    @Test
    public void restauraReemplazaLosDatosYReprogramaLosRecordatorios() {
        subirRespaldo(respaldoDeOtroTelefono(1), null);

        assertEquals(ResultadoRespaldo.OK, servicio.ejecutar(null));

        assertEquals(1, almacen.restauraciones);
        assertEquals("Luis", almacen.ultimaRestaurada.filas("perfil").get(0).get("nombre"));
        assertEquals(1, reprogramaciones);
    }

    @Test
    public void trasRestaurarLosAjustesDelDispositivoYLasSesionesEnCursoQuedanNormalizados() {
        subirRespaldo(respaldoDeOtroTelefono(1), null);

        servicio.ejecutar(null);

        Map<String, Object> ajustes = almacen.ultimaRestaurada.filas("ajustes").get(0);
        assertEquals(0L, ajustes.get("bloqueoActivo"));
        assertEquals(0L, ajustes.get("no_molestar_global"));
        assertEquals("OSCURO", ajustes.get("tema"));
        assertEquals("PENDIENTE", almacen.ultimaRestaurada.filas("sesion_estudio").get(0).get("estado"));
    }

    @Test
    public void conContrasenaCorrectaRestauraElRespaldoCifrado() {
        subirRespaldo(respaldoDeOtroTelefono(1), "secreta".toCharArray());

        assertEquals(ResultadoRespaldo.OK, servicio.ejecutar("secreta".toCharArray()));

        assertEquals(1, almacen.restauraciones);
    }

    @Test
    public void conContrasenaIncorrectaNoTocaLosDatosLocalesYPermiteReintentar() {
        subirRespaldo(respaldoDeOtroTelefono(1), "secreta".toCharArray());

        assertEquals(ResultadoRespaldo.CONTRASENA_INCORRECTA, servicio.ejecutar("otra".toCharArray()));

        assertEquals(0, almacen.restauraciones);
        assertEquals(0, almacen.borrados);
        assertEquals(ResultadoRespaldo.OK, servicio.ejecutar("secreta".toCharArray()));
    }

    @Test
    public void unRespaldoCifradoSinContrasenaLaPide() {
        subirRespaldo(respaldoDeOtroTelefono(1), "secreta".toCharArray());

        assertEquals(ResultadoRespaldo.CONTRASENA_REQUERIDA, servicio.ejecutar(null));
        assertEquals(ResultadoRespaldo.CONTRASENA_REQUERIDA, servicio.ejecutar(new char[0]));

        assertEquals(0, almacen.restauraciones);
    }

    @Test
    public void sinRespaldoEnDriveInformaYNoModificaNada() {
        assertEquals(ResultadoRespaldo.SIN_RESPALDO, servicio.ejecutar(null));

        assertEquals(0, almacen.restauraciones);
        assertEquals(0, reprogramaciones);
        assertNull(almacen.ultimaRestaurada);
    }

    @Test
    public void unRespaldoDeUnaVersionDeFormatoMasNuevaNoSeRestaura() {
        subirRespaldo(respaldoDeOtroTelefono(InstantaneaRespaldo.VERSION_FORMATO_ACTUAL + 1), null);

        assertEquals(ResultadoRespaldo.VERSION_NO_SOPORTADA, servicio.ejecutar(null));

        assertEquals(0, almacen.restauraciones);
    }

    @Test
    public void unArchivoQueNoEsUnRespaldoOEstaDanadoNoSeRestaura() {
        drive.archivo = "basura".getBytes();
        assertEquals(ResultadoRespaldo.RESPALDO_CORRUPTO, servicio.ejecutar(null));

        drive.archivo = cifrador.empaquetar("{no es json".getBytes(), null);
        assertEquals(ResultadoRespaldo.RESPALDO_CORRUPTO, servicio.ejecutar(null));

        assertEquals(0, almacen.restauraciones);
    }

    @Test
    public void sinConexionOSinCuentaNoCambiaNada() {
        subirRespaldo(respaldoDeOtroTelefono(1), null);
        drive.fallo = ResultadoRespaldo.SIN_CONEXION;
        assertEquals(ResultadoRespaldo.SIN_CONEXION, servicio.ejecutar(null));

        configuracion.limpiar();
        drive.fallo = ResultadoRespaldo.OK;
        assertEquals(ResultadoRespaldo.SIN_CUENTA, servicio.ejecutar(null));

        assertEquals(0, almacen.restauraciones);
        assertNotNull(drive.archivo);
    }
}
