package io.github.jhoel0521.studyplan.adapter.out.persistence;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import io.github.jhoel0521.studyplan.domain.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Pruebas instrumentadas de {@link SesionRepositoryRoom} y {@link SesionesDeMateriaRoom} sobre Room
 * en memoria: CRUD, ordenación, sumas reales, reprogramación transaccional, clave foránea
 * {@code ON DELETE RESTRICT} y limpieza completa {@code borrarTodo()}.
 *
 * <p>Verifica: HU-02, HU-08, HU-09, HU-10 a HU-15, RN-03, RN-09, RN-11 y RNF-12.
 */
@RunWith(AndroidJUnit4.class)
public class SesionRepositoryRoomTest {

    private static final Fecha LUNES = Fecha.de(2026, 11, 9).valor();
    private static final Fecha JUEVES = Fecha.de(2026, 11, 12).valor();
    private static final Fecha DOMINGO = Fecha.de(2026, 11, 15).valor();

    private StudyPlanDatabase base;
    private MateriaRepositoryRoom materias;
    private SesionRepositoryRoom sesiones;
    private SesionesDeMateriaRoom sesionesDeMateria;
    private AlmacenDatosRoom almacen;
    private long algebraId;

    @Before
    public void preparar() {
        Context contexto = ApplicationProvider.getApplicationContext();
        base = Room.inMemoryDatabaseBuilder(contexto, StudyPlanDatabase.class).build();
        materias = new MateriaRepositoryRoom(base.materiaDao());
        sesiones = new SesionRepositoryRoom(base.sesionDao());
        sesionesDeMateria = new SesionesDeMateriaRoom(base.sesionDao());
        almacen = new AlmacenDatosRoom(base);
        algebraId = materias.guardar(Materia.crear("Álgebra", 0xFF1E88E5, null).valor()).id();
    }

    @After
    public void cerrar() {
        base.close();
    }

    @Test
    public void crudOrdenPorHoraYConsultasPorEstadoYFecha() {
        SesionEstudio tarde = sesiones.guardar(SesionEstudio.planificar(
                algebraId, JUEVES, LUNES, Hora.de(15, 0).valor(), Hora.de(16, 0).valor(), "Tarde", null)
                .valor());
        SesionEstudio maniana = sesiones.guardar(SesionEstudio.planificar(
                algebraId, JUEVES, LUNES, Hora.de(8, 30).valor(), Hora.de(10, 0).valor(), "Mañana", null)
                .valor());
        sesiones.guardar(SesionEstudio.restaurar(
                null,
                algebraId,
                LUNES,
                Hora.de(9, 0).valor(),
                Hora.de(10, 0).valor(),
                60,
                null,
                CodigoEstadoSesion.PENDIENTE,
                null,
                null));

        List<SesionEstudio> delJueves = sesiones.listarPorFecha(JUEVES);
        assertEquals(2, delJueves.size());
        assertEquals(maniana.id(), delJueves.get(0).id());
        assertEquals(tarde.id(), delJueves.get(1).id());

        List<SesionEstudio> vencidas = sesiones.listarPendientesAnterioresA(JUEVES);
        assertEquals(1, vencidas.size());
        assertEquals(LUNES, vencidas.get(0).fecha());

        List<SesionEstudio> desdeJueves = sesiones.listarPendientesDesde(JUEVES);
        assertEquals(2, desdeJueves.size());

        sesiones.eliminarPorId(tarde.id());
        assertFalse(sesiones.buscarPorId(tarde.id()).isPresent());
    }

    @Test
    public void sumaSoloMinutosRealesDeCompletadasYCompletadasTardiasEnMateriaYRango() {
        sesiones.guardar(SesionEstudio.restaurar(
                null, algebraId, LUNES, Hora.de(8, 0).valor(), Hora.de(9, 0).valor(),
                60, 75, CodigoEstadoSesion.COMPLETADA, null, null));
        sesiones.guardar(SesionEstudio.restaurar(
                null, algebraId, DOMINGO, Hora.de(18, 0).valor(), Hora.de(19, 0).valor(),
                60, 45, CodigoEstadoSesion.COMPLETADA_TARDIA, null, null));
        // Fuera de la semana actual (domingo anterior) y estados que no suman
        sesiones.guardar(SesionEstudio.restaurar(
                null, algebraId, LUNES.sumarDias(-1), Hora.de(10, 0).valor(), Hora.de(11, 0).valor(),
                60, 60, CodigoEstadoSesion.COMPLETADA, null, null));
        sesiones.guardar(SesionEstudio.restaurar(
                null, algebraId, JUEVES, Hora.de(10, 0).valor(), Hora.de(11, 0).valor(),
                60, null, CodigoEstadoSesion.PENDIENTE, null, null));
        sesiones.guardar(SesionEstudio.restaurar(
                null, algebraId, JUEVES, Hora.de(12, 0).valor(), Hora.de(13, 0).valor(),
                60, 60, CodigoEstadoSesion.REPROGRAMADA, null, null));

        assertEquals(180, sesiones.sumarMinutosRealesPorMateria(algebraId));
        assertEquals(120, sesiones.sumarMinutosRealesEnRango(LUNES, DOMINGO));
    }

