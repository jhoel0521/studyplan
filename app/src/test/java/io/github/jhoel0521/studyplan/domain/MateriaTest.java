package io.github.jhoel0521.studyplan.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pruebas de las reglas de la materia: saneamiento del nombre, clave de unicidad y docente opcional.
 *
 * <p>Verifica: HU-06 (Registro válido), HU-07, RN-02 (1 a 50 caracteres, sin distinguir mayúsculas) y RNF-14.
 */
public class MateriaTest {

    private static final int COLOR = 0xFF2196F3;

    private static Materia valida(String nombre) {
        return Materia.crear(nombre, COLOR, null).valor();
    }

    @Test
    public void unaMateriaValidaSeCreaActivaYSinId() {
        Materia m = Materia.crear("Matemáticas", COLOR, "Dra. Ruiz").valor();
        assertEquals("Matemáticas", m.nombre());
        assertEquals("Dra. Ruiz", m.docente());
        assertEquals(COLOR, m.color());
        assertFalse(m.archivada());
        assertNull(m.id());
    }

    @Test
    public void unNombreVacioSeRechaza() {
        assertEquals(ErrorValidacion.NOMBRE_VACIO, Materia.crear("", COLOR, null).error());
        assertEquals(ErrorValidacion.NOMBRE_VACIO, Materia.crear(null, COLOR, null).error());
    }

    @Test
    public void unNombreDeSoloEspaciosSeRechaza() {
        assertEquals(ErrorValidacion.NOMBRE_VACIO, Materia.crear("   \t ", COLOR, null).error());
    }

    @Test
    public void elNombreSeRecortaYColapsaLosEspaciosRepetidos() {
        assertEquals("Cálculo II", valida("   Cálculo    II  ").nombre());
    }

    @Test
    public void elNombreConservaMayusculasYAcentosParaMostrarse() {
        assertEquals("Física General", valida("Física General").nombre());
    }

    @Test
    public void laClaveIgnoraAcentosMayusculasYEspaciosRepetidos() {
        assertEquals("CALCULO II", valida("Cálculo  II").clave());
        assertEquals(valida("Matemáticas").clave(), valida("MATEMATICAS").clave());
        assertEquals(valida("Matemáticas").clave(), valida("  matematicas ").clave());
    }

    @Test
    public void laEnieEsOtraLetraYNoSeConfundeConLaN() {
        assertNotEquals(valida("Año").clave(), valida("Ano").clave());
        assertEquals("AÑO", valida("año").clave());
        assertEquals(valida("Año").clave(), valida("  AÑO ").clave());
    }

    @Test
    public void losEspaciosNoSeparablesYOtrosEspaciosUnicodeSeSanean() {
        assertEquals("Cálculo", valida("\u00A0Cálculo\u00A0").nombre());
        assertEquals("Cálculo II", valida("Cálculo\u202FII").nombre());
        assertEquals("Cálculo II", valida("\u3000Cálculo\u2003 \u00A0II\u3000").nombre());
        assertEquals(valida("Cálculo").clave(), valida("\u00A0Cálculo").clave());
    }

    @Test
    public void unNombreSoloDeEspaciosDeAnchoCeroSeRechazaComoVacio() {
        assertEquals(ErrorValidacion.NOMBRE_VACIO, Materia.crear("\u200B\uFEFF", COLOR, null).error());
        assertEquals(ErrorValidacion.NOMBRE_VACIO, Materia.crear("\u00A0\u00A0", COLOR, null).error());
    }

    @Test
    public void laClaveDistingueNombresDistintos() {
        assertNotEquals(valida("Cálculo I").clave(), valida("Cálculo II").clave());
    }

    @Test
    public void cincuentaCaracteresSeAceptanYCincuentaYUnoNo() {
        String cincuenta = "a".repeat(50);
        assertTrue(Materia.crear(cincuenta, COLOR, null).esValido());
        assertEquals(ErrorValidacion.NOMBRE_MUY_LARGO, Materia.crear(cincuenta + "a", COLOR, null).error());
    }

    @Test
    public void elLargoSeMideSobreElNombreYaSaneado() {
        String conEspacios = "  " + "a".repeat(50) + "  ";
        assertTrue(Materia.crear(conEspacios, COLOR, null).esValido());
        String repetidos = "a".repeat(25) + "     " + "a".repeat(24);
        assertEquals(50, valida(repetidos).nombre().length());
    }

    @Test
    public void unDocenteVacioEsAusente() {
        assertNull(Materia.crear("Física", COLOR, "   ").valor().docente());
        assertNull(Materia.crear("Física", COLOR, null).valor().docente());
    }

    @Test
    public void elDocenteNoTieneLimiteDeLargo() {
        String largo = "d".repeat(500);
        assertEquals(largo, Materia.crear("Física", COLOR, largo).valor().docente());
    }

    @Test
    public void editarConservaElIdYElEstadoDeArchivada() {
        Materia original = valida("Física").conId(7).conArchivada(true);
        Materia editada = original.editar("  Física II ", 0xFF000000, "Dr. Paz").valor();
        assertEquals(Long.valueOf(7), editada.id());
        assertTrue(editada.archivada());
        assertEquals("Física II", editada.nombre());
        assertEquals(0xFF000000, editada.color());
        assertEquals("Dr. Paz", editada.docente());
    }

    @Test
    public void editarConUnNombreInvalidoDevuelveElError() {
        assertEquals(ErrorValidacion.NOMBRE_VACIO, valida("Física").editar("  ", COLOR, null).error());
    }

    @Test
    public void restaurarVuelveASanearYCalculaLaClave() {
        Materia m = Materia.restaurar(3, "  Química  ", COLOR, "", false);
        assertEquals("Química", m.nombre());
        assertEquals("QUIMICA", m.clave());
        assertNull(m.docente());
        assertEquals(Long.valueOf(3), m.id());
    }
}
