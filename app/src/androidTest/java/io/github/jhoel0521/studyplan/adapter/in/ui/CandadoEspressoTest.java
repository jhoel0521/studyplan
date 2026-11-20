package io.github.jhoel0521.studyplan.adapter.in.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.adapter.out.system.BloqueoPantallaSistema;
import io.github.jhoel0521.studyplan.domain.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.TecnicaEstudio;
import io.github.jhoel0521.studyplan.domain.TipoEventoEstudio;

/**
 * Pruebas E2E del candado de la sesión: confirmación, primer uso con permiso, «No volver a preguntar» y
 * bloqueo real de la pantalla (HU-50, RNF-18). Corren solo en el emulador: activan el administrador del
 * dispositivo con {@code dpm set-active-admin}, que el sistema solo admite en una app de depuración.
 */
@RunWith(AndroidJUnit4.class)
public class CandadoEspressoTest {

    private static final long ESPERA_MS = 10_000L;
    private static final String ADMINISTRADOR =
            "io.github.jhoel0521.studyplan/.adapter.out.system.ReceptorAdministradorBloqueo";

    private Context contexto;
    private BloqueoPantallaSistema sistema;

    @Before
    public void preparar() throws Exception {
        contexto = ApplicationProvider.getApplicationContext();
        sistema = new BloqueoPantallaSistema(contexto);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .grantRuntimePermission(contexto.getPackageName(), Manifest.permission.POST_NOTIFICATIONS);
        }
        encenderPantalla();
        Pruebas.reiniciarEstado();
        Pruebas.app().configurarPerfil().ejecutar("Prueba", 20);
        quitarPermisoYEsperar();
    }

    @After
    public void limpiar() throws Exception {
        quitarPermisoYEsperar();
        encenderPantalla();
        Pruebas.reiniciarEstado();
    }

    /** {@code removeActiveAdmin} es asíncrono: se espera a que el sistema lo procese. */
    @SuppressWarnings("BusyWait") // sondeo con tope
    private void quitarPermisoYEsperar() throws InterruptedException {
        sistema.quitarPermiso();
        long limite = System.currentTimeMillis() + ESPERA_MS;
        while (sistema.estaActivo() && System.currentTimeMillis() < limite) {
            Thread.sleep(200);
        }
        assertFalse("El permiso de administrador debe quedar retirado", sistema.estaActivo());
    }

    @Test
    public void cancelarLaConfirmacionNoBloquea() throws Exception {
        long id = iniciar();
        try (ActivityScenario<SesionEnCursoActivity> escenario = lanzar(id)) {
            Pruebas.esperarVisible(withId(R.id.botonBloquear));

            onView(withId(R.id.botonBloquear)).perform(click());
            onView(withText(R.string.bloqueo_confirmar_titulo)).check(matches(isDisplayed()));
            onView(withText(R.string.bloqueo_confirmar_mensaje)).check(matches(isDisplayed()));
            onView(withText(R.string.bloqueo_no_volver_a_preguntar)).check(matches(isDisplayed()));
            onView(withText(R.string.cancelar)).perform(click());

            Thread.sleep(2_000);
            assertEquals(0, contar(id, TipoEventoEstudio.BLOQUEO_PANTALLA));
            onView(withId(R.id.textoCronometro)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void elPrimerUsoSinPermisoExplicaElPermisoYSiSeCancelaNoBloquea() throws Exception {
        assertFalse(sistema.estaActivo());
        long id = iniciar();
        try (ActivityScenario<SesionEnCursoActivity> escenario = lanzar(id)) {
            Pruebas.esperarVisible(withId(R.id.botonBloquear));

            onView(withId(R.id.botonBloquear)).perform(click());
            onView(withText(R.string.bloqueo_confirmar_bloquear)).perform(click());

            onView(withText(R.string.bloqueo_permiso_titulo)).check(matches(isDisplayed()));
            onView(withText(R.string.bloqueo_pantalla_explicacion)).check(matches(isDisplayed()));
            onView(withText(R.string.cancelar)).perform(click());

            Thread.sleep(2_000);
            assertEquals(0, contar(id, TipoEventoEstudio.BLOQUEO_PANTALLA));
            assertFalse(sistema.estaActivo());
        }
    }

    @Test
    public void conPermisoBloqueaLaPantallaYConNoVolverAPreguntarYaNoSeConfirma() throws Exception {
        Pruebas.comando("dpm set-active-admin --user 0 " + ADMINISTRADOR);
        assertTrue("El emulador debe aceptar el administrador de depuración", sistema.estaActivo());
        long id = iniciar();
        try (ActivityScenario<SesionEnCursoActivity> escenario = lanzar(id)) {
            Pruebas.esperarVisible(withId(R.id.botonBloquear));

            onView(withId(R.id.botonBloquear)).perform(click());
            onView(withText(R.string.bloqueo_no_volver_a_preguntar)).perform(click());
            onView(withText(R.string.bloqueo_confirmar_bloquear)).perform(click());

            assertTrue("La pantalla debe apagarse y registrarse el bloqueo",
                    esperar(id, TipoEventoEstudio.BLOQUEO_PANTALLA, 1));
            assertEquals("Bloquear con el candado no es salida", 0, contar(id, TipoEventoEstudio.SALIDA_APP));

            encenderPantalla();
            assertTrue(esperar(id, TipoEventoEstudio.DESBLOQUEO, 1));
            Pruebas.esperarVisible(withId(R.id.botonBloquear));

            onView(withId(R.id.botonBloquear)).perform(click());
            assertTrue("El segundo toque bloquea sin confirmar",
                    esperar(id, TipoEventoEstudio.BLOQUEO_PANTALLA, 2));
            encenderPantalla();
            Pruebas.esperarVisible(withId(R.id.botonBloquear));
            onView(withText(R.string.bloqueo_confirmar_titulo)).check(doesNotExist());
            assertEquals(0, contar(id, TipoEventoEstudio.SALIDA_APP));
        }
    }

    @Test
    public void ajustesOfreceQuitarElPermisoSoloSiEstaActivoYLoRetira() throws Exception {
        Intent ajustes = new Intent(contexto, AjustesActivity.class);
        try (ActivityScenario<AjustesActivity> sinPermiso = ActivityScenario.launch(ajustes)) {
            Pruebas.esperarVisible(withId(R.id.grupoTema));
            onView(withId(R.id.botonQuitarPermisoBloqueo))
                    .check(matches(withEffectiveVisibility(Visibility.GONE)));
        }

        Pruebas.comando("dpm set-active-admin --user 0 " + ADMINISTRADOR);
        assertTrue(sistema.estaActivo());
        try (ActivityScenario<AjustesActivity> conPermiso = ActivityScenario.launch(ajustes)) {
            Pruebas.esperarVisible(withId(R.id.grupoTema));
            onView(withId(R.id.botonQuitarPermisoBloqueo)).perform(
                    androidx.test.espresso.action.ViewActions.scrollTo(), click());

            long limite = System.currentTimeMillis() + ESPERA_MS;
            while (sistema.estaActivo() && System.currentTimeMillis() < limite) {
                Thread.sleep(200);
            }
            assertFalse("El permiso debe quedar retirado", sistema.estaActivo());
            onView(withId(R.id.botonQuitarPermisoBloqueo))
                    .check(matches(withEffectiveVisibility(Visibility.GONE)));
        }
    }

    private long iniciar() {
        StudyPlanApplication app = Pruebas.app();
        long id = app.estudiarAhora()
                .ejecutar(null, TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL)
                .valor().id();
        app.contadorActividades().consumirEvaluacionRecuperacion();
        return id;
    }

    private ActivityScenario<SesionEnCursoActivity> lanzar(long sesionId) {
        Intent intento = new Intent(contexto, SesionEnCursoActivity.class);
        intento.putExtra(SesionEnCursoActivity.EXTRA_SESION_ID, sesionId);
        return ActivityScenario.launch(intento);
    }

    private static void encenderPantalla() throws Exception {
        Pruebas.comando("input keyevent 224");
        Pruebas.comando("input keyevent 82");
        Pruebas.comando("cmd statusbar collapse");
    }

    private static int contar(long sesionId, TipoEventoEstudio tipo) {
        int n = 0;
        for (EventoEstudio e : Pruebas.app().eventoEstudioRepository().listarPorSesion(sesionId)) {
            if (e.tipo() == tipo) {
                n++;
            }
        }
        return n;
    }

    @SuppressWarnings("BusyWait") // sondeo con tope
    private static boolean esperar(long sesionId, TipoEventoEstudio tipo, int cantidad)
            throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA_MS;
        while (System.currentTimeMillis() < limite) {
            if (contar(sesionId, tipo) >= cantidad) {
                return true;
            }
            Thread.sleep(200);
        }
        return false;
    }
}
