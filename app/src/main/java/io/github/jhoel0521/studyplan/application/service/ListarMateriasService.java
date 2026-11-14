package io.github.jhoel0521.studyplan.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.github.jhoel0521.studyplan.application.port.in.ListarMaterias;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.domain.ListadoMaterias;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.ResumenMateria;

/**
 * Implementa {@link ListarMaterias}: ordena alfabéticamente por la clave (sin acentos ni mayúsculas) y, a igualdad,
 * por el nombre. Las horas realizadas son cero hasta que existan sesiones.
 *
 * <p>Implementa: HU-09 y HU-08.
 */
public final class ListarMateriasService implements ListarMaterias {

    /** Horas realizadas mientras no existan sesiones (las completa el change de sesiones). */
    static final double HORAS_SIN_SESIONES = 0;

    private static final Comparator<Materia> ALFABETICO =
            Comparator.comparing(Materia::clave).thenComparing(Materia::nombre);

    private final MateriaRepository repositorio;

    public ListarMateriasService(MateriaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public ListadoMaterias ejecutar() {
        List<Materia> activas = new ArrayList<>(repositorio.activas());
        activas.sort(ALFABETICO);
        List<ResumenMateria> resumenes = new ArrayList<>();
        for (Materia activa : activas) {
            resumenes.add(new ResumenMateria(activa, HORAS_SIN_SESIONES));
        }
        List<Materia> archivadas = new ArrayList<>(repositorio.archivadas());
        archivadas.sort(ALFABETICO);
        return new ListadoMaterias(resumenes, archivadas);
    }
}
