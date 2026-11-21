package io.github.jhoel0521.studyplan.adapter.in.ui.comun;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Doble de {@link ProgramadorRetardado} con reloj manual: {@link #avanzar(long)} mueve el tiempo y ejecuta lo que
 * venza (HU-40).
 */
final class FakeProgramadorRetardado implements ProgramadorRetardado {

    private static final class Tarea {
        final Runnable runnable;
        final long vence;

        Tarea(Runnable runnable, long vence) {
            this.runnable = runnable;
            this.vence = vence;
        }
    }

    private final long[] reloj;
    private final List<Tarea> tareas = new ArrayList<>();

    /** @param reloj reloj compartido de una sola posición, en milisegundos */
    FakeProgramadorRetardado(long[] reloj) {
        this.reloj = reloj;
    }

    @Override
    public void programar(Runnable tarea, long retrasoMs) {
        tareas.add(new Tarea(tarea, reloj[0] + retrasoMs));
    }

    @Override
    public void cancelar(Runnable tarea) {
        for (Iterator<Tarea> it = tareas.iterator(); it.hasNext(); ) {
            if (it.next().runnable == tarea) {
                it.remove();
            }
        }
    }

    /** Avanza el tiempo ejecutando, en orden, las tareas que vencen. */
    void avanzar(long ms) {
        long meta = reloj[0] + ms;
        while (true) {
            Tarea siguiente = null;
            for (Tarea t : tareas) {
                if (t.vence <= meta && (siguiente == null || t.vence < siguiente.vence)) {
                    siguiente = t;
                }
            }
            if (siguiente == null) {
                break;
            }
            tareas.remove(siguiente);
            reloj[0] = Math.max(reloj[0], siguiente.vence);
            siguiente.runnable.run();
        }
        reloj[0] = meta;
    }

    /** Avanza el reloj sin ejecutar tareas: el teléfono durmió y el temporizador no corrió. */
    void dormir(long ms) {
        reloj[0] += ms;
    }
}
