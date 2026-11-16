package io.github.jhoel0521.studyplan.adapter.out.persistence;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

/**
 * Acceso a la tabla {@code sesion_estudio}. Todos sus métodos son síncronos y deben invocarse fuera
 * del hilo principal (RNF-12).
 *
 * <p>Implementa soporte de persistencia para HU-02, HU-08, HU-09 y HU-10 a HU-15.
 */
@Dao
public abstract class SesionDao {

    @Insert
    public abstract long insertar(SesionEntity sesion);

    @Update
    public abstract void actualizar(SesionEntity sesion);

    /**
     * Marca la sesión original como {@code REPROGRAMADA} e inserta la nueva sesión enlazada en
     * una sola transacción (HU-14, RN-09).
     */
    @Transaction
    public long reprogramar(SesionEntity originalReprogramada, SesionEntity nuevaPendiente) {
        actualizar(originalReprogramada);
        return insertar(nuevaPendiente);
    }

    @Query("SELECT * FROM sesion_estudio WHERE id = :id")
    public abstract SesionEntity buscarPorId(long id);

    @Query("SELECT * FROM sesion_estudio WHERE sesion_origen_id = :sesionOrigenId ORDER BY id DESC LIMIT 1")
    public abstract SesionEntity buscarReprogramadaHijaDe(long sesionOrigenId);

    @Query("SELECT * FROM sesion_estudio WHERE plan_fin_utc_milis >= :desdeUtc AND plan_inicio_utc_milis <= :hastaUtc ORDER BY plan_inicio_utc_milis ASC")
    public abstract List<SesionEntity> listarPorRangoUtc(long desdeUtc, long hastaUtc);

    @Query("SELECT * FROM sesion_estudio WHERE estado = 'PENDIENTE' AND plan_fin_utc_milis >= :desdeUtc ORDER BY plan_inicio_utc_milis ASC")
    public abstract List<SesionEntity> listarPendientesDesdeUtc(long desdeUtc);

    @Query("SELECT * FROM sesion_estudio WHERE estado = 'PENDIENTE' AND plan_fin_utc_milis <= :hastaUtc ORDER BY plan_inicio_utc_milis ASC")
    public abstract List<SesionEntity> listarPendientesHastaUtc(long hastaUtc);

    @Query("SELECT COALESCE(SUM(duracion_real_minutos), 0) FROM sesion_estudio WHERE materia_id = :materiaId AND estado IN ('COMPLETADA', 'COMPLETADA_TARDIA') AND duracion_real_minutos > 0")
    public abstract int sumarMinutosRealesPorMateria(long materiaId);

    @Query("SELECT COUNT(*) FROM sesion_estudio WHERE materia_id = :materiaId")
    public abstract int contarPorMateria(long materiaId);

    @Query("DELETE FROM sesion_estudio WHERE id = :id")
    public abstract void eliminarPorId(long id);

    @Query("SELECT * FROM sesion_estudio WHERE estado = 'EN_CURSO' ORDER BY id ASC LIMIT 1")
    public abstract SesionEntity buscarEnCurso();

    @Query("SELECT * FROM sesion_estudio WHERE estado = 'EN_CURSO' ORDER BY id ASC")
    public abstract List<SesionEntity> listarEnCurso();

    @Transaction
    public void completarReparto(SesionEntity primeraCompletada, SesionEntity segundaCompletada) {
        actualizar(primeraCompletada);
        actualizar(segundaCompletada);
    }
}
