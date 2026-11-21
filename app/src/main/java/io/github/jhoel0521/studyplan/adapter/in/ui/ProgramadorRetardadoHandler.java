package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.os.Handler;
import android.os.Looper;

/**
 * {@link ProgramadorRetardado} sobre el hilo principal. El temporizador no avanza con el teléfono dormido, por eso
 * {@link SalidaConGracia} siempre vuelve a comparar con el reloj real al volver a la app (HU-40).
 */
final class ProgramadorRetardadoHandler implements ProgramadorRetardado {

    private final Handler manejador = new Handler(Looper.getMainLooper());

    @Override
    public void programar(Runnable tarea, long retrasoMs) {
        manejador.postDelayed(tarea, retrasoMs);
    }

    @Override
    public void cancelar(Runnable tarea) {
        manejador.removeCallbacks(tarea);
    }
}
