package io.github.jhoel0521.studyplan.application.port.in;

import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Puerto de entrada: activar o desactivar No molestar solo para la sesión {@code EN_CURSO}, sin
 * cambiar el ajuste global (HU-24, RN-18, RNF-18).
 */
public interface AlternarNoMolestarSesion {

    /**
     * @param sesionId sesión En curso sobre la que se actúa
     * @return el estado final de No molestar para esa sesión ({@code true} = activado), o un error
     *     si la sesión no está En curso o falta el permiso del sistema
     */
    Resultado<Boolean> ejecutar(long sesionId);
}
