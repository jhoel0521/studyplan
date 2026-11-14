package io.github.jhoel0521.studyplan.adapter.out.persistence;

import io.github.jhoel0521.studyplan.domain.Ajustes;
import io.github.jhoel0521.studyplan.domain.Idioma;
import io.github.jhoel0521.studyplan.domain.Perfil;
import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.Tema;

/**
 * Convierte entre las filas de Room y los objetos del dominio, para que las {@code Entity} no salgan de este paquete.
 *
 * <p>Implementa: HU-01, HU-03 y HU-04. Si una fila guardada tuviera un valor desconocido (por ejemplo, de una versión
 * futura), se usa el valor por defecto en lugar de fallar.
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

    private static <E extends Enum<E>> E leer(Class<E> tipo, String texto, E porDefecto) {
        try {
            return Enum.valueOf(tipo, texto);
        } catch (IllegalArgumentException e) {
            return porDefecto;
        }
    }
}
