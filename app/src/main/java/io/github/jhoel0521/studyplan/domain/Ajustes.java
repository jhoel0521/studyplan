package io.github.jhoel0521.studyplan.domain;

import java.util.Objects;

/**
 * Ajustes de la aplicación: tema, idioma y si el bloqueo con huella o PIN está activo. Es inmutable: cada
 * cambio devuelve un objeto nuevo.
 *
 * <p>Implementa: HU-03 (tema e idioma) y HU-04 (bloqueo). Los valores por defecto son tema del sistema,
 * español y sin bloqueo; el borrado de datos (HU-38) los restablece porque un almacén vacío devuelve estos valores.
 */
public final class Ajustes {

    private final Tema tema;
    private final Idioma idioma;
    private final boolean bloqueoActivo;
    private final boolean noMolestarGlobal;
    private final ConfiguracionPomodoro pomodoro;

    public Ajustes(Tema tema, Idioma idioma, boolean bloqueoActivo) {
        this(tema, idioma, bloqueoActivo, false, ConfiguracionPomodoro.porDefecto());
    }

    public Ajustes(
            Tema tema,
            Idioma idioma,
            boolean bloqueoActivo,
            boolean noMolestarGlobal,
            ConfiguracionPomodoro pomodoro) {
        this.tema = Objects.requireNonNull(tema);
        this.idioma = Objects.requireNonNull(idioma);
        this.bloqueoActivo = bloqueoActivo;
        this.noMolestarGlobal = noMolestarGlobal;
        this.pomodoro = Objects.requireNonNull(pomodoro);
    }

    /** @return los ajustes que tiene la aplicación antes de que el estudiante cambie algo */
    public static Ajustes porDefecto() {
        return new Ajustes(Tema.SISTEMA, Idioma.ES, false, false, ConfiguracionPomodoro.porDefecto());
    }

    public Tema tema() {
        return tema;
    }

    public Idioma idioma() {
        return idioma;
    }

    public boolean bloqueoActivo() {
        return bloqueoActivo;
    }

    public boolean noMolestarGlobal() {
        return noMolestarGlobal;
    }

    public ConfiguracionPomodoro pomodoro() {
        return pomodoro;
    }

    public Ajustes conTema(Tema nuevo) {
        return new Ajustes(nuevo, idioma, bloqueoActivo, noMolestarGlobal, pomodoro);
    }

    public Ajustes conIdioma(Idioma nuevo) {
        return new Ajustes(tema, nuevo, bloqueoActivo, noMolestarGlobal, pomodoro);
    }

    public Ajustes conBloqueoActivo(boolean activo) {
        return new Ajustes(tema, idioma, activo, noMolestarGlobal, pomodoro);
    }

    public Ajustes conNoMolestarGlobal(boolean activo) {
        return new Ajustes(tema, idioma, bloqueoActivo, activo, pomodoro);
    }

    public Ajustes conPomodoro(ConfiguracionPomodoro nueva) {
        return new Ajustes(tema, idioma, bloqueoActivo, noMolestarGlobal, nueva);
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Ajustes)) {
            return false;
        }
        Ajustes a = (Ajustes) otro;
        return tema == a.tema
                && idioma == a.idioma
                && bloqueoActivo == a.bloqueoActivo
                && noMolestarGlobal == a.noMolestarGlobal
                && Objects.equals(pomodoro, a.pomodoro);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tema, idioma, bloqueoActivo, noMolestarGlobal, pomodoro);
    }

    @Override
    @SuppressWarnings("NullableProblems") // el SDK de Android anota Object.toString y el núcleo no importa androidx
    public String toString() {
        return "Ajustes{tema=" + tema + ", idioma=" + idioma + ", bloqueoActivo=" + bloqueoActivo + '}';
    }
}
