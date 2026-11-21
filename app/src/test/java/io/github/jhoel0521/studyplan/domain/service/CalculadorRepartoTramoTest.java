package io.github.jhoel0521.studyplan.domain.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio;
import io.github.jhoel0521.studyplan.domain.readmodel.ResultadoReparto;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;

/**
 * Pruebas unitarias de {@link CalculadorRepartoTramo} (HU-44, RN-11, RN-28).
 */
public class CalculadorRepartoTramoTest {

    private static final long MIN = 60_000L;
    private static final Fecha HOY = Fecha.de(2026, 11, 16).valor();
    private static final Hora HORA = Hora.de(10, 0).valor();

    @Test
    public void reparteTramoContinuoSinConvertirPausasEnTiempoReal() {
        long t0 = 1_000_000L;
        // 10:00..10:30 estudio, 10:30..10:45 pausa (15 min), 10:45..11:45 estudio -> total 90 min
        EventoEstudio inicio = EventoEstudio.restaurar(1L, 1L, TipoEventoEstudio.INICIO, t0, HOY, HORA, "");
        EventoEstudio pausa = EventoEstudio.restaurar(2L, 1L, TipoEventoEstudio.PAUSA, t0 + 30 * MIN, HOY, HORA, "");
        EventoEstudio reanuda = EventoEstudio.restaurar(3L, 1L, TipoEventoEstudio.REANUDACION, t0 + 45 * MIN, HOY, HORA, "");
        EventoEstudio fin = EventoEstudio.restaurar(4L, 1L, TipoEventoEstudio.FIN, t0 + 105 * MIN, HOY, HORA, "");

        // Corte a las 11:05 (t0 + 65 min): primera = 30 + (65-45) = 50 min; segunda = 105 - 65 = 40 min
        Resultado<ResultadoReparto> res = CalculadorRepartoTramo.repartir(
                TecnicaEstudio.LIBRE,
                null,
                60,
                Arrays.asList(inicio, pausa, reanuda, fin),
                t0 + 105 * MIN,
                t0 + 65 * MIN);

        assertTrue(res.esValido());
        assertEquals(50, res.valor().minutosPrimeraSesion());
        assertEquals(40, res.valor().minutosSegundaSesion());

        // Corte fuera de rango rechazado
        assertEquals(ErrorValidacion.CORTE_REPARTO_INVALIDO,
                CalculadorRepartoTramo.repartir(
                        TecnicaEstudio.LIBRE, null, 60,
                        Arrays.asList(inicio, pausa, reanuda, fin), t0 + 105 * MIN, t0).error());
    }
}
