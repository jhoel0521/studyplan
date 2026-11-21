package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import java.util.Objects;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.github.jhoel0521.studyplan.application.service.tecnicas.ConfigurarAjustesEstudioService;
import io.github.jhoel0521.studyplan.application.service.configuracion.ConfigurarAparienciaService;
import io.github.jhoel0521.studyplan.application.service.configuracion.ConfigurarBloqueoService;
import io.github.jhoel0521.studyplan.application.service.recordatorios.ConfigurarRecordatorioPorDefectoService;
import io.github.jhoel0521.studyplan.application.service.configuracion.ConsultarAjustesService;
import io.github.jhoel0521.studyplan.application.service.configuracion.SesionBloqueo;
import io.github.jhoel0521.studyplan.domain.entity.Ajustes;
import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.domain.enumeration.Idioma;
import io.github.jhoel0521.studyplan.domain.enumeration.Tema;
import io.github.jhoel0521.studyplan.fakes.FakeAjustesRepository;
import io.github.jhoel0521.studyplan.fakes.FakeDisponibilidadBloqueo;

/**
 * Pruebas del ViewModel de ajustes: cada acción llama al caso de uso correcto.
 *
 * <p>Verifica: HU-03 (tema e idioma) y HU-04 (activar el bloqueo con y sin método en el teléfono).
 */
public class AjustesViewModelTest {

    @Rule
    public InstantTaskExecutorRule reglaInstantanea = new InstantTaskExecutorRule();

    private FakeAjustesRepository repositorio;
    private FakeDisponibilidadBloqueo disponibilidad;
    private AjustesViewModel modelo;

    @Before
    public void preparar() {
        repositorio = new FakeAjustesRepository();
        disponibilidad = new FakeDisponibilidadBloqueo();
        modelo = new AjustesViewModel(
                new ConsultarAjustesService(repositorio),
                new ConfigurarAparienciaService(repositorio),
                new ConfigurarBloqueoService(repositorio, disponibilidad, new SesionBloqueo()),
                new ConfigurarAjustesEstudioService(repositorio),
                new ConfigurarRecordatorioPorDefectoService(repositorio),
                Runnable::run);
    }

    @Test
    public void cargarPublicaLosAjustesPorDefecto() {
        modelo.cargar();
        assertEquals(Tema.SISTEMA, ajustesPublicados().tema());
        assertEquals(Idioma.ES, ajustesPublicados().idioma());
        assertEquals(AntelacionRecordatorio.MINUTOS_15, ajustesPublicados().recordatorioPorDefecto());
    }

    @Test
    public void elegirTemaOscuroLoGuardaYLoPublica() {
        modelo.elegirTema(Tema.OSCURO);
        assertEquals(Tema.OSCURO, repositorio.obtener().tema());
        assertEquals(Tema.OSCURO, ajustesPublicados().tema());
    }

    @Test
    public void elegirInglesLoGuardaYLoPublica() {
        modelo.elegirIdioma(Idioma.EN);
        assertEquals(Idioma.EN, repositorio.obtener().idioma());
        assertEquals(Idioma.EN, ajustesPublicados().idioma());
    }

    @Test
    public void activarElBloqueoConMetodoLoGuardaSinAviso() {
        disponibilidad.disponible = true;
        modelo.cambiarBloqueo(true);
        assertTrue(repositorio.obtener().bloqueoActivo());
        assertNull(modelo.avisoSinBloqueo().getValue());
    }

    @Test
    public void activarElBloqueoSinMetodoPublicaElAvisoYNoLoGuarda() {
        disponibilidad.disponible = false;
        modelo.cambiarBloqueo(true);
        assertEquals(Boolean.TRUE, modelo.avisoSinBloqueo().getValue());
        assertFalse(ajustesPublicados().bloqueoActivo());
    }

    @Test
    public void desactivarElBloqueoLoGuarda() {
        disponibilidad.disponible = true;
        modelo.cambiarBloqueo(true);
        modelo.cambiarBloqueo(false);
        assertFalse(repositorio.obtener().bloqueoActivo());
    }

    @Test
    public void elegirRecordatorioPorDefectoLoGuardaYLoPublica() {
        modelo.elegirRecordatorioPorDefecto(AntelacionRecordatorio.MINUTOS_30);
        assertEquals(
                AntelacionRecordatorio.MINUTOS_30,
                repositorio.obtener().recordatorioPorDefecto());
        assertEquals(
                AntelacionRecordatorio.MINUTOS_30,
                ajustesPublicados().recordatorioPorDefecto());

        modelo.elegirRecordatorioPorDefecto(AntelacionRecordatorio.SIN_RECORDATORIO);
        assertEquals(
                AntelacionRecordatorio.SIN_RECORDATORIO,
                repositorio.obtener().recordatorioPorDefecto());
    }

    private Ajustes ajustesPublicados() {
        return Objects.requireNonNull(modelo.ajustes().getValue());
    }
}
