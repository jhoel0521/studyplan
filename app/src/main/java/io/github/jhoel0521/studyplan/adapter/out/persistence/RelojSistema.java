package io.github.jhoel0521.studyplan.adapter.out.persistence;

import java.util.Calendar;

import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;

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
}
