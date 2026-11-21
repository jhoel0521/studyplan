package io.github.jhoel0521.studyplan.application.service.comun;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.out.persistence.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.persistence.MetaSemanalRepository;
import io.github.jhoel0521.studyplan.application.port.out.persistence.PerfilRepository;
import io.github.jhoel0521.studyplan.application.port.out.system.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.persistence.TramoDiarioRepository;
import io.github.jhoel0521.studyplan.domain.service.CalculadorProgresoMetas;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.readmodel.MetaAlcanzada;
import io.github.jhoel0521.studyplan.domain.entity.MetaSemanal;
import io.github.jhoel0521.studyplan.domain.entity.Perfil;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Helper de aplicación compartido por todos los cierres reales de sesión para detectar metas
 * semanales cruzadas por primera vez y marcar {@code felicitada = true} ({@code HU-28},
 * {@code RN-16}, {@code RN-33}).
 */
public final class DetectorFelicitacionMetas {

    private DetectorFelicitacionMetas() {
    }

    public static final class InstantaneaAntes {
        final Fecha lunes;
        final Fecha domingo;
        final int totalMinutos;
        final Map<Long, Integer> porMateria;

        private InstantaneaAntes(
                Fecha lunes, Fecha domingo, int totalMinutos, Map<Long, Integer> porMateria) {
            this.lunes = lunes;
            this.domingo = domingo;
            this.totalMinutos = totalMinutos;
            this.porMateria = porMateria;
        }
    }

    public static InstantaneaAntes capturarAntes(
            TramoDiarioRepository tramos, MetaSemanalRepository metas, Reloj reloj) {
        if (tramos == null || metas == null || reloj == null) {
            return null;
        }
        Fecha lunes = reloj.hoy().lunesDeSemana();
        Fecha domingo = lunes.domingoDeSemana();
        int total = tramos.sumarMinutosEnRango(lunes, domingo);
        Map<Long, Integer> porMateria = tramos.sumarPorMateriaEnRango(lunes, domingo);
        return new InstantaneaAntes(lunes, domingo, total, porMateria);
    }

    public static List<MetaAlcanzada> detectarYMarcar(
            InstantaneaAntes antes,
            MetaSemanalRepository metas,
            PerfilRepository perfiles,
            MateriaRepository materias,
            TramoDiarioRepository tramos) {
        if (antes == null || metas == null || tramos == null) {
            return Collections.emptyList();
        }
        asegurarSnapshotsSemana(antes.lunes, metas, perfiles, materias);
        List<MetaSemanal> metasSemana = metas.listarDeSemana(antes.lunes);
        if (metasSemana.isEmpty()) {
            return Collections.emptyList();
        }

        int totalDespues = tramos.sumarMinutosEnRango(antes.lunes, antes.domingo);
        Map<Long, Integer> porMateriaDespues =
                tramos.sumarPorMateriaEnRango(antes.lunes, antes.domingo);

        Map<Long, String> nombres = new HashMap<>();
        if (materias != null) {
            for (Materia m : materias.activas()) {
                if (m.id() != null) {
                    nombres.put(m.id(), m.nombre());
                }
            }
            for (Materia m : materias.archivadas()) {
                if (m.id() != null) {
                    nombres.put(m.id(), m.nombre());
                }
            }
        }

        List<MetaAlcanzada> alcanzadas = CalculadorProgresoMetas.detectarMetasAlcanzadas(
                metasSemana,
                antes.porMateria,
                porMateriaDespues,
                antes.totalMinutos,
                totalDespues,
                nombres);

        for (MetaAlcanzada alcanzada : alcanzadas) {
            for (MetaSemanal meta : metasSemana) {
                boolean coincide = alcanzada.esMetaTotal()
                        ? meta.esMetaTotal()
                        : (!meta.esMetaTotal() && meta.materiaId().equals(alcanzada.materiaId()));
                if (coincide && !meta.felicitada()) {
                    metas.guardar(meta.marcarFelicitada());
                }
            }
        }
        return alcanzadas;
    }

    private static void asegurarSnapshotsSemana(
            Fecha lunes,
            MetaSemanalRepository metas,
            PerfilRepository perfiles,
            MateriaRepository materias) {
        if (perfiles != null && !metas.buscar(lunes, null).isPresent()) {
            Optional<Perfil> perfilOpt = perfiles.obtener();
            if (perfilOpt.isPresent()) {
                Resultado<MetaSemanal> totalRes =
                        MetaSemanal.crear(lunes, null, perfilOpt.get().metaSemanalHoras());
                if (totalRes.esValido()) {
                    metas.guardar(totalRes.valor());
                }
            }
        }
        if (materias != null) {
            for (Materia m : materias.activas()) {
                if (m.id() != null
                        && m.metaSemanalHoras() != null
                        && !metas.buscar(lunes, m.id()).isPresent()) {
                    Resultado<MetaSemanal> matRes =
                            MetaSemanal.crear(lunes, m.id(), m.metaSemanalHoras());
                    if (matRes.esValido()) {
                        metas.guardar(matRes.valor());
                    }
                }
            }
        }
    }
}
