package io.github.jhoel0521.studyplan.application.service;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.entity.Ajustes;
import io.github.jhoel0521.studyplan.domain.enumeration.EstadoArranque;
import io.github.jhoel0521.studyplan.domain.entity.Perfil;
import io.github.jhoel0521.studyplan.fakes.FakeAjustesRepository;
import io.github.jhoel0521.studyplan.fakes.FakePerfilRepository;

/**
 * Pruebas de qué pantalla corresponde al abrir la app.
 *
 * <p>Verifica: HU-01 (el primer uso no pide autenticación) y HU-04 (con bloqueo activo se pide al abrir y al volver
 * a primer plano; con el bloqueo inactivo la app queda abierta tras leer los ajustes).
 */
public class ConsultarEstadoArranqueServiceTest {

    private FakePerfilRepository perfiles;
    private FakeAjustesRepository ajustes;
    private SesionBloqueo sesion;
    private ConsultarEstadoArranqueService servicio;

    @Before
    public void preparar() {
        perfiles = new FakePerfilRepository();
        ajustes = new FakeAjustesRepository();
        sesion = new SesionBloqueo();
        servicio = new ConsultarEstadoArranqueService(perfiles, ajustes, sesion);
    }

    private void conPerfil() {
        perfiles.guardar(Perfil.crear("Ana", 20).valor());
    }

    @Test
    public void sinPerfilSeMuestraElPrimerUso() {
        assertEquals(EstadoArranque.SIN_PERFIL, servicio.ejecutar());
    }

    @Test
    public void sinPerfilGanaAlBloqueoPorqueNoHayDatosQueProteger() {
        ajustes.guardar(Ajustes.porDefecto().conBloqueoActivo(true));
        assertEquals(EstadoArranque.SIN_PERFIL, servicio.ejecutar());
    }

    @Test
    public void conPerfilYBloqueoActivoAlAbrirLaAppEstaBloqueada() {
        conPerfil();
        ajustes.guardar(Ajustes.porDefecto().conBloqueoActivo(true));
        assertEquals(EstadoArranque.BLOQUEADA, servicio.ejecutar());
    }

    @Test
    public void conPerfilYBloqueoInactivoLaAppQuedaAbiertaTrasLeerLosAjustes() {
        conPerfil();
        assertEquals(EstadoArranque.LISTA, servicio.ejecutar());
    }

    @Test
    public void abrirLaAppVariasVecesNoLaDesbloqueaSola() {
        conPerfil();
        ajustes.guardar(Ajustes.porDefecto().conBloqueoActivo(true));
        servicio.ejecutar();
        assertEquals(EstadoArranque.BLOQUEADA, servicio.ejecutar());
    }

    @Test
    public void trasAutenticarseLaAppEstaListaYAlVolverAPrimerPlanoSeBloqueaOtraVez() {
        conPerfil();
        ajustes.guardar(Ajustes.porDefecto().conBloqueoActivo(true));
        servicio.ejecutar();
        sesion.desbloquear();
        assertEquals(EstadoArranque.LISTA, servicio.ejecutar());
        sesion.alVolverAPrimerPlano();
        assertEquals(EstadoArranque.BLOQUEADA, servicio.ejecutar());
    }
}
