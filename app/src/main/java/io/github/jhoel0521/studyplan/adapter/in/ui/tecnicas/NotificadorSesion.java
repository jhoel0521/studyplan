package io.github.jhoel0521.studyplan.adapter.in.ui.tecnicas;

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
import android.graphics.Bitmap;
import android.net.Uri;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;

import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.adapter.out.system.ProgramadorAvisosAlarmManager;
import io.github.jhoel0521.studyplan.domain.service.CalculadorPomodoro;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.readmodel.EstadoPomodoroCalculado;
import io.github.jhoel0521.studyplan.domain.enumeration.FasePomodoro;
import io.github.jhoel0521.studyplan.domain.readmodel.MetaAlcanzada;
import io.github.jhoel0521.studyplan.domain.readmodel.ResumenEjecucionSesion;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.adapter.in.ui.ReceptorAvisosSesion;
import io.github.jhoel0521.studyplan.adapter.in.ui.ServicioSesionEnCurso;
import io.github.jhoel0521.studyplan.adapter.in.ui.StudyPlanApplication;
import io.github.jhoel0521.studyplan.adapter.in.ui.metas.FelicitacionMetasActivity;
import io.github.jhoel0521.studyplan.adapter.in.ui.sesiones.SesionDetalleActivity;

/**
 * Crea los canales de notificación, emite avisos de retorno, fin planificado, cambio de fase
 * Pomodoro, invasión y recordatorios previos de sesiones, y mantiene la notificación de la sesión En
 * curso con sus controles, respetando {@code POST_NOTIFICATIONS}, bloqueo de pantalla y alarmas
 * exactas (HU-23, HU-34, HU-40, HU-42, HU-49, RNF-18).
 */
public final class NotificadorSesion {

    /**
     * Canal de la notificación de la sesión. Es la versión 2: el canal original era de importancia baja
     * («silenciosa») y Android oculta esas notificaciones en la pantalla de bloqueo; la importancia de un canal ya
     * creado no se puede subir por código, así que se estrena un id nuevo (HU-49).
     */
    public static final String CANAL_SESION_ACTIVA = "canal_sesion_activa_v2";
    private static final String CANAL_SESION_ACTIVA_ANTERIOR = "canal_sesion_activa";
    public static final String CANAL_AVISOS_ESTUDIO = "canal_avisos_estudio";
    public static final int NOTIF_RETORNO_ID = 4001;
    public static final int NOTIF_HITO_ID = 4002;
    /** Notificación de la sesión En curso, sostenida por {@link ServicioSesionEnCurso} (HU-49). */
    public static final int NOTIF_SESION_ID = 4003;
    /** Aviso de metas alcanzadas al detener la sesión desde la notificación (HU-28, HU-49). */
    public static final int NOTIF_METAS_ID = 4004;
    private static final int REQ_SESION_CONTENIDO = 4100;
    private static final int REQ_SESION_PAUSAR = 4101;
    private static final int REQ_SESION_REANUDAR = 4102;
    private static final int REQ_SESION_DETENER = 4103;
    private static final int REQ_SESION_REPUBLICAR = 4104;
    private static final int REQ_METAS = 4105;
    private static final int REQ_SESION_NO_MOLESTAR = 4106;
    private static final int LADO_LOGO_PX = 192;
    private static Bitmap logoEnCache;
    public static final int NOTIF_RECORDATORIO_BASE =
            ProgramadorAvisosAlarmManager.NOTIF_RECORDATORIO_BASE;

    private static volatile Predicate<Context> detectorMusica = NotificadorSesion::hayMusicaSonando;

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
        // Importancia normal para verse en la pantalla de bloqueo, pero sin sonido, vibración ni luz: es un
        // control permanente, no un aviso.
        NotificationChannel activa = new NotificationChannel(
                CANAL_SESION_ACTIVA,
                contexto.getString(R.string.canal_sesion_activa_nombre),
                NotificationManager.IMPORTANCE_DEFAULT);
        activa.setSound(null, null);
        activa.enableVibration(false);
        activa.enableLights(false);
        activa.setShowBadge(false);
        activa.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        // El canal de una versión anterior se borra solo si existe: no en cada refresco de la notificación.
        if (nm.getNotificationChannel(CANAL_SESION_ACTIVA_ANTERIOR) != null) {
            nm.deleteNotificationChannel(CANAL_SESION_ACTIVA_ANTERIOR);
        }
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
     * y protegiendo la pantalla bloqueada si {@code bloqueoActivo}.
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

