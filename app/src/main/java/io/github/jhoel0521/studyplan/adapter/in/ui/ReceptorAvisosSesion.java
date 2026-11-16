package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import io.github.jhoel0521.studyplan.adapter.out.system.ProgramadorAvisosAlarmManager;
import io.github.jhoel0521.studyplan.application.port.in.DecidirFinPlanificadoOInvasion;

/**
 * Receptor de alarmas de hitos, acciones de notificación ([Sigo] / [Terminé]) y reinicio del
 * dispositivo ({@code BOOT_COMPLETED}) para sesiones {@code EN_CURSO} (HU-23, HU-42, RN-25).
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
                } finally {
                    pending.finish();
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
                    pending.finish();
                }
            });
        }
    }
}
