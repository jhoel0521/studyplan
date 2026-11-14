package io.github.jhoel0521.studyplan.application.service;

import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.BorrarDatos;
import io.github.jhoel0521.studyplan.application.port.out.AlmacenDatos;
import io.github.jhoel0521.studyplan.domain.AlcanceBorrado;

/**
 * Implementa {@link BorrarDatos}: vacía el almacén local y deja la sesión sin bloqueo, porque tras el borrado los
 * ajustes vuelven a sus valores por defecto (bloqueo inactivo).
 *
 * <p>Implementa: HU-38 y RN-21 (parcial: solo el alcance «teléfono»).
 */
public final class BorrarDatosService implements BorrarDatos {

    private final AlmacenDatos almacen;
    private final SesionBloqueo sesion;

    public BorrarDatosService(AlmacenDatos almacen, SesionBloqueo sesion) {
        this.almacen = almacen;
        this.sesion = sesion;
    }

    @Override
    public void ejecutar(AlcanceBorrado alcance) {
        Objects.requireNonNull(alcance);
        if (alcance == AlcanceBorrado.TELEFONO) {
            almacen.borrarTodo();
            sesion.alCambiarBloqueo(false);
        }
    }
}
