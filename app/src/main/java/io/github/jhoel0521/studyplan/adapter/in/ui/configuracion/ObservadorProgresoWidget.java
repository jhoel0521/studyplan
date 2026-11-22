package io.github.jhoel0521.studyplan.adapter.in.ui.configuracion;

import androidx.room.InvalidationTracker;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Observa las tablas de las que depende el widget (sesiones, metas, perfil y materias) y pide que se redibuje cuando
 * cambian, sin tocar ningún caso de uso. Los avisos seguidos se agrupan en un solo refresco.
 *
 * <p>Implementa: HU-05 (el widget actualiza las horas realizadas al completar una sesión).
 */
public final class ObservadorProgresoWidget extends InvalidationTracker.Observer {

    private final Executor ejecutor;
    private final Runnable refresco;
    private final AtomicBoolean pendiente = new AtomicBoolean(false);

    /**
     * @param ejecutor hilo en segundo plano donde se pide el refresco
     * @param refresco lo que redibuja los widgets (en producción, {@link WidgetProgresoProvider#refrescarTodos})
     */
    public ObservadorProgresoWidget(Executor ejecutor, Runnable refresco) {
        super("sesion_estudio", "meta_semanal", "perfil", "materia");
        this.ejecutor = ejecutor;
        this.refresco = refresco;
    }

    @Override
    public void onInvalidated(Set<String> tablas) {
        if (!pendiente.compareAndSet(false, true)) {
            return;
        }
        ejecutor.execute(() -> {
            pendiente.set(false);
            refresco.run();
        });
    }
}
