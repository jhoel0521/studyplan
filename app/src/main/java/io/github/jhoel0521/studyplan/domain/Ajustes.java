package io.github.jhoel0521.studyplan.domain;

import java.util.Objects;

/**
 * Ajustes de la aplicación: tema, idioma, bloqueo con huella o PIN, técnicas de estudio y
 * antelación predeterminada de recordatorios. Es inmutable: cada cambio devuelve un objeto nuevo.
 *
 * <p>Implementa: HU-03 (tema e idioma), HU-04 (bloqueo), HU-23, HU-24 y HU-34 (recordatorio). Los
 * valores por defecto son tema del sistema, español, sin bloqueo, sin No molestar global,
 * Pomodoro estándar y recordatorio de 15 minutos antes; el borrado de datos (HU-38) los restablece.
 */
public final class Ajustes {

    private final Tema tema;
    private final Idioma idioma;
    private final boolean bloqueoActivo;
    private final boolean noMolestarGlobal;
    private final ConfiguracionPomodoro pomodoro;
    private final AntelacionRecordatorio recordatorioPorDefecto;

    public Ajustes(Tema tema, Idioma idioma, boolean bloqueoActivo) {
        this(
                tema,
                idioma,
                bloqueoActivo,
                false,
                ConfiguracionPomodoro.porDefecto(),
                AntelacionRecordatorio.MINUTOS_15);
    }

    public Ajustes(
            Tema tema,
            Idioma idioma,
            boolean bloqueoActivo,
            boolean noMolestarGlobal,
            ConfiguracionPomodoro pomodoro) {
        this(
                tema,
                idioma,
                bloqueoActivo,
                noMolestarGlobal,
                pomodoro,
                AntelacionRecordatorio.MINUTOS_15);
    }

    public Ajustes(
            Tema tema,
            Idioma idioma,
            boolean bloqueoActivo,
            boolean noMolestarGlobal,
            ConfiguracionPomodoro pomodoro,
            AntelacionRecordatorio recordatorioPorDefecto) {
        this.tema = Objects.requireNonNull(tema);
        this.idioma = Objects.requireNonNull(idioma);
        this.bloqueoActivo = bloqueoActivo;
        this.noMolestarGlobal = noMolestarGlobal;
        this.pomodoro = Objects.requireNonNull(pomodoro);
        this.recordatorioPorDefecto =
                Objects.requireNonNull(recordatorioPorDefecto, "recordatorioPorDefecto");
    }

    /** @return los ajustes que tiene la aplicación antes de que el estudiante cambie algo */
    public static Ajustes porDefecto() {
        return new Ajustes(
                Tema.SISTEMA,
                Idioma.ES,
                false,
                false,
                ConfiguracionPomodoro.porDefecto(),
                AntelacionRecordatorio.MINUTOS_15);
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

    public AntelacionRecordatorio recordatorioPorDefecto() {
        return recordatorioPorDefecto;
    }

    public Ajustes conTema(Tema nuevo) {
        return new Ajustes(
                nuevo, idioma, bloqueoActivo, noMolestarGlobal, pomodoro, recordatorioPorDefecto);
    }

    public Ajustes conIdioma(Idioma nuevo) {
        return new Ajustes(
                tema, nuevo, bloqueoActivo, noMolestarGlobal, pomodoro, recordatorioPorDefecto);
    }

    public Ajustes conBloqueoActivo(boolean activo) {
        return new Ajustes(
                tema, idioma, activo, noMolestarGlobal, pomodoro, recordatorioPorDefecto);
    }

    public Ajustes conNoMolestarGlobal(boolean activo) {
        return new Ajustes(
                tema, idioma, bloqueoActivo, activo, pomodoro, recordatorioPorDefecto);
    }

    public Ajustes conPomodoro(ConfiguracionPomodoro nueva) {
        return new Ajustes(
                tema, idioma, bloqueoActivo, noMolestarGlobal, nueva, recordatorioPorDefecto);
    }

    public Ajustes conRecordatorioPorDefecto(AntelacionRecordatorio nueva) {
        return new Ajustes(
                tema, idioma, bloqueoActivo, noMolestarGlobal, pomodoro, nueva);
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
                && Objects.equals(pomodoro, a.pomodoro)
                && recordatorioPorDefecto == a.recordatorioPorDefecto;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                tema, idioma, bloqueoActivo, noMolestarGlobal, pomodoro, recordatorioPorDefecto);
    }

    @Override
    @SuppressWarnings("NullableProblems") // el SDK de Android anota Object.toString y el núcleo no importa androidx
    public String toString() {
        return "Ajustes{tema="
                + tema
                + ", idioma="
                + idioma
                + ", bloqueoActivo="
                + bloqueoActivo
                + ", recordatorioPorDefecto="
                + recordatorioPorDefecto
                + '}';
    }
}
