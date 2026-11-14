package io.github.jhoel0521.studyplan.adapter.out.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.jhoel0521.studyplan.domain.Ajustes;
import io.github.jhoel0521.studyplan.domain.Idioma;
import io.github.jhoel0521.studyplan.domain.Perfil;
import io.github.jhoel0521.studyplan.domain.Tema;

/**
 * Pruebas instrumentadas de la base Room real, en memoria. Se ejecutan en el hilo de la prueba, que no es el
 * principal (RNF-12).
 *
 * <p>Verifica: HU-01 (guardar y leer el perfil, RN-01), HU-03 y HU-04 (ajustes) y HU-38 (RN-21: borrar todo
 * restablece los ajustes por defecto).
 */
@RunWith(AndroidJUnit4.class)
public class PersistenciaInstrumentadaTest {

    private StudyPlanDatabase base;
    private PerfilRepositoryRoom perfiles;
    private AjustesRepositoryRoom ajustes;
    private AlmacenDatosRoom almacen;

    @Before
    public void abrirBase() {
        Context contexto = ApplicationProvider.getApplicationContext();
        base = Room.inMemoryDatabaseBuilder(contexto, StudyPlanDatabase.class).build();
        perfiles = new PerfilRepositoryRoom(base.perfilDao());
        ajustes = new AjustesRepositoryRoom(base.ajustesDao());
        almacen = new AlmacenDatosRoom(base);
    }

    @After
    public void cerrarBase() {
        base.close();
    }

    @Test
    public void labaseAbreConLaVersionUno() {
        assertEquals(1, base.getOpenHelper().getWritableDatabase().getVersion());
    }

    @Test
    public void sinGuardarNoHayPerfil() {
        assertFalse(perfiles.obtener().isPresent());
    }

    @Test
    public void guardarYLeerElPerfil() {
        perfiles.guardar(Perfil.crear("Ana", 20).valor());
        assertEquals(Perfil.crear("Ana", 20).valor(), perfiles.obtener().orElseThrow(AssertionError::new));
    }

    @Test
    public void unSegundoPerfilReemplazaAlPrimero() {
        perfiles.guardar(Perfil.crear("Ana", 20).valor());
        perfiles.guardar(Perfil.crear("Luis", 10).valor());
        assertEquals("Luis", perfiles.obtener().orElseThrow(AssertionError::new).nombre());
    }

    @Test
    public void sinGuardarLosAjustesSonLosPorDefecto() {
        assertEquals(Ajustes.porDefecto(), ajustes.obtener());
    }

    @Test
    public void guardarYLeerLosAjustes() {
        Ajustes elegidos = new Ajustes(Tema.OSCURO, Idioma.EN, true);
        ajustes.guardar(elegidos);
        assertEquals(elegidos, ajustes.obtener());
    }

    @Test
    public void borrarTodoVaciaLasTablasYRestableceLosAjustes() {
        perfiles.guardar(Perfil.crear("Ana", 20).valor());
        ajustes.guardar(new Ajustes(Tema.OSCURO, Idioma.EN, true));
        almacen.borrarTodo();
        assertFalse(perfiles.obtener().isPresent());
        assertNull(base.ajustesDao().obtener());
        assertEquals(Ajustes.porDefecto(), ajustes.obtener());
    }
}
