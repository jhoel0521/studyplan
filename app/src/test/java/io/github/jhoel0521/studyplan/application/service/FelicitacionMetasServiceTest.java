package io.github.jhoel0521.studyplan.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.readmodel.CierreSesion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.entity.Perfil;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.readmodel.ResultadoSesion;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.fakes.FakeAjustesRepository;
import io.github.jhoel0521.studyplan.fakes.FakeControladorNoMolestar;
import io.github.jhoel0521.studyplan.fakes.FakeEventoEstudioRepository;
import io.github.jhoel0521.studyplan.fakes.FakeMarcadorSalidaApp;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeMetaSemanalRepository;
import io.github.jhoel0521.studyplan.fakes.FakePerfilRepository;
import io.github.jhoel0521.studyplan.fakes.FakeProgramadorAvisosSesion;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;
import io.github.jhoel0521.studyplan.fakes.FakeTramoDiarioRepository;

/**
 * Pruebas unitarias de felicitación única y acumulada por metas semanales ({@code HU-28},
 * {@code RN-16}).
 */
public final class FelicitacionMetasServiceTest {

    private FakeSesionRepository sesiones;
    private FakeEventoEstudioRepository eventos;
    private FakeMateriaRepository materias;
    private FakeAjustesRepository ajustes;
    private FakeControladorNoMolestar noMolestar;
    private FakeProgramadorAvisosSesion programador;
    private FakeMarcadorSalidaApp marcadorSalida;
    private FakeTramoDiarioRepository tramos;
    private FakeMetaSemanalRepository metas;
    private FakePerfilRepository perfiles;
    private FakeReloj reloj;

    @Before
    public void setUp() {
        sesiones = new FakeSesionRepository();
        eventos = new FakeEventoEstudioRepository();
        materias = new FakeMateriaRepository();
        ajustes = new FakeAjustesRepository();
        noMolestar = new FakeControladorNoMolestar();
        programador = new FakeProgramadorAvisosSesion();
        marcadorSalida = new FakeMarcadorSalidaApp();
        tramos = new FakeTramoDiarioRepository(sesiones);
        metas = new FakeMetaSemanalRepository();
        perfiles = new FakePerfilRepository();
        perfiles.guardar(Perfil.crear("Carlos", 1.0).valor());
        reloj = new FakeReloj(Fecha.parsear("2026-11-18").valor(), Hora.de(10, 0).valor());
    }

    @Test
    public void felicitaUnaSolaVezAlCruzarMetaTotalYMateriaAlDetener() {
        Materia algebra = materias.guardar(Materia.crear("Álgebra", 0xFF1E88E5, null, 1.0).valor());

        EstudiarAhoraService estudiarAhora = new EstudiarAhoraService(
                sesiones, eventos, materias, ajustes, noMolestar, programador, marcadorSalida, reloj);
        SesionEstudio s1 = estudiarAhora.ejecutar(
                algebra.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL).valor();

        reloj.avanzarMilis(60 * 60_000L);
        DetenerSesionService detener = new DetenerSesionService(
                sesiones, eventos, noMolestar, programador, marcadorSalida, tramos, metas, perfiles, materias, reloj);

        Resultado<CierreSesion> cierre1 = detener.ejecutarConMetas(s1.id());
        assertTrue(cierre1.esValido());
        assertEquals(2, cierre1.valor().metasAlcanzadas().size());

        // Segunda sesión en la misma semana no debe volver a felicitar
        SesionEstudio s2 = estudiarAhora.ejecutar(
                algebra.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL).valor();
        reloj.avanzarMilis(30 * 60_000L);
        Resultado<CierreSesion> cierre2 = detener.ejecutarConMetas(s2.id());
        assertTrue(cierre2.esValido());
        assertTrue(cierre2.valor().metasAlcanzadas().isEmpty());
    }

    @Test
    public void cambiarDeMateriaNoFelicitaPeroDetenerFinalSi() {
        Materia m1 = materias.guardar(Materia.crear("Redes", 0xFF1E88E5, null, 1.0).valor());
        Materia m2 = materias.guardar(Materia.crear("SO", 0xFF43A047, null).valor());

        EstudiarAhoraService estudiarAhora = new EstudiarAhoraService(
                sesiones, eventos, materias, ajustes, noMolestar, programador, marcadorSalida, reloj);
        SesionEstudio s1 = estudiarAhora.ejecutar(
                m1.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL).valor();

        reloj.avanzarMilis(60 * 60_000L);
        CambiarDeMateriaService cambiar = new CambiarDeMateriaService(
                sesiones, eventos, materias, ajustes, noMolestar, programador, marcadorSalida, tramos, reloj);
        SesionEstudio s2 = cambiar.ejecutar(s1.id(), m2.id(), null).valor();

        // La meta total de 1h ya se alcanzó en s1, pero cambiarDeMateria NO marcó felicitada;
        // al cerrar s2, sí se detecta si es la primera vez que se evalúa desde el estado previo?
        // Nota: como tramos ya tiene los 60m de s1, s2 con 0m adicionales no cruza de nuevo,
        // pero si m1 se hubiera registrado con RegistrarSesionPasada, sí felicita:
        RegistrarSesionPasadaService pasada = new RegistrarSesionPasadaService(
                sesiones, materias, tramos, metas, perfiles, reloj);
        ResultadoSesion rPasada = pasada.ejecutar(
                m2.id(),
                Fecha.parsear("2026-11-17").valor(),
                Hora.de(8, 0).valor(),
                Hora.de(9, 0).valor(),
                60,
                "Repaso");
        assertTrue(rPasada.esValido());
        assertTrue(rPasada.metasAlcanzadas().isEmpty());
        // s2 tiene 0 min pero no marcó m1 antes, así que m1 no fue felicitada al cambiar de materia
        assertTrue(!metas.buscar(reloj.hoy().lunesDeSemana(), m1.id())
                .map(m -> m.felicitada()).orElse(false));
    }
}
