package io.github.jhoel0521.studyplan.adapter.in.ui.tecnicas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.jhoel0521.studyplan.R;

/**
 * Pruebas instrumentadas de las extensiones de recordatorios y helpers de permisos en
 * {@link NotificadorSesion} ({@code HU-34}, {@code RNF-18}).
 */
@RunWith(AndroidJUnit4.class)
public final class NotificadorRecordatoriosInstrumentalTest {

    private Context context;
    private NotificationManager nm;

    @Before
    public void preparar() {
        context = ApplicationProvider.getApplicationContext();
        nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .grantRuntimePermission(
                            context.getPackageName(), Manifest.permission.POST_NOTIFICATIONS);
        }
        NotificadorSesion.asegurarCanales(context);
        if (nm != null) {
            nm.cancelAll();
        }
    }

    @After
    public void limpiar() {
        NotificadorSesion.cancelarTodosRecordatorios(context);
        if (nm != null) {
            nm.cancelAll();
        }
    }

    @Test
    public void notificacionConBloqueoActivoUsaVisibilityPrivateYPublicVersionSinDatosSensibles() {
        Notification n = NotificadorSesion.construirNotificacionRecordatorio(
                context, 77L, "Álgebra", "14:00", "15:30", true);
        assertNotNull(n);
        assertEquals(Notification.VISIBILITY_PRIVATE, n.visibility);
        assertNotNull(n.publicVersion);

        CharSequence textoPublico = n.publicVersion.extras.getCharSequence(Notification.EXTRA_TEXT);
        assertNotNull(textoPublico);
        assertFalse(textoPublico.toString().contains("Álgebra"));
        assertTrue(n.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("Álgebra"));
        assertTrue(n.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains(
                context.getString(R.string.sesion_rango_horario, "14:00", "15:30")));

        assertNotNull(n.contentIntent);
        assertNotNull(n.actions);
        assertEquals(1, n.actions.length);
        assertEquals(
                context.getString(R.string.notif_recordatorio_accion_iniciar),
                n.actions[0].title.toString());
        assertNotNull(n.actions[0].actionIntent);
    }

    @Test
    public void mostrarYCancelarRecordatoriosUsaBaseMayorA10000YNoBorraAvisosEnCurso() {
        Notification sinBloqueo = NotificadorSesion.construirNotificacionRecordatorio(
                context, 88L, "Historia", "09:00", "10:00", false);
        assertNotNull(sinBloqueo);
        assertEquals(Notification.VISIBILITY_PUBLIC, sinBloqueo.visibility);
        assertNull(sinBloqueo.publicVersion);

        NotificadorSesion.mostrarRecordatorio(context, 88L, "Historia", "09:00", "10:00", false);
        NotificadorSesion.mostrarRecordatorio(context, 89L, "Física", "11:00", "12:00", true);
        Notification ajena = new NotificationCompat.Builder(
                context, NotificadorSesion.CANAL_AVISOS_ESTUDIO)
                .setSmallIcon(R.drawable.ic_estado_en_curso)
                .setContentTitle("Hito")
                .setContentText("Activo")
                .build();
        nm.notify(NotificadorSesion.NOTIF_HITO_ID, ajena);

        int id88 = NotificadorSesion.idRecordatorio(88L);
        int id89 = NotificadorSesion.idRecordatorio(89L);
        assertTrue(id88 >= NotificadorSesion.NOTIF_RECORDATORIO_BASE);
        assertTrue(esperarNotificacion(id88, true));
        assertTrue(esperarNotificacion(id89, true));

        NotificadorSesion.cancelarRecordatorio(context, 88L);
        assertTrue(esperarNotificacion(id88, false));
        assertTrue(estaActiva(id89));

        NotificadorSesion.cancelarTodosRecordatorios(context);
        assertTrue(esperarNotificacion(id89, false));
        assertTrue(estaActiva(NotificadorSesion.NOTIF_HITO_ID));
    }

    @Test
    public void helpersDePermisosYSettingsDevuelvenIntentsValidos() {
        assertTrue(NotificadorSesion.tienePermisoNotificaciones(context));
        // En API 37 puede ser true/false según estado del emulador, pero no debe lanzar excepción
        NotificadorSesion.puedeProgramarAlarmasExactas(context);

        Intent iNotif = NotificadorSesion.intentAjustesNotificaciones(context);
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, iNotif.getAction());
        Intent iAlarmas = NotificadorSesion.intentAjustesAlarmasExactas(context);
        assertEquals(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, iAlarmas.getAction());
    }

    @SuppressWarnings("BusyWait") // NotificationManagerService en system_server es asíncrono vía Binder
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
