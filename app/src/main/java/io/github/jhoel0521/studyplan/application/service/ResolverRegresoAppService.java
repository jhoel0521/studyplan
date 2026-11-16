package io.github.jhoel0521.studyplan.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.ResolverRegresoApp;
import io.github.jhoel0521.studyplan.application.port.out.AjustesRepository;
import io.github.jhoel0521.studyplan.application.port.out.ControladorNoMolestar;
import io.github.jhoel0521.studyplan.application.port.out.EventoEstudioRepository;
import io.github.jhoel0521.studyplan.application.port.out.MarcadorSalidaApp;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.ProgramadorAvisosSesion;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.domain.Ajustes;
import io.github.jhoel0521.studyplan.domain.CalculadorTiempoReal;
import io.github.jhoel0521.studyplan.domain.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.TipoEventoEstudio;

/**
 * Resuelve la declaración del estudiante al volver con una sesión {@code EN_CURSO}
 * ({@code NO_SEGUI}, {@code HASTA_HORA}, {@code SIGO_ESTUDIANDO}) según HU-41 y RN-24.
 */
public final class ResolverRegresoAppService implements ResolverRegresoApp {

    private final SesionRepository sesiones;
    private final EventoEstudioRepository eventos;
    private final MateriaRepository materias;
    private final AjustesRepository ajustesRepo;
    private final ControladorNoMolestar noMolestar;
    private final ProgramadorAvisosSesion programador;
    private final MarcadorSalidaApp marcadorSalida;
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
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.materias = Objects.requireNonNull(materias, "materias");
        this.ajustesRepo = Objects.requireNonNull(ajustesRepo, "ajustesRepo");
        this.noMolestar = Objects.requireNonNull(noMolestar, "noMolestar");
        this.programador = Objects.requireNonNull(programador, "programador");
        this.marcadorSalida = Objects.requireNonNull(marcadorSalida, "marcadorSalida");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public Resultado<SesionEstudio> ejecutar(
            long sesionId, OpcionRegreso opcion, Long hastaInstanteMilis, String nota) {
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
                        nota);
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
                        nota);
                if (!evCerrada.esValido()) {
                    return Resultado.error(evCerrada.error());
                }
                EventoEstudio evRegreso = EventoEstudio.crear(
                        sesionId,
                        TipoEventoEstudio.REGRESO_APP,
                        ahoraMs,
                        reloj.fechaDe(ahoraMs),
                        reloj.horaDe(ahoraMs),
                        "").valor();
                eventos.guardar(evCerrada.valor());
                eventos.guardar(evRegreso);
                lista.add(evCerrada.valor());
                lista.add(evRegreso);
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
                return Resultado.ok(sesion);
            }
        }
        throw new AssertionError("Opción no soportada: " + opcion);
    }

    private Resultado<SesionEstudio> cerrarAlVolver(
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
            return res;
        }
        eventos.guardar(evFin);
        sesiones.actualizar(res.valor());
        programador.cancelarAvisos(sesion.id());
        noMolestar.restaurarSiAplica();
        marcadorSalida.limpiar();
        return res;
    }
}
