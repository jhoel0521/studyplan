package io.github.jhoel0521.studyplan.domain.entity;

import java.util.Objects;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Perfil local del estudiante: su nombre y la meta semanal predeterminada. Es único en el teléfono (RN-01).
 *
 * <p>Implementa: HU-01 (Configurar perfil inicial), RN-01, RN-14 (la meta debe ser mayor que 0 y hasta
 * 168 horas), RN-15 (esta meta se aplica cuando la semana no define otra) y RNF-14.
 */
public final class Perfil {

    /** Máxima meta semanal posible: las horas que tiene una semana (RN-14). */
    public static final double META_MAXIMA_HORAS = 168;

    private final String nombre;
    private final double metaSemanalHoras;

    private Perfil(String nombre, double metaSemanalHoras) {
        this.nombre = nombre;
        this.metaSemanalHoras = metaSemanalHoras;
    }

    /**
     * Crea un perfil validando sus datos. Si hay más de un error se informa primero el del nombre.
     *
     * @param nombre nombre del estudiante; se recortan los espacios de los extremos
     * @param metaSemanalHoras meta predeterminada, mayor que 0 y menor o igual a {@link #META_MAXIMA_HORAS}
     */
    public static Resultado<Perfil> crear(String nombre, double metaSemanalHoras) {
        String limpio = nombre == null ? "" : nombre.trim();
        if (limpio.isEmpty()) {
            return Resultado.error(ErrorValidacion.NOMBRE_VACIO);
        }
        if (Double.isNaN(metaSemanalHoras) || metaSemanalHoras <= 0 || metaSemanalHoras > META_MAXIMA_HORAS) {
            return Resultado.error(ErrorValidacion.META_FUERA_DE_RANGO);
        }
        return Resultado.ok(new Perfil(limpio, metaSemanalHoras));
    }

    public String nombre() {
        return nombre;
    }

    public double metaSemanalHoras() {
        return metaSemanalHoras;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Perfil)) {
            return false;
        }
        Perfil p = (Perfil) otro;
        return nombre.equals(p.nombre) && Double.compare(metaSemanalHoras, p.metaSemanalHoras) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre, metaSemanalHoras);
    }

    @Override
    @SuppressWarnings("NullableProblems") // el SDK de Android anota Object.toString y el núcleo no importa androidx
    public String toString() {
        return "Perfil{nombre='" + nombre + "', metaSemanalHoras=" + metaSemanalHoras + '}';
    }
}
