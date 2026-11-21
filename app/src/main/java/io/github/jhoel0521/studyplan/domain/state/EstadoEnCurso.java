package io.github.jhoel0521.studyplan.domain.state;

import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Estado concreto {@code EN_CURSO} del patrón State para {@link SesionEstudio}
 * (RN-07, RN-24, RN-26, RN-27, RN-29).
 */
public final class EstadoEnCurso implements EstadoSesion {

    public static final EstadoEnCurso INSTANCIA = new EstadoEnCurso();

    private EstadoEnCurso() {
    }

    @Override
    public CodigoEstadoSesion codigo() {
        return CodigoEstadoSesion.EN_CURSO;
    }

    @Override
    public Resultado<EstadoSesion> pausarReanudar() {
        return Resultado.ok(INSTANCIA);
    }

    @Override
    public Resultado<EstadoSesion> detener(int duracionRealMinutos) {
        return completarConDuracion(duracionRealMinutos);
    }

    @Override
    public Resultado<EstadoSesion> cambiarASiguiente(int duracionRealMinutos) {
        return completarConDuracion(duracionRealMinutos);
    }

    @Override
    public Resultado<EstadoSesion> declararFinAlVolver(int duracionRealMinutos) {
        return completarConDuracion(duracionRealMinutos);
    }

    @Override
    public Resultado<EstadoSesion> cerrarPorPlazoVencido(int duracionRealMinutos) {
        return completarConDuracion(duracionRealMinutos);
    }

    private static Resultado<EstadoSesion> completarConDuracion(int duracionRealMinutos) {
        if (duracionRealMinutos < 0) {
            return Resultado.error(ErrorValidacion.DURACION_REAL_INVALIDA);
        }
        return Resultado.ok(EstadoCompletada.INSTANCIA);
    }
}
