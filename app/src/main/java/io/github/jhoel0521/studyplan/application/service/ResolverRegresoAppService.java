package io.github.jhoel0521.studyplan.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.tecnicas.ResolverRegresoApp;
import io.github.jhoel0521.studyplan.application.port.out.AjustesRepository;
import io.github.jhoel0521.studyplan.application.port.out.ControladorNoMolestar;
import io.github.jhoel0521.studyplan.application.port.out.EventoEstudioRepository;
import io.github.jhoel0521.studyplan.application.port.out.MarcadorSalidaApp;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.MetaSemanalRepository;
import io.github.jhoel0521.studyplan.application.port.out.PerfilRepository;
import io.github.jhoel0521.studyplan.application.port.out.ProgramadorAvisosSesion;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.application.port.out.TramoDiarioRepository;
import io.github.jhoel0521.studyplan.domain.entity.Ajustes;
import io.github.jhoel0521.studyplan.domain.service.CalculadorTiempoReal;
import io.github.jhoel0521.studyplan.domain.readmodel.CierreSesion;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.readmodel.MetaAlcanzada;
import io.github.jhoel0521.studyplan.domain.service.RepartidorPorDia;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TopicoEventoEstudio;

/**
 * Resuelve la declaración del estudiante al volver con una sesión {@code EN_CURSO}
 * ({@code NO_SEGUI}, {@code HASTA_HORA}, {@code SIGO_ESTUDIANDO}) según HU-41, RN-24 y RN-16.
 */
public final class ResolverRegresoAppService implements ResolverRegresoApp {

    private final SesionRepository sesiones;
    private final EventoEstudioRepository eventos;
    private final MateriaRepository materias;
    private final AjustesRepository ajustesRepo;
    private final ControladorNoMolestar noMolestar;
    private final ProgramadorAvisosSesion programador;
    private final MarcadorSalidaApp marcadorSalida;
    private final TramoDiarioRepository tramos;
    private final MetaSemanalRepository metas;
    private final PerfilRepository perfiles;
    private final Reloj reloj;

