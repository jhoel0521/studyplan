package io.github.jhoel0521.studyplan.adapter.out.persistence;

import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.out.persistence.SesionesDeMateria;

/**
 * Adaptador real de {@link SesionesDeMateria} respaldado por Room que sustituye al adaptador
 * provisional {@code SinSesionesDeMateria} (HU-08, RN-03).
 */
public final class SesionesDeMateriaRoom implements SesionesDeMateria {

    private final SesionDao dao;

    public SesionesDeMateriaRoom(SesionDao dao) {
        this.dao = Objects.requireNonNull(dao, "dao");
    }

    @Override
    public int contar(long materiaId) {
        return dao.contarPorMateria(materiaId);
    }
}
