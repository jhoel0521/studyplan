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
 * Prueba instrumentada de la migración Room v6 → v7 ({@link Migraciones#MIGRATION_6_7})
 * verificando contra {@code 7.json} la columna nullable {@code topico} en {@code evento_estudio}
 * ({@code HU-39}, {@code HU-45}, {@code RN-30}, {@code RNF-12}).
 */
@RunWith(AndroidJUnit4.class)
public class Migracion7InstrumentalTest {

    private static final String BASE_PRUEBA = "migracion-6-7-prueba";

    @Rule
    public final MigrationTestHelper ayudante = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), StudyPlanDatabase.class);

    @Test
    public void migracionDeVersion6A7ConservaEventosPreviosYPermiteTopicoCanonico() throws Exception {
        SupportSQLiteDatabase v6 = ayudante.createDatabase(BASE_PRUEBA, 6);
        v6.execSQL("INSERT INTO materia (id, nombre, nombre_clave, color, docente, archivada, es_general) "
                + "VALUES (5, 'Álgebra', 'algebra', -16737844, NULL, 0, 0)");
        v6.execSQL("INSERT INTO sesion_estudio (id, materia_id, plan_inicio_utc_milis, "
                + "plan_fin_utc_milis, desfase_minutos, duracion_planificada_minutos, "
                + "duracion_real_minutos, estado, tecnica) "
                + "VALUES (100, 5, 1794834000000, 1794839400000, -240, 90, 90, 'COMPLETADA', 'LIBRE')");
        v6.execSQL("INSERT INTO evento_estudio (id, sesion_id, tipo, instante_milis, desfase_minutos, nota) "
                + "VALUES (1, 100, 'INICIO', 1794834000000, -240, NULL)");
        v6.close();

        SupportSQLiteDatabase v7 = ayudante.runMigrationsAndValidate(
                BASE_PRUEBA, 7, true, Migraciones.MIGRATION_6_7);

        try (Cursor ev = v7.query("SELECT tipo, topico FROM evento_estudio WHERE id = 1")) {
            assertTrue(ev.moveToFirst());
            assertEquals("INICIO", ev.getString(0));
            assertTrue(ev.isNull(1));
        }

        v7.execSQL("INSERT INTO evento_estudio (sesion_id, tipo, instante_milis, desfase_minutos, nota, topico) "
                + "VALUES (100, 'REANUDACION', 1794835000000, -240, NULL, 'DESCANSO_COMIDA')");
        try (Cursor ev2 = v7.query("SELECT topico FROM evento_estudio WHERE tipo = 'REANUDACION'")) {
            assertTrue(ev2.moveToFirst());
            assertEquals("DESCANSO_COMIDA", ev2.getString(0));
        }
    }
}
