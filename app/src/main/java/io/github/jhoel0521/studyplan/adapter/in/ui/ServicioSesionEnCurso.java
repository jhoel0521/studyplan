package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * Servicio en primer plano que sostiene la notificación de la sesión mientras esté {@code EN_CURSO}
 * (HU-49). No ejecuta temporizadores ni lógica de negocio: recibe la notificación ya construida y se
 * detiene cuando la sesión deja de estar En curso. Si el sistema impide iniciarlo desde segundo plano,
 * la notificación se publica sin primer plano.
 */
public final class ServicioSesionEnCurso extends Service {

    static final String EXTRA_NOTIFICACION = "io.github.jhoel0521.studyplan.EXTRA_NOTIFICACION_SESION";

    private static volatile boolean activo;

    /** Publica la notificación: actualiza si el servicio ya corre; si no, lo inicia con ella. */
    static void mostrar(Context contexto, Notification notificacion) {
        NotificationManager nm = (NotificationManager) contexto.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (activo) {
            if (nm != null) {
                nm.notify(NotificadorSesion.NOTIF_SESION_ID, notificacion);
            }
            return;
        }
        try {
            Intent intento = new Intent(contexto, ServicioSesionEnCurso.class)
                    .putExtra(EXTRA_NOTIFICACION, notificacion);
            ContextCompat.startForegroundService(contexto, intento);
        } catch (RuntimeException e) {
            // El sistema rechazó iniciar un servicio en primer plano desde segundo plano (Android 12 o
            // posterior): se publica la notificación sin servicio y se reintenta al abrir la app.
            if (nm != null) {
                nm.notify(NotificadorSesion.NOTIF_SESION_ID, notificacion);
            }
        }
    }

    /** Detiene el servicio si corre; no toca la notificación. */
    static void detener(Context contexto) {
        contexto.stopService(new Intent(contexto, ServicioSesionEnCurso.class));
    }

    static boolean estaActivo() {
        return activo;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        activo = true;
    }

    @Override
    public int onStartCommand(Intent intento, int banderas, int idInicio) {
        Notification notificacion = leerNotificacion(intento);
        if (notificacion == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NotificadorSesion.NOTIF_SESION_ID, notificacion,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NotificadorSesion.NOTIF_SESION_ID, notificacion, 0);
        } else {
            startForeground(NotificadorSesion.NOTIF_SESION_ID, notificacion);
        }
        return START_NOT_STICKY;
    }

    @SuppressWarnings("deprecation")
    @Nullable
    private static Notification leerNotificacion(@Nullable Intent intento) {
        if (intento == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intento.getParcelableExtra(EXTRA_NOTIFICACION, Notification.class);
        }
        return intento.getParcelableExtra(EXTRA_NOTIFICACION);
    }

    @Override
    public void onDestroy() {
        activo = false;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intento) {
        return null;
    }
}
