package io.github.jhoel0521.studyplan.adapter.out.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.junit.Test;

/**
 * Pruebas de {@link CifradorRespaldoAesGcm}: PBKDF2 propio contra el de la JVM, cifrado y descifrado con contraseña
 * correcta e incorrecta, archivo en claro, manipulación y cabecera.
 *
 * <p>Verifica: HU-36 y RN-20.
 */
public class CifradorRespaldoAesGcmTest {

    private final CifradorRespaldoAesGcm cifrador = new CifradorRespaldoAesGcm();
    private final byte[] datos = "{\"tablas\":{}} datos de prueba ñ 日本".getBytes(StandardCharsets.UTF_8);

    @Test
    public void pbkdf2PropioCoincideConElDeLaJvm() throws Exception {
        char[] contrasena = "contraseña ñ 123".toCharArray();
        byte[] sal = "salsalsalsalsal!".getBytes(StandardCharsets.US_ASCII);

        byte[] propio = CifradorRespaldoAesGcm.pbkdf2Sha256(contrasena, sal, 1000, 32);
        byte[] referencia = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(new PBEKeySpec(contrasena, sal, 1000, 256)).getEncoded();

        // La JVM codifica la contraseña en UTF-8 igual que nosotros.
        assertArrayEquals(referencia, propio);
    }

    @Test
    public void cifrarYDescifrarConLaContrasenaCorrectaDevuelveLosDatosOriginales() {
        byte[] archivo = cifrador.empaquetar(datos, "secreta".toCharArray());

        assertTrue(cifrador.reconoce(archivo));
        assertTrue(cifrador.estaCifrado(archivo));
        Optional<byte[]> abierto = cifrador.desempaquetar(archivo, "secreta".toCharArray());
        assertTrue(abierto.isPresent());
        assertArrayEquals(datos, abierto.get());
    }

    @Test
    public void conUnaContrasenaIncorrectaNoDevuelveNadaYPermiteReintentar() {
        byte[] archivo = cifrador.empaquetar(datos, "secreta".toCharArray());

        assertFalse(cifrador.desempaquetar(archivo, "otra".toCharArray()).isPresent());
        assertFalse(cifrador.desempaquetar(archivo, null).isPresent());
        assertTrue(cifrador.desempaquetar(archivo, "secreta".toCharArray()).isPresent());
    }

    @Test
    public void elArchivoCifradoNoContieneLosDatosEnClaro() {
        byte[] archivo = cifrador.empaquetar(datos, "secreta".toCharArray());

        assertFalse(new String(archivo, StandardCharsets.ISO_8859_1).contains("datos de prueba"));
    }

    @Test
    public void cadaCifradoUsaSalYVectorDistintosYDaBytesDistintos() {
        byte[] uno = cifrador.empaquetar(datos, "secreta".toCharArray());
        byte[] dos = cifrador.empaquetar(datos, "secreta".toCharArray());

        assertNotEquals(Arrays.toString(uno), Arrays.toString(dos));
        assertEquals(uno.length, dos.length);
    }

    @Test
    public void unByteAlteradoHaceFallarLaEtiquetaGcm() {
        byte[] archivo = cifrador.empaquetar(datos, "secreta".toCharArray());
        archivo[archivo.length - 1] ^= 0x01;

        assertFalse(cifrador.desempaquetar(archivo, "secreta".toCharArray()).isPresent());
    }

    @Test
    public void sinContrasenaElArchivoVaEnClaroConCabeceraYSeAbreSinContrasena() {
        byte[] archivo = cifrador.empaquetar(datos, null);

        assertTrue(cifrador.reconoce(archivo));
        assertFalse(cifrador.estaCifrado(archivo));
        assertArrayEquals(datos, cifrador.desempaquetar(archivo, null).get());
    }

    @Test
    public void reconoceSoloArchivosConLaCabeceraDeStudyPlan() {
        assertFalse(cifrador.reconoce(null));
        assertFalse(cifrador.reconoce(new byte[0]));
        assertFalse(cifrador.reconoce("basura que no es un respaldo".getBytes(StandardCharsets.UTF_8)));
        assertFalse(cifrador.reconoce(Arrays.copyOf(CifradorRespaldoAesGcm.MAGIA, CifradorRespaldoAesGcm.MAGIA.length)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void noCifraConUnaContrasenaVacia() {
        cifrador.empaquetar(datos, new char[0]);
    }
}
