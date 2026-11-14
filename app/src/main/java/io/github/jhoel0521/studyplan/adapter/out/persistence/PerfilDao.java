package io.github.jhoel0521.studyplan.adapter.out.persistence;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/**
 * Acceso a la tabla del perfil. Los métodos son síncronos: quien los llama debe hacerlo fuera del hilo principal.
 *
 * <p>Implementa: HU-01 y RN-01.
 */
@Dao
public interface PerfilDao {

    @Query("SELECT * FROM perfil WHERE id = 1")
    PerfilEntity obtener();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardar(PerfilEntity perfil);
}
