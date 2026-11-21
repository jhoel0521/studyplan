package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.github.jhoel0521.studyplan.application.service.calendario.ConsultarCalendarioService;
import io.github.jhoel0521.studyplan.application.service.sesiones.VencerSesionesPendientesService;
import io.github.jhoel0521.studyplan.domain.readmodel.DatosCalendario;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.FiltroCalendario;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.VistaCalendario;
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

    @Test
    public void sesionNocturnaSeDivideEnDosBloquesEnSemanaYMarcaContinuacionEnDia() {
        Fecha lunes = Fecha.de(2026, 11, 16).valor();
        Fecha martes = lunes.sumarDias(1);
        sesiones.guardar(SesionEstudio.planificar(
                algebra.id(), lunes, lunes,
                Hora.de(22, 0).valor(), Hora.de(1, 0).valor(), 0, "", null).valor());

        modelo.cargar();
        DatosCalendario semana = modelo.datos().getValue();
        assertNotNull(semana);
        assertEquals(2, semana.bloquesHorario().size());
        assertEquals(0, semana.bloquesHorario().get(0).diaColumna());
        assertEquals(1440, semana.bloquesHorario().get(0).minutoFin());
        assertEquals(1, semana.bloquesHorario().get(1).diaColumna());
        assertEquals(0, semana.bloquesHorario().get(1).minutoInicio());
        assertEquals(60, semana.bloquesHorario().get(1).minutoFin());

        modelo.cambiarVista(VistaCalendario.DIA);
        modelo.siguiente();
        DatosCalendario diaMartes = modelo.datos().getValue();
        assertNotNull(diaMartes);
        assertEquals(1, diaMartes.bloquesHorario().size());
        assertEquals(true, diaMartes.bloquesHorario().get(0).item().esContinuacionDiaAnterior());
        assertEquals(lunes, diaMartes.bloquesHorario().get(0).item().fechaInicioOriginal());
    }
}
