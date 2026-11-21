package io.github.jhoel0521.studyplan.domain.valueobject;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;

/**
 * Criterios inmutables de filtrado para el calendario de estudio (HU-08, HU-19, HU-21).
 */
public final class FiltroCalendario {

    private final Long materiaId;
    private final boolean incluirArchivadas;
    private final Set<CodigoEstadoSesion> estadosVisibles;

    private FiltroCalendario(
            Long materiaId,
            boolean incluirArchivadas,
            Set<CodigoEstadoSesion> estadosVisibles) {
        this.materiaId = materiaId;
        this.incluirArchivadas = incluirArchivadas;
        if (estadosVisibles == null || estadosVisibles.isEmpty()) {
            this.estadosVisibles = Collections.emptySet();
        } else {
            this.estadosVisibles = Collections.unmodifiableSet(EnumSet.copyOf(estadosVisibles));
        }
    }

    /** Filtro por defecto: todas las materias, incluyendo archivadas y todos los estados. */
    public static FiltroCalendario porDefecto() {
        return new FiltroCalendario(null, true, EnumSet.allOf(CodigoEstadoSesion.class));
    }

    /** Construye un filtro con materia opcional, visibilidad de archivadas y estados visibles. */
    public static FiltroCalendario de(
            Long materiaId,
            boolean incluirArchivadas,
            Set<CodigoEstadoSesion> estadosVisibles) {
        return new FiltroCalendario(materiaId, incluirArchivadas, estadosVisibles);
    }

    public Long materiaId() {
        return materiaId;
    }

    public boolean incluirArchivadas() {
        return incluirArchivadas;
    }

    public Set<CodigoEstadoSesion> estadosVisibles() {
        return estadosVisibles;
    }

    /** Indica si existen restricciones activas respecto al filtro por defecto. */
    public boolean tieneRestricciones() {
        return materiaId != null
                || !incluirArchivadas
                || estadosVisibles.size() < CodigoEstadoSesion.values().length;
    }

    public FiltroCalendario conMateria(Long nuevaMateriaId) {
        return new FiltroCalendario(nuevaMateriaId, incluirArchivadas, estadosVisibles);
    }

    public FiltroCalendario conIncluirArchivadas(boolean nuevoIncluirArchivadas) {
        return new FiltroCalendario(materiaId, nuevoIncluirArchivadas, estadosVisibles);
    }

    /** Evalúa si una sesión con su materia archivada cumple el filtro actual. */
    public boolean acepta(long sesionMateriaId, boolean materiaArchivada, CodigoEstadoSesion estado) {
        if (materiaId != null && sesionMateriaId != materiaId) {
            return false;
        }
        if (!incluirArchivadas && materiaArchivada) {
            return false;
        }
        return estado != null && estadosVisibles.contains(estado);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FiltroCalendario)) {
            return false;
        }
        FiltroCalendario otro = (FiltroCalendario) o;
        return incluirArchivadas == otro.incluirArchivadas
                && Objects.equals(materiaId, otro.materiaId)
                && Objects.equals(estadosVisibles, otro.estadosVisibles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(materiaId, incluirArchivadas, estadosVisibles);
    }
}
