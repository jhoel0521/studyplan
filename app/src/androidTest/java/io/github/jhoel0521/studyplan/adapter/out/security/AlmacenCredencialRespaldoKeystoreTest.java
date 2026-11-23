package io.github.jhoel0521.studyplan.adapter.out.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Pruebas instrumentadas de {@link AlmacenCredencialRespaldoKeystore} con el Android KeyStore real: guardar, leer y
 * borrar la contraseña, y que en las preferencias solo quede texto cifrado.
 *
 * <p>Verifica: HU-36 y RN-20 (la contraseña no se guarda en claro).
 */
@RunWith(AndroidJUnit4.class)
public class AlmacenCredencialRespaldoKeystoreTest {

    private Context contexto;
    private AlmacenCredencialRespaldoKeystore almacen;

    @Before
    public void preparar() {
        contexto = ApplicationProvider.getApplicationContext();
        almacen = new AlmacenCredencialRespaldoKeystore(contexto);
        almacen.borrar();
    }

    @After
    public void limpiar() {
        almacen.borrar();
    }

    @Test
    public void sinNadaGuardadoNoHayContrasena() {
        assertNull(almacen.obtener());
    }

    @Test
    public void guardarYObtenerDevuelveLaMismaContrasena() {
        almacen.guardar("secreta ñ 123".toCharArray());

        assertArrayEquals("secreta ñ 123".toCharArray(), almacen.obtener());
    }

    @Test
    public void enLasPreferenciasNoQuedaLaContrasenaEnClaro() {
        almacen.guardar("secreta".toCharArray());

        String guardado = contexto.getSharedPreferences("studyplan_respaldo_credencial", Context.MODE_PRIVATE)
                .getString("contrasena_cifrada", "");

        assertFalse(guardado.isEmpty());
        assertFalse(guardado.contains("secreta"));
    }

    @Test
    public void borrarOlvidaLaContrasena() {
        almacen.guardar("secreta".toCharArray());

        almacen.borrar();

        assertNull(almacen.obtener());
    }

    @Test
    public void guardarDeNuevoReemplazaLaAnterior() {
        almacen.guardar("uno".toCharArray());
        almacen.guardar("dos".toCharArray());

        assertEquals("dos", new String(almacen.obtener()));
    }
}
