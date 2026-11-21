package io.github.jhoel0521.studyplan.adapter.in.ui.tecnicas;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio;
import io.github.jhoel0521.studyplan.adapter.in.ui.StudyPlanApplication;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.Pruebas;
import io.github.jhoel0521.studyplan.adapter.in.ui.configuracion.AjustesActivity;
import io.github.jhoel0521.studyplan.adapter.in.ui.configuracion.MainActivity;
import io.github.jhoel0521.studyplan.adapter.in.ui.sesiones.SesionDetalleActivity;

/**
 * Pruebas E2E Espresso de técnicas de estudio y ejecución en vivo (HU-22 a HU-24, HU-39, HU-41,
 * HU-44, RNF-10, RNF-18).
 */
@RunWith(AndroidJUnit4.class)
public class TecnicasEstudioEspressoTest {

    private Materia calculo;
    private Materia ingles;

    @Before
    public void preparar() {
        Pruebas.reiniciarEstado();
        StudyPlanApplication app = Pruebas.app();
        app.configurarPerfil().ejecutar("Ana", 20);
        calculo = app.registrarMateria().ejecutar("Cálculo", 0xFF1565C0, "Prof. A").valor();
        ingles = app.registrarMateria().ejecutar("Inglés", 0xFF2E7D32, "Prof. B").valor();
    }

    @After
    public void limpiar() {
        Pruebas.reiniciarEstado();
    }

