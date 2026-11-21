package io.github.jhoel0521.studyplan.application.service.configuracion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.entity.Ajustes;
import io.github.jhoel0521.studyplan.domain.enumeration.Idioma;
import io.github.jhoel0521.studyplan.domain.enumeration.Tema;
import io.github.jhoel0521.studyplan.fakes.FakeAjustesRepository;

/**
 * Pruebas de tema e idioma, con un repositorio falso.
 *
 * <p>Verifica: HU-03 (escenarios «Tema» e «Idioma»), RNF-07 y RNF-08.
 */
public class ConfigurarAparienciaServiceTest {

    private FakeAjustesRepository repositorio;
    private ConfigurarAparienciaService servicio;
    private ConsultarAjustesService consulta;

    @Before
    public void preparar() {
        repositorio = new FakeAjustesRepository();
        servicio = new ConfigurarAparienciaService(repositorio);
        consulta = new ConsultarAjustesService(repositorio);
    }

    @Test
    public void losValoresPorDefectoSonTemaDelSistemaYEspanol() {
        Ajustes a = consulta.ejecutar();
        assertEquals(Tema.SISTEMA, a.tema());
        assertEquals(Idioma.ES, a.idioma());
        assertFalse(a.bloqueoActivo());
    }

    @Test
    public void elegirTemaOscuroLoGuarda() {
        servicio.cambiarTema(Tema.OSCURO);
        assertEquals(Tema.OSCURO, consulta.ejecutar().tema());
    }

    @Test
    public void elegirInglesLoGuarda() {
        servicio.cambiarIdioma(Idioma.EN);
        assertEquals(Idioma.EN, consulta.ejecutar().idioma());
    }

    @Test
    public void cambiarElIdiomaConservaElTemaYViceversa() {
        servicio.cambiarTema(Tema.CLARO);
        servicio.cambiarIdioma(Idioma.EN);
        servicio.cambiarTema(Tema.OSCURO);
        Ajustes a = consulta.ejecutar();
        assertEquals(Tema.OSCURO, a.tema());
        assertEquals(Idioma.EN, a.idioma());
    }

    @Test
    public void cambiarLaAparienciaNoTocaElBloqueo() {
        repositorio.guardar(Ajustes.porDefecto().conBloqueoActivo(true));
        servicio.cambiarTema(Tema.OSCURO);
        assertTrue(consulta.ejecutar().bloqueoActivo());
    }

    @Test
    public void losAjustesSonInmutables() {
        Ajustes original = Ajustes.porDefecto();
        original.conTema(Tema.OSCURO);
        assertEquals(Tema.SISTEMA, original.tema());
    }
}
