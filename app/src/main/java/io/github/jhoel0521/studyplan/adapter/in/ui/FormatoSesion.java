package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.Context;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.DetalleSesionInicio;

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
}
