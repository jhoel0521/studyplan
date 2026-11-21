package io.github.jhoel0521.studyplan.application.service.tecnicas;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.tecnicas.IniciarSesion;
import io.github.jhoel0521.studyplan.application.port.out.AjustesRepository;
import io.github.jhoel0521.studyplan.application.port.out.ControladorNoMolestar;
import io.github.jhoel0521.studyplan.application.port.out.EventoEstudioRepository;
import io.github.jhoel0521.studyplan.application.port.out.MarcadorSalidaApp;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.ProgramadorAvisosSesion;
import io.github.jhoel0521.studyplan.application.port.out.ProgramadorRecordatorios;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.domain.entity.Ajustes;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio;
import io.github.jhoel0521.studyplan.application.service.comun.EjecucionHitos;

/**
 * Inicia una sesión {@code PENDIENTE} garantizando la invariante de única sesión {@code EN_CURSO}
 * simultánea, guardando el snapshot Pomodoro y registrando el evento {@code INICIO}
 * (HU-22, HU-23, HU-24, RN-07, RN-17, RN-18).
 */
public final class IniciarSesionService implements IniciarSesion {

    private final SesionRepository sesiones;
    private final EventoEstudioRepository eventos;
    private final MateriaRepository materias;
    private final AjustesRepository ajustesRepo;
    private final ControladorNoMolestar noMolestar;
    private final ProgramadorAvisosSesion programador;
    private final MarcadorSalidaApp marcadorSalida;
    private final Reloj reloj;
    private final ProgramadorRecordatorios programadorRecordatorios;

    public IniciarSesionService(
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
                reloj,
                null);
    }

    public IniciarSesionService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            MateriaRepository materias,
            AjustesRepository ajustesRepo,
            ControladorNoMolestar noMolestar,
            ProgramadorAvisosSesion programador,
            MarcadorSalidaApp marcadorSalida,
            Reloj reloj,
            ProgramadorRecordatorios programadorRecordatorios) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.materias = Objects.requireNonNull(materias, "materias");
        this.ajustesRepo = Objects.requireNonNull(ajustesRepo, "ajustesRepo");
        this.noMolestar = Objects.requireNonNull(noMolestar, "noMolestar");
        this.programador = Objects.requireNonNull(programador, "programador");
        this.marcadorSalida = Objects.requireNonNull(marcadorSalida, "marcadorSalida");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.programadorRecordatorios = programadorRecordatorios;
    }

    @Override
    public Resultado<SesionEstudio> ejecutar(
            long sesionId, TecnicaEstudio tecnica, PreferenciaNoMolestar preferenciaNoMolestar) {
        Optional<SesionEstudio> activa = sesiones.buscarEnCurso();
        if (activa.isPresent() && !Objects.equals(activa.get().id(), sesionId)) {
            return Resultado.error(ErrorValidacion.SESION_YA_EN_CURSO);
        }

        Optional<SesionEstudio> encontrada = sesiones.buscarPorId(sesionId);
        if (!encontrada.isPresent()) {
            return Resultado.error(ErrorValidacion.SESION_NO_ENCONTRADA);
        }

        Ajustes ajustes = ajustesRepo.obtener();
        SesionEstudio preparada = encontrada.get().conPreferenciaNoMolestar(preferenciaNoMolestar);
        Resultado<SesionEstudio> iniciadaRes = preparada.iniciar(tecnica, ajustes.pomodoro());
        if (!iniciadaRes.esValido()) {
            return iniciadaRes;
        }

        SesionEstudio iniciada = iniciadaRes.valor();
        sesiones.actualizar(iniciada);
        if (programadorRecordatorios != null) {
            programadorRecordatorios.cancelar(sesionId);
        }
        marcadorSalida.limpiar();

        long ahoraMs = reloj.ahoraMilis();
        EventoEstudio ev = EventoEstudio.crear(
                sesionId,
                TipoEventoEstudio.INICIO,
                ahoraMs,
                reloj.fechaDe(ahoraMs),
                reloj.horaDe(ahoraMs),
                "").valor();
        EventoEstudio evGuardado = eventos.guardar(ev);

        if (iniciada.debeActivarNoMolestar(ajustes.noMolestarGlobal()) && noMolestar.tienePermiso()) {
            noMolestar.aplicar(true);
        }

        String nombreMateria = materias.obtener(iniciada.materiaId())
                .map(Materia::nombre)
                .orElse("");
        EjecucionHitos.programarProximoHito(
                iniciada,
                nombreMateria,
                Collections.singletonList(evGuardado),
                ahoraMs,
                sesiones,
                programador,
                reloj);

        return Resultado.ok(iniciada);
    }
}
