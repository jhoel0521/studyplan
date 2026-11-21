package io.github.jhoel0521.studyplan.application.service.recordatorios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.domain.readmodel.AvisoRecordatorioPendiente;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeProgramadorRecordatorios;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;

/**
 * Pruebas unitarias de {@link ReprogramarRecordatoriosPendientesService} ({@code HU-34}).
 */
public class ReprogramarRecordatoriosPendientesServiceTest {

    private static final Fecha HOY = Fecha.de(2026, 11, 19).valor();
    private FakeSesionRepository sesiones;
    private FakeMateriaRepository materias;
    private FakeProgramadorRecordatorios programador;
    private long algebraId;

    @Before
    public void preparar() {
        sesiones = new FakeSesionRepository();
        materias = new FakeMateriaRepository();
        programador = new FakeProgramadorRecordatorios();
        algebraId = materias.guardar(Materia.crear("Álgebra", 0xFF1E88E5, null).valor()).id();
    }

    @Test
    public void programaFuturosDevuelveAtrasadosPreInicioEOmiteYaIniciadosYSinAviso() {
        // Reloj actual: 09:50 en UTC-04:00 (-240)
        FakeReloj reloj = new FakeReloj(HOY, Hora.de(9, 50).valor(), -240);
        ReprogramarRecordatoriosPendientesService servicio =
                new ReprogramarRecordatoriosPendientesService(sesiones, materias, programador, reloj);

        // 1) Sesión 09:00-09:40: ya pasó su inicio -> se omite
        SesionEstudio yaIniciada = sesiones.guardar(SesionEstudio.planificar(
                algebraId, HOY, HOY, Hora.de(9, 0).valor(), Hora.de(9, 40).valor(),
                -240, null, null, AntelacionRecordatorio.MINUTOS_15).valor());
        // 2) Sesión 10:00-11:00 con aviso 15m (disparo 09:45 <= 09:50 < 10:00) -> aviso inmediato
        SesionEstudio atrasada = sesiones.guardar(SesionEstudio.planificar(
                algebraId, HOY, HOY, Hora.de(10, 0).valor(), Hora.de(11, 0).valor(),
                -240, null, null, AntelacionRecordatorio.MINUTOS_15).valor());
        // 3) Sesión 12:00-13:00 con aviso 30m (disparo 11:30 > 09:50) -> se reprograma alarma
        SesionEstudio futura = sesiones.guardar(SesionEstudio.planificar(
                algebraId, HOY, HOY, Hora.de(12, 0).valor(), Hora.de(13, 0).valor(),
                -240, null, null, AntelacionRecordatorio.MINUTOS_30).valor());
        // 4) Sesión 14:00-15:00 sin recordatorio -> se omite
        SesionEstudio sinAviso = sesiones.guardar(SesionEstudio.planificar(
                algebraId, HOY, HOY, Hora.de(14, 0).valor(), Hora.de(15, 0).valor(),
                -240, null, null, AntelacionRecordatorio.SIN_RECORDATORIO).valor());
        // 5) Sesión atrasada en distinta zona (-300): 09:05-10:05 en UTC-05:00 (= 10:05 en -240, disparo 09:50) -> incluye sufijo UTC-05:00
        SesionEstudio distintaZona = sesiones.guardar(SesionEstudio.planificar(
                algebraId, HOY, HOY, Hora.de(9, 5).valor(), Hora.de(10, 5).valor(),
                -300, null, null, AntelacionRecordatorio.MINUTOS_15).valor());

        List<AvisoRecordatorioPendiente> inmediatos = servicio.ejecutar();

        assertFalse(programador.estaProgramada(yaIniciada.id()));
        assertFalse(programador.estaProgramada(atrasada.id()));
        assertFalse(programador.estaProgramada(sinAviso.id()));
        assertTrue(programador.estaProgramada(futura.id()));
        assertEquals(futura.instanteRecordatorioUtcMilis(), programador.instanteDe(futura.id()));

        assertEquals(2, inmediatos.size());
        assertEquals(
                new AvisoRecordatorioPendiente(
                        atrasada.id(), "Álgebra", "10:00 \u2013 11:00"),
                inmediatos.get(0));
        assertEquals(
                new AvisoRecordatorioPendiente(
                        distintaZona.id(),
                        "Álgebra",
                        "09:05 \u2013 10:05 (" + distintaZona.inicioInstante().desfaseTexto() + ")"),
                inmediatos.get(1));
    }
}