    public static Notification construirNotificacionRecordatorio(
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

    public static Notification construirNotificacionRecordatorio(
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
                .setSmallIcon(R.drawable.ic_notificacion)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setAutoCancel(true)
                .setContentIntent(piDetalle)
                .addAction(
                        R.drawable.ic_notificacion,
                        contexto.getString(R.string.notif_recordatorio_accion_iniciar),
                        piIniciar);

        if (bloqueoActivo) {
            Notification versionPublica = new NotificationCompat.Builder(
                    contexto, CANAL_AVISOS_ESTUDIO)
                    .setSmallIcon(R.drawable.ic_notificacion)
                    .setContentTitle(titulo)
                    .setContentText(contexto.getString(R.string.notif_recordatorio_privado))
                    .build();
            builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setPublicVersion(versionPublica);
        } else {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }
        aplicarSonidoDeAviso(contexto, builder, false);
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
        for (StatusBarNotification activa : nm.getActiveNotifications()) {
            if (activa.getId() >= NOTIF_RECORDATORIO_BASE) {
                nm.cancel(activa.getTag(), activa.getId());
            }
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

        NotificationCompat.Builder b = new NotificationCompat.Builder(contexto, CANAL_AVISOS_ESTUDIO)
                .setSmallIcon(R.drawable.ic_notificacion)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(contexto.getString(R.string.notif_volver_titulo))
                .setContentText(contexto.getString(R.string.notif_volver_cuerpo, nombreMateria))
                .setAutoCancel(true)
                .setContentIntent(pi);
        NotificationManager nm = (NotificationManager) contexto.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            aplicarSonidoDeAviso(contexto, b, false);
            nm.notify(NOTIF_RETORNO_ID, b.build());
        }
    }

    /**
     * Los avisos no cortan la música (HU-49): con música sonando salen en silencio; sin ella suenan y vibran una sola
     * vez por el canal (Android 8 o posterior) o, en Android 7, por los valores predeterminados si el aviso sonaba antes.
     */
    private static void aplicarSonidoDeAviso(
            Context contexto, NotificationCompat.Builder b, boolean sonidoEnAndroid7) {
        if (detectorMusica.test(contexto)) {
            b.setSilent(true);
        } else if (sonidoEnAndroid7 && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            b.setDefaults(NotificationCompat.DEFAULT_ALL);
        }
    }

    private static boolean hayMusicaSonando(Context contexto) {
        AudioManager audio = (AudioManager) contexto.getSystemService(Context.AUDIO_SERVICE);
        return audio != null && audio.isMusicActive();
    }

    /** Sustituye la detección de música en las pruebas; {@code null} restablece la real. */
    public static void fijarDetectorMusicaParaPruebas(Predicate<Context> detector) {
        detectorMusica = detector == null ? NotificadorSesion::hayMusicaSonando : detector;
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
                .setSmallIcon(R.drawable.ic_notificacion)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setAutoCancel(true)
                .setContentIntent(pi);

        if (conAccionesFin) {
            b.addAction(
                    R.drawable.ic_notificacion,
                    contexto.getString(R.string.notif_accion_sigo),
                    piAccion(contexto, sesionId, ReceptorAvisosSesion.ACCION_SIGO, 1));
            b.addAction(
                    R.drawable.ic_notificacion,
                    contexto.getString(R.string.notif_accion_termine),
                    piAccion(contexto, sesionId, ReceptorAvisosSesion.ACCION_TERMINE, 2));
        }

        NotificationManager nm = (NotificationManager) contexto.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            aplicarSonidoDeAviso(contexto, b, true);
            nm.notify(NOTIF_HITO_ID, b.build());
        }
    }

