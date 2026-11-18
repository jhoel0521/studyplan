package io.github.jhoel0521.studyplan.adapter.in.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.adapter.out.persistence.MateriaRepositoryRoom;
import io.github.jhoel0521.studyplan.adapter.out.persistence.StudyPlanDatabase;
import io.github.jhoel0521.studyplan.adapter.out.persistence.TramoDiarioRepositoryRoom;
import io.github.jhoel0521.studyplan.domain.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.TipoEventoEstudio;
import io.github.jhoel0521.studyplan.domain.TopicoEventoEstudio;

/**
 * Pruebas E2E Espresso de Estadísticas ({@code HU-29}, {@code HU-30}, {@code HU-31},
 * {@code HU-45}).
 */
@RunWith(AndroidJUnit4.class)
public class EstadisticasEspressoTest {

    @Before
    public void preparar() {
        Pruebas.reiniciarEstado();
        StudyPlanApplication app = Pruebas.app();
        app.configurarPerfil().ejecutar("Ana", 20);
        Fecha hoy = app.reloj().hoy();

        StudyPlanDatabase db = StudyPlanDatabase.abrir(app);
        MateriaRepositoryRoom materiasRoom = new MateriaRepositoryRoom(db.materiaDao());
        TramoDiarioRepositoryRoom tramosRoom = new TramoDiarioRepositoryRoom(db.tramoDiarioDao());

        Materia calculo = app.registrarMateria().ejecutar("Cálculo", 0xFF1565C0, "Prof. A").valor();
        Materia archivada = app.registrarMateria().ejecutar("Historia", 0xFF6A1B9A, "Prof. B").valor();
        app.archivarMateria().ejecutar(archivada.id());
        Materia general = materiasRoom.guardar(Materia.crearGeneral(0xFF78909C));
        Fecha ayer = hoy.sumarDias(-1);

        // Sesión completada en Cálculo con pausa + tópico y estudio externo sin clasificar
        SesionEstudio s1 = app.sesionRepository().guardar(SesionEstudio.planificar(
                calculo.id(), ayer, ayer, Hora.de(9, 0).valor(), Hora.de(10, 0).valor(), null, null)
                .valor()
                .iniciar(io.github.jhoel0521.studyplan.domain.TecnicaEstudio.LIBRE, null)
                .valor()
                .detener(60)
                .valor());
        tramosRoom.guardarTramos(s1.id(), Collections.singletonMap(ayer, 60));
        long t0 = app.reloj().instanteDe(ayer, Hora.de(9, 0).valor());
        app.eventoEstudioRepository().guardar(EventoEstudio.crear(
                s1.id(), TipoEventoEstudio.INICIO, t0, -240, "").valor());
        app.eventoEstudioRepository().guardar(EventoEstudio.crear(
                s1.id(), TipoEventoEstudio.PAUSA, t0 + 20 * 60_000L, -240, "").valor());
        app.eventoEstudioRepository().guardar(EventoEstudio.crear(
                s1.id(), TipoEventoEstudio.REANUDACION, t0 + 25 * 60_000L, -240, "",
                TopicoEventoEstudio.DESCANSO_COMIDA).valor());
        app.eventoEstudioRepository().guardar(EventoEstudio.crear(
                s1.id(), TipoEventoEstudio.SALIDA_APP, t0 + 30 * 60_000L, -240, "").valor());
        app.eventoEstudioRepository().guardar(EventoEstudio.crear(
                s1.id(), TipoEventoEstudio.ESTUDIO_APP_CERRADA, t0 + 40 * 60_000L, -240, "",
                TopicoEventoEstudio.SIN_CLASIFICAR).valor());
        app.eventoEstudioRepository().guardar(EventoEstudio.crear(
                s1.id(), TipoEventoEstudio.FIN, t0 + 60 * 60_000L, -240, "").valor());

        // Sesión completada en materia archivada
        SesionEstudio s2 = app.sesionRepository().guardar(SesionEstudio.registrarPasada(
                archivada.id(), ayer, hoy, Hora.de(11, 0).valor(), Hora.de(11, 30).valor(), -240, 30, "")
                .valor());
        tramosRoom.guardarTramos(s2.id(), Collections.singletonMap(ayer, 30));

        // Sesión completada en Estudios varios
        SesionEstudio s3 = app.sesionRepository().guardar(SesionEstudio.registrarPasada(
                general.id(), ayer, hoy, Hora.de(12, 0).valor(), Hora.de(12, 30).valor(), -240, 30, "")
                .valor());
        tramosRoom.guardarTramos(s3.id(), Collections.singletonMap(ayer, 30));

        // Sesión futura pendiente (no debe contar en la tasa de cumplimiento)
        Fecha manana = hoy.sumarDias(1);
        app.planificarSesion().ejecutar(
                calculo.id(), manana, Hora.de(15, 0).valor(), Hora.de(16, 0).valor(), "");
    }

    @After
    public void limpiar() {
        Pruebas.reiniciarEstado();
    }

    @Test
    public void muestraResumenYConcentracionConMateriasArchivadaGeneralYTopicos() throws Exception {
        try (ActivityScenario<MainActivity> escenario = ActivityScenario.launch(MainActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.barraInferior));
            onView(withId(R.id.nav_estadisticas)).perform(click());

            Pruebas.esperarVisible(withId(R.id.textoTotalHorasEstadisticas));
            onView(withId(R.id.textoTotalHorasEstadisticas))
                    .check(matches(withText(org.hamcrest.Matchers.anyOf(
                            containsString("2.0"), containsString("2,0")))));
            onView(withId(R.id.graficoDistribucionMaterias)).check(matches(isDisplayed()));
            onView(withText(containsString("Cálculo"))).check(matches(isDisplayed()));
            onView(withText(containsString("Historia"))).check(matches(isDisplayed()));
            onView(withText(containsString("Estudios varios"))).check(matches(isDisplayed()));
            onView(withId(R.id.textoCumplimientoValor))
                    .check(matches(withText(containsString("100 %"))));
            onView(withId(R.id.textoPendientesAviso))
                    .check(matches(withText(containsString("1"))));

            // Cambiar a Mes
            onView(withId(R.id.btnPeriodoMes)).perform(click());
            Pruebas.esperarVisible(withId(R.id.textoTotalHorasEstadisticas));

            // Subvista Concentración
            onView(withId(R.id.btnSubvistaConcentracion)).perform(click());
            Pruebas.esperarVisible(withId(R.id.textoIndiceConcentracion));
            onView(withId(R.id.textoEstudioExternoResumen))
                    .perform(scrollTo())
                    .check(matches(withText(containsString("10 min"))));
            onView(withText(R.string.topico_descanso_comida))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));
            onView(withText(R.string.topico_sin_clasificar))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));
        }
    }
}
