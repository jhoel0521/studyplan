package io.github.jhoel0521.studyplan.adapter.out.security;

import static org.junit.Assert.assertEquals;

import android.app.KeyguardManager;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Prueba instrumentada del adaptador de disponibilidad del bloqueo: debe coincidir con lo que dice el sistema sobre
 * si el teléfono tiene bloqueo de pantalla (con y sin PIN configurado en el emulador).
 *
 * <p>Verifica: HU-04, escenario «Sin método de bloqueo».
 */
@RunWith(AndroidJUnit4.class)
public class DisponibilidadBloqueoBiometricoTest {

    @Test
    public void coincideConElBloqueoDePantallaDelTelefono() {
        Context contexto = ApplicationProvider.getApplicationContext();
        KeyguardManager guardia = contexto.getSystemService(KeyguardManager.class);
        assertEquals(guardia.isDeviceSecure(), new DisponibilidadBloqueoBiometrico(contexto).estaDisponible());
    }
}
