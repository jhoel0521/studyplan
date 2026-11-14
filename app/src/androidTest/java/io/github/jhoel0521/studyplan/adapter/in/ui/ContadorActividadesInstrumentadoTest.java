package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.jhoel0521.studyplan.application.service.SesionBloqueo;

/**
 * Girar la pantalla no bloquea la app, pero mandarla a segundo plano y volver sí (con el bloqueo activo).
 *
 * <p>Verifica: HU-04 (pedir autenticación al volver a primer plano, sin contar la rotación).
 */
@RunWith(AndroidJUnit4.class)
public class ContadorActividadesInstrumentadoTest {

    @Test
    public void girarLaPantallaNoBloqueaPeroIrASegundoPlanoSi() {
        SesionBloqueo sesion = new SesionBloqueo();
        sesion.alCambiarBloqueo(true);
        sesion.desbloquear();
        ContadorActividades contador = new ContadorActividades(sesion);
        Pruebas.app().registerActivityLifecycleCallbacks(contador);
        try (ActivityScenario<PerfilInicialActivity> escenario = ActivityScenario.launch(PerfilInicialActivity.class)) {
            escenario.recreate();
            assertFalse("Girar la pantalla no debe bloquear", sesion.estaBloqueada());

            escenario.moveToState(Lifecycle.State.CREATED);
            escenario.moveToState(Lifecycle.State.RESUMED);
            assertTrue("Volver de segundo plano debe bloquear", sesion.estaBloqueada());
        } finally {
            Pruebas.app().unregisterActivityLifecycleCallbacks(contador);
        }
    }
}
