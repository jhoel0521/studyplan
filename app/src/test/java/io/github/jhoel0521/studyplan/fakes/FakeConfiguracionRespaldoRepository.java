package io.github.jhoel0521.studyplan.fakes;

import io.github.jhoel0521.studyplan.application.port.out.persistence.ConfiguracionRespaldoRepository;
import io.github.jhoel0521.studyplan.domain.enumeration.FrecuenciaRespaldo;
import io.github.jhoel0521.studyplan.domain.readmodel.ConfiguracionRespaldo;
import io.github.jhoel0521.studyplan.domain.readmodel.MetadatosRespaldo;
import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;

/**
 * Configuración del respaldo en memoria; nace como una instalación nueva (sin cuenta, frecuencia NUNCA).
 *
 * <p>Apoya las pruebas de: HU-35 y HU-36.
 */
public final class FakeConfiguracionRespaldoRepository implements ConfiguracionRespaldoRepository {

    private String correo;
    private FrecuenciaRespaldo frecuencia = FrecuenciaRespaldo.NUNCA;
    private boolean conContrasena;
    private MetadatosRespaldo ultimoRespaldo;
    private ResultadoRespaldo ultimoResultado;

    @Override
    public ConfiguracionRespaldo leer() {
        return new ConfiguracionRespaldo(correo, frecuencia, conContrasena, ultimoRespaldo, ultimoResultado);
    }

    @Override
    public void guardarCorreo(String nuevo) {
        correo = nuevo;
    }

    @Override
    public void guardarFrecuencia(FrecuenciaRespaldo nueva) {
        frecuencia = nueva;
    }

    @Override
    public void guardarConContrasena(boolean valor) {
        conContrasena = valor;
    }

    @Override
    public void guardarUltimoRespaldo(MetadatosRespaldo metadatos) {
        ultimoRespaldo = metadatos;
    }

    @Override
    public void guardarUltimoResultado(ResultadoRespaldo resultado) {
        ultimoResultado = resultado;
    }

    @Override
    public void limpiar() {
        correo = null;
        frecuencia = FrecuenciaRespaldo.NUNCA;
        conContrasena = false;
        ultimoRespaldo = null;
        ultimoResultado = null;
    }
}
