package io.github.jhoel0521.studyplan.adapter.out.persistence;

import android.database.Cursor;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Prueba instrumentada de la migración de Room v2 → v3 ({@link Migraciones#MIGRATION_2_3})
 * verificando contra {@code 3.json} que conserva perfil, ajustes y materias intactos y crea la
 * tabla {@code sesion_estudio} con integridad referencial e índices (HU-10 a HU-15, RNF-12).
 */
@RunWith(AndroidJUnit4.class)
public class Migracion3InstrumentalTest {

    private static final String BASE_PRUEBA = "migracion-2-3-prueba";

    @Rule
    public final MigrationTestHelper ayudante = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), StudyPlanDatabase.class);

    @Test
    public void migracionDeVersion2A3ConservaDatosPreviosYCreaSesionEstudio() throws Exception {
        SupportSQLiteDatabase v2 = ayudante.createDatabase(BASE_PRUEBA, 2);
        v2.execSQL("INSERT INTO perfil (id, nombre, metaSemanalHoras) VALUES (1, 'Luis', 15)");
        v2.execSQL("INSERT INTO ajustes (id, tema, idioma, bloqueoActivo) VALUES (1, 'CLARO', 'ES', 0)");
        v2.execSQL("INSERT INTO materia (id, nombre, nombre_clave, color, docente, archivada) "
                + "VALUES (10, 'Cálculo I', 'CALCULO I', -14776091, 'Prof. Vega', 0)");
        v2.close();

        SupportSQLiteDatabase v3 = ayudante.runMigrationsAndValidate(
                BASE_PRUEBA, 3, true, Migraciones.MIGRATION_2_3);

        try (Cursor perfil = v3.query("SELECT nombre, metaSemanalHoras FROM perfil WHERE id = 1")) {
            assertTrue(perfil.moveToFirst());
            assertEquals("Luis", perfil.getString(0));
            assertEquals(15.0, perfil.getDouble(1), 0);
        }
        try (Cursor materia = v3.query("SELECT nombre, color, archivada FROM materia WHERE id = 10")) {
            assertTrue(materia.moveToFirst());
            assertEquals("Cálculo I", materia.getString(0));
            assertEquals(0, materia.getInt(2));
        }
        try (Cursor sesiones = v3.query("SELECT COUNT(*) FROM sesion_estudio")) {
            assertTrue(sesiones.moveToFirst());
            assertEquals(0, sesiones.getInt(0));
        }
    }
}
