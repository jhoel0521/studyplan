package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import io.github.jhoel0521.studyplan.application.service.metas.ConsultarProgresoMetasService;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Perfil;
import io.github.jhoel0521.studyplan.domain.readmodel.ProgresoMeta;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;
import io.github.jhoel0521.studyplan.fakes.FakeMetaSemanalRepository;
import io.github.jhoel0521.studyplan.fakes.FakePerfilRepository;
import io.github.jhoel0521.studyplan.fakes.FakeReloj;
import io.github.jhoel0521.studyplan.fakes.FakeSesionRepository;
import io.github.jhoel0521.studyplan.fakes.FakeTramoDiarioRepository;

/**
 * Pruebas unitarias de {@link HistorialMetasViewModel} ({@code HU-27}).
 */
public final class HistorialMetasViewModelTest {

    @Rule
    public final InstantTaskExecutorRule regla = new InstantTaskExecutorRule();

    @Test
    public void cargaHistorialDeMetasDesdeElServicio() {
        FakeMetaSemanalRepository metas = new FakeMetaSemanalRepository();
        FakePerfilRepository perfiles = new FakePerfilRepository();
        perfiles.guardar(Perfil.crear("Ana", 12.0).valor());
        FakeMateriaRepository materias = new FakeMateriaRepository();
        FakeSesionRepository sesiones = new FakeSesionRepository();
        FakeTramoDiarioRepository tramos = new FakeTramoDiarioRepository(sesiones);
        FakeReloj reloj = new FakeReloj(Fecha.parsear("2026-11-18").valor(), Hora.de(10, 0).valor());

        ConsultarProgresoMetasService service =
                new ConsultarProgresoMetasService(metas, perfiles, materias, tramos, reloj);
        HistorialMetasViewModel vm = new HistorialMetasViewModel(service, Runnable::run);

        vm.cargar();
        List<ProgresoMeta> lista = vm.historial().getValue();
        assertNotNull(lista);
        assertEquals(1, lista.size());
        assertEquals(12.0, lista.get(0).horasObjetivo(), 1e-6);
    }
}