    @Test
    public void reprogramacionTransaccionalYBúsquedaDeHijaYRestriccionForeignKey() {
        SesionEstudio original = sesiones.guardar(SesionEstudio.planificar(
                algebraId, LUNES, LUNES, Hora.de(10, 0).valor(), Hora.de(11, 0).valor(), null, null)
                .valor());
        SesionEstudio marcada = original.marcarReprogramada().valor();
        SesionEstudio hijaPlan = SesionEstudio.planificar(
                algebraId, DOMINGO, JUEVES, Hora.de(16, 0).valor(), Hora.de(17, 0).valor(), "Reagendada", original.id())
                .valor();

        SesionEstudio hijaGuardada = sesiones.reprogramar(marcada, hijaPlan);
        assertNotNull(hijaGuardada.id());
        assertEquals(
                CodigoEstadoSesion.REPROGRAMADA,
                sesiones.buscarPorId(original.id()).orElseThrow(AssertionError::new).codigoEstado());
        assertEquals(
                hijaGuardada.id(),
                sesiones.buscarReprogramadaHijaDe(original.id())
                        .orElseThrow(AssertionError::new)
                        .id());
        assertEquals(2, sesionesDeMateria.contar(algebraId));

        // RN-03 segunda barrera: ON DELETE RESTRICT bloquea borrar la materia mientras tenga sesiones
        try {
            materias.eliminar(algebraId);
            fail("Debió fallar la eliminación de materia con sesiones por ON DELETE RESTRICT");
        } catch (SQLiteConstraintException esperado) {
            assertTrue(materias.obtener(algebraId).isPresent());
        }

        // AlmacenDatos.borrarTodo() vacía ambas tablas sin violar FKs (conserva Estudios varios)
        almacen.borrarTodo();
        assertEquals(0, sesionesDeMateria.contar(algebraId));
        assertEquals(1, materias.activas().size());
        assertTrue(materias.activas().get(0).esGeneral());
    }

    @Test
    public void listarPorRangoOrdenaInclusivamenteYConservaVersion3SinPermisos() throws Exception {
        SesionEstudio sDom = sesiones.guardar(SesionEstudio.planificar(
                algebraId, DOMINGO, LUNES, Hora.de(10, 0).valor(), Hora.de(11, 0).valor(), "Dom", null)
                .valor());
        SesionEstudio sJueTarde = sesiones.guardar(SesionEstudio.planificar(
                algebraId, JUEVES, LUNES, Hora.de(18, 0).valor(), Hora.de(19, 0).valor(), "Jue2", null)
                .valor());
        SesionEstudio sJueManiana = sesiones.guardar(SesionEstudio.planificar(
                algebraId, JUEVES, LUNES, Hora.de(9, 0).valor(), Hora.de(10, 0).valor(), "Jue1", null)
                .valor());
        SesionEstudio sLun = sesiones.guardar(SesionEstudio.planificar(
                algebraId, LUNES, LUNES, Hora.de(14, 0).valor(), Hora.de(15, 0).valor(), "Lun", null)
                .valor());

        List<SesionEstudio> rango = sesiones.listarPorRango(LUNES, DOMINGO);
        assertEquals(4, rango.size());
        assertEquals(sLun.id(), rango.get(0).id());
        assertEquals(sJueManiana.id(), rango.get(1).id());
        assertEquals(sJueTarde.id(), rango.get(2).id());
        assertEquals(sDom.id(), rango.get(3).id());

        List<SesionEstudio> vacio = sesiones.listarPorRango(
                Fecha.de(2026, 12, 1).valor(), Fecha.de(2026, 12, 7).valor());
        assertTrue(vacio.isEmpty());

        assertEquals(7, base.getOpenHelper().getReadableDatabase().getVersion());
        Context contexto = ApplicationProvider.getApplicationContext();
        String[] permisos = contexto.getPackageManager()
                .getPackageInfo(contexto.getPackageName(), android.content.pm.PackageManager.GET_PERMISSIONS)
                .requestedPermissions;
        if (permisos != null) {
            for (String p : permisos) {
                assertFalse(p.endsWith(".INTERNET")
                        || p.endsWith(".SCHEDULE_EXACT_ALARM"));
            }
        }
    }
}
