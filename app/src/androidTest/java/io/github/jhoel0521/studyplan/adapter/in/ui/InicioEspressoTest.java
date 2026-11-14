package io.github.jhoel0521.studyplan.adapter.in.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Materia;

/**
 * Pruebas E2E de la pantalla de Inicio completa: progreso semanal real, próxima sesión pendiente,
 * sesiones del día actual y acceso rápido «+ Nueva sesión».
 *
 * <p>Verifica: HU-02 y RNF-06.
 */
@RunWith(AndroidJUnit4.class)
public class InicioEspressoTest {

    private static final int COLOR = 0xFF43A047;

    @Before
    public void conPerfil() {
        Pruebas.reiniciarEstado();
        Pruebas.app().configurarPerfil().ejecutar("Ana", 10);
    }

    @After
    public void restablecer() {
        Pruebas.reiniciarEstado();
    }

    @Test
    @SuppressWarnings("unused")
    public void sinMateriasActivasNuevaSesionMuestraAvisoConAccesoAMaterias()
            throws InterruptedException {
        try (ActivityScenario<MainActivity> escenario = ActivityScenario.launch(MainActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.botonNuevaSesion));
            onView(withId(R.id.botonNuevaSesion)).perform(scrollTo(), click());
            Pruebas.esperarVisible(withText(R.string.inicio_sin_materias_aviso));
        }
    }

    @Test
    @SuppressWarnings("unused")
    public void inicioMuestraProgresoRealProximaSesionYSesionesDeHoy()
            throws InterruptedException {
        Materia algebra = Pruebas.app().registrarMateria().ejecutar("Álgebra", COLOR, null).valor();
        Fecha hoy = Pruebas.app().reloj().hoy();
        var sesionHoy = Pruebas.app().planificarSesion().ejecutar(
                algebra.id(),
                hoy,
                Hora.de(8, 0).valor(),
                Hora.de(9, 30).valor(),
                null).sesion();
        Pruebas.app().completarSesion().ejecutar(sesionHoy.id(), 90);

        Pruebas.app().planificarSesion().ejecutar(
                algebra.id(),
                hoy.sumarDias(1),
                Hora.de(15, 0).valor(),
                Hora.de(16, 0).valor(),
                null);

        try (ActivityScenario<MainActivity> escenario = ActivityScenario.launch(MainActivity.class)) {
            Pruebas.esperarVisible(withText(containsString("Ana")));
            Pruebas.esperarVisible(withText("1,5 / 10 h"));
            Pruebas.esperarVisible(withId(R.id.tarjetaProximaSesion));
            Pruebas.esperarVisible(withText(R.string.estado_sesion_completada));

            onView(withId(R.id.botonNuevaSesion)).perform(scrollTo(), click());
            Pruebas.esperarVisible(withId(R.id.campoFecha));
            onView(withId(R.id.campoFecha)).check(matches(withText(hoy.iso())));
        }
    }
}
