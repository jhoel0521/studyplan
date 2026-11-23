package io.github.jhoel0521.studyplan.adapter.in.ui.comun;

import android.app.PendingIntent;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;

import io.github.jhoel0521.studyplan.adapter.out.drive.AutenticadorGoogleDrive;

/**
 * Pide al estudiante, con la pantalla de Google, el permiso de {@code drive.appdata} y entrega el correo de la cuenta
 * elegida. Se crea en {@code onCreate} (registra el resultado de la pantalla de Google) y se lanza con
 * {@link #iniciar()}.
 *
 * <p>Implementa: HU-35 y HU-37.
 */
public final class ProcesoAutorizacionDrive {

    /** Resultado del proceso. */
    public interface Escucha {
        void alAutorizar(String correo);

        void alFallar();
    }

    private final ComponentActivity actividad;
    private final Escucha escucha;
    private final ActivityResultLauncher<IntentSenderRequest> lanzador;

    public ProcesoAutorizacionDrive(ComponentActivity actividad, Escucha escucha) {
        this.actividad = actividad;
        this.escucha = escucha;
        this.lanzador = actividad.registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(), this::onResultadoDePantalla);
    }

    /** Pide la autorización; muestra la pantalla de Google solo si hace falta. */
    public void iniciar() {
        Identity.getAuthorizationClient(actividad)
                .authorize(AutenticadorGoogleDrive.solicitud())
                .addOnSuccessListener(this::onAutorizacion)
                .addOnFailureListener(error -> escucha.alFallar());
    }

    private void onAutorizacion(AuthorizationResult resultado) {
        if (resultado.hasResolution()) {
            PendingIntent pantalla = resultado.getPendingIntent();
            if (pantalla == null) {
                escucha.alFallar();
                return;
            }
            lanzador.launch(new IntentSenderRequest.Builder(pantalla.getIntentSender()).build());
        } else {
            entregar(resultado);
        }
    }

    private void onResultadoDePantalla(ActivityResult resultado) {
        if (resultado.getData() == null) {
            escucha.alFallar();
            return;
        }
        try {
            entregar(Identity.getAuthorizationClient(actividad).getAuthorizationResultFromIntent(resultado.getData()));
        } catch (ApiException e) {
            escucha.alFallar();
        }
    }

    private void entregar(AuthorizationResult resultado) {
        GoogleSignInAccount cuenta = resultado.toGoogleSignInAccount();
        String correo = cuenta == null ? null : cuenta.getEmail();
        if (correo == null || correo.isEmpty()) {
            escucha.alFallar();
        } else {
            escucha.alAutorizar(correo);
        }
    }
}
