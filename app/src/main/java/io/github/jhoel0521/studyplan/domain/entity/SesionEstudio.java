package io.github.jhoel0521.studyplan.domain.entity;

import java.util.Objects;
import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.state.EstadoCompletadaTardia;
import io.github.jhoel0521.studyplan.domain.state.EstadoEnCurso;
import io.github.jhoel0521.studyplan.domain.state.EstadoPendiente;
import io.github.jhoel0521.studyplan.domain.state.EstadoSesion;
import io.github.jhoel0521.studyplan.domain.valueobject.ConfiguracionPomodoro;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.valueobject.Instante;

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
    private final long planInicioUtcMilis;
    private final Long planFinUtcMilis;
    private final int desfaseMinutos;
    private final int duracionPlanificadaMinutos;
    private final Integer duracionRealMinutos;
    private final EstadoSesion estado;
    private final String notas;
    private final Long sesionOrigenId;
    private final TecnicaEstudio tecnica;
    private final PreferenciaNoMolestar preferenciaNoMolestar;
    private final Integer pomodoroTrabajoMinutos;
    private final Integer pomodoroDescansoCortoMinutos;
    private final Integer pomodoroDescansoLargoMinutos;
    private final Integer pomodoroCiclosHastaLargo;
    private final AntelacionRecordatorio antelacionRecordatorio;

    private SesionEstudio(
            Long id,
            long materiaId,
            long planInicioUtcMilis,
            Long planFinUtcMilis,
            int desfaseMinutos,
            int duracionPlanificadaMinutos,
            Integer duracionRealMinutos,
            EstadoSesion estado,
            String notas,
            Long sesionOrigenId,
            TecnicaEstudio tecnica,
            PreferenciaNoMolestar preferenciaNoMolestar,
            Integer pomodoroTrabajoMinutos,
            Integer pomodoroDescansoCortoMinutos,
            Integer pomodoroDescansoLargoMinutos,
            Integer pomodoroCiclosHastaLargo,
            AntelacionRecordatorio antelacionRecordatorio) {
        this.id = id;
        this.materiaId = materiaId;
        this.planInicioUtcMilis = planInicioUtcMilis;
        this.planFinUtcMilis = planFinUtcMilis;
        this.desfaseMinutos = desfaseMinutos;
        this.duracionPlanificadaMinutos = duracionPlanificadaMinutos;
        this.duracionRealMinutos = duracionRealMinutos;
        this.estado = Objects.requireNonNull(estado, "estado");
        this.notas = notas == null ? "" : notas;
        this.sesionOrigenId = sesionOrigenId;
        this.tecnica = tecnica == null ? TecnicaEstudio.LIBRE : tecnica;
        this.preferenciaNoMolestar = preferenciaNoMolestar == null
                ? PreferenciaNoMolestar.HEREDAR_GLOBAL
                : preferenciaNoMolestar;
        this.pomodoroTrabajoMinutos = pomodoroTrabajoMinutos;
        this.pomodoroDescansoCortoMinutos = pomodoroDescansoCortoMinutos;
        this.pomodoroDescansoLargoMinutos = pomodoroDescansoLargoMinutos;
        this.pomodoroCiclosHastaLargo = pomodoroCiclosHastaLargo;
        this.antelacionRecordatorio = antelacionRecordatorio == null
                ? AntelacionRecordatorio.SIN_RECORDATORIO
                : antelacionRecordatorio;
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
        return planificar(
                materiaId,
                fecha,
                hoy,
                inicio,
                fin,
                0,
                notas,
                sesionOrigenId,
                AntelacionRecordatorio.SIN_RECORDATORIO);
    }

    /**
     * Crea una sesión planificada conservando el desfase horario local vigente al planificar
     * (HU-10, HU-14, RN-04, RN-09). Si {@code fin} es anterior a {@code inicio}, termina al día siguiente.
     */
    public static Resultado<SesionEstudio> planificar(
            long materiaId,
            Fecha fecha,
            Fecha hoy,
            Hora inicio,
            Hora fin,
            int desfaseMinutos,
            String notas,
            Long sesionOrigenId) {
        return planificar(
                materiaId,
                fecha,
                hoy,
                inicio,
                fin,
                desfaseMinutos,
                notas,
                sesionOrigenId,
                AntelacionRecordatorio.SIN_RECORDATORIO);
    }

    /**
     * Crea una sesión planificada conservando el desfase horario local y la antelación de
     * recordatorio elegida (HU-10, HU-14, HU-34, RN-04, RN-09).
     */
    public static Resultado<SesionEstudio> planificar(
            long materiaId,
            Fecha fecha,
            Fecha hoy,
            Hora inicio,
            Hora fin,
            int desfaseMinutos,
            String notas,
            Long sesionOrigenId,
            AntelacionRecordatorio antelacionRecordatorio) {
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
        Instante instInicio = Instante.desdeFechaYHoraLocal(fecha, inicio, desfaseMinutos);
        Instante instFin = calcularFinInstante(fecha, inicio, fin, desfaseMinutos);
        int duracionMinutos = calcularDuracionMinutos(inicio, fin);
        return Resultado.ok(new SesionEstudio(
                null,
                materiaId,
                instInicio.utcMilis(),
                instFin.utcMilis(),
                desfaseMinutos,
                duracionMinutos,
                null,
                EstadoPendiente.INSTANCIA,
                notasSaneadas,
                sesionOrigenId,
                TecnicaEstudio.LIBRE,
                PreferenciaNoMolestar.HEREDAR_GLOBAL,
                null,
                null,
                null,
                null,
                antelacionRecordatorio));
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
        return registrarPasada(materiaId, fecha, hoy, inicio, fin, 0, duracionRealMinutos, notas);
    }

    /**
     * Registra directamente una sesión estudiada entre 1 y 3 días atrás conservando el desfase
     * horario local (HU-11, RN-04, RN-06, RN-26).
     */
    public static Resultado<SesionEstudio> registrarPasada(
            long materiaId,
            Fecha fecha,
            Fecha hoy,
            Hora inicio,
            Hora fin,
            int desfaseMinutos,
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
        Instante instInicio = Instante.desdeFechaYHoraLocal(fecha, inicio, desfaseMinutos);
        Instante instFin = calcularFinInstante(fecha, inicio, fin, desfaseMinutos);
        int duracionMinutos = calcularDuracionMinutos(inicio, fin);
        return Resultado.ok(new SesionEstudio(
                null,
                materiaId,
                instInicio.utcMilis(),
                instFin.utcMilis(),
                desfaseMinutos,
                duracionMinutos,
                duracionRealMinutos,
                EstadoCompletadaTardia.INSTANCIA,
                notasSaneadas,
                null,
                TecnicaEstudio.LIBRE,
                PreferenciaNoMolestar.HEREDAR_GLOBAL,
                null,
                null,
                null,
                null,
                AntelacionRecordatorio.SIN_RECORDATORIO));
    }
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
        return restaurar(
                id,
                materiaId,
                fecha,
                horaInicio,
                horaFin,
                duracionPlanificadaMinutos,
                duracionRealMinutos,
                codigoEstado,
                notas,
                sesionOrigenId,
                TecnicaEstudio.LIBRE,
                PreferenciaNoMolestar.HEREDAR_GLOBAL,
                null,
                null,
                null,
                null);
    }

    /** Reconstruye una sesión leída desde almacenamiento persistente v4 con técnica y Pomodoro. */
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
            Long sesionOrigenId,
            TecnicaEstudio tecnica,
            PreferenciaNoMolestar preferenciaNoMolestar,
            Integer pomodoroTrabajoMinutos,
            Integer pomodoroDescansoCortoMinutos,
            Integer pomodoroDescansoLargoMinutos,
            Integer pomodoroCiclosHastaLargo) {
        Instante instInicio = Instante.desdeFechaYHoraLocal(fecha, horaInicio, 0);
        Instante instFin = calcularFinInstante(fecha, horaInicio, horaFin, 0);
        return restaurar(
                id,
                materiaId,
                instInicio.utcMilis(),
                instFin.utcMilis(),
                0,
                duracionPlanificadaMinutos,
                duracionRealMinutos,
                codigoEstado,
                notas,
                sesionOrigenId,
                tecnica,
                preferenciaNoMolestar,
                pomodoroTrabajoMinutos,
                pomodoroDescansoCortoMinutos,
                pomodoroDescansoLargoMinutos,
                pomodoroCiclosHastaLargo);
    }

    /**
     * Crea e inicia directamente una sesión sin horario planificado en estado {@code EN_CURSO}
     * (transición Nueva → En curso de {@code RN-07}, {@code HU-47}, {@code HU-48}).
     */
    public static Resultado<SesionEstudio> estudiarAhora(
            long materiaId,
            Instante inicio,
            TecnicaEstudio tecnicaElegida,
            ConfiguracionPomodoro snapshotPomodoro,
            PreferenciaNoMolestar preferenciaNoMolestar) {
        if (materiaId <= 0) {
            return Resultado.error(ErrorValidacion.MATERIA_SESION_OBLIGATORIA);
        }
        if (inicio == null) {
            return Resultado.error(ErrorValidacion.FECHA_SESION_OBLIGATORIA);
        }
        TecnicaEstudio efectiva = tecnicaElegida == null ? TecnicaEstudio.LIBRE : tecnicaElegida;
        ConfiguracionPomodoro snap = efectiva == TecnicaEstudio.POMODORO
                ? (snapshotPomodoro == null ? ConfiguracionPomodoro.porDefecto() : snapshotPomodoro)
                : null;
        return Resultado.ok(new SesionEstudio(
                null,
                materiaId,
                inicio.utcMilis(),
                null,
                inicio.desfaseMinutos(),
                0,
                null,
                EstadoEnCurso.INSTANCIA,
                "",
                null,
                efectiva,
                preferenciaNoMolestar,
                snap == null ? null : snap.trabajoMinutos(),
                snap == null ? null : snap.descansoCortoMinutos(),
                snap == null ? null : snap.descansoLargoMinutos(),
                snap == null ? null : snap.ciclosHastaLargo(),
                AntelacionRecordatorio.SIN_RECORDATORIO));
    }

    /** Reconstruye una sesión leída desde almacenamiento persistente v5/v6 con instantes UTC y desfase. */
    public static SesionEstudio restaurar(
            Long id,
            long materiaId,
            long planInicioUtcMilis,
            Long planFinUtcMilis,
            int desfaseMinutos,
            int duracionPlanificadaMinutos,
            Integer duracionRealMinutos,
            CodigoEstadoSesion codigoEstado,
            String notas,
            Long sesionOrigenId,
            TecnicaEstudio tecnica,
            PreferenciaNoMolestar preferenciaNoMolestar,
            Integer pomodoroTrabajoMinutos,
            Integer pomodoroDescansoCortoMinutos,
            Integer pomodoroDescansoLargoMinutos,
            Integer pomodoroCiclosHastaLargo) {
        return restaurar(
                id,
                materiaId,
                planInicioUtcMilis,
                planFinUtcMilis,
                desfaseMinutos,
                duracionPlanificadaMinutos,
                duracionRealMinutos,
                codigoEstado,
                notas,
                sesionOrigenId,
                tecnica,
                preferenciaNoMolestar,
                pomodoroTrabajoMinutos,
                pomodoroDescansoCortoMinutos,
                pomodoroDescansoLargoMinutos,
                pomodoroCiclosHastaLargo,
                AntelacionRecordatorio.SIN_RECORDATORIO);
    }

    /** Reconstruye una sesión leída desde almacenamiento persistente v8 con antelación de recordatorio. */
    public static SesionEstudio restaurar(
            Long id,
            long materiaId,
            long planInicioUtcMilis,
            Long planFinUtcMilis,
            int desfaseMinutos,
            int duracionPlanificadaMinutos,
            Integer duracionRealMinutos,
            CodigoEstadoSesion codigoEstado,
            String notas,
            Long sesionOrigenId,
            TecnicaEstudio tecnica,
            PreferenciaNoMolestar preferenciaNoMolestar,
            Integer pomodoroTrabajoMinutos,
            Integer pomodoroDescansoCortoMinutos,
            Integer pomodoroDescansoLargoMinutos,
            Integer pomodoroCiclosHastaLargo,
            AntelacionRecordatorio antelacionRecordatorio) {
        return new SesionEstudio(
                id,
                materiaId,
                planInicioUtcMilis,
                planFinUtcMilis,
                desfaseMinutos,
                duracionPlanificadaMinutos,
                duracionRealMinutos,
                EstadoSesion.de(codigoEstado),
                sanearNotas(notas),
                sesionOrigenId,
                tecnica,
                preferenciaNoMolestar,
                pomodoroTrabajoMinutos,
                pomodoroDescansoCortoMinutos,
                pomodoroDescansoLargoMinutos,
                pomodoroCiclosHastaLargo,
                antelacionRecordatorio);
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

    public long planInicioUtcMilis() {
        return planInicioUtcMilis;
    }

    public Long planFinUtcMilis() {
        return planFinUtcMilis;
    }

    /** Indica si la sesión tiene horario planificado ({@code false} para sesiones «Estudiar ahora»). */
    public boolean tienePlan() {
        return planFinUtcMilis != null;
    }

    public int desfaseMinutos() {
        return desfaseMinutos;
    }

    public Instante inicioInstante() {
        return Instante.de(planInicioUtcMilis, desfaseMinutos);
    }

    /** Instante de fin del horario planificado, o {@code null} si la sesión no tiene horario planificado. */
    public Instante finInstante() {
        return planFinUtcMilis == null ? null : finDelPlan();
    }

    /** Instante de fin del horario planificado; solo se llama cuando {@code planFinUtcMilis} no es nulo. */
    private Instante finDelPlan() {
        return Instante.de(planFinUtcMilis, desfaseMinutos);
    }

    /** Fecha local de inicio derivada del instante de inicio y su desfase guardado. */
    public Fecha fecha() {
        return inicioInstante().fechaLocal();
    }

    /** Fecha local de fin derivada del instante de fin y su desfase guardado. */
    public Fecha fechaFinLocal() {
        return planFinUtcMilis == null ? fecha() : finDelPlan().fechaLocal();
    }

    /** Indica si la sesión planificada cruza la medianoche local. */
    public boolean cruzaMedianoche() {
        return planFinUtcMilis != null && !fecha().equals(fechaFinLocal());
    }

    /** Hora local de inicio derivada del instante de inicio y su desfase guardado. */
    public Hora horaInicio() {
        return inicioInstante().horaLocal();
    }

    /** Hora local de fin derivada del instante de fin y su desfase guardado. */
    public Hora horaFin() {
        return planFinUtcMilis == null ? horaInicio() : finDelPlan().horaLocal();
    }

    public int duracionPlanificadaMinutos() {
        return duracionPlanificadaMinutos;
    }

    /**
     * Minutos planificados de la sesión dentro del rango {@code [inicio, fin]} aplicando el corte
     * estricto diario en medianoche ({@code HU-32}, {@code RN-31}). Si cruza medianoche con fin
     * exacto {@code 00:00}, asigna {@code 0} minutos al día siguiente.
     */
    public int minutosPlanificadosEnRango(Fecha inicio, Fecha fin) {
        if (inicio == null || fin == null || !tienePlan() || duracionPlanificadaMinutos <= 0) {
            return 0;
        }
        if (fin.esAnteriorA(inicio)) {
            return 0;
        }
        if (!cruzaMedianoche()) {
            Fecha dia = fecha();
            return (!dia.esAnteriorA(inicio) && !dia.esPosteriorA(fin))
                    ? duracionPlanificadaMinutos
                    : 0;
        }
        int total = 0;
        Fecha diaInicio = fecha();
        if (!diaInicio.esAnteriorA(inicio) && !diaInicio.esPosteriorA(fin)) {
            total += Math.max(0, 24 * 60 - horaInicio().minutosDesdeMedianoche());
        }
        Fecha diaFin = fechaFinLocal();
        if (!diaFin.esAnteriorA(inicio) && !diaFin.esPosteriorA(fin)) {
            total += Math.max(0, horaFin().minutosDesdeMedianoche());
        }
        return total;
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

    public TecnicaEstudio tecnica() {
        return tecnica;
    }

    public PreferenciaNoMolestar preferenciaNoMolestar() {
        return preferenciaNoMolestar;
    }

    public Integer pomodoroTrabajoMinutos() {
        return pomodoroTrabajoMinutos;
    }

    public Integer pomodoroDescansoCortoMinutos() {
        return pomodoroDescansoCortoMinutos;
    }

    public Integer pomodoroDescansoLargoMinutos() {
        return pomodoroDescansoLargoMinutos;
    }

    public Integer pomodoroCiclosHastaLargo() {
        return pomodoroCiclosHastaLargo;
    }

    public AntelacionRecordatorio antelacionRecordatorio() {
        return antelacionRecordatorio;
    }

    /**
     * Instante UTC en el que debe dispararse el recordatorio previo al inicio planificado, o
     * {@code null} si la sesión no tiene plan o el recordatorio está desactivado ({@code HU-34}).
     */
    public Long instanteRecordatorioUtcMilis() {
        if (!tienePlan() || !antelacionRecordatorio.activo()) {
            return null;
        }
        return inicioInstante().sumarMinutos(-antelacionRecordatorio.minutos()).utcMilis();
    }

    /**
     * Indica si al momento de guardar o reprogramar en {@code ahoraUtcMilis} la hora del aviso ya
     * quedó atrás pero la sesión aún no inicia ({@code inicio − antelación <= ahora < inicio}).
     */
    public boolean recordatorioVencidoAlPlanificar(long ahoraUtcMilis) {
        Long disparo = instanteRecordatorioUtcMilis();
        return disparo != null && disparo <= ahoraUtcMilis && ahoraUtcMilis < planInicioUtcMilis;
    }

    /**
     * Indica si {@code ahoraUtcMilis} cae dentro de la ventana del aviso previo o en el instante de
     * inicio ({@code inicio − antelación <= ahora <= inicio}).
     */
    public boolean estaEnVentanaRecordatorio(long ahoraUtcMilis) {
        Long disparo = instanteRecordatorioUtcMilis();
        return disparo != null && ahoraUtcMilis >= disparo && ahoraUtcMilis <= planInicioUtcMilis;
    }

    /** Devuelve el snapshot Pomodoro almacenado o {@code null} si la sesión es Libre. */
    public ConfiguracionPomodoro configuracionPomodoroSnapshot() {
        if (pomodoroTrabajoMinutos == null
                || pomodoroDescansoCortoMinutos == null
                || pomodoroDescansoLargoMinutos == null
                || pomodoroCiclosHastaLargo == null) {
            return null;
        }
        return ConfiguracionPomodoro.restaurar(
                pomodoroTrabajoMinutos,
                pomodoroDescansoCortoMinutos,
                pomodoroDescansoLargoMinutos,
                pomodoroCiclosHastaLargo);
    }

    /** Indica si esta sesión debe activar No molestar según su preferencia y el ajuste global. */
    public boolean debeActivarNoMolestar(boolean noMolestarGlobal) {
        switch (preferenciaNoMolestar) {
            case FORZAR_ACTIVAR:
                return true;
            case FORZAR_DESACTIVAR:
                return false;
            case HEREDAR_GLOBAL:
            default:
                return noMolestarGlobal;
        }
    }

    /** Devuelve una copia con el identificador asignado por persistencia. */
    public SesionEstudio conId(long nuevoId) {
        return new SesionEstudio(
                nuevoId,
                materiaId,
                planInicioUtcMilis,
                planFinUtcMilis,
                desfaseMinutos,
                duracionPlanificadaMinutos,
                duracionRealMinutos,
                estado,
                notas,
                sesionOrigenId,
                tecnica,
                preferenciaNoMolestar,
                pomodoroTrabajoMinutos,
                pomodoroDescansoCortoMinutos,
                pomodoroDescansoLargoMinutos,
                pomodoroCiclosHastaLargo,
                antelacionRecordatorio);
    }

    public SesionEstudio conPreferenciaNoMolestar(PreferenciaNoMolestar nuevaPreferencia) {
        return new SesionEstudio(
                id,
                materiaId,
                planInicioUtcMilis,
                planFinUtcMilis,
                desfaseMinutos,
                duracionPlanificadaMinutos,
                duracionRealMinutos,
                estado,
                notas,
                sesionOrigenId,
                tecnica,
                nuevaPreferencia == null ? PreferenciaNoMolestar.HEREDAR_GLOBAL : nuevaPreferencia,
                pomodoroTrabajoMinutos,
                pomodoroDescansoCortoMinutos,
                pomodoroDescansoLargoMinutos,
                pomodoroCiclosHastaLargo,
                antelacionRecordatorio);
    }

    public SesionEstudio conTecnica(TecnicaEstudio nuevaTecnica) {
        return new SesionEstudio(
                id,
                materiaId,
                planInicioUtcMilis,
                planFinUtcMilis,
                desfaseMinutos,
                duracionPlanificadaMinutos,
                duracionRealMinutos,
                estado,
                notas,
                sesionOrigenId,
                nuevaTecnica == null ? TecnicaEstudio.LIBRE : nuevaTecnica,
                preferenciaNoMolestar,
                pomodoroTrabajoMinutos,
                pomodoroDescansoCortoMinutos,
                pomodoroDescansoLargoMinutos,
                pomodoroCiclosHastaLargo,
                antelacionRecordatorio);
    }

    /**
     * Comprueba si esta sesión se cruza con otra franja horaria en {@code otraFecha} (RN-05).
     * Las sesiones reprogramadas no ocupan horario y los extremos contiguos no se consideran cruce.
     */
    public boolean seSolapaCon(Fecha otraFecha, Hora otraInicio, Hora otraFin) {
        return seSolapaCon(otraFecha, otraInicio, otraFin, desfaseMinutos);
    }

    /**
     * Comprueba si esta sesión se cruza con otra franja horaria local en {@code otroDesfaseMinutos}
     * (RN-05), incluyendo sesiones nocturnas que cruzan la medianoche.
     */
    public boolean seSolapaCon(
            Fecha otraFecha, Hora otraInicio, Hora otraFin, int otroDesfaseMinutos) {
        if (otraFecha == null || otraInicio == null || otraFin == null) {
            return false;
        }
        Instante otroInicioInst = Instante.desdeFechaYHoraLocal(otraFecha, otraInicio, otroDesfaseMinutos);
        Instante otroFinInst = calcularFinInstante(otraFecha, otraInicio, otraFin, otroDesfaseMinutos);
        return seSolapaCon(otroInicioInst, otroFinInst);
    }

    /**
     * Comprueba si esta sesión se cruza con el intervalo absoluto {@code [otroInicio, otroFin)} (RN-05).
     * Las sesiones sin horario planificado nunca generan cruce.
     */
    public boolean seSolapaCon(Instante otroInicio, Instante otroFin) {
        if (otroInicio == null || otroFin == null || planFinUtcMilis == null || !estado.ocupaHorario()) {
            return false;
        }
        return otroInicio.utcMilis() < planFinUtcMilis && otroFin.utcMilis() > planInicioUtcMilis;
    }

    /** Comprueba si esta sesión se cruza en tiempo absoluto con {@code otra} (RN-05). */
    public boolean seSolapaCon(SesionEstudio otra) {
        return otra != null && otra.planFinUtcMilis != null
                && seSolapaCon(otra.inicioInstante(), otra.finDelPlan());
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
        return aplicarTransicion(estado.marcarCompletada(fecha(), hoy, duracionReal), duracionReal);
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
        int diasAntiguedad = fecha().diasHasta(hoy);
        return aplicarTransicion(estado.registrarTardio(diasAntiguedad, duracionReal), duracionReal);
    }

    /** Inicia la sesión en vivo fijando la técnica y el snapshot Pomodoro si aplica (HU-22, HU-23, RN-07, RN-17). */
    public Resultado<SesionEstudio> iniciar(
            TecnicaEstudio tecnicaElegida, ConfiguracionPomodoro snapshotPomodoro) {
        Resultado<EstadoSesion> transicion = estado.iniciar();
        if (!transicion.esValido()) {
            return Resultado.error(transicion.error());
        }
        TecnicaEstudio efectiva = tecnicaElegida == null ? TecnicaEstudio.LIBRE : tecnicaElegida;
        ConfiguracionPomodoro snap = efectiva == TecnicaEstudio.POMODORO
                ? (snapshotPomodoro == null ? ConfiguracionPomodoro.porDefecto() : snapshotPomodoro)
                : null;
        return Resultado.ok(new SesionEstudio(
                id,
                materiaId,
                planInicioUtcMilis,
                planFinUtcMilis,
                desfaseMinutos,
                duracionPlanificadaMinutos,
                duracionRealMinutos,
                transicion.valor(),
                notas,
                sesionOrigenId,
                efectiva,
                preferenciaNoMolestar,
                snap == null ? null : snap.trabajoMinutos(),
                snap == null ? null : snap.descansoCortoMinutos(),
                snap == null ? null : snap.descansoLargoMinutos(),
                snap == null ? null : snap.ciclosHastaLargo(),
                antelacionRecordatorio));
    }

    /** Registra pausa o reanudación manteniendo el estado {@code EN_CURSO} (RN-07). */
    public Resultado<SesionEstudio> pausarOReanudar() {
        return aplicarTransicion(estado.pausarReanudar(), duracionRealMinutos);
    }

    /** Detiene la sesión en curso y la marca {@code COMPLETADA} con los minutos reales medidos (HU-22, RN-07). */
    public Resultado<SesionEstudio> detener(int duracionReal) {
        return aplicarTransicion(estado.detener(duracionReal), duracionReal);
    }

    /** Completa la sesión en curso al cambiar directamente a la siguiente sesión (HU-43, RN-07, RN-27). */
    public Resultado<SesionEstudio> cambiarASiguiente(int duracionReal) {
        return aplicarTransicion(estado.cambiarASiguiente(duracionReal), duracionReal);
    }

    /** Completa la sesión en curso al declarar fin al volver a la app (HU-41, RN-07, RN-24). */
    public Resultado<SesionEstudio> declararFinAlVolver(int duracionReal) {
        return aplicarTransicion(estado.declararFinAlVolver(duracionReal), duracionReal);
    }

    /** Cierra la sesión en curso tras vencer el plazo de 3 días sin regreso (HU-41, RN-07, RN-29). */
    public Resultado<SesionEstudio> cerrarPorPlazoVencido(int duracionReal) {
        return aplicarTransicion(estado.cerrarPorPlazoVencido(duracionReal), duracionReal);
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
                planInicioUtcMilis,
                planFinUtcMilis,
                desfaseMinutos,
                duracionPlanificadaMinutos,
                nuevaDuracionReal,
                transicion.valor(),
                notas,
                sesionOrigenId,
                tecnica,
                preferenciaNoMolestar,
                pomodoroTrabajoMinutos,
                pomodoroDescansoCortoMinutos,
                pomodoroDescansoLargoMinutos,
                pomodoroCiclosHastaLargo,
                antelacionRecordatorio));
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
        if (fin.equals(inicio)) {
            return ErrorValidacion.HORARIO_FIN_NO_POSTERIOR;
        }
        return null;
    }

    private static Instante calcularFinInstante(
            Fecha fechaInicio, Hora inicio, Hora fin, int desfaseMinutos) {
        Fecha fechaFin = fin.esPosteriorA(inicio) ? fechaInicio : fechaInicio.sumarDias(1);
        return Instante.desdeFechaYHoraLocal(fechaFin, fin, desfaseMinutos);
    }

    private static int calcularDuracionMinutos(Hora inicio, Hora fin) {
        if (fin.esPosteriorA(inicio)) {
            return inicio.minutosHasta(fin);
        }
        return (24 * 60 - inicio.minutosDesdeMedianoche()) + fin.minutosDesdeMedianoche();
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
                && planInicioUtcMilis == otra.planInicioUtcMilis
                && Objects.equals(planFinUtcMilis, otra.planFinUtcMilis)
                && desfaseMinutos == otra.desfaseMinutos
                && duracionPlanificadaMinutos == otra.duracionPlanificadaMinutos
                && Objects.equals(id, otra.id)
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
                planInicioUtcMilis,
                planFinUtcMilis,
                desfaseMinutos,
                duracionPlanificadaMinutos,
                duracionRealMinutos,
                estado.codigo(),
                notas,
                sesionOrigenId);
    }
}
