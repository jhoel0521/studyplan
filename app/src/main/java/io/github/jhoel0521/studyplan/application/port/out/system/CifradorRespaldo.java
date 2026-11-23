package io.github.jhoel0521.studyplan.application.port.out.system;

import java.util.Optional;

/**
 * Puerto de salida: envuelve los bytes del respaldo en el archivo final, cifrados con contraseña o en claro, y los
 * abre de vuelta. El archivo lleva una cabecera propia que indica si está cifrado.
 *
 * <p>Implementa: HU-36 y RN-20 (AES-256-GCM con clave derivada por PBKDF2).
 */
public interface CifradorRespaldo {

    /**
     * @param contrasena contraseña, o {@code null} para guardar sin cifrar
     * @return el archivo final listo para subir
     */
    byte[] empaquetar(byte[] datos, char[] contrasena);

    /** {@code true} si los bytes tienen la cabecera de un respaldo de StudyPlan. */
    boolean reconoce(byte[] archivo);

    /** {@code true} si el archivo está cifrado y pide contraseña. */
    boolean estaCifrado(byte[] archivo);

    /**
     * Abre el archivo.
     *
     * @param contrasena contraseña, o {@code null} si el archivo no está cifrado
     * @return los datos originales, o vacío si la contraseña no abre el archivo (etiqueta GCM inválida)
     */
    Optional<byte[]> desempaquetar(byte[] archivo, char[] contrasena);
}
