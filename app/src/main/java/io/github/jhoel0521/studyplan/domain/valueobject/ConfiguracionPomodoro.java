package io.github.jhoel0521.studyplan.domain.valueobject;

import java.util.Objects;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Parámetros inmutables de la técnica Pomodoro (HU-23, RN-17, RNF-14).
 */
public final class ConfiguracionPomodoro {

    public static final int TRABAJO_MIN = 1;
    public static final int TRABAJO_MAX = 180;
    public static final int DESCANSO_CORTO_MIN = 1;
    public static final int DESCANSO_CORTO_MAX = 60;
    public static final int DESCANSO_LARGO_MIN = 1;
    public static final int DESCANSO_LARGO_MAX = 90;
    public static final int CICLOS_MIN = 1;
    public static final int CICLOS_MAX = 12;

    public static final int TRABAJO_DEFECTO = 25;
    public static final int DESCANSO_CORTO_DEFECTO = 5;
    public static final int DESCANSO_LARGO_DEFECTO = 15;
    public static final int CICLOS_DEFECTO = 4;

    private final int trabajoMinutos;
    private final int descansoCortoMinutos;
    private final int descansoLargoMinutos;
    private final int ciclosHastaLargo;

    private ConfiguracionPomodoro(
            int trabajoMinutos,
            int descansoCortoMinutos,
            int descansoLargoMinutos,
            int ciclosHastaLargo) {
        this.trabajoMinutos = trabajoMinutos;
        this.descansoCortoMinutos = descansoCortoMinutos;
        this.descansoLargoMinutos = descansoLargoMinutos;
        this.ciclosHastaLargo = ciclosHastaLargo;
    }

    /** Configuración clásica 25 / 5 / 15 cada 4 ciclos (RN-17). */
    public static ConfiguracionPomodoro porDefecto() {
        return new ConfiguracionPomodoro(
                TRABAJO_DEFECTO, DESCANSO_CORTO_DEFECTO, DESCANSO_LARGO_DEFECTO, CICLOS_DEFECTO);
    }

    /** Valida rangos y construye una configuración Pomodoro. */
    public static Resultado<ConfiguracionPomodoro> crear(
            int trabajoMinutos,
            int descansoCortoMinutos,
            int descansoLargoMinutos,
            int ciclosHastaLargo) {
        if (trabajoMinutos < TRABAJO_MIN || trabajoMinutos > TRABAJO_MAX
                || descansoCortoMinutos < DESCANSO_CORTO_MIN
                || descansoCortoMinutos > DESCANSO_CORTO_MAX
                || descansoLargoMinutos < DESCANSO_LARGO_MIN
                || descansoLargoMinutos > DESCANSO_LARGO_MAX
                || ciclosHastaLargo < CICLOS_MIN
                || ciclosHastaLargo > CICLOS_MAX) {
            return Resultado.error(ErrorValidacion.CONFIGURACION_POMODORO_INVALIDA);
        }
        return Resultado.ok(new ConfiguracionPomodoro(
                trabajoMinutos, descansoCortoMinutos, descansoLargoMinutos, ciclosHastaLargo));
    }

    /** Reconstruye desde persistencia aplicando valores por defecto si algún campo es inválido. */
    public static ConfiguracionPomodoro restaurar(
            int trabajoMinutos,
            int descansoCortoMinutos,
            int descansoLargoMinutos,
            int ciclosHastaLargo) {
        Resultado<ConfiguracionPomodoro> r = crear(
                trabajoMinutos, descansoCortoMinutos, descansoLargoMinutos, ciclosHastaLargo);
        return r.esValido() ? r.valor() : porDefecto();
    }

    public int trabajoMinutos() {
        return trabajoMinutos;
    }

    public int descansoCortoMinutos() {
        return descansoCortoMinutos;
    }

    public int descansoLargoMinutos() {
        return descansoLargoMinutos;
    }

    public int ciclosHastaLargo() {
        return ciclosHastaLargo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConfiguracionPomodoro)) {
            return false;
        }
        ConfiguracionPomodoro otra = (ConfiguracionPomodoro) o;
        return trabajoMinutos == otra.trabajoMinutos
                && descansoCortoMinutos == otra.descansoCortoMinutos
                && descansoLargoMinutos == otra.descansoLargoMinutos
                && ciclosHastaLargo == otra.ciclosHastaLargo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                trabajoMinutos, descansoCortoMinutos, descansoLargoMinutos, ciclosHastaLargo);
    }
}
