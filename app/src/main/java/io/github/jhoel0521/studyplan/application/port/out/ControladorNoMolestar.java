package io.github.jhoel0521.studyplan.application.port.out;

/**
 * Puerto de salida para aplicar y restaurar de forma segura el modo No molestar del dispositivo
 * (HU-24, RN-18, RNF-18).
 */
public interface ControladorNoMolestar {

    boolean tienePermiso();

    void aplicar(boolean activar);

    void restaurarSiAplica();
}
