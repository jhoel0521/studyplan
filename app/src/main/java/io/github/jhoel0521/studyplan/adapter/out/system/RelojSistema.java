package io.github.jhoel0521.studyplan.adapter.out.system;

import java.util.Calendar;
import java.util.TimeZone;

import io.github.jhoel0521.studyplan.application.port.out.system.Reloj;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.valueobject.Instante;

/**
 * Implementación del puerto {@link Reloj} usando {@link Calendar} local del dispositivo,
 * compatible con {@code minSdk 24} sin requerir {@code java.time} (HU-02, HU-10 a HU-14).
 */
public final class RelojSistema implements Reloj {

    @Override
    public Fecha hoy() {
        Calendar calendario = Calendar.getInstance();
        return Fecha.de(
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH) + 1,
                calendario.get(Calendar.DAY_OF_MONTH)).valor();
    }

    @Override
    public Hora ahora() {
        Calendar calendario = Calendar.getInstance();
        return Hora.de(
                calendario.get(Calendar.HOUR_OF_DAY),
                calendario.get(Calendar.MINUTE)).valor();
    }

    @Override
    public long ahoraMilis() {
        return System.currentTimeMillis();
    }

    @Override
    public Instante ahoraInstante() {
        long ahora = System.currentTimeMillis();
        int desfaseMinutos = TimeZone.getDefault().getOffset(ahora) / (int) Instante.MILIS_POR_MINUTO;
        return Instante.de(ahora, desfaseMinutos);
    }

    @Override
    public Fecha fechaDe(long instanteMilis) {
        Calendar calendario = Calendar.getInstance();
        calendario.setTimeInMillis(instanteMilis);
        return Fecha.de(
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH) + 1,
                calendario.get(Calendar.DAY_OF_MONTH)).valor();
    }

    @Override
    public Hora horaDe(long instanteMilis) {
        Calendar calendario = Calendar.getInstance();
        calendario.setTimeInMillis(instanteMilis);
        return Hora.de(
                calendario.get(Calendar.HOUR_OF_DAY),
                calendario.get(Calendar.MINUTE)).valor();
    }

    @Override
    public long instanteDe(Fecha fecha, Hora hora) {
        Calendar calendario = Calendar.getInstance();
        calendario.set(Calendar.YEAR, fecha.anio());
        calendario.set(Calendar.MONTH, fecha.mes() - 1);
        calendario.set(Calendar.DAY_OF_MONTH, fecha.dia());
        calendario.set(Calendar.HOUR_OF_DAY, hora.hora());
        calendario.set(Calendar.MINUTE, hora.minuto());
        calendario.set(Calendar.SECOND, 0);
        calendario.set(Calendar.MILLISECOND, 0);
        return calendario.getTimeInMillis();
    }
}
