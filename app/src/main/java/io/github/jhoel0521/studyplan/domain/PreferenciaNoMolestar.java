package io.github.jhoel0521.studyplan.domain;

/**
 * Preferencia tri-estado de modo No molestar por sesión (HU-24, RN-18).
 */
public enum PreferenciaNoMolestar {
    /** Usa la configuración global de Ajustes ({@code null} en persistencia). */
    HEREDAR_GLOBAL(null),
    /** Fuerza activar No molestar solo durante esta sesión ({@code true}). */
    FORZAR_ACTIVAR(Boolean.TRUE),
    /** Fuerza mantener desactivado No molestar en esta sesión ({@code false}). */
    FORZAR_DESACTIVAR(Boolean.FALSE);

    private final Boolean valorPersistido;

    PreferenciaNoMolestar(Boolean valorPersistido) {
        this.valorPersistido = valorPersistido;
    }

    public Boolean aBoolean() {
        return valorPersistido;
    }

    public static PreferenciaNoMolestar desdeBoolean(Boolean valor) {
        if (valor == null) {
            return HEREDAR_GLOBAL;
        }
        return valor ? FORZAR_ACTIVAR : FORZAR_DESACTIVAR;
    }
}
