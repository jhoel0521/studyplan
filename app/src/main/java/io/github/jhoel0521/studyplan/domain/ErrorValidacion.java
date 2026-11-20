package io.github.jhoel0521.studyplan.domain;

/**
 * Códigos de error de validación del dominio. El dominio no devuelve textos: la capa de presentación
 * traduce cada código a su mensaje en {@code strings.xml} (RNF-08).
 *
 * <p>Implementa: HU-01 (nombre obligatorio), RN-14 (rango de la meta), HU-06 a HU-08 y HU-46 (materias, RN-02 y
 * RN-03), HU-10 a HU-15 (sesiones de estudio, RN-04 a RN-08) y RNF-14 (validación de entradas).
 */
public enum ErrorValidacion {
    /** El nombre quedó vacío tras recortar los espacios (HU-01 y HU-06). */
    NOMBRE_VACIO,
    /** La meta semanal no es mayor que 0 y menor o igual a 168 horas (RN-14). */
    META_FUERA_DE_RANGO,
    /** El nombre de la materia supera los 50 caracteres una vez saneado (RN-02). */
    NOMBRE_MUY_LARGO,
    /** Ya existe una materia activa con ese nombre, sin distinguir mayúsculas ni acentos (RN-02). */
    NOMBRE_DUPLICADO,
    /** No existe la materia pedida. */
    MATERIA_NO_ENCONTRADA,
    /** La materia ya está activa y no hace falta desarchivarla (HU-46). */
    MATERIA_YA_ACTIVA,
    /** La materia tiene sesiones registradas y no se elimina, solo se archiva (RN-03). */
    MATERIA_CON_SESIONES,
    /** No se seleccionó una materia válida para la sesión (HU-10, HU-11). */
    MATERIA_SESION_OBLIGATORIA,
    /** La materia elegida para la sesión está archivada o inactiva (HU-10, HU-11). */
    MATERIA_INACTIVA,
    /** La fecha de la sesión está vacía (HU-10, HU-11, HU-14). */
    FECHA_SESION_OBLIGATORIA,
    /** La fecha de la sesión no tiene un formato ISO YYYY-MM-DD válido (HU-10, HU-11, HU-14). */
    FECHA_SESION_FORMATO_INVALIDO,
    /** No se puede planificar ni reprogramar una sesión para una fecha anterior a hoy (HU-10, HU-14). */
    FECHA_PLANIFICACION_PASADA,
    /** Una sesión pasada solo puede registrarse entre 1 y 3 días atrás respecto a hoy (RN-06). */
    FECHA_PASADA_FUERA_DE_VENTANA,
    /** La hora de inicio de la sesión está vacía (HU-10, HU-11, HU-14). */
    HORA_INICIO_OBLIGATORIA,
    /** La hora de fin de la sesión está vacía (HU-10, HU-11, HU-14). */
    HORA_FIN_OBLIGATORIA,
    /** La hora ingresada no cumple un formato HH:mm válido (HU-10, HU-11, HU-14). */
    HORA_FORMATO_INVALIDO,
    /** La hora de fin debe ser estrictamente posterior a la hora de inicio (RN-04). */
    HORARIO_FIN_NO_POSTERIOR,
    /** La duración real de la sesión debe ser mayor que cero minutos (RN-26). */
    DURACION_REAL_INVALIDA,
    /** Las notas de la sesión superan el máximo de 500 caracteres (HU-10). */
    NOTAS_SESION_DEMASIADO_LARGAS,
    /** Existe otra sesión que ocupa ese horario en la misma fecha (RN-05). */
    SESION_SOLAPADA,
    /** No existe la sesión solicitada. */
    SESION_NO_ENCONTRADA,
    /** El estado actual de la sesión no permite la transición solicitada (RN-07). */
    TRANSICION_ESTADO_NO_PERMITIDA,
    /** Una sesión pendiente solo puede marcarse completada el mismo día programado (HU-12, RN-07). */
    COMPLETAR_SOLO_MISMO_DIA,
    /** Una sesión no realizada solo admite registro tardío hasta 3 días después de su fecha (RN-10). */
    REGISTRO_TARDIO_FUERA_DE_VENTANA,
    /** Solo pueden eliminarse sesiones en estado Pendiente (RN-12). */
    SESION_NO_ELIMINABLE,
    /** Los parámetros de Pomodoro están fuera de sus rangos válidos (HU-23, RN-17). */
    CONFIGURACION_POMODORO_INVALIDA,
    /** Ya existe otra sesión en estado En curso que debe resolverse primero (HU-22). */
    SESION_YA_EN_CURSO,
    /** La hora declarada al volver no es posterior a la salida ni anterior o igual a ahora (RN-24). */
    HORA_DECLARADA_FUERA_DE_RANGO,
    /** La hora de corte elegida para repartir un tramo no cae dentro del intervalo efectivo (RN-28). */
    CORTE_REPARTO_INVALIDO,
    /** La materia general «Estudios varios» no admite edición, archivado, eliminación ni meta propia (RN-32). */
    MATERIA_GENERAL_PROTEGIDA,
    /** La app no tiene acceso a No molestar, así que no se puede alternar en la sesión (HU-24, RNF-18). */
    SIN_PERMISO_NO_MOLESTAR
}
