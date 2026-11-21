package io.github.jhoel0521.studyplan.application.service.tecnicas;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.tecnicas.EvaluarRecuperacionSesion;
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
import io.github.jhoel0521.studyplan.domain.service.CalculadorTiempoReal;
import io.github.jhoel0521.studyplan.domain.readmodel.DiagnosticoRegresoApp;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.readmodel.MetaAlcanzada;
import io.github.jhoel0521.studyplan.domain.service.RepartidorPorDia;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio;
import io.github.jhoel0521.studyplan.application.service.comun.DetectorFelicitacionMetas;

/**
 * Evalúa al volver a la app si existe una sesión {@code EN_CURSO} con salida sin resolver, cierre
 * abrupto ({@code SIGKILL}) o vencida por más de 3 días (HU-22, HU-41, RN-16, RN-24, RN-29).
 */
public final class EvaluarRecuperacionSesionService implements EvaluarRecuperacionSesion {

    public static final long TRES_DIAS_MILIS = 3L * 24L * 60L * 60L * 1000L;

    private final SesionRepository sesiones;
    private final EventoEstudioRepository eventos;
    private final MateriaRepository materias;
    private final ControladorNoMolestar noMolestar;
    private final ProgramadorAvisosSesion programador;
    private final MarcadorSalidaApp marcadorSalida;
    private final TramoDiarioRepository tramos;
    private final MetaSemanalRepository metas;
    private final PerfilRepository perfiles;
    private final Reloj reloj;

