package io.github.jhoel0521.studyplan.adapter.in.ui.configuracion;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.adapter.in.ui.StudyPlanApplication;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.FormatoHoras;
import io.github.jhoel0521.studyplan.domain.readmodel.DetalleSesionInicio;
import io.github.jhoel0521.studyplan.domain.readmodel.ResumenInicio;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;

/**
 * Widget 1×4 de la pantalla de inicio del teléfono: logo, horas de la semana sobre la meta y las 2 próximas materias. Al tocarlo abre
 * Inicio ({@link MainActivity}), que ya resuelve el bloqueo con huella o PIN.
 *
 * <p>Adaptador de entrada: reutiliza {@code ObtenerResumenInicio} y no toca el dominio. Lee los datos fuera del hilo
 * principal, en el ejecutor de la aplicación.
 *
 * <p>Implementa: HU-05 (RNF-19, RNF-06).
 */
public class WidgetProgresoProvider extends AppWidgetProvider {

    private static final int ANILLO_DP = 48;

    @Override
    public void onUpdate(Context contexto, AppWidgetManager gestor, int[] ids) {
        publicarEnSegundoPlano(contexto, gestor, ids, goAsync());
    }

    /** Pide que se vuelvan a dibujar todos los widgets de StudyPlan que haya en pantalla (no hace nada si no hay). */
    public static void refrescarTodos(Context contexto) {
        Context app = contexto.getApplicationContext();
        AppWidgetManager gestor = AppWidgetManager.getInstance(app);
        int[] ids = gestor.getAppWidgetIds(new ComponentName(app, WidgetProgresoProvider.class));
        if (ids.length > 0) {
            publicarEnSegundoPlano(app, gestor, ids, null);
        }
    }

    private static void publicarEnSegundoPlano(
            Context contexto, AppWidgetManager gestor, int[] ids, BroadcastReceiver.PendingResult pendiente) {
        Context app = contexto.getApplicationContext();
        StudyPlanApplication aplicacion = (StudyPlanApplication) app;
        aplicacion.ejecutor().execute(() -> {
            try {
                Optional<ResumenInicio> resumen = aplicacion.obtenerResumenInicio().ejecutar();
                ModeloWidgetProgreso modelo = ModeloWidgetProgreso.de(resumen);
                RemoteViews vistas = construir(app, modelo);
                for (int id : ids) {
                    gestor.updateAppWidget(id, vistas);
                }
            } catch (RuntimeException falla) {
                // Si la lectura falla se conserva lo que el widget ya mostraba.
            } finally {
                if (pendiente != null) {
                    pendiente.finish();
                }
            }
        });
    }

    /** Arma las vistas del widget para un estado; separado para poder inflarlo en las pruebas de dispositivo. */
    static RemoteViews construir(Context contexto, ModeloWidgetProgreso modelo) {
        RemoteViews vistas = new RemoteViews(contexto.getPackageName(), R.layout.widget_progreso_1x4);

        float densidad = contexto.getResources().getDisplayMetrics().density;
        vistas.setImageViewBitmap(R.id.widget_anillo, DibujanteAnilloProgreso.anillo(
                Math.round(ANILLO_DP * densidad),
                modelo.hayProgreso() ? modelo.porcentaje() : 0,
                ContextCompat.getColor(contexto, R.color.sp_ring_track),
                ContextCompat.getColor(contexto, R.color.sp_action)));

        String textoHoras = modelo.hayProgreso()
                ? contexto.getString(R.string.inicio_progreso,
                        FormatoHoras.texto(modelo.horasRealizadas()), FormatoHoras.texto(modelo.metaHoras()))
                : modelo.estado() == ModeloWidgetProgreso.Estado.SIN_META
                        ? contexto.getString(R.string.widget_sin_meta)
                        : "";
        vistas.setTextViewText(R.id.widget_porcentaje,
                modelo.hayProgreso() ? contexto.getString(R.string.widget_porcentaje, modelo.porcentaje()) : "");
        vistas.setTextViewText(R.id.widget_horas, textoHoras);
        vistas.setViewVisibility(R.id.widget_horas, textoHoras.isEmpty() ? View.GONE : View.VISIBLE);

        List<DetalleSesionInicio> proximas = modelo.proximas();
        ponerProxima(contexto, vistas, modelo, proximas, 0,
                R.id.widget_fila_proxima1, R.id.widget_barra_materia1, R.id.widget_proxima1);
        ponerProxima(contexto, vistas, modelo, proximas, 1,
                R.id.widget_fila_proxima2, R.id.widget_barra_materia2, R.id.widget_proxima2);

        String mensaje = mensajeVacio(contexto, modelo);
        vistas.setViewVisibility(R.id.widget_mensaje, mensaje != null ? View.VISIBLE : View.GONE);
        if (mensaje != null) {
            vistas.setTextViewText(R.id.widget_mensaje, mensaje);
        }

        vistas.setContentDescription(R.id.widget_raiz, descripcion(contexto, modelo, textoHoras, mensaje));
        vistas.setOnClickPendingIntent(R.id.widget_raiz, abrirInicio(contexto));
        return vistas;
    }

