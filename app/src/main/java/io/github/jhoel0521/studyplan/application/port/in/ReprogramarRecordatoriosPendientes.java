package io.github.jhoel0521.studyplan.application.port.in;

import java.util.List;

import io.github.jhoel0521.studyplan.domain.readmodel.AvisoRecordatorioPendiente;

/**
 * Puerto de entrada para restaurar alarmas de sesiones {@code PENDIENTE} tras un reinicio del
 * dispositivo, actualización de paquete o concesión de alarmas exactas, devolviendo los avisos
 * cuya ventana ya venció antes del inicio para su emisión inmediata ({@code HU-34}).
 */
public interface ReprogramarRecordatoriosPendientes {

    List<AvisoRecordatorioPendiente> ejecutar();
}
