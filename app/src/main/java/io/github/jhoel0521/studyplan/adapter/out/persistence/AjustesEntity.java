package io.github.jhoel0521.studyplan.adapter.out.persistence;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Fila de la tabla de ajustes (una sola fila). Tema e idioma se guardan como texto con el nombre de la enumeración.
 *
 * <p>Implementa: HU-03 y HU-04.
 */
@Entity(tableName = "ajustes")
public class AjustesEntity {

    /** Id de la única fila de ajustes. */
    public static final int ID_UNICO = 1;

    @PrimaryKey
    public int id = ID_UNICO;

    @NonNull
    public String tema = "";

    @NonNull
    public String idioma = "";

    public boolean bloqueoActivo;
}
