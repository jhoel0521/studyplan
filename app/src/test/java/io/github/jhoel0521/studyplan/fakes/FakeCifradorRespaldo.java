package io.github.jhoel0521.studyplan.fakes;

import io.github.jhoel0521.studyplan.application.port.out.system.CifradorRespaldo;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 * Cifrador falso reversible: antepone la marca {@code FAKE:} y una bandera con la contraseña en claro. No cifra de
 * verdad (eso lo prueba {@code CifradorRespaldoAesGcmTest}); sirve para comprobar el flujo de los casos de uso.
 *
 * <p>Apoya las pruebas de: HU-36 y HU-37.
 */
public final class FakeCifradorRespaldo implements CifradorRespaldo {

    private static final String MARCA = "FAKE:";

    @Override
    public byte[] empaquetar(byte[] datos, char[] contrasena) {
        String cabecera = MARCA + (contrasena == null ? "" : new String(contrasena)) + "|";
        byte[] bytes = cabecera.getBytes(StandardCharsets.UTF_8);
        byte[] salida = Arrays.copyOf(bytes, bytes.length + datos.length);
        System.arraycopy(datos, 0, salida, bytes.length, datos.length);
        return salida;
    }

    @Override
    public boolean reconoce(byte[] archivo) {
        return new String(archivo, StandardCharsets.UTF_8).startsWith(MARCA);
    }

    @Override
    public boolean estaCifrado(byte[] archivo) {
        String texto = new String(archivo, StandardCharsets.UTF_8);
        return !texto.startsWith(MARCA + "|");
    }

    @Override
    public Optional<byte[]> desempaquetar(byte[] archivo, char[] contrasena) {
        String texto = new String(archivo, StandardCharsets.UTF_8);
        int fin = texto.indexOf('|');
        String guardada = texto.substring(MARCA.length(), fin);
        String dada = contrasena == null ? "" : new String(contrasena);
        if (!guardada.equals(dada)) {
            return Optional.empty();
        }
        int inicioDatos = texto.substring(0, fin + 1).getBytes(StandardCharsets.UTF_8).length;
        return Optional.of(Arrays.copyOfRange(archivo, inicioDatos, archivo.length));
    }
}
