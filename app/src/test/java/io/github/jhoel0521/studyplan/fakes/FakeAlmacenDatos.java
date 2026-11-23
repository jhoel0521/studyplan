package io.github.jhoel0521.studyplan.fakes;

import io.github.jhoel0521.studyplan.application.port.out.persistence.AlmacenDatos;
import io.github.jhoel0521.studyplan.domain.readmodel.InstantaneaRespaldo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Almacén de datos falso: cuenta los borrados y guarda en memoria la instantánea que se exporta y la que se restaura.
 *
 * <p>Apoya las pruebas de: HU-38, HU-35 y HU-37.
 */
public final class FakeAlmacenDatos implements AlmacenDatos {

    public int borrados;
    public int restauraciones;
    public InstantaneaRespaldo ultimaRestaurada;
    /** Lo que {@link #exportarTodo(long)} devuelve; por defecto, un perfil de ejemplo. */
    public Map<String, List<Map<String, Object>>> tablas = tablasDeEjemplo();

    @Override
    public void borrarTodo() {
        borrados++;
    }

    @Override
    public InstantaneaRespaldo exportarTodo(long ahoraUtcMilis) {
        return new InstantaneaRespaldo(InstantaneaRespaldo.VERSION_FORMATO_ACTUAL, 8, ahoraUtcMilis, tablas);
    }

    @Override
    public void restaurarTodo(InstantaneaRespaldo instantanea) {
        restauraciones++;
        ultimaRestaurada = instantanea;
    }

    public static Map<String, List<Map<String, Object>>> tablasDeEjemplo() {
        Map<String, List<Map<String, Object>>> tablas = new LinkedHashMap<>();
        Map<String, Object> perfil = new LinkedHashMap<>();
        perfil.put("id", 1L);
        perfil.put("nombre", "Ana");
        List<Map<String, Object>> filas = new ArrayList<>();
        filas.add(perfil);
        tablas.put("perfil", filas);
        return tablas;
    }
}