    public EvaluarRecuperacionSesionService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            MateriaRepository materias,
            ControladorNoMolestar noMolestar,
            ProgramadorAvisosSesion programador,
            MarcadorSalidaApp marcadorSalida,
            Reloj reloj) {
        this(sesiones, eventos, materias, noMolestar, programador, marcadorSalida, null, null, null, reloj);
    }

    public EvaluarRecuperacionSesionService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            MateriaRepository materias,
            ControladorNoMolestar noMolestar,
            ProgramadorAvisosSesion programador,
            MarcadorSalidaApp marcadorSalida,
            TramoDiarioRepository tramos,
            Reloj reloj) {
        this(sesiones, eventos, materias, noMolestar, programador, marcadorSalida, tramos, null, null, reloj);
    }

    public EvaluarRecuperacionSesionService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            MateriaRepository materias,
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
        this.noMolestar = Objects.requireNonNull(noMolestar, "noMolestar");
        this.programador = Objects.requireNonNull(programador, "programador");
        this.marcadorSalida = Objects.requireNonNull(marcadorSalida, "marcadorSalida");
        this.tramos = tramos;
        this.metas = metas;
        this.perfiles = perfiles;
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public Optional<DiagnosticoRegresoApp> ejecutar() {
        Optional<SesionEstudio> activaOpt = sesiones.buscarEnCurso();
        if (!activaOpt.isPresent()) {
            marcadorSalida.limpiar();
            return Optional.empty();
        }
        SesionEstudio sesion = activaOpt.get();
        List<EventoEstudio> lista = eventos.listarPorSesion(sesion.id());
        long ahoraMs = reloj.ahoraMilis();

        Materia materia = materias.obtener(sesion.materiaId()).orElse(null);
        String nombreMateria = materia == null ? "" : materia.nombre();
        int colorMateria = materia == null ? 0 : materia.color();

        Long ultimaMarca = CalculadorTiempoReal.obtenerUltimoInstanteDentroDeApp(lista);
        Long salidaPendiente = CalculadorTiempoReal.obtenerInstanteSalidaSinResolver(lista);

        Long sesionDescanso = marcadorSalida.obtenerSesionDescansoPomodoro();
        Long finDescansoMs = marcadorSalida.obtenerFinDescansoPomodoro();
        if (sesionDescanso != null
                && finDescansoMs != null
                && sesionDescanso.equals(sesion.id())) {
            marcadorSalida.limpiarFinDescansoPomodoro();
            if (ahoraMs <= finDescansoMs && salidaPendiente == null) {
                return Optional.empty();
            }
            if (ahoraMs > finDescansoMs && salidaPendiente == null) {
                EventoEstudio salidaExceso = eventos.guardar(EventoEstudio.crear(
                        sesion.id(),
                        TipoEventoEstudio.SALIDA_APP,
                        finDescansoMs,
                        reloj.fechaDe(finDescansoMs),
                        reloj.horaDe(finDescansoMs),
                        "").valor());
                lista = new java.util.ArrayList<>(lista);
                lista.add(salidaExceso);
                salidaPendiente = finDescansoMs;
            }
        }

        long referenciaVencimiento = salidaPendiente != null
                ? salidaPendiente
                : (ultimaMarca != null ? ultimaMarca : ahoraMs);

        if (ahoraMs - referenciaVencimiento > TRES_DIAS_MILIS) {
            long cierreMs = ultimaMarca != null ? ultimaMarca : referenciaVencimiento;
            int minutos = CalculadorTiempoReal.calcularMinutosReales(
                    sesion.tecnica(),
                    sesion.configuracionPomodoroSnapshot(),
                    sesion.duracionPlanificadaMinutos(),
                    lista,
                    cierreMs);
            Resultado<SesionEstudio> cerrada = sesion.cerrarPorPlazoVencido(minutos);
            if (cerrada.esValido()) {
                DetectorFelicitacionMetas.InstantaneaAntes antes =
                        DetectorFelicitacionMetas.capturarAntes(tramos, metas, reloj);
                EventoEstudio finEv = eventos.guardar(EventoEstudio.crear(
                        sesion.id(),
                        TipoEventoEstudio.FIN,
                        cierreMs,
                        reloj.ahoraInstante().desfaseMinutos(),
                        "").valor());
                sesiones.actualizar(cerrada.valor());
                if (tramos != null) {
                    List<EventoEstudio> conFin = new java.util.ArrayList<>(lista);
                    conFin.add(finEv);
                    tramos.guardarTramos(
                            sesion.id(),
                            RepartidorPorDia.repartir(
                                    cerrada.valor(), conFin, cierreMs, minutos));
                }
                List<MetaAlcanzada> alcanzadas = DetectorFelicitacionMetas.detectarYMarcar(
                        antes, metas, perfiles, materias, tramos);
                programador.cancelarAvisos(sesion.id());
                noMolestar.restaurarSiAplica();
                marcadorSalida.limpiar();
                return Optional.of(new DiagnosticoRegresoApp(
                        cerrada.valor(),
                        nombreMateria,
                        colorMateria,
                        cierreMs,
                        ahoraMs,
                        false,
                        true,
                        alcanzadas));
            }
        }

        if (salidaPendiente != null) {
            return Optional.of(new DiagnosticoRegresoApp(
                    sesion, nombreMateria, colorMateria, salidaPendiente, ahoraMs, false, false));
        }

        Long marcadaSesion = marcadorSalida.obtenerSesionActiva();
        Long marcadaInstante = marcadorSalida.obtenerInstanteSalida();
        if (marcadaSesion != null
                && marcadaInstante != null
                && marcadaSesion.equals(sesion.id())) {
            eventos.guardar(EventoEstudio.crear(
                    sesion.id(),
                    TipoEventoEstudio.SALIDA_APP,
                    marcadaInstante,
                    reloj.fechaDe(marcadaInstante),
                    reloj.horaDe(marcadaInstante),
                    "").valor());
            return Optional.of(new DiagnosticoRegresoApp(
                    sesion, nombreMateria, colorMateria, marcadaInstante, ahoraMs, false, false));
        }

        // Un bloqueo de pantalla sin desbloqueo, o recién desbloqueado, no es una salida: el estudiante
        // sigue estudiando y no se registra ningún evento ni se pregunta nada (HU-50, RN-22).
        if (!lista.isEmpty() && esMarcaDeBloqueo(lista.get(lista.size() - 1))) {
            return Optional.empty();
        }

        if (!CalculadorTiempoReal.estaPausadaActualmente(lista) && ultimaMarca != null) {
            eventos.guardar(EventoEstudio.crear(
                    sesion.id(),
                    TipoEventoEstudio.SALIDA_APP,
                    ultimaMarca,
                    reloj.fechaDe(ultimaMarca),
                    reloj.horaDe(ultimaMarca),
                    "").valor());
            return Optional.of(new DiagnosticoRegresoApp(
                    sesion, nombreMateria, colorMateria, ultimaMarca, ahoraMs, true, false));
        }

        return Optional.empty();
    }

    private static boolean esMarcaDeBloqueo(EventoEstudio evento) {
        return evento.tipo() == TipoEventoEstudio.BLOQUEO_PANTALLA
                || evento.tipo() == TipoEventoEstudio.DESBLOQUEO;
    }
}
