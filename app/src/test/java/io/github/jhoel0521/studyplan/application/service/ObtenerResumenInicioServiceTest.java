package io.github.jhoel0521.studyplan.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.Perfil;
import io.github.jhoel0521.studyplan.domain.ResumenInicio;
import io.github.jhoel0521.studyplan.fakes.FakePerfilRepository;

/**
 * Pruebas del resumen parcial de Inicio.
 *
 * <p>Verifica, de forma parcial: HU-02 (saludo y progreso {@code 0 / meta}) y RN-15 (la meta es la del perfil).
 * Quedan pendientes los escenarios «Resumen con datos» y «Acceso rápido», que dependen de sesiones.
 */
public class ObtenerResumenInicioServiceTest {

    private FakePerfilRepository perfiles;
    private ObtenerResumenInicioService servicio;

    @Before
    public void preparar() {
        perfiles = new FakePerfilRepository();
        servicio = new ObtenerResumenInicioService(perfiles);
    }

    @Test
    public void sinPerfilNoHayResumen() {
        assertFalse(servicio.ejecutar().isPresent());
    }

    @Test
    public void conPerfilMuestraNombreYProgresoCeroSobreLaMeta() {
        perfiles.guardar(Perfil.crear("Ana", 20).valor());
        ResumenInicio resumen = servicio.ejecutar().orElseThrow(AssertionError::new);
        assertEquals("Ana", resumen.nombre());
        assertEquals(0, resumen.horasRealizadas(), 0);
        assertEquals(20, resumen.metaSemanalHoras(), 0);
        assertEquals(0, resumen.porcentajeProgreso());
    }

    @Test
    public void elPorcentajeDeProgresoNuncaSuperaElCien() {
        assertEquals(60, new ResumenInicio("Ana", 12, 20).porcentajeProgreso());
        assertEquals(100, new ResumenInicio("Ana", 30, 20).porcentajeProgreso());
        assertEquals(0, new ResumenInicio("Ana", 0, 20).porcentajeProgreso());
    }
}
