package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.readmodel.MetaAlcanzada;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio;

/**
 * Pruebas instrumentadas de los botones de la notificación de la sesión: Pausar, Reanudar y Detener
 * usan los {@code PendingIntent} reales; una acción obsoleta no actúa sobre otra sesión; descartar la
 * notificación la vuelve a publicar (HU-49, HU-28). Corren solo en el emulador.
 */
@RunWith(AndroidJUnit4.class)
public class AccionesNotificacionEspressoTest {

    private static final long ESPERA_MS = 10_000L;

    private Context contexto;
    private NotificationManager nm;

    @Before
    public void preparar() {
        contexto = ApplicationProvider.getApplicationContext();
        nm = (NotificationManager) contexto.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .grantRuntimePermission(contexto.getPackageName(), Manifest.permission.POST_NOTIFICATIONS);
        }
        Pruebas.reiniciarEstado();
        nm.cancelAll();
        Pruebas.app().configurarPerfil().ejecutar("Prueba", 20);
    }

    @After
    public void limpiar() {
        Pruebas.reiniciarEstado();
        nm.cancelAll();
    }

    @Test
    public void pausarYReanudarDesdeLosBotonesRegistranSusEventos() throws Exception {
        long id = iniciar();
        Notification corriendo = esperarTitulo(contexto.getString(R.string.notif_sesion_titulo));

        corriendo.actions[0].actionIntent.send();
        assertTrue("Debe registrarse la pausa", esperarEvento(id, TipoEventoEstudio.PAUSA));
        Notification pausada = esperarTitulo(contexto.getString(R.string.notif_sesion_pausa_titulo));

        pausada.actions[0].actionIntent.send();
        assertTrue("Debe registrarse la reanudación", esperarEvento(id, TipoEventoEstudio.REANUDACION));
        esperarTitulo(contexto.getString(R.string.notif_sesion_titulo));
    }

    @Test
    public void detenerDesdeElBotonCierraLaSesionYRetiraLaNotificacion() throws Exception {
        long id = iniciar();
        Notification n = esperarTitulo(contexto.getString(R.string.notif_sesion_titulo));

        n.actions[1].actionIntent.send();

        assertTrue("La sesión debe dejar de estar En curso", esperarQueNoHayaSesionEnCurso());
        assertTrue("La notificación debe desaparecer", esperarSinNotificacion());
        assertTrue(Pruebas.app().eventoEstudioRepository().listarPorSesion(id).size() > 0);
    }

    /**
     * Con {@code FLAG_UPDATE_CURRENT} solo hay un botón por acción y su {@code sesionId} es siempre el de la
     * sesión En curso. Lo que debe garantizarse es que una acción tardía de una sesión ya terminada no haga
     * nada ni deje una notificación.
     */
    @Test
    public void unaAccionTardiaDeUnaSesionTerminadaNoHaceNadaNiDejaNotificacion() throws Exception {
        long id = iniciar();
        Notification vieja = esperarTitulo(contexto.getString(R.string.notif_sesion_titulo));
        assertTrue(Pruebas.app().detenerSesion().ejecutar(id).esValido());
        assertTrue(esperarSinNotificacion());
        int eventosAntes = Pruebas.app().eventoEstudioRepository().listarPorSesion(id).size();

        vieja.actions[0].actionIntent.send(); // «Pausar» de una sesión que ya terminó
        Thread.sleep(2_500);

        assertEquals(eventosAntes, Pruebas.app().eventoEstudioRepository().listarPorSesion(id).size());
        assertFalse(tiene(id, TipoEventoEstudio.PAUSA));
        assertTrue(esperarSinNotificacion());
    }

    /**
     * El sistema no deja que una app quite la notificación de un servicio en primer plano con
     * {@code cancel()}, así que no se puede simular el deslizamiento: se comprueba que el receptor del
     * {@code deleteIntent} responde y deja la notificación publicada con su estado.
     */
    @Test
    public void elReceptorDelDeleteIntentDejaLaNotificacionPublicada() throws Exception {
        iniciar();
        Notification n = esperarTitulo(contexto.getString(R.string.notif_sesion_titulo));
        assertNotNull(n.deleteIntent);

        n.deleteIntent.send();
        Thread.sleep(1_500);

        esperarTitulo(contexto.getString(R.string.notif_sesion_titulo));
    }

    /** HU-24, HU-49: el tercer botón alterna No molestar solo para la sesión y muestra su estado. */
    @Test
    public void elBotonNoMolestarAlternaElEstadoDeLaSesion() throws Exception {
        Pruebas.comando("cmd notification allow_dnd " + contexto.getPackageName());
        long id = iniciar();
        try {
            Notification n = esperarTitulo(contexto.getString(R.string.notif_sesion_titulo));
            assertEquals(contexto.getString(R.string.sesion_no_molestar_apagado), n.actions[2].title.toString());

            n.actions[2].actionIntent.send();
            esperarAccion(2, contexto.getString(R.string.sesion_no_molestar_activado));

            activa().getNotification().actions[2].actionIntent.send();
            esperarAccion(2, contexto.getString(R.string.sesion_no_molestar_apagado));
        } finally {
            Pruebas.app().detenerSesion().ejecutar(id); // restaura el filtro de No molestar
            Pruebas.comando("cmd notification disallow_dnd " + contexto.getPackageName());
        }
    }

    @Test
    public void elAvisoDeMetasAbreLaFelicitacion() throws Exception {
        List<MetaAlcanzada> metas = Collections.singletonList(new MetaAlcanzada(null, "", 10.0, 10.5));

        NotificadorSesion.mostrarMetasAlcanzadas(contexto, metas);

        StatusBarNotification aviso = null;
        long limite = System.currentTimeMillis() + ESPERA_MS;
        while (aviso == null && System.currentTimeMillis() < limite) {
            for (StatusBarNotification sbn : nm.getActiveNotifications()) {
                if (sbn.getId() == NotificadorSesion.NOTIF_METAS_ID) {
                    aviso = sbn;
                }
            }
            Thread.sleep(200);
        }
        assertNotNull("Debe publicarse el aviso de metas", aviso);
        assertEquals(contexto.getString(R.string.felicitacion_titulo),
                aviso.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE).toString());
        assertNotNull(aviso.getNotification().contentIntent);
    }

    /** HU-49, HU-34: con música sonando los avisos salen en silencio y sin música no se silencian. */
    @Test
    public void conMusicaSonandoLosAvisosSalenEnSilencio() throws Exception {
        NotificadorSesion.fijarDetectorMusicaParaPruebas(c -> true);

        NotificadorSesion.mostrarAvisoRetorno(contexto, 1L, "Álgebra");
        NotificadorSesion.mostrarHito(contexto, 1L, "CAMBIO_FASE_POMODORO", "Álgebra");
        NotificadorSesion.mostrarMetasAlcanzadas(
                contexto, Collections.singletonList(new MetaAlcanzada(null, "", 10.0, 10.5)));

        assertTrue("Aviso de volver en silencio",
                silenciosa(esperarId(NotificadorSesion.NOTIF_RETORNO_ID)));
        assertTrue("Hito en silencio",
                silenciosa(esperarId(NotificadorSesion.NOTIF_HITO_ID)));
        assertTrue("Meta alcanzada en silencio",
                silenciosa(esperarId(NotificadorSesion.NOTIF_METAS_ID)));
        assertTrue("Recordatorio previo en silencio", silenciosa(
                NotificadorSesion.construirNotificacionRecordatorio(contexto, 1L, "Álgebra", "08:00", "09:00", false)));
    }

    @Test
    public void sinMusicaLosAvisosNoSeSilencian() throws Exception {
        NotificadorSesion.fijarDetectorMusicaParaPruebas(c -> false);

        NotificadorSesion.mostrarAvisoRetorno(contexto, 1L, "Álgebra");
        NotificadorSesion.mostrarHito(contexto, 1L, "CAMBIO_FASE_POMODORO", "Álgebra");

        assertFalse(silenciosa(esperarId(NotificadorSesion.NOTIF_RETORNO_ID)));
        assertFalse(silenciosa(esperarId(NotificadorSesion.NOTIF_HITO_ID)));
        assertFalse(silenciosa(
                NotificadorSesion.construirNotificacionRecordatorio(contexto, 1L, "Álgebra", "08:00", "09:00", false)));
    }

    /** {@code setSilent(true)} de AndroidX publica el aviso sin sonido ni vibración con alerta de grupo «resumen». */
    private static boolean silenciosa(Notification n) {
        return androidx.core.app.NotificationCompat.getGroupAlertBehavior(n)
                == androidx.core.app.NotificationCompat.GROUP_ALERT_SUMMARY;
    }

    @SuppressWarnings("BusyWait") // sondeo con tope
    private Notification esperarId(int id) throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA_MS;
        while (System.currentTimeMillis() < limite) {
            for (StatusBarNotification sbn : nm.getActiveNotifications()) {
                if (sbn.getId() == id) {
                    return sbn.getNotification();
                }
            }
            Thread.sleep(200);
        }
        throw new AssertionError("No apareció la notificación con id " + id);
    }

    private long iniciar() {
        StudyPlanApplication app = Pruebas.app();
        long id = app.estudiarAhora()
                .ejecutar(null, TecnicaEstudio.LIBRE, PreferenciaNoMolestar.HEREDAR_GLOBAL)
                .valor().id();
        app.contadorActividades().consumirEvaluacionRecuperacion();
        return id;
    }

    private boolean tiene(long sesionId, TipoEventoEstudio tipo) {
        for (EventoEstudio e : Pruebas.app().eventoEstudioRepository().listarPorSesion(sesionId)) {
            if (e.tipo() == tipo) {
                return true;
            }
        }
        return false;
    }

    private StatusBarNotification activa() {
        for (StatusBarNotification sbn : nm.getActiveNotifications()) {
            if (sbn.getId() == NotificadorSesion.NOTIF_SESION_ID) {
                return sbn;
            }
        }
        return null;
    }

    @SuppressWarnings("BusyWait") // sondeo con tope
    private Notification esperarTitulo(String titulo) throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA_MS;
        String ultimo = null;
        while (System.currentTimeMillis() < limite) {
            StatusBarNotification sbn = activa();
            if (sbn != null) {
                CharSequence t = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE);
                ultimo = t == null ? null : t.toString();
                if (titulo.equals(ultimo)) {
                    return sbn.getNotification();
                }
            }
            Thread.sleep(200);
        }
        throw new AssertionError("No apareció la notificación «" + titulo + "»; último título: " + ultimo);
    }

    @SuppressWarnings("BusyWait") // sondeo con tope
    private void esperarAccion(int posicion, String titulo) throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA_MS;
        String ultimo = null;
        while (System.currentTimeMillis() < limite) {
            StatusBarNotification sbn = activa();
            if (sbn != null && sbn.getNotification().actions != null
                    && sbn.getNotification().actions.length > posicion) {
                ultimo = sbn.getNotification().actions[posicion].title.toString();
                if (titulo.equals(ultimo)) {
                    return;
                }
            }
            Thread.sleep(200);
        }
        throw new AssertionError("El botón " + posicion + " no pasó a «" + titulo + "»; último: " + ultimo);
    }

    @SuppressWarnings("BusyWait") // sondeo con tope
    private boolean esperarEvento(long sesionId, TipoEventoEstudio tipo) throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA_MS;
        while (System.currentTimeMillis() < limite) {
            if (tiene(sesionId, tipo)) {
                return true;
            }
            Thread.sleep(200);
        }
        return false;
    }

    @SuppressWarnings("BusyWait") // sondeo con tope
    private boolean esperarSinNotificacion() throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA_MS;
        while (System.currentTimeMillis() < limite) {
            if (activa() == null) {
                return true;
            }
            Thread.sleep(200);
        }
        return false;
    }

    @SuppressWarnings("BusyWait") // sondeo con tope
    private boolean esperarQueNoHayaSesionEnCurso() throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA_MS;
        while (System.currentTimeMillis() < limite) {
            if (!Pruebas.app().sesionRepository().buscarEnCurso().isPresent()) {
                return true;
            }
            Thread.sleep(200);
        }
        return false;
    }
}
