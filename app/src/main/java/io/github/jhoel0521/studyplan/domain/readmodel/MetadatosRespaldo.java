package io.github.jhoel0521.studyplan.domain.readmodel;

/**
 * Datos del último respaldo subido: cuándo, cuánto pesa y si está cifrado.
 *
 * <p>Implementa: HU-35 (fecha y tamaño del último respaldo).
 */
public final class MetadatosRespaldo {

    private final long fechaUtcMilis;
    private final long tamanoBytes;
    private final boolean cifrado;

    public MetadatosRespaldo(long fechaUtcMilis, long tamanoBytes, boolean cifrado) {
        this.fechaUtcMilis = fechaUtcMilis;
        this.tamanoBytes = tamanoBytes;
        this.cifrado = cifrado;
    }

    public long fechaUtcMilis() {
        return fechaUtcMilis;
    }

    public long tamanoBytes() {
        return tamanoBytes;
    }

    public boolean cifrado() {
        return cifrado;
    }
}
