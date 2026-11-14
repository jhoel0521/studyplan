package io.github.jhoel0521.studyplan.domain;

/**
 * Estado concreto {@code PENDIENTE} del patrón State para {@link SesionEstudio}
 * (HU-10, HU-12, HU-14, HU-15, RN-07, RN-08, RN-09, RN-12).
 */
public final class EstadoPendiente implements EstadoSesion {

    public static final EstadoPendiente INSTANCIA = new EstadoPendiente();

    private EstadoPendiente() {
    }

    @Override
    public CodigoEstadoSesion codigo() {
        return CodigoEstadoSesion.PENDIENTE;
    }

    @Override
    public boolean puedeCompletar(Fecha sesionFecha, Fecha hoy) {
        return sesionFecha != null && hoy != null && sesionFecha.equals(hoy);
    }

    @Override
    public boolean puedeReprogramar() {
        return true;
    }

    @Override
    public boolean puedeEliminar() {
        return true;
    }

    @Override
    public Resultado<Boolean> validarEliminacion() {
        return Resultado.ok(Boolean.TRUE);
    }

    @Override
    public Resultado<EstadoSesion> iniciar() {
        return Resultado.ok(EstadoEnCurso.INSTANCIA);
    }

    @Override
    public Resultado<EstadoSesion> marcarCompletada(
            Fecha sesionFecha, Fecha hoy, int duracionRealMinutos) {
        if (sesionFecha == null || hoy == null || !sesionFecha.equals(hoy)) {
            return Resultado.error(ErrorValidacion.COMPLETAR_SOLO_MISMO_DIA);
        }
        if (duracionRealMinutos <= 0) {
            return Resultado.error(ErrorValidacion.DURACION_REAL_INVALIDA);
        }
        return Resultado.ok(EstadoCompletada.INSTANCIA);
    }

    @Override
    public Resultado<EstadoSesion> vencer() {
        return Resultado.ok(EstadoNoRealizada.INSTANCIA);
    }

    @Override
    public Resultado<EstadoSesion> reprogramar() {
        return Resultado.ok(EstadoReprogramada.INSTANCIA);
    }
}