    private static void ponerProxima(
            Context contexto, RemoteViews vistas, ModeloWidgetProgreso modelo,
            List<DetalleSesionInicio> proximas, int indice, int idFila, int idBarra, int idTexto) {
        boolean hay = indice < proximas.size();
        vistas.setViewVisibility(idFila, hay ? View.VISIBLE : View.GONE);
        if (hay) {
            DetalleSesionInicio sesion = proximas.get(indice);
            vistas.setInt(idBarra, "setBackgroundColor", sesion.colorMateria());
            vistas.setTextViewText(idTexto, textoProxima(contexto, modelo, sesion));
        }
    }

    /** «Materia · Hoy 10:00», «Materia · Mañana 10:00» o «Materia · 23/11 10:00». */
    static String textoProxima(Context contexto, ModeloWidgetProgreso modelo, DetalleSesionInicio sesion) {
        Fecha fecha = sesion.fecha();
        String dia;
        if (modelo.hoy() != null && modelo.hoy().equals(fecha)) {
            dia = contexto.getString(R.string.widget_hoy);
        } else if (modelo.hoy() != null && modelo.hoy().sumarDias(1).equals(fecha)) {
            dia = contexto.getString(R.string.widget_manana);
        } else {
            dia = String.format(Locale.getDefault(), "%02d/%02d", fecha.dia(), fecha.mes());
        }
        return contexto.getString(
                R.string.widget_proxima_linea, sesion.nombreMateria(), dia, sesion.horaInicio().texto());
    }

    /** @return el aviso que ocupa el lugar de las próximas materias, o {@code null} si no hace falta */
    private static String mensajeVacio(Context contexto, ModeloWidgetProgreso modelo) {
        switch (modelo.estado()) {
            case SIN_PERFIL:
                return contexto.getString(R.string.widget_sin_perfil);
            case SIN_SESION:
                return contexto.getString(R.string.widget_sin_sesiones);
            case NORMAL:
            default:
                return null;
        }
    }

    private static String descripcion(Context contexto, ModeloWidgetProgreso modelo, String horas, String mensaje) {
        StringBuilder detalle = new StringBuilder(horas);
        for (DetalleSesionInicio sesion : modelo.proximas()) {
            detalle.append(". ").append(textoProxima(contexto, modelo, sesion));
        }
        if (mensaje != null) {
            detalle.append(detalle.length() > 0 ? ". " : "").append(mensaje);
        }
        if (!modelo.hayProgreso()) {
            return contexto.getString(R.string.widget_accesibilidad_vacio, detalle.toString());
        }
        return contexto.getString(R.string.widget_accesibilidad, modelo.porcentaje(), detalle.toString());
    }

    private static PendingIntent abrirInicio(Context contexto) {
        Intent intento = new Intent(contexto, MainActivity.class);
        intento.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                contexto, 0, intento, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
