package io.github.jhoel0521.studyplan.application.service.configuracion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.entity.Perfil;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.fakes.FakePerfilRepository;

/**
 * Pruebas de los casos de uso de configurar y consultar el perfil, con un repositorio falso.
 *
 * <p>Verifica: HU-01 (Primer uso y Nombre vacío), RN-01 (perfil único) y RN-14.
 */
public class ConfigurarPerfilServiceTest {

    private FakePerfilRepository repositorio;
    private ConfigurarPerfilService configurar;
    private ConsultarPerfilService consultar;

    @Before
    public void preparar() {
        repositorio = new FakePerfilRepository();
        configurar = new ConfigurarPerfilService(repositorio);
        consultar = new ConsultarPerfilService(repositorio);
    }

    @Test
    public void sinConfigurarNoHayPerfil() {
        assertFalse(consultar.ejecutar().isPresent());
    }

    @Test
    public void primerUsoGuardaElPerfil() {
        Resultado<Perfil> r = configurar.ejecutar("Ana", 20);
        assertTrue(r.esValido());
        assertEquals("Ana", consultar.ejecutar().orElseThrow(AssertionError::new).nombre());
        assertEquals(1, repositorio.guardados);
    }

    @Test
    public void nombreVacioNoGuardaNada() {
        Resultado<Perfil> r = configurar.ejecutar("  ", 20);
        assertEquals(ErrorValidacion.NOMBRE_VACIO, r.error());
        assertEquals(0, repositorio.guardados);
        assertFalse(consultar.ejecutar().isPresent());
    }

    @Test
    public void metaInvalidaNoGuardaNada() {
        Resultado<Perfil> r = configurar.ejecutar("Ana", 169);
        assertEquals(ErrorValidacion.META_FUERA_DE_RANGO, r.error());
        assertEquals(0, repositorio.guardados);
    }

    @Test
    public void unSegundoPerfilReemplazaAlPrimeroPorqueSoloHayUno() {
        configurar.ejecutar("Ana", 20);
        configurar.ejecutar("Luis", 10);
        assertEquals("Luis", consultar.ejecutar().orElseThrow(AssertionError::new).nombre());
        assertEquals(10, consultar.ejecutar().orElseThrow(AssertionError::new).metaSemanalHoras(), 0);
    }
}
