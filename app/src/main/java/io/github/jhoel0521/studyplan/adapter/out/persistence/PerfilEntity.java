package io.github.jhoel0521.studyplan.adapter.out.persistence;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Fila de la tabla del perfil. Solo existe una fila (id fijo), porque el teléfono tiene un único perfil. Esta clase
 * nunca sale de {@code adapter.out.persistence}: el núcleo usa {@code Perfil}.
 *
 * <p>Implementa: HU-01 y RN-01.
 */
@Entity(tableName = "perfil")
public class PerfilEntity {

    /** Id de la única fila del perfil. */
    public static final int ID_UNICO = 1;

    @PrimaryKey
    @SuppressWarnings("CanBeFinal") // Room asigna los campos al leer la fila
    public int id = ID_UNICO;

    @NonNull
    public String nombre = "";

    public double metaSemanalHoras;
}
