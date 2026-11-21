package io.github.jhoel0521.studyplan.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import io.github.jhoel0521.studyplan.domain.entity.MetaSemanal;
import io.github.jhoel0521.studyplan.domain.readmodel.MetaAlcanzada;
import io.github.jhoel0521.studyplan.domain.readmodel.ProgresoMeta;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;

/**
 * Cálculos puros de progreso de metas semanales e identificación de metas alcanzadas en un tramo
 * ({@code HU-25} a {@code HU-28}, {@code RN-11}, {@code RN-13}, {@code RN-14}, {@code RN-16},
 * {@code RN-31}, {@code RN-33}).
 */
public final class CalculadorProgresoMetas {

    private CalculadorProgresoMetas() {
    }

    public static ProgresoMeta calcularProgreso(
            Fecha semanaInicio,
            Long materiaId,
            String nombreMateria,
            int minutosRealizados,
            double horasObjetivo,
            boolean estimada) {
        return new ProgresoMeta(
                semanaInicio,
                materiaId,
                nombreMateria,
                Math.max(0, minutosRealizados) / 60.0,
                horasObjetivo,
                estimada);
    }

    /** Indica si un incremento de minutos cruzó por primera vez la meta {@code horasObjetivo}. */
    public static boolean cruzoObjetivo(
            int minutosAntes, int minutosDespues, double horasObjetivo) {
        if (horasObjetivo <= 0) {
            return false;
        }
        double objetivoMinutos = horasObjetivo * 60.0;
        return minutosAntes + 1e-6 < objetivoMinutos && minutosDespues + 1e-6 >= objetivoMinutos;
    }

    /**
     * Devuelve las metas no felicitadas previamente cuyo objetivo se alcanzó al pasar de
     * {@code minutosAntes*} a {@code minutosDespues*} ({@code HU-28}, {@code RN-16}).
     */
    public static List<MetaAlcanzada> detectarMetasAlcanzadas(
            List<MetaSemanal> metasSemana,
            Map<Long, Integer> minutosAntesPorMateria,
            Map<Long, Integer> minutosDespuesPorMateria,
            int totalMinutosAntes,
            int totalMinutosDespues,
            Map<Long, String> nombresMateria) {
        List<MetaAlcanzada> alcanzadas = new ArrayList<>();
        if (metasSemana == null || metasSemana.isEmpty()) {
            return alcanzadas;
        }
        for (MetaSemanal meta : metasSemana) {
            if (meta.felicitada()) {
                continue;
            }
            if (meta.esMetaTotal()) {
                if (cruzoObjetivo(totalMinutosAntes, totalMinutosDespues, meta.horasObjetivo())) {
                    alcanzadas.add(new MetaAlcanzada(
                            null, "", meta.horasObjetivo(), totalMinutosDespues / 60.0));
                }
            } else {
                long mId = meta.materiaId();
                int antes = valorDe(minutosAntesPorMateria, mId);
                int despues = valorDe(minutosDespuesPorMateria, mId);
                if (cruzoObjetivo(antes, despues, meta.horasObjetivo())) {
                    String nombre = nombresMateria != null && nombresMateria.containsKey(mId)
                            ? nombresMateria.get(mId)
                            : "";
                    alcanzadas.add(new MetaAlcanzada(
                            mId, nombre, meta.horasObjetivo(), despues / 60.0));
                }
            }
        }
        return alcanzadas;
    }

    private static int valorDe(Map<Long, Integer> mapa, long clave) {
        if (mapa == null) {
            return 0;
        }
        Integer valor = mapa.get(clave);
        return valor == null ? 0 : valor;
    }
}
