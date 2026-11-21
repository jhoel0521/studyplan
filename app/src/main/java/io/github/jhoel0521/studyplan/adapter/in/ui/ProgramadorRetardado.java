package io.github.jhoel0521.studyplan.adapter.in.ui;

/**
 * Programa y cancela tareas con retraso. Existe para que la lógica de la ventana de gracia de la salida
 * ({@link SalidaConGracia}) no dependa de {@code android.os.Handler} y se pueda probar en la JVM (HU-40).
 */
interface ProgramadorRetardado {

    /** Ejecuta {@code tarea} una sola vez tras {@code retrasoMs} milisegundos. */
    void programar(Runnable tarea, long retrasoMs);

    /** Cancela la tarea si aún no se ejecutó. */
    void cancelar(Runnable tarea);
}
