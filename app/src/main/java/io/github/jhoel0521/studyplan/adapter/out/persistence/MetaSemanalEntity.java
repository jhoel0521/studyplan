package io.github.jhoel0521.studyplan.adapter.out.persistence;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Snapshot semanal de una meta de estudio (total si {@code materia_id == null} o por materia)
 * en Room v6 ({@code HU-25} a {@code HU-28}, {@code RN-14}, {@code RN-16}, {@code RN-33}).
 */
@Entity(
        tableName = "meta_semanal",
        foreignKeys = {
                @ForeignKey(
                        entity = MateriaEntity.class,
                        parentColumns = "id",
                        childColumns = "materia_id",
                        onDelete = ForeignKey.CASCADE)
        },
        indices = {
                @Index(value = {"semana_inicio", "materia_id"}, unique = true),
                @Index(value = {"materia_id"})
        })
public class MetaSemanalEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "semana_inicio")
    public String semanaInicio = "";

    @Nullable
    @ColumnInfo(name = "materia_id")
    public Long materiaId;

    @ColumnInfo(name = "horas_objetivo")
    public double horasObjetivo;

    public boolean felicitada;
}
