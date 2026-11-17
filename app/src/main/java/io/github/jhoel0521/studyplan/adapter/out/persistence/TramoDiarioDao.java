package io.github.jhoel0521.studyplan.adapter.out.persistence;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

/**
 * Acceso a la tabla {@code tramo_diario} ({@code RN-31}, {@code RNF-02}).
 */
@Dao
public abstract class TramoDiarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertarTodos(List<TramoDiarioEntity> tramos);

    @Query("DELETE FROM tramo_diario WHERE sesion_id = :sesionId")
    public abstract void eliminarPorSesion(long sesionId);

    @Transaction
    public void reemplazarPorSesion(long sesionId, List<TramoDiarioEntity> tramos) {
        eliminarPorSesion(sesionId);
        if (tramos != null && !tramos.isEmpty()) {
            insertarTodos(tramos);
        }
    }

    @Query("SELECT COALESCE(SUM(minutos), 0) FROM tramo_diario WHERE fecha_local BETWEEN :desde AND :hasta")
    public abstract int sumarMinutosEnRango(String desde, String hasta);

    @Query("SELECT * FROM tramo_diario WHERE fecha_local BETWEEN :desde AND :hasta")
    public abstract List<TramoDiarioEntity> listarEnRango(String desde, String hasta);

    @Query("SELECT s.materia_id AS materiaId, COALESCE(SUM(t.minutos), 0) AS totalMinutos "
            + "FROM tramo_diario t "
            + "INNER JOIN sesion_estudio s ON s.id = t.sesion_id "
            + "WHERE t.fecha_local BETWEEN :desde AND :hasta "
            + "GROUP BY s.materia_id")
    public abstract List<MinutosPorMateria> sumarPorMateriaEnRango(String desde, String hasta);

    public static final class MinutosPorMateria {
        public long materiaId;
        public int totalMinutos;
    }
}