    // ------------------------------------------------------------------------------------------
    // Notificación de la sesión En curso (HU-49)
    // ------------------------------------------------------------------------------------------

    /**
     * Publica, actualiza o retira la notificación de la sesión En curso leyendo su estado guardado
     * (HU-49, HU-40). Debe llamarse fuera del hilo principal.
     */
    public static void refrescar(Context contexto, long sesionId) {
        StudyPlanApplication app = StudyPlanApplication.de(contexto);
        Optional<ResumenEjecucionSesion> resumen = app.consultarEjecucionSesion().ejecutar(sesionId);
        if (!resumen.isPresent()
                || resumen.get().sesion().codigoEstado() != CodigoEstadoSesion.EN_CURSO) {
            Optional<SesionEstudio> otra = app.sesionRepository().buscarEnCurso();
            if (otra.isPresent() && otra.get().id() != null && otra.get().id() != sesionId) {
                refrescar(contexto, otra.get().id());
            } else {
                retirarNotificacionSesion(contexto);
            }
            return;
        }
        if (!puedeNotificar(contexto)) {
            return;
        }
        asegurarCanales(contexto);
        boolean bloqueoApp = app.consultarAjustes().ejecutar().bloqueoActivo();
        Notification notificacion = construirNotificacionSesion(
                contexto, sesionId, resumen.get(), bloqueoApp, System.currentTimeMillis());
        ServicioSesionEnCurso.mostrar(contexto, notificacion);
    }

    /** Refresca la notificación de la sesión En curso, si la hay, o la retira. Fuera del hilo principal. */
    public static void reconciliar(Context contexto) {
        StudyPlanApplication app = StudyPlanApplication.de(contexto);
        Optional<SesionEstudio> enCurso = app.sesionRepository().buscarEnCurso();
        if (enCurso.isPresent() && enCurso.get().id() != null) {
            refrescar(contexto, enCurso.get().id());
        } else {
            retirarNotificacionSesion(contexto);
        }
    }

