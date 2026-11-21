package io.github.jhoel0521.studyplan.adapter.out.persistence;

import io.github.jhoel0521.studyplan.application.port.out.persistence.AlmacenDatos;

/**
 * Adaptador de salida: vacía todas las tablas de la base. Incluye las tablas de épicas futuras porque usa
 * {@code clearAllTables()}.
 *
 * <p>Implementa: HU-38 y RN-21.
 */
public class AlmacenDatosRoom implements AlmacenDatos {

    private final StudyPlanDatabase base;

    public AlmacenDatosRoom(StudyPlanDatabase base) {
        this.base = base;
    }

    @Override
    public void borrarTodo() {
        base.clearAllTables();
        StudyPlanDatabase.asegurarMateriaGeneral(base.getOpenHelper().getWritableDatabase());
    }
}
