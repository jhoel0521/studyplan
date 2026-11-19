package io.github.jhoel0521.studyplan.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Calculador puro de estadísticas de estudio para un periodo ({@code HU-29}, {@code HU-30},
 * {@code HU-31}, {@code HU-45}, {@code RN-11}, {@code RN-30}, {@code RN-31}) y base agregada para
 * reportes ({@code HU-32}).
 */
public final class CalculadorEstadisticas {

    private CalculadorEstadisticas() {
    }

    public static EstadisticasPeriodo calcular(
            PeriodoEstadisticas periodo,
            int minutosTotalesActual,
            int minutosTotalesAnterior,
            Map<Long, Integer> minutosPorMateria,
            Map<Long, Materia> catalogoMaterias,
            List<SesionEstudio> sesionesDelPeriodo,
            Map<Long, List<EventoEstudio>> eventosPorSesion,
            long ahoraMilis) {
        Objects.requireNonNull(periodo, "periodo");

        Double variacion = minutosTotalesAnterior <= 0
                ? null
                : (minutosTotalesActual - minutosTotalesAnterior) * 100.0 / minutosTotalesAnterior;

        Map<Long, Integer> planificadosPorMateria = new HashMap<>();
        Map<Long, Integer> noRealizadasPorMateria = new HashMap<>();
        int completadas = 0;
        int tardias = 0;
        int noRealizadas = 0;
        int reprogramadas = 0;
        int pendientesOVigentes = 0;

        if (sesionesDelPeriodo != null) {
            for (SesionEstudio s : sesionesDelPeriodo) {
                switch (s.codigoEstado()) {
                    case COMPLETADA:
                        completadas++;
                        break;
                    case COMPLETADA_TARDIA:
                        tardias++;
                        break;
                    case NO_REALIZADA:
                        noRealizadas++;
                        noRealizadasPorMateria.put(
                                s.materiaId(),
                                noRealizadasPorMateria.getOrDefault(s.materiaId(), 0) + 1);
                        break;
                    case REPROGRAMADA:
                        reprogramadas++;
                        break;
                    case PENDIENTE:
                    case EN_CURSO:
                        pendientesOVigentes++;
                        break;
                }
                if (s.estado().ocupaHorario() && s.tienePlan()) {
                    planificadosPorMateria.put(
                            s.materiaId(),
                            planificadosPorMateria.getOrDefault(s.materiaId(), 0)
                                    + Math.max(0, s.duracionPlanificadaMinutos()));
                }
            }
        }

        int totalCerradas = completadas + tardias + noRealizadas + reprogramadas;
        Double tasaCumplimiento = totalCerradas == 0
                ? null
                : (completadas + tardias) * 100.0 / totalCerradas;

        List<DesgloseMateriaPeriodo> distribucion = construirDistribucion(
                minutosTotalesActual,
                minutosPorMateria,
                catalogoMaterias,
                planificadosPorMateria,
                noRealizadasPorMateria);

        ConcentracionPeriodo concentracion = calcularConcentracion(
                sesionesDelPeriodo, eventosPorSesion, ahoraMilis);

        return new EstadisticasPeriodo(
                periodo,
                minutosTotalesActual,
                minutosTotalesAnterior,
                variacion,
                distribucion,
                completadas,
                tardias,
                noRealizadas,
                reprogramadas,
                pendientesOVigentes,
                tasaCumplimiento,
                concentracion);
    }

