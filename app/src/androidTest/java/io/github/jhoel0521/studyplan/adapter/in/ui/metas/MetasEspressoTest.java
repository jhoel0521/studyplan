package io.github.jhoel0521.studyplan.adapter.in.ui.metas;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.readmodel.MetaAlcanzada;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.adapter.in.ui.StudyPlanApplication;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.Pruebas;
import io.github.jhoel0521.studyplan.adapter.in.ui.configuracion.MainActivity;
import io.github.jhoel0521.studyplan.adapter.in.ui.materias.MateriasActivity;

/**
 * Pruebas E2E Espresso de metas semanales, «Estudiar ahora», «Cambiar de materia», felicitación
 * única e historial ({@code HU-25} a {@code HU-28}, {@code HU-47}, {@code HU-48}, {@code RN-16},
 * {@code RN-32}, {@code RN-33}).
 */
@RunWith(AndroidJUnit4.class)
public class MetasEspressoTest {

    @Before
    public void preparar() {
        Pruebas.reiniciarEstado();
        StudyPlanApplication app = Pruebas.app();
        app.configurarPerfil().ejecutar("Ana", 10);
    }

    @After
    public void limpiar() {
        Pruebas.reiniciarEstado();
    }

    @Test
    public void estudiarAhoraConEstudiosVariosYCambioDeMateria() throws Exception {
        Pruebas.app().registrarMateria().ejecutar("Matemáticas", 0xFF1565C0, null);
        Pruebas.app().registrarMateria().ejecutar("Informática", 0xFF2E7D32, null);

        try (ActivityScenario<MainActivity> escenario = ActivityScenario.launch(MainActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.botonEstudiarAhora));
            onView(withId(R.id.botonEstudiarAhora)).perform(click());

            Pruebas.esperarVisible(withText(R.string.materia_general_nombre));
            onView(withText(R.string.materia_general_nombre)).perform(click());
            onView(withId(R.id.botonComenzarSelector)).perform(click());

            Pruebas.esperarVisible(withId(R.id.nombreMateriaEnCurso));
            onView(withId(R.id.nombreMateriaEnCurso))
                    .check(matches(withText(R.string.materia_general_nombre)));

            onView(withId(R.id.botonCambiarMateria)).perform(click());
            Pruebas.esperarVisible(withText("Informática"));
            onView(withText("Informática")).perform(click());
            onView(withId(R.id.botonComenzarSelector)).perform(click());

            Pruebas.esperarVisible(withId(R.id.nombreMateriaEnCurso));
            onView(withId(R.id.nombreMateriaEnCurso)).check(matches(withText("Informática")));
            onView(withId(R.id.botonDetenerEnCurso)).perform(click());
        }
    }

    /** HU-47 «Materia elegida visible»: tocar una fila solo la marca; la sesión empieza con «Comenzar». */
    @Test
    public void tocarUnaMateriaNoIniciaHastaComenzar() throws Exception {
        try (ActivityScenario<MainActivity> escenario = ActivityScenario.launch(MainActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.botonEstudiarAhora));
            onView(withId(R.id.botonEstudiarAhora)).perform(click());

            Pruebas.esperarVisible(withId(R.id.botonComenzarSelector));
            onView(withId(R.id.botonComenzarSelector)).check(matches(not(isEnabled())));
            onView(withText(R.string.materia_general_nombre)).perform(click());
            onView(withId(R.id.tituloSelectorEstudio)).check(matches(isDisplayed()));
            onView(withId(R.id.botonComenzarSelector)).check(matches(isEnabled()));

            onView(withId(R.id.botonComenzarSelector)).perform(click());
            Pruebas.esperarVisible(withId(R.id.nombreMateriaEnCurso));
            onView(withId(R.id.botonDetenerEnCurso)).perform(click());
        }
    }

    /** HU-02 «Sesión en curso visible» y HU-47 «Ya hay una sesión En curso»: Inicio ofrece volver a ella. */
    @Test
    public void conSesionEnCursoInicioOfreceVolverYNoPideMateria() throws Exception {
        StudyPlanApplication app = Pruebas.app();
        app.estudiarAhora().ejecutar(null, TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL).valor();
        app.contadorActividades().consumirEvaluacionRecuperacion();

        try (ActivityScenario<MainActivity> escenario = ActivityScenario.launch(MainActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.tarjetaSesionEnCurso));
            onView(withId(R.id.botonEstudiarAhora)).check(matches(withText(R.string.inicio_volver_sesion)));

            onView(withId(R.id.botonEstudiarAhora)).perform(click());
            Pruebas.esperarVisible(withId(R.id.nombreMateriaEnCurso));
            onView(withId(R.id.tituloSelectorEstudio)).check(doesNotExist());
            onView(withId(R.id.botonDetenerEnCurso)).perform(click());
        }
    }

    @Test
    public void estudiarAhoraCreandoNuevaMateriaEnElMismoPaso() throws Exception {
        try (ActivityScenario<MainActivity> escenario = ActivityScenario.launch(MainActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.botonEstudiarAhora));
            onView(withId(R.id.botonEstudiarAhora)).perform(click());

            Pruebas.esperarVisible(withId(R.id.botonMostrarNuevaMateria));
            onView(withId(R.id.botonMostrarNuevaMateria)).perform(click());

            Pruebas.esperarVisible(withId(R.id.campoNombreNuevaMateria));
            onView(withId(R.id.campoNombreNuevaMateria))
                    .perform(replaceText("Biología"), closeSoftKeyboard());
            onView(withId(R.id.botonComenzarNuevaMateria)).perform(click());

            Pruebas.esperarVisible(withId(R.id.nombreMateriaEnCurso));
            onView(withId(R.id.nombreMateriaEnCurso)).check(matches(withText("Biología")));
            onView(withId(R.id.botonDetenerEnCurso)).perform(click());
        }
    }

    @Test
    public void muestraFelicitacionConDosMetasYRegresaAInicio() throws Exception {
        Intent intento = FelicitacionMetasActivity.crearIntent(
                ApplicationProvider.getApplicationContext(),
                Arrays.asList(
                        new MetaAlcanzada(null, "", 10.0, 10.5),
                        new MetaAlcanzada(1L, "Álgebra", 4.0, 4.2)));

        try (ActivityScenario<FelicitacionMetasActivity> escenario = ActivityScenario.launch(intento)) {
            Pruebas.esperarVisible(withText(R.string.felicitacion_titulo));
            onView(withText(containsString("10"))).check(matches(isDisplayed()));
            onView(withText(containsString("Álgebra"))).check(matches(isDisplayed()));
            onView(withId(R.id.botonVolverInicioFelicitacion)).perform(click());
            Pruebas.esperarVisible(withId(R.id.botonEstudiarAhora));
        }
    }

    @Test
    public void muestraMetaPorMateriaEnListaYActualizaHistorial() throws Exception {
        StudyPlanApplication app = Pruebas.app();
        Materia fisica = app.registrarMateria().ejecutar("Física", 0xFF1565C0, null, 4.0).valor();
        Fecha hoy = app.reloj().hoy();
        long sesionId = app.planificarSesion().ejecutar(
                fisica.id(), hoy, Hora.de(9, 0).valor(), Hora.de(12, 0).valor(), "Repaso")
                .sesion().id();
        app.completarSesion().ejecutar(sesionId, 180);

        try (ActivityScenario<MateriasActivity> escenario = ActivityScenario.launch(MateriasActivity.class)) {
            Pruebas.esperarVisible(withText("Física"));
            Pruebas.esperarVisible(withText(containsString("3 / 4 h")));
        }

        app.definirMetaSemanal().ejecutar(14.0);
        try (ActivityScenario<HistorialMetasActivity> escenario =
                     ActivityScenario.launch(HistorialMetasActivity.class)) {
            Pruebas.esperarVisible(withText(containsString("3 / 14 h")));
        }
    }
}
