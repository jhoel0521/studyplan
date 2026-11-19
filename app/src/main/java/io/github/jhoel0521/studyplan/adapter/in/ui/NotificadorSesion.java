package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.adapter.out.system.ProgramadorAvisosAlarmManager;

/**
 * Crea los canales de notificación, emite avisos de retorno, fin planificado, cambio de fase
 * Pomodoro, invasión y recordatorios previos de sesiones respetando {@code POST_NOTIFICATIONS},
 * bloqueo de pantalla y alarmas exactas (HU-23, HU-34, HU-40, HU-42, RNF-18).
 */
public final class NotificadorSesion {

    public static final String CANAL_SESION_ACTIVA = "canal_sesion_activa";
    public static final String CANAL_AVISOS_ESTUDIO = "canal_avisos_estudio";
    public static final int NOTIF_RETORNO_ID = 4001;
    public static final int NOTIF_HITO_ID = 4002;
    public static final int NOTIF_RECORDATORIO_BASE =
            ProgramadorAvisosAlarmManager.NOTIF_RECORDATORIO_BASE;

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

    /** Alias legible del estado de {@code POST_NOTIFICATIONS} para vistas y ajustes ({@code RNF-18}). */
    public static boolean tienePermisoNotificaciones(Context contexto) {
        return puedeNotificar(contexto);
    }

