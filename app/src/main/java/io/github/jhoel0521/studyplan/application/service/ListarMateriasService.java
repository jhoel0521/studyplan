package io.github.jhoel0521.studyplan.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.github.jhoel0521.studyplan.application.port.in.ListarMaterias;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.domain.ListadoMaterias;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.ResumenMateria;

/**
 * Implementa {@link ListarMaterias}: ordena alfabéticamente por la clave (sin acentos ni
 * mayúsculas) y, a igualdad, por el nombre, acumulando las horas reales de sesiones completadas o
 * completadas tardíamente (HU-09, HU-08, RN-11).
 */
public final class ListarMateriasService implements ListarMaterias {

    private static final Comparator<Materia> ALFABETICO =
            Comparator.comparing(Materia::clave).thenComparing(Materia::nombre);

    private final MateriaRepository repositorio;
    private final SesionRepository sesiones;

    public ListarMateriasService(MateriaRepository repositorio, SesionRepository sesiones) {
        this.repositorio = repositorio;
        this.sesiones = sesiones;
    }

    @Override
    public ListadoMaterias ejecutar() {
        List<Materia> activas = new ArrayList<>(repositorio.activas());
        activas.sort(ALFABETICO);
        List<ResumenMateria> resumenes = new ArrayList<>();
        for (Materia activa : activas) {
            double horas = sesiones == null
                    ? 0.0
                    : sesiones.sumarMinutosRealesPorMateria(activa.id()) / 60.0;
            resumenes.add(new ResumenMateria(activa, horas));
        }
        List<Materia> archivadas = new ArrayList<>(repositorio.archivadas());
        archivadas.sort(ALFABETICO);
        return new ListadoMaterias(resumenes, archivadas);
    }
}
