package io.github.jhoel0521.studyplan.adapter.in.ui.calendario;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.adapter.in.ui.StudyPlanApplication;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.Pruebas;

/**
 * Pruebas E2E Espresso de {@link CalendarioActivity} (HU-07, HU-08, HU-13, HU-14, HU-16 a HU-21,
 * RNF-06, RNF-10).
 */
@RunWith(AndroidJUnit4.class)
public class CalendarioActivityEspressoTest {

    private Fecha hoy;
    private Materia calculo;
    private Materia fisicaArchivada;

    @Before
    public void preparar() {
        Pruebas.reiniciarEstado();
        Context ctx = ApplicationProvider.getApplicationContext();
        ctx.getSharedPreferences("preferencias_calendario_ui", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();

        StudyPlanApplication app = Pruebas.app();
        app.configurarPerfil().ejecutar("Ana", 20);
        hoy = app.reloj().hoy();

        calculo = app.registrarMateria().ejecutar("Cálculo", 0xFF1565C0, "Prof. A").valor();
        Materia fisica = app.registrarMateria().ejecutar("Física", 0xFF2E7D32, "Prof. B").valor();
        fisicaArchivada = app.archivarMateria().ejecutar(fisica.id()).valor();
    }

    @After
    public void limpiar() {
        Pruebas.reiniciarEstado();
    }

    @Test
    public void arrancaEnSemanaRecuerdaVistaMesYConservaFocoAlCambiarDeVista() throws Exception {
        try (ActivityScenario<CalendarioActivity> escenario =
                     ActivityScenario.launch(CalendarioActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.botonVistaSemana));
            onView(withId(R.id.botonVistaSemana)).check(matches(isChecked()));

            // Cambia a Mes, retrocede 2 meses y cambia a Semana conservando el foco (1.ª semana)
            onView(withId(R.id.botonVistaMes)).perform(click());
            Pruebas.esperarVisible(withId(R.id.contenedorVistaMes));
            onView(withId(R.id.botonPeriodoAnterior)).perform(click());
            onView(withId(R.id.botonPeriodoAnterior)).perform(click());
            onView(withId(R.id.botonVistaSemana)).perform(click());
            Pruebas.esperarVisible(withId(R.id.contenedorVistaHorario));

            // Regresa a Mes para persistir MES como última vista
            onView(withId(R.id.botonVistaMes)).perform(click());
            Pruebas.esperarVisible(withId(R.id.contenedorVistaMes));
        }

        // Al reabrir, recuerda MES
        try (ActivityScenario<CalendarioActivity> escenario =
                     ActivityScenario.launch(CalendarioActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.contenedorVistaMes));
            onView(withId(R.id.botonVistaMes)).check(matches(isChecked()));
        }
    }

    @Test
    public void vistaMesListaSesionesAlTocarDiaYLeyendaMuestraTodosLosEstados() throws Exception {
        StudyPlanApplication app = Pruebas.app();
        app.planificarSesion().ejecutar(
                calculo.id(),
                hoy,
                Hora.de(9, 0).valor(),
                Hora.de(10, 0).valor(),
                "Repaso derivadas");

        try (ActivityScenario<CalendarioActivity> escenario =
                     ActivityScenario.launch(CalendarioActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.botonVistaMes));
            onView(withId(R.id.botonVistaMes)).perform(click());
            Pruebas.esperarVisible(withId(R.id.listaCeldasMes));

            // Tocar el día de hoy en la cuadrícula mensual lista sus sesiones debajo (HU-16)
            onView(allOf(withId(R.id.tarjetaCeldaMes), withContentDescription(containsString(hoy.iso()))))
                    .perform(click());
            Pruebas.esperarVisible(allOf(withId(R.id.nombreMateriaItem), withText("Cálculo")));

            // Abrir leyenda de estados (HU-19)
            onView(withId(R.id.accion_leyenda)).perform(click());
            Pruebas.esperarVisible(withText(R.string.calendario_leyenda_titulo));
            onView(withText(R.string.estado_sesion_no_realizada)).check(matches(isDisplayed()));
            onView(withText(R.string.estado_sesion_reprogramada)).check(matches(isDisplayed()));
            onView(withText(android.R.string.ok)).perform(click());
        }
    }

    @Test
    public void pulsacionLargaEnHoyAbreNuevaSesionYEnPasadoNoCrea() throws Exception {
        Fecha ayer = hoy.sumarDias(-1);
        try (ActivityScenario<CalendarioActivity> escenario =
                     ActivityScenario.launch(CalendarioActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.botonVistaDia));
            onView(withId(R.id.botonVistaDia)).perform(click());
            Pruebas.esperarVisible(withId(R.id.capaFranjasHorario));

            // Pulsación larga en hoy a las 15:00 abre SesionFormularioActivity con 15:00 - 16:00 (HU-20)
            String descHoy15 = ApplicationProvider.getApplicationContext().getString(
                    R.string.calendario_franja_libre_desc, "15:00", hoy.iso());
            onView(withContentDescription(descHoy15)).perform(scrollTo(), longClick());
            Pruebas.esperarVisible(withId(R.id.campoHoraInicio));
            onView(withId(R.id.campoFecha)).check(matches(withText(hoy.iso())));
            onView(withId(R.id.campoHoraInicio)).check(matches(withText("15:00")));
            onView(withId(R.id.campoHoraFin)).check(matches(withText("16:00")));
            pressBack();

            // Retrocede a ayer: pulsación larga NO debe abrir el formulario (HU-20)
            Pruebas.esperarVisible(withId(R.id.botonPeriodoAnterior));
            onView(withId(R.id.botonPeriodoAnterior)).perform(click());
            String descAyer15 = ApplicationProvider.getApplicationContext().getString(
                    R.string.calendario_franja_libre_desc, "15:00", ayer.iso());
            onView(withContentDescription(descAyer15)).perform(scrollTo(), longClick());
            onView(withId(R.id.campoHoraInicio)).check(doesNotExist());
        }
    }

    @Test
    public void filtrosPorMateriaArchivadaResumenPlanRealYRotacion() throws Exception {
        StudyPlanApplication app = Pruebas.app();
        app.planificarSesion().ejecutar(
                calculo.id(), hoy, Hora.de(9, 0).valor(), Hora.de(10, 0).valor(), "");
        app.sesionRepository().guardar(SesionEstudio.restaurar(
                null,
                fisicaArchivada.id(),
                hoy,
                Hora.de(11, 0).valor(),
                Hora.de(13, 0).valor(),
                120,
                120,
                CodigoEstadoSesion.COMPLETADA,
                "",
                null));

        try (ActivityScenario<CalendarioActivity> escenario =
                     ActivityScenario.launch(CalendarioActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.textoResumenPeriodo));
            // 3.0 h planificadas, 2.0 h reales
            onView(withId(R.id.textoResumenPeriodo)).check(matches(withText(containsString("3"))));

            // Abrir filtros y seleccionar Física (archivada)
            onView(withId(R.id.accion_filtros)).perform(click());
            Pruebas.esperarVisible(withId(R.id.selectorFiltroMateria));
            onView(withId(R.id.selectorFiltroMateria)).perform(click());
            onData(allOf(is(instanceOf(String.class)), is("Física")))
                    .inRoot(isPlatformPopup())
                    .perform(click());
            onView(withText(R.string.calendario_filtros_aplicar)).perform(click());

            // El botón de filtro activo aparece y Física queda visible, Cálculo oculto
            Pruebas.esperarVisible(withId(R.id.botonFiltroActivo));
            onView(withText("Física")).check(matches(isDisplayed()));
            onView(withText("Cálculo")).check(doesNotExist());

            // Rotación conserva el filtro y el periodo (RNF-10)
            escenario.recreate();
            Pruebas.esperarVisible(withId(R.id.botonFiltroActivo));
            onView(withText("Física")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void abreSesionNoRealizadaPasadaYReflejaColorEditadoDeMateria() throws Exception {
        StudyPlanApplication app = Pruebas.app();
        Fecha ayer = hoy.sumarDias(-1);
        SesionEstudio pasadaPendiente = SesionEstudio.restaurar(
                null,
                calculo.id(),
                ayer,
                Hora.de(10, 0).valor(),
                Hora.de(11, 0).valor(),
                60,
                null,
                CodigoEstadoSesion.PENDIENTE,
                "Pendiente vencida",
                null);
        app.sesionRepository().guardar(pasadaPendiente);

        // Editar color de Cálculo (HU-07)
        app.editarMateria().ejecutar(calculo.id(), "Cálculo", 0xFFD32F2F, "Prof. A");

        try (ActivityScenario<CalendarioActivity> escenario =
                     ActivityScenario.launch(CalendarioActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.botonVistaDia));
            onView(withId(R.id.botonVistaDia)).perform(click());
            onView(withId(R.id.botonPeriodoAnterior)).perform(click());

            String descBloque = String.format(
                    Locale.getDefault(), "%s", "Cálculo");
            Pruebas.esperarVisible(withText(descBloque));
            onView(withText(R.string.estado_sesion_no_realizada)).check(matches(isDisplayed()));

            // Tocar el bloque abre SesionDetalleActivity para registro tardío o reprogramación (HU-13/HU-14)
            onView(withText(descBloque)).perform(click());
            Pruebas.esperarVisible(withId(R.id.botonRegistrarTardio));
            onView(withId(R.id.botonReprogramarSesion)).check(matches(isDisplayed()));
        }
    }
}