    public ResolverRegresoAppService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            MateriaRepository materias,
            AjustesRepository ajustesRepo,
            ControladorNoMolestar noMolestar,
            ProgramadorAvisosSesion programador,
            MarcadorSalidaApp marcadorSalida,
            Reloj reloj) {
        this(
                sesiones,
                eventos,
                materias,
                ajustesRepo,
                noMolestar,
                programador,
                marcadorSalida,
                null,
                null,
                null,
                reloj);
    }

    public ResolverRegresoAppService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            MateriaRepository materias,
            AjustesRepository ajustesRepo,
            ControladorNoMolestar noMolestar,
            ProgramadorAvisosSesion programador,
            MarcadorSalidaApp marcadorSalida,
            TramoDiarioRepository tramos,
            Reloj reloj) {
        this(
                sesiones,
                eventos,
                materias,
                ajustesRepo,
                noMolestar,
                programador,
                marcadorSalida,
                tramos,
                null,
                null,
                reloj);
    }

    public ResolverRegresoAppService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            MateriaRepository materias,
            AjustesRepository ajustesRepo,
            ControladorNoMolestar noMolestar,
            ProgramadorAvisosSesion programador,
            MarcadorSalidaApp marcadorSalida,
            TramoDiarioRepository tramos,
            MetaSemanalRepository metas,
            PerfilRepository perfiles,
            Reloj reloj) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.materias = Objects.requireNonNull(materias, "materias");
        this.ajustesRepo = Objects.requireNonNull(ajustesRepo, "ajustesRepo");
        this.noMolestar = Objects.requireNonNull(noMolestar, "noMolestar");
        this.programador = Objects.requireNonNull(programador, "programador");
        this.marcadorSalida = Objects.requireNonNull(marcadorSalida, "marcadorSalida");
        this.tramos = tramos;
        this.metas = metas;
        this.perfiles = perfiles;
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public Resultado<SesionEstudio> ejecutar(
            long sesionId, OpcionRegreso opcion, Long hastaInstanteMilis, String nota) {
        Resultado<CierreSesion> res =
                ejecutarConMetas(sesionId, opcion, hastaInstanteMilis, nota);
        return res.esValido() ? Resultado.ok(res.valor().sesion()) : Resultado.error(res.error());
    }

    @Override
    public Resultado<CierreSesion> ejecutarConMetas(
            long sesionId, OpcionRegreso opcion, Long hastaInstanteMilis, String nota) {
        return ejecutarConMetas(sesionId, opcion, hastaInstanteMilis, nota, null);
    }

    @Override
    public Resultado<CierreSesion> ejecutarConMetas(
            long sesionId,
            OpcionRegreso opcion,
            Long hastaInstanteMilis,
            String nota,
            TopicoEventoEstudio topico) {
        Objects.requireNonNull(opcion, "opcion");
        Optional<SesionEstudio> encontrada = sesiones.buscarPorId(sesionId);
        if (!encontrada.isPresent()) {
            return Resultado.error(ErrorValidacion.SESION_NO_ENCONTRADA);
        }
        SesionEstudio sesion = encontrada.get();
        List<EventoEstudio> lista = new ArrayList<>(eventos.listarPorSesion(sesionId));
        long ahoraMs = reloj.ahoraMilis();
        Long salidaPendiente = CalculadorTiempoReal.obtenerInstanteSalidaSinResolver(lista);
        long salidaMs = salidaPendiente != null
                ? salidaPendiente
                : Optional.ofNullable(CalculadorTiempoReal.obtenerUltimoInstanteDentroDeApp(lista))
                        .orElse(ahoraMs);
        TopicoEventoEstudio topicoEfectivo =
                topico != null ? topico : TopicoEventoEstudio.SIN_CLASIFICAR;

        switch (opcion) {
            case NO_SEGUI:
                return cerrarAlVolver(sesion, lista, salidaMs);
            case HASTA_HORA: {
                if (hastaInstanteMilis == null
                        || hastaInstanteMilis <= salidaMs
                        || hastaInstanteMilis > ahoraMs) {
                    return Resultado.error(ErrorValidacion.HORA_DECLARADA_FUERA_DE_RANGO);
                }
                Resultado<EventoEstudio> evCerrada = EventoEstudio.crear(
                        sesionId,
                        TipoEventoEstudio.ESTUDIO_APP_CERRADA,
                        hastaInstanteMilis,
                        reloj.fechaDe(hastaInstanteMilis),
                        reloj.horaDe(hastaInstanteMilis),
                        nota,
                        topicoEfectivo);
                if (!evCerrada.esValido()) {
                    return Resultado.error(evCerrada.error());
                }
                eventos.guardar(evCerrada.valor());
                lista.add(evCerrada.valor());
                return cerrarAlVolver(sesion, lista, hastaInstanteMilis);
            }
            case SIGO_ESTUDIANDO: {
                Resultado<EventoEstudio> evCerrada = EventoEstudio.crear(
                        sesionId,
                        TipoEventoEstudio.ESTUDIO_APP_CERRADA,
                        ahoraMs,
                        reloj.fechaDe(ahoraMs),
                        reloj.horaDe(ahoraMs),
                        nota,
                        topicoEfectivo);
                if (!evCerrada.esValido()) {
                    return Resultado.error(evCerrada.error());
                }
                EventoEstudio evRegreso = EventoEstudio.crear(
                        sesionId,
                        TipoEventoEstudio.REGRESO_APP,
                        ahoraMs,
                        reloj.fechaDe(ahoraMs),
                        reloj.horaDe(ahoraMs),
                        "",
                        topicoEfectivo).valor();
                eventos.guardar(evCerrada.valor());
                eventos.guardar(evRegreso);
                lista.add(evCerrada.valor());
                lista.add(evRegreso);
                return reanudarAlVolver(sesion, lista, ahoraMs);
            }
            case RETOMAR_SIN_SUMAR: {
                Resultado<EventoEstudio> evRegresoRes = EventoEstudio.crear(
                        sesionId,
                        TipoEventoEstudio.REGRESO_APP,
                        ahoraMs,
                        reloj.fechaDe(ahoraMs),
                        reloj.horaDe(ahoraMs),
                        nota,
                        topicoEfectivo);
                if (!evRegresoRes.esValido()) {
                    return Resultado.error(evRegresoRes.error());
                }
                eventos.guardar(evRegresoRes.valor());
                lista.add(evRegresoRes.valor());
                return reanudarAlVolver(sesion, lista, ahoraMs);
            }
        }
        throw new AssertionError("Opción no soportada: " + opcion);
    }

    private Resultado<CierreSesion> reanudarAlVolver(
            SesionEstudio sesion, List<EventoEstudio> lista, long ahoraMs) {
        marcadorSalida.limpiar();
        Ajustes ajustes = ajustesRepo.obtener();
        if (sesion.debeActivarNoMolestar(ajustes.noMolestarGlobal())
                && noMolestar.tienePermiso()) {
            noMolestar.aplicar(true);
        }
        String nombreMateria = materias.obtener(sesion.materiaId())
                .map(Materia::nombre)
                .orElse("");
        EjecucionHitos.programarProximoHito(
                sesion, nombreMateria, lista, ahoraMs, sesiones, programador, reloj);
        return Resultado.ok(new CierreSesion(sesion, java.util.Collections.emptyList()));
    }

    private Resultado<CierreSesion> cerrarAlVolver(
            SesionEstudio sesion, List<EventoEstudio> lista, long finMs) {
        EventoEstudio evFin = EventoEstudio.crear(
                sesion.id(),
                TipoEventoEstudio.FIN,
                finMs,
                reloj.fechaDe(finMs),
                reloj.horaDe(finMs),
                "").valor();
        lista.add(evFin);

        int minutos = CalculadorTiempoReal.calcularMinutosReales(
                sesion.tecnica(),
                sesion.configuracionPomodoroSnapshot(),
                sesion.duracionPlanificadaMinutos(),
                lista,
                finMs);
        Resultado<SesionEstudio> res = sesion.declararFinAlVolver(minutos);
        if (!res.esValido()) {
            return Resultado.error(res.error());
        }
        DetectorFelicitacionMetas.InstantaneaAntes antes =
                DetectorFelicitacionMetas.capturarAntes(tramos, metas, reloj);
        eventos.guardar(evFin);
        sesiones.actualizar(res.valor());
        if (tramos != null) {
            tramos.guardarTramos(
                    sesion.id(),
                    RepartidorPorDia.repartir(res.valor(), lista, finMs, minutos));
        }
        List<MetaAlcanzada> alcanzadas = DetectorFelicitacionMetas.detectarYMarcar(
                antes, metas, perfiles, materias, tramos);
        programador.cancelarAvisos(sesion.id());
        noMolestar.restaurarSiAplica();
        marcadorSalida.limpiar();
        return Resultado.ok(new CierreSesion(res.valor(), alcanzadas));
    }
}
