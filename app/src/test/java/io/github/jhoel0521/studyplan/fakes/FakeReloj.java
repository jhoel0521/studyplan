package io.github.jhoel0521.studyplan.fakes;

import java.util.Calendar;
import java.util.TimeZone;

import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;

/**
 * Reloj determinista configurable en memoria para pruebas unitarias (HU-02, HU-10 a HU-14,
 * HU-22 a HU-24, HU-39 a HU-44, RN-06, RN-08, RN-10, RN-13, RN-22 a RN-29).
 */
public final class FakeReloj implements Reloj {

    private Fecha hoy;
    private Hora ahora;
    private long extraMilis;

    public FakeReloj(Fecha hoy, Hora ahora) {
        this.hoy = hoy;
        this.ahora = ahora;
    }

    public void fijar(Fecha nuevaFecha, Hora nuevaHora) {
        this.hoy = nuevaFecha;
        this.ahora = nuevaHora;
        this.extraMilis = 0L;
    }

    public void fijarHoy(Fecha nuevaFecha) {
        this.hoy = nuevaFecha;
    }

    public void avanzarMilis(long deltaMilis) {
        long nuevo = ahoraMilis() + deltaMilis;
        this.hoy = fechaDe(nuevo);
        this.ahora = horaDe(nuevo);
        this.extraMilis = Math.floorMod(nuevo, 60_000L);
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
    public Fecha fechaDe(long instanteMilis) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis(instanteMilis);
        return Fecha.de(
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH)).valor();
    }

    @Override
    public Hora horaDe(long instanteMilis) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis(instanteMilis);
        return Hora.de(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)).valor();
    }

    @Override
    public long instanteDe(Fecha fecha, Hora hora) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.set(Calendar.YEAR, fecha.anio());
        c.set(Calendar.MONTH, fecha.mes() - 1);
        c.set(Calendar.DAY_OF_MONTH, fecha.dia());
        c.set(Calendar.HOUR_OF_DAY, hora.hora());
        c.set(Calendar.MINUTE, hora.minuto());
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
