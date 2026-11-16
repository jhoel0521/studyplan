package io.github.jhoel0521.studyplan.adapter.out.persistence;

import static org.junit.Assert.assertEquals;
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
 * Prueba instrumentada de la migración Room v4 → v5 ({@link Migraciones#MIGRATION_4_5})
 * verificando contra {@code 5.json} la conversión a instantes UTC, conservación del desfase por
 * evento, creación de {@code tramo_diario} e integridad referencial (HU-10, HU-39, RN-31, RNF-12).
 */
@RunWith(AndroidJUnit4.class)
public class Migracion5InstrumentalTest {

    private static final String BASE_PRUEBA = "migracion-4-5-prueba";

    @Rule
    public final MigrationTestHelper ayudante = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), StudyPlanDatabase.class);

    @Test
    public void migracionDeVersion4A5MigraSesionesEventosYCreaTramoDiario() throws Exception {
        SupportSQLiteDatabase v4 = ayudante.createDatabase(BASE_PRUEBA, 4);
        v4.execSQL("INSERT INTO materia (id, nombre, nombre_clave, color, docente, archivada) "
                + "VALUES (5, 'Álgebra', 'ALGEBRA', -16737844, 'Prof. X', 0)");
        v4.execSQL("INSERT INTO sesion_estudio (id, materia_id, fecha, hora_inicio, hora_fin, "
                + "duracion_planificada_minutos, duracion_real_minutos, estado, notas, "
                + "sesion_origen_id, tecnica) "
                + "VALUES (100, 5, '2026-11-16', '09:00', '10:30', 90, 90, 'COMPLETADA', 'Repaso', NULL, 'LIBRE')");
        // 2026-11-16 09:00 en UTC-04:00 = 2026-11-16 13:00 UTC = 1794834000000L
        v4.execSQL("INSERT INTO evento_estudio (id, sesion_id, tipo, instante_milis, fecha, hora, nota) "
                + "VALUES (1, 100, 'INICIO', 1794834000000, '2026-11-16', '09:00', NULL)");
        v4.close();

        SupportSQLiteDatabase v5 = ayudante.runMigrationsAndValidate(
                BASE_PRUEBA, 5, true, Migraciones.MIGRATION_4_5);

        try (Cursor ses = v5.query(
                "SELECT plan_inicio_utc_milis, plan_fin_utc_milis, duracion_real_minutos FROM sesion_estudio WHERE id = 100")) {
            assertTrue(ses.moveToFirst());
            assertTrue(ses.getLong(1) > ses.getLong(0));
            assertEquals(90 * 60_000L, ses.getLong(1) - ses.getLong(0));
            assertEquals(90, ses.getInt(2));
        }
        try (Cursor ev = v5.query(
                "SELECT desfase_minutos FROM evento_estudio WHERE id = 1")) {
            assertTrue(ev.moveToFirst());
            assertEquals(-240, ev.getInt(0));
        }
        try (Cursor tramo = v5.query(
                "SELECT fecha_local, minutos FROM tramo_diario WHERE sesion_id = 100")) {
            assertTrue(tramo.moveToFirst());
            assertEquals("2026-11-16", tramo.getString(0));
            assertEquals(90, tramo.getInt(1));
        }

        v5.setForeignKeyConstraintsEnabled(true);
        v5.execSQL("DELETE FROM sesion_estudio WHERE id = 100");
        try (Cursor tramosRestantes = v5.query("SELECT COUNT(*) FROM tramo_diario")) {
            assertTrue(tramosRestantes.moveToFirst());
            assertEquals(0, tramosRestantes.getInt(0));
        }
    }
}
