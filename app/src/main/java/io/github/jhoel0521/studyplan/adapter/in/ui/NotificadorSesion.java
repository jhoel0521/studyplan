package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import io.github.jhoel0521.studyplan.R;

/**
 * Crea los canales de notificación y emite avisos de retorno, fin planificado, cambio de fase
 * Pomodoro e invasión respetando {@code POST_NOTIFICATIONS} (HU-23, HU-40, HU-42, RNF-18).
 */
public final class NotificadorSesion {

    public static final String CANAL_SESION_ACTIVA = "canal_sesion_activa";
    public static final String CANAL_AVISOS_ESTUDIO = "canal_avisos_estudio";
    public static final int NOTIF_RETORNO_ID = 4001;
    public static final int NOTIF_HITO_ID = 4002;

    private NotificadorSesion() {
    }

    public static void asegurarCanales(Context contexto) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager nm = (NotificationManager) contexto.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        NotificationChannel activa = new NotificationChannel(
                CANAL_SESION_ACTIVA,
                contexto.getString(R.string.canal_sesion_activa_nombre),
                NotificationManager.IMPORTANCE_LOW);
        NotificationChannel avisos = new NotificationChannel(
                CANAL_AVISOS_ESTUDIO,
                contexto.getString(R.string.canal_avisos_estudio_nombre),
                NotificationManager.IMPORTANCE_HIGH);
        nm.createNotificationChannel(activa);
        nm.createNotificationChannel(avisos);
    }

    public static boolean puedeNotificar(Context contexto) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                    contexto, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static void mostrarAvisoRetorno(Context contexto, long sesionId, String nombreMateria) {
        if (!puedeNotificar(contexto)) {
            return;
        }
        asegurarCanales(contexto);
        Intent abrir = new Intent(contexto, SesionEnCursoActivity.class);
        abrir.putExtra(SesionEnCursoActivity.EXTRA_SESION_ID, sesionId);
        abrir.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                contexto,
                NOTIF_RETORNO_ID,
                abrir,
                flagsCompat(PendingIntent.FLAG_UPDATE_CURRENT));

        NotificationCompat.Builder b = new NotificationCompat.Builder(contexto, CANAL_SESION_ACTIVA)
                .setSmallIcon(R.drawable.ic_estado_en_curso)
                .setContentTitle(contexto.getString(R.string.notif_volver_titulo))
                .setContentText(contexto.getString(R.string.notif_volver_cuerpo, nombreMateria))
                .setAutoCancel(true)
                .setContentIntent(pi);
        NotificationManager nm = (NotificationManager) contexto.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIF_RETORNO_ID, b.build());
        }
    }

    public static void cancelarAvisoRetorno(Context contexto) {
        NotificationManager nm = (NotificationManager) contexto.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(NOTIF_RETORNO_ID);
            nm.cancel(NOTIF_HITO_ID);
        }
    }

    public static void mostrarHito(
            Context contexto, long sesionId, String tipoHito, String nombreMateria) {
        vibrarYSonar(contexto);
        if (!puedeNotificar(contexto)) {
            return;
        }
        asegurarCanales(contexto);
        Intent abrir = new Intent(contexto, SesionEnCursoActivity.class);
        abrir.putExtra(SesionEnCursoActivity.EXTRA_SESION_ID, sesionId);
        abrir.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                contexto,
                NOTIF_HITO_ID,
                abrir,
                flagsCompat(PendingIntent.FLAG_UPDATE_CURRENT));

        String titulo;
        String cuerpo;
        boolean conAccionesFin = false;
        if ("CAMBIO_FASE_POMODORO".equals(tipoHito)) {
            titulo = contexto.getString(R.string.notif_pomodoro_titulo);
            cuerpo = contexto.getString(R.string.notif_pomodoro_cuerpo, nombreMateria);
        } else if ("INVASION_SESION".equals(tipoHito)) {
            titulo = contexto.getString(R.string.notif_invasion_titulo);
            cuerpo = contexto.getString(R.string.notif_invasion_cuerpo, nombreMateria);
        } else {
            titulo = contexto.getString(R.string.notif_fin_plan_titulo);
            cuerpo = contexto.getString(R.string.notif_fin_plan_cuerpo, nombreMateria);
            conAccionesFin = true;
        }

        NotificationCompat.Builder b = new NotificationCompat.Builder(contexto, CANAL_AVISOS_ESTUDIO)
                .setSmallIcon(R.drawable.ic_estado_en_curso)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setAutoCancel(true)
                .setContentIntent(pi);

        if (conAccionesFin) {
            b.addAction(
                    R.drawable.ic_estado_en_curso,
                    contexto.getString(R.string.notif_accion_sigo),
                    piAccion(contexto, sesionId, ReceptorAvisosSesion.ACCION_SIGO, 1));
            b.addAction(
                    R.drawable.ic_estado_completada,
                    contexto.getString(R.string.notif_accion_termine),
                    piAccion(contexto, sesionId, ReceptorAvisosSesion.ACCION_TERMINE, 2));
        }

        NotificationManager nm = (NotificationManager) contexto.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIF_HITO_ID, b.build());
        }
    }

    private static PendingIntent piAccion(
            Context contexto, long sesionId, String accion, int reqOffset) {
        Intent i = new Intent(contexto, ReceptorAvisosSesion.class);
        i.setAction(accion);
        i.putExtra(ReceptorAvisosSesion.EXTRA_SESION_ID, sesionId);
        return PendingIntent.getBroadcast(
                contexto,
                NOTIF_HITO_ID + reqOffset,
                i,
                flagsCompat(PendingIntent.FLAG_UPDATE_CURRENT));
    }

    private static void vibrarYSonar(Context contexto) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) contexto.getSystemService(
                        Context.VIBRATOR_MANAGER_SERVICE);
                if (vm != null) {
                    vm.getDefaultVibrator().vibrate(
                            VibrationEffect.createOneShot(300L, VibrationEffect.DEFAULT_AMPLITUDE));
                }
            } else {
                Vibrator v = (Vibrator) contexto.getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null && v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(
                                300L, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        v.vibrate(300L);
                    }
                }
            }
            android.net.Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (uri != null) {
                android.media.Ringtone r = RingtoneManager.getRingtone(contexto, uri);
                if (r != null) {
                    r.play();
                }
            }
        } catch (RuntimeException ignored) {
            // En emuladores sin hardware de vibración/audio o permisos restringidos no abortar
        }
    }

    private static int flagsCompat(int base) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return base | PendingIntent.FLAG_IMMUTABLE;
        }
        return base;
    }
}