    /** Indica si el sistema permite programar alarmas exactas para recordatorios ({@code RNF-18}). */
    public static boolean puedeProgramarAlarmasExactas(Context contexto) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        AlarmManager am = (AlarmManager) contexto.getSystemService(Context.ALARM_SERVICE);
        return am != null && am.canScheduleExactAlarms();
    }

    /** Construye el intent del sistema hacia ajustes de notificaciones de la app. */
    public static Intent intentAjustesNotificaciones(Context contexto) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.getPackageName());
        }
        return intentDetallesAplicacion(contexto);
    }

    /** Construye el intent del sistema hacia "Alarmas y recordatorios" en Android 12+. */
    public static Intent intentAjustesAlarmasExactas(Context contexto) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.fromParts("package", contexto.getPackageName(), null));
        }
        return intentDetallesAplicacion(contexto);
    }

    /** Abre la configuración del sistema de notificaciones sin fallar ante ROMs restringidas. */
    public static void abrirAjustesNotificaciones(Context contexto) {
        abrirConRespaldo(contexto, intentAjustesNotificaciones(contexto));
    }

    /** Abre la configuración del sistema de alarmas exactas con respaldo seguro. */
    public static void abrirAjustesAlarmasExactas(Context contexto) {
        abrirConRespaldo(contexto, intentAjustesAlarmasExactas(contexto));
    }

    private static Intent intentDetallesAplicacion(Context contexto) {
        return new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", contexto.getPackageName(), null));
    }

    private static void abrirConRespaldo(Context contexto, Intent primario) {
        if (!(contexto instanceof Activity)) {
            primario.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        try {
            contexto.startActivity(primario);
        } catch (ActivityNotFoundException | SecurityException e) {
            Intent respaldo = intentDetallesAplicacion(contexto);
            if (!(contexto instanceof Activity)) {
                respaldo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            try {
                contexto.startActivity(respaldo);
            } catch (ActivityNotFoundException | SecurityException ignored) {
                // Evita caída si no hay Activity de ajustes disponible en el entorno
            }
        }
    }

    /** Devuelve el identificador determinista de notificación de recordatorio por sesión. */
    public static int idRecordatorio(long sesionId) {
        return ProgramadorAvisosAlarmManager.idNotificacionRecordatorio(sesionId);
    }

    /**
     * Emite la notificación local previa de recordatorio usando {@code R.string.sesion_rango_horario}
     * sin activar {@code vibrarYSonar()} y protegiendo la pantalla bloqueada si {@code bloqueoActivo}.
     */
    public static void mostrarRecordatorio(
            Context contexto,
            long sesionId,
            String nombreMateria,
            String horaInicio,
            String horaFin,
            boolean bloqueoActivo) {
        String horario = contexto.getString(R.string.sesion_rango_horario, horaInicio, horaFin);
        mostrarRecordatorio(contexto, sesionId, nombreMateria, horario, bloqueoActivo);
    }

    /**
     * Emite la notificación local previa de recordatorio con apertura directa de detalle y acción
     * «Iniciar» hacia {@link SesionEnCursoActivity} sin {@code FLAG_ACTIVITY_CLEAR_TASK}.
     */
    public static void mostrarRecordatorio(
            Context contexto,
            long sesionId,
            String nombreMateria,
            String horarioTexto,
            boolean bloqueoActivo) {
        Notification notification = construirNotificacionRecordatorio(
                contexto, sesionId, nombreMateria, horarioTexto, bloqueoActivo);
        if (notification == null || !puedeNotificar(contexto)) {
            return;
        }
        asegurarCanales(contexto);
        NotificationManager nm = (NotificationManager) contexto.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(idRecordatorio(sesionId), notification);
        }
    }

    static Notification construirNotificacionRecordatorio(
            Context contexto,
            long sesionId,
            String nombreMateria,
            String horaInicio,
            String horaFin,
            boolean bloqueoActivo) {
        String horario = contexto.getString(R.string.sesion_rango_horario, horaInicio, horaFin);
        return construirNotificacionRecordatorio(
                contexto, sesionId, nombreMateria, horario, bloqueoActivo);
    }

    static Notification construirNotificacionRecordatorio(
            Context contexto,
            long sesionId,
            String nombreMateria,
            String horarioTexto,
            boolean bloqueoActivo) {
        if (sesionId <= 0) {
            return null;
        }
        String materiaSegura = nombreMateria == null ? "" : nombreMateria;
        String horarioSeguro = horarioTexto == null ? "" : horarioTexto;

        Intent abrirDetalle = new Intent(contexto, SesionDetalleActivity.class);
        abrirDetalle.putExtra(SesionDetalleActivity.EXTRA_SESION_ID, sesionId);
        abrirDetalle.setData(Uri.parse("studyplan://detalle/" + sesionId));
        abrirDetalle.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent piDetalle = PendingIntent.getActivity(
                contexto,
                idRecordatorio(sesionId),
                abrirDetalle,
                flagsCompat(PendingIntent.FLAG_UPDATE_CURRENT));

        Intent iniciarDirecto = new Intent(contexto, SesionEnCursoActivity.class);
        iniciarDirecto.putExtra(SesionEnCursoActivity.EXTRA_SESION_ID, sesionId);
        iniciarDirecto.putExtra(SesionEnCursoActivity.EXTRA_INICIAR_DESDE_RECORDATORIO, true);
        iniciarDirecto.setData(Uri.parse("studyplan://en_curso/recordatorio/" + sesionId));
        iniciarDirecto.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent piIniciar = PendingIntent.getActivity(
                contexto,
                idRecordatorio(sesionId) + 100_000,
                iniciarDirecto,
                flagsCompat(PendingIntent.FLAG_UPDATE_CURRENT));

        String titulo = contexto.getString(R.string.notif_recordatorio_titulo);
        String cuerpo = contexto.getString(
                R.string.notif_recordatorio_cuerpo, materiaSegura, horarioSeguro);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(contexto, CANAL_AVISOS_ESTUDIO)
                .setSmallIcon(R.drawable.ic_estado_pendiente)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setAutoCancel(true)
                .setContentIntent(piDetalle)
                .addAction(
                        R.drawable.ic_estado_en_curso,
                        contexto.getString(R.string.notif_recordatorio_accion_iniciar),
                        piIniciar);

        if (bloqueoActivo) {
            Notification versionPublica = new NotificationCompat.Builder(
                    contexto, CANAL_AVISOS_ESTUDIO)
                    .setSmallIcon(R.drawable.ic_estado_pendiente)
                    .setContentTitle(titulo)
                    .setContentText(contexto.getString(R.string.notif_recordatorio_privado))
                    .build();
            builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setPublicVersion(versionPublica);
        } else {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }
        return builder.build();
    }

    /** Cancela la notificación de recordatorio de la sesión indicada si sigue visible. */
    public static void cancelarRecordatorio(Context contexto, long sesionId) {
        if (sesionId <= 0) {
            return;
        }
        NotificationManager nm = (NotificationManager) contexto.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(idRecordatorio(sesionId));
        }
    }

    /** Cancela todas las notificaciones de recordatorio activas sin tocar avisos en curso. */
    public static void cancelarTodosRecordatorios(Context contexto) {
        NotificationManager nm = (NotificationManager) contexto.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (StatusBarNotification activa : nm.getActiveNotifications()) {
                if (activa.getId() >= NOTIF_RECORDATORIO_BASE) {
                    nm.cancel(activa.getTag(), activa.getId());
                }
            }
        } else {
            nm.cancelAll();
        }
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
