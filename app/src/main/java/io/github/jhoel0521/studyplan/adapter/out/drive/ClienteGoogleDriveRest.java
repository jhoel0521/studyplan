package io.github.jhoel0521.studyplan.adapter.out.drive;

import io.github.jhoel0521.studyplan.application.port.out.system.ClienteGoogleDrive;
import io.github.jhoel0521.studyplan.domain.readmodel.RespaldoDescargado;
import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adaptador de salida: habla con Google Drive v3 por HTTPS ({@code HttpURLConnection}, sin librerías pesadas) y
 * guarda un único archivo {@code studyplan_backup.spbk} en {@code appDataFolder}, que solo esta app puede ver. Crea
 * el archivo con una subida multipart y lo reemplaza con una subida de contenido. Ninguna operación lanza
 * excepciones: devuelve un {@link ResultadoRespaldo}.
 *
 * <p>Errores: 401/403 de autorización → {@code AUTORIZACION_REQUERIDA}; fallos de red, 429 y 5xx →
 * {@code SIN_CONEXION} (el trabajo se reintenta); el resto → {@code ERROR_DRIVE}.
 *
 * <p>Implementa: HU-35, HU-37 y HU-38.
 */
public final class ClienteGoogleDriveRest implements ClienteGoogleDrive {

    public static final String URL_API = "https://www.googleapis.com/drive/v3";
    public static final String URL_SUBIDA = "https://www.googleapis.com/upload/drive/v3";
    static final String NOMBRE_ARCHIVO = "studyplan_backup.spbk";

    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
    private static final int TIEMPO_CONEXION_MS = 15_000;
    private static final int TIEMPO_LECTURA_MS = 60_000;

    private final ProveedorTokenDrive tokens;
    private final String urlApi;
    private final String urlSubida;

    public ClienteGoogleDriveRest(ProveedorTokenDrive tokens) {
        this(tokens, URL_API, URL_SUBIDA);
    }

    /** Para pruebas contra un servidor local. */
    ClienteGoogleDriveRest(ProveedorTokenDrive tokens, String urlApi, String urlSubida) {
        this.tokens = tokens;
        this.urlApi = urlApi;
        this.urlSubida = urlSubida;
    }

    @Override
    public ResultadoRespaldo subir(byte[] archivo) {
        try {
            String token = tokens.obtenerToken();
            Respuesta busqueda = buscar(token);
            if (busqueda.codigo != 200) {
                return mapear(busqueda);
            }
            String id = primerId(busqueda.cuerpoTexto());
            Respuesta subida = id == null ? crear(token, archivo) : reemplazar(token, id, archivo);
            return mapear(subida);
        } catch (ProveedorTokenDrive.AutorizacionRequeridaException e) {
            return ResultadoRespaldo.AUTORIZACION_REQUERIDA;
        } catch (IOException e) {
            return ResultadoRespaldo.SIN_CONEXION;
        }
    }

    @Override
    public RespaldoDescargado descargar() {
        try {
            String token = tokens.obtenerToken();
            Respuesta busqueda = buscar(token);
            if (busqueda.codigo != 200) {
                return RespaldoDescargado.fallo(mapear(busqueda));
            }
            String id = primerId(busqueda.cuerpoTexto());
            if (id == null) {
                return RespaldoDescargado.fallo(ResultadoRespaldo.SIN_RESPALDO);
            }
            Respuesta descarga = enviar("GET", urlApi + "/files/" + id + "?alt=media", token, null, null, null);
            if (descarga.codigo != 200) {
                return RespaldoDescargado.fallo(mapear(descarga));
            }
            return new RespaldoDescargado(ResultadoRespaldo.OK, descarga.cuerpo);
        } catch (ProveedorTokenDrive.AutorizacionRequeridaException e) {
            return RespaldoDescargado.fallo(ResultadoRespaldo.AUTORIZACION_REQUERIDA);
        } catch (IOException e) {
            return RespaldoDescargado.fallo(ResultadoRespaldo.SIN_CONEXION);
        }
    }

    @Override
    public ResultadoRespaldo eliminar() {
        try {
            String token = tokens.obtenerToken();
            Respuesta busqueda = buscar(token);
            if (busqueda.codigo != 200) {
                return mapear(busqueda);
            }
            String id = primerId(busqueda.cuerpoTexto());
            if (id == null) {
                return ResultadoRespaldo.OK;
            }
            Respuesta borrado = enviar("DELETE", urlApi + "/files/" + id, token, null, null, null);
            return borrado.codigo == 404 ? ResultadoRespaldo.OK : mapear(borrado);
        } catch (ProveedorTokenDrive.AutorizacionRequeridaException e) {
            return ResultadoRespaldo.AUTORIZACION_REQUERIDA;
        } catch (IOException e) {
            return ResultadoRespaldo.SIN_CONEXION;
        }
    }

