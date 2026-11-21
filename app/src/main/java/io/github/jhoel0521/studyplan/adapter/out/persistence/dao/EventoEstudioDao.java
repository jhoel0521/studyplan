package io.github.jhoel0521.studyplan.adapter.out.persistence.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import io.github.jhoel0521.studyplan.adapter.out.persistence.entity.EventoEstudioEntity;

/**
 * Acceso a la tabla {@code evento_estudio} (HU-39, RN-22, RNF-12).
 */
@Dao
public abstract class EventoEstudioDao {

    @Insert
    public abstract long insertar(EventoEstudioEntity evento);

    @Query("SELECT * FROM evento_estudio WHERE sesion_id = :sesionId ORDER BY instante_milis ASC, id ASC")
    public abstract List<EventoEstudioEntity> listarPorSesion(long sesionId);

    @Query("SELECT e.* FROM evento_estudio e "
            + "INNER JOIN sesion_estudio s ON s.id = e.sesion_id "
            + "WHERE COALESCE(s.plan_fin_utc_milis, s.plan_inicio_utc_milis) >= :desdeUtcMilis "
            + "AND s.plan_inicio_utc_milis <= :hastaUtcMilis "
            + "ORDER BY e.instante_milis ASC, e.id ASC")
    public abstract List<EventoEstudioEntity> listarPorRangoSesionesUtc(
            long desdeUtcMilis, long hastaUtcMilis);

    @Query("DELETE FROM evento_estudio WHERE sesion_id = :sesionId")
    public abstract void eliminarPorSesion(long sesionId);

    @Query("DELETE FROM evento_estudio")
    public abstract void eliminarTodos();
}
