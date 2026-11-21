package io.github.jhoel0521.studyplan.domain.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.domain.enumeration.Idioma;
import io.github.jhoel0521.studyplan.domain.enumeration.Tema;
import io.github.jhoel0521.studyplan.domain.valueobject.ConfiguracionPomodoro;

/**
 * Pruebas unitarias de la antelación de recordatorio en {@link Ajustes} ({@code HU-34}).
 */
public class AjustesRecordatorioTest {

    @Test
    public void ajustesPorDefectoUsaQuinceMinutosYConstructoresCompatiblesConservanDefecto() {
        assertEquals(
                AntelacionRecordatorio.MINUTOS_15,
                Ajustes.porDefecto().recordatorioPorDefecto());

        Ajustes tresArgs = new Ajustes(Tema.OSCURO, Idioma.EN, true);
        assertEquals(AntelacionRecordatorio.MINUTOS_15, tresArgs.recordatorioPorDefecto());

        Ajustes cincoArgs = new Ajustes(
                Tema.CLARO, Idioma.ES, false, true, ConfiguracionPomodoro.porDefecto());
        assertEquals(AntelacionRecordatorio.MINUTOS_15, cincoArgs.recordatorioPorDefecto());
    }

    @Test
    public void conRecordatorioPorDefectoEsInmutableYAfectaIgualdad() {
        Ajustes base = Ajustes.porDefecto();
        Ajustes sinAviso = base.conRecordatorioPorDefecto(AntelacionRecordatorio.SIN_RECORDATORIO);
        Ajustes treinta = sinAviso.conRecordatorioPorDefecto(AntelacionRecordatorio.MINUTOS_30);

        assertEquals(AntelacionRecordatorio.MINUTOS_15, base.recordatorioPorDefecto());
        assertEquals(AntelacionRecordatorio.SIN_RECORDATORIO, sinAviso.recordatorioPorDefecto());
        assertEquals(AntelacionRecordatorio.MINUTOS_30, treinta.recordatorioPorDefecto());
        assertNotEquals(base, sinAviso);
    }
}
