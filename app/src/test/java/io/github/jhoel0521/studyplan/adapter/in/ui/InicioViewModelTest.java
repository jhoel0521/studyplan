package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import java.util.Objects;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.github.jhoel0521.studyplan.application.service.ObtenerResumenInicioService;
import io.github.jhoel0521.studyplan.domain.Perfil;
import io.github.jhoel0521.studyplan.domain.ResumenInicio;
import io.github.jhoel0521.studyplan.fakes.FakePerfilRepository;

/**
 * Pruebas del ViewModel de Inicio.
 *
 * <p>Verifica, de forma parcial: HU-02 (saludo y progreso {@code 0 / meta}).
 */
public class InicioViewModelTest {

    @Rule
    public InstantTaskExecutorRule reglaInstantanea = new InstantTaskExecutorRule();

    private FakePerfilRepository perfiles;
    private InicioViewModel modelo;

    @Before
    public void preparar() {
        perfiles = new FakePerfilRepository();
        modelo = new InicioViewModel(new ObtenerResumenInicioService(perfiles), Runnable::run);
    }

    @Test
    public void conPerfilPublicaElSaludoYElProgresoEnCero() {
        perfiles.guardar(Perfil.crear("Ana", 20).valor());
        modelo.cargar();
        assertEquals("Ana", resumen().nombre());
        assertEquals(0, resumen().horasRealizadas(), 0);
        assertEquals(20, resumen().metaSemanalHoras(), 0);
    }

    @Test
    public void sinPerfilNoPublicaNada() {
        modelo.cargar();
        assertNull(modelo.resumen().getValue());
    }

    private ResumenInicio resumen() {
        return Objects.requireNonNull(modelo.resumen().getValue());
    }
}
