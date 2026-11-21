package io.github.jhoel0521.studyplan.adapter.in.ui.estadisticas;

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
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TopicoEventoEstudio;
import io.github.jhoel0521.studyplan.adapter.in.ui.StudyPlanApplication;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.Pruebas;
import io.github.jhoel0521.studyplan.adapter.in.ui.configuracion.MainActivity;

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

        Materia calculo = app.registrarMateria().ejecutar("Cálculo", 0xFF1565C0, "Prof. A").valor();
        Materia archivada = app.registrarMateria().ejecutar("Historia", 0xFF6A1B9A, "Prof. B").valor();
        Materia general = app.materiaRepository().guardar(Materia.crearGeneral(0xFF78909C));
        int desfase = app.reloj().ahoraInstante().desfaseMinutos();

        // Sesión completada en Cálculo en hoy (siempre dentro de la semana y mes actuales)
        SesionEstudio s1 = app.sesionRepository().guardar(SesionEstudio.planificar(
                calculo.id(), hoy, hoy, Hora.de(8, 0).valor(), Hora.de(9, 0).valor(),
                desfase, null, null)
                .valor()
                .iniciar(io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio.LIBRE, null)
                .valor()
                .detener(60)
                .valor());
        app.tramoDiarioRepository().guardarTramos(s1.id(), Collections.singletonMap(hoy, 60));
        long t0 = app.reloj().instanteDe(hoy, Hora.de(8, 0).valor());
        app.eventoEstudioRepository().guardar(EventoEstudio.crear(
                s1.id(), TipoEventoEstudio.INICIO, t0, desfase, "").valor());
        app.eventoEstudioRepository().guardar(EventoEstudio.crear(
                s1.id(), TipoEventoEstudio.PAUSA, t0 + 20 * 60_000L, desfase, "").valor());
        app.eventoEstudioRepository().guardar(EventoEstudio.crear(
                s1.id(), TipoEventoEstudio.REANUDACION, t0 + 25 * 60_000L, desfase, "",
                TopicoEventoEstudio.DESCANSO_COMIDA).valor());
        app.eventoEstudioRepository().guardar(EventoEstudio.crear(
                s1.id(), TipoEventoEstudio.SALIDA_APP, t0 + 30 * 60_000L, desfase, "").valor());
        app.eventoEstudioRepository().guardar(EventoEstudio.crear(
                s1.id(), TipoEventoEstudio.ESTUDIO_APP_CERRADA, t0 + 40 * 60_000L, desfase, "",
                TopicoEventoEstudio.SIN_CLASIFICAR).valor());
        app.eventoEstudioRepository().guardar(EventoEstudio.crear(
                s1.id(), TipoEventoEstudio.FIN, t0 + 60 * 60_000L, desfase, "").valor());

        // Sesión completada en Historia antes de archivarla (sin depender de registrarPasada)
        SesionEstudio s2 = app.sesionRepository().guardar(SesionEstudio.planificar(
                archivada.id(), hoy, hoy, Hora.de(9, 30).valor(), Hora.de(10, 0).valor(),
                desfase, null, null)
                .valor()
                .iniciar(io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio.LIBRE, null)
                .valor()
                .detener(30)
                .valor());
        app.tramoDiarioRepository().guardarTramos(s2.id(), Collections.singletonMap(hoy, 30));
        app.archivarMateria().ejecutar(archivada.id());

        // Sesión completada en Estudios varios
        SesionEstudio s3 = app.sesionRepository().guardar(SesionEstudio.planificar(
                general.id(), hoy, hoy, Hora.de(10, 30).valor(), Hora.de(11, 0).valor(),
                desfase, null, null)
                .valor()
                .iniciar(io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio.LIBRE, null)
                .valor()
                .detener(30)
                .valor());
        app.tramoDiarioRepository().guardarTramos(s3.id(), Collections.singletonMap(hoy, 30));

        // Sesión reprogramada (cuenta en denominador de cumplimiento: 3 de 4 = 75 %) + pendiente
        SesionEstudio sReprog = app.planificarSesion().ejecutar(
                calculo.id(), hoy, Hora.de(12, 0).valor(), Hora.de(13, 0).valor(), "").sesion();
        app.reprogramarSesion().ejecutar(
                sReprog.id(), hoy, Hora.de(23, 0).valor(), Hora.de(23, 30).valor(), "");
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
            onView(withText(containsString("Cálculo"))).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withText(containsString("Historia"))).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withText(containsString("Estudios varios"))).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.textoCumplimientoValor))
                    .perform(scrollTo())
                    .check(matches(withText(containsString("75 % (3 de 4)"))));
            onView(withId(R.id.textoPendientesAviso))
                    .perform(scrollTo())
                    .check(matches(withText(containsString("1"))));

            // Cambiar a Mes
            onView(withId(R.id.btnPeriodoMes)).perform(click());
            onView(withId(R.id.textoTotalHorasEstadisticas)).perform(scrollTo());
            Pruebas.esperarVisible(withId(R.id.textoTotalHorasEstadisticas));
            onView(withId(R.id.textoTotalHorasEstadisticas))
                    .check(matches(withText(org.hamcrest.Matchers.anyOf(
                            containsString("2.0"), containsString("2,0")))));
            onView(withId(R.id.textoCumplimientoValor))
                    .perform(scrollTo())
                    .check(matches(withText(containsString("75 % (3 de 4)"))));

            // Subvista Concentración
            onView(withId(R.id.btnSubvistaConcentracion)).perform(click());
            onView(withId(R.id.textoIndiceConcentracion)).perform(scrollTo());
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
