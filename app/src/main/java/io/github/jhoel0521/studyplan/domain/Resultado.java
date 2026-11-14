package io.github.jhoel0521.studyplan.domain;

import java.util.Objects;

/**
 * Resultado de una validación del dominio: o bien un valor correcto o bien un {@link ErrorValidacion}.
 *
 * <p>Implementa: HU-01 y RNF-14 (validación de entradas sin lanzar excepciones).
 *
 * @param <T> tipo del valor cuando la validación es correcta
 */
public final class Resultado<T> {

    private final T valor;
    private final ErrorValidacion error;

    private Resultado(T valor, ErrorValidacion error) {
        this.valor = valor;
        this.error = error;
    }

    public static <T> Resultado<T> ok(T valor) {
        return new Resultado<>(Objects.requireNonNull(valor), null);
    }

    public static <T> Resultado<T> error(ErrorValidacion error) {
        return new Resultado<>(null, Objects.requireNonNull(error));
    }

    public boolean esValido() {
        return error == null;
    }

    /** @throws IllegalStateException si el resultado es un error */
    public T valor() {
        if (error != null) {
            throw new IllegalStateException("El resultado es un error: " + error);
        }
        return valor;
    }

    /** @return el código de error, o {@code null} si el resultado es válido */
    public ErrorValidacion error() {
        return error;
    }
}
