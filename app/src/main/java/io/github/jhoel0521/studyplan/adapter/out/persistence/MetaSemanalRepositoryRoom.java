package io.github.jhoel0521.studyplan.adapter.out.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.out.MetaSemanalRepository;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.entity.MetaSemanal;

/**
 * Adaptador de salida Room para {@link MetaSemanalRepository} ({@code HU-25} a {@code HU-28},
 * {@code RN-33}).
 */
public final class MetaSemanalRepositoryRoom implements MetaSemanalRepository {

    private final MetaSemanalDao dao;

    public MetaSemanalRepositoryRoom(MetaSemanalDao dao) {
        this.dao = Objects.requireNonNull(dao, "dao");
    }

    @Override
    public Optional<MetaSemanal> buscar(Fecha semanaInicio, Long materiaId) {
        String iso = semanaInicio.lunesDeSemana().iso();
        MetaSemanalEntity e = materiaId == null
                ? dao.buscarTotal(iso)
                : dao.buscarPorMateria(iso, materiaId);
        return Optional.ofNullable(e).map(Mapeadores::aDominio);
    }

    @Override
    public List<MetaSemanal> listarDeSemana(Fecha semanaInicio) {
        List<MetaSemanal> lista = new ArrayList<>();
        for (MetaSemanalEntity e : dao.listarDeSemana(semanaInicio.lunesDeSemana().iso())) {
            MetaSemanal m = Mapeadores.aDominio(e);
            if (m != null) {
                lista.add(m);
            }
        }
        return lista;
    }

    @Override
    public List<MetaSemanal> listarTodas() {
        List<MetaSemanal> lista = new ArrayList<>();
        for (MetaSemanalEntity e : dao.listarTodas()) {
            MetaSemanal m = Mapeadores.aDominio(e);
            if (m != null) {
                lista.add(m);
            }
        }
        return lista;
    }

    @Override
    public MetaSemanal guardar(MetaSemanal meta) {
        MetaSemanalEntity e = Mapeadores.aEntidad(meta);
        long id = dao.guardarPorClave(e);
        return meta.conId(id);
    }
}
