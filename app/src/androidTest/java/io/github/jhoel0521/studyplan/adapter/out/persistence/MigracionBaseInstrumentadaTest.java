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
 * Prueba de la migración de la base de la versión 1 (perfil y ajustes) a la 2 (más materias): abre una base v1 desde
 * su esquema JSON, la migra y valida el resultado contra el esquema 2.json que se versiona.
 *
 * <p>Verifica: HU-01, HU-03 y HU-04 (los datos de la versión 1 se conservan) y HU-06 a HU-09 (la tabla de materias
 * existe y está vacía). RNF-12: sin migración destructiva.
 */
@RunWith(AndroidJUnit4.class)
public class MigracionBaseInstrumentadaTest {

    private static final String BASE_DE_PRUEBA = "migracion-prueba";

    @Rule
    public final MigrationTestHelper ayudante = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), StudyPlanDatabase.class);

    @Test
    public void laMigracionDeLaVersion1ALa2ConservaPerfilYAjustesYCreaLaTablaDeMaterias() throws Exception {
        SupportSQLiteDatabase v1 = ayudante.createDatabase(BASE_DE_PRUEBA, 1);
        v1.execSQL("INSERT INTO perfil (id, nombre, metaSemanalHoras) VALUES (1, 'Ana', 20)");
        v1.execSQL("INSERT INTO ajustes (id, tema, idioma, bloqueoActivo) VALUES (1, 'OSCURO', 'EN', 1)");
        v1.close();

        SupportSQLiteDatabase v2 = ayudante.runMigrationsAndValidate(
                BASE_DE_PRUEBA, 2, true, Migraciones.MIGRATION_1_2);

        try (Cursor perfil = v2.query("SELECT nombre, metaSemanalHoras FROM perfil WHERE id = 1")) {
            assertTrue(perfil.moveToFirst());
            assertEquals("Ana", perfil.getString(0));
            assertEquals(20.0, perfil.getDouble(1), 0);
        }
        try (Cursor ajustes = v2.query("SELECT tema, idioma, bloqueoActivo FROM ajustes WHERE id = 1")) {
            assertTrue(ajustes.moveToFirst());
            assertEquals("OSCURO", ajustes.getString(0));
            assertEquals("EN", ajustes.getString(1));
            assertEquals(1, ajustes.getInt(2));
        }
        try (Cursor materias = v2.query("SELECT COUNT(*) FROM materia")) {
            assertTrue(materias.moveToFirst());
            assertEquals(0, materias.getInt(0));
        }
    }
}
