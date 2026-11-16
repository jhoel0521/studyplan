package io.github.jhoel0521.studyplan.adapter.out.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Prueba instrumentada de la migración de Room v3 → v4 ({@link Migraciones#MIGRATION_3_4})
 * verificando contra {@code 4.json} la conservación de datos previos y la creación de
 * {@code evento_estudio} con clave foránea en cascada (HU-22 a HU-24, HU-39, RNF-12).
 */
@RunWith(AndroidJUnit4.class)
public class Migracion4InstrumentalTest {

    private static final String BASE_PRUEBA = "migracion-3-4-prueba";

    @Rule
    public final MigrationTestHelper ayudante = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), StudyPlanDatabase.class);

    @Test
    public void migracionDeVersion3A4ConservaDatosYCreaEventoEstudio() throws Exception {
        SupportSQLiteDatabase v3 = ayudante.createDatabase(BASE_PRUEBA, 3);
        v3.execSQL("INSERT INTO perfil (id, nombre, metaSemanalHoras) VALUES (1, 'Ana', 20)");
        v3.execSQL("INSERT INTO ajustes (id, tema, idioma, bloqueoActivo) VALUES (1, 'OSCURO', 'ES', 1)");
        v3.execSQL("INSERT INTO materia (id, nombre, nombre_clave, color, docente, archivada) "
                + "VALUES (5, 'Álgebra', 'ALGEBRA', -16737844, 'Prof. X', 0)");
        v3.execSQL("INSERT INTO sesion_estudio (id, materia_id, fecha, hora_inicio, hora_fin, "
                + "duracion_planificada_minutos, duracion_real_minutos, estado, notas, sesion_origen_id) "
                + "VALUES (100, 5, '2026-11-16', '09:00', '10:00', 60, NULL, 'PENDIENTE', 'Repaso', NULL)");
        v3.close();

        SupportSQLiteDatabase v4 = ayudante.runMigrationsAndValidate(
                BASE_PRUEBA, 4, true, Migraciones.MIGRATION_3_4);

        try (Cursor aj = v4.query("SELECT no_molestar_global, pomodoro_trabajo_min, "
                + "pomodoro_descanso_corto_min, pomodoro_descanso_largo_min, pomodoro_ciclos_hasta_largo "
                + "FROM ajustes WHERE id = 1")) {
            assertTrue(aj.moveToFirst());
            assertEquals(0, aj.getInt(0));
            assertEquals(25, aj.getInt(1));
            assertEquals(5, aj.getInt(2));
            assertEquals(15, aj.getInt(3));
            assertEquals(4, aj.getInt(4));
        }
        try (Cursor ses = v4.query("SELECT tecnica, forzar_no_molestar FROM sesion_estudio WHERE id = 100")) {
            assertTrue(ses.moveToFirst());
            assertEquals("LIBRE", ses.getString(0));
            assertTrue(ses.isNull(1));
        }

        v4.setForeignKeyConstraintsEnabled(true);
        v4.execSQL("INSERT INTO evento_estudio (sesion_id, tipo, instante_milis, fecha, hora, nota) "
                + "VALUES (100, 'INICIO', 1763283600000, '2026-11-16', '09:00', NULL)");
        v4.execSQL("DELETE FROM sesion_estudio WHERE id = 100");
        try (Cursor ev = v4.query("SELECT COUNT(*) FROM evento_estudio")) {
            assertTrue(ev.moveToFirst());
            assertEquals(0, ev.getInt(0));
        }
    }
}
