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
 * Prueba instrumentada de la migración Room v7 -> v8 ({@link Migraciones#MIGRATION_7_8})
 * verificando contra {@code 8.json} la columna {@code recordatorio_minutos} en {@code ajustes} y
 * {@code sesion_estudio} ({@code HU-34}, {@code RNF-12}).
 */
@RunWith(AndroidJUnit4.class)
public class Migracion8InstrumentalTest {

    private static final String BASE_PRUEBA = "migracion-7-8-prueba";

    @Rule
    public final MigrationTestHelper ayudante = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), StudyPlanDatabase.class);

    @Test
    public void migracionDeVersion7A8ConservaDatosYAsignaValoresPorDefecto() throws Exception {
        SupportSQLiteDatabase v7 = ayudante.createDatabase(BASE_PRUEBA, 7);
        v7.execSQL("INSERT INTO ajustes (id, tema, idioma, bloqueoActivo, no_molestar_global, "
                + "pomodoro_trabajo_min, pomodoro_descanso_corto_min, "
                + "pomodoro_descanso_largo_min, pomodoro_ciclos_hasta_largo) "
                + "VALUES (1, 'OSCURO', 'ES', 0, 1, 25, 5, 15, 4)");
        v7.execSQL("INSERT INTO materia (id, nombre, nombre_clave, color, docente, archivada, es_general) "
                + "VALUES (5, 'Álgebra', 'algebra', -16737844, NULL, 0, 0)");
        v7.execSQL("INSERT INTO sesion_estudio (id, materia_id, plan_inicio_utc_milis, "
                + "plan_fin_utc_milis, desfase_minutos, duracion_planificada_minutos, "
                + "duracion_real_minutos, estado, tecnica) "
                + "VALUES (100, 5, 1794834000000, 1794839400000, -240, 90, NULL, 'PENDIENTE', 'LIBRE')");
        v7.close();

        SupportSQLiteDatabase v8 = ayudante.runMigrationsAndValidate(
                BASE_PRUEBA, 8, true, Migraciones.MIGRATION_7_8);

        try (Cursor ajustes = v8.query("SELECT tema, recordatorio_minutos FROM ajustes WHERE id = 1")) {
            assertTrue(ajustes.moveToFirst());
            assertEquals("OSCURO", ajustes.getString(0));
            assertEquals(15, ajustes.getInt(1));
        }

        try (Cursor sesion = v8.query("SELECT estado, recordatorio_minutos FROM sesion_estudio WHERE id = 100")) {
            assertTrue(sesion.moveToFirst());
            assertEquals("PENDIENTE", sesion.getString(0));
            assertTrue(sesion.isNull(1));
        }

        v8.execSQL("UPDATE ajustes SET recordatorio_minutos = 5 WHERE id = 1");
        v8.execSQL("INSERT INTO sesion_estudio (id, materia_id, plan_inicio_utc_milis, "
                + "plan_fin_utc_milis, desfase_minutos, duracion_planificada_minutos, "
                + "duracion_real_minutos, estado, tecnica, recordatorio_minutos) "
                + "VALUES (101, 5, 1794920400000, 1794924000000, -240, 60, NULL, 'PENDIENTE', 'LIBRE', 30)");

        try (Cursor ajustesAct = v8.query("SELECT recordatorio_minutos FROM ajustes WHERE id = 1")) {
            assertTrue(ajustesAct.moveToFirst());
            assertEquals(5, ajustesAct.getInt(0));
        }
        try (Cursor sesionNueva = v8.query("SELECT recordatorio_minutos FROM sesion_estudio WHERE id = 101")) {
            assertTrue(sesionNueva.moveToFirst());
            assertEquals(30, sesionNueva.getInt(0));
        }
    }
}
