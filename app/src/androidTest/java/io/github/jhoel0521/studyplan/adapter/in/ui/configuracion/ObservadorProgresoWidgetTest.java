package io.github.jhoel0521.studyplan.adapter.in.ui.configuracion;

import static org.junit.Assert.assertTrue;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.github.jhoel0521.studyplan.adapter.in.ui.StudyPlanApplication;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.Pruebas;
import io.github.jhoel0521.studyplan.adapter.out.persistence.StudyPlanDatabase;

/**
 * Comprueba que un cambio en los datos del widget (aquí, el perfil) dispara el refresco.
 *
 * <p>Verifica: HU-05 (el widget se actualiza al cambiar las horas o la meta).
 */
public class ObservadorProgresoWidgetTest {

    @Before
    public void estadoLimpio() {
        Pruebas.reiniciarEstado();
    }

    @Test
    public void cambiarElPerfilDisparaElRefresco() throws Exception {
        StudyPlanApplication app = Pruebas.app();
        CountDownLatch refresco = new CountDownLatch(1);
        ObservadorProgresoWidget observador = new ObservadorProgresoWidget(app.ejecutor(), refresco::countDown);
        StudyPlanDatabase base = StudyPlanDatabase.abrir(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
        base.getInvalidationTracker().addObserver(observador);
        try {
            app.enviarTarea(() -> app.configurarPerfil().ejecutar("Ana", 20)).get(8, TimeUnit.SECONDS);
            assertTrue("No se pidió el refresco del widget", refresco.await(8, TimeUnit.SECONDS));
        } finally {
            base.getInvalidationTracker().removeObserver(observador);
        }
    }
}
