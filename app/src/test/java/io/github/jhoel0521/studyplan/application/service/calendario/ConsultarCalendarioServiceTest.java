package io.github.jhoel0521.studyplan.application.service.calendario;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.readmodel.BloqueHorarioCalendario;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.readmodel.DatosCalendario;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.FiltroCalendario;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.valueobject.PeriodoCalendario;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.VistaCalendario;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;
import io.github.jhoel0521.studyplan.fakes.FakeTramoDiarioRepository;
import io.github.jhoel0521.studyplan.application.service.sesiones.VencerSesionesPendientesService;

/**
 * Pruebas unitarias de {@link ConsultarCalendarioService} (HU-08, HU-16 a HU-21, RN-08, RN-11).
 */
public class ConsultarCalendarioServiceTest {

    private FakeSesionRepository sesiones;
    private FakeMateriaRepository materias;
    private FakeReloj reloj;
    private ConsultarCalendarioService servicio;

    private Materia calculo;
    private Materia fisicaArchivada;

    @Before
    public void configurar() {
        sesiones = new FakeSesionRepository();
        materias = new FakeMateriaRepository();
        Fecha hoy = Fecha.de(2026, 11, 16).valor(); // lunes
        reloj = new FakeReloj(hoy, Hora.de(10, 0).valor());
        VencerSesionesPendientesService vencer = new VencerSesionesPendientesService(sesiones, reloj);
        servicio = new ConsultarCalendarioService(sesiones, materias, reloj, vencer);

        calculo = materias.guardar(Materia.crear("Cálculo", 0xFF1565C0, "Prof. A").valor());
        Materia fisica = materias.guardar(Materia.crear("Física", 0xFF2E7D32, "Prof. B").valor());
        fisicaArchivada = materias.guardar(fisica.conArchivada(true));
    }

    @Test
    public void vencePendientesPasadasYCalculaResumenSoloSobrePeriodoVisible() {
        Fecha ayer = Fecha.de(2026, 11, 15).valor(); // domingo semana anterior (y mes nov 2026)
        Fecha hoy = Fecha.de(2026, 11, 16).valor(); // lunes
        Fecha oct31 = Fecha.de(2026, 10, 31).valor(); // mes anterior visible en cuadrícula nov? 1 nov 2026 es dom -> 26 oct..1 nov

        // Sesión pendiente vencida en oct 2026 (visible en cuadrícula de nov, pero fuera del mes visible)
        SesionEstudio vencidaOct = SesionEstudio.planificar(
                calculo.id(),
                Fecha.de(2026, 10, 28).valor(),
                Fecha.de(2026, 10, 28).valor(),
                Hora.de(9, 0).valor(),
                Hora.de(10, 0).valor(),
                "",
                null).valor();
        sesiones.guardar(vencidaOct);

        // Sesión pendiente vencida ayer (nov 15)
        SesionEstudio vencidaAyer = SesionEstudio.planificar(
                calculo.id(),
                ayer,
                ayer,
                Hora.de(14, 0).valor(),
                Hora.de(15, 30).valor(),
                "",
                null).valor();
        sesiones.guardar(vencidaAyer);

        // Sesión completada hoy (nov 16)
        SesionEstudio hoyPlan = SesionEstudio.planificar(
                calculo.id(),
                hoy,
                hoy,
                Hora.de(8, 0).valor(),
                Hora.de(9, 0).valor(),
                "",
                null).valor();
        SesionEstudio hoyCompletada = hoyPlan.completar(hoy, 55).valor();
        sesiones.guardar(hoyCompletada);

        // En vista SEMANA (16..22 nov): solo cuenta la de hoy (Plan 60, Real 55)
        PeriodoCalendario semana = PeriodoCalendario.inicial(hoy);
        DatosCalendario datosSemana = servicio.ejecutar(semana, FiltroCalendario.porDefecto());
        assertEquals(1, datosSemana.bloquesHorario().size());
        assertEquals(60, datosSemana.resumen().minutosPlanificados());
        assertEquals(55, datosSemana.resumen().minutosRealizados());
        assertEquals(6 * 60, datosSemana.minutoScrollInicial());

        // En vista MES (nov 2026): la de oct 28 ya fue vencida a NO_REALIZADA pero NO suma al resumen de nov
        PeriodoCalendario mes = semana.cambiarVista(VistaCalendario.MES, hoy).seleccionarDia(ayer);
        DatosCalendario datosMes = servicio.ejecutar(mes, FiltroCalendario.porDefecto());
        assertEquals(42, datosMes.celdasMes().size()); // 26 oct (lun) .. 6 dic (dom) = 42 celdas
        assertEquals(1, datosMes.sesionesDiaSeleccionado().size());
        assertEquals(CodigoEstadoSesion.NO_REALIZADA, datosMes.sesionesDiaSeleccionado().get(0).codigoEstado());
        assertEquals(150, datosMes.resumen().minutosPlanificados()); // 90 (nov 15) + 60 (nov 16)
        assertEquals(55, datosMes.resumen().minutosRealizados());
    }

