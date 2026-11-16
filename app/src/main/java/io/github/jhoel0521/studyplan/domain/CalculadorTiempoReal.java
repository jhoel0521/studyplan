package io.github.jhoel0521.studyplan.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Reconstruye intervalos efectivos de estudio y calcula el tiempo real estudiado a partir de los
 * eventos persistidos en {@link EventoEstudio} (HU-22, HU-23, HU-39 a HU-44, RN-11, RN-22, RN-29).
 */
public final class CalculadorTiempoReal {

    private CalculadorTiempoReal() {
    }

    /**
     * Reconstruye la secuencia de intervalos efectivos excluyendo pausas y salidas no declaradas.
     */
    public static List<IntervaloEfectivo> reconstruirIntervalos(
            List<EventoEstudio> eventos, long ahoraMilis) {
        List<IntervaloEfectivo> intervalos = new ArrayList<>();
        if (eventos == null || eventos.isEmpty()) {
            return intervalos;
        }
        List<EventoEstudio> ordenados = ordenar(eventos);
        Long inicioActivo = null;
        Long ultimaSalida = null;

        for (EventoEstudio e : ordenados) {
            long t = e.instanteMilis();
            switch (e.tipo()) {
                case INICIO:
                case REANUDACION:
                case REGRESO_APP:
                    if (inicioActivo == null) {
                        inicioActivo = t;
                    }
                    ultimaSalida = null;
                    break;
                case PAUSA:
                case FIN:
                case CAMBIO_SESION:
                    if (inicioActivo != null) {
                        intervalos.add(new IntervaloEfectivo(inicioActivo, t, false));
                        inicioActivo = null;
                    }
                    ultimaSalida = null;
                    break;
                case SALIDA_APP:
                    if (inicioActivo != null) {
                        intervalos.add(new IntervaloEfectivo(inicioActivo, t, false));
                        inicioActivo = null;
                    }
                    ultimaSalida = t;
                    break;
                case ESTUDIO_APP_CERRADA:
                    if (ultimaSalida != null && t >= ultimaSalida) {
                        intervalos.add(new IntervaloEfectivo(ultimaSalida, t, true));
                    }
                    ultimaSalida = null;
                    break;
                default:
                    break;
            }
        }

        if (inicioActivo != null) {
            intervalos.add(new IntervaloEfectivo(
                    inicioActivo, Math.max(inicioActivo, ahoraMilis), false));
        }
        return intervalos;
    }

    /**
     * Calcula los milisegundos reales contabilizables según la técnica:
     * en {@code LIBRE} suma todos los intervalos efectivos; en {@code POMODORO} excluye descansos
     * dentro de la app (RN-11) y suma íntegro el tramo {@code ESTUDIO_APP_CERRADA} (RN-24).
     */
    public static long calcularMilisEfectivos(
            TecnicaEstudio tecnica,
            ConfiguracionPomodoro pomodoro,
            int duracionPlanificadaMinutos,
            List<EventoEstudio> eventos,
            long ahoraMilis) {
        List<IntervaloEfectivo> intervalos = reconstruirIntervalos(eventos, ahoraMilis);
        long dentroAppMilis = 0L;
        long appCerradaMilis = 0L;
        for (IntervaloEfectivo iv : intervalos) {
            if (iv.esAppCerrada()) {
                appCerradaMilis += iv.duracionMilis();
            } else {
                dentroAppMilis += iv.duracionMilis();
            }
        }
        if (tecnica != TecnicaEstudio.POMODORO) {
            return dentroAppMilis + appCerradaMilis;
        }
        ConfiguracionPomodoro cfg = pomodoro == null ? ConfiguracionPomodoro.porDefecto() : pomodoro;
        int objetivo = CalculadorPomodoro.pomodorosQueCabenEnPlan(duracionPlanificadaMinutos, cfg);
        boolean tieneTiempoExtra = contieneEvento(eventos, TipoEventoEstudio.TIEMPO_EXTRA);
        EstadoPomodoroCalculado estado = CalculadorPomodoro.calcularEstado(
                dentroAppMilis, cfg, objetivo, tieneTiempoExtra);
        return estado.milisTrabajoAcumulados() + appCerradaMilis;
    }

