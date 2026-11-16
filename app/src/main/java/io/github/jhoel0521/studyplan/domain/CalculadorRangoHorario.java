package io.github.jhoel0521.studyplan.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cálculo puro de ventana de scroll inicial (± 2 h sobre 00:00–24:00), reparto de columnas
 * paralelas ante solapamientos visuales y resumen {@code Plan / Real} del periodo visible
 * (HU-17, HU-18, HU-21, RN-05, RN-09, RN-11).
 */
public final class CalculadorRangoHorario {

    public static final int MINUTO_INICIO_DEFECTO = 8 * 60;
    public static final int MARGEN_SCROLL_MINUTOS = 2 * 60;
    public static final int MINUTOS_POR_DIA = 24 * 60;

    private CalculadorRangoHorario() {
    }

    /**
     * Calcula el minuto inicial de desplazamiento sobre la rejilla 00:00–24:00.
     *
     * <p>Si existen sesiones, enfoca desde {@code max(0, min(horaInicio) - 120)}; si no hay
     * sesiones, enfoca a las {@code 08:00} (o {@code max(0, ahora - 120)} si se observa hoy y
     * la hora actual supera las 10:00).
     */
    public static int calcularMinutoScrollInicial(
            List<ItemSesionCalendario> sesiones, boolean incluyeHoy, Hora ahora) {
        if (sesiones != null && !sesiones.isEmpty()) {
            int minInicio = MINUTOS_POR_DIA;
            for (ItemSesionCalendario s : sesiones) {
                int inicio = s.horaInicio().minutosDesdeMedianoche();
                if (inicio < minInicio) {
                    minInicio = inicio;
                }
            }
            return Math.max(0, minInicio - MARGEN_SCROLL_MINUTOS);
        }
        if (incluyeHoy && ahora != null) {
            int sugerido = ahora.minutosDesdeMedianoche() - MARGEN_SCROLL_MINUTOS;
            if (sugerido > MINUTO_INICIO_DEFECTO) {
                return sugerido;
            }
        }
        return MINUTO_INICIO_DEFECTO;
    }

    /**
     * Calcula el límite superior del rango activo ({@code min(1440, max(horaFin) + 120)}).
     */
    public static int calcularMinutoFinRangoActivo(List<ItemSesionCalendario> sesiones) {
        if (sesiones == null || sesiones.isEmpty()) {
            return MINUTOS_POR_DIA;
        }
        int maxFin = 0;
        for (ItemSesionCalendario s : sesiones) {
            int fin = s.horaFin().minutosDesdeMedianoche();
            if (fin > maxFin) {
                maxFin = fin;
            }
        }
        return Math.min(MINUTOS_POR_DIA, maxFin + MARGEN_SCROLL_MINUTOS);
    }

    /**
     * Construye los bloques posicionados para {@code Semana} o {@code Día}, repartiendo en columnas
     * paralelas las sesiones que coincidan visualmente en el mismo día (p. ej. una sesión
     * {@code REPROGRAMADA} junto con la nueva sesión activa en ese horario).
     */
    public static List<BloqueHorarioCalendario> construirBloques(
            List<ItemSesionCalendario> sesiones, VistaCalendario vista) {
        if (sesiones == null || sesiones.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Fecha, List<ItemSesionCalendario>> porDia = new LinkedHashMap<>();
        for (ItemSesionCalendario item : sesiones) {
            porDia.computeIfAbsent(item.fecha(), k -> new ArrayList<>()).add(item);
        }

        Comparator<ItemSesionCalendario> orden = Comparator
                .comparingInt((ItemSesionCalendario s) -> s.horaInicio().minutosDesdeMedianoche())
                .thenComparingInt(s -> s.horaFin().minutosDesdeMedianoche())
                .thenComparingLong(ItemSesionCalendario::sesionId);

        List<BloqueHorarioCalendario> resultado = new ArrayList<>();
        for (List<ItemSesionCalendario> delDia : porDia.values()) {
            delDia.sort(orden);
            repartirDiaEnColumnas(delDia, vista, resultado);
        }
        return resultado;
    }

    /**
     * Suma minutos planificados y realizados del periodo visible:
     * {@code Real} suma {@code duracionRealMinutos} cuando {@code estado.sumaHorasReales()} (RN-11),
     * {@code Plan} suma {@code duracionPlanificadaMinutos} cuando {@code estado.ocupaHorario()}
     * (excluyendo {@code REPROGRAMADA}).
     */
    public static ResumenPeriodoCalendario calcularResumen(List<ItemSesionCalendario> sesiones) {
        if (sesiones == null || sesiones.isEmpty()) {
            return new ResumenPeriodoCalendario(0, 0);
        }
        int plan = 0;
        int real = 0;
        for (ItemSesionCalendario s : sesiones) {
            if (s.estado().ocupaHorario()) {
                plan += Math.max(0, s.duracionPlanificadaMinutos());
            }
            if (s.estado().sumaHorasReales() && s.duracionRealMinutos() != null) {
                real += Math.max(0, s.duracionRealMinutos());
            }
        }
        return new ResumenPeriodoCalendario(plan, real);
    }

    private static void repartirDiaEnColumnas(
            List<ItemSesionCalendario> ordenadas,
            VistaCalendario vista,
            List<BloqueHorarioCalendario> destino) {
        int n = ordenadas.size();
        int i = 0;
        while (i < n) {
            int maxFinCluster = ordenadas.get(i).horaFin().minutosDesdeMedianoche();
            int j = i + 1;
            while (j < n && ordenadas.get(j).horaInicio().minutosDesdeMedianoche() < maxFinCluster) {
                int finJ = ordenadas.get(j).horaFin().minutosDesdeMedianoche();
                if (finJ > maxFinCluster) {
                    maxFinCluster = finJ;
                }
                j++;
            }
            asignarCluster(ordenadas, i, j, vista, destino);
            i = j;
        }
    }

    private static void asignarCluster(
            List<ItemSesionCalendario> ordenadas,
            int inicioCluster,
            int finExclusivoCluster,
            VistaCalendario vista,
            List<BloqueHorarioCalendario> destino) {
        List<Integer> finPorColumna = new ArrayList<>();
        int cantidad = finExclusivoCluster - inicioCluster;
        int[] columnaDeItem = new int[cantidad];

        for (int k = 0; k < cantidad; k++) {
            ItemSesionCalendario item = ordenadas.get(inicioCluster + k);
            int inicio = item.horaInicio().minutosDesdeMedianoche();
            int fin = item.horaFin().minutosDesdeMedianoche();
            int colAsignada = -1;
            for (int c = 0; c < finPorColumna.size(); c++) {
                if (finPorColumna.get(c) <= inicio) {
                    colAsignada = c;
                    finPorColumna.set(c, fin);
                    break;
                }
            }
            if (colAsignada < 0) {
                colAsignada = finPorColumna.size();
                finPorColumna.add(fin);
            }
            columnaDeItem[k] = colAsignada;
        }

        int totalColumnas = finPorColumna.size();
        for (int k = 0; k < cantidad; k++) {
            ItemSesionCalendario item = ordenadas.get(inicioCluster + k);
            int diaCol = vista == VistaCalendario.DIA ? 0 : item.fecha().diaDeSemanaIso() - 1;
            destino.add(new BloqueHorarioCalendario(
                    item,
                    diaCol,
                    item.horaInicio().minutosDesdeMedianoche(),
                    item.horaFin().minutosDesdeMedianoche(),
                    columnaDeItem[k],
                    totalColumnas));
        }
    }
}
