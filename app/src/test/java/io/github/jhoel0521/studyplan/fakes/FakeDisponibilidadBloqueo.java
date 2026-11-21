package io.github.jhoel0521.studyplan.fakes;

import io.github.jhoel0521.studyplan.application.port.out.system.DisponibilidadBloqueo;

/**
 * Puerto de salida falso: el teléfono tiene o no un método de bloqueo, según lo fije la prueba.
 *
 * <p>Apoya las pruebas de: HU-04.
 */
public final class FakeDisponibilidadBloqueo implements DisponibilidadBloqueo {

    public boolean disponible = true;

    @Override
    public boolean estaDisponible() {
        return disponible;
    }
}
