package io.github.jhoel0521.studyplan.application.port.out;

import java.util.Map;

import io.github.jhoel0521.studyplan.domain.Fecha;

/**
 * Puerto de salida para persistir y consultar los minutos reales materializados por día local
 * según {@code RN-31} (HU-02, HU-17, HU-18, HU-21).
 */
public interface TramoDiarioRepository {

    /** Reemplaza los tramos diarios asociados a {@code sesionId} con {@code minutosPorDia}. */
    void guardarTramos(long sesionId, Map<Fecha, Integer> minutosPorDia);

    /** Suma todos los minutos reales estudiados entre {@code desde} y {@code hasta} inclusive. */
    int sumarMinutosEnRango(Fecha desde, Fecha hasta);

    /** Devuelve los minutos totales estudiados por cada fecha dentro de {@code [desde, hasta]}. */
    Map<Fecha, Integer> sumarPorDiaEnRango(Fecha desde, Fecha hasta);

    /** Elimina los tramos diarios asociados a {@code sesionId}. */
    void eliminarPorSesion(long sesionId);
}
