package io.github.jhoel0521.studyplan.adapter.out.system;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
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
import io.github.jhoel0521.studyplan.adapter.in.ui.ReceptorAvisosSesion;
import io.github.jhoel0521.studyplan.adapter.in.ui.tecnicas.SesionEnCursoActivity;
import io.github.jhoel0521.studyplan.application.port.out.system.ProgramadorAvisosSesion;

/**
 * Pruebas instrumentadas del adaptador de recordatorios dentro de
 * {@link ProgramadorAvisosAlarmManager} ({@code HU-34}, {@code RNF-18}).
 */
@RunWith(AndroidJUnit4.class)
public final class ProgramadorRecordatoriosAlarmManagerInstrumentalTest {

    private static final String CANAL_PRUEBA = "canal_avisos_estudio";

    private Context context;
    private ProgramadorAvisosAlarmManager programador;
    private NotificationManager nm;

    @Before
    public void preparar() {
        context = ApplicationProvider.getApplicationContext();
        programador = new ProgramadorAvisosAlarmManager(
                context, ReceptorAvisosSesion.class, SesionEnCursoActivity.class);
        nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .grantRuntimePermission(
                            context.getPackageName(), Manifest.permission.POST_NOTIFICATIONS);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
            nm.createNotificationChannel(new NotificationChannel(
                    CANAL_PRUEBA, "Pruebas", NotificationManager.IMPORTANCE_LOW));
        }
        programador.cancelarTodasNotificaciones();
        if (nm != null) {
            nm.cancelAll();
        }
    }

    @After
    public void limpiar() {
        programador.cancelar(10L);
        programador.cancelar(20L);
        programador.cancelar(30L);
        programador.cancelar(41L);
        programador.cancelar(42L);
        programador.cancelarAvisos(10L);
        programador.cancelarTodasNotificaciones();
        if (nm != null) {
            nm.cancelAll();
        }
    }

    @Test
    public void programarYCancelarUsaUriUnicaYSeparadaDeHitosEnCurso() {
        long futuro = System.currentTimeMillis() + 3_600_000L;
        programador.programar(10L, futuro);
        programador.programar(20L, futuro + 60_000L);
        programador.programarSiguienteHito(
                10L, ProgramadorAvisosSesion.TipoHito.FIN_PLANIFICADO, futuro, "Álgebra");

        assertNotNull(buscarPendingIntentRecordatorio(10L));
        assertNotNull(buscarPendingIntentRecordatorio(20L));
        assertNotNull(buscarPendingIntentHito(10L));

        programador.cancelar(10L);

        assertNull(buscarPendingIntentRecordatorio(10L));
        assertNotNull(buscarPendingIntentRecordatorio(20L));
        assertNotNull(buscarPendingIntentHito(10L));
    }

    @Test
    public void cancelarEliminaAlarmaYNotificacionActivaYCancelarTodasLimpiaTodo() {
        long futuro = System.currentTimeMillis() + 3_600_000L;
        programador.programar(30L, futuro);
        programador.programar(41L, futuro);
        programador.programar(42L, futuro);

        int notif30 = ProgramadorAvisosAlarmManager.idNotificacionRecordatorio(30L);
        int notif41 = ProgramadorAvisosAlarmManager.idNotificacionRecordatorio(41L);
        int notif42 = ProgramadorAvisosAlarmManager.idNotificacionRecordatorio(42L);
        int notifAjena = 4001;

        publicarNotificacionDummy(notif30);
        publicarNotificacionDummy(notif41);
        publicarNotificacionDummy(notif42);
        publicarNotificacionDummy(notifAjena);

        assertTrue(notificacionActiva(notif30));
        programador.cancelar(30L);
        assertNull(buscarPendingIntentRecordatorio(30L));
        assertFalse(notificacionActiva(notif30));
        assertTrue(notificacionActiva(notif41));

        programador.cancelarTodasNotificaciones();
        assertNull(buscarPendingIntentRecordatorio(41L));
        assertNull(buscarPendingIntentRecordatorio(42L));
        assertFalse(notificacionActiva(notif41));
        assertFalse(notificacionActiva(notif42));
        assertTrue(notificacionActiva(notifAjena));
    }

    private PendingIntent buscarPendingIntentRecordatorio(long sesionId) {
        Intent intent = ProgramadorAvisosAlarmManager.intentRecordatorio(
                context, ReceptorAvisosSesion.class, sesionId);
        return PendingIntent.getBroadcast(
                context,
                ProgramadorAvisosAlarmManager.codigoSolicitud(sesionId),
                intent,
                ProgramadorAvisosAlarmManager.flagsCompat(PendingIntent.FLAG_NO_CREATE));
    }

    private PendingIntent buscarPendingIntentHito(long sesionId) {
        Intent intent = new Intent(context, ReceptorAvisosSesion.class);
        intent.setAction(ProgramadorAvisosAlarmManager.ACCION_HITO_SESION);
        intent.putExtra(ProgramadorAvisosAlarmManager.EXTRA_SESION_ID, sesionId);
        return PendingIntent.getBroadcast(
                context,
                ProgramadorAvisosAlarmManager.codigoSolicitud(sesionId),
                intent,
                ProgramadorAvisosAlarmManager.flagsCompat(PendingIntent.FLAG_NO_CREATE));
    }

    private void publicarNotificacionDummy(int id) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CANAL_PRUEBA)
                .setSmallIcon(R.drawable.ic_estado_en_curso)
                .setContentTitle("Prueba")
                .setContentText("Contenido");
        nm.notify(id, b.build());
    }

    private boolean notificacionActiva(int id) {
        if (nm == null) {
            return false;
        }
        for (StatusBarNotification sbn : nm.getActiveNotifications()) {
            if (sbn.getId() == id) {
                return true;
            }
        }
        return false;
    }
}
