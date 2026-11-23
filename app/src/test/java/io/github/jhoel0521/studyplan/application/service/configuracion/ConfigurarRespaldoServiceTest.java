package io.github.jhoel0521.studyplan.application.service.configuracion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.github.jhoel0521.studyplan.domain.enumeration.FrecuenciaRespaldo;
import io.github.jhoel0521.studyplan.domain.readmodel.ConfiguracionRespaldo;
import io.github.jhoel0521.studyplan.domain.readmodel.MetadatosRespaldo;
import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;
import io.github.jhoel0521.studyplan.fakes.FakeAlmacenCredencialRespaldo;
import io.github.jhoel0521.studyplan.fakes.FakeConfiguracionRespaldoRepository;
import io.github.jhoel0521.studyplan.fakes.FakeProgramadorRespaldo;

import org.junit.Before;
import org.junit.Test;

/**
 * Pruebas de {@link ConfigurarRespaldoService}: apagado por defecto, frecuencia y tarea periódica, contraseña y
 * desvincular.
 *
 * <p>Verifica: HU-35, HU-36, RN-19 y RN-20.
 */
public class ConfigurarRespaldoServiceTest {

    private FakeConfiguracionRespaldoRepository configuracion;
    private FakeAlmacenCredencialRespaldo credencial;
    private FakeProgramadorRespaldo programador;
    private ConfigurarRespaldoService servicio;

    @Before
    public void preparar() {
        configuracion = new FakeConfiguracionRespaldoRepository();
        credencial = new FakeAlmacenCredencialRespaldo();
        programador = new FakeProgramadorRespaldo();
        servicio = new ConfigurarRespaldoService(configuracion, credencial, programador);
    }

    @Test
    public void elRespaldoNaceApagadoSinCuentaNiTareaProgramada() {
        ConfiguracionRespaldo estado = servicio.consultar();

        assertFalse(estado.hayCuenta());
        assertEquals(FrecuenciaRespaldo.NUNCA, estado.frecuencia());
        assertFalse(estado.conContrasena());
        assertNull(estado.ultimoRespaldo());
        assertFalse(estado.ultimoIntentoFallo());
        assertNull(programador.programada);
    }

    @Test
    public void conCuentaVinculadaLaFrecuenciaSemanalProgramaLaTareaPeriodica() {
        servicio.vincularCuenta("  ana@example.com ");

        assertEquals(ResultadoRespaldo.OK, servicio.cambiarFrecuencia(FrecuenciaRespaldo.SEMANAL));

        assertEquals("ana@example.com", servicio.consultar().correo());
        assertEquals(FrecuenciaRespaldo.SEMANAL, servicio.consultar().frecuencia());
        assertEquals(FrecuenciaRespaldo.SEMANAL, programador.programada);
    }

    @Test
    public void sinCuentaNoSePuedeActivarUnaFrecuenciaPeroNuncaSiEsValida() {
        assertEquals(ResultadoRespaldo.SIN_CUENTA, servicio.cambiarFrecuencia(FrecuenciaRespaldo.DIARIA));
        assertEquals(FrecuenciaRespaldo.NUNCA, servicio.consultar().frecuencia());
        assertNull(programador.programada);

        assertEquals(ResultadoRespaldo.OK, servicio.cambiarFrecuencia(FrecuenciaRespaldo.NUNCA));
    }

    @Test
    public void soloManualYNuncaCancelanLaTareaPeriodica() {
        servicio.vincularCuenta("ana@example.com");
        servicio.cambiarFrecuencia(FrecuenciaRespaldo.MENSUAL);
        assertEquals(FrecuenciaRespaldo.MENSUAL, programador.programada);

        servicio.cambiarFrecuencia(FrecuenciaRespaldo.SOLO_MANUAL);
        assertNull(programador.programada);
        assertEquals(FrecuenciaRespaldo.SOLO_MANUAL, servicio.consultar().frecuencia());

        servicio.cambiarFrecuencia(FrecuenciaRespaldo.DIARIA);
        servicio.cambiarFrecuencia(FrecuenciaRespaldo.NUNCA);
        assertNull(programador.programada);
    }

    @Test(expected = IllegalArgumentException.class)
    public void noVinculaUnCorreoVacio() {
        servicio.vincularCuenta("   ");
    }

    @Test
    public void definirContrasenaLaGuardaProtegidaYMarcaElRespaldoComoCifrado() {
        assertEquals(ResultadoRespaldo.OK, servicio.definirContrasena("secreta".toCharArray()));

        assertTrue(servicio.consultar().conContrasena());
        assertEquals("secreta", new String(credencial.obtener()));
    }

    @Test
    public void unaContrasenaVaciaSeRechazaSinGuardarNada() {
        assertEquals(ResultadoRespaldo.CONTRASENA_REQUERIDA, servicio.definirContrasena(new char[0]));
        assertEquals(ResultadoRespaldo.CONTRASENA_REQUERIDA, servicio.definirContrasena(null));

        assertFalse(servicio.consultar().conContrasena());
        assertNull(credencial.obtener());
    }

    @Test
    public void quitarLaContrasenaBorraLaCredencial() {
        servicio.definirContrasena("secreta".toCharArray());

        servicio.quitarContrasena();

        assertFalse(servicio.consultar().conContrasena());
        assertNull(credencial.obtener());
    }

    @Test
    public void desvincularCancelaLaTareaOlvidaCuentaYContrasenaYDejaNuncaSinTocarDrive() {
        servicio.vincularCuenta("ana@example.com");
        servicio.cambiarFrecuencia(FrecuenciaRespaldo.SEMANAL);
        servicio.definirContrasena("secreta".toCharArray());
        configuracion.guardarUltimoRespaldo(new MetadatosRespaldo(1L, 100L, true));

        servicio.desvincular();

        ConfiguracionRespaldo estado = servicio.consultar();
        assertFalse(estado.hayCuenta());
        assertEquals(FrecuenciaRespaldo.NUNCA, estado.frecuencia());
        assertFalse(estado.conContrasena());
        assertNull(credencial.obtener());
        assertNull(programador.programada);
    }
}
