package io.github.jhoel0521.studyplan.application.port.out.system;

import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.valueobject.Instante;

/**
 * Puerto de salida para obtener la fecha y hora actuales sin acoplar el núcleo de negocio al
 * reloj del sistema (DIP, HU-02, HU-10 a HU-14, RN-06, RN-08, RN-10, RN-13).
 */
public interface Reloj {

    /** Fecha calendario actual en la zona horaria del dispositivo. */
    Fecha hoy();

    /** Hora local actual del dispositivo. */
    Hora ahora();

    /** Instante actual en milisegundos desde época UTC. */
    long ahoraMilis();

    /** Instante actual en milisegundos UTC junto con el desfase horario local vigente. */
    Instante ahoraInstante();

    /** Fecha local correspondiente a {@code instanteMilis}. */
    Fecha fechaDe(long instanteMilis);

    /** Hora local correspondiente a {@code instanteMilis}. */
    Hora horaDe(long instanteMilis);

    /** Instante en milisegundos para una fecha y hora locales. */
    long instanteDe(Fecha fecha, Hora hora);
}
