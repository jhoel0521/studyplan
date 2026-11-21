package io.github.jhoel0521.studyplan.adapter.out.persistence.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

/**
 * Fila de la tabla {@code tramo_diario} en Room v5 que materializa los minutos reales estudiados
 * en cada fecha calendario local ({@code RN-31}).
 */
@Entity(
        tableName = "tramo_diario",
        primaryKeys = {"sesion_id", "fecha_local"},
        foreignKeys = {
                @ForeignKey(
                        entity = SesionEntity.class,
                        parentColumns = "id",
                        childColumns = "sesion_id",
                        onDelete = ForeignKey.CASCADE)
        },
        indices = {
                @Index(value = {"fecha_local"})
        })
public class TramoDiarioEntity {

    @ColumnInfo(name = "sesion_id")
    public long sesionId;

    @NonNull
    @ColumnInfo(name = "fecha_local")
    public String fechaLocal = "";

    public int minutos;
}
