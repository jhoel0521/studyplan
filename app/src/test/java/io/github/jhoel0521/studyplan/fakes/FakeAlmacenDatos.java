package io.github.jhoel0521.studyplan.fakes;

import io.github.jhoel0521.studyplan.application.port.out.persistence.AlmacenDatos;

/**
 * Puerto de salida falso que solo cuenta cuántas veces se pidió borrar.
 *
 * <p>Apoya las pruebas de: HU-38.
 */
public final class FakeAlmacenDatos implements AlmacenDatos {

    public int borrados;

    @Override
    public void borrarTodo() {
        borrados++;
    }
}
