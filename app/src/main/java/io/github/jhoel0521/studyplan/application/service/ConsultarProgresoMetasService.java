package io.github.jhoel0521.studyplan.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import io.github.jhoel0521.studyplan.application.port.in.ConsultarProgresoMetas;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.MetaSemanalRepository;
import io.github.jhoel0521.studyplan.application.port.out.PerfilRepository;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.TramoDiarioRepository;
import io.github.jhoel0521.studyplan.domain.service.CalculadorProgresoMetas;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.entity.MetaSemanal;
import io.github.jhoel0521.studyplan.domain.entity.Perfil;
import io.github.jhoel0521.studyplan.domain.readmodel.ProgresoMeta;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Implementa {@link ConsultarProgresoMetas}: calcula progreso de la semana actual (creando el
 * snapshot si aún no existe) y el historial semanal ({@code HU-27}, {@code RN-31}, {@code RN-33}).
 */
public final class ConsultarProgresoMetasService implements ConsultarProgresoMetas {

    private final MetaSemanalRepository metas;
    private final PerfilRepository perfiles;
    private final MateriaRepository materias;
    private final TramoDiarioRepository tramos;
    private final Reloj reloj;

    public ConsultarProgresoMetasService(
            MetaSemanalRepository metas,
            PerfilRepository perfiles,
            MateriaRepository materias,
            TramoDiarioRepository tramos,
            Reloj reloj) {
        this.metas = Objects.requireNonNull(metas, "metas");
        this.perfiles = Objects.requireNonNull(perfiles, "perfiles");
        this.materias = Objects.requireNonNull(materias, "materias");
        this.tramos = Objects.requireNonNull(tramos, "tramos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public ProgresoMeta progresoSemanaActualTotal() {
        Fecha lunes = reloj.hoy().lunesDeSemana();
        Fecha domingo = lunes.domingoDeSemana();
        double objetivo = asegurarObjetivoTotalSemana(lunes);
        int minutos = tramos.sumarMinutosEnRango(lunes, domingo);
        return CalculadorProgresoMetas.calcularProgreso(lunes, null, "", minutos, objetivo, false);
    }

    @Override
    public List<ProgresoMeta> historialMetas() {
        Fecha lunesActual = reloj.hoy().lunesDeSemana();
        double metaVigente = asegurarObjetivoTotalSemana(lunesActual);

        List<MetaSemanal> todas = metas.listarTodas();
        Map<Fecha, MetaSemanal> totalesPorSemana = new TreeMap<>(Comparator.reverseOrder());
        Map<Fecha, List<MetaSemanal>> materiasPorSemana = new HashMap<>();
        Fecha semanaMasAntigua = lunesActual;

        for (MetaSemanal meta : todas) {
            Fecha semana = meta.semanaInicio();
            if (semana.iso().compareTo(semanaMasAntigua.iso()) < 0) {
                semanaMasAntigua = semana;
            }
            if (meta.esMetaTotal()) {
                totalesPorSemana.put(semana, meta);
            } else {
                materiasPorSemana.computeIfAbsent(semana, k -> new ArrayList<>()).add(meta);
            }
        }

        Map<Long, String> nombresMateria = new HashMap<>();
        for (Materia m : materias.activas()) {
            if (m.id() != null) {
                nombresMateria.put(m.id(), m.nombre());
            }
        }
        for (Materia m : materias.archivadas()) {
            if (m.id() != null) {
                nombresMateria.put(m.id(), m.nombre());
            }
        }

        List<ProgresoMeta> resultado = new ArrayList<>();
        Fecha cursor = lunesActual;
        while (cursor.iso().compareTo(semanaMasAntigua.iso()) >= 0) {
            Fecha finSemana = cursor.domingoDeSemana();
            MetaSemanal snapshotTotal = totalesPorSemana.get(cursor);
            boolean estimada = snapshotTotal == null;
            double objetivoTotal = snapshotTotal != null ? snapshotTotal.horasObjetivo() : metaVigente;
            int minutosTotal = tramos.sumarMinutosEnRango(cursor, finSemana);
            resultado.add(CalculadorProgresoMetas.calcularProgreso(
                    cursor, null, "", minutosTotal, objetivoTotal, estimada));

            List<MetaSemanal> metasMaterias = materiasPorSemana.get(cursor);
            if (metasMaterias != null && !metasMaterias.isEmpty()) {
                Map<Long, Integer> minutosPorMateria = tramos.sumarPorMateriaEnRango(cursor, finSemana);
                for (MetaSemanal mm : metasMaterias) {
                    long mId = mm.materiaId();
                    int minMateria = minutosPorMateria.getOrDefault(mId, 0);
                    String nombre = nombresMateria.getOrDefault(mId, "");
                    resultado.add(CalculadorProgresoMetas.calcularProgreso(
                            cursor, mId, nombre, minMateria, mm.horasObjetivo(), false));
                }
            }
            cursor = cursor.sumarDias(-7);
        }
        return resultado;
    }

    private double asegurarObjetivoTotalSemana(Fecha lunes) {
        Optional<MetaSemanal> existente = metas.buscar(lunes, null);
        if (existente.isPresent()) {
            return existente.get().horasObjetivo();
        }
        Optional<Perfil> perfilOpt = perfiles.obtener();
        if (!perfilOpt.isPresent()) {
            return 0.0;
        }
        double horasPerfil = perfilOpt.get().metaSemanalHoras();
        Resultado<MetaSemanal> creada = MetaSemanal.crear(lunes, null, horasPerfil);
        if (creada.esValido()) {
            metas.guardar(creada.valor());
        }
        return horasPerfil;
    }
}
