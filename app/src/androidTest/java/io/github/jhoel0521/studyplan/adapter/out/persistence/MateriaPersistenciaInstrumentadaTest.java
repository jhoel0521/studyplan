package io.github.jhoel0521.studyplan.adapter.out.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.adapter.out.persistence.repository.MateriaRepositoryRoom;

/**
 * Pruebas instrumentadas de las materias con Room en memoria, fuera del hilo principal (RNF-12).
 *
 * <p>Verifica: HU-06 (guardar y leer, con su clave), HU-07 (editar), HU-08 (archivar y eliminar), HU-09 (listas) y
 * HU-46 (desarchivar).
 */
@RunWith(AndroidJUnit4.class)
public class MateriaPersistenciaInstrumentadaTest {

    private static final int COLOR = 0xFF009688;

    private StudyPlanDatabase base;
    private MateriaRepositoryRoom materias;

    @Before
    public void abrirBase() {
        Context contexto = ApplicationProvider.getApplicationContext();
        base = Room.inMemoryDatabaseBuilder(contexto, StudyPlanDatabase.class).build();
        materias = new MateriaRepositoryRoom(base.materiaDao());
    }

    @After
    public void cerrarBase() {
        base.close();
    }

    private Materia guardar(String nombre, String docente) {
        return materias.guardar(Materia.crear(nombre, COLOR, docente).valor());
    }

    @Test
    public void guardarAsignaUnIdYSePuedeLeer() {
        Materia guardada = guardar("Cálculo II", "Dra. Ruiz");
        assertTrue(guardada.id() != null && guardada.id() > 0);
        Materia leida = materias.obtener(guardada.id()).orElseThrow(AssertionError::new);
        assertEquals("Cálculo II", leida.nombre());
        assertEquals("CALCULO II", leida.clave());
        assertEquals("Dra. Ruiz", leida.docente());
        assertEquals(COLOR, leida.color());
        assertFalse(leida.archivada());
    }

    @Test
    public void laClaveSeGuardaEnLaColumnaNombreClave() {
        Materia guardada = guardar("Física", null);
        assertEquals("FISICA", base.materiaDao().obtener(guardada.id()).nombreClave);
    }

    @Test
    public void unDocenteAusenteSeLeeComoNulo() {
        assertNull(materias.obtener(guardar("Física", null).id()).orElseThrow(AssertionError::new).docente());
    }

    @Test
    public void editarConservaElIdYReemplazaLosDatos() {
        Materia guardada = guardar("Física", null);
        materias.guardar(guardada.editar("Física II", 0xFF000000, "Dr. Paz").valor());
        assertEquals(1, materias.activas().size());
        Materia leida = materias.obtener(guardada.id()).orElseThrow(AssertionError::new);
        assertEquals("Física II", leida.nombre());
        assertEquals("Dr. Paz", leida.docente());
    }

    @Test
    public void archivarYDesarchivarMueveLaMateriaEntreListas() {
        Materia guardada = guardar("Física", null);
        materias.guardar(guardada.conArchivada(true));
        assertEquals(0, materias.activas().size());
        assertEquals(1, materias.archivadas().size());
        materias.guardar(guardada.conArchivada(false));
        assertEquals(1, materias.activas().size());
        assertEquals(0, materias.archivadas().size());
    }

    @Test
    public void eliminarBorraLaFila() {
        Materia guardada = guardar("Física", null);
        materias.eliminar(guardada.id());
        assertFalse(materias.obtener(guardada.id()).isPresent());
    }

    @Test
    public void unaArchivadaYUnaActivaPuedenCompartirNombre() {
        materias.guardar(guardar("Física", null).conArchivada(true));
        guardar("Física", null);
        assertEquals(1, materias.activas().size());
        assertEquals(1, materias.archivadas().size());
    }

    @Test
    public void obtenerUnaInexistenteDevuelveVacio() {
        assertFalse(materias.obtener(999).isPresent());
    }
}
