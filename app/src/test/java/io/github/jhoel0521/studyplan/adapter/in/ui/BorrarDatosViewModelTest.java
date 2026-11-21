package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertEquals;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.github.jhoel0521.studyplan.application.service.configuracion.BorrarDatosService;
import io.github.jhoel0521.studyplan.application.service.configuracion.SesionBloqueo;
import io.github.jhoel0521.studyplan.fakes.FakeAlmacenDatos;

/**
 * Pruebas del ViewModel de borrado: la doble confirmación solo borra al confirmar el segundo paso.
 *
 * <p>Verifica: HU-38 (alcance «teléfono»), RN-21 (irreversible) y RNF-13.
 */
public class BorrarDatosViewModelTest {

    @Rule
    public InstantTaskExecutorRule reglaInstantanea = new InstantTaskExecutorRule();

    private FakeAlmacenDatos almacen;
    private BorrarDatosViewModel modelo;

    @Before
    public void preparar() {
        almacen = new FakeAlmacenDatos();
        modelo = new BorrarDatosViewModel(new BorrarDatosService(almacen, new SesionBloqueo()), Runnable::run);
    }

    @Test
    public void empiezaSinNingunPasoActivo() {
        assertEquals(BorrarDatosViewModel.Paso.NINGUNO, modelo.paso().getValue());
    }

    @Test
    public void pedirElBorradoMuestraLaPrimeraConfirmacion() {
        modelo.pedirBorrado();
        assertEquals(BorrarDatosViewModel.Paso.PRIMERA_CONFIRMACION, modelo.paso().getValue());
    }

    @Test
    public void confirmarLaPrimeraPasaALaSegundaSinBorrar() {
        modelo.pedirBorrado();
        modelo.confirmarPrimera();
        assertEquals(BorrarDatosViewModel.Paso.SEGUNDA_CONFIRMACION, modelo.paso().getValue());
        assertEquals(0, almacen.borrados);
    }

    @Test
    public void cancelarEnLaSegundaConfirmacionNoBorra() {
        modelo.pedirBorrado();
        modelo.confirmarPrimera();
        modelo.cancelar();
        assertEquals(BorrarDatosViewModel.Paso.NINGUNO, modelo.paso().getValue());
        assertEquals(0, almacen.borrados);
    }

    @Test
    public void confirmarLaSegundaBorraUnaSolaVezYTermina() {
        modelo.pedirBorrado();
        modelo.confirmarPrimera();
        modelo.confirmarSegunda();
        assertEquals(1, almacen.borrados);
        assertEquals(BorrarDatosViewModel.Paso.BORRADO, modelo.paso().getValue());
    }
}
