package io.github.jhoel0521.studyplan.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reparte los minutos reales de una sesión entre las fechas locales en que ocurrieron según la
 * regla de negocio {@code RN-31} (HU-02, HU-17, HU-18, HU-21, HU-39).
 *
 * <p>Para sesiones con eventos persistidos corta cada {@link IntervaloEfectivo} en la medianoche
 * local del desfase del evento que abre el tramo. Para sesiones sin eventos (registro tardío,
 * {@code HU-11} y {@code HU-13}), reparte la duración real declarada en proporción a la
 * superposición del intervalo planificado con cada día local.
 */
public final class RepartidorPorDia {

    private RepartidorPorDia() {
    }

    /**
     * Reparte {@code minutosRealesTotales} de {@code sesion} por {@link Fecha} local usando los
     * eventos registrados (si existen) o el intervalo planificado como respaldo.
     */
    public static Map<Fecha, Integer> repartir(
            SesionEstudio sesion,
            List<EventoEstudio> eventos,
            long ahoraMilis,
            int minutosRealesTotales) {
        if (sesion == null || minutosRealesTotales <= 0) {
            return Collections.emptyMap();
        }
        Map<Fecha, Long> milisPorDia = new TreeMap<>();
        List<IntervaloEfectivo> intervalos = eventos == null || eventos.isEmpty()
                ? Collections.emptyList()
                : CalculadorTiempoReal.reconstruirIntervalos(eventos, ahoraMilis);

        if (intervalos.isEmpty() && sesion.planFinUtcMilis() != null) {
            acumularIntervalo(
                    milisPorDia,
                    sesion.planInicioUtcMilis(),
                    sesion.planFinUtcMilis(),
                    sesion.desfaseMinutos());
        } else if (!intervalos.isEmpty()) {
            List<EventoEstudio> ordenados = new ArrayList<>(eventos);
            ordenados.sort(Comparator.comparingLong(EventoEstudio::instanteMilis));
            for (IntervaloEfectivo intervalo : intervalos) {
                int desfase = desfaseParaInicio(
                        ordenados, intervalo.inicioMilis(), sesion.desfaseMinutos());
                acumularIntervalo(
                        milisPorDia, intervalo.inicioMilis(), intervalo.finMilis(), desfase);
            }
        }

        if (milisPorDia.isEmpty()) {
            Map<Fecha, Integer> unico = new TreeMap<>();
            unico.put(sesion.fecha(), minutosRealesTotales);
            return unico;
        }
        return repartirEnteros(milisPorDia, minutosRealesTotales);
    }

    private static int desfaseParaInicio(
            List<EventoEstudio> eventosOrdenados, long inicioMilis, int desfasePorDefecto) {
        int desfase = desfasePorDefecto;
        for (EventoEstudio evento : eventosOrdenados) {
            if (evento.instanteMilis() <= inicioMilis) {
                desfase = evento.desfaseMinutos();
            } else {
                break;
            }
        }
        return desfase;
    }

    private static void acumularIntervalo(
            Map<Fecha, Long> milisPorDia, long inicioMilis, long finMilis, int desfaseMinutos) {
        long cursor = inicioMilis;
        while (cursor < finMilis) {
            Instante inst = Instante.de(cursor, desfaseMinutos);
            Fecha fechaDia = inst.fechaLocal();
            long siguienteMedianoche = Instante.desdeFechaYHoraLocal(
                    fechaDia.sumarDias(1), Hora.de(0, 0).valor(), desfaseMinutos).utcMilis();
            long finTramo = Math.min(finMilis, siguienteMedianoche);
            long duracion = Math.max(0L, finTramo - cursor);
            if (duracion > 0L) {
                Long actual = milisPorDia.get(fechaDia);
                milisPorDia.put(fechaDia, (actual == null ? 0L : actual) + duracion);
            }
            if (finTramo <= cursor) {
                break;
            }
            cursor = finTramo;
        }
    }

    private static Map<Fecha, Integer> repartirEnteros(
            Map<Fecha, Long> pesosPorDia, int totalMinutos) {
        long sumaPesos = 0L;
        for (long peso : pesosPorDia.values()) {
            sumaPesos += peso;
        }
        if (sumaPesos <= 0L) {
            Fecha primero = pesosPorDia.keySet().iterator().next();
            Map<Fecha, Integer> unico = new TreeMap<>();
            unico.put(primero, totalMinutos);
            return unico;
        }

        Map<Fecha, Integer> resultado = new TreeMap<>();
        List<Map.Entry<Fecha, Double>> restos = new ArrayList<>();
        int asignados = 0;

        for (Map.Entry<Fecha, Long> entry : pesosPorDia.entrySet()) {
            double exacto = (entry.getValue() * (double) totalMinutos) / (double) sumaPesos;
            int entero = (int) Math.floor(exacto);
            if (entero > 0) {
                resultado.put(entry.getKey(), entero);
                asignados += entero;
            }
            restos.add(new java.util.AbstractMap.SimpleEntry<>(entry.getKey(), exacto - entero));
        }

        int faltantes = totalMinutos - asignados;
        restos.sort((a, b) -> {
            int cmp = Double.compare(b.getValue(), a.getValue());
            return cmp != 0 ? cmp : a.getKey().compareTo(b.getKey());
        });
        for (int i = 0; i < faltantes && i < restos.size(); i++) {
            Fecha fecha = restos.get(i).getKey();
            Integer prev = resultado.get(fecha);
            resultado.put(fecha, (prev == null ? 0 : prev) + 1);
        }
        return resultado;
    }
}
