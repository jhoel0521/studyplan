package io.github.jhoel0521.studyplan.adapter.in.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;

/**
 * Pruebas E2E de sesiones de estudio en pantalla: planificación, registro pasado, completar el mismo
 * día, registro tardío desde No realizada, reprogramación con enlace a destino, eliminación
 * protegida y rotación de pantalla.
 *
 * <p>Verifica: HU-10, HU-11, HU-12, HU-13, HU-14, HU-15, RN-04, RN-05, RN-06, RN-12 y RNF-10.
 */
@RunWith(AndroidJUnit4.class)
public class SesionesEspressoTest {

    private static final int COLOR = 0xFF1E88E5;
    private Materia materiaCalculo;

    @Before
    public void preparar() {
        Pruebas.reiniciarEstado();
        Pruebas.app().configurarPerfil().ejecutar("Ana", 20);
        materiaCalculo = Pruebas.app().registrarMateria().ejecutar("Cálculo", COLOR, null).valor();
    }

    @After
    public void limpiar() {
        Pruebas.reiniciarEstado();
    }

    private static Intent intentoFormulario(String modo, String fechaIso) {
        Intent i = new Intent(ApplicationProvider.getApplicationContext(), SesionFormularioActivity.class);
        i.putExtra(SesionFormularioActivity.EXTRA_MODO, modo);
        if (fechaIso != null) {
            i.putExtra(SesionFormularioActivity.EXTRA_FECHA, fechaIso);
        }
        return i;
    }

    private static Intent intentoDetalle(long sesionId) {
        Intent i = new Intent(ApplicationProvider.getApplicationContext(), SesionDetalleActivity.class);
        i.putExtra(SesionDetalleActivity.EXTRA_SESION_ID, sesionId);
        return i;
    }

    private static void elegirMateria(String nombre) throws InterruptedException {
        Pruebas.esperarVisible(withId(R.id.selectorMateria));
        onView(withId(R.id.selectorMateria)).perform(scrollTo(), click());
        onView(withText(nombre)).inRoot(isPlatformPopup()).perform(click());
    }

    @Test
    @SuppressWarnings("unused")
    public void planificarSesionValidaLaGuardaEnRepositorio() throws InterruptedException {
        Fecha manana = Pruebas.app().reloj().hoy().sumarDias(1);
        try (ActivityScenario<SesionFormularioActivity> escenario = ActivityScenario.launch(
                intentoFormulario(SesionFormularioActivity.MODO_PLANIFICAR, manana.iso()))) {
            elegirMateria("Cálculo");
            onView(withId(R.id.campoHoraInicio)).perform(scrollTo(), replaceText("10:00"), closeSoftKeyboard());
            onView(withId(R.id.campoHoraFin)).perform(scrollTo(), replaceText("11:30"), closeSoftKeyboard());
            onView(withId(R.id.campoNotas)).perform(scrollTo(), replaceText("Integrales"), closeSoftKeyboard());
            onView(withId(R.id.botonGuardarSesion)).perform(scrollTo(), click());

            long limite = System.currentTimeMillis() + 5000;
            while (Pruebas.app().sesionRepository().listarPorFecha(manana).isEmpty()
                    && System.currentTimeMillis() < limite) {
                Thread.sleep(100);
            }
        }
        var guardadas = Pruebas.app().sesionRepository().listarPorFecha(manana);
        assertEquals(1, guardadas.size());
        assertEquals(CodigoEstadoSesion.PENDIENTE, guardadas.get(0).codigoEstado());
        assertEquals(90, guardadas.get(0).duracionPlanificadaMinutos());
    }

    @Test
    @SuppressWarnings("unused")
    public void horarioFinNoPosteriorMuestraErrorSinGuardar() throws InterruptedException {
        Fecha manana = Pruebas.app().reloj().hoy().sumarDias(1);
        try (ActivityScenario<SesionFormularioActivity> escenario = ActivityScenario.launch(
                intentoFormulario(SesionFormularioActivity.MODO_PLANIFICAR, manana.iso()))) {
            elegirMateria("Cálculo");
            onView(withId(R.id.campoHoraInicio)).perform(scrollTo(), replaceText("10:00"), closeSoftKeyboard());
            onView(withId(R.id.campoHoraFin)).perform(scrollTo(), replaceText("09:30"), closeSoftKeyboard());
            onView(withId(R.id.botonGuardarSesion)).perform(scrollTo(), click());
            Pruebas.esperarVisible(withText(R.string.error_sesion_horario_fin_no_posterior));
        }
        assertEquals(0, Pruebas.app().sesionRepository().listarPorFecha(manana).size());
    }

