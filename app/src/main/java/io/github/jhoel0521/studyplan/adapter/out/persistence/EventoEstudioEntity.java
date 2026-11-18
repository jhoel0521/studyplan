package io.github.jhoel0521.studyplan.adapter.out.persistence;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Fila de la tabla {@code evento_estudio} en Room v4 (HU-39, RN-22).
 */
@Entity(
        tableName = "evento_estudio",
        foreignKeys = {
                @ForeignKey(
                        entity = SesionEntity.class,
                        parentColumns = "id",
                        childColumns = "sesion_id",
                        onDelete = ForeignKey.CASCADE)
        },
        indices = {
                @Index(value = {"sesion_id"}),
                @Index(value = {"instante_milis"})
        })
public class EventoEstudioEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "sesion_id")
    public long sesionId;

    @NonNull
    public String tipo = "";

    @ColumnInfo(name = "instante_milis")
    public long instanteMilis;

    @ColumnInfo(name = "desfase_minutos")
    public int desfaseMinutos;

    @Nullable
    public String nota;

    @Nullable
    public String topico;
}
