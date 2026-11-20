package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.ConfiguracionPomodoro;
import io.github.jhoel0521.studyplan.domain.EstadoPomodoroCalculado;
import io.github.jhoel0521.studyplan.domain.FasePomodoro;
import io.github.jhoel0521.studyplan.domain.ResumenEjecucionSesion;
import io.github.jhoel0521.studyplan.domain.TecnicaEstudio;

/**
 * Sesión multimedia que existe solo mientras hay una sesión de estudio En curso (HU-49). Es lo que hace que Android
 * muestre en la pantalla de bloqueo la tarjeta de control como la de un reproductor de música: logo como carátula,
 * título, subtítulo, botones y barra de progreso. No reproduce audio. Con Pomodoro la barra avanza durante la fase; con
 * la sesión libre solo se muestra el tiempo. Los botones de los audífonos se ignoran para que no controlen por error la
 * sesión de estudio en lugar de la música.
 */
final class SesionMultimedia {

    private static MediaSessionCompat sesion;
    private static long sesionId;

    private SesionMultimedia() {
    }

    /** Crea o actualiza la sesión multimedia con el estado actual y devuelve su token para la notificación. */
    static MediaSessionCompat.Token actualizar(
            Context contexto,
            long id,
            ResumenEjecucionSesion resumen,
            Bitmap logo,
            boolean ocultarMateria) {
        Context app = contexto.getApplicationContext();
        sesionId = id;
        if (sesion == null) {
            sesion = new MediaSessionCompat(app, "StudyPlanSesion");
            sesion.setCallback(new MediaSessionCompat.Callback() {
                @Override
                public void onPlay() {
                    enviar(app, ReceptorAvisosSesion.ACCION_REANUDAR);
                }

                @Override
                public void onPause() {
                    enviar(app, ReceptorAvisosSesion.ACCION_PAUSAR);
                }

                @Override
                public void onStop() {
                    enviar(app, ReceptorAvisosSesion.ACCION_DETENER);
                }

                @Override
                public boolean onMediaButtonEvent(Intent intento) {
                    return true; // los audífonos no controlan la sesión de estudio
                }
            }, new Handler(Looper.getMainLooper()));
        }

        boolean detenida = resumen.pausada() || resumen.salidaPendienteResolver();
        EstadoPomodoroCalculado pomodoro = resumen.sesion().tecnica() == TecnicaEstudio.POMODORO
                ? resumen.estadoPomodoro()
                : null;
        long duracion = -1L; // desconocida: sin barra de progreso
        long posicion = resumen.milisEfectivos();
        if (pomodoro != null && pomodoro.faseActual() != FasePomodoro.TIEMPO_EXTRA_LIBRE) {
            long faseMs = duracionDeFaseMs(resumen, pomodoro.faseActual());
            if (faseMs > 0) {
                duracion = faseMs;
                posicion = Math.max(0L, faseMs - pomodoro.milisRestantesEnFase());
            }
        }

        String titulo = ocultarMateria
                ? contexto.getString(R.string.notif_sesion_titulo)
                : resumen.nombreMateria();
        String subtitulo;
        if (resumen.salidaPendienteResolver()) {
            subtitulo = contexto.getString(R.string.notif_volver_titulo);
        } else if (resumen.pausada()) {
            subtitulo = contexto.getString(R.string.notif_sesion_pausa_titulo);
        } else if (pomodoro != null) {
            int objetivo = io.github.jhoel0521.studyplan.domain.CalculadorPomodoro.pomodorosQueCabenEnPlan(
                    resumen.sesion().duracionPlanificadaMinutos(),
                    resumen.sesion().configuracionPomodoroSnapshot());
            subtitulo = TextosSesion.fase(contexto, pomodoro.faseActual(), pomodoro.cicloActual(), objetivo);
        } else {
            subtitulo = contexto.getString(R.string.sesion_en_curso_activa);
        }

        sesion.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, titulo)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, subtitulo)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, logo)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duracion)
                .build());
        sesion.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_STOP)
                .setState(detenida ? PlaybackStateCompat.STATE_PAUSED : PlaybackStateCompat.STATE_PLAYING,
                        posicion, detenida ? 0f : 1f, SystemClock.elapsedRealtime())
                .build());
        sesion.setActive(true);
        return sesion.getSessionToken();
    }

    /** Libera la sesión multimedia cuando la sesión de estudio deja de estar En curso. */
    static void liberar() {
        if (sesion != null) {
            sesion.setActive(false);
            sesion.release();
            sesion = null;
        }
    }

    static boolean estaActiva() {
        return sesion != null && sesion.isActive();
    }

    private static long duracionDeFaseMs(ResumenEjecucionSesion resumen, FasePomodoro fase) {
        ConfiguracionPomodoro cfg = resumen.sesion().configuracionPomodoroSnapshot();
        if (cfg == null) {
            return 0L;
        }
        int minutos;
        switch (fase) {
            case DESCANSO_CORTO:
                minutos = cfg.descansoCortoMinutos();
                break;
            case DESCANSO_LARGO:
                minutos = cfg.descansoLargoMinutos();
                break;
            case TRABAJO:
            default:
                minutos = cfg.trabajoMinutos();
                break;
        }
        return minutos * 60_000L;
    }

    private static void enviar(Context app, String accion) {
        Intent intento = new Intent(app, ReceptorAvisosSesion.class);
        intento.setAction(accion);
        intento.putExtra(ReceptorAvisosSesion.EXTRA_SESION_ID, sesionId);
        app.sendBroadcast(intento);
    }
}
