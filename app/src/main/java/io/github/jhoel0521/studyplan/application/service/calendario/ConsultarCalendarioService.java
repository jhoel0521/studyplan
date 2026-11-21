package io.github.jhoel0521.studyplan.application.service.calendario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.github.jhoel0521.studyplan.application.port.in.calendario.ConsultarCalendario;
import io.github.jhoel0521.studyplan.application.port.in.sesiones.VencerSesionesPendientes;
import io.github.jhoel0521.studyplan.application.port.out.persistence.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.system.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.persistence.SesionRepository;
import io.github.jhoel0521.studyplan.application.port.out.persistence.TramoDiarioRepository;
import io.github.jhoel0521.studyplan.domain.readmodel.BloqueHorarioCalendario;
import io.github.jhoel0521.studyplan.domain.service.CalculadorRangoHorario;
import io.github.jhoel0521.studyplan.domain.readmodel.CeldaDiaMes;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.readmodel.DatosCalendario;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.FiltroCalendario;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.valueobject.Instante;
import io.github.jhoel0521.studyplan.domain.readmodel.ItemSesionCalendario;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.valueobject.PeriodoCalendario;
import io.github.jhoel0521.studyplan.domain.service.RepartidorPorDia;
import io.github.jhoel0521.studyplan.domain.readmodel.ResumenPeriodoCalendario;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.VistaCalendario;

/**
 * Servicio de aplicación que orquesta la consulta del calendario de estudio en Java puro
 * (HU-07, HU-08, HU-16 a HU-21, RN-08, RN-11, RN-13).
 *
 * <p>Nota sobre Room v3: al no existir columna separada para distinguir {@code COMPLETADA_TARDIA}
 * derivada de {@code NO_REALIZADA} respecto a registro tardío directo, el total {@code Plan}
 * suma {@code duracionPlanificadaMinutos} de toda sesión cuyo estado cumple
 * {@code estado.ocupaHorario()} (excluyendo {@code REPROGRAMADA}).
 */
public final class ConsultarCalendarioService implements ConsultarCalendario {

    private final SesionRepository sesiones;
    private final MateriaRepository materias;
    private final TramoDiarioRepository tramos;
    private final Reloj reloj;
    private final VencerSesionesPendientes vencerPendientes;

    public ConsultarCalendarioService(
            SesionRepository sesiones,
            MateriaRepository materias,
            Reloj reloj,
            VencerSesionesPendientes vencerPendientes) {
        this(sesiones, materias, null, reloj, vencerPendientes);
    }

