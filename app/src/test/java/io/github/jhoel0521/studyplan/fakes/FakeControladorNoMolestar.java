package io.github.jhoel0521.studyplan.fakes;

import io.github.jhoel0521.studyplan.application.port.out.system.ControladorNoMolestar;

/**
 * Falso de {@link ControladorNoMolestar} para pruebas unitarias (HU-24, RN-18).
 */
public final class FakeControladorNoMolestar implements ControladorNoMolestar {

    public boolean permiso = true;
    public boolean activo;
    public int restauraciones;

    @Override
    public boolean tienePermiso() {
        return permiso;
    }

    @Override
    public void aplicar(boolean activar) {
        if (permiso) {
            this.activo = activar;
        }
    }

    @Override
    public void restaurarSiAplica() {
        restauraciones++;
        if (permiso) {
            this.activo = false;
        }
    }
}
