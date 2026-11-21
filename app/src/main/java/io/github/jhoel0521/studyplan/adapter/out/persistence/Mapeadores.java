package io.github.jhoel0521.studyplan.adapter.out.persistence;

import io.github.jhoel0521.studyplan.domain.entity.Ajustes;
import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.valueobject.ConfiguracionPomodoro;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.enumeration.Idioma;
import io.github.jhoel0521.studyplan.domain.valueobject.Instante;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.entity.MetaSemanal;
import io.github.jhoel0521.studyplan.domain.entity.Perfil;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.Tema;
import io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TopicoEventoEstudio;

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
        e.recordatorioMinutos = ajustes.recordatorioPorDefecto().minutos();
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
                pomodoro,
                AntelacionRecordatorio.deMinutos(
                        e.recordatorioMinutos, porDefecto.recordatorioPorDefecto()));
    }

    static MateriaEntity aEntidad(Materia materia) {
        MateriaEntity e = new MateriaEntity();
        e.id = materia.id() == null ? 0 : materia.id();
        e.nombre = materia.nombre();
        e.nombreClave = materia.clave();
        e.color = materia.color();
        e.docente = materia.docente();
        e.archivada = materia.archivada();
        e.metaSemanalHoras = materia.metaSemanalHoras();
        e.esGeneral = materia.esGeneral();
        return e;
    }

    static Materia aDominio(MateriaEntity e) {
        return Materia.restaurar(
                e.id, e.nombre, e.color, e.docente, e.archivada, e.metaSemanalHoras, e.esGeneral);
    }

    static MetaSemanalEntity aEntidad(MetaSemanal meta) {
        MetaSemanalEntity e = new MetaSemanalEntity();
        e.id = meta.id() == null ? 0 : meta.id();
        e.semanaInicio = meta.semanaInicio().iso();
        e.materiaId = meta.materiaId();
        e.horasObjetivo = meta.horasObjetivo();
        e.felicitada = meta.felicitada();
        return e;
    }

    static MetaSemanal aDominio(MetaSemanalEntity e) {
        Resultado<Fecha> fecha = Fecha.parsear(e.semanaInicio);
        if (!fecha.esValido()) {
            return null;
        }
        return MetaSemanal.restaurar(
                e.id, fecha.valor(), e.materiaId, e.horasObjetivo, e.felicitada);
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
        e.recordatorioMinutos = s.antelacionRecordatorio().aMinutosNullable();
        return e;
    }

    static SesionEstudio aDominio(SesionEntity e) {
        if (e.desfaseMinutos < Instante.DESFASE_MINIMO_MINUTOS
                || e.desfaseMinutos > Instante.DESFASE_MAXIMO_MINUTOS
                || (e.planFinUtcMilis != null && e.planFinUtcMilis <= e.planInicioUtcMilis)) {
            return null;
        }
        CodigoEstadoSesion codigo = leer(
                CodigoEstadoSesion.class, e.estado, CodigoEstadoSesion.NO_REALIZADA);
        TecnicaEstudio tecnica = leer(TecnicaEstudio.class, e.tecnica, TecnicaEstudio.LIBRE);
        PreferenciaNoMolestar pref = PreferenciaNoMolestar.desdeBoolean(e.forzarNoMolestar);
        AntelacionRecordatorio antelacion = AntelacionRecordatorio.deMinutos(
                e.recordatorioMinutos, AntelacionRecordatorio.SIN_RECORDATORIO);
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
                e.pomodoroCiclosHastaLargo,
                antelacion);
    }

    static EventoEstudioEntity aEntidad(EventoEstudio ev) {
        EventoEstudioEntity e = new EventoEstudioEntity();
        e.id = ev.id() == null ? 0 : ev.id();
        e.sesionId = ev.sesionId();
        e.tipo = ev.tipo().name();
        e.instanteMilis = ev.instanteMilis();
        e.desfaseMinutos = ev.desfaseMinutos();
        e.nota = ev.nota().isEmpty() ? null : ev.nota();
        e.topico = ev.topico().map(TopicoEventoEstudio::clave).orElse(null);
        return e;
    }

    static EventoEstudio aDominio(EventoEstudioEntity e) {
        TipoEventoEstudio tipo = leer(TipoEventoEstudio.class, e.tipo, TipoEventoEstudio.AJUSTE_MANUAL);
        return EventoEstudio.restaurar(
                e.id, e.sesionId, tipo, e.instanteMilis, e.desfaseMinutos, e.nota, e.topico);
    }

    private static <E extends Enum<E>> E leer(Class<E> tipo, String texto, E porDefecto) {
        try {
            return Enum.valueOf(tipo, texto);
        } catch (IllegalArgumentException e) {
            return porDefecto;
        }
    }
}
