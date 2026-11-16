package io.github.jhoel0521.studyplan.fakes;

import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Instante;

/**
 * Reloj determinista configurable en memoria para pruebas unitarias (HU-02, HU-10 a HU-14,
 * HU-22 a HU-24, HU-39 a HU-44, RN-06, RN-08, RN-10, RN-13, RN-22 a RN-29, RN-31).
 */
public final class FakeReloj implements Reloj {

    private Fecha hoy;
    private Hora ahora;
    private long extraMilis;
    private int desfaseMinutos;

    public FakeReloj(Fecha hoy, Hora ahora) {
        this(hoy, ahora, 0);
    }

    public FakeReloj(Fecha hoy, Hora ahora, int desfaseMinutos) {
        this.hoy = hoy;
        this.ahora = ahora;
        this.desfaseMinutos = desfaseMinutos;
    }

    public void fijar(Fecha nuevaFecha, Hora nuevaHora) {
        this.hoy = nuevaFecha;
        this.ahora = nuevaHora;
        this.extraMilis = 0L;
    }

    public void fijarHoy(Fecha nuevaFecha) {
        this.hoy = nuevaFecha;
    }

    public void fijarDesfaseMinutos(int nuevoDesfaseMinutos) {
        this.desfaseMinutos = nuevoDesfaseMinutos;
    }

    public int desfaseMinutos() {
        return desfaseMinutos;
    }

    public void avanzarMilis(long deltaMilis) {
        long nuevo = ahoraMilis() + deltaMilis;
        this.hoy = fechaDe(nuevo);
        this.ahora = horaDe(nuevo);
        this.extraMilis = Math.floorMod(nuevo + desfaseMinutos * Instante.MILIS_POR_MINUTO, 60_000L);
    }

    @Override
    public Fecha hoy() {
        return hoy;
    }

    @Override
    public Hora ahora() {
        return ahora;
    }

    @Override
    public long ahoraMilis() {
        return instanteDe(hoy, ahora) + extraMilis;
    }

    @Override
    public Instante ahoraInstante() {
        return Instante.de(ahoraMilis(), desfaseMinutos);
    }

    @Override
    public Fecha fechaDe(long instanteMilis) {
        return Instante.de(instanteMilis, desfaseMinutos).fechaLocal();
    }

    @Override
    public Hora horaDe(long instanteMilis) {
        return Instante.de(instanteMilis, desfaseMinutos).horaLocal();
    }

    @Override
    public long instanteDe(Fecha fecha, Hora hora) {
        return Instante.desdeFechaYHoraLocal(fecha, hora, desfaseMinutos).utcMilis();
    }
}
