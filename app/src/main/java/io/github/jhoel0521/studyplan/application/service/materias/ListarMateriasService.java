package io.github.jhoel0521.studyplan.application.service.materias;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.materias.ListarMaterias;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.MetaSemanalRepository;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.application.port.out.TramoDiarioRepository;
import io.github.jhoel0521.studyplan.domain.service.CalculadorProgresoMetas;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.readmodel.ListadoMaterias;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.entity.MetaSemanal;
import io.github.jhoel0521.studyplan.domain.readmodel.ProgresoMeta;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.readmodel.ResumenMateria;

/**
 * Implementa {@link ListarMaterias}: orden alfabético en español, dejando al final la materia
 * general {@code Estudios varios} ({@code HU-07}, {@code HU-26}, {@code RN-32}, {@code RN-33}).
 */
public final class ListarMateriasService implements ListarMaterias {

    private static final Comparator<Materia> ALFABETICO =
            Comparator.comparing(Materia::esGeneral)
                    .thenComparing(Materia::clave)
                    .thenComparing(Materia::nombre);

    private final MateriaRepository repositorio;
    private final SesionRepository sesiones;
    private final MetaSemanalRepository metas;
    private final TramoDiarioRepository tramos;
    private final Reloj reloj;

    public ListarMateriasService(MateriaRepository repositorio, SesionRepository sesiones) {
        this(repositorio, sesiones, null, null, null);
    }

    public ListarMateriasService(
            MateriaRepository repositorio,
            SesionRepository sesiones,
            MetaSemanalRepository metas,
            TramoDiarioRepository tramos,
            Reloj reloj) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.metas = metas;
        this.tramos = tramos;
        this.reloj = reloj;
    }

    @Override
    public ListadoMaterias ejecutar() {
        List<Materia> lista = new ArrayList<>(repositorio.activas());
        lista.sort(ALFABETICO);

        Fecha lunes = reloj != null ? reloj.hoy().lunesDeSemana() : null;
        Map<Long, Integer> minutosSemanaPorMateria = (tramos != null && lunes != null)
                ? tramos.sumarPorMateriaEnRango(lunes, lunes.domingoDeSemana())
                : Collections.emptyMap();

        List<ResumenMateria> activas = new ArrayList<>(lista.size());
        for (Materia activa : lista) {
            double horas = sesiones.sumarMinutosRealesPorMateria(activa.id()) / 60.0;
            ProgresoMeta progreso = calcularProgresoSemana(activa, lunes, minutosSemanaPorMateria);
            activas.add(new ResumenMateria(activa, horas, progreso));
        }

        List<Materia> archivadas = new ArrayList<>(repositorio.archivadas());
        archivadas.sort(ALFABETICO);
        return new ListadoMaterias(activas, archivadas);
    }

    private ProgresoMeta calcularProgresoSemana(
            Materia materia, Fecha lunes, Map<Long, Integer> minutosSemanaPorMateria) {
        if (lunes == null || materia.id() == null) {
            return null;
        }
        Double objetivoVigente = materia.metaSemanalHoras();
        Optional<MetaSemanal> snapshotOpt = metas != null
                ? metas.buscar(lunes, materia.id())
                : Optional.empty();
        Double objetivo = null;
        if (snapshotOpt.isPresent()) {
            objetivo = snapshotOpt.get().horasObjetivo();
        } else if (objetivoVigente != null) {
            objetivo = objetivoVigente;
            if (metas != null) {
                Resultado<MetaSemanal> creada = MetaSemanal.crear(lunes, materia.id(), objetivoVigente);
                if (creada.esValido()) {
                    metas.guardar(creada.valor());
                }
            }
        }
        if (objetivo == null) {
            return null;
        }
        int minutos = minutosSemanaPorMateria.getOrDefault(materia.id(), 0);
        return CalculadorProgresoMetas.calcularProgreso(
                lunes, materia.id(), materia.nombre(), minutos, objetivo, false);
    }
}
