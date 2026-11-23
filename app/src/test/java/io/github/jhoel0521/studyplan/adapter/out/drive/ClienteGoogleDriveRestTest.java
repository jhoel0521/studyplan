package io.github.jhoel0521.studyplan.adapter.out.drive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.github.jhoel0521.studyplan.domain.readmodel.RespaldoDescargado;
import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Pruebas de {@link ClienteGoogleDriveRest} contra un servidor HTTP local mínimo que imita a Drive: crear,
 * reemplazar, bajar, borrar y la traducción de errores.
 *
 * <p>Verifica: HU-35, HU-37 y HU-38.
 */
public class ClienteGoogleDriveRestTest {

    private ServerSocket socket;
    private Thread hilo;
    private ClienteGoogleDriveRest cliente;
    private final ProveedorTokenDrive tokens = () -> "token-de-prueba";

    /** Peticiones recibidas: "METODO ruta|cabecera de método|Authorization". */
    private final List<String> peticiones = Collections.synchronizedList(new ArrayList<String>());
    private volatile byte[] archivoEnDrive;
    private volatile String idEnDrive;
    private volatile int codigoForzado = 0;
    private volatile String cuerpoForzado = "{}";

    @Before
    public void arrancarServidor() throws IOException {
        socket = new ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"));
        hilo = new Thread(() -> {
            while (!socket.isClosed()) {
                try (Socket conexion = socket.accept()) {
                    atender(conexion);
                } catch (IOException e) {
                    // Servidor cerrado al terminar la prueba.
                }
            }
        });
        hilo.setDaemon(true);
        hilo.start();
        String base = "http://127.0.0.1:" + socket.getLocalPort();
        cliente = new ClienteGoogleDriveRest(tokens, base + "/api", base + "/subida");
    }

    @After
    public void pararServidor() throws IOException {
        socket.close();
    }

    private void atender(Socket conexion) throws IOException {
        InputStream in = conexion.getInputStream();
        String lineaPeticion = leerLinea(in);
        if (lineaPeticion.isEmpty()) {
            return;
        }
        String[] partes = lineaPeticion.split(" ");
        Map<String, String> cabeceras = new HashMap<>();
        String linea;
        while (!(linea = leerLinea(in)).isEmpty()) {
            int dos = linea.indexOf(':');
            cabeceras.put(linea.substring(0, dos).trim().toLowerCase(Locale.ROOT), linea.substring(dos + 1).trim());
        }
        int largo = cabeceras.containsKey("content-length") ? Integer.parseInt(cabeceras.get("content-length")) : 0;
        byte[] cuerpo = new byte[largo];
        int leidos = 0;
        while (leidos < largo) {
            int n = in.read(cuerpo, leidos, largo - leidos);
            if (n < 0) {
                break;
            }
            leidos += n;
        }
        String metodo = partes[0];
        String ruta = partes[1];
        String sustituto = cabeceras.get("x-http-method-override");
        peticiones.add(metodo + " " + ruta + "|" + sustituto + "|" + cabeceras.get("authorization"));

        int codigo = 200;
        byte[] respuesta;
        if (codigoForzado != 0) {
            codigo = codigoForzado;
            respuesta = cuerpoForzado.getBytes(StandardCharsets.UTF_8);
        } else if (ruta.startsWith("/api/files?")) {
            respuesta = (idEnDrive == null ? "{\"files\":[]}" : "{\"files\":[{\"id\":\"" + idEnDrive + "\"}]}")
                    .getBytes(StandardCharsets.UTF_8);
        } else if (ruta.startsWith("/subida/files?uploadType=multipart")) {
            String marca = "application/octet-stream\r\n\r\n";
            String texto = new String(cuerpo, StandardCharsets.ISO_8859_1);
            int inicio = texto.indexOf(marca) + marca.length();
            int fin = texto.lastIndexOf("\r\n--");
            archivoEnDrive = Arrays.copyOfRange(cuerpo, inicio, fin);
            idEnDrive = "id-nuevo";
            respuesta = "{\"id\":\"id-nuevo\"}".getBytes(StandardCharsets.UTF_8);
        } else if (ruta.startsWith("/subida/files/") && "PATCH".equals(sustituto)) {
            archivoEnDrive = cuerpo;
            respuesta = "{\"id\":\"id-nuevo\"}".getBytes(StandardCharsets.UTF_8);
        } else if (ruta.startsWith("/api/files/") && ruta.endsWith("alt=media")) {
            respuesta = archivoEnDrive;
        } else if ("DELETE".equals(metodo)) {
            archivoEnDrive = null;
            idEnDrive = null;
            codigo = 204;
            respuesta = new byte[0];
        } else {
            codigo = 400;
            respuesta = new byte[0];
        }
        OutputStream out = conexion.getOutputStream();
        out.write(("HTTP/1.1 " + codigo + " X\r\nContent-Length: " + respuesta.length + "\r\nConnection: close\r\n\r\n")
                .getBytes(StandardCharsets.ISO_8859_1));
        out.write(respuesta);
        out.flush();
    }

