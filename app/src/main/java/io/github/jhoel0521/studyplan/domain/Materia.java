package io.github.jhoel0521.studyplan.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

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

    /** Espacios de cualquier tipo (también no separables, de ancho fijo y de ancho cero) que se colapsan en uno. */
    private static final Pattern ESPACIOS = Pattern.compile("[\\s\\p{Z}\\u200B\\uFEFF]+");
    private static final Pattern MARCAS_DE_ACENTO = Pattern.compile("\\p{M}+");

    private final Long id;
    private final String nombre;
    private final String clave;
    private final int color;
    private final String docente;
    private final boolean archivada;

    private Materia(Long id, String nombre, int color, String docente, boolean archivada) {
        this.id = id;
        this.nombre = nombre;
        this.clave = claveDe(nombre);
        this.color = color;
        this.docente = docente;
        this.archivada = archivada;
    }

    /**
     * Crea una materia nueva y activa validando sus datos.
     *
     * @param nombre nombre escrito por el estudiante (se sanea)
     * @param color color como entero ARGB
     * @param docente docente opcional; un texto vacío equivale a no tener docente
     */
    public static Resultado<Materia> crear(String nombre, int color, String docente) {
        Resultado<String> saneado = sanearNombre(nombre);
        if (!saneado.esValido()) {
            return Resultado.error(saneado.error());
        }
        return Resultado.ok(new Materia(null, saneado.valor(), color, sanearDocente(docente), false));
    }

    /**
     * Reconstruye una materia guardada. Lo usa el adaptador de persistencia: los datos ya fueron validados al
     * guardarse, así que aquí solo se vuelven a sanear por seguridad.
     */
    public static Materia restaurar(long id, String nombre, int color, String docente, boolean archivada) {
        String limpio = normalizarNombre(nombre);
        return new Materia(id, limpio, color, sanearDocente(docente), archivada);
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

    /**
     * Cambia el nombre, el color y el docente conservando el id y el estado de archivada (HU-07).
     *
     * @return la materia editada o el error de validación del nombre
     */
    public Resultado<Materia> editar(String nuevoNombre, int nuevoColor, String nuevoDocente) {
        Resultado<String> saneado = sanearNombre(nuevoNombre);
        if (!saneado.esValido()) {
            return Resultado.error(saneado.error());
        }
        return Resultado.ok(new Materia(id, saneado.valor(), nuevoColor, sanearDocente(nuevoDocente), archivada));
    }

    public Materia conArchivada(boolean nuevoEstado) {
        return new Materia(id, nombre, color, docente, nuevoEstado);
    }

    /** @return la misma materia con el id que le asignó el almacén */
    public Materia conId(long nuevoId) {
        return new Materia(nuevoId, nombre, color, docente, archivada);
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

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Materia)) {
            return false;
        }
        Materia m = (Materia) otro;
        return Objects.equals(id, m.id) && nombre.equals(m.nombre) && color == m.color
                && Objects.equals(docente, m.docente) && archivada == m.archivada;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nombre, color, docente, archivada);
    }

    @Override
    @SuppressWarnings("NullableProblems") // el SDK de Android anota Object.toString y el núcleo no importa androidx
    public String toString() {
        return "Materia{id=" + id + ", nombre='" + nombre + "', archivada=" + archivada + '}';
    }
}
