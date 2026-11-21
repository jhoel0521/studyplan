package io.github.jhoel0521.studyplan.domain.readmodel;

import java.util.Objects;

/**
 * Datos puros de un recordatorio cuya ventana de disparo ya venció antes de iniciar la sesión
 * tras un reinicio o reactivación de permisos ({@code HU-34}).
 */
public final class AvisoRecordatorioPendiente {

    private final long sesionId;
    private final String nombreMateria;
    private final String horarioLocal;

    public AvisoRecordatorioPendiente(
            long sesionId, String nombreMateria, String horarioLocal) {
        if (sesionId <= 0) {
            throw new IllegalArgumentException("sesionId debe ser positivo");
        }
        this.sesionId = sesionId;
        this.nombreMateria = nombreMateria == null ? "" : nombreMateria;
        this.horarioLocal = horarioLocal == null ? "" : horarioLocal;
    }

    public long sesionId() {
        return sesionId;
    }

    public String nombreMateria() {
        return nombreMateria;
    }

    public String horarioLocal() {
        return horarioLocal;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof AvisoRecordatorioPendiente)) {
            return false;
        }
        AvisoRecordatorioPendiente a = (AvisoRecordatorioPendiente) otro;
        return sesionId == a.sesionId
                && nombreMateria.equals(a.nombreMateria)
                && horarioLocal.equals(a.horarioLocal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sesionId, nombreMateria, horarioLocal);
    }
}