    @Test
    public void flujoLibrePausaReanudaDetieneYRegistraLineaDeTiempo() throws Exception {
        Fecha hoy = Pruebas.app().reloj().hoy();
        SesionEstudio s = Pruebas.app().planificarSesion().ejecutar(
                calculo.id(), hoy, Hora.de(9, 0).valor(), Hora.de(10, 0).valor(), "").sesion();

        Intent det = new Intent(ApplicationProvider.getApplicationContext(), SesionDetalleActivity.class);
        det.putExtra(SesionDetalleActivity.EXTRA_SESION_ID, s.id());

        try (ActivityScenario<SesionDetalleActivity> escenario = ActivityScenario.launch(det)) {
            Pruebas.esperarVisible(withId(R.id.botonIniciarSesion));
            onView(withId(R.id.botonIniciarSesion)).perform(scrollTo(), click());

            Pruebas.esperarVisible(withId(R.id.botonPausarReanudar));
            onView(withId(R.id.botonPausarReanudar)).perform(click());
            Pruebas.esperarVisible(withText(R.string.sesion_en_curso_pausada));
            onView(withId(R.id.botonPausarReanudar)).perform(click());
            Pruebas.esperarVisible(withText(R.string.topico_pausa_titulo));
            onView(withText(R.string.topico_descanso_comida)).perform(click());
            onView(withText(R.string.topico_reanudar_confirmar)).perform(click());
            Pruebas.esperarVisible(withText(R.string.sesion_en_curso_activa));

            onView(withId(R.id.botonDetenerEnCurso)).perform(click());

            Pruebas.esperarVisible(withId(R.id.listaEventosDetalle));
            // Rotación conserva el detalle y su línea de tiempo (RNF-10)
            escenario.recreate();
            Pruebas.esperarVisible(withId(R.id.listaEventosDetalle));
            Pruebas.esperarVisible(withText(R.string.evento_inicio));
            onView(withText(R.string.evento_pausa)).check(matches(isDisplayed()));
            onView(withText(R.string.topico_descanso_comida)).check(matches(isDisplayed()));
            onView(withText(R.string.evento_fin)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void ajustesPomodoroYNoMolestarSinPermisoNoBloquean() throws Exception {
        try (ActivityScenario<AjustesActivity> escenario =
                     ActivityScenario.launch(AjustesActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.campoPomodoroTrabajo));
            onView(withId(R.id.campoPomodoroTrabajo))
                    .perform(scrollTo(), replaceText("30"), closeSoftKeyboard());
            onView(withId(R.id.botonGuardarPomodoro)).perform(scrollTo(), click());
            Pruebas.esperarVisible(withText(R.string.ajustes_pomodoro_guardado));

            if (!Pruebas.app().controladorNoMolestar().tienePermiso()) {
                onView(withId(R.id.interruptorNoMolestarGlobal)).perform(scrollTo(), click());
                Pruebas.esperarVisible(withText(R.string.ajustes_no_molestar_dialogo_titulo));
                onView(withText(android.R.string.cancel)).perform(click());
            }
        }
    }

    @Test
    public void pomodoroMuestraFaseYRecuperacionW13CierraSesionAlVolver() throws Exception {
        StudyPlanApplication app = Pruebas.app();
        Fecha hoy = app.reloj().hoy();
        SesionEstudio s = app.planificarSesion().ejecutar(
                calculo.id(), hoy, Hora.de(9, 0).valor(), Hora.de(11, 0).valor(), "").sesion();
        app.iniciarSesion().ejecutar(
                s.id(), TecnicaEstudio.POMODORO, PreferenciaNoMolestar.HEREDAR_GLOBAL);
        app.contadorActividades().consumirEvaluacionRecuperacion();

        Intent enCurso = new Intent(
                ApplicationProvider.getApplicationContext(), SesionEnCursoActivity.class);
        enCurso.putExtra(SesionEnCursoActivity.EXTRA_SESION_ID, s.id());
        try (ActivityScenario<SesionEnCursoActivity> escenario = ActivityScenario.launch(enCurso)) {
            Pruebas.esperarVisible(withId(R.id.textoFasePomodoro));
            onView(withId(R.id.textoFasePomodoro)).check(matches(withText(containsString("1/4"))));
            // Rotación conserva la pantalla en curso (RNF-10)
            escenario.recreate();
            Pruebas.esperarVisible(withId(R.id.textoFasePomodoro));
            onView(withId(R.id.textoFasePomodoro)).check(matches(withText(containsString("1/4"))));
        }

        // Simula salida de la app y apertura posterior en MainActivity -> aparece diálogo W13
        long salidaMs = app.reloj().ahoraMilis() - 10 * 60_000L;
        app.eventoEstudioRepository().guardar(EventoEstudio.crear(
                s.id(),
                TipoEventoEstudio.SALIDA_APP,
                salidaMs,
                app.reloj().fechaDe(salidaMs),
                app.reloj().horaDe(salidaMs),
                "").valor());
        app.contadorActividades().solicitarEvaluacionRecuperacion();

        try (ActivityScenario<MainActivity> escenario = ActivityScenario.launch(MainActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.radioNoSegui));
            onView(withId(R.id.radioNoSegui)).perform(click());
            onView(withText(R.string.recuperacion_confirmar)).perform(click());
            Pruebas.esperarVisible(withId(R.id.saludo));
        }
    }

    @Test
    public void repartoW14AjustaYGuardaAmbasSesiones() throws Exception {
        StudyPlanApplication app = Pruebas.app();
        Fecha hoy = app.reloj().hoy();
        SesionEstudio s1 = app.planificarSesion().ejecutar(
                calculo.id(), hoy, Hora.de(10, 0).valor(), Hora.de(11, 0).valor(), "").sesion();
        SesionEstudio s2 = app.planificarSesion().ejecutar(
                ingles.id(), hoy, Hora.de(11, 0).valor(), Hora.de(12, 0).valor(), "").sesion();

        long ahoraMs = app.reloj().ahoraMilis();
        long inicioMs = ahoraMs - 90 * 60_000L;
        SesionEstudio enCurso = s1.iniciar(TecnicaEstudio.LIBRE, null).valor();
        app.sesionRepository().actualizar(enCurso);
        app.eventoEstudioRepository().guardar(EventoEstudio.crear(
                s1.id(),
                TipoEventoEstudio.INICIO,
                inicioMs,
                app.reloj().fechaDe(inicioMs),
                app.reloj().horaDe(inicioMs),
                "").valor());

        Intent reparto = new Intent(
                ApplicationProvider.getApplicationContext(), RepartoSesionActivity.class);
        reparto.putExtra(RepartoSesionActivity.EXTRA_SESION_ACTUAL_ID, s1.id());
        reparto.putExtra(RepartoSesionActivity.EXTRA_SESION_SIGUIENTE_ID, s2.id());

        try (ActivityScenario<RepartoSesionActivity> escenario = ActivityScenario.launch(reparto)) {
            Pruebas.esperarVisible(withId(R.id.botonMas5Min));
            onView(withId(R.id.botonMas5Min)).perform(click());
            onView(withId(R.id.botonConfirmarReparto)).perform(scrollTo(), click());
        }
    }

    @Test
    public void regresoDirectoASesionEnCursoAbreW13YRetomarSinSumar() throws Exception {
        StudyPlanApplication app = Pruebas.app();
        SesionEstudio s = app.estudiarAhora().ejecutar(
                calculo.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL).valor();

        long salidaMs = app.reloj().ahoraMilis() - 5 * 60_000L;
        app.eventoEstudioRepository().guardar(EventoEstudio.crear(
                s.id(),
                TipoEventoEstudio.SALIDA_APP,
                salidaMs,
                app.reloj().fechaDe(salidaMs),
                app.reloj().horaDe(salidaMs),
                "").valor());
        app.contadorActividades().solicitarEvaluacionRecuperacion();

        Intent enCurso = new Intent(
                ApplicationProvider.getApplicationContext(), SesionEnCursoActivity.class);
        enCurso.putExtra(SesionEnCursoActivity.EXTRA_SESION_ID, s.id());
        try (ActivityScenario<SesionEnCursoActivity> escenario = ActivityScenario.launch(enCurso)) {
            Pruebas.esperarVisible(withId(R.id.radioRetomarSinSumar));
            onView(withId(R.id.radioRetomarSinSumar)).perform(click());
            onView(withText(R.string.recuperacion_confirmar)).perform(click());
            Pruebas.esperarVisible(withText(R.string.sesion_en_curso_activa));
            // En Estudiar ahora nunca se muestra tarjetaFinPlanificado
            onView(withId(R.id.tarjetaFinPlanificado)).check(matches(
                    org.hamcrest.Matchers.not(isDisplayed())));
        }
    }

    @Test
    public void descansoPomodoroDentroNoAbreW13YFueraSiAbreW13() throws Exception {
        StudyPlanApplication app = Pruebas.app();
        SesionEstudio s = app.estudiarAhora().ejecutar(
                calculo.id(), TecnicaEstudio.POMODORO, PreferenciaNoMolestar.HEREDAR_GLOBAL).valor();

        io.github.jhoel0521.studyplan.adapter.out.system.MarcadorSalidaAppPrefs marcador =
                new io.github.jhoel0521.studyplan.adapter.out.system.MarcadorSalidaAppPrefs(app);

        // Caso 1: dentro de descanso Pomodoro -> evaluarRecuperacionSesion() vacío
        long finFuturo = app.reloj().ahoraMilis() + 3 * 60_000L;
        marcador.marcarFinDescansoPomodoro(s.id(), finFuturo);
        org.junit.Assert.assertFalse(app.evaluarRecuperacionSesion().ejecutar().isPresent());

        // Caso 2: fin de descanso ya vencido -> abre W13 al lanzar SesionEnCursoActivity
        long finPasado = app.reloj().ahoraMilis() - 2 * 60_000L;
        marcador.marcarFinDescansoPomodoro(s.id(), finPasado);
        app.contadorActividades().solicitarEvaluacionRecuperacion();

        Intent enCurso = new Intent(
                ApplicationProvider.getApplicationContext(), SesionEnCursoActivity.class);
        enCurso.putExtra(SesionEnCursoActivity.EXTRA_SESION_ID, s.id());
        try (ActivityScenario<SesionEnCursoActivity> escenario = ActivityScenario.launch(enCurso)) {
            Pruebas.esperarVisible(withId(R.id.radioNoSegui));
            onView(withId(R.id.radioNoSegui)).perform(click());
            onView(withText(R.string.recuperacion_confirmar)).perform(click());
        }
    }
}