    /** Convierte los milisegundos efectivos a minutos enteros (admitiendo {@code 0}). */
    public static int calcularMinutosReales(
            TecnicaEstudio tecnica,
            ConfiguracionPomodoro pomodoro,
            int duracionPlanificadaMinutos,
            List<EventoEstudio> eventos,
            long ahoraMilis) {
        long milis = calcularMilisEfectivos(
                tecnica, pomodoro, duracionPlanificadaMinutos, eventos, ahoraMilis);
        return (int) (milis / 60_000L);
    }

    /** Suma los milisegundos de intervalos dentro de la app (sin incluir {@code ESTUDIO_APP_CERRADA}). */
    public static long calcularMilisDentroDeApp(List<EventoEstudio> eventos, long ahoraMilis) {
        long total = 0L;
        for (IntervaloEfectivo iv : reconstruirIntervalos(eventos, ahoraMilis)) {
            if (!iv.esAppCerrada()) {
                total += iv.duracionMilis();
            }
        }
        return total;
    }

    /** Indica si la sesión está actualmente pausada por el estudiante. */
    public static boolean estaPausadaActualmente(List<EventoEstudio> eventos) {
        if (eventos == null || eventos.isEmpty()) {
            return false;
        }
        boolean pausada = false;
        for (EventoEstudio e : ordenar(eventos)) {
            switch (e.tipo()) {
                case PAUSA:
                    pausada = true;
                    break;
                case INICIO:
                case REANUDACION:
                case REGRESO_APP:
                case FIN:
                case CAMBIO_SESION:
                    pausada = false;
                    break;
                default:
                    break;
            }
        }
        return pausada;
    }

    /** Indica si existe una salida de la app pendiente de resolver al volver (HU-41). */
    public static boolean tieneSalidaSinResolver(List<EventoEstudio> eventos) {
        return obtenerInstanteSalidaSinResolver(eventos) != null;
    }

    /** Devuelve el instante de la última {@code SALIDA_APP} sin resolver, o {@code null}. */
    public static Long obtenerInstanteSalidaSinResolver(List<EventoEstudio> eventos) {
        if (eventos == null || eventos.isEmpty()) {
            return null;
        }
        Long salidaPendiente = null;
        for (EventoEstudio e : ordenar(eventos)) {
            switch (e.tipo()) {
                case SALIDA_APP:
                    salidaPendiente = e.instanteMilis();
                    break;
                case REGRESO_APP:
                case ESTUDIO_APP_CERRADA:
                case FIN:
                case CAMBIO_SESION:
                    salidaPendiente = null;
                    break;
                default:
                    break;
            }
        }
        return salidaPendiente;
    }

    /**
     * Devuelve el último instante conocido dentro de la app ({@code esMarcaDentroDeApp() == true}),
     * usado ante cierre abrupto ({@code SIGKILL}) o vencimiento tras 3 días (RN-29).
     */
    public static Long obtenerUltimoInstanteDentroDeApp(List<EventoEstudio> eventos) {
        if (eventos == null || eventos.isEmpty()) {
            return null;
        }
        Long ultimo = null;
        for (EventoEstudio e : ordenar(eventos)) {
            if (e.tipo().esMarcaDentroDeApp()) {
                ultimo = e.instanteMilis();
            }
        }
        return ultimo;
    }

    private static boolean contieneEvento(List<EventoEstudio> eventos, TipoEventoEstudio tipo) {
        if (eventos == null) {
            return false;
        }
        for (EventoEstudio e : eventos) {
            if (e.tipo() == tipo) {
                return true;
            }
        }
        return false;
    }

    private static List<EventoEstudio> ordenar(List<EventoEstudio> eventos) {
        List<EventoEstudio> copia = new ArrayList<>(eventos);
        copia.sort(Comparator.comparingLong(EventoEstudio::instanteMilis)
                .thenComparing(e -> e.id() == null ? Long.MAX_VALUE : e.id()));
        return copia;
    }
}
