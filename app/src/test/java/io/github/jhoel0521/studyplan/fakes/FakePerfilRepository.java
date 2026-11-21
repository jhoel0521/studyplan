package io.github.jhoel0521.studyplan.fakes;

import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.out.persistence.PerfilRepository;
import io.github.jhoel0521.studyplan.domain.entity.Perfil;

/**
 * Puerto de salida falso en memoria para las pruebas del núcleo.
 *
 * <p>Apoya las pruebas de: HU-01, HU-02, RN-01.
 */
public final class FakePerfilRepository implements PerfilRepository {

    private Perfil perfil;
    public int guardados;

    @Override
    public Optional<Perfil> obtener() {
        return Optional.ofNullable(perfil);
    }

    @Override
    public void guardar(Perfil perfil) {
        this.perfil = perfil;
        guardados++;
    }
}
