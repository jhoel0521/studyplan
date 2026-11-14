package io.github.jhoel0521.studyplan.fakes;

import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;

/**
 * Reloj determinista configurable en memoria para pruebas unitarias (HU-02, HU-10 a HU-14,
 * RN-06, RN-08, RN-10, RN-13).
 */
public final class FakeReloj implements Reloj {

    private Fecha hoy;
    private Hora ahora;

    public FakeReloj(Fecha hoy, Hora ahora) {
        this.hoy = hoy;
        this.ahora = ahora;
    }

    public void fijar(Fecha nuevaFecha, Hora nuevaHora) {
        this.hoy = nuevaFecha;
        this.ahora = nuevaHora;
    }

    public void fijarHoy(Fecha nuevaFecha) {
        this.hoy = nuevaFecha;
    }

    @Override
    public Fecha hoy() {
        return hoy;
    }

    @Override
    public Hora ahora() {
        return ahora;
    }
}
