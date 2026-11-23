package io.github.jhoel0521.studyplan.domain.service;

import io.github.jhoel0521.studyplan.domain.readmodel.InstantaneaRespaldo;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Convierte una {@link InstantaneaRespaldo} a los bytes JSON (UTF-8) del archivo de respaldo y de vuelta. Es Java puro:
 * escribe y lee el JSON a mano para que el núcleo no dependa de {@code org.json} (parte de Android) y se pruebe en la
 * JVM.
 *
 * <p>Formato: {@code {"formato":"STUDYPLAN","versionFormato":1,"versionEsquema":8,"fechaExportacionUtcMilis":...,
 * "tablas":{"perfil":[{...}],...}}}.
 *
 * <p>Implementa: HU-35 y HU-37.
 */
public final class SerializadorRespaldo {

    private static final String MARCA = "STUDYPLAN";

    private SerializadorRespaldo() {
    }

    /** Escribe la instantánea como JSON UTF-8. */
    public static byte[] serializar(InstantaneaRespaldo instantanea) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("{\"formato\":");
        escribirCadena(sb, MARCA);
        sb.append(",\"versionFormato\":").append(instantanea.versionFormato());
        sb.append(",\"versionEsquema\":").append(instantanea.versionEsquema());
        sb.append(",\"fechaExportacionUtcMilis\":").append(instantanea.fechaExportacionUtcMilis());
        sb.append(",\"tablas\":{");
        boolean primeraTabla = true;
        for (Map.Entry<String, List<Map<String, Object>>> tabla : instantanea.tablas().entrySet()) {
            if (!primeraTabla) {
                sb.append(',');
            }
            primeraTabla = false;
            escribirCadena(sb, tabla.getKey());
            sb.append(":[");
            boolean primeraFila = true;
            for (Map<String, Object> fila : tabla.getValue()) {
                if (!primeraFila) {
                    sb.append(',');
                }
                primeraFila = false;
                sb.append('{');
                boolean primeraColumna = true;
                for (Map.Entry<String, Object> columna : fila.entrySet()) {
                    if (!primeraColumna) {
                        sb.append(',');
                    }
                    primeraColumna = false;
                    escribirCadena(sb, columna.getKey());
                    sb.append(':');
                    escribirValor(sb, columna.getValue());
                }
                sb.append('}');
            }
            sb.append(']');
        }
        sb.append("}}");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Lee un respaldo.
     *
     * @throws IllegalArgumentException si los bytes no son un respaldo válido de StudyPlan
     */
    public static InstantaneaRespaldo deserializar(byte[] datos) {
        Object raiz;
        try {
            raiz = new Lector(new String(datos, StandardCharsets.UTF_8)).leerDocumento();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("JSON inválido", e);
        }
        if (!(raiz instanceof Map)) {
            throw new IllegalArgumentException("La raíz no es un objeto");
        }
        Map<?, ?> documento = (Map<?, ?>) raiz;
        if (!MARCA.equals(documento.get("formato"))) {
            throw new IllegalArgumentException("No es un respaldo de StudyPlan");
        }
        int versionFormato = entero(documento.get("versionFormato"));
        int versionEsquema = entero(documento.get("versionEsquema"));
        long fecha = largo(documento.get("fechaExportacionUtcMilis"));
        Object tablasCrudas = documento.get("tablas");
        if (!(tablasCrudas instanceof Map)) {
            throw new IllegalArgumentException("Faltan las tablas");
        }
        Map<String, List<Map<String, Object>>> tablas = new LinkedHashMap<>();
        for (Map.Entry<?, ?> tabla : ((Map<?, ?>) tablasCrudas).entrySet()) {
            if (!(tabla.getValue() instanceof List)) {
                throw new IllegalArgumentException("La tabla " + tabla.getKey() + " no es una lista");
            }
            List<Map<String, Object>> filas = new ArrayList<>();
            for (Object fila : (List<?>) tabla.getValue()) {
                if (!(fila instanceof Map)) {
                    throw new IllegalArgumentException("Una fila no es un objeto");
                }
                Map<String, Object> columnas = new LinkedHashMap<>();
                for (Map.Entry<?, ?> columna : ((Map<?, ?>) fila).entrySet()) {
                    columnas.put((String) columna.getKey(), columna.getValue());
                }
                filas.add(columnas);
            }
            tablas.put((String) tabla.getKey(), filas);
        }
        return new InstantaneaRespaldo(versionFormato, versionEsquema, fecha, tablas);
    }

