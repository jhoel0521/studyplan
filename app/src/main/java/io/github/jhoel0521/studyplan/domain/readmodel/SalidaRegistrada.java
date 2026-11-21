package io.github.jhoel0521.studyplan.domain.readmodel;

import java.util.Objects;

/**
 * Datos de una salida de la app registrada durante una sesión {@code EN_CURSO} fuera de descanso
 * Pomodoro ({@code HU-40}, {@code RN-23}).
 */
public final class SalidaRegistrada {

    private final long sesionId;
    private final String nombreMateria;

    public SalidaRegistrada(long sesionId, String nombreMateria) {
        this.sesionId = sesionId;
        this.nombreMateria = Objects.requireNonNull(nombreMateria, "nombreMateria");
    }

    public long sesionId() {
        return sesionId;
    }

    public String nombreMateria() {
        return nombreMateria;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SalidaRegistrada)) {
            return false;
        }
        SalidaRegistrada that = (SalidaRegistrada) o;
        return sesionId == that.sesionId && nombreMateria.equals(that.nombreMateria);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sesionId, nombreMateria);
    }
}
