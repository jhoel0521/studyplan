package io.github.jhoel0521.studyplan.adapter.out.system;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.application.port.out.ProgramadorAvisosSesion;
import io.github.jhoel0521.studyplan.fakes.FakeProgramadorAvisosSesion;

/**
 * Pruebas unitarias de {@link ProgramadorAvisosConNotificacion} (HU-49, HU-40).
 */
public class ProgramadorAvisosConNotificacionTest {

    private FakeProgramadorAvisosSesion real;
    private List<Long> refrescos;
    private ProgramadorAvisosConNotificacion decorador;

    @Before
    public void configurar() {
        real = new FakeProgramadorAvisosSesion();
        refrescos = new ArrayList<>();
        decorador = new ProgramadorAvisosConNotificacion(real, refrescos::add);
    }

    @Test
    public void programarDelegaYRefrescaUnaVezConLaSesionCorrecta() {
        decorador.programarSiguienteHito(
                7L, ProgramadorAvisosSesion.TipoHito.FIN_PLANIFICADO, 123_000L, "Cálculo");

        assertEquals(Long.valueOf(123_000L), real.ultimoInstante);
        assertEquals(1, refrescos.size());
        assertEquals(Long.valueOf(7L), refrescos.get(0));
    }

    @Test
    public void cancelarDelegaYRefrescaUnaVez() {
        decorador.cancelarAvisos(9L);

        assertEquals(1, refrescos.size());
        assertEquals(Long.valueOf(9L), refrescos.get(0));
    }

    @Test
    public void cadaLlamadaRefrescaPorSeparado() {
        decorador.programarSiguienteHito(
                1L, ProgramadorAvisosSesion.TipoHito.CAMBIO_FASE_POMODORO, 1L, "A");
        decorador.cancelarAvisos(1L);
        decorador.cancelarAvisos(2L);

        assertEquals(3, refrescos.size());
    }
}
