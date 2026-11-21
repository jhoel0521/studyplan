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

import io.github.jhoel0521.studyplan.domain.entity.Ajustes;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.enumeration.Idioma;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.entity.Perfil;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.Tema;
import io.github.jhoel0521.studyplan.adapter.out.persistence.repository.AjustesRepositoryRoom;
import io.github.jhoel0521.studyplan.adapter.out.persistence.repository.AlmacenDatosRoom;
import io.github.jhoel0521.studyplan.adapter.out.persistence.repository.MateriaRepositoryRoom;
import io.github.jhoel0521.studyplan.adapter.out.persistence.repository.PerfilRepositoryRoom;
import io.github.jhoel0521.studyplan.adapter.out.persistence.repository.SesionRepositoryRoom;

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
    private MateriaRepositoryRoom materias;
    private SesionRepositoryRoom sesiones;

    @Before
    public void abrirBase() {
        Context contexto = ApplicationProvider.getApplicationContext();
        base = Room.inMemoryDatabaseBuilder(contexto, StudyPlanDatabase.class).build();
        perfiles = new PerfilRepositoryRoom(base.perfilDao());
        ajustes = new AjustesRepositoryRoom(base.ajustesDao());
        almacen = new AlmacenDatosRoom(base);
        materias = new MateriaRepositoryRoom(base.materiaDao());
        sesiones = new SesionRepositoryRoom(base.sesionDao());
    }

    @After
    public void cerrarBase() {
        base.close();
    }

    @Test
    public void laBaseAbreConLaVersionOcho() {
        assertEquals(8, base.getOpenHelper().getWritableDatabase().getVersion());
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
        Materia fisica = materias.guardar(Materia.crear("Física", 0xFF2196F3, null).valor());
        Fecha hoy = Fecha.de(2026, 11, 14).valor();
        sesiones.guardar(SesionEstudio.planificar(
                fisica.id(), hoy, hoy, Hora.de(10, 0).valor(), Hora.de(11, 0).valor(), null, null)
                .valor());
        almacen.borrarTodo();
        assertFalse(perfiles.obtener().isPresent());
        assertEquals(1, materias.activas().size());
        assertTrue(materias.activas().get(0).esGeneral());
        assertTrue(sesiones.listarPorFecha(hoy).isEmpty());
        assertNull(base.ajustesDao().obtener());
        assertEquals(Ajustes.porDefecto(), ajustes.obtener());
    }
}
