package io.github.jhoel0521.studyplan.application.service.recordatorios;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.entity.Ajustes;
import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.fakes.FakeAjustesRepository;

/**
 * Pruebas unitarias de {@link ConfigurarRecordatorioPorDefectoService} ({@code HU-34}).
 */
public class ConfigurarRecordatorioPorDefectoServiceTest {

    private FakeAjustesRepository ajustesRepo;
    private ConfigurarRecordatorioPorDefectoService servicio;

    @Before
    public void preparar() {
        ajustesRepo = new FakeAjustesRepository();
        servicio = new ConfigurarRecordatorioPorDefectoService(ajustesRepo);
    }

    @Test
    public void guardaAntelacionElegidaONormalizaNullASinRecordatorio() {
        Ajustes treinta = servicio.ejecutar(AntelacionRecordatorio.MINUTOS_30);
        assertEquals(AntelacionRecordatorio.MINUTOS_30, treinta.recordatorioPorDefecto());
        assertEquals(
                AntelacionRecordatorio.MINUTOS_30,
                ajustesRepo.obtener().recordatorioPorDefecto());

        Ajustes sinAviso = servicio.ejecutar(null);
        assertEquals(AntelacionRecordatorio.SIN_RECORDATORIO, sinAviso.recordatorioPorDefecto());
        assertEquals(
                AntelacionRecordatorio.SIN_RECORDATORIO,
                ajustesRepo.obtener().recordatorioPorDefecto());
    }
}