    @Test
    @SuppressWarnings("unused")
    public void cruceHorarioMuestraLaSesionEnConflicto() throws InterruptedException {
        Fecha manana = Pruebas.app().reloj().hoy().sumarDias(1);
        Pruebas.app().planificarSesion().ejecutar(
                materiaCalculo.id(),
                manana,
                Hora.de(10, 0).valor(),
                Hora.de(12, 0).valor(),
                null);

        try (ActivityScenario<SesionFormularioActivity> escenario = ActivityScenario.launch(
                intentoFormulario(SesionFormularioActivity.MODO_PLANIFICAR, manana.iso()))) {
            elegirMateria("Cálculo");
            onView(withId(R.id.campoHoraInicio)).perform(scrollTo(), replaceText("11:00"), closeSoftKeyboard());
            onView(withId(R.id.campoHoraFin)).perform(scrollTo(), replaceText("13:00"), closeSoftKeyboard());
            onView(withId(R.id.botonGuardarSesion)).perform(scrollTo(), click());
            Pruebas.esperarVisible(withText(containsString("Se cruza con «Cálculo»")));
        }
        assertEquals(1, Pruebas.app().sesionRepository().listarPorFecha(manana).size());
    }

    @Test
    @SuppressWarnings("unused")
    public void registrarSesionPasadaDentroDeTresDiasLaGuardaYCuatroDiasSeRechaza()
            throws InterruptedException {
        Fecha haceDosDias = Pruebas.app().reloj().hoy().sumarDias(-2);
        Fecha haceCuatroDias = Pruebas.app().reloj().hoy().sumarDias(-4);

        try (ActivityScenario<SesionFormularioActivity> escenario = ActivityScenario.launch(
                intentoFormulario(SesionFormularioActivity.MODO_PLANIFICAR, haceCuatroDias.iso()))) {
            onView(withId(R.id.radioPasada)).perform(scrollTo(), click());
            elegirMateria("Cálculo");
            onView(withId(R.id.campoHoraInicio)).perform(scrollTo(), replaceText("14:00"), closeSoftKeyboard());
            onView(withId(R.id.campoHoraFin)).perform(scrollTo(), replaceText("15:00"), closeSoftKeyboard());
            onView(withId(R.id.botonGuardarSesion)).perform(scrollTo(), click());
            Pruebas.esperarVisible(withText(R.string.error_sesion_pasada_fuera_ventana));

            onView(withId(R.id.campoFecha)).perform(scrollTo(), replaceText(haceDosDias.iso()), closeSoftKeyboard());
            onView(withId(R.id.campoDuracionReal)).perform(scrollTo(), replaceText("55"), closeSoftKeyboard());
            onView(withId(R.id.botonGuardarSesion)).perform(scrollTo(), click());

            long limite = System.currentTimeMillis() + 5000;
            while (Pruebas.app().sesionRepository().listarPorFecha(haceDosDias).isEmpty()
                    && System.currentTimeMillis() < limite) {
                Thread.sleep(100);
            }
        }
        var guardadas = Pruebas.app().sesionRepository().listarPorFecha(haceDosDias);
        assertEquals(1, guardadas.size());
        assertEquals(CodigoEstadoSesion.COMPLETADA_TARDIA, guardadas.get(0).codigoEstado());
        assertEquals(Integer.valueOf(55), guardadas.get(0).duracionRealMinutos());
    }

    @Test
    @SuppressWarnings("unused")
    public void completarSesionDeHoyConDuracionEditadaActualizaElDetalle() throws InterruptedException {
        Fecha hoy = Pruebas.app().reloj().hoy();
        SesionEstudio s = Pruebas.app().planificarSesion().ejecutar(
                materiaCalculo.id(),
                hoy,
                Hora.de(18, 0).valor(),
                Hora.de(19, 30).valor(),
                "Guía 1").sesion();

        try (ActivityScenario<SesionDetalleActivity> escenario =
                     ActivityScenario.launch(intentoDetalle(s.id()))) {
            Pruebas.esperarVisible(withId(R.id.botonCompletarSesion));
            onView(withId(R.id.campoDuracionRealDetalle))
                    .perform(scrollTo(), replaceText("75"), closeSoftKeyboard());
            onView(withId(R.id.botonCompletarSesion)).perform(scrollTo(), click());

            Pruebas.esperarVisible(withText(R.string.estado_sesion_completada));
            Pruebas.esperarVisible(allOf(
                    withId(R.id.textoDuracionRealDetalle),
                    withText(containsString("75"))));
            onView(withId(R.id.botonEliminarSesion)).check(matches(not(isDisplayed())));
        }
    }

    @Test
    @SuppressWarnings("unused")
    public void registrarTardioEnNoRealizadaDentroDePlazo() throws InterruptedException {
        Fecha ayer = Pruebas.app().reloj().hoy().sumarDias(-1);
        SesionEstudio vencida = SesionEstudio.planificar(
                materiaCalculo.id(),
                ayer,
                ayer,
                Hora.de(9, 0).valor(),
                Hora.de(10, 0).valor(),
                null,
                null).valor().vencer().valor();
        SesionEstudio guardada = Pruebas.app().sesionRepository().guardar(vencida);

        try (ActivityScenario<SesionDetalleActivity> escenario =
                     ActivityScenario.launch(intentoDetalle(guardada.id()))) {
            Pruebas.esperarVisible(withText(R.string.estado_sesion_no_realizada));
            Pruebas.esperarVisible(withId(R.id.botonRegistrarTardio));
            onView(withId(R.id.campoDuracionRealDetalle))
                    .perform(scrollTo(), replaceText("50"), closeSoftKeyboard());
            onView(withId(R.id.botonRegistrarTardio)).perform(scrollTo(), click());

            Pruebas.esperarVisible(withText(R.string.estado_sesion_completada_tardia));
            Pruebas.esperarVisible(allOf(
                    withId(R.id.textoDuracionRealDetalle),
                    withText(containsString("50"))));
        }
    }

