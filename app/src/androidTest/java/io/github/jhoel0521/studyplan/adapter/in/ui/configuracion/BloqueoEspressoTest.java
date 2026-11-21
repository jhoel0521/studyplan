package io.github.jhoel0521.studyplan.adapter.in.ui.configuracion;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.entity.Ajustes;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.Pruebas;

/**
 * Con el bloqueo activo, volver a primer plano muestra la pantalla de autenticación y no deja ver los datos de
 * Inicio. El diálogo del sistema (huella o PIN) no se automatiza: se verifica a mano.
 *
 * <p>Verifica: HU-04 (pedir autenticación al volver a primer plano) y RNF-12 (nunca mostrar datos sin autenticar).
 */
@RunWith(AndroidJUnit4.class)
public class BloqueoEspressoTest {

    @Before
    public void conPerfilYBloqueoActivo() {
        Pruebas.reiniciarEstado();
        Pruebas.app().configurarPerfil().ejecutar("Ana", 20);
        // El teléfono de pruebas puede no tener bloqueo de pantalla: se escribe el ajuste usando la instancia única de Room.
        Pruebas.app().ajustesRepository().guardar(Ajustes.porDefecto().conBloqueoActivo(true));
        // Se lee el ajuste como al arrancar y se simula volver de segundo plano: la sesión queda bloqueada.
        Pruebas.app().consultarEstadoArranque().ejecutar();
        Pruebas.app().sesionBloqueo().alVolverAPrimerPlano();
    }

    @After
    public void restablecer() {
        Pruebas.reiniciarEstado();
    }

    @Test
    @SuppressWarnings("unused") // el escenario solo se usa para cerrarse con try-with-resources
    public void alVolverAPrimerPlanoSePideAutenticacionYNoSeVenLosDatos() throws InterruptedException {
        try (ActivityScenario<MainActivity> escenario = ActivityScenario.launch(MainActivity.class)) {
            // La sesión nace bloqueada: se muestra la autenticación y nunca el saludo.
            Pruebas.esperarVisible(withId(R.id.botonReintentar));
            assertTrue(Pruebas.app().sesionBloqueo().estaBloqueada());
            onView(withId(R.id.saludo)).check(doesNotExist());
        }
    }
}
