package io.github.jhoel0521.studyplan.adapter.out.persistence;

import io.github.jhoel0521.studyplan.domain.Ajustes;
import io.github.jhoel0521.studyplan.domain.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Idioma;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.Perfil;
import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.Tema;

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
        return e;
    }

    static Ajustes aDominio(AjustesEntity e) {
        Ajustes porDefecto = Ajustes.porDefecto();
        return new Ajustes(
                leer(Tema.class, e.tema, porDefecto.tema()),
                leer(Idioma.class, e.idioma, porDefecto.idioma()),
                e.bloqueoActivo);
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
        e.fecha = s.fecha().iso();
        e.horaInicio = s.horaInicio().texto();
        e.horaFin = s.horaFin().texto();
        e.duracionPlanificadaMinutos = s.duracionPlanificadaMinutos();
        e.duracionRealMinutos = s.duracionRealMinutos();
        e.estado = s.codigoEstado().name();
        e.notas = s.notas().isEmpty() ? null : s.notas();
        e.sesionOrigenId = s.sesionOrigenId();
        return e;
    }

    static SesionEstudio aDominio(SesionEntity e) {
        Resultado<Fecha> fecha = Fecha.parsear(e.fecha);
        Resultado<Hora> inicio = Hora.parsear(e.horaInicio);
        Resultado<Hora> fin = Hora.parsear(e.horaFin);
        if (!fecha.esValido() || !inicio.esValido() || !fin.esValido()) {
            return null;
        }
        CodigoEstadoSesion codigo = leer(
                CodigoEstadoSesion.class, e.estado, CodigoEstadoSesion.NO_REALIZADA);
        return SesionEstudio.restaurar(
                e.id,
                e.materiaId,
                fecha.valor(),
                inicio.valor(),
                fin.valor(),
                e.duracionPlanificadaMinutos,
                e.duracionRealMinutos,
                codigo,
                e.notas,
                e.sesionOrigenId);
    }

    private static <E extends Enum<E>> E leer(Class<E> tipo, String texto, E porDefecto) {
        try {
            return Enum.valueOf(tipo, texto);
        } catch (IllegalArgumentException e) {
            return porDefecto;
        }
    }
}
