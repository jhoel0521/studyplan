package io.github.jhoel0521.studyplan.application.port.out;

import java.util.List;
import java.util.Optional;

import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.MetaSemanal;

/**
 * Puerto de salida para persistir y consultar snapshots semanales de metas ({@code HU-25} a
 * {@code HU-28}, {@code RN-16}, {@code RN-33}).
 */
public interface MetaSemanalRepository {

    /** Busca el snapshot de la semana {@code semanaInicio} para {@code materiaId} ({@code null} = total). */
    Optional<MetaSemanal> buscar(Fecha semanaInicio, Long materiaId);

    /** Lista todos los snapshots registrados para {@code semanaInicio}. */
    List<MetaSemanal> listarDeSemana(Fecha semanaInicio);

    /** Lista todos los snapshots existentes ordenados cronológicamente. */
    List<MetaSemanal> listarTodas();

    /** Inserta o actualiza un snapshot por su clave única {@code (semanaInicio, materiaId)}. */
    MetaSemanal guardar(MetaSemanal meta);
}
