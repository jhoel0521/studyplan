package io.github.jhoel0521.studyplan.adapter.out.persistence;

import io.github.jhoel0521.studyplan.application.port.out.AjustesRepository;
import io.github.jhoel0521.studyplan.domain.Ajustes;

/**
 * Adaptador de salida: guarda los ajustes en Room. Sin fila guardada devuelve los valores por defecto.
 *
 * <p>Implementa: HU-03 y HU-04.
 */
public class AjustesRepositoryRoom implements AjustesRepository {

    private final AjustesDao dao;

    public AjustesRepositoryRoom(AjustesDao dao) {
        this.dao = dao;
    }

    @Override
    public Ajustes obtener() {
        AjustesEntity fila = dao.obtener();
        return fila == null ? Ajustes.porDefecto() : Mapeadores.aDominio(fila);
    }

    @Override
    public void guardar(Ajustes ajustes) {
        dao.guardar(Mapeadores.aEntidad(ajustes));
    }
}
