package io.github.jhoel0521.studyplan.adapter.in.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Instante;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.PeriodoEstadisticas;
import io.github.jhoel0521.studyplan.domain.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.TecnicaEstudio;

/**
 * Pruebas E2E Espresso de Reportes ({@code HU-32}, {@code HU-33}, {@code RN-05}, {@code RNF-10},
 * {@code W8}).
 */
@RunWith(AndroidJUnit4.class)
public final class ReportesEspressoTest {

    @Before
    public void preparar() {
        Pruebas.reiniciarEstado();
        Pruebas.app().configurarPerfil().ejecutar("Ana", 20);
    }

    @After
    public void limpiar() {
        Pruebas.reiniciarEstado();
    }

    @Test
    public void entradaDesdeEstadisticasMuestraPeriodoCoincidenteCeroRealesGeneralArchivadaSinPlanYExcluyeReprogramada()
            throws Exception {
        StudyPlanApplication app = Pruebas.app();
        Fecha hoy = app.reloj().hoy();
        PeriodoEstadisticas mesActual = PeriodoEstadisticas.mensual(hoy);
        PeriodoEstadisticas mesAnterior = mesActual.anterior();
        PeriodoEstadisticas semanaActual = PeriodoEstadisticas.semanal(hoy);
        Fecha fechaMesAnt = mesAnterior.inicio().sumarDias(10);
        int desfase = app.reloj().ahoraInstante().desfaseMinutos();

        Materia calculo = app.registrarMateria().ejecutar("Cálculo", 0xFF1565C0, "Prof. A").valor();
        Materia historia = app.registrarMateria().ejecutar("Historia", 0xFF6A1B9A, "Prof. B").valor();
        Materia fisica = app.registrarMateria().ejecutar("Física", 0xFF2E7D32, "Prof. C").valor();
        Materia general = app.materiaRepository().guardar(Materia.crearGeneral(0xFF78909C));

        // Mes anterior: Cálculo (60 min plan, 90 min real -> 150 %) + Historia archivada (60 min NO_REALIZADA, 0 real)
        long iniCalcAnt = Instante.desdeFechaYHoraLocal(
                fechaMesAnt, Hora.de(8, 0).valor(), desfase).utcMilis();
        SesionEstudio sCalcAnt = app.sesionRepository().guardar(SesionEstudio.restaurar(
                null, calculo.id(), iniCalcAnt, iniCalcAnt + 60 * 60_000L, desfase, 60, 90,
                CodigoEstadoSesion.COMPLETADA, "", null, TecnicaEstudio.LIBRE,
                PreferenciaNoMolestar.HEREDAR_GLOBAL, null, null, null, null));
        app.tramoDiarioRepository().guardarTramos(sCalcAnt.id(), Collections.singletonMap(fechaMesAnt, 90));

        long iniHistAnt = Instante.desdeFechaYHoraLocal(
                fechaMesAnt, Hora.de(10, 0).valor(), desfase).utcMilis();
        app.sesionRepository().guardar(SesionEstudio.restaurar(
                null, historia.id(), iniHistAnt, iniHistAnt + 60 * 60_000L, desfase, 60, null,
                CodigoEstadoSesion.NO_REALIZADA, "", null, TecnicaEstudio.LIBRE,
                PreferenciaNoMolestar.HEREDAR_GLOBAL, null, null, null, null));
        app.archivarMateria().ejecutar(historia.id());

        // Periodo actual (hoy): Física sin plan (0 plan, 30 real -> —), Estudios varios (60 plan, 30 real -> 50 %),
        // y Cálculo REPROGRAMADA (120 min plan que NO debe sumarse ni mostrar Cálculo en hoy)
        long iniFisHoy = Instante.desdeFechaYHoraLocal(
                hoy, Hora.de(8, 0).valor(), desfase).utcMilis();
        SesionEstudio sFisHoy = app.sesionRepository().guardar(SesionEstudio.restaurar(
                null, fisica.id(), iniFisHoy, null, desfase, 0, 30,
                CodigoEstadoSesion.COMPLETADA, "", null, TecnicaEstudio.LIBRE,
                PreferenciaNoMolestar.HEREDAR_GLOBAL, null, null, null, null));
        app.tramoDiarioRepository().guardarTramos(sFisHoy.id(), Collections.singletonMap(hoy, 30));

        long iniGenHoy = Instante.desdeFechaYHoraLocal(
                hoy, Hora.de(10, 0).valor(), desfase).utcMilis();
        SesionEstudio sGenHoy = app.sesionRepository().guardar(SesionEstudio.restaurar(
                null, general.id(), iniGenHoy, iniGenHoy + 60 * 60_000L, desfase, 60, 30,
                CodigoEstadoSesion.COMPLETADA, "", null, TecnicaEstudio.LIBRE,
                PreferenciaNoMolestar.HEREDAR_GLOBAL, null, null, null, null));
        app.tramoDiarioRepository().guardarTramos(sGenHoy.id(), Collections.singletonMap(hoy, 30));

        long iniReprogHoy = Instante.desdeFechaYHoraLocal(
                hoy, Hora.de(14, 0).valor(), desfase).utcMilis();
        app.sesionRepository().guardar(SesionEstudio.restaurar(
                null, calculo.id(), iniReprogHoy, iniReprogHoy + 120 * 60_000L, desfase, 120,
                null, CodigoEstadoSesion.REPROGRAMADA, "", null, TecnicaEstudio.LIBRE,
                PreferenciaNoMolestar.HEREDAR_GLOBAL, null, null, null, null));

        try (ActivityScenario<MainActivity> escenario =
                ActivityScenario.launch(MainActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.barraInferior));
            verificarCuatroPestanasFijas();

            onView(withId(R.id.nav_estadisticas)).perform(click());
            Pruebas.esperarVisible(withId(R.id.btnPeriodoMes));
            onView(withId(R.id.btnPeriodoMes)).perform(click());
            Pruebas.esperarVisible(allOf(
                    withId(R.id.textoRangoEstadisticas),
                    withText(containsString(mesActual.inicio().toString()))));

            onView(withId(R.id.btnPeriodoAnteriorEstadisticas)).perform(click());
            Pruebas.esperarVisible(allOf(
                    withId(R.id.textoRangoEstadisticas),
                    withText(containsString(mesAnterior.inicio().toString()))));

            onView(withId(R.id.botonAbrirReportes)).perform(scrollTo(), click());

            Pruebas.esperarVisible(allOf(
                    withId(R.id.textoRangoReportes),
                    withText(containsString(mesAnterior.inicio().toString()))));
            onView(withId(R.id.btnPeriodoMesReportes)).check(matches(isChecked()));

            onView(withId(R.id.textoTotalPlanificadasReportes))
                    .check(matches(withText(decimalConPuntoOComa("2.0", "2,0"))));
            onView(withId(R.id.textoTotalRealesReportes))
                    .check(matches(withText(decimalConPuntoOComa("1.5", "1,5"))));
            onView(withId(R.id.textoTotalCumplimientoReportes))
                    .check(matches(withText(containsString("75 %"))));
            onView(withId(R.id.textoTotalNoRealizadasReportes))
                    .check(matches(withText("1")));

            onView(withText("Cálculo")).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withText(containsString("Historia (Archivada)")))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));
            onView(withText(allOf(
                    decimalConPuntoOComa("0.0", "0,0"),
                    containsString("No real.: 1"))))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));

            onView(withId(R.id.botonExportarPdf)).check(matches(isEnabled()));
            onView(withId(R.id.botonExportarCsv)).check(matches(isEnabled()));

            // Navegar al mes actual: Estudios varios + Física sin plan (—), Cálculo REPROGRAMADA ausente
            onView(withId(R.id.btnPeriodoSiguienteReportes)).perform(click());
            Pruebas.esperarVisible(allOf(
                    withId(R.id.textoRangoReportes),
                    withText(containsString(mesActual.inicio().toString()))));
            onView(withId(R.id.textoTotalPlanificadasReportes))
                    .check(matches(withText(decimalConPuntoOComa("1.0", "1,0"))));
            onView(withText("Estudios varios")).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withText("Física")).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withText(containsString("\u2014"))).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withText("Cálculo")).check(doesNotExist());

            // Cambiar a vista Semana desde el mes navegado (conTipo mantiene fechaFoco del periodo visible)
            PeriodoEstadisticas semanaDesdeMes =
                    mesAnterior.siguiente().conTipo(PeriodoEstadisticas.Tipo.SEMANA);
            onView(withId(R.id.btnPeriodoSemanaReportes)).perform(click());
            Pruebas.esperarVisible(allOf(
                    withId(R.id.textoRangoReportes),
                    withText(containsString(semanaDesdeMes.inicio().toString()))));

            pressBack();
            Pruebas.esperarVisible(withId(R.id.barraInferior));
            verificarCuatroPestanasFijas();

            // Entrar desde Inicio (accion_reportes) directamente a la semana actual con actividad
            onView(withId(R.id.nav_inicio)).perform(click());
            Pruebas.esperarVisible(withId(R.id.accion_reportes));
            onView(withId(R.id.accion_reportes)).perform(click());
            Pruebas.esperarVisible(allOf(
                    withId(R.id.textoRangoReportes),
                    withText(containsString(semanaActual.inicio().toString()))));
            onView(withId(R.id.btnPeriodoSemanaReportes)).check(matches(isChecked()));
            onView(withText("Estudios varios")).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withText("Física")).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withText("Cálculo")).check(doesNotExist());

            pressBack();
            Pruebas.esperarVisible(withId(R.id.barraInferior));
            verificarCuatroPestanasFijas();
        }
    }

    @Test
    public void entradaDesdeInicioEnPeriodoSinDatosDeshabilitaExportacionYRotacionPreservaEstado()
            throws Exception {
        Fecha hoy = Pruebas.app().reloj().hoy();
        PeriodoEstadisticas semanaActual = PeriodoEstadisticas.semanal(hoy);
        PeriodoEstadisticas mesAnterior = PeriodoEstadisticas.mensual(hoy).anterior();

        try (ActivityScenario<MainActivity> escenario =
                ActivityScenario.launch(MainActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.barraInferior));
            verificarCuatroPestanasFijas();

            Pruebas.esperarVisible(withId(R.id.accion_reportes));
            onView(withId(R.id.accion_reportes)).perform(click());

            Pruebas.esperarVisible(allOf(
                    withId(R.id.textoRangoReportes),
                    withText(containsString(semanaActual.inicio().toString()))));
            Pruebas.esperarVisible(withId(R.id.textoSinDatosReportes));
            onView(withId(R.id.botonExportarPdf)).check(matches(not(isEnabled())));
            onView(withId(R.id.botonExportarCsv)).check(matches(not(isEnabled())));

            pressBack();
            Pruebas.esperarVisible(withId(R.id.barraInferior));
            verificarCuatroPestanasFijas();
        }

        // Rotación (recreate) tras cambiar a Mes y navegar al periodo anterior (RNF-10)
        try (ActivityScenario<ReportesActivity> escenarioReportes = ActivityScenario.launch(
                ReportesActivity.crearIntent(
                        ApplicationProvider.getApplicationContext(),
                        PeriodoEstadisticas.Tipo.SEMANA,
                        hoy))) {
            Pruebas.esperarVisible(withId(R.id.btnPeriodoMesReportes));
            onView(withId(R.id.btnPeriodoMesReportes)).perform(click());
            onView(withId(R.id.btnPeriodoAnteriorReportes)).perform(click());
            Pruebas.esperarVisible(allOf(
                    withId(R.id.textoRangoReportes),
                    withText(containsString(mesAnterior.inicio().toString()))));

            escenarioReportes.recreate();

            Pruebas.esperarVisible(allOf(
                    withId(R.id.textoRangoReportes),
                    withText(containsString(mesAnterior.inicio().toString()))));
            onView(withId(R.id.btnPeriodoMesReportes)).check(matches(isChecked()));
            onView(withId(R.id.botonExportarPdf)).check(matches(not(isEnabled())));
            onView(withId(R.id.botonExportarCsv)).check(matches(not(isEnabled())));
        }
    }

    private static void verificarCuatroPestanasFijas() {
        onView(withId(R.id.nav_inicio)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_materias)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_calendario)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_estadisticas)).check(matches(isDisplayed()));
    }

    private static Matcher<String> decimalConPuntoOComa(String conPunto, String conComa) {
        return anyOf(containsString(conPunto), containsString(conComa));
    }
}
