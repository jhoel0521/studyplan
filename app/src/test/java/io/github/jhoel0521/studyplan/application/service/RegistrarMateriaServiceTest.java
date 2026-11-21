package io.github.jhoel0521.studyplan.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.fakes.FakeMateriaRepository;

/**
 * Pruebas del caso de uso de registrar una materia con un repositorio falso.
 *
 * <p>Verifica: HU-06 (Registro válido y Nombre duplicado), RN-02 y RNF-14.
 */
public class RegistrarMateriaServiceTest {

    private static final int COLOR = 0xFF4CAF50;

    private FakeMateriaRepository repositorio;
    private RegistrarMateriaService servicio;

    @Before
    public void preparar() {
        repositorio = new FakeMateriaRepository();
        servicio = new RegistrarMateriaService(repositorio);
    }

    @Test
    public void unRegistroValidoGuardaLaMateriaConId() {
        Resultado<Materia> r = servicio.ejecutar("Matemáticas", COLOR, null);
        assertTrue(r.esValido());
        assertEquals(1, repositorio.activas().size());
        assertEquals(Long.valueOf(1), r.valor().id());
    }

    @Test
    public void elNombreRepetidoEntreActivasSeRechazaSinDistinguirMayusculasNiAcentos() {
        servicio.ejecutar("Matemáticas", COLOR, null);
        assertEquals(ErrorValidacion.NOMBRE_DUPLICADO, servicio.ejecutar("matemáticas", COLOR, null).error());
        assertEquals(ErrorValidacion.NOMBRE_DUPLICADO, servicio.ejecutar("Matematicas", COLOR, null).error());
        assertEquals(ErrorValidacion.NOMBRE_DUPLICADO, servicio.ejecutar("  MATEMÁTICAS  ", COLOR, null).error());
        assertEquals(1, repositorio.cantidad());
    }

    @Test
    public void unaArchivadaConElMismoNombreNoImpideElRegistro() {
        Materia vieja = servicio.ejecutar("Física", COLOR, null).valor();
        repositorio.guardar(vieja.conArchivada(true));
        assertTrue(servicio.ejecutar("Física", COLOR, null).esValido());
        assertEquals(1, repositorio.activas().size());
        assertEquals(1, repositorio.archivadas().size());
    }

    @Test
    public void unNombreInvalidoNoGuardaNada() {
        assertEquals(ErrorValidacion.NOMBRE_VACIO, servicio.ejecutar("   ", COLOR, null).error());
        assertEquals(ErrorValidacion.NOMBRE_MUY_LARGO, servicio.ejecutar("a".repeat(51), COLOR, null).error());
        assertEquals(0, repositorio.guardados);
    }

    @Test
    public void elNombreSeGuardaSaneado() {
        Resultado<Materia> r = servicio.ejecutar("   Cálculo    II ", COLOR, "  ");
        assertEquals("Cálculo II", r.valor().nombre());
    }
}