    @Test
    public void filtraPorMateriaYPorInclusionDeArchivadas() {
        Fecha hoy = Fecha.de(2026, 11, 16).valor();
        sesiones.guardar(SesionEstudio.planificar(
                calculo.id(),
                hoy,
                hoy,
                Hora.de(9, 0).valor(),
                Hora.de(10, 0).valor(),
                "",
                null).valor());
        sesiones.guardar(SesionEstudio.planificar(
                fisicaArchivada.id(),
                hoy,
                hoy,
                Hora.de(11, 0).valor(),
                Hora.de(12, 0).valor(),
                "",
                null).valor());

        PeriodoCalendario dia = PeriodoCalendario.de(VistaCalendario.DIA, hoy, hoy);

        DatosCalendario conArchivadas = servicio.ejecutar(dia, FiltroCalendario.porDefecto());
        assertEquals(2, conArchivadas.bloquesHorario().size());
        assertEquals(2, conArchivadas.materiasDisponibles().size());

        DatosCalendario sinArchivadas = servicio.ejecutar(
                dia, FiltroCalendario.porDefecto().conIncluirArchivadas(false));
        assertEquals(1, sinArchivadas.bloquesHorario().size());
        assertEquals(1, sinArchivadas.materiasDisponibles().size());
        assertFalse(sinArchivadas.bloquesHorario().get(0).item().materiaArchivada());

        DatosCalendario soloArchivada = servicio.ejecutar(
                dia, FiltroCalendario.porDefecto().conMateria(fisicaArchivada.id()));
        assertEquals(1, soloArchivada.bloquesHorario().size());
        assertTrue(soloArchivada.bloquesHorario().get(0).item().materiaArchivada());
    }

    @Test
    public void sesionNocturnaGeneraDosBloquesYSumaRealPorDiaConTramosRn31() {
        Fecha domingo = Fecha.de(2026, 11, 15).valor();
        Fecha lunes = Fecha.de(2026, 11, 16).valor();
        Fecha martes = Fecha.de(2026, 11, 17).valor();
        FakeTramoDiarioRepository tramos = new FakeTramoDiarioRepository();
        VencerSesionesPendientesService vencer = new VencerSesionesPendientesService(sesiones, reloj);
        ConsultarCalendarioService conTramos =
                new ConsultarCalendarioService(sesiones, materias, tramos, reloj, vencer);

        // Sesión domingo 23:30-00:30 (60m: 30m semana anterior, 30m esta semana)
        SesionEstudio domLun = sesiones.guardar(SesionEstudio.planificar(
                calculo.id(), domingo, domingo,
                Hora.de(23, 30).valor(), Hora.de(0, 30).valor(), 0, "", null)
                .valor().iniciar(io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio.LIBRE, null)
                .valor().detener(60).valor());
        java.util.Map<Fecha, Integer> mapa = new java.util.TreeMap<>();
        mapa.put(domingo, 30);
        mapa.put(lunes, 30);
        tramos.guardarTramos(domLun.id(), mapa);

        // Sesión nocturna lunes 22:00-01:00 martes
        sesiones.guardar(SesionEstudio.planificar(
                calculo.id(), lunes, lunes,
                Hora.de(22, 0).valor(), Hora.de(1, 0).valor(), 0, "", null).valor());

        DatosCalendario semana = conTramos.ejecutar(
                PeriodoCalendario.inicial(lunes), FiltroCalendario.porDefecto());
        assertEquals(30, semana.resumen().minutosRealizados());

        int bloquesNocturna = 0;
        boolean tieneContinuacionMartes = false;
        for (BloqueHorarioCalendario b : semana.bloquesHorario()) {
            if (b.item().fechaInicioOriginal().equals(lunes)
                    && b.item().horaInicio().equals(Hora.de(22, 0).valor())
                    && b.minutoFin() == 1440) {
                bloquesNocturna++;
            }
            if (b.item().esContinuacionDiaAnterior() && b.item().fecha().equals(martes)) {
                bloquesNocturna++;
                tieneContinuacionMartes = true;
            }
        }
        assertEquals(2, bloquesNocturna);
        assertTrue(tieneContinuacionMartes);
    }
}
