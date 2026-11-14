package io.github.jhoel0521.studyplan.domain;

import java.util.Objects;

/**
 * Resultado de una operación sobre sesiones de estudio que puede concluir con éxito, con un
 * código de validación o con un {@link ConflictoHorario} detallado sin lanzar excepciones
 * (HU-10, HU-11, HU-14, RN-05, RNF-14).
 */
public final class ResultadoSesion {

    private final SesionEstudio sesion;
    private final ErrorValidacion error;
    private final ConflictoHorario conflicto;

    private ResultadoSesion(
            SesionEstudio sesion, ErrorValidacion error, ConflictoHorario conflicto) {
        this.sesion = sesion;
        this.error = error;
        this.conflicto = conflicto;
    }

    public static ResultadoSesion ok(SesionEstudio sesion) {
        return new ResultadoSesion(Objects.requireNonNull(sesion, "sesion"), null, null);
    }

    public static ResultadoSesion error(ErrorValidacion error) {
        return new ResultadoSesion(null, Objects.requireNonNull(error, "error"), null);
    }

    public static ResultadoSesion conConflicto(ConflictoHorario conflicto) {
        return new ResultadoSesion(
                null,
                ErrorValidacion.SESION_SOLAPADA,
                Objects.requireNonNull(conflicto, "conflicto"));
    }

    public boolean esValido() {
        return error == null;
    }

    public SesionEstudio sesion() {
        return sesion;
    }

    public ErrorValidacion error() {
        return error;
    }

    public ConflictoHorario conflicto() {
        return conflicto;
    }
}