    /** Detiene el servicio y quita la notificación de la sesión. */
    public static void retirarNotificacionSesion(Context contexto) {
        ServicioSesionEnCurso.detener(contexto);
        NotificationManager nm = (NotificationManager) contexto.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(NOTIF_SESION_ID);
        }
    }

    /** Construye la notificación de la sesión; con el bloqueo de la app activo añade su versión pública. */
    public static Notification construirNotificacionSesion(
            Context contexto,
            long sesionId,
            ResumenEjecucionSesion resumen,
            boolean bloqueoApp,
            long ahoraMs) {
        NotificationCompat.Builder completa =
                constructorSesion(contexto, sesionId, resumen, false, ahoraMs);
        if (bloqueoApp) {
            completa.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setPublicVersion(constructorSesion(contexto, sesionId, resumen, true, ahoraMs).build());
        } else {
            completa.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }
        return completa.build();
    }

    private static NotificationCompat.Builder constructorSesion(
            Context contexto,
            long sesionId,
            ResumenEjecucionSesion resumen,
            boolean privada,
            long ahoraMs) {
        boolean pausada = resumen.pausada();
        boolean salida = resumen.salidaPendienteResolver();
        boolean corriendo = !pausada && !salida;
        EstadoPomodoroCalculado pomodoro = resumen.sesion().tecnica() == TecnicaEstudio.POMODORO
                ? resumen.estadoPomodoro()
                : null;

        String titulo;
        if (salida) {
            titulo = contexto.getString(R.string.notif_volver_titulo);
        } else if (pausada) {
            titulo = contexto.getString(R.string.notif_sesion_pausa_titulo);
        } else if (pomodoro != null) {
            int objetivo = CalculadorPomodoro.pomodorosQueCabenEnPlan(
                    resumen.sesion().duracionPlanificadaMinutos(),
                    resumen.sesion().configuracionPomodoroSnapshot());
            titulo = TextosSesion.fase(contexto, pomodoro.faseActual(), pomodoro.cicloActual(), objetivo);
        } else {
            titulo = contexto.getString(R.string.notif_sesion_titulo);
        }

        String texto;
        if (privada && corriendo) {
            texto = contexto.getString(R.string.notif_sesion_privada_texto);
        } else if (privada) {
            // Detenida no hay cronómetro que avance: el tiempo va en el texto, sin el nombre de la materia (RNF-12).
            texto = contexto.getString(
                    R.string.notif_sesion_privada_texto_con_tiempo, TextosSesion.hms(resumen.milisEfectivos()));
        } else if (corriendo) {
            texto = resumen.nombreMateria();
        } else {
            texto = contexto.getString(
                    R.string.notif_sesion_texto_con_tiempo,
                    resumen.nombreMateria(),
                    TextosSesion.hms(resumen.milisEfectivos()));
        }

        Intent abrir = new Intent(contexto, SesionEnCursoActivity.class);
        abrir.putExtra(SesionEnCursoActivity.EXTRA_SESION_ID, sesionId);
        abrir.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contenido = PendingIntent.getActivity(
                contexto, REQ_SESION_CONTENIDO, abrir, flagsCompat(PendingIntent.FLAG_UPDATE_CURRENT));
        PendingIntent republicar = PendingIntent.getBroadcast(
                contexto,
                REQ_SESION_REPUBLICAR,
                intentReceptor(contexto, ReceptorAvisosSesion.ACCION_REPUBLICAR, sesionId),
                flagsCompat(PendingIntent.FLAG_UPDATE_CURRENT));

        NotificationCompat.Builder b = new NotificationCompat.Builder(contexto, CANAL_SESION_ACTIVA)
                .setSmallIcon(R.drawable.ic_notificacion)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                // Sin MediaStyle, Android 12 o posterior retrasa hasta 10 s la notificación de un servicio.
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setContentIntent(contenido)
                .setDeleteIntent(republicar)
                .setLargeIcon(logoDeLaApp(contexto));

        if (corriendo) {
            b.setShowWhen(true).setUsesChronometer(true);
            if (pomodoro != null && pomodoro.faseActual() != FasePomodoro.TIEMPO_EXTRA_LIBRE) {
                b.setWhen(ahoraMs + pomodoro.milisRestantesEnFase()).setChronometerCountDown(true);
            } else {
                b.setWhen(ahoraMs - resumen.milisEfectivos());
            }
        } else {
            b.setShowWhen(false).setUsesChronometer(false);
        }

        if (pausada) {
            b.addAction(accionSesion(contexto, R.drawable.ic_reproducir, R.string.sesion_en_curso_reanudar,
                    ReceptorAvisosSesion.ACCION_REANUDAR, REQ_SESION_REANUDAR, sesionId, privada));
        } else {
            b.addAction(accionSesion(contexto, R.drawable.ic_pausar, R.string.sesion_en_curso_pausar,
                    ReceptorAvisosSesion.ACCION_PAUSAR, REQ_SESION_PAUSAR, sesionId, privada));
        }
        b.addAction(accionSesion(contexto, R.drawable.ic_detener, R.string.sesion_en_curso_detener,
                ReceptorAvisosSesion.ACCION_DETENER, REQ_SESION_DETENER, sesionId, privada));
        b.addAction(accionNoMolestar(contexto, sesionId, resumen.noMolestarActivo(), privada));
        return b;
    }

    /** Logo de la app como icono grande de la notificación (el que ve el estudiante en la pantalla de bloqueo). */
    private static Bitmap logoDeLaApp(Context contexto) {
        // R.mipmap.ic_launcher es un icono adaptativo (XML): BitmapFactory no lo lee, se dibuja desde su Drawable.
        if (logoEnCache == null) {
            android.graphics.drawable.Drawable icono = ContextCompat.getDrawable(contexto, R.mipmap.ic_launcher);
            Bitmap bitmap = Bitmap.createBitmap(LADO_LOGO_PX, LADO_LOGO_PX, Bitmap.Config.ARGB_8888);
            if (icono != null) {
                icono.setBounds(0, 0, LADO_LOGO_PX, LADO_LOGO_PX);
                icono.draw(new android.graphics.Canvas(bitmap));
            }
            logoEnCache = bitmap;
        }
        return logoEnCache;
    }

    /**
     * Tercer botón: alterna No molestar solo para la sesión y muestra su estado (HU-24, HU-49). Si la app no tiene
     * acceso a No molestar, el botón lleva a la pantalla del sistema donde se concede.
     */
    private static NotificationCompat.Action accionNoMolestar(
            Context contexto, long sesionId, boolean activo, boolean exigirDesbloqueo) {
        int icono = activo ? R.drawable.ic_no_molestar : R.drawable.ic_no_molestar_apagado;
        int texto = activo ? R.string.sesion_no_molestar_activado : R.string.sesion_no_molestar_apagado;
        if (!StudyPlanApplication.de(contexto).controladorNoMolestar().tienePermiso()) {
            Intent ajustes = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            PendingIntent abrir = PendingIntent.getActivity(
                    contexto, REQ_SESION_NO_MOLESTAR, ajustes, flagsCompat(PendingIntent.FLAG_UPDATE_CURRENT));
            return new NotificationCompat.Action.Builder(icono, contexto.getString(texto), abrir).build();
        }
        return accionSesion(contexto, icono, texto, ReceptorAvisosSesion.ACCION_NO_MOLESTAR,
                REQ_SESION_NO_MOLESTAR, sesionId, exigirDesbloqueo);
    }

    private static Intent intentReceptor(Context contexto, String accion, long sesionId) {
        Intent intento = new Intent(contexto, ReceptorAvisosSesion.class);
        intento.setAction(accion);
        intento.putExtra(ReceptorAvisosSesion.EXTRA_SESION_ID, sesionId);
        return intento;
    }

    /** Botón de la notificación; con el bloqueo de la app activo exige desbloquear el teléfono (Android 12 o posterior). */
    private static NotificationCompat.Action accionSesion(
            Context contexto,
            int icono,
            @StringRes int texto,
            String accion,
            int codigoPeticion,
            long sesionId,
            boolean exigirDesbloqueo) {
        PendingIntent intento = PendingIntent.getBroadcast(
                contexto,
                codigoPeticion,
                intentReceptor(contexto, accion, sesionId),
                flagsCompat(PendingIntent.FLAG_UPDATE_CURRENT));
        NotificationCompat.Action.Builder b = new NotificationCompat.Action.Builder(
                icono, contexto.getString(texto), intento);
        if (exigirDesbloqueo && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            b.setAuthenticationRequired(true);
        }
        return b.build();
    }

    /** Aviso que abre la felicitación cuando «Detener» desde la notificación alcanza una meta (HU-28, HU-49). */
    public static void mostrarMetasAlcanzadas(Context contexto, List<MetaAlcanzada> metas) {
        if (metas == null || metas.isEmpty() || !puedeNotificar(contexto)) {
            return;
        }
        asegurarCanales(contexto);
        Intent abrir = FelicitacionMetasActivity.crearIntent(contexto, metas);
        abrir.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                contexto, REQ_METAS, abrir, flagsCompat(PendingIntent.FLAG_UPDATE_CURRENT));
        NotificationCompat.Builder b = new NotificationCompat.Builder(contexto, CANAL_AVISOS_ESTUDIO)
                .setSmallIcon(R.drawable.ic_notificacion)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(contexto.getString(R.string.felicitacion_titulo))
                .setContentText(contexto.getString(R.string.felicitacion_subtitulo))
                .setAutoCancel(true)
                .setContentIntent(pi);
        NotificationManager nm = (NotificationManager) contexto.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            aplicarSonidoDeAviso(contexto, b, false);
            nm.notify(NOTIF_METAS_ID, b.build());
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

    private static int flagsCompat(int base) {
        return base | PendingIntent.FLAG_IMMUTABLE;
    }
}
