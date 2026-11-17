package io.github.jhoel0521.studyplan.adapter.in.ui;

import java.util.Objects;

import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.ProgresoMeta;
import io.github.jhoel0521.studyplan.domain.ResumenMateria;

/**
 * Una fila de la lista de materias: una materia activa, el encabezado plegable de las archivadas o una archivada.
 *
 * <p>Implementa: HU-09 (lista con horas), HU-08 (sección «Archivadas») y HU-26/HU-27 (meta semanal).
 */
public final class FilaMateria {

    /** Tipos de fila. */
    public enum Tipo { ACTIVA, ENCABEZADO_ARCHIVADAS, ARCHIVADA }

    private final Tipo tipo;
    private final Materia materia;
    private final double horasRealizadas;
    private final ProgresoMeta metaSemanaActual;
    private final int cantidadArchivadas;
    private final boolean expandida;

    private FilaMateria(
            Tipo tipo,
            Materia materia,
            double horasRealizadas,
            ProgresoMeta metaSemanaActual,
            int cantidadArchivadas,
            boolean expandida) {
        this.tipo = tipo;
        this.materia = materia;
        this.horasRealizadas = horasRealizadas;
        this.metaSemanaActual = metaSemanaActual;
        this.cantidadArchivadas = cantidadArchivadas;
        this.expandida = expandida;
    }

    public static FilaMateria activa(ResumenMateria resumen) {
        return new FilaMateria(
                Tipo.ACTIVA,
                resumen.materia(),
                resumen.horasRealizadas(),
                resumen.metaSemanaActual(),
                0,
                false);
    }

    public static FilaMateria encabezadoArchivadas(int cantidad, boolean expandida) {
        return new FilaMateria(Tipo.ENCABEZADO_ARCHIVADAS, null, 0, null, cantidad, expandida);
    }

    public static FilaMateria archivada(Materia materia) {
        return new FilaMateria(Tipo.ARCHIVADA, materia, 0, null, 0, false);
    }

    public Tipo tipo() {
        return tipo;
    }

    /** @return la materia, o {@code null} en el encabezado */
    public Materia materia() {
        return materia;
    }

    public double horasRealizadas() {
        return horasRealizadas;
    }

    public ProgresoMeta metaSemanaActual() {
        return metaSemanaActual;
    }

    public int cantidadArchivadas() {
        return cantidadArchivadas;
    }

    public boolean expandida() {
        return expandida;
    }

    /** @return identificador estable para la lista: el id de la materia o -1 para el encabezado */
    public long idEstable() {
        return materia == null || materia.id() == null ? -1 : materia.id();
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof FilaMateria)) {
            return false;
        }
        FilaMateria f = (FilaMateria) otro;
        return tipo == f.tipo && Objects.equals(materia, f.materia)
                && Double.compare(horasRealizadas, f.horasRealizadas) == 0
                && Objects.equals(
                        metaSemanaActual == null ? null : metaSemanaActual.horasRealizadas(),
                        f.metaSemanaActual == null ? null : f.metaSemanaActual.horasRealizadas())
                && Objects.equals(
                        metaSemanaActual == null ? null : metaSemanaActual.horasObjetivo(),
                        f.metaSemanaActual == null ? null : f.metaSemanaActual.horasObjetivo())
                && cantidadArchivadas == f.cantidadArchivadas && expandida == f.expandida;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tipo, materia, horasRealizadas, cantidadArchivadas, expandida);
    }
}
