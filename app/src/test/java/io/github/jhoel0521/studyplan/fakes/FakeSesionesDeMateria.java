package io.github.jhoel0521.studyplan.fakes;

import java.util.HashMap;
import java.util.Map;

import io.github.jhoel0521.studyplan.application.port.out.SesionesDeMateria;

/**
 * Puerto de salida falso: permite declarar cuántas sesiones tiene cada materia.
 *
 * <p>Apoya las pruebas de: HU-08 y RN-03.
 */
public final class FakeSesionesDeMateria implements SesionesDeMateria {

    private final Map<Long, Integer> sesiones = new HashMap<>();

    public void conSesiones(long materiaId, int cantidad) {
        sesiones.put(materiaId, cantidad);
    }

    @Override
    public int contar(long materiaId) {
        Integer cantidad = sesiones.get(materiaId);
        return cantidad == null ? 0 : cantidad;
    }
}
