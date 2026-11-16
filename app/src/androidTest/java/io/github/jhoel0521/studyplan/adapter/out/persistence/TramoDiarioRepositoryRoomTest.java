package io.github.jhoel0521.studyplan.adapter.out.persistence;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.TreeMap;

import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;

/**
 * Prueba instrumentada de {@link TramoDiarioRepositoryRoom} ({@code RN-31}, {@code RNF-02}).
 */
@RunWith(AndroidJUnit4.class)
public class TramoDiarioRepositoryRoomTest {

    private StudyPlanDatabase base;
    private MateriaRepositoryRoom materias;
    private SesionRepositoryRoom sesiones;
    private TramoDiarioRepositoryRoom tramos;

    @Before
    public void configurar() {
        Context contexto = ApplicationProvider.getApplicationContext();
        base = Room.inMemoryDatabaseBuilder(contexto, StudyPlanDatabase.class).build();
        materias = new MateriaRepositoryRoom(base.materiaDao());
        sesiones = new SesionRepositoryRoom(base.sesionDao());
        tramos = new TramoDiarioRepositoryRoom(base.tramoDiarioDao());
    }

    @After
    public void cerrar() {
        base.close();
    }

    @Test
    public void guardaYSumaTramosPorRangoDeFechasLocales() {
        Materia m = materias.guardar(Materia.crear("Física", 0xFF1565C0, null).valor());
        Fecha domingo = Fecha.de(2026, 11, 15).valor();
        Fecha lunes = Fecha.de(2026, 11, 16).valor();
        SesionEstudio s = sesiones.guardar(SesionEstudio.planificar(
                m.id(), domingo, domingo,
                Hora.de(23, 0).valor(), Hora.de(1, 0).valor(), -240, "", null).valor());

        Map<Fecha, Integer> reparto = new TreeMap<>();
        reparto.put(domingo, 60);
        reparto.put(lunes, 60);
        tramos.guardarTramos(s.id(), reparto);

        assertEquals(60, tramos.sumarMinutosEnRango(domingo, domingo));
        assertEquals(60, tramos.sumarMinutosEnRango(lunes, lunes));
        assertEquals(120, tramos.sumarMinutosEnRango(domingo, lunes));

        Map<Fecha, Integer> porDia = tramos.sumarPorDiaEnRango(domingo, lunes);
        assertEquals(Integer.valueOf(60), porDia.get(domingo));
        assertEquals(Integer.valueOf(60), porDia.get(lunes));
    }
}
