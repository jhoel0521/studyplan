package io.github.jhoel0521.studyplan.adapter.out.system;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import io.github.jhoel0521.studyplan.application.port.out.system.ProgramadorAvisosSesion;
import io.github.jhoel0521.studyplan.application.port.out.system.ProgramadorRecordatorios;

/**
 * Programa hitos de sesiones {@code EN_CURSO} ({@link AlarmManager#setAlarmClock}) y recordatorios
 * previos de sesiones {@code PENDIENTE} ({@code setExactAndAllowWhileIdle} /
 * {@code setAndAllowWhileIdle}) con cancelación conjunta de alarma y notificación (HU-23, HU-34,
 * HU-42, HU-43, RN-25, RN-27).
 */
public final class ProgramadorAvisosAlarmManager
        implements ProgramadorAvisosSesion, ProgramadorRecordatorios {

    public static final String ACCION_HITO_SESION =
            "io.github.jhoel0521.studyplan.ACCION_HITO_SESION";
    public static final String ACCION_RECORDATORIO_SESION =
            "io.github.jhoel0521.studyplan.ACCION_RECORDATORIO_SESION";
    public static final String EXTRA_SESION_ID = "extra_sesion_id";
    public static final String EXTRA_TIPO_HITO = "extra_tipo_hito";
    public static final String EXTRA_NOMBRE_MATERIA = "extra_nombre_materia";
    public static final int NOTIF_RECORDATORIO_BASE = 10_000;

    private static final String PREFS_RECORDATORIOS = "studyplan_recordatorios_programados";
    private static final String CLAVE_IDS_PROGRAMADOS = "ids_sesiones";
    private static final String PREFIJO_URI_RECORDATORIO = "studyplan://recordatorio/";

    private final Context contexto;
    private final AlarmManager alarmManager;
    private final Class<?> receptorClase;
    private final Class<?> actividadMostrarClase;

    public ProgramadorAvisosAlarmManager(
            Context contexto, Class<?> receptorClase, Class<?> actividadMostrarClase) {
        this.contexto = Objects.requireNonNull(contexto, "contexto").getApplicationContext();
        this.alarmManager = (AlarmManager) this.contexto.getSystemService(Context.ALARM_SERVICE);
        this.receptorClase = Objects.requireNonNull(receptorClase, "receptorClase");
        this.actividadMostrarClase =
                Objects.requireNonNull(actividadMostrarClase, "actividadMostrarClase");
    }

    @Override
    public void programarSiguienteHito(
            long sesionId, TipoHito tipo, long instanteDisparoMilis, String nombreMateria) {
        if (alarmManager == null || tipo == null) {
            return;
        }
        PendingIntent operacion = construirPendingIntent(
                sesionId, tipo, nombreMateria, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent mostrarIntent = new Intent(contexto, actividadMostrarClase);
        mostrarIntent.putExtra(EXTRA_SESION_ID, sesionId);
        PendingIntent showPi = PendingIntent.getActivity(
                contexto,
                codigoSolicitud(sesionId),
                mostrarIntent,
                flagsCompat(PendingIntent.FLAG_UPDATE_CURRENT));
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAlarmClock(
                        new AlarmManager.AlarmClockInfo(instanteDisparoMilis, showPi), operacion);
            } else {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, instanteDisparoMilis, operacion);
            }
        } catch (SecurityException e) {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, instanteDisparoMilis, operacion);
        }
    }

    @Override
    public void cancelarAvisos(long sesionId) {
        if (alarmManager == null) {
            return;
        }
        PendingIntent operacion = construirPendingIntent(
                sesionId, TipoHito.FIN_PLANIFICADO, "", PendingIntent.FLAG_NO_CREATE);
        if (operacion != null) {
            alarmManager.cancel(operacion);
            operacion.cancel();
        }
    }

    @Override
    public void programar(long sesionId, long instanteDisparoUtcMilis) {
        if (alarmManager == null || sesionId <= 0) {
            return;
        }
        PendingIntent operacion = construirPendingIntentRecordatorio(
                sesionId, PendingIntent.FLAG_UPDATE_CURRENT);
        registrarRecordatorioProgramado(sesionId);
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                    || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, instanteDisparoUtcMilis, operacion);
            } else {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, instanteDisparoUtcMilis, operacion);
            }
        } catch (SecurityException | IllegalStateException e) {
            try {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, instanteDisparoUtcMilis, operacion);
            } catch (RuntimeException ignored) {
                // Evita abortar la persistencia si el sistema rechaza alarmas adicionales
            }
        }
    }

    @Override
    public void cancelar(long sesionId) {
        if (sesionId <= 0) {
            return;
        }
        if (alarmManager != null) {
            PendingIntent operacion = construirPendingIntentRecordatorio(
                    sesionId, PendingIntent.FLAG_NO_CREATE);
            if (operacion != null) {
                alarmManager.cancel(operacion);
                operacion.cancel();
            }
        }
        eliminarRecordatorioProgramado(sesionId);
        NotificationManager nm = (NotificationManager) contexto.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(idNotificacionRecordatorio(sesionId));
        }
    }

    @Override
    public void cancelarTodasNotificaciones() {
        Set<String> ids = obtenerYLimpiarRecordatoriosProgramados();
        if (alarmManager != null) {
            for (String raw : ids) {
                try {
                    long sesionId = Long.parseLong(raw);
                    PendingIntent operacion = construirPendingIntentRecordatorio(
                            sesionId, PendingIntent.FLAG_NO_CREATE);
                    if (operacion != null) {
                        alarmManager.cancel(operacion);
                        operacion.cancel();
                    }
                } catch (NumberFormatException ignored) {
                    // Entrada corrupta ignorada
                }
            }
        }
        NotificationManager nm = (NotificationManager) contexto.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (nm != null) {
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
    }

    /** Devuelve el identificador de notificación determinista reservado para una sesión. */
    public static int idNotificacionRecordatorio(long sesionId) {
        int sufijo = (int) ((sesionId ^ (sesionId >>> 32)) & 0x0FFF_FFFFL);
        return NOTIF_RECORDATORIO_BASE + sufijo;
    }

    static Intent intentRecordatorio(
            Context contexto, Class<?> receptorClase, long sesionId) {
        Intent intent = new Intent(contexto, receptorClase);
        intent.setAction(ACCION_RECORDATORIO_SESION);
        intent.setData(Uri.parse(PREFIJO_URI_RECORDATORIO + sesionId));
        intent.putExtra(EXTRA_SESION_ID, sesionId);
        return intent;
    }

    private PendingIntent construirPendingIntentRecordatorio(long sesionId, int flagBase) {
        Intent intent = intentRecordatorio(contexto, receptorClase, sesionId);
        return PendingIntent.getBroadcast(
                contexto, codigoSolicitud(sesionId), intent, flagsCompat(flagBase));
    }

    private synchronized void registrarRecordatorioProgramado(long sesionId) {
        SharedPreferences prefs = contexto.getSharedPreferences(
                PREFS_RECORDATORIOS, Context.MODE_PRIVATE);
        Set<String> actuales = prefs.getStringSet(CLAVE_IDS_PROGRAMADOS, null);
        Set<String> nuevos = actuales == null ? new HashSet<>() : new HashSet<>(actuales);
        nuevos.add(String.valueOf(sesionId));
        prefs.edit().putStringSet(CLAVE_IDS_PROGRAMADOS, nuevos).commit();
    }

    private synchronized void eliminarRecordatorioProgramado(long sesionId) {
        SharedPreferences prefs = contexto.getSharedPreferences(
                PREFS_RECORDATORIOS, Context.MODE_PRIVATE);
        Set<String> actuales = prefs.getStringSet(CLAVE_IDS_PROGRAMADOS, null);
        if (actuales == null || actuales.isEmpty()) {
            return;
        }
        Set<String> nuevos = new HashSet<>(actuales);
        if (nuevos.remove(String.valueOf(sesionId))) {
            prefs.edit().putStringSet(CLAVE_IDS_PROGRAMADOS, nuevos).commit();
        }
    }

    private synchronized Set<String> obtenerYLimpiarRecordatoriosProgramados() {
        SharedPreferences prefs = contexto.getSharedPreferences(
                PREFS_RECORDATORIOS, Context.MODE_PRIVATE);
        Set<String> actuales = prefs.getStringSet(CLAVE_IDS_PROGRAMADOS, null);
        Set<String> copia = actuales == null ? new HashSet<>() : new HashSet<>(actuales);
        prefs.edit().remove(CLAVE_IDS_PROGRAMADOS).commit();
        return copia;
    }

    private PendingIntent construirPendingIntent(
            long sesionId, TipoHito tipo, String nombreMateria, int flagBase) {
        Intent intent = new Intent(contexto, receptorClase);
        intent.setAction(ACCION_HITO_SESION);
        intent.putExtra(EXTRA_SESION_ID, sesionId);
        intent.putExtra(EXTRA_TIPO_HITO, tipo.name());
        intent.putExtra(EXTRA_NOMBRE_MATERIA, nombreMateria == null ? "" : nombreMateria);
        return PendingIntent.getBroadcast(
                contexto, codigoSolicitud(sesionId), intent, flagsCompat(flagBase));
    }

    static int codigoSolicitud(long sesionId) {
        return (int) (sesionId ^ (sesionId >>> 32));
    }

    static int flagsCompat(int base) {
        return base | PendingIntent.FLAG_IMMUTABLE;
    }
}
