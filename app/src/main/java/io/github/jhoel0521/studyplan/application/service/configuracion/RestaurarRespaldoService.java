package io.github.jhoel0521.studyplan.application.service.configuracion;

import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.configuracion.RestaurarRespaldo;
import io.github.jhoel0521.studyplan.application.port.in.recordatorios.ReprogramarRecordatoriosPendientes;
import io.github.jhoel0521.studyplan.application.port.out.persistence.AlmacenDatos;
import io.github.jhoel0521.studyplan.application.port.out.persistence.ConfiguracionRespaldoRepository;
import io.github.jhoel0521.studyplan.application.port.out.system.CifradorRespaldo;
import io.github.jhoel0521.studyplan.application.port.out.system.ClienteGoogleDrive;
import io.github.jhoel0521.studyplan.domain.readmodel.InstantaneaRespaldo;
import io.github.jhoel0521.studyplan.domain.readmodel.RespaldoDescargado;
import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;
import io.github.jhoel0521.studyplan.domain.service.NormalizadorRespaldo;
import io.github.jhoel0521.studyplan.domain.service.SerializadorRespaldo;

/**
 * Implementa {@link RestaurarRespaldo}: baja el archivo, lo abre y valida todo antes de tocar los datos locales; solo
 * entonces los reemplaza en una transacción, normaliza lo que depende del teléfono y reprograma los recordatorios. Ante
 * cualquier fallo (contraseña, versión, archivo dañado, sin conexión) los datos locales quedan como estaban.
 *
 * <p>Implementa: HU-37 y HU-36.
 */
public final class RestaurarRespaldoService implements RestaurarRespaldo {

    private final AlmacenDatos almacen;
    private final ClienteGoogleDrive drive;
    private final CifradorRespaldo cifrador;
    private final ConfiguracionRespaldoRepository configuracion;
    private final ReprogramarRecordatoriosPendientes reprogramarRecordatorios;

    public RestaurarRespaldoService(
            AlmacenDatos almacen,
            ClienteGoogleDrive drive,
            CifradorRespaldo cifrador,
            ConfiguracionRespaldoRepository configuracion,
            ReprogramarRecordatoriosPendientes reprogramarRecordatorios) {
        this.almacen = almacen;
        this.drive = drive;
        this.cifrador = cifrador;
        this.configuracion = configuracion;
        this.reprogramarRecordatorios = reprogramarRecordatorios;
    }

    @Override
    public ResultadoRespaldo ejecutar(char[] contrasena) {
        if (!configuracion.leer().hayCuenta()) {
            return ResultadoRespaldo.SIN_CUENTA;
        }
        RespaldoDescargado descarga = drive.descargar();
        if (descarga.resultado() != ResultadoRespaldo.OK) {
            return descarga.resultado();
        }
        byte[] archivo = descarga.datos();
        if (!cifrador.reconoce(archivo)) {
            return ResultadoRespaldo.RESPALDO_CORRUPTO;
        }
        boolean cifrado = cifrador.estaCifrado(archivo);
        if (cifrado && (contrasena == null || contrasena.length == 0)) {
            return ResultadoRespaldo.CONTRASENA_REQUERIDA;
        }
        Optional<byte[]> datos = cifrador.desempaquetar(archivo, cifrado ? contrasena : null);
        if (!datos.isPresent()) {
            return ResultadoRespaldo.CONTRASENA_INCORRECTA;
        }
        InstantaneaRespaldo instantanea;
        try {
            instantanea = SerializadorRespaldo.deserializar(datos.get());
        } catch (IllegalArgumentException e) {
            return ResultadoRespaldo.RESPALDO_CORRUPTO;
        }
        if (instantanea.versionFormato() > InstantaneaRespaldo.VERSION_FORMATO_ACTUAL) {
            return ResultadoRespaldo.VERSION_NO_SOPORTADA;
        }
        almacen.restaurarTodo(NormalizadorRespaldo.normalizar(instantanea));
        if (reprogramarRecordatorios != null) {
            reprogramarRecordatorios.ejecutar();
        }
        return ResultadoRespaldo.OK;
    }
}
