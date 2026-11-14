package io.github.jhoel0521.studyplan.domain;

import java.util.Objects;

/**
 * Contrato del patrón State (GoF) para el ciclo de vida de {@link SesionEstudio} según RN-07.
 *
 * <p>Cada implementación concreta conoce únicamente sus transiciones permitidas y capacidades,
 * devolviendo {@link Resultado} sin lanzar excepciones (RNF-14). Implementa RN-07 y soporta
 * HU-10, HU-11, HU-12, HU-13, HU-14 y HU-15.
 */
public interface EstadoSesion {

    /**
     * Hidrata la instancia de estado correspondiente al código persistido.
     */
    static EstadoSesion de(CodigoEstadoSesion codigo) {
        Objects.requireNonNull(codigo, "codigo");
        switch (codigo) {
            case PENDIENTE:
                return EstadoPendiente.INSTANCIA;
            case EN_CURSO:
                return EstadoEnCurso.INSTANCIA;
            case NO_REALIZADA:
                return EstadoNoRealizada.INSTANCIA;
            case COMPLETADA:
                return EstadoCompletada.INSTANCIA;
            case COMPLETADA_TARDIA:
                return EstadoCompletadaTardia.INSTANCIA;
            case REPROGRAMADA:
                return EstadoReprogramada.INSTANCIA;
        }
        throw new AssertionError("Estado no soportado: " + codigo);
    }

    /** Código canónico de este estado. */
    CodigoEstadoSesion codigo();

    /** Indica si es un estado terminal que rechaza toda transición posterior (RN-07). */
    default boolean esFinal() {
        return false;
    }

    /** Indica si las sesiones en este estado aportan su duración real a horas cumplidas (RN-11). */
    default boolean sumaHorasReales() {
        return false;
    }

    /** Indica si la sesión ocupa franja horaria para detección de cruces (RN-05). */
    default boolean ocupaHorario() {
        return true;
    }

    /** Indica si el estudiante puede marcar esta sesión como completada en {@code hoy} (HU-12). */
    default boolean puedeCompletar(Fecha sesionFecha, Fecha hoy) {
        return false;
    }

    /** Indica si admite registro tardío con {@code diasAntiguedad} respecto a hoy (HU-13, RN-10). */
    default boolean puedeRegistrarTardio(int diasAntiguedad) {
        return false;
    }

    /** Indica si puede reprogramarse a otra fecha u horario (HU-14, RN-09). */
    default boolean puedeReprogramar() {
        return false;
    }

    /** Indica si puede eliminarse permanentemente (HU-15, RN-12). */
    default boolean puedeEliminar() {
        return false;
    }

    /** Valida si la eliminación está permitida en el estado actual (RN-12, RNF-14). */
    default Resultado<Boolean> validarEliminacion() {
        return Resultado.error(ErrorValidacion.SESION_NO_ELIMINABLE);
    }

    /** Transición Pendiente → En curso (RN-07). */
    default Resultado<EstadoSesion> iniciar() {
        return Resultado.error(ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA);
    }

    /** Transición En curso → En curso al pausar o reanudar (RN-07). */
    default Resultado<EstadoSesion> pausarReanudar() {
        return Resultado.error(ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA);
    }

    /** Transición En curso → Completada al detener la sesión (RN-07). */
    default Resultado<EstadoSesion> detener(int duracionRealMinutos) {
        return Resultado.error(ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA);
    }

    /** Transición Pendiente → Completada en el mismo día (HU-12, RN-07). */
    default Resultado<EstadoSesion> marcarCompletada(
            Fecha sesionFecha, Fecha hoy, int duracionRealMinutos) {
        return Resultado.error(ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA);
    }

    /** Transición automática Pendiente → No realizada al vencer la fecha (RN-08). */
    default Resultado<EstadoSesion> vencer() {
        return Resultado.error(ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA);
    }

    /** Transición Pendiente o No realizada → Reprogramada (HU-14, RN-09). */
    default Resultado<EstadoSesion> reprogramar() {
        return Resultado.error(ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA);
    }

    /** Transición No realizada → Completada tardía dentro de la ventana de 3 días (HU-13, RN-10). */
    default Resultado<EstadoSesion> registrarTardio(
            int diasAntiguedad, int duracionRealMinutos) {
        return Resultado.error(ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA);
    }

    /** Transición En curso → Completada al cambiar a la siguiente sesión (RN-07, RN-27). */
    default Resultado<EstadoSesion> cambiarASiguiente(int duracionRealMinutos) {
        return Resultado.error(ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA);
    }

    /** Transición En curso → Completada al declarar fin al regresar a la app (RN-07, RN-24). */
    default Resultado<EstadoSesion> declararFinAlVolver(int duracionRealMinutos) {
        return Resultado.error(ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA);
    }

    /** Transición En curso → Completada al vencer el plazo de declaración (RN-07, RN-29). */
    default Resultado<EstadoSesion> cerrarPorPlazoVencido(int duracionRealMinutos) {
        return Resultado.error(ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA);
    }
}
