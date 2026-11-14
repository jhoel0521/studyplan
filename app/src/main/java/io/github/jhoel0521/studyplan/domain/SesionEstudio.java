package io.github.jhoel0521.studyplan.domain;

import java.util.Objects;

/**
 * Entidad de dominio inmutable que representa una sesión de estudio planificada o registrada
 * (HU-10 a HU-15, RN-04 a RN-12, RN-26).
 *
 * <p>Delega las transiciones y capacidades de su ciclo de vida al patrón State ({@link EstadoSesion})
 * y devuelve {@link Resultado} sin lanzar excepciones ante reglas de negocio incumplidas (RNF-14).
 */
public final class SesionEstudio {

    public static final int LONGITUD_MAXIMA_NOTAS = 500;
    public static final int MAX_DIAS_SESION_PASADA = 3;

    private final Long id;
    private final long materiaId;
    private final Fecha fecha;
    private final Hora horaInicio;
    private final Hora horaFin;
    private final int duracionPlanificadaMinutos;
    private final Integer duracionRealMinutos;
    private final EstadoSesion estado;
    private final String notas;
    private final Long sesionOrigenId;

    private SesionEstudio(
            Long id,
            long materiaId,
            Fecha fecha,
            Hora horaInicio,
            Hora horaFin,
            int duracionPlanificadaMinutos,
            Integer duracionRealMinutos,
            EstadoSesion estado,
            String notas,
            Long sesionOrigenId) {
        this.id = id;
        this.materiaId = materiaId;
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        this.horaInicio = Objects.requireNonNull(horaInicio, "horaInicio");
        this.horaFin = Objects.requireNonNull(horaFin, "horaFin");
        this.duracionPlanificadaMinutos = duracionPlanificadaMinutos;
        this.duracionRealMinutos = duracionRealMinutos;
        this.estado = Objects.requireNonNull(estado, "estado");
        this.notas = notas == null ? "" : notas;
        this.sesionOrigenId = sesionOrigenId;
    }

    /**
     * Crea una sesión planificada para hoy o una fecha futura en estado {@code PENDIENTE}
     * (HU-10, HU-14, RN-04, RN-09).
     */
    public static Resultado<SesionEstudio> planificar(
            long materiaId,
            Fecha fecha,
            Fecha hoy,
            Hora inicio,
            Hora fin,
            String notas,
            Long sesionOrigenId) {
        ErrorValidacion errorBase = validarDatosComunes(materiaId, fecha, hoy, inicio, fin);
        if (errorBase != null) {
            return Resultado.error(errorBase);
        }
        if (fecha.esAnteriorA(hoy)) {
            return Resultado.error(ErrorValidacion.FECHA_PLANIFICACION_PASADA);
        }
        String notasSaneadas = sanearNotas(notas);
        if (notasSaneadas.length() > LONGITUD_MAXIMA_NOTAS) {
            return Resultado.error(ErrorValidacion.NOTAS_SESION_DEMASIADO_LARGAS);
        }
        return Resultado.ok(new SesionEstudio(
                null,
                materiaId,
                fecha,
                inicio,
                fin,
                inicio.minutosHasta(fin),
                null,
                EstadoPendiente.INSTANCIA,
                notasSaneadas,
                sesionOrigenId));
    }

