package io.github.jhoel0521.studyplan.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Resultado del cierre real de una sesión de estudio junto con las metas semanales alcanzadas por
 * primera vez en la semana actual ({@code HU-28}, {@code RN-16}).
 */
public final class CierreSesion {

    private final SesionEstudio sesion;
    private final List<MetaAlcanzada> metasAlcanzadas;

    public CierreSesion(SesionEstudio sesion, List<MetaAlcanzada> metasAlcanzadas) {
        this.sesion = Objects.requireNonNull(sesion, "sesion");
        this.metasAlcanzadas = metasAlcanzadas == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(metasAlcanzadas));
    }

    public SesionEstudio sesion() {
        return sesion;
    }

    public List<MetaAlcanzada> metasAlcanzadas() {
        return metasAlcanzadas;
    }
}
