package io.github.jhoel0521.studyplan.adapter.out.persistence;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import io.github.jhoel0521.studyplan.domain.Materia;

/**
 * Base de datos Room de la aplicación (versión 6: perfil, ajustes, materias con meta y materia
 * general, sesiones con plan opcional, eventos, tramos diarios y snapshots de metas semanales).
 * Nunca se permite consultar desde el hilo principal ni se usa migración destructiva (el esquema
 * JSON se exporta a {@code app/schemas/}): una versión nueva exigirá una migración escrita (RNF-12).
 *
 * <p>Implementa: HU-01 a HU-15, HU-25 a HU-28, HU-38, HU-47, HU-48.
 */
@Database(
        entities = {
                PerfilEntity.class,
                AjustesEntity.class,
                MateriaEntity.class,
                SesionEntity.class,
                EventoEstudioEntity.class,
                TramoDiarioEntity.class,
                MetaSemanalEntity.class
        },
        version = 7)
public abstract class StudyPlanDatabase extends RoomDatabase {

    public static final String NOMBRE = "studyplan.db";
    private static final int COLOR_MATERIA_GENERAL = 0xFF607D8B;

    public abstract PerfilDao perfilDao();

    public abstract AjustesDao ajustesDao();

    public abstract MateriaDao materiaDao();

    public abstract SesionDao sesionDao();

    public abstract EventoEstudioDao eventoEstudioDao();

    public abstract TramoDiarioDao tramoDiarioDao();

    public abstract MetaSemanalDao metaSemanalDao();

    /** Abre la base en disco. */
    public static StudyPlanDatabase abrir(Context contexto) {
        return Room.databaseBuilder(contexto.getApplicationContext(), StudyPlanDatabase.class, NOMBRE)
                .addMigrations(
                        Migraciones.MIGRATION_1_2,
                        Migraciones.MIGRATION_2_3,
                        Migraciones.MIGRATION_3_4,
                        Migraciones.MIGRATION_4_5,
                        Migraciones.MIGRATION_5_6,
                        Migraciones.MIGRATION_6_7)
                .addCallback(new Callback() {
                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        asegurarMateriaGeneral(db);
                    }
                })
                .build();
    }

    static void asegurarMateriaGeneral(@NonNull SupportSQLiteDatabase db) {
        db.execSQL(
                "INSERT INTO `materia` (`nombre`, `nombre_clave`, `color`, `docente`, `archivada`, `meta_semanal_horas`, `es_general`) "
                        + "SELECT ?, ?, ?, NULL, 0, NULL, 1 "
                        + "WHERE NOT EXISTS (SELECT 1 FROM `materia` WHERE `es_general` = 1)",
                new Object[]{Materia.NOMBRE_GENERAL, Materia.CLAVE_GENERAL, COLOR_MATERIA_GENERAL});
    }
}
