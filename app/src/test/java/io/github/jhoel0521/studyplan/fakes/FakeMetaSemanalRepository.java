package io.github.jhoel0521.studyplan.fakes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.out.MetaSemanalRepository;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.entity.MetaSemanal;

/**
 * Repositorio falso en memoria para {@link MetaSemanalRepository}.
 */
public final class FakeMetaSemanalRepository implements MetaSemanalRepository {

    private final List<MetaSemanal> metas = new ArrayList<>();
    private long siguienteId = 1L;

    @Override
    public Optional<MetaSemanal> buscar(Fecha semanaInicio, Long materiaId) {
        Fecha lunes = semanaInicio.lunesDeSemana();
        for (MetaSemanal m : metas) {
            if (m.semanaInicio().equals(lunes) && Objects.equals(m.materiaId(), materiaId)) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<MetaSemanal> listarDeSemana(Fecha semanaInicio) {
        Fecha lunes = semanaInicio.lunesDeSemana();
        List<MetaSemanal> resultado = new ArrayList<>();
        for (MetaSemanal m : metas) {
            if (m.semanaInicio().equals(lunes)) {
                resultado.add(m);
            }
        }
        return resultado;
    }

    @Override
    public List<MetaSemanal> listarTodas() {
        List<MetaSemanal> copia = new ArrayList<>(metas);
        copia.sort(Comparator.comparing(MetaSemanal::semanaInicio).reversed());
        return copia;
    }

    @Override
    public MetaSemanal guardar(MetaSemanal meta) {
        for (int i = 0; i < metas.size(); i++) {
            MetaSemanal actual = metas.get(i);
            if (actual.semanaInicio().equals(meta.semanaInicio())
                    && Objects.equals(actual.materiaId(), meta.materiaId())) {
                MetaSemanal reemplazo = meta.id() == null ? meta.conId(actual.id()) : meta;
                metas.set(i, reemplazo);
                return reemplazo;
            }
        }
        MetaSemanal guardada = meta.id() == null ? meta.conId(siguienteId++) : meta;
        metas.add(guardada);
        return guardada;
    }
}
