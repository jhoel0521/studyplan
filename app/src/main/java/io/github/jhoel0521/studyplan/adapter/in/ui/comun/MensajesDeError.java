package io.github.jhoel0521.studyplan.adapter.in.ui.comun;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.EnumMap;
import java.util.Map;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.readmodel.ConflictoHorario;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;

/**
 * Traduce los códigos de error del dominio al texto de {@code strings.xml} (RNF-08: el dominio no
 * devuelve textos).
 *
 * <p>Implementa: HU-06 a HU-08, HU-10 a HU-15, HU-46, RN-02 a RN-12 y RNF-08.
 */
public final class MensajesDeError {

    private static final Map<ErrorValidacion, Integer> MENSAJES_SESION =
            new EnumMap<>(ErrorValidacion.class);

    static {
        MENSAJES_SESION.put(
                ErrorValidacion.MATERIA_SESION_OBLIGATORIA,
                R.string.error_sesion_materia_obligatoria);
        MENSAJES_SESION.put(
                ErrorValidacion.MATERIA_NO_ENCONTRADA,
                R.string.error_materia_no_encontrada);
        MENSAJES_SESION.put(
                ErrorValidacion.MATERIA_INACTIVA,
                R.string.error_sesion_materia_inactiva);
        MENSAJES_SESION.put(
                ErrorValidacion.FECHA_SESION_OBLIGATORIA,
                R.string.error_sesion_fecha_obligatoria);
        MENSAJES_SESION.put(
                ErrorValidacion.FECHA_SESION_FORMATO_INVALIDO,
                R.string.error_sesion_fecha_invalida);
        MENSAJES_SESION.put(
                ErrorValidacion.FECHA_PLANIFICACION_PASADA,
                R.string.error_sesion_fecha_pasada);
        MENSAJES_SESION.put(
                ErrorValidacion.FECHA_PASADA_FUERA_DE_VENTANA,
                R.string.error_sesion_pasada_fuera_ventana);
        MENSAJES_SESION.put(
                ErrorValidacion.HORA_INICIO_OBLIGATORIA,
                R.string.error_sesion_hora_inicio_obligatoria);
        MENSAJES_SESION.put(
                ErrorValidacion.HORA_FIN_OBLIGATORIA,
                R.string.error_sesion_hora_fin_obligatoria);
        MENSAJES_SESION.put(
                ErrorValidacion.HORA_FORMATO_INVALIDO,
                R.string.error_sesion_hora_invalida);
        MENSAJES_SESION.put(
                ErrorValidacion.HORARIO_FIN_NO_POSTERIOR,
                R.string.error_sesion_horario_fin_no_posterior);
        MENSAJES_SESION.put(
                ErrorValidacion.DURACION_REAL_INVALIDA,
                R.string.error_sesion_duracion_real_invalida);
        MENSAJES_SESION.put(
                ErrorValidacion.NOTAS_SESION_DEMASIADO_LARGAS,
                R.string.error_sesion_notas_largas);
        MENSAJES_SESION.put(
                ErrorValidacion.SESION_SOLAPADA,
                R.string.error_sesion_solapada);
        MENSAJES_SESION.put(
                ErrorValidacion.SESION_NO_ENCONTRADA,
                R.string.sesion_no_encontrada);
        MENSAJES_SESION.put(
                ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA,
                R.string.error_sesion_transicion_no_permitida);
        MENSAJES_SESION.put(
                ErrorValidacion.COMPLETAR_SOLO_MISMO_DIA,
                R.string.error_sesion_completar_mismo_dia);
        MENSAJES_SESION.put(
                ErrorValidacion.REGISTRO_TARDIO_FUERA_DE_VENTANA,
                R.string.error_sesion_registro_tardio_vencido);
        MENSAJES_SESION.put(
                ErrorValidacion.SESION_NO_ELIMINABLE,
                R.string.error_sesion_no_eliminable);
        MENSAJES_SESION.put(
                ErrorValidacion.SESION_YA_EN_CURSO,
                R.string.error_sesion_ya_en_curso);
        MENSAJES_SESION.put(
                ErrorValidacion.SIN_PERMISO_NO_MOLESTAR,
                R.string.error_sesion_sin_permiso_no_molestar);
    }

    private MensajesDeError() {
    }

    @StringRes
    public static int paraMateria(ErrorValidacion error) {
        switch (error) {
            case NOMBRE_VACIO:
                return R.string.error_materia_nombre_vacio;
            case NOMBRE_MUY_LARGO:
                return R.string.error_materia_nombre_largo;
            case NOMBRE_DUPLICADO:
                return R.string.error_materia_duplicada;
            case MATERIA_CON_SESIONES:
                return R.string.error_materia_con_sesiones;
            case MATERIA_YA_ACTIVA:
                return R.string.error_materia_ya_activa;
            case META_FUERA_DE_RANGO:
                return R.string.error_meta_fuera_de_rango;
            case MATERIA_GENERAL_PROTEGIDA:
                return R.string.error_materia_general_protegida;
            default:
                return R.string.error_materia_no_encontrada;
        }
    }

    public static String textoSesion(
            Context contexto,
            ErrorValidacion error,
            @Nullable ConflictoHorario conflicto) {
        if (error == ErrorValidacion.SESION_SOLAPADA && conflicto != null) {
            return contexto.getString(
                    R.string.error_sesion_solapada_detalle,
                    conflicto.nombreMateria(),
                    conflicto.horaInicio().texto(),
                    conflicto.horaFin().texto());
        }
        Integer recurso = MENSAJES_SESION.get(error);
        if (recurso != null) {
            return contexto.getString(recurso);
        }
        return contexto.getString(R.string.error_sesion_transicion_no_permitida);
    }
}
