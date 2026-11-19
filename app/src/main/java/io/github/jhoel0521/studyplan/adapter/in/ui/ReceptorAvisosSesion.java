package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.List;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.adapter.out.system.ProgramadorAvisosAlarmManager;
import io.github.jhoel0521.studyplan.application.port.in.DecidirFinPlanificadoOInvasion;
import io.github.jhoel0521.studyplan.domain.AvisoRecordatorioPendiente;
import io.github.jhoel0521.studyplan.domain.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;

/**
 * Receptor de alarmas de hitos, recordatorios previos, acciones de notificación ([Sigo] / [Terminé]),
 * reinicio del dispositivo ({@code BOOT_COMPLETED}), actualización de paquete y cambio de permiso
 * de alarmas exactas (HU-23, HU-34, HU-42, RN-25).
 */
public class ReceptorAvisosSesion extends BroadcastReceiver {

    public static final String ACCION_SIGO = "io.github.jhoel0521.studyplan.ACCION_SIGO";
    public static final String ACCION_TERMINE = "io.github.jhoel0521.studyplan.ACCION_TERMINE";
    public static final String EXTRA_SESION_ID = ProgramadorAvisosAlarmManager.EXTRA_SESION_ID;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String accion = intent.getAction();
        StudyPlanApplication app = StudyPlanApplication.de(context);

        if (Intent.ACTION_BOOT_COMPLETED.equals(accion)) {
            PendingResult pending = goAsync();
            app.ejecutor().execute(() -> {
                try {
                    app.evaluarRecuperacionSesion().ejecutar();
                    reprogramarYNotificarPendientes(context, app);
                } finally {
                    if (pending != null) {
                        pending.finish();
                    }
                }
            });
            return;
        }

        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(accion)
                || esCambioPermisoAlarmasExactas(accion)) {
            PendingResult pending = goAsync();
            app.ejecutor().execute(() -> {
                try {
                    reprogramarYNotificarPendientes(context, app);
                } finally {
                    if (pending != null) {
                        pending.finish();
                    }
                }
            });
            return;
        }

        if (ProgramadorAvisosAlarmManager.ACCION_RECORDATORIO_SESION.equals(accion)) {
            long sesionId = intent.getLongExtra(ProgramadorAvisosAlarmManager.EXTRA_SESION_ID, -1L);
            if (sesionId <= 0) {
                return;
            }
            PendingResult pending = goAsync();
            app.ejecutor().execute(() -> {
                try {
                    procesarAlarmaRecordatorio(context, app, sesionId);
                } finally {
                    if (pending != null) {
                        pending.finish();
                    }
                }
            });
            return;
        }

        if (ProgramadorAvisosAlarmManager.ACCION_HITO_SESION.equals(accion)) {
            long sesionId = intent.getLongExtra(ProgramadorAvisosAlarmManager.EXTRA_SESION_ID, -1L);
            String tipo = intent.getStringExtra(ProgramadorAvisosAlarmManager.EXTRA_TIPO_HITO);
            String materia = intent.getStringExtra(ProgramadorAvisosAlarmManager.EXTRA_NOMBRE_MATERIA);
            if (sesionId > 0) {
                NotificadorSesion.mostrarHito(context, sesionId, tipo, materia == null ? "" : materia);
            }
            return;
        }

        if (ACCION_SIGO.equals(accion) || ACCION_TERMINE.equals(accion)) {
            long sesionId = intent.getLongExtra(EXTRA_SESION_ID, -1L);
            if (sesionId <= 0) {
                return;
            }
            NotificadorSesion.cancelarAvisoRetorno(context);
            DecidirFinPlanificadoOInvasion.DecisionFinOInvasion decision = ACCION_TERMINE.equals(accion)
                    ? DecidirFinPlanificadoOInvasion.DecisionFinOInvasion.TERMINE
                    : DecidirFinPlanificadoOInvasion.DecisionFinOInvasion.SIGO_ESTUDIANDO;
            PendingResult pending = goAsync();
            app.ejecutor().execute(() -> {
                try {
                    app.decidirFinPlanificadoOInvasion().ejecutar(sesionId, decision, null);
                } finally {
                    if (pending != null) {
                        pending.finish();
                    }
                }
            });
        }
    }

    private static boolean esCambioPermisoAlarmasExactas(String accion) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(accion);
    }

    /** Reprograma futuros avisos en Room y notifica los recordatorios vencidos aún antes del inicio. */
    static void reprogramarYNotificarPendientes(Context context, StudyPlanApplication app) {
        List<AvisoRecordatorioPendiente> inmediatos =
                app.reprogramarRecordatoriosPendientes().ejecutar();
        if (inmediatos.isEmpty()) {
            return;
        }
        boolean bloqueoActivo = app.consultarAjustes().ejecutar().bloqueoActivo();
        for (AvisoRecordatorioPendiente aviso : inmediatos) {
            NotificadorSesion.mostrarRecordatorio(
                    context,
                    aviso.sesionId(),
                    aviso.nombreMateria(),
                    aviso.horarioLocal(),
                    bloqueoActivo);
        }
    }

    /** Emite la notificación solo si la sesión sigue {@code PENDIENTE}, con aviso activo y antes del inicio. */
    static void procesarAlarmaRecordatorio(
            Context context, StudyPlanApplication app, long sesionId) {
        SesionEstudio sesion = app.sesionRepository().buscarPorId(sesionId).orElse(null);
        if (sesion == null
                || sesion.codigoEstado() != CodigoEstadoSesion.PENDIENTE
                || !sesion.antelacionRecordatorio().activo()) {
            return;
        }
        long ahoraMs = app.reloj().ahoraMilis();
        if (ahoraMs >= sesion.planInicioUtcMilis()) {
            return;
        }
        String nombreMateria = app.materiaRepository()
                .obtener(sesion.materiaId())
                .map(Materia::nombre)
                .orElse("");
        boolean bloqueoActivo = app.consultarAjustes().ejecutar().bloqueoActivo();
        int desfaseActual = app.reloj().ahoraInstante().desfaseMinutos();
        String horario = context.getString(
                R.string.sesion_rango_horario,
                sesion.horaInicio().texto(),
                sesion.horaFin().texto())
                + FormatoSesion.sufijoZonaSiDifiere(sesion.desfaseMinutos(), desfaseActual);
        NotificadorSesion.mostrarRecordatorio(
                context, sesionId, nombreMateria, horario, bloqueoActivo);
    }
}
