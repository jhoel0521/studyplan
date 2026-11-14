package io.github.jhoel0521.studyplan.adapter.out.persistence;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

/**
 * Acceso a la tabla de materias. Los métodos son síncronos: quien los llama debe hacerlo fuera del hilo principal.
 *
 * <p>Implementa: HU-06 a HU-09 y HU-46.
 */
@Dao
public interface MateriaDao {

    /** Inserta una fila nueva (id 0: la base asigna uno) y devuelve su id. */
    @Insert
    long insertar(MateriaEntity materia);

    /**
     * Actualiza la fila con ese id sin borrarla y volver a insertarla: así, cuando existan tablas que apunten a
     * {@code materia}, editar o archivar no dispara sus claves foráneas.
     */
    @Update
    void actualizar(MateriaEntity materia);

    @Query("SELECT * FROM materia WHERE id = :id")
    MateriaEntity obtener(long id);

    @Query("SELECT * FROM materia WHERE archivada = 0")
    List<MateriaEntity> activas();

    @Query("SELECT * FROM materia WHERE archivada = 1")
    List<MateriaEntity> archivadas();

    @Query("DELETE FROM materia WHERE id = :id")
    void eliminar(long id);
}
