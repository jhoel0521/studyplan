package io.github.jhoel0521.studyplan.adapter.out.persistence;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

/**
 * Acceso a la tabla {@code meta_semanal} ({@code HU-25} a {@code HU-28}, {@code RN-33}).
 */
@Dao
public abstract class MetaSemanalDao {

    @Query("SELECT * FROM meta_semanal WHERE semana_inicio = :semanaInicio AND materia_id IS NULL LIMIT 1")
    public abstract MetaSemanalEntity buscarTotal(String semanaInicio);

    @Query("SELECT * FROM meta_semanal WHERE semana_inicio = :semanaInicio AND materia_id = :materiaId LIMIT 1")
    public abstract MetaSemanalEntity buscarPorMateria(String semanaInicio, long materiaId);

    @Query("SELECT * FROM meta_semanal WHERE semana_inicio = :semanaInicio ORDER BY materia_id ASC")
    public abstract List<MetaSemanalEntity> listarDeSemana(String semanaInicio);

    @Query("SELECT * FROM meta_semanal ORDER BY semana_inicio DESC, materia_id ASC")
    public abstract List<MetaSemanalEntity> listarTodas();

    @Insert
    public abstract long insertar(MetaSemanalEntity entidad);

    @Update
    public abstract void actualizar(MetaSemanalEntity entidad);

    /**
     * Inserta o actualiza por clave lógica {@code (semana_inicio, materia_id)}, manejando
     * explícitamente {@code materia_id IS NULL} porque en SQLite los {@code NULL} no colisionan
     * en índices {@code UNIQUE}.
     */
    @Transaction
    public long guardarPorClave(MetaSemanalEntity entidad) {
        MetaSemanalEntity existente = entidad.materiaId == null
                ? buscarTotal(entidad.semanaInicio)
                : buscarPorMateria(entidad.semanaInicio, entidad.materiaId);
        if (existente == null) {
            return insertar(entidad);
        }
        entidad.id = existente.id;
        actualizar(entidad);
        return existente.id;
    }
}
