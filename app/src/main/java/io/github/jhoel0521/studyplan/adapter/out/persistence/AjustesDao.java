package io.github.jhoel0521.studyplan.adapter.out.persistence;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/**
 * Acceso a la tabla de ajustes. Los métodos son síncronos: quien los llama debe hacerlo fuera del hilo principal.
 *
 * <p>Implementa: HU-03 y HU-04.
 */
@Dao
public interface AjustesDao {

    @Query("SELECT * FROM ajustes WHERE id = 1")
    AjustesEntity obtener();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardar(AjustesEntity ajustes);
}
