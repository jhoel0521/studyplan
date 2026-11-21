package io.github.jhoel0521.studyplan.application.service.configuracion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
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
import io.github.jhoel0521.studyplan.application.service.sesiones.VencerSesionesPendientesService;

/**
 * Pruebas unitarias completas del resumen en Inicio: saludo, vencimiento automático, suma real
 * lunes–domingo, próxima sesión pendiente y sesiones del día.
 *
 * <p>Verifica: HU-02, RN-08, RN-11, RN-13 y RN-15.
 */
public class ObtenerResumenInicioServiceTest {

    // Jueves 2026-11-12 (lunes 2026-11-09, domingo 2026-11-15)
    private static final Fecha HOY = Fecha.de(2026, 11, 12).valor();

    private FakePerfilRepository perfiles;
    private FakeSesionRepository sesiones;
    private FakeMateriaRepository materias;
    private FakeReloj reloj;
    private ObtenerResumenInicioService servicio;
    private long algebraId;

    @Before
    public void preparar() {
        perfiles = new FakePerfilRepository();
        sesiones = new FakeSesionRepository();
        materias = new FakeMateriaRepository();
        reloj = new FakeReloj(HOY, Hora.de(11, 0).valor());
        servicio = new ObtenerResumenInicioService(
                perfiles,
                sesiones,
                materias,
                reloj,
                new VencerSesionesPendientesService(sesiones, reloj));
        algebraId = materias.guardar(Materia.crear("Álgebra", 0xFF1E88E5, null).valor()).id();
    }

    @Test
    public void sinPerfilNoHayResumen() {
        assertFalse(servicio.ejecutar().isPresent());
    }

    @Test
    public void conPerfilSinSesionesMuestraProgresoCeroYListaVacia() {
        perfiles.guardar(Perfil.crear("Ana", 20).valor());
        ResumenInicio resumen = servicio.ejecutar().orElseThrow(AssertionError::new);
        assertEquals("Ana", resumen.nombre());
        assertEquals(0, resumen.horasRealizadas(), 0.001);
        assertEquals(20, resumen.metaSemanalHoras(), 0.001);
        assertEquals(0, resumen.porcentajeProgreso());
        assertNull(resumen.proximaSesion());
        assertEquals(0, resumen.sesionesDeHoy().size());
    }

    @Test
    public void sumaSemanaLunesADomingoVencePendientesYEligeProximaSesion() {
        perfiles.guardar(Perfil.crear("Ana", 10).valor());

        // Semana anterior (domingo 2026-11-08): NO debe sumar en semana actual
        sesiones.guardar(SesionEstudio.restaurar(
                null,
                algebraId,
                Fecha.de(2026, 11, 8).valor(),
                Hora.de(10, 0).valor(),
                Hora.de(12, 0).valor(),
                120,
                120,
                CodigoEstadoSesion.COMPLETADA,
                null,
                null));

        // Lunes de esta semana (2026-11-09): COMPLETADA_TARDIA de 90 min (1.5 h) -> SÍ suma
        sesiones.guardar(SesionEstudio.restaurar(
                null,
                algebraId,
                Fecha.de(2026, 11, 9).valor(),
                Hora.de(8, 0).valor(),
                Hora.de(9, 30).valor(),
                90,
                90,
                CodigoEstadoSesion.COMPLETADA_TARDIA,
                null,
                null));

        // Ayer (2026-11-11) PENDIENTE: debe vencer automáticamente a NO_REALIZADA
        SesionEstudio ayerPendiente = sesiones.guardar(SesionEstudio.restaurar(
                null,
                algebraId,
                HOY.sumarDias(-1),
                Hora.de(14, 0).valor(),
                Hora.de(15, 0).valor(),
                60,
                null,
                CodigoEstadoSesion.PENDIENTE,
                null,
                null));

        // Hoy (11:00 ahora): una pendiente pasada de hora (09:00-10:00), una completada (10:00-10:30, 30 min = 0.5h), y una pendiente futura (15:00-16:00)
        sesiones.guardar(SesionEstudio.restaurar(
                null,
                algebraId,
                HOY,
                Hora.de(9, 0).valor(),
                Hora.de(10, 0).valor(),
                60,
                null,
                CodigoEstadoSesion.PENDIENTE,
                null,
                null));
        sesiones.guardar(SesionEstudio.restaurar(
                null,
                algebraId,
                HOY,
                Hora.de(10, 0).valor(),
                Hora.de(10, 30).valor(),
                30,
                30,
                CodigoEstadoSesion.COMPLETADA,
                null,
                null));
        SesionEstudio siguienteHoy = sesiones.guardar(SesionEstudio.restaurar(
                null,
                algebraId,
                HOY,
                Hora.de(15, 0).valor(),
                Hora.de(16, 0).valor(),
                60,
                null,
                CodigoEstadoSesion.PENDIENTE,
                null,
                null));

        ResumenInicio resumen = servicio.ejecutar().orElseThrow(AssertionError::new);
        assertEquals(2.0, resumen.horasRealizadas(), 0.001); // 90 + 30 min = 2.0 h
        assertEquals(20, resumen.porcentajeProgreso());
        assertEquals(3, resumen.sesionesDeHoy().size());
        assertNotNull(resumen.proximaSesion());
        assertEquals(siguienteHoy.id().longValue(), resumen.proximaSesion().sesionId());
        assertEquals("Álgebra", resumen.proximaSesion().nombreMateria());
        assertEquals(
                CodigoEstadoSesion.NO_REALIZADA,
                sesiones.buscarPorId(ayerPendiente.id()).orElseThrow(AssertionError::new)
                        .codigoEstado());
    }