    /**
     * Construye el reporte de estudio de un periodo ({@code HU-32}, {@code RN-31}) con la unión de
     * materias que registran minutos reales, horas planificadas dentro del rango o sesiones
     * {@code NO_REALIZADA} iniciadas en el periodo.
     */
    public static ReporteEstudio calcularReporte(
            PeriodoEstadisticas periodo,
            long generadoEnMilis,
            Map<Long, Integer> minutosPorMateria,
            Map<Long, Materia> catalogoMaterias,
            List<SesionEstudio> sesionesDelPeriodo) {
        Objects.requireNonNull(periodo, "periodo");

        Map<Long, Integer> realesPorMateria = copiarPositivos(minutosPorMateria);
        Map<Long, Integer> planificadosPorMateria = new HashMap<>();
        Map<Long, Integer> noRealizadasPorMateria = new HashMap<>();
        acumularSesionesReporte(
                periodo, sesionesDelPeriodo, planificadosPorMateria, noRealizadasPorMateria);

        int totalReales = sumarValores(realesPorMateria);
        int totalPlanificados = 0;
        int totalNoRealizadas = 0;

        Set<Long> ids = unirClavesPositivas(
                realesPorMateria, planificadosPorMateria, noRealizadasPorMateria);

        List<DesgloseMateriaPeriodo> filas = new ArrayList<>();
        for (Long id : ids) {
            int minReal = realesPorMateria.getOrDefault(id, 0);
            int minPlan = planificadosPorMateria.getOrDefault(id, 0);
            int noReal = noRealizadasPorMateria.getOrDefault(id, 0);
            if (minReal <= 0 && minPlan <= 0 && noReal <= 0) {
                continue;
            }
            Materia materia = catalogoMaterias != null ? catalogoMaterias.get(id) : null;
            String nombre = materia != null ? materia.nombre() : "";
            boolean esGeneral = materia != null && materia.esGeneral();
            int color = materia != null ? materia.color() : 0xFF607D8B;
            boolean archivada = materia != null && materia.archivada();
            double pct = totalReales <= 0 ? 0.0 : minReal * 100.0 / totalReales;

            totalPlanificados += minPlan;
            totalNoRealizadas += noReal;

            filas.add(new DesgloseMateriaPeriodo(
                    id, nombre, esGeneral, color, archivada, minReal, pct, minPlan, noReal));
        }

        filas.sort(
                Comparator.comparingInt(DesgloseMateriaPeriodo::minutosReales)
                        .reversed()
                        .thenComparing(
                                Comparator.comparingInt(
                                                DesgloseMateriaPeriodo::minutosPlanificadosDeSesiones)
                                        .reversed())
                        .thenComparing(
                                DesgloseMateriaPeriodo::nombreMateria,
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparingLong(DesgloseMateriaPeriodo::materiaId));

        return new ReporteEstudio(
                periodo,
                generadoEnMilis,
                filas,
                totalPlanificados,
                totalReales,
                totalNoRealizadas);
    }

    private static Map<Long, Integer> copiarPositivos(Map<Long, Integer> origen) {
        Map<Long, Integer> destino = new HashMap<>();
        if (origen == null) {
            return destino;
        }
        for (Map.Entry<Long, Integer> entrada : origen.entrySet()) {
            if (entrada.getKey() != null && entrada.getValue() != null && entrada.getValue() > 0) {
                destino.put(entrada.getKey(), entrada.getValue());
            }
        }
        return destino;
    }

    private static void acumularSesionesReporte(
            PeriodoEstadisticas periodo,
            List<SesionEstudio> sesiones,
            Map<Long, Integer> planificadosPorMateria,
            Map<Long, Integer> noRealizadasPorMateria) {
        if (sesiones == null) {
            return;
        }
        Fecha inicio = periodo.inicio();
        Fecha fin = periodo.fin();
        for (SesionEstudio sesion : sesiones) {
            if (sesion == null) {
                continue;
            }
            if (sesion.estado().ocupaHorario() && sesion.tienePlan()) {
                int minPlan = sesion.minutosPlanificadosEnRango(inicio, fin);
                if (minPlan > 0) {
                    planificadosPorMateria.put(
                            sesion.materiaId(),
                            planificadosPorMateria.getOrDefault(sesion.materiaId(), 0) + minPlan);
                }
            }
            if (sesion.codigoEstado() == CodigoEstadoSesion.NO_REALIZADA
                    && !sesion.fecha().esAnteriorA(inicio)
                    && !sesion.fecha().esPosteriorA(fin)) {
                noRealizadasPorMateria.put(
                        sesion.materiaId(),
                        noRealizadasPorMateria.getOrDefault(sesion.materiaId(), 0) + 1);
            }
        }
    }

    private static int sumarValores(Map<Long, Integer> mapa) {
        int total = 0;
        for (int valor : mapa.values()) {
            total += Math.max(0, valor);
        }
        return total;
    }

    private static Set<Long> unirClavesPositivas(
            Map<Long, Integer> reales,
            Map<Long, Integer> planificados,
            Map<Long, Integer> noRealizadas) {
        Set<Long> ids = new LinkedHashSet<>();
        agregarClavesPositivas(ids, reales);
        agregarClavesPositivas(ids, planificados);
        agregarClavesPositivas(ids, noRealizadas);
        return ids;
    }

    private static void agregarClavesPositivas(Set<Long> destino, Map<Long, Integer> fuente) {
        for (Map.Entry<Long, Integer> entrada : fuente.entrySet()) {
            if (entrada.getValue() != null && entrada.getValue() > 0) {
                destino.add(entrada.getKey());
            }
        }
    }

    private static List<DesgloseMateriaPeriodo> construirDistribucion(
            int minutosTotalesActual,
            Map<Long, Integer> minutosPorMateria,
            Map<Long, Materia> catalogoMaterias,
            Map<Long, Integer> planificadosPorMateria,
            Map<Long, Integer> noRealizadasPorMateria) {
        Set<Long> ids = new LinkedHashSet<>();
        if (minutosPorMateria != null) {
            for (Map.Entry<Long, Integer> entry : minutosPorMateria.entrySet()) {
                if (entry.getValue() != null && entry.getValue() > 0) {
                    ids.add(entry.getKey());
                }
            }
        }
        List<DesgloseMateriaPeriodo> resultado = new ArrayList<>();
        for (Long id : ids) {
            int min = minutosPorMateria.getOrDefault(id, 0);
            if (min <= 0) {
                continue;
            }
            Materia m = catalogoMaterias != null ? catalogoMaterias.get(id) : null;
            String nombre = m != null ? m.nombre() : "";
            boolean esGeneral = m != null && m.esGeneral();
            int color = m != null ? m.color() : 0xFF607D8B;
            boolean archivada = m != null && m.archivada();
            double pct = minutosTotalesActual <= 0 ? 0.0 : min * 100.0 / minutosTotalesActual;
            resultado.add(new DesgloseMateriaPeriodo(
                    id,
                    nombre,
                    esGeneral,
                    color,
                    archivada,
                    min,
                    pct,
                    planificadosPorMateria.getOrDefault(id, 0),
                    noRealizadasPorMateria.getOrDefault(id, 0)));
        }
        resultado.sort(Comparator.comparingInt(DesgloseMateriaPeriodo::minutosReales).reversed()
                .thenComparing(DesgloseMateriaPeriodo::nombreMateria));
        return resultado;
    }

    private static ConcentracionPeriodo calcularConcentracion(
            List<SesionEstudio> sesiones,
            Map<Long, List<EventoEstudio>> eventosPorSesion,
            long ahoraMilis) {
        long dentroAppMs = 0L;
        long pausasManualesMs = 0L;
        long tiempoFueraNoAcademicoMs = 0L;
        int totalInterrupciones = 0;

        double sumaPuntualidadMin = 0.0;
        int sesionesPuntualidad = 0;

        long sumaRealRespetoMin = 0L;
        long sumaPlanRespetoMin = 0L;

        Map<TopicoEventoEstudio, Long> estudioExternoMs = new EnumMap<>(TopicoEventoEstudio.class);
        Map<TopicoEventoEstudio, Integer> estudioExternoConteo =
                new EnumMap<>(TopicoEventoEstudio.class);
        Map<TopicoEventoEstudio, Long> interrupcionesMs = new EnumMap<>(TopicoEventoEstudio.class);
        Map<TopicoEventoEstudio, Integer> interrupcionesConteo =
                new EnumMap<>(TopicoEventoEstudio.class);

        if (sesiones != null) {
            for (SesionEstudio s : sesiones) {
                if (s.codigoEstado() == CodigoEstadoSesion.COMPLETADA_TARDIA) {
                    continue;
                }
                List<EventoEstudio> eventos = (s.id() != null && eventosPorSesion != null)
                        ? eventosPorSesion.getOrDefault(s.id(), Collections.emptyList())
                        : Collections.emptyList();
                if (eventos.isEmpty()) {
                    continue;
                }

                CalculadorTiempoReal.AnalisisEventos analisis =
                        CalculadorTiempoReal.analizarEventos(eventos, ahoraMilis);
                if (analisis.primerInicioMilis() == null) {
                    continue;
                }

                dentroAppMs += CalculadorTiempoReal.calcularMilisDentroDeApp(eventos, ahoraMilis);
                pausasManualesMs += analisis.pausasManualesMs();
                tiempoFueraNoAcademicoMs += analisis.salidasNoAcademicasMs();
                totalInterrupciones +=
                        analisis.cantidadPausasManuales() + analisis.cantidadSalidasNoAcademicas();

                for (Map.Entry<TopicoEventoEstudio, Long> e :
                        analisis.estudioExternoPorTopicoMs().entrySet()) {
                    estudioExternoMs.put(
                            e.getKey(),
                            estudioExternoMs.getOrDefault(e.getKey(), 0L) + e.getValue());
                    estudioExternoConteo.put(
                            e.getKey(),
                            estudioExternoConteo.getOrDefault(e.getKey(), 0) + 1);
                }

                for (Map.Entry<TopicoEventoEstudio, Long> e :
                        analisis.interrupcionesPorTopicoMs().entrySet()) {
                    interrupcionesMs.put(
                            e.getKey(),
                            interrupcionesMs.getOrDefault(e.getKey(), 0L) + e.getValue());
                }
                for (Map.Entry<TopicoEventoEstudio, Integer> e :
                        analisis.interrupcionesPorTopicoConteo().entrySet()) {
                    interrupcionesConteo.put(
                            e.getKey(),
                            interrupcionesConteo.getOrDefault(e.getKey(), 0) + e.getValue());
                }

                if (s.tienePlan() && s.duracionPlanificadaMinutos() > 0) {
                    sumaPuntualidadMin +=
                            (analisis.primerInicioMilis() - s.planInicioUtcMilis()) / 60_000.0;
                    sesionesPuntualidad++;

                    if (s.codigoEstado() == CodigoEstadoSesion.COMPLETADA
                            && s.duracionRealMinutos() != null) {
                        sumaPlanRespetoMin += s.duracionPlanificadaMinutos();
                        sumaRealRespetoMin += Math.max(0, s.duracionRealMinutos());
                    }
                }
            }
        }

        long denominadorConcentracion = dentroAppMs + pausasManualesMs;
        Double indiceConcentracion = denominadorConcentracion <= 0L
                ? null
                : dentroAppMs * 100.0 / denominadorConcentracion;

        double interrupcionesPorHora = dentroAppMs <= 0L
                ? 0.0
                : totalInterrupciones / (dentroAppMs / 3_600_000.0);

        Double puntualidadPromedio = sesionesPuntualidad == 0
                ? null
                : sumaPuntualidadMin / sesionesPuntualidad;

        Double respetoDelTiempo = sumaPlanRespetoMin <= 0L
                ? null
                : sumaRealRespetoMin * 100.0 / sumaPlanRespetoMin;

        int tiempoFueraMin = (int) (tiempoFueraNoAcademicoMs / 60_000L);
        long totalExternoMs = 0L;
        for (long ms : estudioExternoMs.values()) {
            totalExternoMs += ms;
        }
        int estudioFueraMin = (int) (totalExternoMs / 60_000L);
        int sinClasificarMin = (int) (estudioExternoMs.getOrDefault(
                TopicoEventoEstudio.SIN_CLASIFICAR, 0L) / 60_000L);

        List<ResumenTopico> listaExterno = convertirTopicos(estudioExternoMs, estudioExternoConteo);
        List<ResumenTopico> listaInterrupciones =
                convertirTopicos(interrupcionesMs, interrupcionesConteo);

        return new ConcentracionPeriodo(
                indiceConcentracion,
                interrupcionesPorHora,
                totalInterrupciones,
                puntualidadPromedio,
                respetoDelTiempo,
                tiempoFueraMin,
                estudioFueraMin,
                sinClasificarMin,
                listaExterno,
                listaInterrupciones);
    }

    private static List<ResumenTopico> convertirTopicos(
            Map<TopicoEventoEstudio, Long> duracionesMs,
            Map<TopicoEventoEstudio, Integer> conteos) {
        List<ResumenTopico> lista = new ArrayList<>();
        for (Map.Entry<TopicoEventoEstudio, Long> e : duracionesMs.entrySet()) {
            int cant = conteos.getOrDefault(e.getKey(), 0);
            int min = (int) (e.getValue() / 60_000L);
            if (cant > 0 || min > 0) {
                lista.add(new ResumenTopico(e.getKey(), cant, min));
            }
        }
        lista.sort(Comparator.comparingInt(ResumenTopico::minutos).reversed()
                .thenComparingInt(ResumenTopico::cantidad).reversed()
                .thenComparing(r -> r.topico().name()));
        return lista;
    }
}
