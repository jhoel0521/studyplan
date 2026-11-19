package io.github.jhoel0521.studyplan.adapter.out.persistence;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Fila de la tabla de ajustes (una sola fila). Tema e idioma se guardan como texto con el nombre de la enumeración.
 *
 * <p>Implementa: HU-03, HU-04, HU-23, HU-24 y HU-34.
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

    @ColumnInfo(name = "no_molestar_global")
    public boolean noMolestarGlobal;

    @ColumnInfo(name = "pomodoro_trabajo_min")
    public int pomodoroTrabajoMin = 25;

    @ColumnInfo(name = "pomodoro_descanso_corto_min")
    public int pomodoroDescansoCortoMin = 5;

    @ColumnInfo(name = "pomodoro_descanso_largo_min")
    public int pomodoroDescansoLargoMin = 15;

    @ColumnInfo(name = "pomodoro_ciclos_hasta_largo")
    public int pomodoroCiclosHastaLargo = 4;

    @ColumnInfo(name = "recordatorio_minutos")
    public int recordatorioMinutos = 15;
}
