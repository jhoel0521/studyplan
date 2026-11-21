package io.github.jhoel0521.studyplan.adapter.out.security;

import android.content.Context;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricManager.Authenticators;

import io.github.jhoel0521.studyplan.application.port.out.system.DisponibilidadBloqueo;

/**
 * Adaptador de salida: pregunta al sistema si el teléfono tiene huella, PIN, patrón o contraseña configurados.
 *
 * <p>Implementa: HU-04, escenario «Sin método de bloqueo».
 */
public class DisponibilidadBloqueoBiometrico implements DisponibilidadBloqueo {

    private final Context contexto;

    public DisponibilidadBloqueoBiometrico(Context contexto) {
        this.contexto = contexto.getApplicationContext();
    }

    @Override
    public boolean estaDisponible() {
        int resultado = BiometricManager.from(contexto)
                .canAuthenticate(Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL);
        return resultado == BiometricManager.BIOMETRIC_SUCCESS;
    }
}
