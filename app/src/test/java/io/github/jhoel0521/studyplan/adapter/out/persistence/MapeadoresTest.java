package io.github.jhoel0521.studyplan.adapter.out.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.Ajustes;
import io.github.jhoel0521.studyplan.domain.Idioma;
import io.github.jhoel0521.studyplan.domain.Perfil;
import io.github.jhoel0521.studyplan.domain.Tema;

/**
 * Pruebas de los mapeadores entre filas de Room y objetos del dominio.
 *
 * <p>Verifica: HU-01 (perfil), HU-03 (tema e idioma) y HU-04 (bloqueo).
 */
public class MapeadoresTest {

    @Test
    public void elPerfilIdaYVueltaConservaSusDatos() {
        Perfil perfil = Perfil.crear("Ana", 12.5).valor();
        assertEquals(perfil, Mapeadores.aDominio(Mapeadores.aEntidad(perfil)));
    }

    @Test
    public void laFilaDelPerfilUsaElIdUnico() {
        assertEquals(PerfilEntity.ID_UNICO, Mapeadores.aEntidad(Perfil.crear("Ana", 20).valor()).id);
    }

    @Test
    public void unaFilaDePerfilInvalidaDevuelveNuloEnLugarDeFallar() {
        PerfilEntity fila = new PerfilEntity();
        fila.nombre = "  ";
        fila.metaSemanalHoras = 20;
        assertNull(Mapeadores.aDominio(fila));
    }

    @Test
    public void losAjustesIdaYVueltaConservanSusDatos() {
        Ajustes ajustes = new Ajustes(Tema.OSCURO, Idioma.EN, true);
        assertEquals(ajustes, Mapeadores.aDominio(Mapeadores.aEntidad(ajustes)));
    }

    @Test
    public void unValorDesconocidoEnLosAjustesUsaElValorPorDefecto() {
        AjustesEntity fila = new AjustesEntity();
        fila.tema = "FUTURO";
        fila.idioma = "FR";
        fila.bloqueoActivo = true;
        assertEquals(new Ajustes(Tema.SISTEMA, Idioma.ES, true), Mapeadores.aDominio(fila));
    }
}
