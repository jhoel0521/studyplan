package io.github.jhoel0521.studyplan.adapter.out.persistence;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Migraciones escritas a mano de la base de datos. No se usa migración destructiva: cada cambio de versión exige una
 * migración aquí y su prueba (RNF-12).
 *
 * <p>Implementa: HU-06 a HU-09 (la tabla de materias llega en la versión 2) y conserva los datos de HU-01, HU-03 y HU-04.
 */
public final class Migraciones {

    private Migraciones() {
    }

    /** Versión 1 (perfil y ajustes) a versión 2: solo crea la tabla de materias y su índice. */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase base) {
            base.execSQL("CREATE TABLE IF NOT EXISTS `materia` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`nombre` TEXT NOT NULL, `nombre_clave` TEXT NOT NULL, `color` INTEGER NOT NULL, "
                    + "`docente` TEXT, `archivada` INTEGER NOT NULL)");
            base.execSQL("CREATE INDEX IF NOT EXISTS `index_materia_nombre_clave` ON `materia` (`nombre_clave`)");
        }
    };
}