    private static int entero(Object valor) {
        return (int) largo(valor);
    }

    private static long largo(Object valor) {
        if (valor instanceof Long) {
            return (Long) valor;
        }
        throw new IllegalArgumentException("Se esperaba un número entero");
    }

    private static void escribirValor(StringBuilder sb, Object valor) {
        if (valor == null) {
            sb.append("null");
        } else if (valor instanceof Boolean) {
            sb.append(((Boolean) valor) ? "true" : "false");
        } else if (valor instanceof Double || valor instanceof Float) {
            double d = ((Number) valor).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new IllegalArgumentException("Número no finito");
            }
            sb.append(d);
        } else if (valor instanceof Number) {
            sb.append(((Number) valor).longValue());
        } else {
            escribirCadena(sb, valor.toString());
        }
    }

    private static void escribirCadena(StringBuilder sb, String texto) {
        sb.append('"');
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    /** Lector JSON mínimo: objetos, listas, cadenas, números, booleanos y null. */
    private static final class Lector {
        private final String texto;
        private int pos;

        Lector(String texto) {
            this.texto = texto;
        }

        Object leerDocumento() {
            Object valor = leerValor();
            saltarEspacios();
            if (pos != texto.length()) {
                throw new IllegalArgumentException("Texto sobrante");
            }
            return valor;
        }

        private Object leerValor() {
            saltarEspacios();
            char c = actual();
            if (c == '{') {
                return leerObjeto();
            }
            if (c == '[') {
                return leerLista();
            }
            if (c == '"') {
                return leerCadena();
            }
            if (texto.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (texto.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            if (texto.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            return leerNumero();
        }

        private Map<String, Object> leerObjeto() {
            Map<String, Object> mapa = new LinkedHashMap<>();
            pos++;
            saltarEspacios();
            if (actual() == '}') {
                pos++;
                return mapa;
            }
            while (true) {
                saltarEspacios();
                String clave = leerCadena();
                saltarEspacios();
                esperar(':');
                mapa.put(clave, leerValor());
                saltarEspacios();
                if (actual() == ',') {
                    pos++;
                } else {
                    esperar('}');
                    return mapa;
                }
            }
        }

        private List<Object> leerLista() {
            List<Object> lista = new ArrayList<>();
            pos++;
            saltarEspacios();
            if (actual() == ']') {
                pos++;
                return lista;
            }
            while (true) {
                lista.add(leerValor());
                saltarEspacios();
                if (actual() == ',') {
                    pos++;
                } else {
                    esperar(']');
                    return lista;
                }
            }
        }

        private String leerCadena() {
            esperar('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = texto.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                char e = texto.charAt(pos++);
                switch (e) {
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'u':
                        sb.append((char) Integer.parseInt(texto.substring(pos, pos + 4), 16));
                        pos += 4;
                        break;
                    case '"':
                    case '\\':
                    case '/':
                        sb.append(e);
                        break;
                    default:
                        throw new IllegalArgumentException("Escape inválido");
                }
            }
        }

        private Object leerNumero() {
            int inicio = pos;
            boolean decimal = false;
            while (pos < texto.length()) {
                char c = texto.charAt(pos);
                if (c == '.' || c == 'e' || c == 'E') {
                    decimal = true;
                } else if (!(c == '-' || c == '+' || (c >= '0' && c <= '9'))) {
                    break;
                }
                pos++;
            }
            if (inicio == pos) {
                throw new IllegalArgumentException("Valor inesperado");
            }
            String numero = texto.substring(inicio, pos);
            return decimal ? (Object) Double.valueOf(numero) : (Object) Long.valueOf(numero);
        }

        private void saltarEspacios() {
            while (pos < texto.length() && Character.isWhitespace(texto.charAt(pos))) {
                pos++;
            }
        }

        private char actual() {
            if (pos >= texto.length()) {
                throw new IllegalArgumentException("Fin inesperado");
            }
            return texto.charAt(pos);
        }

        private void esperar(char c) {
            if (actual() != c) {
                throw new IllegalArgumentException("Se esperaba '" + c + "'");
            }
            pos++;
        }
    }
}
