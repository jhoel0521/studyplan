package io.github.jhoel0521.studyplan.adapter.in.ui.configuracion;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.Pruebas;
import io.github.jhoel0521.studyplan.domain.enumeration.FrecuenciaRespaldo;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Pantalla Copia de seguridad: nace apagada, la contraseña pide la advertencia antes de guardarse y elegir una
 * frecuencia programa la tarea periódica. No llama a Google: la cuenta se vincula directamente en el caso de uso.
 *
 * <p>Verifica: HU-35, HU-36, RN-19 y RN-20.
 */
@RunWith(AndroidJUnit4.class)
public class RespaldoEspressoTest {

    private static final String TAREA = "studyplan_respaldo_periodico";

    @Before
    public void sinDatos() {
        Pruebas.reiniciarEstado();
        Pruebas.app().configurarPerfil().ejecutar("Ana", 20);
    }

    @After
    public void restablecer() {
        Pruebas.reiniciarEstado();
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void nacApagadaSinCuentaConFrecuenciaNunca() throws InterruptedException {
        try (ActivityScenario<RespaldoActivity> escenario = ActivityScenario.launch(RespaldoActivity.class)) {
            Pruebas.esperarVisible(withText(R.string.respaldo_sin_cuenta));
            onView(withId(R.id.frecuenciaNunca)).check(matches(isChecked()));
            onView(withId(R.id.botonRespaldarAhora)).check(matches(not(isEnabled())));
            onView(withId(R.id.botonRestaurar)).check(matches(not(isEnabled())));
            onView(withId(R.id.botonVincular)).check(matches(isDisplayed()));
        }
        assertFalse(Pruebas.app().configurarRespaldo().consultar().hayCuenta());
    }

    @Test
    @SuppressWarnings("unused")
    public void definirLaContrasenaMuestraLaAdvertenciaYLaGuarda() throws InterruptedException {
        Pruebas.app().configurarRespaldo().vincularCuenta("ana@example.com");
        try (ActivityScenario<RespaldoActivity> escenario = ActivityScenario.launch(RespaldoActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.botonDefinirContrasena));
            onView(withId(R.id.botonDefinirContrasena)).perform(scrollTo(), click());

            Pruebas.esperarVisible(withText(R.string.respaldo_contrasena_advertencia));
            onView(withHint(R.string.respaldo_contrasena_hint)).inRoot(isDialog())
                    .perform(typeText("secreta"), closeSoftKeyboard());
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click());

            Pruebas.esperarVisible(withId(R.id.botonQuitarContrasena));
        }
        assertTrue(Pruebas.app().configurarRespaldo().consultar().conContrasena());
    }

    @Test
    @SuppressWarnings("unused")
    public void cancelarElDialogoDeLaContrasenaNoLaGuarda() throws InterruptedException {
        Pruebas.app().configurarRespaldo().vincularCuenta("ana@example.com");
        try (ActivityScenario<RespaldoActivity> escenario = ActivityScenario.launch(RespaldoActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.botonDefinirContrasena));
            onView(withId(R.id.botonDefinirContrasena)).perform(scrollTo(), click());
            Pruebas.esperarVisible(withText(R.string.respaldo_contrasena_advertencia));
            onView(withId(android.R.id.button2)).inRoot(isDialog()).perform(click());
        }
        assertFalse(Pruebas.app().configurarRespaldo().consultar().conContrasena());
    }

    @Test
    @SuppressWarnings("unused")
    public void elegirFrecuenciaSemanalLaGuardaYProgramaLaTareaPeriodica() throws Exception {
        Pruebas.app().configurarRespaldo().vincularCuenta("ana@example.com");
        try (ActivityScenario<RespaldoActivity> escenario = ActivityScenario.launch(RespaldoActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.frecuenciaSemanal));
            onView(withId(R.id.frecuenciaSemanal)).perform(scrollTo(), click());
            esperarFrecuencia(FrecuenciaRespaldo.SEMANAL);
            onView(withId(R.id.frecuenciaSemanal)).check(matches(isChecked()));
        }
        assertEquals(FrecuenciaRespaldo.SEMANAL, Pruebas.app().configurarRespaldo().consultar().frecuencia());
        assertEquals(1, tareasProgramadas().size());
        assertEquals(WorkInfo.State.ENQUEUED, tareasProgramadas().get(0).getState());
    }

    @Test
    @SuppressWarnings("unused")
    public void elegirSoloManualCancelaLaTareaPeriodica() throws Exception {
        Pruebas.app().configurarRespaldo().vincularCuenta("ana@example.com");
        Pruebas.app().configurarRespaldo().cambiarFrecuencia(FrecuenciaRespaldo.DIARIA);
        try (ActivityScenario<RespaldoActivity> escenario = ActivityScenario.launch(RespaldoActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.frecuenciaManual));
            onView(withId(R.id.frecuenciaManual)).perform(scrollTo(), click());
            esperarFrecuencia(FrecuenciaRespaldo.SOLO_MANUAL);
        }
        assertEquals(FrecuenciaRespaldo.SOLO_MANUAL, Pruebas.app().configurarRespaldo().consultar().frecuencia());
        for (WorkInfo info : tareasProgramadas()) {
            assertTrue(info.getState() == WorkInfo.State.CANCELLED);
        }
    }

    /** El cambio se guarda en el hilo de fondo: se espera (hasta 8 s) a que la configuración lo refleje. */
    private static void esperarFrecuencia(FrecuenciaRespaldo esperada) throws InterruptedException {
        long limite = System.currentTimeMillis() + 8000;
        while (Pruebas.app().configurarRespaldo().consultar().frecuencia() != esperada
                && System.currentTimeMillis() < limite) {
            Thread.sleep(100);
        }
    }

    private static List<WorkInfo> tareasProgramadas() throws Exception {
        Context contexto = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return WorkManager.getInstance(contexto).getWorkInfosForUniqueWork(TAREA).get(5, TimeUnit.SECONDS);
    }
}