    private Respuesta buscar(String token) throws IOException {
        String consulta = "name='" + NOMBRE_ARCHIVO + "' and trashed=false";
        String url = urlApi + "/files?spaces=appDataFolder&pageSize=1&orderBy=modifiedTime%20desc&fields=files(id)&q="
                + URLEncoder.encode(consulta, "UTF-8");
        return enviar("GET", url, token, null, null, null);
    }

    private Respuesta crear(String token, byte[] archivo) throws IOException {
        String frontera = "studyplan" + System.nanoTime();
        String metadatos = "{\"name\":\"" + NOMBRE_ARCHIVO + "\",\"parents\":[\"appDataFolder\"]}";
        ByteArrayOutputStream cuerpo = new ByteArrayOutputStream(archivo.length + 256);
        cuerpo.write(("--" + frontera + "\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n" + metadatos
                + "\r\n--" + frontera + "\r\nContent-Type: application/octet-stream\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        cuerpo.write(archivo);
        cuerpo.write(("\r\n--" + frontera + "--").getBytes(StandardCharsets.UTF_8));
        return enviar("POST", urlSubida + "/files?uploadType=multipart", token, cuerpo.toByteArray(),
                "multipart/related; boundary=" + frontera, null);
    }

    private Respuesta reemplazar(String token, String id, byte[] archivo) throws IOException {
        // HttpURLConnection de la JVM no admite PATCH; Google acepta POST con esta cabecera.
        return enviar("POST", urlSubida + "/files/" + id + "?uploadType=media", token, archivo,
                "application/octet-stream", "PATCH");
    }

    private static Respuesta enviar(
            String metodo, String url, String token, byte[] cuerpo, String tipoContenido, String sustitutoMetodo)
            throws IOException {
        HttpURLConnection conexion = (HttpURLConnection) new URL(url).openConnection();
        try {
            conexion.setRequestMethod(metodo);
            conexion.setConnectTimeout(TIEMPO_CONEXION_MS);
            conexion.setReadTimeout(TIEMPO_LECTURA_MS);
            conexion.setRequestProperty("Authorization", "Bearer " + token);
            if (sustitutoMetodo != null) {
                conexion.setRequestProperty("X-HTTP-Method-Override", sustitutoMetodo);
            }
            if (cuerpo != null) {
                conexion.setDoOutput(true);
                conexion.setRequestProperty("Content-Type", tipoContenido);
                conexion.setFixedLengthStreamingMode(cuerpo.length);
                try (OutputStream salida = conexion.getOutputStream()) {
                    salida.write(cuerpo);
                }
            }
            int codigo = conexion.getResponseCode();
            InputStream entrada = codigo >= 400 ? conexion.getErrorStream() : conexion.getInputStream();
            return new Respuesta(codigo, entrada == null ? new byte[0] : leerTodo(entrada));
        } finally {
            conexion.disconnect();
        }
    }

    private static byte[] leerTodo(InputStream entrada) throws IOException {
        try (InputStream in = entrada) {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int leidos;
            while ((leidos = in.read(buffer)) != -1) {
                salida.write(buffer, 0, leidos);
            }
            return salida.toByteArray();
        }
    }

    private static String primerId(String json) {
        Matcher m = ID.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static ResultadoRespaldo mapear(Respuesta respuesta) {
        int c = respuesta.codigo;
        if (c >= 200 && c < 300) {
            return ResultadoRespaldo.OK;
        }
        if (c == 401) {
            return ResultadoRespaldo.AUTORIZACION_REQUERIDA;
        }
        if (c == 403) {
            return respuesta.cuerpoTexto().contains("ateLimit")
                    ? ResultadoRespaldo.SIN_CONEXION
                    : ResultadoRespaldo.AUTORIZACION_REQUERIDA;
        }
        if (c == 429 || c >= 500) {
            return ResultadoRespaldo.SIN_CONEXION;
        }
        return ResultadoRespaldo.ERROR_DRIVE;
    }

    private static final class Respuesta {
        final int codigo;
        final byte[] cuerpo;

        Respuesta(int codigo, byte[] cuerpo) {
            this.codigo = codigo;
            this.cuerpo = cuerpo;
        }

        String cuerpoTexto() {
            return new String(cuerpo, StandardCharsets.UTF_8);
        }
    }
}
