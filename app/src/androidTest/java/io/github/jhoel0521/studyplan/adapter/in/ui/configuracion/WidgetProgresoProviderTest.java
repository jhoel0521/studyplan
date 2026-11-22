package io.github.jhoel0521.studyplan.adapter.in.ui.configuracion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RemoteViews;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.readmodel.DetalleSesionInicio;
import io.github.jhoel0521.studyplan.domain.readmodel.ResumenInicio;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;

/**
 * Infla las vistas del widget 1×4 en cada estado, para detectar vistas no admitidas por
 * {@code RemoteViews} o recursos que faltan.
 *
 * <p>Verifica: HU-05 (RNF-19).
 */
public class WidgetProgresoProviderTest {

    private static ResumenInicio resumen(double horas, double meta, int proximas) {
        Fecha hoy = Fecha.de(2026, 11, 22).valor();
        DetalleSesionInicio una = new DetalleSesionInicio(1L, 2L, "Historia", 0xFF1E88E5, hoy,
                Hora.de(10, 0).valor(), Hora.de(11, 30).valor(), 90, null, CodigoEstadoSesion.PENDIENTE);
        DetalleSesionInicio otra = new DetalleSesionInicio(3L, 4L, "Álgebra", 0xFF43A047, hoy.sumarDias(1),
                Hora.de(15, 0).valor(), Hora.de(16, 0).valor(), 60, null, CodigoEstadoSesion.PENDIENTE);
        List<DetalleSesionInicio> lista = Arrays.asList(una, otra).subList(0, proximas);
        return new ResumenInicio("Ana", horas, meta, hoy, lista.isEmpty() ? null : lista.get(0),
                Collections.emptyList(), null, lista);
    }

    private static void inflar(ModeloWidgetProgreso modelo, int visibilidadMensajeEsperada, int visibilidadFila2) {
        Context contexto = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteViews vistas = WidgetProgresoProvider.construir(contexto, modelo);
        assertNotNull(vistas);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View raiz = vistas.apply(contexto, new FrameLayout(contexto));
            assertEquals(visibilidadMensajeEsperada, raiz.findViewById(R.id.widget_mensaje).getVisibility());
            assertEquals(visibilidadFila2, raiz.findViewById(R.id.widget_fila_proxima2).getVisibility());
            assertNotNull(raiz.findViewById(R.id.widget_raiz).getContentDescription());
        });
    }

    @Test
    public void inflaElWidgetEnTodosLosEstados() {
        inflar(ModeloWidgetProgreso.de(Optional.of(resumen(12, 20, 2))), View.GONE, View.VISIBLE);
        inflar(ModeloWidgetProgreso.de(Optional.of(resumen(12, 20, 1))), View.GONE, View.GONE);
        inflar(ModeloWidgetProgreso.de(Optional.of(resumen(5, 20, 0))), View.VISIBLE, View.GONE);
        inflar(ModeloWidgetProgreso.de(Optional.of(resumen(0, 0, 2))), View.GONE, View.VISIBLE);
        inflar(ModeloWidgetProgreso.de(Optional.empty()), View.VISIBLE, View.GONE);
    }
}
