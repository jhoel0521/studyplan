package io.github.jhoel0521.studyplan.application.port.out;

import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;

/**
 * Puerto de salida para obtener la fecha y hora actuales sin acoplar el núcleo de negocio al
 * reloj del sistema (DIP, HU-02, HU-10 a HU-14, RN-06, RN-08, RN-10, RN-13).
 */
public interface Reloj {

    /** Fecha calendario actual en la zona horaria del dispositivo. */
    Fecha hoy();

    /** Hora local actual del dispositivo. */
    Hora ahora();
}
