package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import java.util.Objects;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.github.jhoel0521.studyplan.application.service.ConfigurarPerfilService;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.entity.Perfil;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.fakes.FakePerfilRepository;

/**
 * Pruebas del ViewModel del perfil inicial con ejecución instantánea.
 *
 * <p>Verifica: HU-01 (Nombre vacío y Primer uso) y RNF-14 (la meta debe ser un número válido).
 */
public class PerfilInicialViewModelTest {

    @Rule
    public InstantTaskExecutorRule reglaInstantanea = new InstantTaskExecutorRule();

    private FakePerfilRepository repositorio;
    private PerfilInicialViewModel modelo;

    @Before
    public void preparar() {
        repositorio = new FakePerfilRepository();
        modelo = new PerfilInicialViewModel(new ConfigurarPerfilService(repositorio), Runnable::run);
    }

    @Test
    public void unNombreVacioPublicaElCodigoNombreVacio() {
        modelo.guardar("   ", "20");
        assertEquals(ErrorValidacion.NOMBRE_VACIO, resultadoPublicado().error());
        assertEquals(0, repositorio.guardados);
    }

    @Test
    public void unaMetaQueNoEsUnNumeroPublicaMetaFueraDeRango() {
        modelo.guardar("Ana", "abc");
        assertEquals(ErrorValidacion.META_FUERA_DE_RANGO, resultadoPublicado().error());
    }

    @Test
    public void unaMetaConComaDecimalSeAcepta() {
        modelo.guardar("Ana", "12,5");
        assertTrue(resultadoPublicado().esValido());
        assertEquals(12.5, repositorio.obtener().orElseThrow(AssertionError::new).metaSemanalHoras(), 0);
    }

    @Test
    public void unPerfilValidoPublicaGuardado() {
        modelo.guardar("Ana", "20");
        assertTrue(resultadoPublicado().esValido());
        assertEquals(1, repositorio.guardados);
    }

    @Test
    public void consumirElResultadoLoLimpia() {
        modelo.guardar("Ana", "20");
        modelo.consumirResultado();
        assertNull(modelo.resultado().getValue());
    }

    private Resultado<Perfil> resultadoPublicado() {
        return Objects.requireNonNull(modelo.resultado().getValue());
    }
}
