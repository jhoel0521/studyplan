package io.github.jhoel0521.studyplan.adapter.out.system;

import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.out.ProgramadorAvisosSesion;

/**
 * Decorador de {@link ProgramadorAvisosSesion}: además de programar o cancelar los hitos, refresca la
 * notificación de la sesión. Los servicios ya llaman a este puerto en cada cambio de estado, así que la
 * notificación se mantiene al día sin cambiar ninguno de ellos (HU-49, HU-40).
 */
public final class ProgramadorAvisosConNotificacion implements ProgramadorAvisosSesion {

    private final ProgramadorAvisosSesion delegado;
    private final NotificacionSesion notificacion;

    public ProgramadorAvisosConNotificacion(
            ProgramadorAvisosSesion delegado, NotificacionSesion notificacion) {
        this.delegado = Objects.requireNonNull(delegado, "delegado");
        this.notificacion = Objects.requireNonNull(notificacion, "notificacion");
    }

    @Override
    public void programarSiguienteHito(
            long sesionId, TipoHito tipo, long instanteDisparoMilis, String nombreMateria) {
        delegado.programarSiguienteHito(sesionId, tipo, instanteDisparoMilis, nombreMateria);
        notificacion.refrescar(sesionId);
    }

    @Override
    public void cancelarAvisos(long sesionId) {
        delegado.cancelarAvisos(sesionId);
        notificacion.refrescar(sesionId);
    }
}
