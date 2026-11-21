package io.github.jhoel0521.studyplan.domain.entity;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Materia (asignatura) que agrupa las sesiones de estudio. Es inmutable: cada cambio devuelve un objeto nuevo.
 *
 * <p>Implementa: HU-06 (registrar), HU-07 (editar), HU-08 (archivar), HU-46 (desarchivar), RN-02 (nombre de 1 a 50
 * caracteres, único entre las activas sin distinguir mayúsculas) y RNF-14 (validación de entradas).
 *
 * <p><b>Saneamiento del nombre (decisión del Product Owner que amplía RN-02):</b> el nombre guardado se recorta y
 * colapsa los espacios repetidos, conservando mayúsculas y acentos para mostrarlo; la {@link #clave() clave} le quita
 * los acentos y lo pasa a mayúsculas, y es la que decide si dos nombres son el mismo.
 */
public final class Materia {

    /** Largo máximo del nombre ya saneado (RN-02). */
    public static final int NOMBRE_MAXIMO = 50;
    /** Nombre base de la materia general incluida desde la instalación (RN-32). */
    public static final String NOMBRE_GENERAL = "Estudios varios";

    /** Espacios de cualquier tipo (también no separables, de ancho fijo y de ancho cero) que se colapsan en uno. */
    private static final Pattern ESPACIOS = Pattern.compile("[\\s\\p{Z}\\u200B\\uFEFF]+");
    private static final Pattern MARCAS_DE_ACENTO = Pattern.compile("\\p{M}+");
    /** Clave reservada de la materia general (RN-32). */
    public static final String CLAVE_GENERAL = claveDe(NOMBRE_GENERAL);

    private final Long id;
    private final String nombre;
    private final String clave;
    private final int color;
    private final String docente;
    private final boolean archivada;
    private final Double metaSemanalHoras;
    private final boolean esGeneral;

    private Materia(
            Long id,
            String nombre,
            int color,
            String docente,
            boolean archivada,
            Double metaSemanalHoras,
            boolean esGeneral) {
        this.id = id;
        this.nombre = nombre;
        this.clave = claveDe(nombre);
        this.color = color;
        this.docente = docente;
        this.archivada = archivada;
        this.metaSemanalHoras = metaSemanalHoras;
        this.esGeneral = esGeneral;
    }

    /**
     * Crea una materia nueva y activa validando sus datos.
     */
    public static Resultado<Materia> crear(String nombre, int color, String docente) {
        return crear(nombre, color, docente, null);
    }

    /**
     * Crea una materia nueva con meta semanal opcional validada según {@code RN-14} (HU-06, HU-26).
     */
    public static Resultado<Materia> crear(
            String nombre, int color, String docente, Double metaSemanalHoras) {
        Resultado<String> saneado = sanearNombre(nombre);
        if (!saneado.esValido()) {
            return Resultado.error(saneado.error());
        }
        ErrorValidacion errorMeta = validarMeta(metaSemanalHoras);
        if (errorMeta != null) {
            return Resultado.error(errorMeta);
        }
        return Resultado.ok(new Materia(
                null, saneado.valor(), color, sanearDocente(docente), false, metaSemanalHoras, false));
    }

    /** Crea la materia general «Estudios varios» (RN-32). */
    public static Materia crearGeneral(int color) {
        return new Materia(null, NOMBRE_GENERAL, color, null, false, null, true);
    }

    /**
     * Reconstruye una materia guardada en versiones Room anteriores a v6.
     */
    public static Materia restaurar(long id, String nombre, int color, String docente, boolean archivada) {
        return restaurar(id, nombre, color, docente, archivada, null, false);
    }

    /**
     * Reconstruye una materia guardada en Room v6 con meta semanal opcional y marca general.
     */
    public static Materia restaurar(
            long id,
            String nombre,
            int color,
            String docente,
            boolean archivada,
            Double metaSemanalHoras,
            boolean esGeneral) {
        String limpio = normalizarNombre(nombre);
        return new Materia(id, limpio, color, sanearDocente(docente), archivada, metaSemanalHoras, esGeneral);
    }

    /** Normaliza un nombre: recorta los extremos y colapsa los espacios repetidos. */
    public static String normalizarNombre(String nombre) {
        return ESPACIOS.matcher(nombre == null ? "" : nombre).replaceAll(" ").trim();
    }

    /**
     * @return la clave de unicidad de un nombre: sin acentos y en mayúsculas. La ñ se conserva porque en español es otra
     *         letra («Año» y «Ano» son palabras distintas).
     */
    public static String claveDe(String nombre) {
        String descompuesto = Normalizer.normalize(normalizarNombre(nombre), Normalizer.Form.NFD)
                .replace("n\u0303", "ñ").replace("N\u0303", "Ñ");
        String sinAcentos = MARCAS_DE_ACENTO.matcher(descompuesto).replaceAll("");
        return sinAcentos.toUpperCase(Locale.ROOT);
    }

    private static Resultado<String> sanearNombre(String nombre) {
        String limpio = normalizarNombre(nombre);
        if (limpio.isEmpty()) {
            return Resultado.error(ErrorValidacion.NOMBRE_VACIO);
        }
        if (limpio.length() > NOMBRE_MAXIMO) {
            return Resultado.error(ErrorValidacion.NOMBRE_MUY_LARGO);
        }
        return Resultado.ok(limpio);
    }

    private static String sanearDocente(String docente) {
        String limpio = docente == null ? "" : docente.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private static ErrorValidacion validarMeta(Double metaSemanalHoras) {
        if (metaSemanalHoras == null) {
            return null;
        }
        if (!Double.isFinite(metaSemanalHoras)
                || metaSemanalHoras <= 0
                || metaSemanalHoras > Perfil.META_MAXIMA_HORAS) {
            return ErrorValidacion.META_FUERA_DE_RANGO;
        }
        return null;
    }

    /**
     * Cambia el nombre, el color y el docente conservando el id, el estado de archivada y la meta (HU-07).
     */
    public Resultado<Materia> editar(String nuevoNombre, int nuevoColor, String nuevoDocente) {
        return editar(nuevoNombre, nuevoColor, nuevoDocente, metaSemanalHoras);
    }

    /**
     * Cambia el nombre, el color, el docente y la meta semanal opcional (HU-07, HU-26, RN-14, RN-32).
     */
    public Resultado<Materia> editar(
            String nuevoNombre, int nuevoColor, String nuevoDocente, Double nuevaMetaSemanalHoras) {
        if (esGeneral) {
            return Resultado.error(ErrorValidacion.MATERIA_GENERAL_PROTEGIDA);
        }
        Resultado<String> saneado = sanearNombre(nuevoNombre);
        if (!saneado.esValido()) {
            return Resultado.error(saneado.error());
        }
        ErrorValidacion errorMeta = validarMeta(nuevaMetaSemanalHoras);
        if (errorMeta != null) {
            return Resultado.error(errorMeta);
        }
        return Resultado.ok(new Materia(
                id,
                saneado.valor(),
                nuevoColor,
                sanearDocente(nuevoDocente),
                archivada,
                nuevaMetaSemanalHoras,
                false));
    }

    /** Archiva o desarchiva verificando que no sea la materia general (RN-32). */
    public Resultado<Materia> intentarArchivar(boolean nuevoEstado) {
        if (esGeneral) {
            return Resultado.error(ErrorValidacion.MATERIA_GENERAL_PROTEGIDA);
        }
        return Resultado.ok(conArchivada(nuevoEstado));
    }

    /** Valida si la materia puede eliminarse (RN-32: la materia general es protegida). */
    public Resultado<Materia> validarEliminacion() {
        if (esGeneral) {
            return Resultado.error(ErrorValidacion.MATERIA_GENERAL_PROTEGIDA);
        }
        return Resultado.ok(this);
    }

    public Materia conArchivada(boolean nuevoEstado) {
        return new Materia(id, nombre, color, docente, nuevoEstado, metaSemanalHoras, esGeneral);
    }

    /** @return la misma materia con el id que le asignó el almacén */
    public Materia conId(long nuevoId) {
        return new Materia(nuevoId, nombre, color, docente, archivada, metaSemanalHoras, esGeneral);
    }

    /** @return el id, o {@code null} si la materia todavía no se guardó */
    public Long id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    /** @return la clave de unicidad: el nombre sin acentos y en mayúsculas */
    public String clave() {
        return clave;
    }

    public int color() {
        return color;
    }

    /** @return el docente, o {@code null} si no tiene */
    public String docente() {
        return docente;
    }

    public boolean archivada() {
        return archivada;
    }

    /** Meta semanal opcional en horas (nula si la materia no tiene meta propia). */
    public Double metaSemanalHoras() {
        return metaSemanalHoras;
    }

    /** Indica si es la materia general «Estudios varios» protegida por {@code RN-32}. */
    public boolean esGeneral() {
        return esGeneral;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Materia)) {
            return false;
        }
        Materia m = (Materia) otro;
        return Objects.equals(id, m.id)
                && nombre.equals(m.nombre)
                && color == m.color
                && Objects.equals(docente, m.docente)
                && archivada == m.archivada
                && Objects.equals(metaSemanalHoras, m.metaSemanalHoras)
                && esGeneral == m.esGeneral;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nombre, color, docente, archivada, metaSemanalHoras, esGeneral);
    }

    @Override
    @SuppressWarnings("NullableProblems") // el SDK de Android anota Object.toString y el núcleo no importa androidx
    public String toString() {
        return "Materia{id=" + id + ", nombre='" + nombre + "', archivada=" + archivada + '}';
    }
}
