package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.List;
import java.util.Optional;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.adapter.out.system.ProgramadorAvisosAlarmManager;
import io.github.jhoel0521.studyplan.application.port.in.tecnicas.DecidirFinPlanificadoOInvasion;
import io.github.jhoel0521.studyplan.domain.readmodel.AvisoRecordatorioPendiente;
import io.github.jhoel0521.studyplan.domain.readmodel.CierreSesion;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.readmodel.ResumenEjecucionSesion;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TopicoEventoEstudio;

/**
 * Receptor de alarmas de hitos, recordatorios previos, acciones de notificación ([Sigo] / [Terminé]),
 * botones de la notificación de la sesión (Pausar, Reanudar y Detener), reinicio del dispositivo
 * ({@code BOOT_COMPLETED}), actualización de paquete y cambio de permiso de alarmas exactas (HU-23, HU-34,
 * HU-42, HU-49, RN-25).
 */
public class ReceptorAvisosSesion extends BroadcastReceiver {

    public static final String ACCION_SIGO = "io.github.jhoel0521.studyplan.ACCION_SIGO";
    public static final String ACCION_TERMINE = "io.github.jhoel0521.studyplan.ACCION_TERMINE";
    public static final String ACCION_PAUSAR = "io.github.jhoel0521.studyplan.ACCION_PAUSAR";
    public static final String ACCION_REANUDAR = "io.github.jhoel0521.studyplan.ACCION_REANUDAR";
    public static final String ACCION_DETENER = "io.github.jhoel0521.studyplan.ACCION_DETENER";
    /** Botón «No molestar» de la notificación de la sesión (HU-24, HU-49). */
    public static final String ACCION_NO_MOLESTAR = "io.github.jhoel0521.studyplan.ACCION_NO_MOLESTAR";
    /** El sistema permitió descartar la notificación de la sesión: se vuelve a mostrar (HU-49). */
    public static final String ACCION_REPUBLICAR = "io.github.jhoel0521.studyplan.ACCION_REPUBLICAR";
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
                    NotificadorSesion.reconciliar(context);
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
                    NotificadorSesion.reconciliar(context);
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

        if (ACCION_PAUSAR.equals(accion)
                || ACCION_REANUDAR.equals(accion)
                || ACCION_DETENER.equals(accion)
                || ACCION_NO_MOLESTAR.equals(accion)
                || ACCION_REPUBLICAR.equals(accion)) {
            long sesionId = intent.getLongExtra(EXTRA_SESION_ID, -1L);
            if (sesionId <= 0) {
                return;
            }
            PendingResult pending = goAsync();
            app.ejecutor().execute(() -> {
                try {
                    procesarAccionSesion(context, app, accion, sesionId);
                } finally {
                    if (pending != null) {
                        pending.finish();
                    }
                }
            });
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

    /**
     * Atiende los botones de la notificación de la sesión. Una acción cuya sesión ya no es la que está En
     * curso (notificación obsoleta) no actúa sobre otra sesión: solo se reconcilia la notificación (HU-49).
     */
    static void procesarAccionSesion(
            Context context, StudyPlanApplication app, String accion, long sesionId) {
        Optional<SesionEstudio> enCurso = app.sesionRepository().buscarEnCurso();
        if (!enCurso.isPresent() || enCurso.get().id() == null || enCurso.get().id() != sesionId) {
            NotificadorSesion.reconciliar(context);
            return;
        }
        if (ACCION_REPUBLICAR.equals(accion)) {
            NotificadorSesion.refrescar(context, sesionId);
            return;
        }
        if (ACCION_NO_MOLESTAR.equals(accion)) {
            app.alternarNoMolestarSesion().ejecutar(sesionId);
            NotificadorSesion.refrescar(context, sesionId);
            return;
        }
        boolean pausada = app.consultarEjecucionSesion().ejecutar(sesionId)
                .map(ResumenEjecucionSesion::pausada)
                .orElse(false);
        if (ACCION_PAUSAR.equals(accion)) {
            if (!pausada) {
                app.pausarReanudarSesion().ejecutar(sesionId);
            }
        } else if (ACCION_REANUDAR.equals(accion)) {
            if (pausada) {
                app.pausarReanudarSesion().ejecutar(sesionId, TopicoEventoEstudio.SIN_CLASIFICAR, "");
            }
        } else if (ACCION_DETENER.equals(accion)) {
            Resultado<CierreSesion> cierre = app.detenerSesion().ejecutarConMetas(sesionId);
            if (cierre.esValido()) {
                NotificadorSesion.mostrarMetasAlcanzadas(context, cierre.valor().metasAlcanzadas());
            }
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
