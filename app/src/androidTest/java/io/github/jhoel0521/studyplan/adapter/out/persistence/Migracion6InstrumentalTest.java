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
 * Prueba instrumentada de la migración Room v5 → v6 ({@link Migraciones#MIGRATION_5_6})
 * verificando contra {@code 6.json} la conservación de materias y sesiones previas, la fila única
 * {@code Estudios varios} ({@code RN-32}), {@code plan_fin_utc_milis} nullable ({@code HU-47}) y la
 * tabla {@code meta_semanal} ({@code HU-25} a {@code HU-28}, {@code RN-33}, {@code RNF-12}).
 */
@RunWith(AndroidJUnit4.class)
public class Migracion6InstrumentalTest {

    private static final String BASE_PRUEBA = "migracion-5-6-prueba";

    @Rule
    public final MigrationTestHelper ayudante = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), StudyPlanDatabase.class);

    @Test
    public void migracionDeVersion5A6ConservaDatosCreaGeneralYPermiteSesionSinPlan() throws Exception {
        SupportSQLiteDatabase v5 = ayudante.createDatabase(BASE_PRUEBA, 5);
        v5.execSQL("INSERT INTO materia (id, nombre, nombre_clave, color, docente, archivada) "
                + "VALUES (5, 'Álgebra', 'algebra', -16737844, 'Prof. X', 0)");
        v5.execSQL("INSERT INTO sesion_estudio (id, materia_id, plan_inicio_utc_milis, "
                + "plan_fin_utc_milis, desfase_minutos, duracion_planificada_minutos, "
                + "duracion_real_minutos, estado, notas, sesion_origen_id, tecnica) "
                + "VALUES (100, 5, 1794834000000, 1794839400000, -240, 90, 90, 'COMPLETADA', 'Repaso', NULL, 'LIBRE')");
        v5.close();

        SupportSQLiteDatabase v6 = ayudante.runMigrationsAndValidate(
                BASE_PRUEBA, 6, true, Migraciones.MIGRATION_5_6);

        try (Cursor mat = v6.query("SELECT nombre, es_general, meta_semanal_horas FROM materia WHERE id = 5")) {
            assertTrue(mat.moveToFirst());
            assertEquals("Álgebra", mat.getString(0));
            assertEquals(0, mat.getInt(1));
            assertTrue(mat.isNull(2));
        }

        try (Cursor gen = v6.query("SELECT COUNT(*), MAX(nombre) FROM materia WHERE es_general = 1")) {
            assertTrue(gen.moveToFirst());
            assertEquals(1, gen.getInt(0));
            assertEquals("Estudios varios", gen.getString(1));
        }

        // Idempotencia de asegurarMateriaGeneral
        StudyPlanDatabase.asegurarMateriaGeneral(v6);
        try (Cursor gen2 = v6.query("SELECT COUNT(*) FROM materia WHERE es_general = 1")) {
            assertTrue(gen2.moveToFirst());
            assertEquals(1, gen2.getInt(0));
        }

        // Sesión previa conservada y nueva sesión sin plan (plan_fin_utc_milis NULL)
        try (Cursor ses = v6.query("SELECT duracion_real_minutos FROM sesion_estudio WHERE id = 100")) {
            assertTrue(ses.moveToFirst());
            assertEquals(90, ses.getInt(0));
        }
        v6.execSQL("INSERT INTO sesion_estudio (materia_id, plan_inicio_utc_milis, "
                + "plan_fin_utc_milis, desfase_minutos, duracion_planificada_minutos, "
                + "duracion_real_minutos, estado, tecnica) "
                + "VALUES (5, 1794920400000, NULL, -240, 0, NULL, 'EN_CURSO', 'LIBRE')");

        v6.execSQL("INSERT INTO meta_semanal (semana_inicio, materia_id, horas_objetivo, felicitada) "
                + "VALUES ('2026-11-16', 5, 4.0, 0)");
        try (Cursor meta = v6.query("SELECT horas_objetivo, felicitada FROM meta_semanal WHERE materia_id = 5")) {
            assertTrue(meta.moveToFirst());
            assertEquals(4.0, meta.getDouble(0), 1e-6);
            assertEquals(0, meta.getInt(1));
        }
    }
}
