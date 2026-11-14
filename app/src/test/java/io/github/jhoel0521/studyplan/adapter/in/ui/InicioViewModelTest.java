package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import java.util.Objects;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.github.jhoel0521.studyplan.application.service.ObtenerResumenInicioService;
import io.github.jhoel0521.studyplan.application.service.VencerSesionesPendientesService;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Perfil;
import io.github.jhoel0521.studyplan.domain.ResumenInicio;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakePerfilRepository;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;

/**
 * Pruebas del ViewModel de Inicio.
 *
 * <p>Verifica: HU-02.
 */
public class InicioViewModelTest {

    @Rule
    public InstantTaskExecutorRule reglaInstantanea = new InstantTaskExecutorRule();

    private FakePerfilRepository perfiles;
    private InicioViewModel modelo;

    @Before
    public void preparar() {
        perfiles = new FakePerfilRepository();
        FakeSesionRepository sesiones = new FakeSesionRepository();
        FakeMateriaRepository materias = new FakeMateriaRepository();
        FakeReloj reloj = new FakeReloj(
                Fecha.de(2026, 11, 14).valor(), Hora.de(10, 0).valor());
        modelo = new InicioViewModel(
                new ObtenerResumenInicioService(
                        perfiles,
                        sesiones,
                        materias,
                        reloj,
                        new VencerSesionesPendientesService(sesiones, reloj)),
                Runnable::run);
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
