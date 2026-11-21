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

import io.github.jhoel0521.studyplan.application.service.ListarMateriasService;
import io.github.jhoel0521.studyplan.application.service.ObtenerResumenInicioService;
import io.github.jhoel0521.studyplan.application.service.VencerSesionesPendientesService;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.entity.Perfil;
import io.github.jhoel0521.studyplan.domain.readmodel.ResumenInicio;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
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
    private FakeMateriaRepository materias;
    private FakeSesionRepository sesiones;
    private InicioViewModel modelo;

    @Before
    public void preparar() {
        perfiles = new FakePerfilRepository();
        sesiones = new FakeSesionRepository();
        materias = new FakeMateriaRepository();
        FakeReloj reloj = new FakeReloj(
                Fecha.de(2026, 11, 14).valor(), Hora.de(10, 0).valor());
        modelo = new InicioViewModel(
                new ObtenerResumenInicioService(
                        perfiles,
                        sesiones,
                        materias,
                        reloj,
                        new VencerSesionesPendientesService(sesiones, reloj)),
                new ListarMateriasService(materias, sesiones),
                Runnable::run);
    }

    @Test
    public void conPerfilPublicaElSaludoYElProgresoEnCero() {
        perfiles.guardar(Perfil.crear("Ana", 20).valor());
        modelo.cargar();
        assertEquals("Ana", resumen().nombre());
        assertEquals(0, resumen().horasRealizadas(), 0);
        assertEquals(20, resumen().metaSemanalHoras(), 0);
        assertFalse(Boolean.TRUE.equals(modelo.hayMateriasActivas().getValue()));
    }

    @Test
    public void publicaMateriasActivasProximaSesionYSesionesDeHoy() {
        perfiles.guardar(Perfil.crear("Ana", 10).valor());
        long materiaId = materias.guardar(Materia.crear("Cálculo", 0xFF1E88E5, null).valor()).id();
        Fecha hoy = Fecha.de(2026, 11, 14).valor();
        sesiones.guardar(SesionEstudio.planificar(
                materiaId,
                hoy,
                hoy,
                Hora.de(14, 0).valor(),
                Hora.de(15, 0).valor(),
                null,
                null).valor());

        modelo.cargar();

        assertTrue(Boolean.TRUE.equals(modelo.hayMateriasActivas().getValue()));
        assertTrue(resumen().proximaSesion() != null);
        assertEquals(1, resumen().sesionesDeHoy().size());
        assertEquals("Cálculo", resumen().sesionesDeHoy().get(0).nombreMateria());
    }

    @Test
    public void sinPerfilNoPublicaNada() {
        modelo.cargar();
        assertNull(modelo.resumen().getValue());
    }

    @Test
    public void defineMetaSemanalYArrancaEstudiarAhora() {
        perfiles.guardar(Perfil.crear("Ana", 10).valor());
        io.github.jhoel0521.studyplan.fakes.FakeMetaSemanalRepository metas =
                new io.github.jhoel0521.studyplan.fakes.FakeMetaSemanalRepository();
        io.github.jhoel0521.studyplan.fakes.FakeReloj reloj =
                new io.github.jhoel0521.studyplan.fakes.FakeReloj(
                        Fecha.de(2026, 11, 14).valor(), Hora.de(10, 0).valor());
        io.github.jhoel0521.studyplan.application.service.DefinirMetaSemanalService defMeta =
                new io.github.jhoel0521.studyplan.application.service.DefinirMetaSemanalService(metas, reloj);
        io.github.jhoel0521.studyplan.application.service.EstudiarAhoraService estAhora =
                new io.github.jhoel0521.studyplan.application.service.EstudiarAhoraService(
                        sesiones,
                        new io.github.jhoel0521.studyplan.fakes.FakeEventoEstudioRepository(),
                        materias,
                        new io.github.jhoel0521.studyplan.fakes.FakeAjustesRepository(),
                        new io.github.jhoel0521.studyplan.fakes.FakeControladorNoMolestar(),
                        new io.github.jhoel0521.studyplan.fakes.FakeProgramadorAvisosSesion(),
                        new io.github.jhoel0521.studyplan.fakes.FakeMarcadorSalidaApp(),
                        reloj);
        InicioViewModel vm = new InicioViewModel(
                new io.github.jhoel0521.studyplan.application.service.ObtenerResumenInicioService(
                        perfiles,
                        sesiones,
                        materias,
                        new io.github.jhoel0521.studyplan.fakes.FakeTramoDiarioRepository(sesiones),
                        metas,
                        reloj,
                        () -> 0),
                new io.github.jhoel0521.studyplan.application.service.ListarMateriasService(materias, sesiones),
                defMeta,
                estAhora,
                null,
                Runnable::run);

        vm.definirMetaSemana("16");
        assertEquals(Boolean.TRUE, vm.metaActualizada().getValue());
        assertEquals(16.0, vm.resumen().getValue().metaSemanalHoras(), 1e-6);

        vm.iniciarEstudiarAhora(null, io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio.LIBRE);
        assertTrue(vm.sesionIniciadaId().getValue() != null);
    }

    private ResumenInicio resumen() {
        return Objects.requireNonNull(modelo.resumen().getValue());
    }
}
