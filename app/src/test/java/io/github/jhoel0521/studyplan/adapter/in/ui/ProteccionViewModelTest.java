package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.github.jhoel0521.studyplan.application.service.configuracion.ConsultarEstadoArranqueService;
import io.github.jhoel0521.studyplan.application.service.configuracion.SesionBloqueo;
import io.github.jhoel0521.studyplan.domain.entity.Ajustes;
import io.github.jhoel0521.studyplan.domain.enumeration.EstadoArranque;
import io.github.jhoel0521.studyplan.domain.entity.Perfil;
import io.github.jhoel0521.studyplan.fakes.FakeAjustesRepository;
import io.github.jhoel0521.studyplan.fakes.FakePerfilRepository;

/**
 * Pruebas del ViewModel de protección: cada estado de arranque se publica tal cual para que la pantalla abra la
 * acción correcta (primer uso, autenticación o contenido).
 *
 * <p>Verifica: HU-01 (primer uso sin autenticación) y HU-04 (autenticar al abrir y al volver).
 */
public class ProteccionViewModelTest {

    @Rule
    public InstantTaskExecutorRule reglaInstantanea = new InstantTaskExecutorRule();

    private FakePerfilRepository perfiles;
    private FakeAjustesRepository ajustes;
    private ProteccionViewModel modelo;

    @Before
    public void preparar() {
        perfiles = new FakePerfilRepository();
        ajustes = new FakeAjustesRepository();
        modelo = new ProteccionViewModel(
                new ConsultarEstadoArranqueService(perfiles, ajustes, new SesionBloqueo()), Runnable::run);
    }

    @Test
    public void sinPerfilPublicaSinPerfil() {
        modelo.comprobar();
        assertEquals(EstadoArranque.SIN_PERFIL, modelo.estado().getValue());
    }

    @Test
    public void conPerfilYBloqueoActivoPublicaBloqueada() {
        perfiles.guardar(Perfil.crear("Ana", 20).valor());
        ajustes.guardar(Ajustes.porDefecto().conBloqueoActivo(true));
        modelo.comprobar();
        assertEquals(EstadoArranque.BLOQUEADA, modelo.estado().getValue());
    }

    @Test
    public void conPerfilYSinBloqueoPublicaLista() {
        perfiles.guardar(Perfil.crear("Ana", 20).valor());
        modelo.comprobar();
        assertEquals(EstadoArranque.LISTA, modelo.estado().getValue());
    }

    @Test
    public void consumirElEstadoLoLimpia() {
        modelo.comprobar();
        modelo.consumirEstado();
        assertNull(modelo.estado().getValue());
    }
}
