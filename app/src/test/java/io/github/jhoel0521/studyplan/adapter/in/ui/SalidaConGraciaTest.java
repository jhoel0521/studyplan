package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Pruebas de la ventana de gracia de 1 minuto al salir de la app (HU-40, HU-50, RN-23).
 */
public class SalidaConGraciaTest {

    private static final long SEG = 1_000L;
    private static final long MIN = 60_000L;

    private long[] reloj;
    private FakeProgramadorRetardado programador;
    private boolean pantallaApagada;
    private int bloqueos;
    private final List<long[]> salidas = new ArrayList<>(); // {instante, avisar(0/1)}
    private SalidaConGracia gracia;

    @Before
    public void configurar() {
        reloj = new long[] {10 * MIN};
        programador = new FakeProgramadorRetardado(reloj);
        gracia = new SalidaConGracia(
                () -> reloj[0],
                programador,
                () -> pantallaApagada,
                new SalidaConGracia.Destino() {
                    @Override
                    public void registrarBloqueo() {
                        bloqueos++;
                    }

                    @Override
                    public void confirmarSalida(long instanteSalidaMs, boolean avisar) {
                        salidas.add(new long[] {instanteSalidaMs, avisar ? 1 : 0});
                    }
                },
                1_200L,
                MIN);
    }

    @Test
    public void sinSalidaNoHaceNada() {
        assertEquals(SalidaConGracia.Retorno.SIN_SALIDA, gracia.actividadIniciada());
        assertTrue(salidas.isEmpty());
    }

    @Test
    public void volverEn55SegundosNoEsSalida() {
        gracia.ultimaActividadDetenida();
        programador.avanzar(55 * SEG);

        assertEquals(SalidaConGracia.Retorno.DENTRO_DE_GRACIA, gracia.actividadIniciada());

        programador.avanzar(10 * MIN);
        assertTrue("No debe confirmarse ninguna salida", salidas.isEmpty());
        assertEquals(0, bloqueos);
    }

    @Test
    public void volverAntesDeDecidirNoEsSalida() {
        gracia.ultimaActividadDetenida();
        programador.avanzar(500);

        assertEquals(SalidaConGracia.Retorno.DENTRO_DE_GRACIA, gracia.actividadIniciada());
        programador.avanzar(5 * MIN);
        assertTrue(salidas.isEmpty());
    }

    @Test
    public void alVencerLaGraciaConfirmaLaSalidaConLaHoraRealDeSalida() {
        long salida = reloj[0];
        gracia.ultimaActividadDetenida();

        programador.avanzar(MIN + 1);

        assertEquals(1, salidas.size());
        assertEquals(salida, salidas.get(0)[0]);
        assertEquals("Debe avisar", 1, salidas.get(0)[1]);
        assertEquals(SalidaConGracia.Retorno.SALIDA_CONFIRMADA, gracia.actividadIniciada());
        assertEquals("No se confirma dos veces", 1, salidas.size());
    }

    @Test
    public void conElTelefonoDormidoSeConfirmaAlVolverConElRelojReal() {
        long salida = reloj[0];
        gracia.ultimaActividadDetenida();
        programador.avanzar(10 * SEG);
        programador.dormir(30 * MIN); // el temporizador no corrió

        assertEquals(SalidaConGracia.Retorno.SALIDA_CONFIRMADA, gracia.actividadIniciada());

        assertEquals(1, salidas.size());
        assertEquals("Salida a la hora real, no al volver", salida, salidas.get(0)[0]);
        assertEquals("Ya está de vuelta: sin aviso", 0, salidas.get(0)[1]);
    }

    @Test
    public void conLaPantallaApagadaEsUnBloqueoYNoUnaSalida() {
        gracia.ultimaActividadDetenida();
        pantallaApagada = true;
        programador.avanzar(1_300L);

        assertEquals(1, bloqueos);
        programador.avanzar(5 * MIN);
        assertTrue(salidas.isEmpty());
        assertEquals(SalidaConGracia.Retorno.DESBLOQUEO, gracia.actividadIniciada());
    }

    @Test
    public void unaGraciaMasCortaParaPruebasVenceAntes() {
        gracia.fijarGraciaMs(3 * SEG);
        gracia.ultimaActividadDetenida();

        programador.avanzar(3 * SEG + 1);

        assertEquals(1, salidas.size());
    }

    @Test
    public void reiniciarCancelaLoPendienteYRestableceLaGracia() {
        gracia.fijarGraciaMs(3 * SEG);
        gracia.ultimaActividadDetenida();
        gracia.reiniciar();
        programador.avanzar(5 * MIN);
        assertTrue(salidas.isEmpty());

        gracia.ultimaActividadDetenida();
        programador.avanzar(30 * SEG);
        assertTrue("La gracia volvió a 1 minuto", salidas.isEmpty());
    }
}
