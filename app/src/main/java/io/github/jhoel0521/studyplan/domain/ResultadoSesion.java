package io.github.jhoel0521.studyplan.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Resultado de una operación sobre sesiones de estudio que puede concluir con éxito, con un
 * código de validación o con un {@link ConflictoHorario} detallado sin lanzar excepciones
 * (HU-10, HU-11, HU-14, HU-28, RN-05, RN-16, RNF-14).
 */
public final class ResultadoSesion {

    private final SesionEstudio sesion;
    private final ErrorValidacion error;
    private final ConflictoHorario conflicto;
    private final List<MetaAlcanzada> metasAlcanzadas;

    private ResultadoSesion(
            SesionEstudio sesion,
            ErrorValidacion error,
            ConflictoHorario conflicto,
            List<MetaAlcanzada> metasAlcanzadas) {
        this.sesion = sesion;
        this.error = error;
        this.conflicto = conflicto;
        this.metasAlcanzadas = metasAlcanzadas == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(metasAlcanzadas));
    }

    public static ResultadoSesion ok(SesionEstudio sesion) {
        return ok(sesion, Collections.emptyList());
    }

    public static ResultadoSesion ok(SesionEstudio sesion, List<MetaAlcanzada> metasAlcanzadas) {
        return new ResultadoSesion(
                Objects.requireNonNull(sesion, "sesion"), null, null, metasAlcanzadas);
    }

    public static ResultadoSesion error(ErrorValidacion error) {
        return new ResultadoSesion(null, Objects.requireNonNull(error, "error"), null, null);
    }

    public static ResultadoSesion conConflicto(ConflictoHorario conflicto) {
        return new ResultadoSesion(
                null,
                ErrorValidacion.SESION_SOLAPADA,
                Objects.requireNonNull(conflicto, "conflicto"),
                null);
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

    public List<MetaAlcanzada> metasAlcanzadas() {
        return metasAlcanzadas;
    }
}
