package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager.Authenticators;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

import io.github.jhoel0521.studyplan.R;

/**
 * Pantalla de bloqueo: no muestra datos y lanza el diálogo del sistema con huella o credencial del dispositivo.
 * Al acertar desbloquea la sesión y vuelve a la pantalla anterior; atrás sale de la app.
 *
 * <p>Implementa: HU-04 (pedir autenticación) y RNF-12.
 */
public class AutenticacionActivity extends ActividadBase {

    private TextView mensaje;
    private BiometricPrompt dialogo;
    private BiometricPrompt.PromptInfo datosDialogo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_autenticacion);
        aplicarInsets();
        enlazarVistas();
        prepararDialogo();
        getOnBackPressedDispatcher().addCallback(this, new AlPulsarAtras());
        if (savedInstanceState == null) {
            lanzarDialogo();
        }
    }

    private void enlazarVistas() {
        mensaje = findViewById(R.id.mensajeAutenticacion);
        MaterialButton botonReintentar = findViewById(R.id.botonReintentar);
        botonReintentar.setOnClickListener(this::onReintentarClick);
    }

    private void prepararDialogo() {
        dialogo = new BiometricPrompt(this, ContextCompat.getMainExecutor(this), new AlAutenticar());
        datosDialogo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.autenticacion_titulo_dialogo))
                .setAllowedAuthenticators(Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL)
                .build();
    }

    private void lanzarDialogo() {
        mensaje.setText(R.string.autenticacion_mensaje);
        dialogo.authenticate(datosDialogo);
    }

    private void onAutenticado() {
        app().sesionBloqueo().desbloquear();
        finish();
    }

    private void onErrorDeAutenticacion(CharSequence texto) {
        mensaje.setText(texto);
    }

    private void onReintentarClick(View vista) {
        lanzarDialogo();
    }

    private void onAtras() {
        finishAffinity();
    }

    /** Respuestas del diálogo del sistema. */
    private final class AlAutenticar extends BiometricPrompt.AuthenticationCallback {
        @Override
        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult resultado) {
            onAutenticado();
        }

        @Override
        public void onAuthenticationError(int codigo, @NonNull CharSequence texto) {
            onErrorDeAutenticacion(texto);
        }
    }

    /** Atrás sale de la app en lugar de volver a una pantalla con datos. */
    private final class AlPulsarAtras extends OnBackPressedCallback {
        AlPulsarAtras() {
            super(true);
        }

        @Override
        public void handleOnBackPressed() {
            onAtras();
        }
    }
}
