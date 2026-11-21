package io.github.jhoel0521.studyplan.application.service;

import io.github.jhoel0521.studyplan.application.port.in.configuracion.ConfigurarPerfil;
import io.github.jhoel0521.studyplan.application.port.out.PerfilRepository;
import io.github.jhoel0521.studyplan.domain.entity.Perfil;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Implementa el caso de uso {@link ConfigurarPerfil}: valida con el dominio y solo guarda si los datos son válidos.
 *
 * <p>Implementa: HU-01, RN-01, RN-14 y RNF-14.
 */
public final class ConfigurarPerfilService implements ConfigurarPerfil {

    private final PerfilRepository repositorio;

    public ConfigurarPerfilService(PerfilRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Resultado<Perfil> ejecutar(String nombre, double metaSemanalHoras) {
        Resultado<Perfil> resultado = Perfil.crear(nombre, metaSemanalHoras);
        if (resultado.esValido()) {
            repositorio.guardar(resultado.valor());
        }
        return resultado;
    }
}
