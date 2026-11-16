package io.github.jhoel0521.studyplan.application.port.out;

import java.util.List;

import io.github.jhoel0521.studyplan.domain.EventoEstudio;

/**
 * Puerto de salida para persistir y consultar eventos de una sesión de estudio (HU-39, RN-22).
 */
public interface EventoEstudioRepository {

    EventoEstudio guardar(EventoEstudio evento);

    List<EventoEstudio> listarPorSesion(long sesionId);

    void eliminarPorSesion(long sesionId);

    void eliminarTodos();
}