    /**
     * Registra directamente una sesión estudiada entre 1 y 3 días atrás en estado
     * {@code COMPLETADA_TARDIA} (HU-11, RN-04, RN-06, RN-26).
     */
    public static Resultado<SesionEstudio> registrarPasada(
            long materiaId,
            Fecha fecha,
            Fecha hoy,
            Hora inicio,
            Hora fin,
            int duracionRealMinutos,
            String notas) {
        ErrorValidacion errorBase = validarDatosComunes(materiaId, fecha, hoy, inicio, fin);
        if (errorBase != null) {
            return Resultado.error(errorBase);
        }
        int diasAntiguedad = fecha.diasHasta(hoy);
        if (diasAntiguedad < 1 || diasAntiguedad > MAX_DIAS_SESION_PASADA) {
            return Resultado.error(ErrorValidacion.FECHA_PASADA_FUERA_DE_VENTANA);
        }
        if (duracionRealMinutos <= 0) {
            return Resultado.error(ErrorValidacion.DURACION_REAL_INVALIDA);
        }
        String notasSaneadas = sanearNotas(notas);
        if (notasSaneadas.length() > LONGITUD_MAXIMA_NOTAS) {
            return Resultado.error(ErrorValidacion.NOTAS_SESION_DEMASIADO_LARGAS);
        }
        return Resultado.ok(new SesionEstudio(
                null,
                materiaId,
                fecha,
                inicio,
                fin,
                inicio.minutosHasta(fin),
                duracionRealMinutos,
                EstadoCompletadaTardia.INSTANCIA,
                notasSaneadas,
                null));
    }

    /** Reconstruye una sesión leída desde almacenamiento persistente. */
    public static SesionEstudio restaurar(
            Long id,
            long materiaId,
            Fecha fecha,
            Hora horaInicio,
            Hora horaFin,
            int duracionPlanificadaMinutos,
            Integer duracionRealMinutos,
            CodigoEstadoSesion codigoEstado,
            String notas,
            Long sesionOrigenId) {
        return new SesionEstudio(
                id,
                materiaId,
                fecha,
                horaInicio,
                horaFin,
                duracionPlanificadaMinutos,
                duracionRealMinutos,
                EstadoSesion.de(codigoEstado),
                sanearNotas(notas),
                sesionOrigenId);
    }

    /** Sanea el texto de notas opcionales recortando espacios extremos. */
    public static String sanearNotas(String notas) {
        return notas == null ? "" : notas.trim();
    }

    public Long id() {
        return id;
    }

    public long materiaId() {
        return materiaId;
    }

    public Fecha fecha() {
        return fecha;
    }

    public Hora horaInicio() {
        return horaInicio;
    }

    public Hora horaFin() {
        return horaFin;
    }

    public int duracionPlanificadaMinutos() {
        return duracionPlanificadaMinutos;
    }

    public Integer duracionRealMinutos() {
        return duracionRealMinutos;
    }

    public EstadoSesion estado() {
        return estado;
    }

    public CodigoEstadoSesion codigoEstado() {
        return estado.codigo();
    }

    public String notas() {
        return notas;
    }

    public Long sesionOrigenId() {
        return sesionOrigenId;
    }

    /** Devuelve una copia con el identificador asignado por persistencia. */
    public SesionEstudio conId(long nuevoId) {
        return new SesionEstudio(
                nuevoId,
                materiaId,
                fecha,
                horaInicio,
                horaFin,
                duracionPlanificadaMinutos,
                duracionRealMinutos,
                estado,
                notas,
                sesionOrigenId);
    }

    /**
     * Comprueba si esta sesión se cruza con otra franja horaria en {@code otraFecha} (RN-05).
     * Las sesiones reprogramadas no ocupan horario y los extremos contiguos no se consideran cruce.
     */
    public boolean seSolapaCon(Fecha otraFecha, Hora otraInicio, Hora otraFin) {
        if (otraFecha == null || otraInicio == null || otraFin == null) {
            return false;
        }
        if (!estado.ocupaHorario() || !fecha.equals(otraFecha)) {
            return false;
        }
        return otraInicio.minutosDesdeMedianoche() < horaFin.minutosDesdeMedianoche()
                && otraFin.minutosDesdeMedianoche() > horaInicio.minutosDesdeMedianoche();
    }

    /** Minutos reales contabilizables en progreso semanal y acumulado por materia (RN-11). */
    public int minutosRealesContabilizables() {
        if (!estado.sumaHorasReales() || duracionRealMinutos == null || duracionRealMinutos <= 0) {
            return 0;
        }
        return duracionRealMinutos;
    }