    @Test
    public void elPorcentajeDeProgresoNuncaSuperaElCien() {
        assertEquals(60, new ResumenInicio("Ana", 12, 20).porcentajeProgreso());
        assertEquals(100, new ResumenInicio("Ana", 30, 20).porcentajeProgreso());
        assertEquals(0, new ResumenInicio("Ana", 0, 20).porcentajeProgreso());
    }

    @Test
    public void sumaHorasSemanalesDesdeTramoDiarioConCorteEstrictoLunesRn31() {
        FakePerfilRepository perfiles = new FakePerfilRepository();
        FakeMateriaRepository materias = new FakeMateriaRepository();
        FakeSesionRepository sesiones = new FakeSesionRepository();
        io.github.jhoel0521.studyplan.fakes.FakeTramoDiarioRepository tramos =
                new io.github.jhoel0521.studyplan.fakes.FakeTramoDiarioRepository();
        Fecha domingo = Fecha.de(2026, 11, 15).valor();
        Fecha lunes = Fecha.de(2026, 11, 16).valor();
        FakeReloj reloj = new FakeReloj(lunes, Hora.de(9, 0).valor());
        VencerSesionesPendientesService vencer = new VencerSesionesPendientesService(sesiones, reloj);
        ObtenerResumenInicioService servicio = new ObtenerResumenInicioService(
                perfiles, sesiones, materias, tramos, reloj, vencer);

        perfiles.guardar(Perfil.crear("Ana", 10).valor());
        java.util.Map<Fecha, Integer> reparto = new java.util.TreeMap<>();
        reparto.put(domingo, 60); // semana pasada
        reparto.put(lunes, 60);   // esta semana
        tramos.guardarTramos(99L, reparto);

        ResumenInicio resumen = servicio.ejecutar().orElseThrow(AssertionError::new);
        assertEquals(1.0, resumen.horasRealizadas(), 0.001);
    }

    /** HU-02 «Sesión en curso visible» y HU-47 «Ya hay una sesión En curso»: el resumen la expone. */
    @Test
    public void exponeLaSesionEnCursoParaVolverAElla() {
        perfiles.guardar(Perfil.crear("Ana", 10).valor());
        SesionEstudio enCurso = sesiones.guardar(SesionEstudio.restaurar(
                null,
                algebraId,
                HOY,
                Hora.de(10, 30).valor(),
                Hora.de(11, 30).valor(),
                60,
                0,
                CodigoEstadoSesion.EN_CURSO,
                null,
                null));

        ResumenInicio resumen = servicio.ejecutar().orElseThrow(AssertionError::new);

        assertEquals((long) enCurso.id(), (long) resumen.sesionEnCurso().sesionId());
        assertEquals("Álgebra", resumen.sesionEnCurso().nombreMateria());
    }

    @Test
    public void sinSesionEnCursoNoHayAviso() {
        perfiles.guardar(Perfil.crear("Ana", 10).valor());
        sesiones.guardar(SesionEstudio.restaurar(
                null,
                algebraId,
                HOY,
                Hora.de(14, 0).valor(),
                Hora.de(15, 0).valor(),
                60,
                0,
                CodigoEstadoSesion.PENDIENTE,
                null,
                null));

        assertNull(servicio.ejecutar().orElseThrow(AssertionError::new).sesionEnCurso());
    }
}
