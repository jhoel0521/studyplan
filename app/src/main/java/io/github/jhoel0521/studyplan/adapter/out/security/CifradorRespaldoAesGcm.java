package io.github.jhoel0521.studyplan.adapter.out.security;

import io.github.jhoel0521.studyplan.application.port.out.system.CifradorRespaldo;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Optional;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Adaptador de salida: archivo de respaldo con cabecera propia y cifrado opcional AES-256-GCM. La clave sale de la
 * contraseña con PBKDF2-HMAC-SHA256 (120 000 iteraciones, sal aleatoria de 16 bytes); el IV de GCM tiene 12 bytes y la
 * etiqueta 128 bits. Si la contraseña es incorrecta la etiqueta falla y no se devuelve nada.
 *
 * <p>Formato: {@code "SPBK1"} + bandera ({@code 0} claro, {@code 1} cifrado) + [sal(16) + IV(12)] + contenido. La
 * cabecera se autentica como datos asociados de GCM. PBKDF2 se calcula aquí con HMAC-SHA256 porque
 * {@code SecretKeyFactory("PBKDF2WithHmacSHA256")} solo existe desde Android 8 y la app admite Android 7 (minSdk 24).
 *
 * <p>Implementa: HU-36 y RN-20.
 */
public final class CifradorRespaldoAesGcm implements CifradorRespaldo {

    static final byte[] MAGIA = "SPBK1".getBytes(StandardCharsets.US_ASCII);
    static final int ITERACIONES = 120_000;
    private static final int LARGO_SAL = 16;
    private static final int LARGO_IV = 12;
    private static final int LARGO_CLAVE_BYTES = 32;
    private static final int BITS_ETIQUETA = 128;
    private static final byte CLARO = 0;
    private static final byte CIFRADO = 1;

    private final SecureRandom azar = new SecureRandom();

    @Override
    public byte[] empaquetar(byte[] datos, char[] contrasena) {
        if (contrasena == null) {
            byte[] salida = new byte[MAGIA.length + 1 + datos.length];
            System.arraycopy(MAGIA, 0, salida, 0, MAGIA.length);
            salida[MAGIA.length] = CLARO;
            System.arraycopy(datos, 0, salida, MAGIA.length + 1, datos.length);
            return salida;
        }
        if (contrasena.length == 0) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }
        try {
            byte[] sal = new byte[LARGO_SAL];
            byte[] iv = new byte[LARGO_IV];
            azar.nextBytes(sal);
            azar.nextBytes(iv);
            byte[] cabecera = cabecera(CIFRADO);
            Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
            cifrador.init(Cipher.ENCRYPT_MODE, claveDe(contrasena, sal), new GCMParameterSpec(BITS_ETIQUETA, iv));
            cifrador.updateAAD(cabecera);
            byte[] cifrado = cifrador.doFinal(datos);
            ByteBuffer salida = ByteBuffer.allocate(cabecera.length + LARGO_SAL + LARGO_IV + cifrado.length);
            salida.put(cabecera).put(sal).put(iv).put(cifrado);
            return salida.array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el respaldo", e);
        }
    }

    @Override
    public boolean reconoce(byte[] archivo) {
        if (archivo == null || archivo.length < MAGIA.length + 1) {
            return false;
        }
        for (int i = 0; i < MAGIA.length; i++) {
            if (archivo[i] != MAGIA[i]) {
                return false;
            }
        }
        byte bandera = archivo[MAGIA.length];
        if (bandera == CLARO) {
            return true;
        }
        return bandera == CIFRADO && archivo.length >= MAGIA.length + 1 + LARGO_SAL + LARGO_IV;
    }

    @Override
    public boolean estaCifrado(byte[] archivo) {
        return reconoce(archivo) && archivo[MAGIA.length] == CIFRADO;
    }

    @Override
    public Optional<byte[]> desempaquetar(byte[] archivo, char[] contrasena) {
        if (!reconoce(archivo)) {
            throw new IllegalArgumentException("No es un archivo de respaldo de StudyPlan");
        }
        int inicio = MAGIA.length + 1;
        if (!estaCifrado(archivo)) {
            return Optional.of(Arrays.copyOfRange(archivo, inicio, archivo.length));
        }
        if (contrasena == null || contrasena.length == 0) {
            return Optional.empty();
        }
        try {
            byte[] sal = Arrays.copyOfRange(archivo, inicio, inicio + LARGO_SAL);
            byte[] iv = Arrays.copyOfRange(archivo, inicio + LARGO_SAL, inicio + LARGO_SAL + LARGO_IV);
            Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
            cifrador.init(Cipher.DECRYPT_MODE, claveDe(contrasena, sal), new GCMParameterSpec(BITS_ETIQUETA, iv));
            cifrador.updateAAD(cabecera(CIFRADO));
            return Optional.of(cifrador.doFinal(archivo, inicio + LARGO_SAL + LARGO_IV,
                    archivo.length - (inicio + LARGO_SAL + LARGO_IV)));
        } catch (AEADBadTagException e) {
            return Optional.empty();
        } catch (GeneralSecurityException e) {
            // En algunas versiones de Android la etiqueta inválida llega como otra excepción de seguridad.
            return Optional.empty();
        }
    }

    private static byte[] cabecera(byte bandera) {
        byte[] cabecera = Arrays.copyOf(MAGIA, MAGIA.length + 1);
        cabecera[MAGIA.length] = bandera;
        return cabecera;
    }

    private static SecretKeySpec claveDe(char[] contrasena, byte[] sal) throws GeneralSecurityException {
        return new SecretKeySpec(pbkdf2Sha256(contrasena, sal, ITERACIONES, LARGO_CLAVE_BYTES), "AES");
    }

    /** PBKDF2-HMAC-SHA256 (RFC 8018) para una clave de hasta 32 bytes (un solo bloque). */
    static byte[] pbkdf2Sha256(char[] contrasena, byte[] sal, int iteraciones, int largo)
            throws GeneralSecurityException {
        if (largo > 32) {
            throw new IllegalArgumentException("Solo se soporta un bloque de 32 bytes");
        }
        ByteBuffer codificada = StandardCharsets.UTF_8.encode(CharBuffer.wrap(contrasena));
        byte[] clave = new byte[codificada.remaining()];
        codificada.get(clave);
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(clave, "HmacSHA256"));
        Arrays.fill(clave, (byte) 0);
        hmac.update(sal);
        hmac.update(new byte[] {0, 0, 0, 1});
        byte[] u = hmac.doFinal();
        byte[] t = u.clone();
        for (int i = 1; i < iteraciones; i++) {
            u = hmac.doFinal(u);
            for (int j = 0; j < t.length; j++) {
                t[j] ^= u[j];
            }
        }
        return Arrays.copyOf(t, largo);
    }
}
