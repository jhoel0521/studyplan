package io.github.jhoel0521.studyplan.adapter.out.persistence.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio;
import io.github.jhoel0521.studyplan.adapter.out.persistence.StudyPlanDatabase;

/**
 * Pruebas instrumentadas de {@link EventoEstudioRepositoryRoom} y {@link SesionRepositoryRoom} v4
 * (HU-22, HU-39, HU-44, RN-22, RN-28).
 */
@RunWith(AndroidJUnit4.class)
public class EventoEstudioRepositoryRoomTest {

    private StudyPlanDatabase base;
    private MateriaRepositoryRoom materias;
    private SesionRepositoryRoom sesiones;
    private EventoEstudioRepositoryRoom eventos;
    private long materiaId;

    @Before
    public void preparar() {
        Context ctx = ApplicationProvider.getApplicationContext();
        base = Room.inMemoryDatabaseBuilder(ctx, StudyPlanDatabase.class).build();
        materias = new MateriaRepositoryRoom(base.materiaDao());
        sesiones = new SesionRepositoryRoom(base.sesionDao());
        eventos = new EventoEstudioRepositoryRoom(base.eventoEstudioDao());
        materiaId = materias.guardar(Materia.crear("Química", 0xFF00897B, null).valor()).id();
    }

    @After
    public void cerrar() {
        base.close();
    }

    @Test
    public void persisteEventosOrdenadosYCompletaRepartoEnTransaccion() {
        Fecha hoy = Fecha.de(2026, 11, 16).valor();
        SesionEstudio s1 = sesiones.guardar(SesionEstudio.planificar(
                materiaId, hoy, hoy, Hora.de(9, 0).valor(), Hora.de(10, 0).valor(), "", null)
                .valor()
                .conPreferenciaNoMolestar(PreferenciaNoMolestar.FORZAR_ACTIVAR)
                .iniciar(TecnicaEstudio.LIBRE, null)
                .valor());
        SesionEstudio s2 = sesiones.guardar(SesionEstudio.planificar(
                materiaId, hoy, hoy, Hora.de(10, 0).valor(), Hora.de(11, 0).valor(), "", null)
                .valor());

        assertTrue(sesiones.buscarEnCurso().isPresent());
        assertEquals(1, sesiones.listarEnCurso().size());

        eventos.guardar(EventoEstudio.crear(
                s1.id(), TipoEventoEstudio.PAUSA, 2000L, hoy, Hora.de(9, 20).valor(), "").valor());
        eventos.guardar(EventoEstudio.crear(
                s1.id(), TipoEventoEstudio.INICIO, 1000L, hoy, Hora.de(9, 0).valor(), "").valor());

        List<EventoEstudio> lista = eventos.listarPorSesion(s1.id());
        assertEquals(2, lista.size());
        assertEquals(TipoEventoEstudio.INICIO, lista.get(0).tipo());
        assertEquals(TipoEventoEstudio.PAUSA, lista.get(1).tipo());

        sesiones.completarReparto(
                s1.detener(35).valor(),
                s2.iniciar(TecnicaEstudio.LIBRE, null).valor().detener(25).valor());
        assertTrue(sesiones.listarEnCurso().isEmpty());

        long desde = s1.planInicioUtcMilis();
        long hasta = s1.planFinUtcMilis();
        List<EventoEstudio> porRango = eventos.listarPorRangoSesionesUtc(desde, hasta);
        assertEquals(2, porRango.size());
    }
}
