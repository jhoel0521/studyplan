package io.github.jhoel0521.studyplan.adapter.out.persistence.repository;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.sqlite.db.SupportSQLiteDatabase;

import io.github.jhoel0521.studyplan.adapter.out.persistence.StudyPlanDatabase;
import io.github.jhoel0521.studyplan.application.port.out.persistence.AlmacenDatos;
import io.github.jhoel0521.studyplan.domain.readmodel.InstantaneaRespaldo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adaptador de salida: vacía todas las tablas de la base, exporta todo su contenido y lo restaura en una sola
 * transacción. Incluye las tablas de épicas futuras: {@code clearAllTables()} las vacía y las demás se descubren en
 * {@code sqlite_master}.
 *
 * <p>Implementa: HU-38 y RN-21 (borrado), HU-35 (exportar) y HU-37 (restaurar).
 */
public class AlmacenDatosRoom implements AlmacenDatos {

    /** Tablas conocidas, padres antes que hijos (claves foráneas). Las tablas nuevas se agregan después. */
    private static final List<String> ORDEN_CONOCIDO = Arrays.asList(
            "perfil", "ajustes", "materia", "sesion_estudio", "evento_estudio", "tramo_diario", "meta_semanal");

    private final StudyPlanDatabase base;

    public AlmacenDatosRoom(StudyPlanDatabase base) {
        this.base = base;
    }

    @Override
    public void borrarTodo() {
        base.clearAllTables();
        StudyPlanDatabase.asegurarMateriaGeneral(base.getOpenHelper().getWritableDatabase());
    }

    @Override
    public InstantaneaRespaldo exportarTodo(long ahoraUtcMilis) {
        final InstantaneaRespaldo[] resultado = new InstantaneaRespaldo[1];
        // Una transacción da una lectura consistente aunque otra parte de la app escriba a la vez.
        base.runInTransaction(() -> {
            SupportSQLiteDatabase db = base.getOpenHelper().getWritableDatabase();
            Map<String, List<Map<String, Object>>> tablas = new LinkedHashMap<>();
            for (String tabla : tablasEnOrden(db)) {
                tablas.put(tabla, leerFilas(db, tabla));
            }
            resultado[0] = new InstantaneaRespaldo(
                    InstantaneaRespaldo.VERSION_FORMATO_ACTUAL, db.getVersion(), ahoraUtcMilis, tablas);
        });
        return resultado[0];
    }

    @Override
    public void restaurarTodo(InstantaneaRespaldo instantanea) {
        base.runInTransaction(() -> {
            SupportSQLiteDatabase db = base.getOpenHelper().getWritableDatabase();
            // Las claves foráneas se comprueban al confirmar, no fila a fila, y la marca se apaga sola al terminar.
            db.execSQL("PRAGMA defer_foreign_keys = ON");
            List<String> tablas = tablasEnOrden(db);
            for (int i = tablas.size() - 1; i >= 0; i--) {
                db.execSQL("DELETE FROM `" + tablas.get(i) + "`");
            }
            for (String tabla : tablas) {
                for (Map<String, Object> fila : instantanea.filas(tabla)) {
                    // insert() devuelve -1 en vez de lanzar si la fila no cumple las restricciones.
                    if (db.insert(tabla, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, aValores(fila))
                            == -1L) {
                        throw new IllegalStateException("Fila inválida en la tabla " + tabla);
                    }
                }
            }
            StudyPlanDatabase.asegurarMateriaGeneral(db);
            // Se comprueba aquí y no al confirmar: un COMMIT que falla por una clave foránea diferida deja la
            // transacción abierta en Android y los datos a medias visibles. Lanzar ahora provoca el rollback.
            try (Cursor rotas = db.query("PRAGMA foreign_key_check")) {
                if (rotas.moveToFirst()) {
                    throw new IllegalStateException("El respaldo tiene referencias rotas entre tablas");
                }
            }
        });
    }

    private static List<String> tablasEnOrden(SupportSQLiteDatabase db) {
        List<String> existentes = new ArrayList<>();
        try (Cursor c = db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'"
                + " AND name NOT LIKE 'android_%' AND name NOT LIKE 'room_%'")) {
            while (c.moveToNext()) {
                existentes.add(c.getString(0));
            }
        }
        List<String> orden = new ArrayList<>();
        for (String conocida : ORDEN_CONOCIDO) {
            if (existentes.remove(conocida)) {
                orden.add(conocida);
            }
        }
        orden.addAll(existentes);
        return orden;
    }

    private static List<Map<String, Object>> leerFilas(SupportSQLiteDatabase db, String tabla) {
        List<Map<String, Object>> filas = new ArrayList<>();
        try (Cursor c = db.query("SELECT * FROM `" + tabla + "`")) {
            String[] columnas = c.getColumnNames();
            while (c.moveToNext()) {
                Map<String, Object> fila = new LinkedHashMap<>();
                for (int i = 0; i < columnas.length; i++) {
                    fila.put(columnas[i], valorDe(c, i));
                }
                filas.add(fila);
            }
        }
        return filas;
    }

    private static Object valorDe(Cursor c, int indice) {
        switch (c.getType(indice)) {
            case Cursor.FIELD_TYPE_INTEGER:
                return c.getLong(indice);
            case Cursor.FIELD_TYPE_FLOAT:
                return c.getDouble(indice);
            case Cursor.FIELD_TYPE_STRING:
                return c.getString(indice);
            default:
                return null;
        }
    }

    private static ContentValues aValores(Map<String, Object> fila) {
        ContentValues valores = new ContentValues();
        for (Map.Entry<String, Object> columna : fila.entrySet()) {
            Object valor = columna.getValue();
            if (valor == null) {
                valores.putNull(columna.getKey());
            } else if (valor instanceof Long) {
                valores.put(columna.getKey(), (Long) valor);
            } else if (valor instanceof Double) {
                valores.put(columna.getKey(), (Double) valor);
            } else if (valor instanceof Boolean) {
                valores.put(columna.getKey(), ((Boolean) valor) ? 1L : 0L);
            } else {
                valores.put(columna.getKey(), valor.toString());
            }
        }
        return valores;
    }
}
