package io.github.jhoel0521.studyplan.adapter.out.drive;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Adaptador de salida: pregunta a Google el correo de la cuenta dueña de un token de acceso (alcance {@code email}).
 * Google Identity Services autoriza {@code drive.appdata} sin entregar el correo, y la app necesita mostrar qué cuenta
 * quedó vinculada. Bloquea: llamar siempre fuera del hilo principal.
 *
 * <p>Implementa: HU-35 y HU-37.
 */
public final class ConsultaCorreoGoogle {

    private static final String URL_USERINFO = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final int TIEMPO_MS = 15_000;

    private ConsultaCorreoGoogle() {
    }

    /** Devuelve el correo de la cuenta del token, o lanza {@link IOException} si Google no lo entrega. */
    public static String correoDe(String tokenAcceso) throws IOException {
        HttpURLConnection conexion = (HttpURLConnection) new URL(URL_USERINFO).openConnection();
        try {
            conexion.setConnectTimeout(TIEMPO_MS);
            conexion.setReadTimeout(TIEMPO_MS);
            conexion.setRequestProperty("Authorization", "Bearer " + tokenAcceso);
            int codigo = conexion.getResponseCode();
            if (codigo != HttpURLConnection.HTTP_OK) {
                throw new IOException("Google respondió " + codigo + " al pedir el correo de la cuenta");
            }
            String correo = new JSONObject(leer(conexion.getInputStream())).optString("email", "");
            if (correo.isEmpty()) {
                throw new IOException("Google no entregó el correo de la cuenta");
            }
            return correo;
        } catch (JSONException e) {
            throw new IOException("Respuesta de Google ilegible", e);
        } finally {
            conexion.disconnect();
        }
    }

    private static String leer(InputStream entrada) throws IOException {
        try (InputStream in = entrada; ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            byte[] bloque = new byte[1024];
            int leidos;
            while ((leidos = in.read(bloque)) != -1) {
                salida.write(bloque, 0, leidos);
            }
            return salida.toString(StandardCharsets.UTF_8.name());
        }
    }
}
