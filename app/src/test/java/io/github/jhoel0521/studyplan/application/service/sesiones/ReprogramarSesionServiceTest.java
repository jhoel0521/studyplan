package io.github.jhoel0521.studyplan.application.service.sesiones;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.readmodel.ResultadoSesion;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeProgramadorRecordatorios;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Pruebas unitarias de {@link ReprogramarSesionService} (HU-14, RN-04, RN-05, RN-07, RN-09).
 */
public class ReprogramarSesionServiceTest {

    // Martes 2026-11-10 -> hoy Jueves 2026-11-12
    private static final Fecha HOY = Fecha.de(2026, 11, 12).valor();
    private FakeSesionRepository sesiones;
    private FakeMateriaRepository materias;
    private ReprogramarSesionService servicio;
    private long algebraId;

    @Before
    public void preparar() {
        sesiones = new FakeSesionRepository();
        materias = new FakeMateriaRepository();
        FakeReloj reloj = new FakeReloj(HOY, Hora.de(9, 0).valor());
        servicio = new ReprogramarSesionService(sesiones, materias, reloj);
        algebraId = materias.guardar(Materia.crear("Álgebra", 0xFF1E88E5, null).valor()).id();
    }

    @Test
    public void reprogramaNoRealizadaEnlazandoConNuevaPendienteYPermiteSolaparConsigoMisma() {
        SesionEstudio martesNoRealizada = sesiones.guardar(SesionEstudio.restaurar(
                null,
                algebraId,
                HOY.sumarDias(-2),
                Hora.de(10, 0).valor(),
                Hora.de(11, 0).valor(),
                60,
                null,
                CodigoEstadoSesion.NO_REALIZADA,
                "Notas previas",
                null));

        ResultadoSesion res = servicio.ejecutar(
                martesNoRealizada.id(),
                HOY,
                Hora.de(16, 0).valor(),
                Hora.de(17, 0).valor(),
                null);

        assertTrue(res.esValido());
        assertEquals(CodigoEstadoSesion.PENDIENTE, res.sesion().codigoEstado());
        assertEquals(martesNoRealizada.id(), res.sesion().sesionOrigenId());
        assertEquals("Notas previas", res.sesion().notas());
        assertEquals(
                CodigoEstadoSesion.REPROGRAMADA,
                sesiones.buscarPorId(martesNoRealizada.id()).orElseThrow(AssertionError::new)
                        .codigoEstado());

        // Reprogramar PENDIENTE del mismo día solapando con su propio horario original (16:00-17:00 -> 16:30-17:30)
        ResultadoSesion mismaFecha = servicio.ejecutar(
                res.sesion().id(),
                HOY,
                Hora.de(16, 30).valor(),
                Hora.de(17, 30).valor(),
                "Movida 30 min");
        assertTrue(mismaFecha.esValido());
        assertEquals("Movida 30 min", mismaFecha.sesion().notas());
    }

    @Test
    public void bloqueaCruceConOtraSesionFechaPasadaYEstadoFinal() {
        SesionEstudio ocupada = sesiones.guardar(SesionEstudio.planificar(
                algebraId,
                HOY,
                HOY,
                Hora.de(16, 0).valor(),
                Hora.de(17, 0).valor(),
                null,
                null).valor());
        SesionEstudio pendiente = sesiones.guardar(SesionEstudio.planificar(
                algebraId,
                HOY,
                HOY,
                Hora.de(10, 0).valor(),
                Hora.de(11, 0).valor(),
                null,
                null).valor());

        ResultadoSesion cruce = servicio.ejecutar(
                pendiente.id(),
                HOY,
                Hora.de(16, 30).valor(),
                Hora.de(17, 30).valor(),
                null);
        assertFalse(cruce.esValido());
        assertEquals(ErrorValidacion.SESION_SOLAPADA, cruce.error());
        assertNotNull(cruce.conflicto());
        assertEquals(ocupada.id().longValue(), cruce.conflicto().sesionId());

        assertEquals(
                ErrorValidacion.FECHA_PLANIFICACION_PASADA,
                servicio.ejecutar(
                                pendiente.id(),
                                HOY.sumarDias(-1),
                                Hora.de(12, 0).valor(),
                                Hora.de(13, 0).valor(),
                                null)
                        .error());

        SesionEstudio completada = pendiente.completar(HOY, 60).valor();
        sesiones.actualizar(completada);
        assertEquals(
                ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA,
                servicio.ejecutar(
                                completada.id(),
                                HOY.sumarDias(1),
                                Hora.de(12, 0).valor(),
                                Hora.de(13, 0).valor(),
                                null)
                        .error());
    }

