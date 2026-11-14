package io.github.jhoel0521.studyplan.fakes;

import io.github.jhoel0521.studyplan.application.port.out.AjustesRepository;
import io.github.jhoel0521.studyplan.domain.Ajustes;

/**
 * Puerto de salida falso en memoria para las pruebas del núcleo. Parte de los ajustes por defecto, como el adaptador real.
 *
 * <p>Apoya las pruebas de: HU-03, HU-04 y HU-38.
 */
public final class FakeAjustesRepository implements AjustesRepository {

    private Ajustes ajustes = Ajustes.porDefecto();
    public int guardados;

    @Override
    public Ajustes obtener() {
        return ajustes;
    }

    @Override
    public void guardar(Ajustes ajustes) {
        this.ajustes = ajustes;
        guardados++;
    }
}
