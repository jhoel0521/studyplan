package io.github.jhoel0521.studyplan.adapter.out.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import io.github.jhoel0521.studyplan.application.port.out.system.AlmacenCredencialRespaldo;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Adaptador de salida: guarda la contraseña del respaldo cifrada con una clave AES-GCM del Android KeyStore, que no
 * sale del dispositivo. En {@code SharedPreferences} queda solo el texto cifrado (IV + contenido en Base64): nunca la
 * contraseña en claro. Si la clave se pierde (por ejemplo, tras restaurar la app en otro teléfono) {@link #obtener()}
 * devuelve {@code null} y el respaldo vuelve a pedir la contraseña.
 *
 * <p>Implementa: HU-36 y RN-20.
 */
public final class AlmacenCredencialRespaldoKeystore implements AlmacenCredencialRespaldo {

    private static final String PROVEEDOR = "AndroidKeyStore";
    private static final String ALIAS = "studyplan_respaldo_contrasena";
    private static final String PREFS = "studyplan_respaldo_credencial";
    private static final String CLAVE_TEXTO = "contrasena_cifrada";
    private static final int LARGO_IV = 12;

    private final SharedPreferences preferencias;

    public AlmacenCredencialRespaldoKeystore(Context contexto) {
        this.preferencias = contexto.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public void guardar(char[] contrasena) {
        try {
            Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
            cifrador.init(Cipher.ENCRYPT_MODE, obtenerOCrearClave());
            byte[] claro = aBytes(contrasena);
            byte[] cifrado = cifrador.doFinal(claro);
            Arrays.fill(claro, (byte) 0);
            byte[] iv = cifrador.getIV();
            ByteBuffer salida = ByteBuffer.allocate(iv.length + cifrado.length);
            salida.put(iv).put(cifrado);
            preferencias.edit()
                    .putString(CLAVE_TEXTO, Base64.encodeToString(salida.array(), Base64.NO_WRAP))
                    .apply();
        } catch (GeneralSecurityException | java.io.IOException e) {
            throw new IllegalStateException("No se pudo proteger la contraseña del respaldo", e);
        }
    }

    @Override
    public char[] obtener() {
        String texto = preferencias.getString(CLAVE_TEXTO, null);
        if (texto == null) {
            return null;
        }
        try {
            byte[] datos = Base64.decode(texto, Base64.NO_WRAP);
            KeyStore almacen = KeyStore.getInstance(PROVEEDOR);
            almacen.load(null);
            SecretKey clave = (SecretKey) almacen.getKey(ALIAS, null);
            if (clave == null) {
                return null;
            }
            Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
            cifrador.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(128, datos, 0, LARGO_IV));
            byte[] claro = cifrador.doFinal(datos, LARGO_IV, datos.length - LARGO_IV);
            char[] contrasena = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(claro)).array();
            Arrays.fill(claro, (byte) 0);
            return trimmed(contrasena);
        } catch (GeneralSecurityException | java.io.IOException | IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public void borrar() {
        preferencias.edit().remove(CLAVE_TEXTO).apply();
        try {
            KeyStore almacen = KeyStore.getInstance(PROVEEDOR);
            almacen.load(null);
            if (almacen.containsAlias(ALIAS)) {
                almacen.deleteEntry(ALIAS);
            }
        } catch (GeneralSecurityException | java.io.IOException e) {
            // Sin la clave el texto cifrado ya no se puede abrir; no hay nada más que borrar.
        }
    }

    private static SecretKey obtenerOCrearClave() throws GeneralSecurityException, java.io.IOException {
        KeyStore almacen = KeyStore.getInstance(PROVEEDOR);
        almacen.load(null);
        SecretKey existente = (SecretKey) almacen.getKey(ALIAS, null);
        if (existente != null) {
            return existente;
        }
        KeyGenerator generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVEEDOR);
        generador.init(new KeyGenParameterSpec.Builder(
                ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generador.generateKey();
    }

    private static byte[] aBytes(char[] contrasena) {
        ByteBuffer codificada = StandardCharsets.UTF_8.encode(CharBuffer.wrap(contrasena));
        byte[] bytes = new byte[codificada.remaining()];
        codificada.get(bytes);
        return bytes;
    }

    /** {@code CharBuffer.array()} puede traer relleno de ceros al final; se recorta. */
    private static char[] trimmed(char[] caracteres) {
        int fin = caracteres.length;
        while (fin > 0 && caracteres[fin - 1] == '\0') {
            fin--;
        }
        return Arrays.copyOf(caracteres, fin);
    }
}