    @Test
    @SuppressWarnings("unused")
    public void reprogramarSesionConservaEstadoYEnlaceNavegableADestino() throws InterruptedException {
        Fecha manana = Pruebas.app().reloj().hoy().sumarDias(1);
        Fecha pasado = Pruebas.app().reloj().hoy().sumarDias(2);
        SesionEstudio original = Pruebas.app().planificarSesion().ejecutar(
                materiaCalculo.id(),
                manana,
                Hora.de(10, 0).valor(),
                Hora.de(11, 0).valor(),
                "Original").sesion();

        try (ActivityScenario<SesionDetalleActivity> escenario =
                     ActivityScenario.launch(intentoDetalle(original.id()))) {
            Pruebas.esperarVisible(withId(R.id.botonReprogramarSesion));
            onView(withId(R.id.botonReprogramarSesion)).perform(scrollTo(), click());

            Pruebas.esperarVisible(withId(R.id.campoFecha));
            onView(withId(R.id.campoFecha))
                    .perform(scrollTo(), replaceText(pasado.iso()), closeSoftKeyboard());
            onView(withId(R.id.campoHoraInicio))
                    .perform(scrollTo(), replaceText("16:00"), closeSoftKeyboard());
            onView(withId(R.id.campoHoraFin))
                    .perform(scrollTo(), replaceText("17:00"), closeSoftKeyboard());
            onView(withId(R.id.botonGuardarSesion)).perform(scrollTo(), click());

            Pruebas.esperarVisible(withText(R.string.estado_sesion_reprogramada));
            Pruebas.esperarVisible(withId(R.id.botonVerReprogramada));
            onView(withId(R.id.botonVerReprogramada)).perform(scrollTo(), click());

            Pruebas.esperarVisible(withText(R.string.estado_sesion_pendiente));
            Pruebas.esperarVisible(withText(containsString(pasado.iso())));
        }
    }

    @Test
    @SuppressWarnings("unused")
    public void eliminarSesionPendienteConConfirmacionLaBorra() throws InterruptedException {
        Fecha manana = Pruebas.app().reloj().hoy().sumarDias(1);
        SesionEstudio s = Pruebas.app().planificarSesion().ejecutar(
                materiaCalculo.id(),
                manana,
                Hora.de(8, 0).valor(),
                Hora.de(9, 0).valor(),
                null).sesion();

        try (ActivityScenario<SesionDetalleActivity> escenario =
                     ActivityScenario.launch(intentoDetalle(s.id()))) {
            Pruebas.esperarVisible(withId(R.id.botonEliminarSesion));
            onView(withId(R.id.botonEliminarSesion)).perform(scrollTo(), click());
            Pruebas.esperarVisible(withId(android.R.id.button1));
            onView(withId(android.R.id.button1)).perform(click());

            long limite = System.currentTimeMillis() + 5000;
            while (Pruebas.app().sesionRepository().buscarPorId(s.id()).isPresent()
                    && System.currentTimeMillis() < limite) {
                Thread.sleep(100);
            }
        }
        assertFalse(Pruebas.app().sesionRepository().buscarPorId(s.id()).isPresent());
    }

    @Test
    @SuppressWarnings("unused")
    public void rotarFormularioConservaDatosIngresados() throws InterruptedException {
        Fecha hoy = Pruebas.app().reloj().hoy();
        try (ActivityScenario<SesionFormularioActivity> escenario = ActivityScenario.launch(
                intentoFormulario(SesionFormularioActivity.MODO_PLANIFICAR, hoy.iso()))) {
            onView(withId(R.id.radioPasada)).perform(scrollTo(), click());
            elegirMateria("Cálculo");
            onView(withId(R.id.campoHoraInicio)).perform(scrollTo(), replaceText("10:00"), closeSoftKeyboard());
            onView(withId(R.id.campoHoraFin)).perform(scrollTo(), replaceText("11:00"), closeSoftKeyboard());
            onView(withId(R.id.campoDuracionReal)).perform(scrollTo(), replaceText("45"), closeSoftKeyboard());
            onView(withId(R.id.campoNotas)).perform(scrollTo(), replaceText("Repaso"), closeSoftKeyboard());

            escenario.recreate();

            Pruebas.esperarVisible(withId(R.id.radioPasada));
            onView(withId(R.id.radioPasada)).check(matches(isChecked()));
            onView(withId(R.id.selectorMateria)).check(matches(withText("Cálculo")));
            onView(withId(R.id.campoHoraInicio)).check(matches(withText("10:00")));
            onView(withId(R.id.campoHoraFin)).check(matches(withText("11:00")));
            onView(withId(R.id.campoDuracionReal)).check(matches(withText("45")));
            onView(withId(R.id.campoNotas)).check(matches(withText("Repaso")));
        }
    }
}
