package io.github.jhoel0521.studyplan.adapter.out.persistence;

import io.github.jhoel0521.studyplan.domain.Ajustes;
import io.github.jhoel0521.studyplan.domain.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.ConfiguracionPomodoro;
import io.github.jhoel0521.studyplan.domain.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Idioma;
import io.github.jhoel0521.studyplan.domain.Instante;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.Perfil;
import io.github.jhoel0521.studyplan.domain.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.TecnicaEstudio;
import io.github.jhoel0521.studyplan.domain.Tema;
import io.github.jhoel0521.studyplan.domain.TipoEventoEstudio;

/**
 * Convierte entre las filas de Room y los objetos del dominio, para que las {@code Entity} no salgan de este paquete.
 *
 * <p>Implementa: HU-01, HU-03, HU-04, HU-06 a HU-15. Si una fila guardada tuviera un valor desconocido (por ejemplo,
 * de una versión futura), se usa el valor por defecto en lugar de fallar.
 */
final class Mapeadores {

    private Mapeadores() {
    }

    static PerfilEntity aEntidad(Perfil perfil) {
        PerfilEntity e = new PerfilEntity();
        e.nombre = perfil.nombre();
        e.metaSemanalHoras = perfil.metaSemanalHoras();
        return e;
    }

    /** @return el perfil de la fila, o {@code null} si la fila guardada ya no cumple las reglas del dominio */
    static Perfil aDominio(PerfilEntity e) {
        Resultado<Perfil> r = Perfil.crear(e.nombre, e.metaSemanalHoras);
        return r.esValido() ? r.valor() : null;
    }

    static AjustesEntity aEntidad(Ajustes ajustes) {
        AjustesEntity e = new AjustesEntity();
        e.tema = ajustes.tema().name();
        e.idioma = ajustes.idioma().name();
        e.bloqueoActivo = ajustes.bloqueoActivo();
        e.noMolestarGlobal = ajustes.noMolestarGlobal();
        e.pomodoroTrabajoMin = ajustes.pomodoro().trabajoMinutos();
        e.pomodoroDescansoCortoMin = ajustes.pomodoro().descansoCortoMinutos();
        e.pomodoroDescansoLargoMin = ajustes.pomodoro().descansoLargoMinutos();
        e.pomodoroCiclosHastaLargo = ajustes.pomodoro().ciclosHastaLargo();
        return e;
    }

    static Ajustes aDominio(AjustesEntity e) {
        Ajustes porDefecto = Ajustes.porDefecto();
        ConfiguracionPomodoro pomodoro = ConfiguracionPomodoro.restaurar(
                e.pomodoroTrabajoMin,
                e.pomodoroDescansoCortoMin,
                e.pomodoroDescansoLargoMin,
                e.pomodoroCiclosHastaLargo);
        return new Ajustes(
                leer(Tema.class, e.tema, porDefecto.tema()),
                leer(Idioma.class, e.idioma, porDefecto.idioma()),
                e.bloqueoActivo,
                e.noMolestarGlobal,
                pomodoro);
    }

    static MateriaEntity aEntidad(Materia materia) {
        MateriaEntity e = new MateriaEntity();
        e.id = materia.id() == null ? 0 : materia.id();
        e.nombre = materia.nombre();
        e.nombreClave = materia.clave();
        e.color = materia.color();
        e.docente = materia.docente();
        e.archivada = materia.archivada();
        return e;
    }

    static Materia aDominio(MateriaEntity e) {
        return Materia.restaurar(e.id, e.nombre, e.color, e.docente, e.archivada);
    }

    static SesionEntity aEntidad(SesionEstudio s) {
        SesionEntity e = new SesionEntity();
        e.id = s.id() == null ? 0 : s.id();
        e.materiaId = s.materiaId();
        e.planInicioUtcMilis = s.planInicioUtcMilis();
        e.planFinUtcMilis = s.planFinUtcMilis();
        e.desfaseMinutos = s.desfaseMinutos();
        e.duracionPlanificadaMinutos = s.duracionPlanificadaMinutos();
        e.duracionRealMinutos = s.duracionRealMinutos();
        e.estado = s.codigoEstado().name();
        e.notas = s.notas().isEmpty() ? null : s.notas();
        e.sesionOrigenId = s.sesionOrigenId();
        e.tecnica = s.tecnica().name();
        e.forzarNoMolestar = s.preferenciaNoMolestar().aBoolean();
        e.pomodoroTrabajoMin = s.pomodoroTrabajoMinutos();
        e.pomodoroDescansoCortoMin = s.pomodoroDescansoCortoMinutos();
        e.pomodoroDescansoLargoMin = s.pomodoroDescansoLargoMinutos();
        e.pomodoroCiclosHastaLargo = s.pomodoroCiclosHastaLargo();
        return e;
    }

    static SesionEstudio aDominio(SesionEntity e) {
        if (e.desfaseMinutos < Instante.DESFASE_MINIMO_MINUTOS
                || e.desfaseMinutos > Instante.DESFASE_MAXIMO_MINUTOS
                || e.planFinUtcMilis <= e.planInicioUtcMilis) {
            return null;
        }
        CodigoEstadoSesion codigo = leer(
                CodigoEstadoSesion.class, e.estado, CodigoEstadoSesion.NO_REALIZADA);
        TecnicaEstudio tecnica = leer(TecnicaEstudio.class, e.tecnica, TecnicaEstudio.LIBRE);
        PreferenciaNoMolestar pref = PreferenciaNoMolestar.desdeBoolean(e.forzarNoMolestar);
        return SesionEstudio.restaurar(
                e.id,
                e.materiaId,
                e.planInicioUtcMilis,
                e.planFinUtcMilis,
                e.desfaseMinutos,
                e.duracionPlanificadaMinutos,
                e.duracionRealMinutos,
                codigo,
                e.notas,
                e.sesionOrigenId,
                tecnica,
                pref,
                e.pomodoroTrabajoMin,
                e.pomodoroDescansoCortoMin,
                e.pomodoroDescansoLargoMin,
                e.pomodoroCiclosHastaLargo);
    }

    static EventoEstudioEntity aEntidad(EventoEstudio ev) {
        EventoEstudioEntity e = new EventoEstudioEntity();
        e.id = ev.id() == null ? 0 : ev.id();
        e.sesionId = ev.sesionId();
        e.tipo = ev.tipo().name();
        e.instanteMilis = ev.instanteMilis();
        e.desfaseMinutos = ev.desfaseMinutos();
        e.nota = ev.nota().isEmpty() ? null : ev.nota();
        return e;
    }

    static EventoEstudio aDominio(EventoEstudioEntity e) {
        TipoEventoEstudio tipo = leer(TipoEventoEstudio.class, e.tipo, TipoEventoEstudio.AJUSTE_MANUAL);
        return EventoEstudio.restaurar(
                e.id, e.sesionId, tipo, e.instanteMilis, e.desfaseMinutos, e.nota);
    }

    private static <E extends Enum<E>> E leer(Class<E> tipo, String texto, E porDefecto) {
        try {
            return Enum.valueOf(tipo, texto);
        } catch (IllegalArgumentException e) {
            return porDefecto;
        }
    }
}
