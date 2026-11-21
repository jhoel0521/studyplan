package io.github.jhoel0521.studyplan.domain.enumeration;

import java.util.Locale;
import java.util.Optional;

/**
 * Claves canónicas de tópico asociadas a eventos de pausa, salida o estudio fuera de la app
 * ({@code HU-22}, {@code HU-39}, {@code HU-41}, {@code HU-45}, {@code RN-30}).
 *
 * <p>Las etiquetas visibles se resuelven en la capa de presentación mediante {@code strings.xml}
 * ({@code RNF-08}).
 */
public enum TopicoEventoEstudio {
    PDF_MOODLE(true, false),
    LIBRO_APUNTES(true, false),
    GRUPO_CLASE(true, false),
    DESCANSO_COMIDA(false, true),
    LLAMADA_MENSAJE(false, true),
    OTRA_APP(false, true),
    OTRO(true, true),
    SIN_CLASIFICAR(true, true);

    private final boolean paraEstudioExterno;
    private final boolean paraInterrupcion;

    TopicoEventoEstudio(boolean paraEstudioExterno, boolean paraInterrupcion) {
        this.paraEstudioExterno = paraEstudioExterno;
        this.paraInterrupcion = paraInterrupcion;
    }

    public String clave() {
        return name();
    }

    public boolean esParaEstudioExterno() {
        return paraEstudioExterno;
    }

    public boolean esParaInterrupcion() {
        return paraInterrupcion;
    }

    /**
     * Convierte una clave persistida en {@code evento_estudio.topico}. Un valor nulo o en blanco
     * histórico (previo a Room v7) retorna {@link Optional#empty()}, distinto de
     * {@link #SIN_CLASIFICAR}.
     */
    public static Optional<TopicoEventoEstudio> desdeClave(String clave) {
        if (clave == null || clave.trim().isEmpty()) {
            return Optional.empty();
        }
        String normalizada = clave.trim().toUpperCase(Locale.ROOT);
        for (TopicoEventoEstudio valor : values()) {
            if (valor.name().equals(normalizada)) {
                return Optional.of(valor);
            }
        }
        return Optional.of(SIN_CLASIFICAR);
    }
}
