package io.github.jhoel0521.studyplan.adapter.out.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.Ajustes;
import io.github.jhoel0521.studyplan.domain.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Idioma;
import io.github.jhoel0521.studyplan.domain.Perfil;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.Tema;

/**
 * Pruebas de los mapeadores entre filas de Room y objetos del dominio.
 *
 * <p>Verifica: HU-01 (perfil), HU-03 (tema e idioma), HU-04 (bloqueo) y HU-10 a HU-15 (sesiones).
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

    @Test
    public void laSesionIdaYVueltaConservaTodosSusCampos() {
        SesionEstudio sesion = SesionEstudio.restaurar(
                7L,
                3L,
                Fecha.de(2026, 11, 14).valor(),
                Hora.de(10, 0).valor(),
                Hora.de(11, 30).valor(),
                90,
                85,
                CodigoEstadoSesion.COMPLETADA_TARDIA,
                "Repaso completo",
                2L);

        assertEquals(sesion, Mapeadores.aDominio(Mapeadores.aEntidad(sesion)));
    }

    @Test
    public void sesionConRangoODesfaseInvalidoDevuelveNuloYEstadoDesconocidoCaeANoRealizada() {
        SesionEntity invalida = new SesionEntity();
        invalida.id = 1L;
        invalida.materiaId = 2L;
        invalida.planInicioUtcMilis = 1_700_000_000_000L;
        invalida.planFinUtcMilis = 1_700_000_000_000L; // fin <= inicio
        invalida.desfaseMinutos = -240;
        invalida.estado = "PENDIENTE";
        assertNull(Mapeadores.aDominio(invalida));

        SesionEntity estadoFuturo = new SesionEntity();
        estadoFuturo.id = 4L;
        estadoFuturo.materiaId = 2L;
        estadoFuturo.planInicioUtcMilis = 1_700_000_000_000L;
        estadoFuturo.planFinUtcMilis = 1_700_003_600_000L;
        estadoFuturo.desfaseMinutos = -240;
        estadoFuturo.duracionPlanificadaMinutos = 60;
        estadoFuturo.estado = "ESTADO_FUTURO";
        assertEquals(
                CodigoEstadoSesion.NO_REALIZADA,
                Mapeadores.aDominio(estadoFuturo).codigoEstado());
    }

    @Test
    public void eventoEstudioMapeaTopicoCanonicoYNuloHistorico() {
        io.github.jhoel0521.studyplan.domain.EventoEstudio ev =
                io.github.jhoel0521.studyplan.domain.EventoEstudio.crear(
                        5L,
                        io.github.jhoel0521.studyplan.domain.TipoEventoEstudio.REANUDACION,
                        1_700_000_000_000L,
                        -240,
                        "",
                        io.github.jhoel0521.studyplan.domain.TopicoEventoEstudio.DESCANSO_COMIDA)
                        .valor();
        EventoEstudioEntity entidad = Mapeadores.aEntidad(ev);
        assertEquals("DESCANSO_COMIDA", entidad.topico);
        assertEquals(
                io.github.jhoel0521.studyplan.domain.TopicoEventoEstudio.DESCANSO_COMIDA,
                Mapeadores.aDominio(entidad).topico().orElse(null));

        entidad.topico = null;
        assertEquals(java.util.Optional.empty(), Mapeadores.aDominio(entidad).topico());
    }
}