    @Test
    public void sobrecargaCincoArgumentosConservaAntelacionOriginalYReemplazaAlarma() {
        FakeReloj relojLocal = new FakeReloj(HOY, Hora.de(9, 0).valor(), -240);
        FakeProgramadorRecordatorios recordatorios = new FakeProgramadorRecordatorios();
        PlanificarSesionService planificador =
                new PlanificarSesionService(sesiones, materias, relojLocal, recordatorios);
        ReprogramarSesionService reprogramador =
                new ReprogramarSesionService(sesiones, materias, relojLocal, recordatorios);

        SesionEstudio original = planificador.ejecutar(
                        algebraId,
                        HOY,
                        Hora.de(14, 0).valor(),
                        Hora.de(15, 0).valor(),
                        "Repaso",
                        AntelacionRecordatorio.MINUTOS_15)
                .sesion();
        assertTrue(recordatorios.estaProgramada(original.id()));

        ResultadoSesion reprogramada = reprogramador.ejecutar(
                original.id(),
                HOY.sumarDias(1),
                Hora.de(10, 0).valor(),
                Hora.de(11, 0).valor(),
                null);

        assertTrue(reprogramada.esValido());
        SesionEstudio hija = reprogramada.sesion();
        assertEquals(AntelacionRecordatorio.MINUTOS_15, hija.antelacionRecordatorio());
        assertFalse(recordatorios.estaProgramada(original.id()));
        assertTrue(recordatorios.cancelaciones().contains(original.id()));
        assertTrue(recordatorios.estaProgramada(hija.id()));
        assertEquals(
                Long.valueOf(relojLocal.instanteDe(HOY.sumarDias(1), Hora.de(9, 45).valor())),
                recordatorios.instanteDe(hija.id()));
    }

    @Test
    public void reprogramaDesactivandoRecordatorioOEnVentanaVencidaCancelaOriginalSinReprogramar() {
        FakeReloj relojLocal = new FakeReloj(HOY, Hora.de(9, 50).valor(), -240);
        FakeProgramadorRecordatorios recordatorios = new FakeProgramadorRecordatorios();
        PlanificarSesionService planificador =
                new PlanificarSesionService(sesiones, materias, relojLocal, recordatorios);
        ReprogramarSesionService reprogramador =
                new ReprogramarSesionService(sesiones, materias, relojLocal, recordatorios);

        SesionEstudio original = planificador.ejecutar(
                        algebraId,
                        HOY,
                        Hora.de(15, 0).valor(),
                        Hora.de(16, 0).valor(),
                        null,
                        AntelacionRecordatorio.MINUTOS_30)
                .sesion();
        assertTrue(recordatorios.estaProgramada(original.id()));

        // Caso 1: desactivar explicitamente el recordatorio
        ResultadoSesion sinRecordatorio = reprogramador.ejecutar(
                original.id(),
                HOY.sumarDias(1),
                Hora.de(12, 0).valor(),
                Hora.de(13, 0).valor(),
                null,
                AntelacionRecordatorio.SIN_RECORDATORIO);
        assertTrue(sinRecordatorio.esValido());
        assertEquals(
                AntelacionRecordatorio.SIN_RECORDATORIO,
                sinRecordatorio.sesion().antelacionRecordatorio());
        assertFalse(recordatorios.estaProgramada(original.id()));
        assertEquals(0, recordatorios.cantidadProgramadas());

        // Caso 2: nueva sesion cuya antelacion cae en ventana vencida (09:50 vs 10:00 - 15 min)
        SesionEstudio segunda = planificador.ejecutar(
                        algebraId,
                        HOY,
                        Hora.de(17, 0).valor(),
                        Hora.de(18, 0).valor(),
                        null,
                        AntelacionRecordatorio.MINUTOS_15)
                .sesion();
        ResultadoSesion vencida = reprogramador.ejecutar(
                segunda.id(),
                HOY,
                Hora.de(10, 0).valor(),
                Hora.de(11, 0).valor(),
                null,
                AntelacionRecordatorio.MINUTOS_15);
        assertTrue(vencida.esValido());
        assertFalse(recordatorios.estaProgramada(segunda.id()));
        assertFalse(recordatorios.estaProgramada(vencida.sesion().id()));
    }
}
