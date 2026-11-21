package io.github.jhoel0521.studyplan.adapter.out.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.github.jhoel0521.studyplan.domain.entity.Ajustes;
import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.enumeration.Idioma;
import io.github.jhoel0521.studyplan.domain.entity.Perfil;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.Tema;

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
        Ajustes ajustes = new Ajustes(
                Tema.OSCURO,
                Idioma.EN,
                true,
                false,
                Ajustes.porDefecto().pomodoro(),
                AntelacionRecordatorio.MINUTOS_30);
        assertEquals(ajustes, Mapeadores.aDominio(Mapeadores.aEntidad(ajustes)));
    }

    @Test
    public void unValorDesconocidoEnLosAjustesUsaElValorPorDefecto() {
        AjustesEntity fila = new AjustesEntity();
        fila.tema = "FUTURO";
        fila.idioma = "FR";
        fila.bloqueoActivo = true;
        fila.recordatorioMinutos = 999;
        assertEquals(
                new Ajustes(
                        Tema.SISTEMA,
                        Idioma.ES,
                        true,
                        false,
                        Ajustes.porDefecto().pomodoro(),
                        AntelacionRecordatorio.MINUTOS_15),
                Mapeadores.aDominio(fila));

        fila.recordatorioMinutos = 0;
        assertEquals(
                AntelacionRecordatorio.SIN_RECORDATORIO,
                Mapeadores.aDominio(fila).recordatorioPorDefecto());
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
        io.github.jhoel0521.studyplan.domain.entity.EventoEstudio ev =
                io.github.jhoel0521.studyplan.domain.entity.EventoEstudio.crear(
                        5L,
                        io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio.REANUDACION,
                        1_700_000_000_000L,
                        -240,
                        "",
                        io.github.jhoel0521.studyplan.domain.enumeration.TopicoEventoEstudio.DESCANSO_COMIDA)
                        .valor();
        EventoEstudioEntity entidad = Mapeadores.aEntidad(ev);
        assertEquals("DESCANSO_COMIDA", entidad.topico);
        assertEquals(
                io.github.jhoel0521.studyplan.domain.enumeration.TopicoEventoEstudio.DESCANSO_COMIDA,
                Mapeadores.aDominio(entidad).topico().orElse(null));

        entidad.topico = null;
        assertEquals(java.util.Optional.empty(), Mapeadores.aDominio(entidad).topico());
    }

    @Test
    public void sesionMapeaAntelacionRecordatorioYToleraNuloOValorDesconocido() {
        SesionEstudio conRec = SesionEstudio.restaurar(
                9L,
                3L,
                1_700_000_000_000L,
                1_700_003_600_000L,
                -240,
                60,
                null,
                CodigoEstadoSesion.PENDIENTE,
                "",
                null,
                TecnicaEstudio.LIBRE,
                PreferenciaNoMolestar.HEREDAR_GLOBAL,
                null,
                null,
                null,
                null,
                AntelacionRecordatorio.MINUTOS_30);
        SesionEntity entidad = Mapeadores.aEntidad(conRec);
        assertEquals(Integer.valueOf(30), entidad.recordatorioMinutos);
        assertEquals(AntelacionRecordatorio.MINUTOS_30, Mapeadores.aDominio(entidad).antelacionRecordatorio());

        entidad.recordatorioMinutos = null;
        assertEquals(
                AntelacionRecordatorio.SIN_RECORDATORIO,
                Mapeadores.aDominio(entidad).antelacionRecordatorio());

        entidad.recordatorioMinutos = 999;
        assertEquals(
                AntelacionRecordatorio.SIN_RECORDATORIO,
                Mapeadores.aDominio(entidad).antelacionRecordatorio());
    }
}
