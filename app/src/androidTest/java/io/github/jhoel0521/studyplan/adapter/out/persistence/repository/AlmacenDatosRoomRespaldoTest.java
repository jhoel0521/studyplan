package io.github.jhoel0521.studyplan.adapter.out.persistence.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import io.github.jhoel0521.studyplan.adapter.out.persistence.StudyPlanDatabase;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.entity.Perfil;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.readmodel.InstantaneaRespaldo;
import io.github.jhoel0521.studyplan.domain.service.SerializadorRespaldo;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Pruebas instrumentadas de {@link AlmacenDatosRoom#exportarTodo(long)} y {@link AlmacenDatosRoom#restaurarTodo}
 * sobre Room en memoria: ida y vuelta por el archivo JSON a otra base, reemplazo de los datos existentes y atomicidad
 * si el respaldo trae filas inconsistentes.
 *
 * <p>Verifica: HU-35 (exportar), HU-37 (restaurar de forma transaccional) y RNF-12.
 */
@RunWith(AndroidJUnit4.class)
public class AlmacenDatosRoomRespaldoTest {

    private static final Fecha JUEVES = Fecha.de(2026, 11, 12).valor();
    private static final Fecha LUNES = Fecha.de(2026, 11, 9).valor();

    private StudyPlanDatabase origen;
    private StudyPlanDatabase destino;

    @Before
    public void abrirBases() {
        Context contexto = ApplicationProvider.getApplicationContext();
        origen = Room.inMemoryDatabaseBuilder(contexto, StudyPlanDatabase.class).build();
        destino = Room.inMemoryDatabaseBuilder(contexto, StudyPlanDatabase.class).build();
    }

    @After
    public void cerrarBases() {
        origen.close();
        destino.close();
    }

    private long llenarOrigen() {
        new PerfilRepositoryRoom(origen.perfilDao()).guardar(Perfil.crear("Ana", 20).valor());
        MateriaRepositoryRoom materias = new MateriaRepositoryRoom(origen.materiaDao());
        long algebraId = materias.guardar(Materia.crear("Álgebra", 0xFF1E88E5, null).valor()).id();
        materias.guardar(Materia.crear("Física", 0xFFE53935, null).valor());
        SesionRepositoryRoom sesiones = new SesionRepositoryRoom(origen.sesionDao());
        sesiones.guardar(SesionEstudio.planificar(
                algebraId, JUEVES, LUNES, Hora.de(15, 0).valor(), Hora.de(16, 0).valor(), "Tarde", null).valor());
        sesiones.guardar(SesionEstudio.planificar(
                algebraId, JUEVES, LUNES, Hora.de(8, 30).valor(), Hora.de(10, 0).valor(), "Mañana", null).valor());
        return algebraId;
    }

    @Test
    public void exportarIncluyeLasSieteTablasConSusFilas() {
        llenarOrigen();

        InstantaneaRespaldo instantanea = new AlmacenDatosRoom(origen).exportarTodo(1234L);

        assertEquals(InstantaneaRespaldo.VERSION_FORMATO_ACTUAL, instantanea.versionFormato());
        assertEquals(8, instantanea.versionEsquema());
        assertEquals(1234L, instantanea.fechaExportacionUtcMilis());
        for (String tabla : new String[] {"perfil", "ajustes", "materia", "sesion_estudio", "evento_estudio",
                "tramo_diario", "meta_semanal"}) {
            assertTrue("Falta la tabla " + tabla, instantanea.tablas().containsKey(tabla));
        }
        assertEquals("Ana", instantanea.filas("perfil").get(0).get("nombre"));
        assertEquals(2, instantanea.filas("sesion_estudio").size());
        assertTrue(instantanea.filas("materia").size() >= 2);
    }

    @Test
    public void elRespaldoViajaPorJsonYSeRestauraEnOtraBaseIgual() {
        llenarOrigen();
        byte[] archivo = SerializadorRespaldo.serializar(new AlmacenDatosRoom(origen).exportarTodo(1L));

        new AlmacenDatosRoom(destino).restaurarTodo(SerializadorRespaldo.deserializar(archivo));

        assertEquals("Ana", new PerfilRepositoryRoom(destino.perfilDao()).obtener().orElseThrow(AssertionError::new)
                .nombre());
        List<String> nombres = new ArrayList<>();
        for (Materia m : new MateriaRepositoryRoom(destino.materiaDao()).activas()) {
            nombres.add(m.nombre());
        }
        assertTrue(nombres.contains("Álgebra"));
        assertTrue(nombres.contains("Física"));
        assertEquals(2, new SesionRepositoryRoom(destino.sesionDao()).listarPorFecha(JUEVES).size());
    }

    @Test
    public void restaurarReemplazaLosDatosQueYaHabiaEnElTelefono() {
        llenarOrigen();
        InstantaneaRespaldo instantanea = new AlmacenDatosRoom(origen).exportarTodo(1L);
        new PerfilRepositoryRoom(destino.perfilDao()).guardar(Perfil.crear("Otro", 5).valor());
        new MateriaRepositoryRoom(destino.materiaDao()).guardar(Materia.crear("Química", 0xFF43A047, null).valor());

        new AlmacenDatosRoom(destino).restaurarTodo(instantanea);

        assertEquals("Ana", new PerfilRepositoryRoom(destino.perfilDao()).obtener().orElseThrow(AssertionError::new)
                .nombre());
        for (Materia m : new MateriaRepositoryRoom(destino.materiaDao()).activas()) {
            assertFalse("Química no debía sobrevivir", "Química".equals(m.nombre()));
        }
    }

    @Test
    public void unRespaldoInconsistenteNoCambiaNadaDelTelefono() {
        new PerfilRepositoryRoom(destino.perfilDao()).guardar(Perfil.crear("Luis", 10).valor());
        Map<String, List<Map<String, Object>>> tablas = new LinkedHashMap<>();
        Map<String, Object> perfil = new LinkedHashMap<>();
        perfil.put("id", 1L);
        perfil.put("nombre", "Ana");
        perfil.put("metaSemanalHoras", 20.0);
        tablas.put("perfil", new ArrayList<>(java.util.Collections.singletonList(perfil)));
        // Una sesión que apunta a una materia que no existe rompe la clave foránea al confirmar.
        Map<String, Object> sesionHuerfana = new LinkedHashMap<>();
        sesionHuerfana.put("id", 1L);
        sesionHuerfana.put("materia_id", 999L);
        sesionHuerfana.put("plan_inicio_utc_milis", 1L);
        sesionHuerfana.put("desfase_minutos", 0L);
        sesionHuerfana.put("duracion_planificada_minutos", 30L);
        sesionHuerfana.put("estado", "PENDIENTE");
        sesionHuerfana.put("tecnica", "LIBRE");
        tablas.put("sesion_estudio", new ArrayList<>(java.util.Collections.singletonList(sesionHuerfana)));

        try {
            new AlmacenDatosRoom(destino).restaurarTodo(new InstantaneaRespaldo(1, 8, 1L, tablas));
            fail("Debía rechazar el respaldo inconsistente");
        } catch (RuntimeException esperada) {
            // La transacción se deshizo.
        }

        assertEquals("Luis", new PerfilRepositoryRoom(destino.perfilDao()).obtener().orElseThrow(AssertionError::new)
                .nombre());
    }
}
