package io.github.jhoel0521.studyplan.domain.valueobject;

import java.util.Locale;
import java.util.Objects;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Value Object inmutable que representa una hora del día en formato de 24 horas ({@code HH:mm}).
 *
 * <p>Implementa validación horaria y cálculo de duración planificada para HU-10, HU-11, HU-14,
 * RN-04 y RN-05.
 */
public final class Hora implements Comparable<Hora> {

    private static final int MINUTOS_POR_DIA = 24 * 60;

    private final int minutosDesdeMedianoche;

    private Hora(int minutosDesdeMedianoche) {
        this.minutosDesdeMedianoche = minutosDesdeMedianoche;
    }

    /** Construye una hora válida entre {@code 00:00} y {@code 23:59}. */
    public static Resultado<Hora> de(int hora, int minuto) {
        if (hora < 0 || hora > 23 || minuto < 0 || minuto > 59) {
            return Resultado.error(ErrorValidacion.HORA_FORMATO_INVALIDO);
        }
        return Resultado.ok(new Hora(hora * 60 + minuto));
    }

    /** Construye una hora a partir de minutos transcurridos desde medianoche ({@code 0..1439}). */
    public static Resultado<Hora> desdeMinutos(int minutosDesdeMedianoche) {
        if (minutosDesdeMedianoche < 0 || minutosDesdeMedianoche >= MINUTOS_POR_DIA) {
            return Resultado.error(ErrorValidacion.HORA_FORMATO_INVALIDO);
        }
        return Resultado.ok(new Hora(minutosDesdeMedianoche));
    }

    /** Parsea una hora {@code HH:mm}, devolviendo {@code HORA_FORMATO_INVALIDO} si está vacía. */
    public static Resultado<Hora> parsear(String texto) {
        return parsear(texto, ErrorValidacion.HORA_FORMATO_INVALIDO);
    }

    /**
     * Parsea una hora {@code HH:mm} indicando el error a emitir cuando el texto está vacío.
     */
    public static Resultado<Hora> parsear(String texto, ErrorValidacion errorSiVacio) {
        if (texto == null) {
            return Resultado.error(errorSiVacio);
        }
        String limpio = texto.trim();
        if (limpio.isEmpty()) {
            return Resultado.error(errorSiVacio);
        }
        if (limpio.length() != 5 || limpio.charAt(2) != ':') {
            return Resultado.error(ErrorValidacion.HORA_FORMATO_INVALIDO);
        }
        int hh = parsearParDigitos(limpio, 0);
        int mm = parsearParDigitos(limpio, 3);
        if (hh < 0 || mm < 0) {
            return Resultado.error(ErrorValidacion.HORA_FORMATO_INVALIDO);
        }
        return de(hh, mm);
    }

    public int hora() {
        return minutosDesdeMedianoche / 60;
    }

    public int minuto() {
        return minutosDesdeMedianoche % 60;
    }

    public int minutosDesdeMedianoche() {
        return minutosDesdeMedianoche;
    }

    /** Formato legible de 24 horas {@code HH:mm}. */
    public String texto() {
        return String.format(Locale.US, "%02d:%02d", hora(), minuto());
    }

    /** Indica si esta hora es estrictamente posterior a {@code otra} (RN-04). */
    public boolean esPosteriorA(Hora otra) {
        return minutosDesdeMedianoche > otra.minutosDesdeMedianoche;
    }

    /** Minutos transcurridos desde esta hora hasta {@code fin}. */
    public int minutosHasta(Hora fin) {
        return fin.minutosDesdeMedianoche - minutosDesdeMedianoche;
    }

    @Override
    public int compareTo(Hora otra) {
        return Integer.compare(minutosDesdeMedianoche, otra.minutosDesdeMedianoche);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Hora)) {
            return false;
        }
        Hora otra = (Hora) o;
        return minutosDesdeMedianoche == otra.minutosDesdeMedianoche;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minutosDesdeMedianoche);
    }

    @Override
    public String toString() {
        return texto();
    }

    private static int parsearParDigitos(String texto, int inicio) {
        char d1 = texto.charAt(inicio);
        char d2 = texto.charAt(inicio + 1);
        if (d1 < '0' || d1 > '9' || d2 < '0' || d2 > '9') {
            return -1;
        }
        return (d1 - '0') * 10 + (d2 - '0');
    }
}
