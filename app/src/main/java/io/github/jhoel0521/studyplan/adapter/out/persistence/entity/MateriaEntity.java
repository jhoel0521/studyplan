package io.github.jhoel0521.studyplan.adapter.out.persistence.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Fila de la tabla de materias. La clave de unicidad se guarda en su propia columna con un índice no único: dos
 * materias pueden compartir clave si una está archivada. Esta clase nunca sale de {@code adapter.out.persistence}.
 *
 * <p>Implementa: HU-06 a HU-09, HU-46 y RN-02.
 */
@Entity(tableName = "materia", indices = {@Index(value = {"nombre_clave"})})
public class MateriaEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String nombre = "";

    @NonNull
    @ColumnInfo(name = "nombre_clave")
    public String nombreClave = "";

    public int color;

    @Nullable
    public String docente;

    public boolean archivada;

    @Nullable
    @ColumnInfo(name = "meta_semanal_horas")
    public Double metaSemanalHoras;

    @ColumnInfo(name = "es_general")
    public boolean esGeneral;
}
