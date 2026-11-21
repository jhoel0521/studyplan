package io.github.jhoel0521.studyplan.domain.valueobject;

import java.util.Locale;
import java.util.Objects;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Value Object inmutable que representa una fecha calendario ISO ({@code YYYY-MM-DD}) en Java puro.
 *
 * <p>Implementa soporte temporal para HU-10, HU-11, HU-12, HU-13, HU-14, RN-06, RN-08, RN-10 y RN-13
 * mediante aritmética entera del calendario gregoriano proléptico sin depender de {@code java.time},
 * preservando compatibilidad nativa con {@code minSdk 24}.
 */
public final class Fecha implements Comparable<Fecha> {

    private static final int ANIO_MINIMO = 1;
    private static final int ANIO_MAXIMO = 9999;

    private final int anio;
    private final int mes;
    private final int dia;
    private final long diasEpoca;

    private Fecha(int anio, int mes, int dia, long diasEpoca) {
        this.anio = anio;
        this.mes = mes;
        this.dia = dia;
        this.diasEpoca = diasEpoca;
    }

    /**
     * Construye una fecha validando año (1..9999), mes (1..12) y día según el mes y años bisiestos.
     */
    public static Resultado<Fecha> de(int anio, int mes, int dia) {
        if (anio < ANIO_MINIMO || anio > ANIO_MAXIMO || mes < 1 || mes > 12) {
            return Resultado.error(ErrorValidacion.FECHA_SESION_FORMATO_INVALIDO);
        }
        int maxDia = diasDelMes(anio, mes);
        if (dia < 1 || dia > maxDia) {
            return Resultado.error(ErrorValidacion.FECHA_SESION_FORMATO_INVALIDO);
        }
        return Resultado.ok(new Fecha(anio, mes, dia, calcularDiasDesdeEpoca(anio, mes, dia)));
    }

    /**
     * Parsea una fecha en formato ISO estricto {@code YYYY-MM-DD}.
     */
    public static Resultado<Fecha> parsear(String texto) {
        if (texto == null) {
            return Resultado.error(ErrorValidacion.FECHA_SESION_OBLIGATORIA);
        }
        String limpio = texto.trim();
        if (limpio.isEmpty()) {
            return Resultado.error(ErrorValidacion.FECHA_SESION_OBLIGATORIA);
        }
        if (limpio.length() != 10 || limpio.charAt(4) != '-' || limpio.charAt(7) != '-') {
            return Resultado.error(ErrorValidacion.FECHA_SESION_FORMATO_INVALIDO);
        }
        int anio = parsearDigitos(limpio, 0, 4);
        int mes = parsearDigitos(limpio, 5, 7);
        int dia = parsearDigitos(limpio, 8, 10);
        if (anio < 0 || mes < 0 || dia < 0) {
            return Resultado.error(ErrorValidacion.FECHA_SESION_FORMATO_INVALIDO);
        }
        return de(anio, mes, dia);
    }

    /** Indica si un año es bisiesto en el calendario gregoriano. */
    public static boolean esBisiesto(int anio) {
        return (anio % 4 == 0 && anio % 100 != 0) || (anio % 400 == 0);
    }

    /** Cantidad de días que tiene {@code mes} (1..12) en el año indicado. */
    public static int diasDelMes(int anio, int mes) {
        switch (mes) {
            case 2:
                return esBisiesto(anio) ? 29 : 28;
            case 4:
            case 6:
            case 9:
            case 11:
                return 30;
            default:
                return 31;
        }
    }

    public int anio() {
        return anio;
    }

    public int mes() {
        return mes;
    }

    public int dia() {
        return dia;
    }

    /** Días exactos respecto a la época Unix ({@code 1970-01-01 = 0}). */
    public long diasDesdeEpoca() {
        return diasEpoca;
    }

    /** Representación canónica ISO {@code YYYY-MM-DD}. */
    public String iso() {
        return String.format(Locale.US, "%04d-%02d-%02d", anio, mes, dia);
    }

    public boolean esAnteriorA(Fecha otra) {
        return diasEpoca < otra.diasEpoca;
    }

    public boolean esPosteriorA(Fecha otra) {
        return diasEpoca > otra.diasEpoca;
    }

    /**
     * Días exactos desde esta fecha hasta {@code otra} (positivo si {@code otra} es posterior).
     */
    public int diasHasta(Fecha otra) {
        return (int) (otra.diasEpoca - diasEpoca);
    }

    /** Devuelve una nueva fecha desplazada {@code dias} días (positivos o negativos). */
    public Fecha sumarDias(int dias) {
        return desdeDiasEpoca(diasEpoca + dias);
    }

    /**
     * Día de la semana ISO-8601 (1 = lunes .. 7 = domingo), acorde con RN-13.
     */
    public int diaDeSemanaIso() {
        return (int) Math.floorMod(diasEpoca + 3L, 7L) + 1;
    }

    /** Lunes de la semana calendario (lunes a domingo) que contiene esta fecha (RN-13). */
    public Fecha lunesDeSemana() {
        return sumarDias(-(diaDeSemanaIso() - 1));
    }

    /** Domingo de la semana calendario (lunes a domingo) que contiene esta fecha (RN-13). */
    public Fecha domingoDeSemana() {
        return lunesDeSemana().sumarDias(6);
    }

    @Override
    public int compareTo(Fecha otra) {
        return Long.compare(diasEpoca, otra.diasEpoca);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Fecha)) {
            return false;
        }
        Fecha otra = (Fecha) o;
        return diasEpoca == otra.diasEpoca;
    }

    @Override
    public int hashCode() {
        return Objects.hash(diasEpoca);
    }

    @Override
    public String toString() {
        return iso();
    }

    private static int parsearDigitos(String texto, int inicio, int finExclusivo) {
        int acumulado = 0;
        for (int i = inicio; i < finExclusivo; i++) {
            char c = texto.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            acumulado = acumulado * 10 + (c - '0');
        }
        return acumulado;
    }

    // Algoritmo gregoriano proléptico de Howard Hinnant respecto a 1970-01-01 = 0.
    private static long calcularDiasDesdeEpoca(int y, int m, int d) {
        long yr = y - (m <= 2 ? 1L : 0L);
        long era = (yr >= 0 ? yr : yr - 399L) / 400L;
        long yoe = yr - era * 400L;
        long doy = (153L * (m + (m > 2 ? -3 : 9)) + 2L) / 5L + d - 1L;
        long doe = yoe * 365L + yoe / 4L - yoe / 100L + doy;
        return era * 146097L + doe - 719468L;
    }

    /** Construye una fecha a partir de los días respecto a {@code 1970-01-01 = 0}. */
    public static Fecha desdeDiasEpoca(long diasEpoca) {
        long z = diasEpoca + 719468L;
        long era = (z >= 0 ? z : z - 146096L) / 146097L;
        long doe = z - era * 146097L;
        long yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L;
        long y = yoe + era * 400L;
        long doy = doe - (365L * yoe + yoe / 4L - yoe / 100L);
        long mp = (5L * doy + 2L) / 153L;
        int d = (int) (doy - (153L * mp + 2L) / 5L + 1L);
        int m = (int) (mp + (mp < 10 ? 3 : -9));
        int yr = (int) (y + (m <= 2 ? 1 : 0));
        return new Fecha(yr, m, d, diasEpoca);
    }
}