    /** Marca completada una sesión pendiente el mismo día programado (HU-12, RN-07, RN-26). */
    public Resultado<SesionEstudio> completar(Fecha hoy, int duracionReal) {
        return aplicarTransicion(estado.marcarCompletada(fecha, hoy, duracionReal), duracionReal);
    }

    /** Vence una sesión pendiente cuya fecha ya pasó (RN-08). */
    public Resultado<SesionEstudio> vencer() {
        return aplicarTransicion(estado.vencer(), duracionRealMinutos);
    }

    /** Marca una sesión pendiente o no realizada como reprogramada (HU-14, RN-09). */
    public Resultado<SesionEstudio> marcarReprogramada() {
        return aplicarTransicion(estado.reprogramar(), duracionRealMinutos);
    }

    /** Registra tardíamente una sesión no realizada dentro de la ventana de 3 días (HU-13, RN-10). */
    public Resultado<SesionEstudio> registrarTardio(Fecha hoy, int duracionReal) {
        if (hoy == null) {
            return Resultado.error(ErrorValidacion.FECHA_SESION_OBLIGATORIA);
        }
        int diasAntiguedad = fecha.diasHasta(hoy);
        return aplicarTransicion(estado.registrarTardio(diasAntiguedad, duracionReal), duracionReal);
    }

    /** Valida si esta sesión puede eliminarse permanentemente según RN-12 (HU-15). */
    public Resultado<SesionEstudio> validarEliminacion() {
        Resultado<Boolean> permitido = estado.validarEliminacion();
        if (!permitido.esValido()) {
            return Resultado.error(permitido.error());
        }
        return Resultado.ok(this);
    }

    private Resultado<SesionEstudio> aplicarTransicion(
            Resultado<EstadoSesion> transicion, Integer nuevaDuracionReal) {
        if (!transicion.esValido()) {
            return Resultado.error(transicion.error());
        }
        return Resultado.ok(new SesionEstudio(
                id,
                materiaId,
                fecha,
                horaInicio,
                horaFin,
                duracionPlanificadaMinutos,
                nuevaDuracionReal,
                transicion.valor(),
                notas,
                sesionOrigenId));
    }

    private static ErrorValidacion validarDatosComunes(
            long materiaId, Fecha fecha, Fecha hoy, Hora inicio, Hora fin) {
        if (materiaId <= 0) {
            return ErrorValidacion.MATERIA_SESION_OBLIGATORIA;
        }
        if (fecha == null || hoy == null) {
            return ErrorValidacion.FECHA_SESION_OBLIGATORIA;
        }
        if (inicio == null) {
            return ErrorValidacion.HORA_INICIO_OBLIGATORIA;
        }
        if (fin == null) {
            return ErrorValidacion.HORA_FIN_OBLIGATORIA;
        }
        if (!fin.esPosteriorA(inicio)) {
            return ErrorValidacion.HORARIO_FIN_NO_POSTERIOR;
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SesionEstudio)) {
            return false;
        }
        SesionEstudio otra = (SesionEstudio) o;
        return materiaId == otra.materiaId
                && duracionPlanificadaMinutos == otra.duracionPlanificadaMinutos
                && Objects.equals(id, otra.id)
                && fecha.equals(otra.fecha)
                && horaInicio.equals(otra.horaInicio)
                && horaFin.equals(otra.horaFin)
                && Objects.equals(duracionRealMinutos, otra.duracionRealMinutos)
                && estado.codigo() == otra.estado.codigo()
                && notas.equals(otra.notas)
                && Objects.equals(sesionOrigenId, otra.sesionOrigenId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                materiaId,
                fecha,
                horaInicio,
                horaFin,
                duracionPlanificadaMinutos,
                duracionRealMinutos,
                estado.codigo(),
                notas,
                sesionOrigenId);
    }
}
