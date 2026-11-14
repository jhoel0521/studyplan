package io.github.jhoel0521.studyplan.adapter.out.persistence;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Base de datos Room de la aplicación (versión 1: perfil y ajustes). Nunca se permite consultar desde el hilo
 * principal ni se usa migración destructiva (el esquema JSON se exporta a {@code app/schemas/}, que es el valor por defecto): una versión nueva exigirá una migración escrita (RNF-12).
 *
 * <p>Implementa: HU-01, HU-03, HU-04 y HU-38.
 */
@Database(entities = {PerfilEntity.class, AjustesEntity.class}, version = 1)
public abstract class StudyPlanDatabase extends RoomDatabase {

    public static final String NOMBRE = "studyplan.db";

    public abstract PerfilDao perfilDao();

    public abstract AjustesDao ajustesDao();

    /** Abre la base en disco. */
    public static StudyPlanDatabase abrir(Context contexto) {
        return Room.databaseBuilder(contexto.getApplicationContext(), StudyPlanDatabase.class, NOMBRE).build();
    }
}
