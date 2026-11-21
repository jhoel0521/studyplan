package io.github.jhoel0521.studyplan.adapter.out.persistence;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.Calendar;

import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.valueobject.Instante;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

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

    /**
     * Versión 4 -> 5: migra {@code sesion_estudio} y {@code evento_estudio} a instantes UTC +
     * desfase local y materializa {@code tramo_diario} para el corte diario estricto ({@code RN-31}).
     */
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase base) {
            base.execSQL("CREATE TABLE IF NOT EXISTS `sesion_estudio_nueva` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`materia_id` INTEGER NOT NULL, "
                    + "`plan_inicio_utc_milis` INTEGER NOT NULL, "
                    + "`plan_fin_utc_milis` INTEGER NOT NULL, "
                    + "`desfase_minutos` INTEGER NOT NULL, "
                    + "`duracion_planificada_minutos` INTEGER NOT NULL, "
                    + "`duracion_real_minutos` INTEGER, "
                    + "`estado` TEXT NOT NULL, "
                    + "`notas` TEXT, "
                    + "`sesion_origen_id` INTEGER, "
                    + "`tecnica` TEXT NOT NULL, "
                    + "`forzar_no_molestar` INTEGER, "
                    + "`pomodoro_trabajo_min` INTEGER, "
                    + "`pomodoro_descanso_corto_min` INTEGER, "
                    + "`pomodoro_descanso_largo_min` INTEGER, "
                    + "`pomodoro_ciclos_hasta_largo` INTEGER, "
                    + "FOREIGN KEY(`materia_id`) REFERENCES `materia`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT, "
                    + "FOREIGN KEY(`sesion_origen_id`) REFERENCES `sesion_estudio`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )");

            try (Cursor c = base.query(
                    "SELECT id, materia_id, fecha, hora_inicio, hora_fin, "
                            + "duracion_planificada_minutos, duracion_real_minutos, estado, notas, "
                            + "sesion_origen_id, tecnica, forzar_no_molestar, pomodoro_trabajo_min, "
                            + "pomodoro_descanso_corto_min, pomodoro_descanso_largo_min, "
                            + "pomodoro_ciclos_hasta_largo FROM sesion_estudio")) {
                while (c.moveToNext()) {
                    long id = c.getLong(0);
                    long materiaId = c.getLong(1);
                    String fechaStr = c.getString(2);
                    String horaIniStr = c.getString(3);
                    String horaFinStr = c.getString(4);
                    int durPlan = c.getInt(5);
                    Integer durReal = c.isNull(6) ? null : c.getInt(6);
                    String estado = c.getString(7);
                    String notas = c.isNull(8) ? null : c.getString(8);
                    Long sesionOrigenId = c.isNull(9) ? null : c.getLong(9);
                    String tecnica = c.getString(10);
                    Integer forzarNoMolestar = c.isNull(11) ? null : c.getInt(11);
                    Integer pomTrabajo = c.isNull(12) ? null : c.getInt(12);
                    Integer pomCorto = c.isNull(13) ? null : c.getInt(13);
                    Integer pomLargo = c.isNull(14) ? null : c.getInt(14);
                    Integer pomCiclos = c.isNull(15) ? null : c.getInt(15);

                    long[] inst = convertirHorarioLocalAUctYDesfase(fechaStr, horaIniStr, horaFinStr);
                    base.execSQL(
                            "INSERT INTO `sesion_estudio_nueva` ("
                                    + "id, materia_id, plan_inicio_utc_milis, plan_fin_utc_milis, desfase_minutos, "
                                    + "duracion_planificada_minutos, duracion_real_minutos, estado, notas, "
                                    + "sesion_origen_id, tecnica, forzar_no_molestar, pomodoro_trabajo_min, "
                                    + "pomodoro_descanso_corto_min, pomodoro_descanso_largo_min, "
                                    + "pomodoro_ciclos_hasta_largo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            new Object[]{
                                    id, materiaId, inst[0], inst[1], (int) inst[2],
                                    durPlan, durReal, estado, notas,
                                    sesionOrigenId, tecnica, forzarNoMolestar, pomTrabajo,
                                    pomCorto, pomLargo, pomCiclos
                            });
                }
            }

            base.execSQL("CREATE TABLE IF NOT EXISTS `evento_estudio_nueva` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`sesion_id` INTEGER NOT NULL, "
                    + "`tipo` TEXT NOT NULL, "
                    + "`instante_milis` INTEGER NOT NULL, "
                    + "`desfase_minutos` INTEGER NOT NULL, "
                    + "`nota` TEXT, "
                    + "FOREIGN KEY(`sesion_id`) REFERENCES `sesion_estudio`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");

            try (Cursor c = base.query(
                    "SELECT id, sesion_id, tipo, instante_milis, fecha, hora, nota FROM evento_estudio")) {
                while (c.moveToNext()) {
                    long id = c.getLong(0);
                    long sesionId = c.getLong(1);
                    String tipo = c.getString(2);
                    long instanteMilis = c.getLong(3);
                    String fechaStr = c.getString(4);
                    String horaStr = c.getString(5);
                    String nota = c.isNull(6) ? null : c.getString(6);
                    int desfase = calcularDesfaseEvento(instanteMilis, fechaStr, horaStr);
                    base.execSQL(
                            "INSERT INTO `evento_estudio_nueva` "
                                    + "(id, sesion_id, tipo, instante_milis, desfase_minutos, nota) "
                                    + "VALUES (?, ?, ?, ?, ?, ?)",
                            new Object[]{id, sesionId, tipo, instanteMilis, desfase, nota});
                }
            }

            base.execSQL("DROP TABLE `evento_estudio`");
            base.execSQL("DROP TABLE `sesion_estudio`");
            base.execSQL("ALTER TABLE `sesion_estudio_nueva` RENAME TO `sesion_estudio`");
            base.execSQL("ALTER TABLE `evento_estudio_nueva` RENAME TO `evento_estudio`");

            base.execSQL("CREATE INDEX IF NOT EXISTS `index_sesion_estudio_materia_id` ON `sesion_estudio` (`materia_id`)");
            base.execSQL("CREATE INDEX IF NOT EXISTS `index_sesion_estudio_plan_inicio_utc_milis` ON `sesion_estudio` (`plan_inicio_utc_milis`)");
            base.execSQL("CREATE INDEX IF NOT EXISTS `index_sesion_estudio_estado` ON `sesion_estudio` (`estado`)");
            base.execSQL("CREATE INDEX IF NOT EXISTS `index_sesion_estudio_sesion_origen_id` ON `sesion_estudio` (`sesion_origen_id`)");
            base.execSQL("CREATE INDEX IF NOT EXISTS `index_evento_estudio_sesion_id` ON `evento_estudio` (`sesion_id`)");
            base.execSQL("CREATE INDEX IF NOT EXISTS `index_evento_estudio_instante_milis` ON `evento_estudio` (`instante_milis`)");

            base.execSQL("CREATE TABLE IF NOT EXISTS `tramo_diario` ("
                    + "`sesion_id` INTEGER NOT NULL, "
                    + "`fecha_local` TEXT NOT NULL, "
                    + "`minutos` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`sesion_id`, `fecha_local`), "
                    + "FOREIGN KEY(`sesion_id`) REFERENCES `sesion_estudio`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            base.execSQL("CREATE INDEX IF NOT EXISTS `index_tramo_diario_fecha_local` ON `tramo_diario` (`fecha_local`)");

            try (Cursor c = base.query(
                    "SELECT id, plan_inicio_utc_milis, desfase_minutos, duracion_real_minutos "
                            + "FROM sesion_estudio "
                            + "WHERE estado IN ('COMPLETADA', 'COMPLETADA_TARDIA') AND duracion_real_minutos > 0")) {
                while (c.moveToNext()) {
                    long sesionId = c.getLong(0);
                    long inicioUtc = c.getLong(1);
                    int desfase = c.getInt(2);
                    int minutos = c.getInt(3);
                    String fechaLocal = Instante.de(inicioUtc, desfase).fechaLocal().iso();
                    base.execSQL(
                            "INSERT OR REPLACE INTO `tramo_diario` (sesion_id, fecha_local, minutos) VALUES (?, ?, ?)",
                            new Object[]{sesionId, fechaLocal, minutos});
                }
            }
        }
    };

    /**
     * Versión 5 -> 6: añade {@code meta_semanal_horas} y {@code es_general} a {@code materia},
     * inserta la materia general {@code Estudios varios}, permite {@code plan_fin_utc_milis}
     * nulo en {@code sesion_estudio} y crea la tabla {@code meta_semanal} ({@code HU-25} a
     * {@code HU-28}, {@code HU-47}, {@code RN-32}, {@code RN-33}).
     */
    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase base) {
            base.execSQL("ALTER TABLE `materia` ADD COLUMN `meta_semanal_horas` REAL");
            base.execSQL("ALTER TABLE `materia` ADD COLUMN `es_general` INTEGER NOT NULL DEFAULT 0");
            StudyPlanDatabase.asegurarMateriaGeneral(base);

            base.execSQL("CREATE TABLE IF NOT EXISTS `sesion_estudio_nueva` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`materia_id` INTEGER NOT NULL, "
                    + "`plan_inicio_utc_milis` INTEGER NOT NULL, "
                    + "`plan_fin_utc_milis` INTEGER, "
                    + "`desfase_minutos` INTEGER NOT NULL, "
                    + "`duracion_planificada_minutos` INTEGER NOT NULL, "
                    + "`duracion_real_minutos` INTEGER, "
                    + "`estado` TEXT NOT NULL, "
                    + "`notas` TEXT, "
                    + "`sesion_origen_id` INTEGER, "
                    + "`tecnica` TEXT NOT NULL, "
                    + "`forzar_no_molestar` INTEGER, "
                    + "`pomodoro_trabajo_min` INTEGER, "
                    + "`pomodoro_descanso_corto_min` INTEGER, "
                    + "`pomodoro_descanso_largo_min` INTEGER, "
                    + "`pomodoro_ciclos_hasta_largo` INTEGER, "
                    + "FOREIGN KEY(`materia_id`) REFERENCES `materia`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , "
                    + "FOREIGN KEY(`sesion_origen_id`) REFERENCES `sesion_estudio`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )");
            base.execSQL("INSERT INTO `sesion_estudio_nueva` ("
                    + "`id`, `materia_id`, `plan_inicio_utc_milis`, `plan_fin_utc_milis`, "
                    + "`desfase_minutos`, `duracion_planificada_minutos`, `duracion_real_minutos`, "
                    + "`estado`, `notas`, `sesion_origen_id`, `tecnica`, `forzar_no_molestar`, "
                    + "`pomodoro_trabajo_min`, `pomodoro_descanso_corto_min`, "
                    + "`pomodoro_descanso_largo_min`, `pomodoro_ciclos_hasta_largo`) "
                    + "SELECT `id`, `materia_id`, `plan_inicio_utc_milis`, `plan_fin_utc_milis`, "
                    + "`desfase_minutos`, `duracion_planificada_minutos`, `duracion_real_minutos`, "
                    + "`estado`, `notas`, `sesion_origen_id`, `tecnica`, `forzar_no_molestar`, "
                    + "`pomodoro_trabajo_min`, `pomodoro_descanso_corto_min`, "
                    + "`pomodoro_descanso_largo_min`, `pomodoro_ciclos_hasta_largo` "
                    + "FROM `sesion_estudio`");
            base.execSQL("DROP TABLE `sesion_estudio`");
            base.execSQL("ALTER TABLE `sesion_estudio_nueva` RENAME TO `sesion_estudio`");
            base.execSQL("CREATE INDEX IF NOT EXISTS `index_sesion_estudio_materia_id` ON `sesion_estudio` (`materia_id`)");
            base.execSQL("CREATE INDEX IF NOT EXISTS `index_sesion_estudio_plan_inicio_utc_milis` ON `sesion_estudio` (`plan_inicio_utc_milis`)");
            base.execSQL("CREATE INDEX IF NOT EXISTS `index_sesion_estudio_estado` ON `sesion_estudio` (`estado`)");
            base.execSQL("CREATE INDEX IF NOT EXISTS `index_sesion_estudio_sesion_origen_id` ON `sesion_estudio` (`sesion_origen_id`)");

            base.execSQL("CREATE TABLE IF NOT EXISTS `meta_semanal` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`semana_inicio` TEXT NOT NULL, "
                    + "`materia_id` INTEGER, "
                    + "`horas_objetivo` REAL NOT NULL, "
                    + "`felicitada` INTEGER NOT NULL, "
                    + "FOREIGN KEY(`materia_id`) REFERENCES `materia`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            base.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_meta_semanal_semana_inicio_materia_id` "
                    + "ON `meta_semanal` (`semana_inicio`, `materia_id`)");
            base.execSQL("CREATE INDEX IF NOT EXISTS `index_meta_semanal_materia_id` "
                    + "ON `meta_semanal` (`materia_id`)");
        }
    };

    /**
     * Versión 6 -> 7: añade la columna nullable {@code topico} a {@code evento_estudio} para
     * clasificar pausas, salidas y estudio externo ({@code HU-39}, {@code HU-45}, {@code RN-30}).
     */
    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase base) {
            base.execSQL("ALTER TABLE `evento_estudio` ADD COLUMN `topico` TEXT");
        }
    };

    /**
     * Versión 7 -> 8: añade {@code recordatorio_minutos} a {@code ajustes} (valor por defecto
     * {@code 15}) y {@code recordatorio_minutos} nullable a {@code sesion_estudio} ({@code HU-34},
     * {@code RNF-12}).
     */
    public static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase base) {
            base.execSQL(
                    "ALTER TABLE `ajustes` ADD COLUMN `recordatorio_minutos` INTEGER NOT NULL DEFAULT 15");
            base.execSQL(
                    "ALTER TABLE `sesion_estudio` ADD COLUMN `recordatorio_minutos` INTEGER");
        }
    };

    private static long[] convertirHorarioLocalAUctYDesfase(
            String fechaStr, String horaInicioStr, String horaFinStr) {
        Resultado<Fecha> f = Fecha.parsear(fechaStr);
        Resultado<Hora> hi = Hora.parsear(horaInicioStr);
        Resultado<Hora> hf = Hora.parsear(horaFinStr);
        if (!f.esValido() || !hi.esValido() || !hf.esValido()) {
            long ahora = System.currentTimeMillis();
            int desfase = Calendar.getInstance().getTimeZone().getOffset(ahora) / 60_000;
            return new long[]{ahora, ahora, desfase};
        }
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, f.valor().anio());
        cal.set(Calendar.MONTH, f.valor().mes() - 1);
        cal.set(Calendar.DAY_OF_MONTH, f.valor().dia());
        cal.set(Calendar.HOUR_OF_DAY, hi.valor().hora());
        cal.set(Calendar.MINUTE, hi.valor().minuto());
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long inicioUtc = cal.getTimeInMillis();
        int desfase = cal.getTimeZone().getOffset(inicioUtc) / 60_000;
        Fecha fechaFin = hf.valor().esPosteriorA(hi.valor()) ? f.valor() : f.valor().sumarDias(1);
        long finUtc = Instante.desdeFechaYHoraLocal(fechaFin, hf.valor(), desfase).utcMilis();
        return new long[]{inicioUtc, finUtc, desfase};
    }

    private static int calcularDesfaseEvento(long instanteMilis, String fechaStr, String horaStr) {
        Resultado<Fecha> f = Fecha.parsear(fechaStr);
        Resultado<Hora> h = Hora.parsear(horaStr);
        if (!f.esValido() || !h.esValido()) {
            return Calendar.getInstance().getTimeZone().getOffset(instanteMilis) / 60_000;
        }
        long localMilis = f.valor().diasDesdeEpoca() * Instante.MILIS_POR_DIA
                + h.valor().minutosDesdeMedianoche() * Instante.MILIS_POR_MINUTO;
        long diffMin = Math.round((localMilis - instanteMilis) / (double) Instante.MILIS_POR_MINUTO);
        return (int) Math.max(
                Instante.DESFASE_MINIMO_MINUTOS,
                Math.min(Instante.DESFASE_MAXIMO_MINUTOS, diffMin));
    }
}
