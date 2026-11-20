package io.github.jhoel0521.studyplan.adapter.out.system;

/**
 * Publica o retira la notificación de la sesión En curso según el estado guardado de la sesión
 * (HU-49, HU-40). Se implementa con {@code NotificadorSesion}; es una interfaz para poder probar
 * {@link ProgramadorAvisosConNotificacion} sin Android.
 */
public interface NotificacionSesion {

    /** Relee el estado de la sesión y actualiza o retira la notificación. Fuera del hilo principal. */
    void refrescar(long sesionId);
}
