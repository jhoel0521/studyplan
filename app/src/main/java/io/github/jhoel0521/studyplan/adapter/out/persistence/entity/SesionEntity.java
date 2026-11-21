package io.github.jhoel0521.studyplan.adapter.out.persistence.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Fila de la tabla {@code sesion_estudio} en Room v8.
 *
 * <p>Mantiene integridad referencial con {@link MateriaEntity} ({@code ON DELETE RESTRICT} como
 * respaldo de RN-03) y autorreferencia en {@code sesion_origen_id} ({@code ON DELETE SET NULL}
 * para el historial de reprogramaciones RN-09). Implementa HU-10 a HU-15 y HU-34.
 */
@Entity(
        tableName = "sesion_estudio",
        foreignKeys = {
                @ForeignKey(
                        entity = MateriaEntity.class,
                        parentColumns = "id",
                        childColumns = "materia_id",
                        onDelete = ForeignKey.RESTRICT),
                @ForeignKey(
                        entity = SesionEntity.class,
                        parentColumns = "id",
                        childColumns = "sesion_origen_id",
                        onDelete = ForeignKey.SET_NULL)
        },
        indices = {
                @Index(value = {"materia_id"}),
                @Index(value = {"plan_inicio_utc_milis"}),
                @Index(value = {"estado"}),
                @Index(value = {"sesion_origen_id"})
        })
public class SesionEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "materia_id")
    public long materiaId;

    @ColumnInfo(name = "plan_inicio_utc_milis")
    public long planInicioUtcMilis;

    @Nullable
    @ColumnInfo(name = "plan_fin_utc_milis")
    public Long planFinUtcMilis;

    @ColumnInfo(name = "desfase_minutos")
    public int desfaseMinutos;

    @ColumnInfo(name = "duracion_planificada_minutos")
    public int duracionPlanificadaMinutos;

    @Nullable
    @ColumnInfo(name = "duracion_real_minutos")
    public Integer duracionRealMinutos;

    @NonNull
    public String estado = "";

    @Nullable
    public String notas;

    @Nullable
    @ColumnInfo(name = "sesion_origen_id")
    public Long sesionOrigenId;

    @NonNull
    @ColumnInfo(name = "tecnica")
    public String tecnica = "LIBRE";

    @Nullable
    @ColumnInfo(name = "forzar_no_molestar")
    public Boolean forzarNoMolestar;

    @Nullable
    @ColumnInfo(name = "pomodoro_trabajo_min")
    public Integer pomodoroTrabajoMin;

    @Nullable
    @ColumnInfo(name = "pomodoro_descanso_corto_min")
    public Integer pomodoroDescansoCortoMin;

    @Nullable
    @ColumnInfo(name = "pomodoro_descanso_largo_min")
    public Integer pomodoroDescansoLargoMin;

    @Nullable
    @ColumnInfo(name = "pomodoro_ciclos_hasta_largo")
    public Integer pomodoroCiclosHastaLargo;

    @Nullable
    @ColumnInfo(name = "recordatorio_minutos")
    public Integer recordatorioMinutos;
}
