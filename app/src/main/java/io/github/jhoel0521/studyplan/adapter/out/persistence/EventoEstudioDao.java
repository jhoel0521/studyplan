package io.github.jhoel0521.studyplan.adapter.out.persistence;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

/**
 * Acceso a la tabla {@code evento_estudio} (HU-39, RN-22, RNF-12).
 */
@Dao
public abstract class EventoEstudioDao {

    @Insert
    public abstract long insertar(EventoEstudioEntity evento);

    @Query("SELECT * FROM evento_estudio WHERE sesion_id = :sesionId ORDER BY instante_milis ASC, id ASC")
    public abstract List<EventoEstudioEntity> listarPorSesion(long sesionId);

    @Query("DELETE FROM evento_estudio WHERE sesion_id = :sesionId")
    public abstract void eliminarPorSesion(long sesionId);

    @Query("DELETE FROM evento_estudio")
    public abstract void eliminarTodos();
}
