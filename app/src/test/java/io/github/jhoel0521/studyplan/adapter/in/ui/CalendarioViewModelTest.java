package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.github.jhoel0521.studyplan.application.service.ConsultarCalendarioService;
import io.github.jhoel0521.studyplan.application.service.VencerSesionesPendientesService;
import io.github.jhoel0521.studyplan.domain.DatosCalendario;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.FiltroCalendario;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.VistaCalendario;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;

/**
 * Pruebas unitarias de {@link CalendarioViewModel} y {@link PreferenciaVistaCalendario}
 * (HU-16 a HU-21, RNF-02, RNF-10).
 */
public class CalendarioViewModelTest {

    @Rule
    public final InstantTaskExecutorRule reglaInstantanea = new InstantTaskExecutorRule();

    private FakeSesionRepository sesiones;
    private FakeMateriaRepository materias;
    private FakeReloj reloj;
    private PreferenciaVistaCalendario preferencia;
    private ConsultarCalendarioService servicio;
    private CalendarioViewModel modelo;
    private Materia algebra;

    @Before
    public void configurar() {
        sesiones = new FakeSesionRepository();
        materias = new FakeMateriaRepository();
        Fecha hoy = Fecha.de(2026, 11, 16).valor();
        reloj = new FakeReloj(hoy, Hora.de(9, 0).valor());
        preferencia = PreferenciaVistaCalendario.enMemoria();
        servicio = new ConsultarCalendarioService(
                sesiones, materias, reloj, new VencerSesionesPendientesService(sesiones, reloj));
        modelo = new CalendarioViewModel(servicio, reloj, preferencia, Runnable::run);

        algebra = materias.guardar(Materia.crear("Álgebra", 0xFF1E88E5, null).valor());
    }

    @Test
    public void arrancaEnSemanaYRecuerdaUltimaVistaEntreInstancias() {
        modelo.cargar();
        DatosCalendario inicial = modelo.datos().getValue();
        assertNotNull(inicial);
        assertEquals(VistaCalendario.SEMANA, inicial.periodo().vista());

        modelo.cambiarVista(VistaCalendario.MES);
        assertEquals(VistaCalendario.MES, preferencia.leer());

        // Nueva instancia del ViewModel con la misma preferencia arranca en MES
        CalendarioViewModel segundo =
                new CalendarioViewModel(servicio, reloj, preferencia, Runnable::run);
        segundo.cargar();
        assertEquals(VistaCalendario.MES, segundo.datos().getValue().periodo().vista());
    }

    @Test
    public void cambiaVistaConservandoFocoNavegaYAplicaFiltrosYRecarga() {
        Fecha hoy = Fecha.de(2026, 11, 16).valor();
        modelo.cargar();
        modelo.cambiarVista(VistaCalendario.MES);
        modelo.anterior(); // octubre 2026
        modelo.anterior(); // septiembre 2026
        modelo.cambiarVista(VistaCalendario.SEMANA);

        DatosCalendario enSemana = modelo.datos().getValue();
        assertEquals(VistaCalendario.SEMANA, enSemana.periodo().vista());
        assertEquals(Fecha.de(2026, 8, 31).valor(), enSemana.periodo().rangoVisibleInicio());

        modelo.irAHoy();
        assertEquals(hoy, modelo.datos().getValue().periodo().fechaFoco());

        sesiones.guardar(SesionEstudio.planificar(
                algebra.id(), hoy, hoy, Hora.de(10, 0).valor(), Hora.de(11, 0).valor(), "", null)
                .valor());
        modelo.cargar();
        assertEquals(1, modelo.datos().getValue().bloquesHorario().size());

        modelo.aplicarFiltro(FiltroCalendario.porDefecto().conMateria(999L));
        assertEquals(0, modelo.datos().getValue().bloquesHorario().size());

        modelo.restablecerFiltro();
        assertEquals(1, modelo.datos().getValue().bloquesHorario().size());
    }
}
