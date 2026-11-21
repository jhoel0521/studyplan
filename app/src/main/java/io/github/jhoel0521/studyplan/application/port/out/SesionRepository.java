package io.github.jhoel0521.studyplan.application.port.out;

import java.util.List;
import java.util.Optional;

import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;

/**
 * Puerto de salida de persistencia para {@link SesionEstudio} (DIP/ISP).
 *
 * <p>Implementa soporte para HU-02, HU-08, HU-09, HU-10 a HU-15, RN-05, RN-08, RN-09 y RN-11.
 */
public interface SesionRepository {

    /** Guarda una sesión nueva y devuelve la copia con su identificador asignado. */
    SesionEstudio guardar(SesionEstudio sesion);

    /** Actualiza el estado y duración real de una sesión existente. */
    void actualizar(SesionEstudio sesion);

    /**
     * Marca {@code originalReprogramada} en {@code REPROGRAMADA} y crea {@code nuevaPendiente} en
     * una única operación atómica, devolviendo la nueva sesión con su identificador (RN-09).
     */
    SesionEstudio reprogramar(SesionEstudio originalReprogramada, SesionEstudio nuevaPendiente);

    /** Busca una sesión por identificador. */
    Optional<SesionEstudio> buscarPorId(long id);

    /** Busca la sesión hija creada al reprogramar {@code sesionOrigenId} (HU-14). */
    Optional<SesionEstudio> buscarReprogramadaHijaDe(long sesionOrigenId);

    /** Lista todas las sesiones de {@code fecha} ordenadas por {@code horaInicio} ascendente. */
    List<SesionEstudio> listarPorFecha(Fecha fecha);

    /**
     * Lista todas las sesiones entre {@code desde} y {@code hasta} inclusive, ordenadas por
     * inicio ascendente (HU-16 a HU-21).
     */
    List<SesionEstudio> listarPorRango(Fecha desde, Fecha hasta);

    /**
     * Lista todas las sesiones cuyo intervalo planificado se superpone con el rango UTC
     * {@code [desdeUtcMilis, hastaUtcMilis]} inclusive (RN-04, RN-05, RN-31).
     */
    List<SesionEstudio> listarPorRangoUtc(long desdeUtcMilis, long hastaUtcMilis);

    /**
     * Lista sesiones en estado {@code PENDIENTE} desde {@code desde} en adelante, ordenadas por
     * fecha y hora de inicio ascendentes (HU-02).
     */
    List<SesionEstudio> listarPendientesDesde(Fecha desde);

    /** Lista sesiones en estado {@code PENDIENTE} cuya fecha sea estrictamente anterior a {@code hoy}. */
    List<SesionEstudio> listarPendientesAnterioresA(Fecha hoy);

    /** Suma minutos reales de sesiones {@code COMPLETADA} y {@code COMPLETADA_TARDIA} de la materia (HU-09, RN-11). */
    int sumarMinutosRealesPorMateria(long materiaId);

    /** Suma minutos reales contabilizables entre {@code desde} y {@code hasta} inclusive (RN-11, RN-13). */
    int sumarMinutosRealesEnRango(Fecha desde, Fecha hasta);

    /** Elimina permanentemente una sesión por su identificador (HU-15). */
    void eliminarPorId(long id);

    /** Busca la sesión actualmente en estado {@code EN_CURSO}, si existe (HU-22). */
    Optional<SesionEstudio> buscarEnCurso();

    /** Lista todas las sesiones en estado {@code EN_CURSO} (para detección y vencimiento RN-29). */
    List<SesionEstudio> listarEnCurso();

    /**
     * Completa en una única transacción atómica la sesión actual y la sesión invadida repartida
     * (HU-44, RN-28).
     */
    void completarReparto(SesionEstudio primeraCompletada, SesionEstudio segundaCompletada);
}
