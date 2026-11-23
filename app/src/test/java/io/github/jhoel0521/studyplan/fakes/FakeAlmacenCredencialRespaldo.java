package io.github.jhoel0521.studyplan.fakes;

import io.github.jhoel0521.studyplan.application.port.out.system.AlmacenCredencialRespaldo;

/**
 * Almacén de la contraseña del respaldo en memoria.
 *
 * <p>Apoya las pruebas de: HU-36.
 */
public final class FakeAlmacenCredencialRespaldo implements AlmacenCredencialRespaldo {

    public char[] contrasena;

    @Override
    public void guardar(char[] nueva) {
        contrasena = nueva.clone();
    }

    @Override
    public char[] obtener() {
        return contrasena == null ? null : contrasena.clone();
    }

    @Override
    public void borrar() {
        contrasena = null;
    }
}
