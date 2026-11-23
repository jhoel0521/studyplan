package io.github.jhoel0521.studyplan.adapter.out.drive;

import android.content.Context;

import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Tasks;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

/**
 * Adaptador de salida: pide a Google el token de acceso con el alcance {@code drive.appdata} usando la API de
 * autorización de Google Identity Services (no el antiguo Google Sign-In). Sin interfaz solo funciona si el
 * estudiante ya dio su consentimiento; si Google exige pantalla, lanza {@link AutorizacionRequeridaException}.
 *
 * <p>Implementa: HU-35 y HU-37.
 */
public final class AutenticadorGoogleDrive implements ProveedorTokenDrive {

    /** Único alcance que usa la app: la carpeta oculta de datos de la aplicación. */
    public static final String ALCANCE_APPDATA = "https://www.googleapis.com/auth/drive.appdata";

    private final Context contexto;

    public AutenticadorGoogleDrive(Context contexto) {
        this.contexto = contexto.getApplicationContext();
    }

    /** Solicitud de autorización de {@code drive.appdata}; la usan también las pantallas para pedir el permiso. */
    public static AuthorizationRequest solicitud() {
        return AuthorizationRequest.builder()
                .setRequestedScopes(Collections.singletonList(new Scope(ALCANCE_APPDATA)))
                .build();
    }

    @Override
    public String obtenerToken() throws AutorizacionRequeridaException, IOException {
        try {
            AuthorizationResult resultado =
                    Tasks.await(Identity.getAuthorizationClient(contexto).authorize(solicitud()));
            if (resultado.hasResolution() || resultado.getAccessToken() == null) {
                throw new AutorizacionRequeridaException("Google pide que el estudiante autorice de nuevo");
            }
            return resultado.getAccessToken();
        } catch (ExecutionException e) {
            Throwable causa = e.getCause();
            if (causa instanceof ApiException
                    && ((ApiException) causa).getStatusCode() == CommonStatusCodes.NETWORK_ERROR) {
                throw new IOException("Sin conexión con los servicios de Google", causa);
            }
            throw new AutorizacionRequeridaException("Google rechazó la autorización: " + causa);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Autorización interrumpida", e);
        }
    }
}
