package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.domain.readmodel.DetalleSesionInicio;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.valueobject.Instante;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;

/**
 * Formatea rangos horarios, fechas y duraciones de sesión para la interfaz sin duplicar plantillas.
 *
 * <p>Implementa: HU-02 y HU-10 a HU-15.
 */
final class FormatoSesion {

    private FormatoSesion() {
    }

    static String rangoYDuracion(Context contexto, DetalleSesionInicio sesion, boolean incluirFecha) {
        String horario = incluirFecha
                ? contexto.getString(
                        R.string.sesion_fecha_y_rango,
                        sesion.fecha().iso(),
                        sesion.horaInicio().texto(),
                        sesion.horaFin().texto())
                : contexto.getString(
                        R.string.sesion_rango_horario,
                        sesion.horaInicio().texto(),
                        sesion.horaFin().texto());
        Integer real = sesion.duracionRealMinutos();
        String duracion = real != null && real > 0
                ? contexto.getString(R.string.sesion_duracion_real_minutos, real)
                : contexto.getString(
                        R.string.sesion_duracion_minutos, sesion.duracionPlanificadaMinutos());
        return horario + " · " + duracion;
    }

    /** Devuelve el desfase horario actual del teléfono en minutos respecto a UTC. */
    static int desfaseActualSistema() {
        return TimeZone.getDefault().getOffset(System.currentTimeMillis())
                / (int) Instante.MILIS_POR_MINUTO;
    }

    /** Devuelve la indicación {@code (UTC±HH:mm)} cuando el desfase guardado difiere del actual. */
    static String sufijoZonaSiDifiere(int desfaseGuardado, int desfaseActual) {
        if (desfaseGuardado == desfaseActual) {
            return "";
        }
        return " (" + Instante.de(0L, desfaseGuardado).desfaseTexto() + ")";
    }

    /** Formatea fecha y rango de una sesión conservando su desfase guardado ({@code HU-10}). */
    static String horarioSesionConZona(SesionEstudio sesion, int desfaseActual) {
        return sesion.fecha().iso()
                + " · "
                + sesion.horaInicio().texto()
                + "–"
                + sesion.horaFin().texto()
                + sufijoZonaSiDifiere(sesion.desfaseMinutos(), desfaseActual);
    }

    /** Formatea la hora de un evento conservando su desfase guardado ({@code HU-39}). */
    static String horaEventoConZona(EventoEstudio evento, int desfaseActual) {
        return evento.hora().texto()
                + sufijoZonaSiDifiere(evento.desfaseMinutos(), desfaseActual);
    }

    /** Devuelve la etiqueta localizada de una antelación de recordatorio ({@code HU-34}). */
    static String etiquetaAntelacion(Context contexto, AntelacionRecordatorio antelacion) {
        AntelacionRecordatorio segura =
                antelacion == null ? AntelacionRecordatorio.SIN_RECORDATORIO : antelacion;
        switch (segura) {
            case MINUTOS_5:
                return contexto.getString(R.string.recordatorio_5_min);
            case MINUTOS_15:
                return contexto.getString(R.string.recordatorio_15_min);
            case MINUTOS_30:
                return contexto.getString(R.string.recordatorio_30_min);
            case SIN_RECORDATORIO:
            default:
                return contexto.getString(R.string.recordatorio_ninguno);
        }
    }

    /** Devuelve las etiquetas en el mismo orden de {@link AntelacionRecordatorio#values()}. */
    static List<String> etiquetasAntelacion(Context contexto) {
        AntelacionRecordatorio[] valores = AntelacionRecordatorio.values();
        List<String> etiquetas = new ArrayList<>(valores.length);
        for (AntelacionRecordatorio valor : valores) {
            etiquetas.add(etiquetaAntelacion(contexto, valor));
        }
        return etiquetas;
    }
}
