package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertNotNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * La raíz de composición expone todos los servicios sin nulos.
 *
 * <p>Verifica: HU-01, HU-02, HU-03, HU-04 y HU-38 (todos los casos de uso quedan conectados a sus adaptadores).
 */
@RunWith(AndroidJUnit4.class)
public class StudyPlanApplicationTest {

    @Test
    public void laAplicacionExponeTodosLosServicios() {
        StudyPlanApplication app = Pruebas.app();
        assertNotNull(app.configurarPerfil());
        assertNotNull(app.consultarPerfil());
        assertNotNull(app.consultarAjustes());
        assertNotNull(app.configurarApariencia());
        assertNotNull(app.configurarBloqueo());
        assertNotNull(app.sesionBloqueo());
        assertNotNull(app.consultarEstadoArranque());
        assertNotNull(app.obtenerResumenInicio());
        assertNotNull(app.borrarDatos());
        assertNotNull(app.reloj());
        assertNotNull(app.vencerSesionesPendientes());
        assertNotNull(app.planificarSesion());
        assertNotNull(app.registrarSesionPasada());
        assertNotNull(app.completarSesion());
        assertNotNull(app.registrarSesionTardia());
        assertNotNull(app.reprogramarSesion());
        assertNotNull(app.consultarSesion());
        assertNotNull(app.eliminarSesion());
        assertNotNull(app.ejecutor());
        assertNotNull(app.fabrica());
    }
}
