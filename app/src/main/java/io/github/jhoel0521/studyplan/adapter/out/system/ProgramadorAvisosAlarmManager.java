package io.github.jhoel0521.studyplan.adapter.out.system;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.out.ProgramadorAvisosSesion;

/**
 * Programa el próximo hito crítico mediante {@link AlarmManager#setAlarmClock} para tolerar Doze
 * sin requerir {@code SCHEDULE_EXACT_ALARM} (HU-23, HU-42, HU-43, RN-25, RN-27).
 */
public final class ProgramadorAvisosAlarmManager implements ProgramadorAvisosSesion {

    public static final String ACCION_HITO_SESION =
            "io.github.jhoel0521.studyplan.ACCION_HITO_SESION";
    public static final String EXTRA_SESION_ID = "extra_sesion_id";
    public static final String EXTRA_TIPO_HITO = "extra_tipo_hito";
    public static final String EXTRA_NOMBRE_MATERIA = "extra_nombre_materia";

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

    private static int codigoSolicitud(long sesionId) {
        return (int) (sesionId ^ (sesionId >>> 32));
    }

    private static int flagsCompat(int base) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return base | PendingIntent.FLAG_IMMUTABLE;
        }
        return base;
    }
}
