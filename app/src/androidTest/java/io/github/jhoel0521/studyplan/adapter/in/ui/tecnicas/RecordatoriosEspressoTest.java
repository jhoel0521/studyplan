package io.github.jhoel0521.studyplan.adapter.in.ui.tecnicas;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.espresso.EspressoException;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.valueobject.Instante;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.Pruebas;
import io.github.jhoel0521.studyplan.adapter.in.ui.sesiones.SesionDetalleActivity;

/**
 * Pruebas E2E del flujo completo de recordatorios: visualización y limpieza al abrir detalle,
 * arranque directo con {@link SesionEnCursoActivity#EXTRA_INICIAR_DESDE_RECORDATORIO}, retorno al
 * inicio cuando la actividad es raíz de tarea y manejo seguro de {@code SESION_YA_EN_CURSO}
 * ({@code HU-34}, {@code RNF-18}).
 */
@RunWith(AndroidJUnit4.class)
public final class RecordatoriosEspressoTest {

    private Context context;
    private Materia materia;
    private NotificationManager nm;

    @Before
    public void preparar() {
        Pruebas.reiniciarEstado();
        context = ApplicationProvider.getApplicationContext();
        nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .grantRuntimePermission(
                            context.getPackageName(), Manifest.permission.POST_NOTIFICATIONS);
        }
        Pruebas.app().configurarPerfil().ejecutar("Ana", 20);
        materia = Pruebas.app().registrarMateria().ejecutar("Álgebra", 0xFF1E88E5, null).valor();
    }

    @After
    public void limpiar() {
        Pruebas.reiniciarEstado();
    }

    @Test
    public void detalleMuestraAntelacionLimpiaNotificacionYVuelveAlInicioSiEsRaiz()
            throws Exception {
        Fecha manana = Pruebas.app().reloj().hoy().sumarDias(1);
        SesionEstudio s = Pruebas.app().planificarSesion().ejecutar(
                materia.id(),
                manana,
                Hora.de(10, 0).valor(),
                Hora.de(11, 0).valor(),
                null,
                AntelacionRecordatorio.MINUTOS_15).sesion();

        NotificadorSesion.mostrarRecordatorio(
                context, s.id(), materia.nombre(), "10:00", "11:00", false);
        assertTrue(esperarNotificacion(NotificadorSesion.idRecordatorio(s.id()), true));

        Intent detalleIntent = new Intent(context, SesionDetalleActivity.class);
        detalleIntent.putExtra(SesionDetalleActivity.EXTRA_SESION_ID, s.id());

        try (ActivityScenario<SesionDetalleActivity> escenario =
                     ActivityScenario.launch(detalleIntent)) {
            Pruebas.esperarVisible(withId(R.id.textoRecordatorioDetalle));
            onView(withId(R.id.textoRecordatorioDetalle)).check(matches(
                    withText(containsString(context.getString(R.string.recordatorio_15_min)))));
            assertTrue(esperarNotificacion(NotificadorSesion.idRecordatorio(s.id()), false));

            onView(withContentDescription(R.string.volver)).perform(click());
            Pruebas.esperarVisible(withId(R.id.barraInferior));
        }
    }

    @Test
    public void accionIniciarDesdeRecordatorioIniciaLibreYLuegoVuelveAlInicioComoRaiz()
            throws Exception {
        SesionEstudio sesion = sembrarPendienteParaHoy(AntelacionRecordatorio.MINUTOS_15);
        NotificadorSesion.mostrarRecordatorio(
                context, sesion.id(), materia.nombre(), "10:00", "11:00", false);
        assertTrue(esperarNotificacion(NotificadorSesion.idRecordatorio(sesion.id()), true));

        Intent iniciarIntent = new Intent(context, SesionEnCursoActivity.class);
        iniciarIntent.putExtra(SesionEnCursoActivity.EXTRA_SESION_ID, sesion.id());
        iniciarIntent.putExtra(SesionEnCursoActivity.EXTRA_INICIAR_DESDE_RECORDATORIO, true);

        try (ActivityScenario<SesionEnCursoActivity> escenario =
                     ActivityScenario.launch(iniciarIntent)) {
            Pruebas.esperarVisible(withId(R.id.nombreMateriaEnCurso));
            onView(withId(R.id.nombreMateriaEnCurso)).check(matches(withText("Álgebra")));
            Pruebas.esperarVisible(withId(R.id.botonDetenerEnCurso));

            SesionEstudio enCurso = Pruebas.app()
                    .sesionRepository()
                    .buscarPorId(sesion.id())
                    .orElseThrow(AssertionError::new);
            assertEquals(CodigoEstadoSesion.EN_CURSO, enCurso.codigoEstado());
            assertEquals(TecnicaEstudio.LIBRE, enCurso.tecnica());
            assertTrue(esperarNotificacion(NotificadorSesion.idRecordatorio(sesion.id()), false));

            // Detener sesión como Activity raíz abre MainActivity en lugar de abandonar la app
            onView(withId(R.id.botonDetenerEnCurso)).perform(scrollTo(), click());
            Pruebas.esperarVisible(withId(R.id.barraInferior));
        }
    }

    @Test
    public void iniciarDesdeRecordatorioConOtraSesionPausadaMuestraUnDialogoYPermiteIrAActiva()
            throws Exception {
        SesionEstudio activa = sembrarPendienteParaHoy(AntelacionRecordatorio.MINUTOS_15);
        SesionEstudio segunda = sembrarPendienteParaHoyDeHora(15, AntelacionRecordatorio.MINUTOS_30);

        Pruebas.app().iniciarSesion().ejecutar(
                activa.id(), TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL);
        Pruebas.app().pausarReanudarSesion().ejecutar(activa.id());

        Intent iniciarSegunda = new Intent(context, SesionEnCursoActivity.class);
        iniciarSegunda.putExtra(SesionEnCursoActivity.EXTRA_SESION_ID, segunda.id());
        iniciarSegunda.putExtra(SesionEnCursoActivity.EXTRA_INICIAR_DESDE_RECORDATORIO, true);

        try (ActivityScenario<SesionEnCursoActivity> escenario =
                     ActivityScenario.launch(iniciarSegunda)) {
            esperarDialogoConTexto(R.string.error_sesion_ya_en_curso);
            onView(withText(R.string.sesion_en_curso_ir_a_activa))
                    .inRoot(isDialog())
                    .perform(click());

            Pruebas.esperarVisible(withId(R.id.nombreMateriaEnCurso));
            onView(withId(R.id.textoEstadoEnCurso)).check(
                    matches(withText(R.string.sesion_en_curso_pausada)));
            assertFalse(Pruebas.app().sesionRepository().listarPendientesDesde(
                    Pruebas.app().reloj().hoy()).isEmpty());
        }
    }

    private SesionEstudio sembrarPendienteParaHoy(AntelacionRecordatorio antelacion)
            throws Exception {
        return sembrarPendienteParaHoyDeHora(10, antelacion);
    }

    private SesionEstudio sembrarPendienteParaHoyDeHora(
            int offsetHoras, AntelacionRecordatorio antelacion) throws Exception {
        long ahora = Pruebas.app().reloj().ahoraMilis();
        long inicio = ahora + offsetHoras * 60L * 60_000L + 30 * 60_000L;
        long fin = inicio + 60 * 60_000L;
        int desfase = Pruebas.app().reloj().ahoraInstante().desfaseMinutos();
        SesionEstudio nueva = SesionEstudio.restaurar(
                null,
                materia.id(),
                inicio,
                fin,
                desfase,
                60,
                null,
                CodigoEstadoSesion.PENDIENTE,
                "",
                null,
                TecnicaEstudio.LIBRE,
                PreferenciaNoMolestar.HEREDAR_GLOBAL,
                null,
                null,
                null,
                null,
                antelacion);
        return Pruebas.app()
                .ejecutor()
                .submit(() -> Pruebas.app().sesionRepository().guardar(nueva))
                .get(5, TimeUnit.SECONDS);
    }

    @SuppressWarnings("BusyWait") // Espera acotada hasta que el ejecutor muestre el AlertDialog
    private static void esperarDialogoConTexto(int textoRes) throws InterruptedException {
        long limite = System.currentTimeMillis() + 8000L;
        while (true) {
            try {
                onView(withText(textoRes))
                        .inRoot(isDialog())
                        .check(matches(isDisplayed()));
                return;
            } catch (RuntimeException | AssertionError e) {
                if ((!(e instanceof AssertionError) && !(e instanceof EspressoException))
                        || System.currentTimeMillis() > limite) {
                    throw e;
                }
                Thread.sleep(100L);
            }
        }
    }

    @SuppressWarnings("BusyWait") // Binder asíncrono de NotificationManagerService
    private boolean esperarNotificacion(int id, boolean activaEsperada) {
        long limite = System.currentTimeMillis() + 3000L;
        while (System.currentTimeMillis() < limite) {
            if (estaActiva(id) == activaEsperada) {
                return true;
            }
            try {
                Thread.sleep(25L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return estaActiva(id) == activaEsperada;
    }

    private boolean estaActiva(int id) {
        for (StatusBarNotification sbn : nm.getActiveNotifications()) {
            if (sbn.getId() == id) {
                return true;
            }
        }
        return false;
    }
}
