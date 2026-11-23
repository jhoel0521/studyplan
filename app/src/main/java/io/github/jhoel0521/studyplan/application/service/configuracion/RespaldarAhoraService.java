package io.github.jhoel0521.studyplan.application.service.configuracion;

import io.github.jhoel0521.studyplan.application.port.in.configuracion.RespaldarAhora;
import io.github.jhoel0521.studyplan.application.port.out.persistence.AlmacenDatos;
import io.github.jhoel0521.studyplan.application.port.out.persistence.ConfiguracionRespaldoRepository;
import io.github.jhoel0521.studyplan.application.port.out.system.AlmacenCredencialRespaldo;
import io.github.jhoel0521.studyplan.application.port.out.system.CifradorRespaldo;
import io.github.jhoel0521.studyplan.application.port.out.system.ClienteGoogleDrive;
import io.github.jhoel0521.studyplan.application.port.out.system.Reloj;
import io.github.jhoel0521.studyplan.domain.readmodel.ConfiguracionRespaldo;
import io.github.jhoel0521.studyplan.domain.readmodel.InstantaneaRespaldo;
import io.github.jhoel0521.studyplan.domain.readmodel.MetadatosRespaldo;
import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;
import io.github.jhoel0521.studyplan.domain.service.SerializadorRespaldo;

/**
 * Implementa {@link RespaldarAhora}: exporta todos los datos, los cifra si hay contraseña y los sube a Drive. Si el
 * estudiante activó la contraseña pero no se puede recuperar, no sube nada en claro. Guarda el resultado para que
 * Ajustes muestre si el último intento falló.
 *
 * <p>Implementa: HU-35 y HU-36.
 */
public final class RespaldarAhoraService implements RespaldarAhora {

    private final AlmacenDatos almacen;
    private final ClienteGoogleDrive drive;
    private final CifradorRespaldo cifrador;
    private final AlmacenCredencialRespaldo credencial;
    private final ConfiguracionRespaldoRepository configuracion;
    private final Reloj reloj;

    public RespaldarAhoraService(
            AlmacenDatos almacen,
            ClienteGoogleDrive drive,
            CifradorRespaldo cifrador,
            AlmacenCredencialRespaldo credencial,
            ConfiguracionRespaldoRepository configuracion,
            Reloj reloj) {
        this.almacen = almacen;
        this.drive = drive;
        this.cifrador = cifrador;
        this.credencial = credencial;
        this.configuracion = configuracion;
        this.reloj = reloj;
    }

    @Override
    public ResultadoRespaldo ejecutar() {
        ResultadoRespaldo resultado = intentar();
        configuracion.guardarUltimoResultado(resultado);
        return resultado;
    }

    private ResultadoRespaldo intentar() {
        ConfiguracionRespaldo estado = configuracion.leer();
        if (!estado.hayCuenta()) {
            return ResultadoRespaldo.SIN_CUENTA;
        }
        char[] contrasena = null;
        if (estado.conContrasena()) {
            contrasena = credencial.obtener();
            if (contrasena == null) {
                return ResultadoRespaldo.CONTRASENA_REQUERIDA;
            }
        }
        long ahora = reloj.ahoraMilis();
        InstantaneaRespaldo instantanea = almacen.exportarTodo(ahora);
        byte[] archivo = cifrador.empaquetar(SerializadorRespaldo.serializar(instantanea), contrasena);
        ResultadoRespaldo subida = drive.subir(archivo);
        if (subida == ResultadoRespaldo.OK) {
            configuracion.guardarUltimoRespaldo(new MetadatosRespaldo(ahora, archivo.length, contrasena != null));
        }
        return subida;
    }
}
