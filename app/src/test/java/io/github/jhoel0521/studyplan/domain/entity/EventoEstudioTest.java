package io.github.jhoel0521.studyplan.domain.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TopicoEventoEstudio;
import io.github.jhoel0521.studyplan.domain.readmodel.SalidaRegistrada;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.valueobject.Instante;

/**
 * Pruebas unitarias de {@link TopicoEventoEstudio}, {@link SalidaRegistrada} y {@link EventoEstudio}
 * ({@code HU-39}, {@code HU-40}, {@code RN-30}).
 */
public final class EventoEstudioTest {

    @Test
    public void topicoNuloOVacioHistoricoSeExponeComoEmptyYSeDistingueDeSinClasificar() {
        EventoEstudio historico = EventoEstudio.restaurar(
                1L, 10L, TipoEventoEstudio.REANUDACION, 1_794_834_000_000L, -240, "", null);
        assertFalse(historico.topico().isPresent());

        EventoEstudio sinClasificar = EventoEstudio.restaurar(
                2L, 10L, TipoEventoEstudio.REANUDACION, 1_794_834_000_000L, -240, "", "SIN_CLASIFICAR");
        assertTrue(sinClasificar.topico().isPresent());
        assertEquals(TopicoEventoEstudio.SIN_CLASIFICAR, sinClasificar.topico().get());
    }

    @Test
    public void crearEventoConTopicoConservaClaveYConIdLaPreserva() {
        Instante inst = Instante.de(1_794_834_000_000L, -240);
        Resultado<EventoEstudio> res = EventoEstudio.crear(
                5L,
                TipoEventoEstudio.ESTUDIO_APP_CERRADA,
                inst,
                "Lectura PDF",
                TopicoEventoEstudio.PDF_MOODLE);

        assertTrue(res.esValido());
        assertEquals(TopicoEventoEstudio.PDF_MOODLE, res.valor().topico().orElse(null));
        assertEquals(
                TopicoEventoEstudio.PDF_MOODLE,
                res.valor().conId(99L).topico().orElse(null));
    }

    @Test
    public void salidaRegistradaConservaSesionYMateria() {
        SalidaRegistrada salida = new SalidaRegistrada(7L, "Cálculo");
        assertEquals(7L, salida.sesionId());
        assertEquals("Cálculo", salida.nombreMateria());
    }
}
