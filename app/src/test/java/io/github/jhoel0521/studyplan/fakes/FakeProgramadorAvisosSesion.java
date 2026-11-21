package io.github.jhoel0521.studyplan.fakes;

import io.github.jhoel0521.studyplan.application.port.out.system.ProgramadorAvisosSesion;

/**
 * Falso de {@link ProgramadorAvisosSesion} para pruebas unitarias (HU-23, HU-42, HU-43).
 */
public final class FakeProgramadorAvisosSesion implements ProgramadorAvisosSesion {

    public Long ultimaSesionId;
    public TipoHito ultimoTipo;
    public Long ultimoInstante;
    public String ultimaMateria;
    public int cancelaciones;

    @Override
    public void programarSiguienteHito(
            long sesionId, TipoHito tipo, long instanteDisparoMilis, String nombreMateria) {
        this.ultimaSesionId = sesionId;
        this.ultimoTipo = tipo;
        this.ultimoInstante = instanteDisparoMilis;
        this.ultimaMateria = nombreMateria;
    }

    @Override
    public void cancelarAvisos(long sesionId) {
        this.cancelaciones++;
        if (ultimaSesionId != null && ultimaSesionId == sesionId) {
            ultimoTipo = null;
            ultimoInstante = null;
        }
    }
}
