package io.github.jhoel0521.studyplan.application.port.out;

/**
 * Puerto de salida: ¿el teléfono tiene un método de bloqueo (huella, PIN, patrón o contraseña)? Lo implementa un
 * adaptador que consulta al sistema operativo.
 *
 * <p>Implementa: HU-04, escenario «Sin método de bloqueo».
 */
public interface DisponibilidadBloqueo {

    boolean estaDisponible();
}