    private static String leerLinea(InputStream in) throws IOException {
        ByteArrayOutputStream linea = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1 && b != '\n') {
            if (b != '\r') {
                linea.write(b);
            }
        }
        return linea.toString("ISO-8859-1");
    }

    @Test
    public void laPrimeraSubidaCreaElArchivoEnAppDataFolderYSeBajaIgual() {
        byte[] datos = {0, 1, 2, 3, (byte) 200, 10, 13, 10};

        assertEquals(ResultadoRespaldo.OK, cliente.subir(datos));

        assertArrayEquals(datos, archivoEnDrive);
        assertTrue(peticiones.get(0).contains("spaces=appDataFolder"));
        assertTrue(peticiones.get(0).endsWith("Bearer token-de-prueba"));
        RespaldoDescargado descarga = cliente.descargar();
        assertEquals(ResultadoRespaldo.OK, descarga.resultado());
        assertArrayEquals(datos, descarga.datos());
    }

    @Test
    public void unaSegundaSubidaReemplazaElArchivoSinCrearOtro() {
        cliente.subir(new byte[] {1, 2, 3});
        int creaciones = contar("uploadType=multipart");

        assertEquals(ResultadoRespaldo.OK, cliente.subir(new byte[] {9, 9}));

        assertArrayEquals(new byte[] {9, 9}, archivoEnDrive);
        assertEquals(creaciones, contar("uploadType=multipart"));
        assertEquals(1, contar("uploadType=media"));
    }

    @Test
    public void sinRespaldoEnDriveDescargarDiceSinRespaldo() {
        RespaldoDescargado descarga = cliente.descargar();

        assertEquals(ResultadoRespaldo.SIN_RESPALDO, descarga.resultado());
        assertNull(descarga.datos());
    }

    @Test
    public void eliminarBorraElArchivoYEsCorrectoSiYaNoExistia() {
        assertEquals(ResultadoRespaldo.OK, cliente.eliminar());
        cliente.subir(new byte[] {1});

        assertEquals(ResultadoRespaldo.OK, cliente.eliminar());

        assertNull(archivoEnDrive);
        assertEquals(ResultadoRespaldo.SIN_RESPALDO, cliente.descargar().resultado());
    }

    @Test
    public void traduceLosCodigosDeError() {
        codigoForzado = 401;
        assertEquals(ResultadoRespaldo.AUTORIZACION_REQUERIDA, cliente.subir(new byte[] {1}));
        codigoForzado = 403;
        cuerpoForzado = "{\"error\":{\"errors\":[{\"reason\":\"insufficientPermissions\"}]}}";
        assertEquals(ResultadoRespaldo.AUTORIZACION_REQUERIDA, cliente.descargar().resultado());
        cuerpoForzado = "{\"error\":{\"errors\":[{\"reason\":\"userRateLimitExceeded\"}]}}";
        assertEquals(ResultadoRespaldo.SIN_CONEXION, cliente.eliminar());
        codigoForzado = 503;
        assertEquals(ResultadoRespaldo.SIN_CONEXION, cliente.subir(new byte[] {1}));
        codigoForzado = 429;
        assertEquals(ResultadoRespaldo.SIN_CONEXION, cliente.subir(new byte[] {1}));
        codigoForzado = 400;
        assertEquals(ResultadoRespaldo.ERROR_DRIVE, cliente.subir(new byte[] {1}));
    }

    @Test
    public void sinServidorAlcanzableEsSinConexion() throws IOException {
        socket.close();

        assertEquals(ResultadoRespaldo.SIN_CONEXION, cliente.subir(new byte[] {1}));
        assertEquals(ResultadoRespaldo.SIN_CONEXION, cliente.descargar().resultado());
        assertEquals(ResultadoRespaldo.SIN_CONEXION, cliente.eliminar());
    }

    @Test
    public void siGoogleExigeAutorizarDeNuevoNoSeLlamaADrive() {
        ProveedorTokenDrive sinPermiso = () -> {
            throw new ProveedorTokenDrive.AutorizacionRequeridaException("caducó");
        };
        String base = "http://127.0.0.1:" + socket.getLocalPort();
        ClienteGoogleDriveRest sinToken = new ClienteGoogleDriveRest(sinPermiso, base + "/api", base + "/subida");

        assertEquals(ResultadoRespaldo.AUTORIZACION_REQUERIDA, sinToken.subir(new byte[] {1}));
        assertEquals(ResultadoRespaldo.AUTORIZACION_REQUERIDA, sinToken.descargar().resultado());
        assertEquals(ResultadoRespaldo.AUTORIZACION_REQUERIDA, sinToken.eliminar());

        assertTrue(peticiones.isEmpty());
    }

    private int contar(String fragmento) {
        int total = 0;
        synchronized (peticiones) {
            for (String p : peticiones) {
                if (p.contains(fragmento)) {
                    total++;
                }
            }
        }
        return total;
    }
}
