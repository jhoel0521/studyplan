package io.github.jhoel0521.studyplan.domain;

/**
 * Estado concreto {@code NO_REALIZADA} del patrón State para {@link SesionEstudio}
 * (HU-13, HU-14, RN-07, RN-08, RN-09, RN-10).
 */
public final class EstadoNoRealizada implements EstadoSesion {

    /** Antigüedad máxima en días para pasar a {@code COMPLETADA_TARDIA} (RN-10). */
    public static final int MAX_DIAS_REGISTRO_TARDIO = 3;

    public static final EstadoNoRealizada INSTANCIA = new EstadoNoRealizada();

    private EstadoNoRealizada() {
    }

    @Override
    public CodigoEstadoSesion codigo() {
        return CodigoEstadoSesion.NO_REALIZADA;
    }

    @Override
    public boolean puedeRegistrarTardio(int diasAntiguedad) {
        return diasAntiguedad >= 0 && diasAntiguedad <= MAX_DIAS_REGISTRO_TARDIO;
    }

    @Override
    public boolean puedeReprogramar() {
        return true;
    }

    @Override
    public Resultado<EstadoSesion> registrarTardio(int diasAntiguedad, int duracionRealMinutos) {
        if (!puedeRegistrarTardio(diasAntiguedad)) {
            return Resultado.error(ErrorValidacion.REGISTRO_TARDIO_FUERA_DE_VENTANA);
        }
        if (duracionRealMinutos <= 0) {
            return Resultado.error(ErrorValidacion.DURACION_REAL_INVALIDA);
        }
        return Resultado.ok(EstadoCompletadaTardia.INSTANCIA);
    }

    @Override
    public Resultado<EstadoSesion> reprogramar() {
        return Resultado.ok(EstadoReprogramada.INSTANCIA);
    }
}
