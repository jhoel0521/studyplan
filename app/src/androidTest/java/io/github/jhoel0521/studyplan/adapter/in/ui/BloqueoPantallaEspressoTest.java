package io.github.jhoel0521.studyplan.adapter.in.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.service.notification.StatusBarNotification;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.TecnicaEstudio;
import io.github.jhoel0521.studyplan.domain.TipoEventoEstudio;

/**
 * Pruebas E2E del bloqueo de pantalla: apagar y encender la pantalla con StudyPlan en primer plano
 * cuenta como estudio y no como salida, y apagarla después de salir sigue siendo salida
 * (HU-50, HU-40, RN-23). Corren solo en el emulador: usan {@code input keyevent} (223 apaga, 224
 * enciende, 82 descarta el bloqueo, 3 es Inicio).
 */
@RunWith(AndroidJUnit4.class)
public class BloqueoPantallaEspressoTest {

    private static final long ESPERA_MS = 10_000L;

    @Before
    public void preparar() throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(
                    ApplicationProvider.getApplicationContext().getPackageName(),
                    Manifest.permission.POST_NOTIFICATIONS);
        }
        encenderPantalla();
        Pruebas.reiniciarEstado();
        Pruebas.app().configurarPerfil().ejecutar("Prueba", 20);
    }

    @After
    public void limpiar() throws Exception {
        encenderPantalla();
        Pruebas.reiniciarEstado();
    }

    @Test
    public void apagarYEncenderConLaAppEnPrimerPlanoRegistraBloqueoYDesbloqueoSinSalida() throws Exception {
        long sesionId = iniciarSesionEnCurso();

        try (ActivityScenario<SesionEnCursoActivity> escenario = lanzar(sesionId)) {
            Pruebas.esperarVisible(withId(R.id.textoCronometro));

            comando("input keyevent 223");
            assertTrue("Debe registrarse el bloqueo", esperarTipo(sesionId, TipoEventoEstudio.BLOQUEO_PANTALLA));
            assertFalse("Apagar la pantalla no es salida", tipos(sesionId).contains(TipoEventoEstudio.SALIDA_APP));

            encenderPantalla();
            assertTrue("Debe registrarse el desbloqueo", esperarTipo(sesionId, TipoEventoEstudio.DESBLOQUEO));
            assertFalse(tipos(sesionId).contains(TipoEventoEstudio.SALIDA_APP));
        }

        Pruebas.app().detenerSesion().ejecutar(sesionId);
        Intent detalle = new Intent(
                ApplicationProvider.getApplicationContext(), SesionDetalleActivity.class);
        detalle.putExtra(SesionDetalleActivity.EXTRA_SESION_ID, sesionId);
        try (ActivityScenario<SesionDetalleActivity> escenario = ActivityScenario.launch(detalle)) {
            Pruebas.esperarVisible(withText(R.string.evento_bloqueo_pantalla));
            onView(withText(R.string.evento_bloqueo_pantalla)).check(matches(isDisplayed()));
            onView(withText(R.string.evento_desbloqueo)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void apagarLaPantallaDespuesDeSalirSigueSiendoSalidaYNoRegistraBloqueo() throws Exception {
        long sesionId = iniciarSesionEnCurso();

        try (ActivityScenario<SesionEnCursoActivity> escenario = lanzar(sesionId)) {
            Pruebas.esperarVisible(withId(R.id.textoCronometro));

            comando("input keyevent 3");
            assertTrue("Salir de la app registra la salida", esperarTipo(sesionId, TipoEventoEstudio.SALIDA_APP));

            comando("input keyevent 223");
            Thread.sleep(2_500);
            assertFalse("Apagar la pantalla tras salir no registra bloqueo",
                    tipos(sesionId).contains(TipoEventoEstudio.BLOQUEO_PANTALLA));
        }
    }

    /** HU-40, HU-49: al salir suena un aviso de importancia alta y la notificación de la sesión pasa a «volver». */
    @Test
    public void alSalirSePublicaElAvisoDeImportanciaAltaYLaNotificacionPasaAVolver() throws Exception {
        long sesionId = iniciarSesionEnCurso();
        Context contexto = ApplicationProvider.getApplicationContext();
        NotificationManager nm = (NotificationManager) contexto.getSystemService(Context.NOTIFICATION_SERVICE);

        try (ActivityScenario<SesionEnCursoActivity> escenario = lanzar(sesionId)) {
            Pruebas.esperarVisible(withId(R.id.textoCronometro));
            comando("input keyevent 3");
            assertTrue(esperarTipo(sesionId, TipoEventoEstudio.SALIDA_APP));

            StatusBarNotification aviso = esperarNotificacion(nm, NotificadorSesion.NOTIF_RETORNO_ID);
            assertEquals(NotificadorSesion.CANAL_AVISOS_ESTUDIO, aviso.getNotification().getChannelId());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertEquals(NotificationManager.IMPORTANCE_HIGH,
                        nm.getNotificationChannel(NotificadorSesion.CANAL_AVISOS_ESTUDIO).getImportance());
            }
            StatusBarNotification sesionNotif = esperarNotificacion(nm, NotificadorSesion.NOTIF_SESION_ID);
            assertEquals(contexto.getString(R.string.notif_volver_titulo),
                    sesionNotif.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE).toString());
        }
    }

    @SuppressWarnings("BusyWait") // sondeo con tope
    private static StatusBarNotification esperarNotificacion(NotificationManager nm, int id)
            throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA_MS;
        while (System.currentTimeMillis() < limite) {
            for (StatusBarNotification sbn : nm.getActiveNotifications()) {
                if (sbn.getId() == id) {
                    return sbn;
                }
            }
            Thread.sleep(200);
        }
        throw new AssertionError("No apareció la notificación con id " + id);
    }

    private long iniciarSesionEnCurso() {
        StudyPlanApplication app = Pruebas.app();
        long id = app.estudiarAhora()
                .ejecutar(null, TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL)
                .valor().id();
        app.contadorActividades().consumirEvaluacionRecuperacion();
        return id;
    }

    private ActivityScenario<SesionEnCursoActivity> lanzar(long sesionId) {
        Intent intento = new Intent(
                ApplicationProvider.getApplicationContext(), SesionEnCursoActivity.class);
        intento.putExtra(SesionEnCursoActivity.EXTRA_SESION_ID, sesionId);
        return ActivityScenario.launch(intento);
    }

    private static void encenderPantalla() throws Exception {
        comando("input keyevent 224");
        comando("input keyevent 82");
        comando("cmd statusbar collapse");
    }

    /** Ejecuta un comando de shell con los permisos de la instrumentación y espera a que termine. */
    private static void comando(String linea) throws Exception {
        UiAutomation automatizacion = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        ParcelFileDescriptor descriptor = automatizacion.executeShellCommand(linea);
        try (InputStream entrada = new ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
            byte[] relleno = new byte[1024];
            while (entrada.read(relleno) != -1) {
                // se descarta la salida: solo importa que el comando termine
            }
        }
    }

    private static List<TipoEventoEstudio> tipos(long sesionId) {
        List<TipoEventoEstudio> resultado = new ArrayList<>();
        for (EventoEstudio e : Pruebas.app().eventoEstudioRepository().listarPorSesion(sesionId)) {
            resultado.add(e.tipo());
        }
        return resultado;
    }

    @SuppressWarnings("BusyWait") // sondeo con tope
    private static boolean esperarTipo(long sesionId, TipoEventoEstudio tipo) throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA_MS;
        while (System.currentTimeMillis() < limite) {
            if (tipos(sesionId).contains(tipo)) {
                return true;
            }
            Thread.sleep(200);
        }
        return false;
    }
}