    public ConsultarCalendarioService(
            SesionRepository sesiones,
            MateriaRepository materias,
            TramoDiarioRepository tramos,
            Reloj reloj,
            VencerSesionesPendientes vencerPendientes) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.materias = Objects.requireNonNull(materias, "materias");
        this.tramos = tramos;
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.vencerPendientes = Objects.requireNonNull(vencerPendientes, "vencerPendientes");
    }

    @Override
    public DatosCalendario ejecutar(PeriodoCalendario periodo, FiltroCalendario filtro) {
        Objects.requireNonNull(periodo, "periodo");
        FiltroCalendario filtroEfectivo =
                filtro == null ? FiltroCalendario.porDefecto() : filtro;

        // RN-08: toda sesión pendiente vencida pasa a NO_REALIZADA antes de pintar el calendario.
        vencerPendientes.ejecutar();

        Fecha hoy = reloj.hoy();
        Hora ahora = reloj.ahora();

        List<Materia> activas = materias.activas();
        List<Materia> archivadas = materias.archivadas();
        Map<Long, Materia> mapaMaterias = new HashMap<>();
        for (Materia m : activas) {
            if (m.id() != null) {
                mapaMaterias.put(m.id(), m);
            }
        }
        for (Materia m : archivadas) {
            if (m.id() != null) {
                mapaMaterias.put(m.id(), m);
            }
        }

        List<Materia> materiasDisponibles = new ArrayList<>(activas);
        if (filtroEfectivo.incluirArchivadas()) {
            materiasDisponibles.addAll(archivadas);
        }

        Fecha consultaInicio = periodo.rangoConsultaInicio();
        Fecha consultaFin = periodo.rangoConsultaFin();
        long margenDesfase = Instante.DESFASE_MAXIMO_MINUTOS * Instante.MILIS_POR_MINUTO;
        long desdeUtc = Instante.desdeFechaYHoraLocal(
                consultaInicio, Hora.de(0, 0).valor(), 0).utcMilis() - margenDesfase;
        long hastaUtc = Instante.desdeFechaYHoraLocal(
                consultaFin.sumarDias(1), Hora.de(0, 0).valor(), 0).utcMilis() + margenDesfase;
        List<SesionEstudio> sesionesRango = sesiones.listarPorRangoUtc(desdeUtc, hastaUtc);

        List<SesionEstudio> sesionesFiltradas = new ArrayList<>();
        List<ItemSesionCalendario> itemsConsulta = new ArrayList<>();
        for (SesionEstudio s : sesionesRango) {
            if (s.fecha().esPosteriorA(consultaFin) || s.fechaFinLocal().esAnteriorA(consultaInicio)) {
                continue;
            }
            Materia materia = mapaMaterias.get(s.materiaId());
            if (materia == null) {
                continue;
            }
            if (!filtroEfectivo.acepta(s.materiaId(), materia.archivada(), s.codigoEstado())) {
                continue;
            }
            sesionesFiltradas.add(s);
            agregarItemsDeSesion(itemsConsulta, s, materia);
        }

        Fecha visibleInicio = periodo.rangoVisibleInicio();
        Fecha visibleFin = periodo.rangoVisibleFin();
        List<ItemSesionCalendario> itemsVisibles = new ArrayList<>();
        for (ItemSesionCalendario item : itemsConsulta) {
            if (!item.fecha().esAnteriorA(visibleInicio)
                    && !item.fecha().esPosteriorA(visibleFin)) {
                itemsVisibles.add(item);
            }
        }

        int planVisible = 0;
        for (SesionEstudio s : sesionesFiltradas) {
            if (s.estado().ocupaHorario()) {
                planVisible += s.minutosPlanificadosEnRango(visibleInicio, visibleFin);
            }
        }
        int realVisible = calcularRealVisible(
                filtroEfectivo, sesionesFiltradas, visibleInicio, visibleFin);
        ResumenPeriodoCalendario resumen = new ResumenPeriodoCalendario(planVisible, realVisible);

        if (periodo.vista() == VistaCalendario.MES) {
            List<CeldaDiaMes> celdasMes = construirCeldasMes(
                    periodo, consultaInicio, consultaFin, hoy, itemsConsulta);
            List<ItemSesionCalendario> sesionesDia = filtrarPorDia(
                    itemsConsulta, periodo.diaSeleccionado());
            return new DatosCalendario(
                    periodo,
                    filtroEfectivo,
                    resumen,
                    celdasMes,
                    sesionesDia,
                    Collections.emptyList(),
                    CalculadorRangoHorario.MINUTO_INICIO_DEFECTO,
                    materiasDisponibles,
                    hoy,
                    ahora);
        }

        List<BloqueHorarioCalendario> bloques =
                CalculadorRangoHorario.construirBloques(itemsVisibles, periodo.vista());
        boolean incluyeHoy = !hoy.esAnteriorA(visibleInicio) && !hoy.esPosteriorA(visibleFin);
        int minutoScroll =
                CalculadorRangoHorario.calcularMinutoScrollInicial(itemsVisibles, incluyeHoy, ahora);

        return new DatosCalendario(
                periodo,
                filtroEfectivo,
                resumen,
                Collections.emptyList(),
                Collections.emptyList(),
                bloques,
                minutoScroll,
                materiasDisponibles,
                hoy,
                ahora);
    }

    private static List<CeldaDiaMes> construirCeldasMes(
            PeriodoCalendario periodo,
            Fecha inicio,
            Fecha fin,
            Fecha hoy,
            List<ItemSesionCalendario> items) {
        Map<Fecha, List<ItemSesionCalendario>> porFecha = new HashMap<>();
        for (ItemSesionCalendario item : items) {
            porFecha.computeIfAbsent(item.fecha(), k -> new ArrayList<>()).add(item);
        }

        List<CeldaDiaMes> celdas = new ArrayList<>();
        Fecha actual = inicio;
        while (!actual.esPosteriorA(fin)) {
            List<ItemSesionCalendario> delDia = porFecha.get(actual);
            Set<Integer> colores = new LinkedHashSet<>();
            int minutosPlan = 0;
            int cantidad = 0;
            if (delDia != null) {
                cantidad = delDia.size();
                for (ItemSesionCalendario s : delDia) {
                    colores.add(s.colorMateria());
                    if (s.estado().ocupaHorario()) {
                        minutosPlan += Math.max(0, s.duracionPlanificadaMinutos());
                    }
                }
            }
            celdas.add(new CeldaDiaMes(
                    actual,
                    periodo.perteneceAMesVisible(actual),
                    actual.equals(hoy),
                    actual.equals(periodo.diaSeleccionado()),
                    new ArrayList<>(colores),
                    minutosPlan,
                    cantidad));
            actual = actual.sumarDias(1);
        }
        return celdas;
    }

    private static List<ItemSesionCalendario> filtrarPorDia(
            List<ItemSesionCalendario> items, Fecha diaSeleccionado) {
        if (diaSeleccionado == null) {
            return Collections.emptyList();
        }
        List<ItemSesionCalendario> resultado = new ArrayList<>();
        for (ItemSesionCalendario item : items) {
            if (item.fecha().equals(diaSeleccionado)) {
                resultado.add(item);
            }
        }
        return resultado;
    }

    private void agregarItemsDeSesion(
            List<ItemSesionCalendario> destino, SesionEstudio s, Materia materia) {
        if (!s.cruzaMedianoche()) {
            destino.add(new ItemSesionCalendario(
                    s.id(),
                    s.materiaId(),
                    materia.nombre(),
                    materia.color(),
                    materia.archivada(),
                    s.fecha(),
                    s.horaInicio(),
                    s.horaFin(),
                    s.duracionPlanificadaMinutos(),
                    s.duracionRealMinutos(),
                    s.estado(),
                    s.fecha(),
                    false));
            return;
        }
        int minPrimerDia = s.minutosPlanificadosEnRango(s.fecha(), s.fecha());
        int minSegundoDia = s.minutosPlanificadosEnRango(s.fechaFinLocal(), s.fechaFinLocal());
        destino.add(new ItemSesionCalendario(
                s.id(),
                s.materiaId(),
                materia.nombre(),
                materia.color(),
                materia.archivada(),
                s.fecha(),
                s.horaInicio(),
                Hora.de(23, 59).valor(),
                minPrimerDia,
                s.duracionRealMinutos(),
                s.estado(),
                s.fecha(),
                true));
        if (minSegundoDia > 0) {
            destino.add(new ItemSesionCalendario(
                    s.id(),
                    s.materiaId(),
                    materia.nombre(),
                    materia.color(),
                    materia.archivada(),
                    s.fechaFinLocal(),
                    Hora.de(0, 0).valor(),
                    s.horaFin(),
                    minSegundoDia,
                    s.duracionRealMinutos(),
                    s.estado(),
                    s.fecha(),
                    false));
        }
    }

    private int calcularRealVisible(
            FiltroCalendario filtro,
            List<SesionEstudio> sesionesFiltradas,
            Fecha visibleInicio,
            Fecha visibleFin) {
        boolean sinFiltroRestringido = filtro.materiaId() == null
                && filtro.incluirArchivadas()
                && filtro.estadosVisibles().size() == CodigoEstadoSesion.values().length;
        if (tramos != null && sinFiltroRestringido) {
            return tramos.sumarMinutosEnRango(visibleInicio, visibleFin);
        }
        int total = 0;
        for (SesionEstudio s : sesionesFiltradas) {
            int minutos = s.minutosRealesContabilizables();
            if (minutos <= 0) {
                continue;
            }
            Map<Fecha, Integer> porDia = RepartidorPorDia.repartir(s, null, 0L, minutos);
            for (Map.Entry<Fecha, Integer> e : porDia.entrySet()) {
                if (!e.getKey().esAnteriorA(visibleInicio)
                        && !e.getKey().esPosteriorA(visibleFin)) {
                    total += e.getValue();
                }
            }
        }
        return total;
    }
}
