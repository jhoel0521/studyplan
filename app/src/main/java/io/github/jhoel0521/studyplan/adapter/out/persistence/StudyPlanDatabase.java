package io.github.jhoel0521.studyplan.adapter.out.persistence;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Base de datos Room de la aplicación (versión 3: perfil, ajustes, materias y sesiones de estudio).
 * Nunca se permite consultar desde el hilo principal ni se usa migración destructiva (el esquema
 * JSON se exporta a {@code app/schemas/}): una versión nueva exigirá una migración escrita (RNF-12).
 *
 * <p>Implementa: HU-01 a HU-15 y HU-38.
 */
@Database(
        entities = {
                PerfilEntity.class,
                AjustesEntity.class,
                MateriaEntity.class,
                SesionEntity.class
        },
        version = 3)
public abstract class StudyPlanDatabase extends RoomDatabase {

    public static final String NOMBRE = "studyplan.db";

    public abstract PerfilDao perfilDao();

    public abstract AjustesDao ajustesDao();

    public abstract MateriaDao materiaDao();

    public abstract SesionDao sesionDao();

    /** Abre la base en disco. */
    public static StudyPlanDatabase abrir(Context contexto) {
        return Room.databaseBuilder(contexto.getApplicationContext(), StudyPlanDatabase.class, NOMBRE)
                .addMigrations(Migraciones.MIGRATION_1_2, Migraciones.MIGRATION_2_3)
                .build();
    }
}
