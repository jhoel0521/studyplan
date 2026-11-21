package io.github.jhoel0521.studyplan.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.entity.MetaSemanal;
import io.github.jhoel0521.studyplan.domain.entity.Perfil;
import io.github.jhoel0521.studyplan.domain.readmodel.ProgresoMeta;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.readmodel.ResumenMateria;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeMetaSemanalRepository;
import io.github.jhoel0521.studyplan.fakes.FakePerfilRepository;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;
import io.github.jhoel0521.studyplan.fakes.FakeSesionesDeMateria;
import io.github.jhoel0521.studyplan.fakes.FakeTramoDiarioRepository;

/**
 * Pruebas unitarias de metas semanales, snapshots {@code RN-33}, progreso e historial
 * ({@code HU-25}, {@code HU-26}, {@code HU-27}, {@code RN-14}, {@code RN-32}).
 */
public final class MetasServiceTest {

    private FakeMateriaRepository materias;
    private FakeSesionRepository sesiones;
    private FakeSesionesDeMateria sesionesDeMateria;
    private FakeMetaSemanalRepository metas;
    private FakePerfilRepository perfiles;
    private FakeTramoDiarioRepository tramos;
    private FakeReloj reloj;

    @Before
    public void setUp() {
        materias = new FakeMateriaRepository();
        sesiones = new FakeSesionRepository();
        sesionesDeMateria = new FakeSesionesDeMateria();
        metas = new FakeMetaSemanalRepository();
        perfiles = new FakePerfilRepository();
        perfiles.guardar(Perfil.crear("Ana", 10.0).valor());
        tramos = new FakeTramoDiarioRepository(sesiones);
        reloj = new FakeReloj(Fecha.parsear("2026-11-18").valor(), Hora.de(10, 0).valor());
    }

    @Test
    public void definirMetaSemanalActualizaSnapshotSoloDeEstaSemanaSinAlterarPerfilNiPasadas() {
        Fecha semanaPasada = Fecha.parsear("2026-11-09").valor();
        metas.guardar(MetaSemanal.crear(semanaPasada, null, 8.0).valor());

        DefinirMetaSemanalService service = new DefinirMetaSemanalService(metas, reloj);
        Resultado<MetaSemanal> res = service.ejecutar(15.0);

        assertTrue(res.esValido());
        assertEquals(15.0, res.valor().horasObjetivo(), 1e-6);
        assertEquals(10.0, perfiles.obtener().get().metaSemanalHoras(), 1e-6);
        assertEquals(8.0, metas.buscar(semanaPasada, null).get().horasObjetivo(), 1e-6);
    }

    @Test
    public void materiaGeneralNoSeEditaArchivaNiEliminaYListaAlFinal() {
        Materia general = materias.guardar(Materia.crearGeneral(0xFF607D8B));
        RegistrarMateriaService registrar =
                new RegistrarMateriaService(materias, metas, reloj);
        Materia calculo = registrar.ejecutar("Cálculo", 0xFF1E88E5, null, 4.0).valor();

        EditarMateriaService editar = new EditarMateriaService(materias, metas, reloj);
        assertEquals(
                ErrorValidacion.MATERIA_GENERAL_PROTEGIDA,
                editar.ejecutar(general.id(), "Otro", 0xFF000000, null, null).error());

        ArchivarMateriaService archivar = new ArchivarMateriaService(materias);
        assertEquals(
                ErrorValidacion.MATERIA_GENERAL_PROTEGIDA,
                archivar.ejecutar(general.id()).error());

        EliminarMateriaService eliminar = new EliminarMateriaService(materias, sesionesDeMateria);
        assertEquals(
                ErrorValidacion.MATERIA_GENERAL_PROTEGIDA,
                eliminar.ejecutar(general.id()).error());

        ListarMateriasService listar =
                new ListarMateriasService(materias, sesiones, metas, tramos, reloj);
        List<ResumenMateria> activas = listar.ejecutar().activas();
        assertEquals(2, activas.size());
        assertEquals(calculo.id(), activas.get(0).materia().id());
        assertEquals(general.id(), activas.get(1).materia().id());
        assertNotNull(activas.get(0).metaSemanaActual());
        assertEquals(4.0, activas.get(0).metaSemanaActual().horasObjetivo(), 1e-6);
        assertNull(activas.get(1).metaSemanaActual());
    }

    @Test
    public void historialIncluyeSemanasSinFotoComoEstimadasYCalculaProgreso() {
        Fecha haceDosSemanas = Fecha.parsear("2026-11-02").valor();
        metas.guardar(MetaSemanal.crear(haceDosSemanas, null, 12.0).valor());

        Materia fisica = materias.guardar(Materia.crear("Física", 0xFF43A047, null).valor());
        SesionEstudio s = sesiones.guardar(SesionEstudio.registrarPasada(
                fisica.id(),
                Fecha.parsear("2026-11-17").valor(),
                reloj.hoy(),
                Hora.de(9, 0).valor(),
                Hora.de(11, 0).valor(),
                -240,
                120,
                "").valor());
        tramos.guardarTramos(s.id(), java.util.Collections.singletonMap(
                Fecha.parsear("2026-11-17").valor(), 120));

        ConsultarProgresoMetasService service =
                new ConsultarProgresoMetasService(metas, perfiles, materias, tramos, reloj);
        ProgresoMeta actual = service.progresoSemanaActualTotal();
        assertEquals(2.0, actual.horasRealizadas(), 1e-6);
        assertEquals(10.0, actual.horasObjetivo(), 1e-6);
        assertFalse(actual.estimada());

        List<ProgresoMeta> historial = service.historialMetas();
        assertEquals(3, historial.size());
        assertEquals(Fecha.parsear("2026-11-16").valor(), historial.get(0).semanaInicio());
        assertFalse(historial.get(0).estimada());

        assertEquals(Fecha.parsear("2026-11-09").valor(), historial.get(1).semanaInicio());
        assertTrue(historial.get(1).estimada());

        assertEquals(haceDosSemanas, historial.get(2).semanaInicio());
        assertFalse(historial.get(2).estimada());
        assertEquals(12.0, historial.get(2).horasObjetivo(), 1e-6);
    }
}
