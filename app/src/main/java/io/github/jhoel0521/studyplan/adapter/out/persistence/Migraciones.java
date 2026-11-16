package io.github.jhoel0521.studyplan.adapter.out.persistence;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Migraciones escritas a mano de la base de datos. No se usa migración destructiva: cada cambio de versión exige una
 * migración aquí y su prueba (RNF-12).
 *
 * <p>Implementa: HU-06 a HU-09 (materias en v2), HU-10 a HU-15 (sesiones de estudio en v3) y conserva los datos de HU-01, HU-03 y HU-04.
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

    /** Versión 2 a versión 3: crea la tabla {@code sesion_estudio} con FKs e índices (HU-10 a HU-15). */
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase base) {
            base.execSQL("CREATE TABLE IF NOT EXISTS `sesion_estudio` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`materia_id` INTEGER NOT NULL, "
                    + "`fecha` TEXT NOT NULL, "
                    + "`hora_inicio` TEXT NOT NULL, "
                    + "`hora_fin` TEXT NOT NULL, "
                    + "`duracion_planificada_minutos` INTEGER NOT NULL, "
                    + "`duracion_real_minutos` INTEGER, "
                    + "`estado` TEXT NOT NULL, "
                    + "`notas` TEXT, "
                    + "`sesion_origen_id` INTEGER, "
                    + "FOREIGN KEY(`materia_id`) REFERENCES `materia`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , "
                    + "FOREIGN KEY(`sesion_origen_id`) REFERENCES `sesion_estudio`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )");
            base.execSQL("CREATE INDEX IF NOT EXISTS `index_sesion_estudio_materia_id` ON `sesion_estudio` (`materia_id`)");
            base.execSQL("CREATE INDEX IF NOT EXISTS `index_sesion_estudio_fecha` ON `sesion_estudio` (`fecha`)");
            base.execSQL("CREATE INDEX IF NOT EXISTS `index_sesion_estudio_estado` ON `sesion_estudio` (`estado`)");
            base.execSQL("CREATE INDEX IF NOT EXISTS `index_sesion_estudio_sesion_origen_id` ON `sesion_estudio` (`sesion_origen_id`)");
        }
    };

    /** Versión 3 a versión 4: añade ajustes Pomodoro/DND, columnas de técnica en sesión y tabla {@code evento_estudio}. */
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase base) {
            base.execSQL("ALTER TABLE `ajustes` ADD COLUMN `no_molestar_global` INTEGER NOT NULL DEFAULT 0");
            base.execSQL("ALTER TABLE `ajustes` ADD COLUMN `pomodoro_trabajo_min` INTEGER NOT NULL DEFAULT 25");
            base.execSQL("ALTER TABLE `ajustes` ADD COLUMN `pomodoro_descanso_corto_min` INTEGER NOT NULL DEFAULT 5");
            base.execSQL("ALTER TABLE `ajustes` ADD COLUMN `pomodoro_descanso_largo_min` INTEGER NOT NULL DEFAULT 15");
            base.execSQL("ALTER TABLE `ajustes` ADD COLUMN `pomodoro_ciclos_hasta_largo` INTEGER NOT NULL DEFAULT 4");

            base.execSQL("ALTER TABLE `sesion_estudio` ADD COLUMN `tecnica` TEXT NOT NULL DEFAULT 'LIBRE'");
            base.execSQL("ALTER TABLE `sesion_estudio` ADD COLUMN `forzar_no_molestar` INTEGER");
            base.execSQL("ALTER TABLE `sesion_estudio` ADD COLUMN `pomodoro_trabajo_min` INTEGER");
            base.execSQL("ALTER TABLE `sesion_estudio` ADD COLUMN `pomodoro_descanso_corto_min` INTEGER");
            base.execSQL("ALTER TABLE `sesion_estudio` ADD COLUMN `pomodoro_descanso_largo_min` INTEGER");
            base.execSQL("ALTER TABLE `sesion_estudio` ADD COLUMN `pomodoro_ciclos_hasta_largo` INTEGER");

            base.execSQL("CREATE TABLE IF NOT EXISTS `evento_estudio` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`sesion_id` INTEGER NOT NULL, "
                    + "`tipo` TEXT NOT NULL, "
                    + "`instante_milis` INTEGER NOT NULL, "
                    + "`fecha` TEXT NOT NULL, "
                    + "`hora` TEXT NOT NULL, "
                    + "`nota` TEXT, "
                    + "FOREIGN KEY(`sesion_id`) REFERENCES `sesion_estudio`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            base.execSQL("CREATE INDEX IF NOT EXISTS `index_evento_estudio_sesion_id` ON `evento_estudio` (`sesion_id`)");
            base.execSQL("CREATE INDEX IF NOT EXISTS `index_evento_estudio_instante_milis` ON `evento_estudio` (`instante_milis`)");
        }
    };
}
