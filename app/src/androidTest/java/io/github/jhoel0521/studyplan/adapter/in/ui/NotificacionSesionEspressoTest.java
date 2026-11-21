package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.ResumenEjecucionSesion;
import io.github.jhoel0521.studyplan.domain.TecnicaEstudio;
import io.github.jhoel0521.studyplan.domain.TipoEventoEstudio;

/**
 * Pruebas instrumentadas de la notificación de la sesión En curso: estados, botones, privacidad y ciclo
 * de vida del servicio (HU-49, HU-40, RNF-12). Corren solo en el emulador.
 */
@RunWith(AndroidJUnit4.class)
public class NotificacionSesionEspressoTest {

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
    public void libreMuestraMateriaCronometroYLosBotonesPausarYDetener() throws Exception {
        long id = iniciar(TecnicaEstudio.LIBRE);

        Notification n = esperarNotificacion(R.string.notif_sesion_titulo);

        assertEquals(contexto.getString(R.string.materia_general_nombre),
                n.extras.getCharSequence(Notification.EXTRA_TEXT).toString());
        assertTrue(n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER));
        assertFalse(n.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN));
        assertEquals(Notification.VISIBILITY_PUBLIC, n.visibility);
        assertEquals(3, n.actions.length);
        assertEquals(contexto.getString(R.string.sesion_en_curso_pausar), n.actions[0].title.toString());
        assertEquals(contexto.getString(R.string.sesion_en_curso_detener), n.actions[1].title.toString());
        assertEquals(contexto.getString(R.string.sesion_no_molestar_apagado), n.actions[2].title.toString());
        assertNotNull("Debe llevar el logo de la app como icono grande", n.getLargeIcon());
        assertFalse("No debe ser un reproductor: sin sesión multimedia",
                n.extras.containsKey(Notification.EXTRA_MEDIA_SESSION));
        assertEquals("Categoría de estado, no de transporte", Notification.CATEGORY_STATUS, n.category);
        assertNotNull(n.deleteIntent);
        assertTrue("El servicio debe estar corriendo", esperarServicio(true));
        assertTrue(id > 0);
    }

    @Test
    public void pomodoroMuestraLaFaseYUnaCuentaAtras() throws Exception {
        Pruebas.app().configurarAjustesEstudio().ejecutar(true, 25, 5, 15, 4);
        iniciar(TecnicaEstudio.POMODORO);

        Notification n = esperarNotificacionCon(contexto.getString(
                R.string.sesion_en_curso_fase_trabajo, 1, 1));

        assertTrue(n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER));
        assertTrue(n.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN));
    }

    @Test
    public void laPausaMuestraReanudarYApagaElCronometro() throws Exception {
        long id = iniciar(TecnicaEstudio.LIBRE);
        esperarNotificacion(R.string.notif_sesion_titulo);

        assertTrue(Pruebas.app().pausarReanudarSesion().ejecutar(id).esValido());

        Notification n = esperarNotificacion(R.string.notif_sesion_pausa_titulo);
        assertFalse(n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER));
        assertEquals(contexto.getString(R.string.sesion_en_curso_reanudar), n.actions[0].title.toString());
        assertEquals(contexto.getString(R.string.sesion_en_curso_detener), n.actions[1].title.toString());
    }

    @Test
    public void laSalidaDeLaAppDetieneElCronometroYMuestraVolver() throws Exception {
        long id = iniciar(TecnicaEstudio.LIBRE);
        esperarNotificacion(R.string.notif_sesion_titulo);

        long ahora = Pruebas.app().reloj().ahoraMilis();
        Pruebas.app().eventoEstudioRepository().guardar(EventoEstudio.crear(
                id, TipoEventoEstudio.SALIDA_APP, ahora,
                Pruebas.app().reloj().fechaDe(ahora), Pruebas.app().reloj().horaDe(ahora), "").valor());
        NotificadorSesion.refrescar(contexto, id);

        Notification n = esperarNotificacion(R.string.notif_volver_titulo);
        assertFalse(n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER));
        assertEquals(3, n.actions.length);
    }

    @Test
    public void detenerLaSesionRetiraLaNotificacionYElServicio() throws Exception {
        long id = iniciar(TecnicaEstudio.LIBRE);
        esperarNotificacion(R.string.notif_sesion_titulo);
        assertTrue(esperarServicio(true));

        assertTrue(Pruebas.app().detenerSesion().ejecutar(id).esValido());

        assertTrue("La notificación debe desaparecer", esperarSinNotificacion());
        assertTrue("El servicio debe detenerse", esperarServicio(false));
    }

    @Test
    public void conElBloqueoDeLaAppActivoSeOcultaLaMateriaYSeProtegenLosBotones() throws Exception {
        long id = iniciar(TecnicaEstudio.LIBRE);
        ResumenEjecucionSesion resumen =
                Pruebas.app().consultarEjecucionSesion().ejecutar(id).get();

        Notification n = NotificadorSesion.construirNotificacionSesion(
                contexto, id, resumen, true, System.currentTimeMillis());

        assertEquals(Notification.VISIBILITY_PRIVATE, n.visibility);
        assertNotNull(n.publicVersion);
        String textoPublico = n.publicVersion.extras.getCharSequence(Notification.EXTRA_TEXT).toString();
        assertFalse(textoPublico.contains(contexto.getString(R.string.materia_general_nombre)));
        assertEquals(3, n.publicVersion.actions.length);
        assertNotNull(n.publicVersion.getLargeIcon());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertTrue(n.publicVersion.actions[0].isAuthenticationRequired());
            assertTrue(n.publicVersion.actions[1].isAuthenticationRequired());
            assertFalse(n.actions[0].isAuthenticationRequired());
        }
    }

    /** RNF-12, HU-49: con el bloqueo de la app y la sesión en pausa se muestra el tiempo, sin la materia. */
    @Test
    public void conElBloqueoDeLaAppYEnPausaSeMuestraElTiempoSinLaMateria() throws Exception {
        long id = iniciar(TecnicaEstudio.LIBRE);
        Pruebas.app().pausarReanudarSesion().ejecutar(id);
        ResumenEjecucionSesion resumen =
                Pruebas.app().consultarEjecucionSesion().ejecutar(id).get();

        Notification n = NotificadorSesion.construirNotificacionSesion(
                contexto, id, resumen, true, System.currentTimeMillis());

        String texto = n.publicVersion.extras.getCharSequence(Notification.EXTRA_TEXT).toString();
        assertFalse("No debe mostrar la materia", texto.contains(contexto.getString(R.string.materia_general_nombre)));
        assertTrue("Debe mostrar el tiempo detenido: " + texto, texto.matches(".*[0-9]{2}:[0-9]{2}.*"));
    }

    @Test
    public void unaSesionQueYaNoEstaEnCursoNoDejaNotificacion() throws Exception {
        long id = iniciar(TecnicaEstudio.LIBRE);
        esperarNotificacion(R.string.notif_sesion_titulo);
        Pruebas.app().detenerSesion().ejecutar(id);
        esperarSinNotificacion();

        NotificadorSesion.refrescar(contexto, id);

        assertNull(activa());
    }

    private long iniciar(TecnicaEstudio tecnica) {
        StudyPlanApplication app = Pruebas.app();
        long id = app.estudiarAhora()
                .ejecutar(null, tecnica, PreferenciaNoMolestar.HEREDAR_GLOBAL)
                .valor().id();
        app.contadorActividades().consumirEvaluacionRecuperacion();
        return id;
    }

    private StatusBarNotification activa() {
        for (StatusBarNotification sbn : nm.getActiveNotifications()) {
            if (sbn.getId() == NotificadorSesion.NOTIF_SESION_ID) {
                return sbn;
            }
        }
        return null;
    }

    private Notification esperarNotificacion(int titulo) throws InterruptedException {
        return esperarNotificacionCon(contexto.getString(titulo));
    }

    @SuppressWarnings("BusyWait") // sondeo con tope
    private Notification esperarNotificacionCon(String titulo) throws InterruptedException {
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
    private boolean esperarServicio(boolean activo) throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA_MS;
        while (System.currentTimeMillis() < limite) {
            if (ServicioSesionEnCurso.estaActivo() == activo) {
                return true;
            }
            Thread.sleep(200);
        }
        return false;
    }
}
