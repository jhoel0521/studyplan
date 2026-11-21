package io.github.jhoel0521.studyplan.adapter.in.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;

/**
 * Pruebas E2E de los controles de la sesión en curso: Pausar/Reanudar de 64 dp, iconos de 48 dp, candado
 * dentro del anillo y No molestar con su estado visible (HU-03, HU-24, HU-50). Corren solo en el emulador.
 */
@RunWith(AndroidJUnit4.class)
public class SesionEnCursoControlesEspressoTest {

    private Context contexto;
    private long sesionId;

    @Before
    public void preparar() throws Exception {
        contexto = ApplicationProvider.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .grantRuntimePermission(contexto.getPackageName(), Manifest.permission.POST_NOTIFICATIONS);
        }
        Pruebas.comando("cmd statusbar collapse");
        Pruebas.reiniciarEstado();
        Pruebas.comando("cmd notification disallow_dnd " + contexto.getPackageName());
        Pruebas.app().configurarPerfil().ejecutar("Prueba", 20);
        StudyPlanApplication app = Pruebas.app();
        sesionId = app.estudiarAhora()
                .ejecutar(null, TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL)
                .valor().id();
        app.contadorActividades().consumirEvaluacionRecuperacion();
    }

    @After
    public void limpiar() throws Exception {
        Pruebas.app().detenerSesion().ejecutar(sesionId); // restaura el filtro de No molestar
        Pruebas.comando("cmd notification disallow_dnd " + contexto.getPackageName());
        Pruebas.reiniciarEstado();
    }

    @Test
    public void elBotonRedondoMide64dpYLosIconosMidenLoMismoQueLosBotones() throws Exception {
        try (ActivityScenario<SesionEnCursoActivity> escenario = lanzar()) {
            Pruebas.esperarVisible(withId(R.id.botonPausarReanudar));
            escenario.onActivity(actividad -> {
                float densidad = actividad.getResources().getDisplayMetrics().density;
                int base = actividad.getResources().getDimensionPixelSize(R.dimen.button_height);
                assertEquals(Math.round(64 * densidad), alto(actividad, R.id.botonPausarReanudar), 1);
                for (int id : new int[] {R.id.botonDetenerEnCurso, R.id.botonCambiarMateria,
                        R.id.botonNoMolestar, R.id.botonBloquear}) {
                    assertEquals("Alto de " + actividad.getResources().getResourceEntryName(id),
                            base, alto(actividad, id));
                }
            });
        }
    }

    /**
     * HU-03 «Texto ampliado»: con la fuente del sistema al 200 % los botones de icono conservan la misma
     * altura (el texto vacío de un botón de icono no debe crecer con la fuente) y el botón redondo sigue en
     * 64 dp. Se restablece la fuente al terminar.
     */
    @Test
    public void conLaFuenteAmpliadaLosIconosConservanSuAltura() throws Exception {
        Pruebas.comando("settings put system font_scale 2.0");
        try (ActivityScenario<SesionEnCursoActivity> escenario = lanzar()) {
            Pruebas.esperarVisible(withId(R.id.botonBloquear));
            escenario.onActivity(actividad -> {
                assertTrue(actividad.getResources().getConfiguration().fontScale > 1.5f);
                float densidad = actividad.getResources().getDisplayMetrics().density;
                int base = actividad.getResources().getDimensionPixelSize(R.dimen.button_height);
                assertEquals(Math.round(64 * densidad), alto(actividad, R.id.botonPausarReanudar), 1);
                for (int id : new int[] {R.id.botonDetenerEnCurso, R.id.botonCambiarMateria,
                        R.id.botonNoMolestar, R.id.botonBloquear}) {
                    assertEquals("Alto con la fuente al 200 % de "
                            + actividad.getResources().getResourceEntryName(id), base, alto(actividad, id));
                }
            });
        } finally {
            Pruebas.comando("settings put system font_scale 1.0");
        }
    }

    @Test
    public void elCandadoEstaDentroDelAnilloDelCronometro() throws Exception {
        try (ActivityScenario<SesionEnCursoActivity> escenario = lanzar()) {
            Pruebas.esperarVisible(withId(R.id.botonBloquear));
            escenario.onActivity(actividad -> {
                View anillo = actividad.findViewById(R.id.anilloProgresoEnCurso);
                View candado = actividad.findViewById(R.id.botonBloquear);
                int[] a = new int[2];
                int[] c = new int[2];
                anillo.getLocationOnScreen(a);
                candado.getLocationOnScreen(c);
                double centroAnilloX = a[0] + anillo.getWidth() / 2.0;
                double centroAnilloY = a[1] + anillo.getHeight() / 2.0;
                double centroCandadoX = c[0] + candado.getWidth() / 2.0;
                double centroCandadoY = c[1] + candado.getHeight() / 2.0;
                double distancia = Math.hypot(centroCandadoX - centroAnilloX, centroCandadoY - centroAnilloY);
                assertTrue("El candado debe quedar dentro del anillo (distancia " + distancia + " px)",
                        distancia + candado.getWidth() / 2.0 < anillo.getWidth() / 2.0);
            });
        }
    }

    @Test
    public void pausarCambiaElIconoLaEtiquetaYLaDescripcion() throws Exception {
        try (ActivityScenario<SesionEnCursoActivity> escenario = lanzar()) {
            Pruebas.esperarVisible(withId(R.id.botonPausarReanudar));
            onView(withId(R.id.textoEtiquetaPausarReanudar)).check(matches(withText(R.string.sesion_en_curso_pausar)));

            onView(withId(R.id.botonPausarReanudar)).perform(click());

            Pruebas.esperarVisible(withText(R.string.sesion_en_curso_pausada));
            onView(withId(R.id.textoEtiquetaPausarReanudar)).check(matches(withText(R.string.sesion_en_curso_reanudar)));
            onView(withId(R.id.botonPausarReanudar))
                    .check(matches(withContentDescription(R.string.sesion_en_curso_reanudar)));
        }
    }

    @Test
    public void noMolestarSinPermisoLlevaALaExplicacion() throws Exception {
        try (ActivityScenario<SesionEnCursoActivity> escenario = lanzar()) {
            Pruebas.esperarVisible(withId(R.id.botonNoMolestar));

            onView(withId(R.id.botonNoMolestar)).perform(click());

            Pruebas.esperarVisible(withText(R.string.ajustes_no_molestar_dialogo_titulo));
            onView(withText(R.string.ajustes_no_molestar_dialogo_msg)).check(matches(isDisplayed()));
            onView(withText(android.R.string.cancel)).perform(click());
            onView(withId(R.id.textoEtiquetaNoMolestar)).check(matches(withText(R.string.sesion_no_molestar_apagado)));
        }
    }

    @Test
    public void noMolestarConPermisoAlternaSuEstadoConIconoYTexto() throws Exception {
        Pruebas.comando("cmd notification allow_dnd " + contexto.getPackageName());
        assertTrue("La app debe tener acceso a No molestar",
                Pruebas.app().controladorNoMolestar().tienePermiso());
        try (ActivityScenario<SesionEnCursoActivity> escenario = lanzar()) {
            Pruebas.esperarVisible(withId(R.id.botonNoMolestar));
            onView(withId(R.id.textoEtiquetaNoMolestar)).check(matches(withText(R.string.sesion_no_molestar_apagado)));

            onView(withId(R.id.botonNoMolestar)).perform(click());
            Pruebas.esperarVisible(withText(R.string.sesion_no_molestar_activado));
            onView(withId(R.id.botonNoMolestar))
                    .check(matches(withContentDescription(R.string.sesion_no_molestar_activado)));

            onView(withId(R.id.botonNoMolestar)).perform(click());
            Pruebas.esperarVisible(withText(R.string.sesion_no_molestar_apagado));
        }
    }

    private ActivityScenario<SesionEnCursoActivity> lanzar() {
        Intent intento = new Intent(contexto, SesionEnCursoActivity.class);
        intento.putExtra(SesionEnCursoActivity.EXTRA_SESION_ID, sesionId);
        return ActivityScenario.launch(intento);
    }

    private static int alto(android.app.Activity actividad, int id) {
        return actividad.findViewById(id).getHeight();
    }
}
