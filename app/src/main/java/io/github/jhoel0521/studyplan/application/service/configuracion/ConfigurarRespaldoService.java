package io.github.jhoel0521.studyplan.application.service.configuracion;

import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.configuracion.ConfigurarRespaldo;
import io.github.jhoel0521.studyplan.application.port.out.persistence.ConfiguracionRespaldoRepository;
import io.github.jhoel0521.studyplan.application.port.out.system.AlmacenCredencialRespaldo;
import io.github.jhoel0521.studyplan.application.port.out.system.ProgramadorRespaldo;
import io.github.jhoel0521.studyplan.domain.enumeration.FrecuenciaRespaldo;
import io.github.jhoel0521.studyplan.domain.readmodel.ConfiguracionRespaldo;
import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;

/**
 * Implementa {@link ConfigurarRespaldo}: guarda cuenta, frecuencia y contraseña, y mantiene la tarea periódica en
 * sintonía con la frecuencia. El respaldo nace apagado: sin cuenta y con frecuencia {@code NUNCA}.
 *
 * <p>Implementa: HU-35, HU-36, RN-19 y RN-20.
 */
public final class ConfigurarRespaldoService implements ConfigurarRespaldo {

    private final ConfiguracionRespaldoRepository configuracion;
    private final AlmacenCredencialRespaldo credencial;
    private final ProgramadorRespaldo programador;

    public ConfigurarRespaldoService(
            ConfiguracionRespaldoRepository configuracion,
            AlmacenCredencialRespaldo credencial,
            ProgramadorRespaldo programador) {
        this.configuracion = configuracion;
        this.credencial = credencial;
        this.programador = programador;
    }

    @Override
    public ConfiguracionRespaldo consultar() {
        return configuracion.leer();
    }

    @Override
    public void vincularCuenta(String correo) {
        String limpio = correo == null ? "" : correo.trim();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El correo de la cuenta no puede estar vacío");
        }
        configuracion.guardarCorreo(limpio);
    }

    @Override
    public void desvincular() {
        programador.cancelar();
        credencial.borrar();
        configuracion.limpiar();
    }

    @Override
    public ResultadoRespaldo cambiarFrecuencia(FrecuenciaRespaldo frecuencia) {
        Objects.requireNonNull(frecuencia);
        if (frecuencia != FrecuenciaRespaldo.NUNCA && !configuracion.leer().hayCuenta()) {
            return ResultadoRespaldo.SIN_CUENTA;
        }
        configuracion.guardarFrecuencia(frecuencia);
        if (frecuencia.esAutomatica()) {
            programador.programar(frecuencia);
        } else {
            programador.cancelar();
        }
        return ResultadoRespaldo.OK;
    }

    @Override
    public ResultadoRespaldo definirContrasena(char[] contrasena) {
        if (contrasena == null || contrasena.length == 0) {
            return ResultadoRespaldo.CONTRASENA_REQUERIDA;
        }
        credencial.guardar(contrasena);
        configuracion.guardarConContrasena(true);
        return ResultadoRespaldo.OK;
    }

    @Override
    public void quitarContrasena() {
        credencial.borrar();
        configuracion.guardarConContrasena(false);
    }
}
