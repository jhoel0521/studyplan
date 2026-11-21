package io.github.jhoel0521.studyplan.application.service.tecnicas;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.tecnicas.PausarReanudarSesion;
import io.github.jhoel0521.studyplan.application.port.out.AjustesRepository;
import io.github.jhoel0521.studyplan.application.port.out.ControladorNoMolestar;
import io.github.jhoel0521.studyplan.application.port.out.EventoEstudioRepository;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.ProgramadorAvisosSesion;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.domain.entity.Ajustes;
import io.github.jhoel0521.studyplan.domain.service.CalculadorTiempoReal;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TopicoEventoEstudio;
import io.github.jhoel0521.studyplan.application.service.comun.EjecucionHitos;

/**
 * Alterna pausa y reanudación en una sesión {@code EN_CURSO} gestionando eventos, avisos y No
 * molestar (HU-22, HU-24, RN-07, RN-18).
 */
public final class PausarReanudarSesionService implements PausarReanudarSesion {

    private final SesionRepository sesiones;
    private final EventoEstudioRepository eventos;
    private final MateriaRepository materias;
    private final AjustesRepository ajustesRepo;
    private final ControladorNoMolestar noMolestar;
    private final ProgramadorAvisosSesion programador;
    private final Reloj reloj;

    public PausarReanudarSesionService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            MateriaRepository materias,
            AjustesRepository ajustesRepo,
            ControladorNoMolestar noMolestar,
            ProgramadorAvisosSesion programador,
            Reloj reloj) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.materias = Objects.requireNonNull(materias, "materias");
        this.ajustesRepo = Objects.requireNonNull(ajustesRepo, "ajustesRepo");
        this.noMolestar = Objects.requireNonNull(noMolestar, "noMolestar");
        this.programador = Objects.requireNonNull(programador, "programador");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public Resultado<SesionEstudio> ejecutar(long sesionId) {
        return ejecutar(sesionId, null, "");
    }

    @Override
    public Resultado<SesionEstudio> ejecutar(
            long sesionId, TopicoEventoEstudio topico, String nota) {
        Optional<SesionEstudio> encontrada = sesiones.buscarPorId(sesionId);
        if (!encontrada.isPresent()) {
            return Resultado.error(ErrorValidacion.SESION_NO_ENCONTRADA);
        }
        Resultado<SesionEstudio> transicion = encontrada.get().pausarOReanudar();
        if (!transicion.esValido()) {
            return transicion;
        }

        SesionEstudio sesion = transicion.valor();
        List<EventoEstudio> lista = new ArrayList<>(eventos.listarPorSesion(sesionId));
        boolean estabaPausada = CalculadorTiempoReal.estaPausadaActualmente(lista);
        long ahoraMs = reloj.ahoraMilis();

        TipoEventoEstudio tipo = estabaPausada ? TipoEventoEstudio.REANUDACION : TipoEventoEstudio.PAUSA;
        TopicoEventoEstudio topicoEfectivo = estabaPausada
                ? (topico != null ? topico : TopicoEventoEstudio.SIN_CLASIFICAR)
                : null;
        String notaEfectiva = estabaPausada && nota != null ? nota : "";
        Resultado<EventoEstudio> eventoRes = EventoEstudio.crear(
                sesionId,
                tipo,
                ahoraMs,
                reloj.fechaDe(ahoraMs),
                reloj.horaDe(ahoraMs),
                notaEfectiva,
                topicoEfectivo);
        if (!eventoRes.esValido()) {
            return Resultado.error(eventoRes.error());
        }
        EventoEstudio nuevo = eventos.guardar(eventoRes.valor());
        lista.add(nuevo);
        sesiones.actualizar(sesion);

        if (estabaPausada) {
            Ajustes ajustes = ajustesRepo.obtener();
            if (sesion.debeActivarNoMolestar(ajustes.noMolestarGlobal()) && noMolestar.tienePermiso()) {
                noMolestar.aplicar(true);
            }
            String nombreMateria = materias.obtener(sesion.materiaId())
                    .map(Materia::nombre)
                    .orElse("");
            EjecucionHitos.programarProximoHito(
                    sesion, nombreMateria, lista, ahoraMs, sesiones, programador, reloj);
        } else {
            programador.cancelarAvisos(sesionId);
            noMolestar.restaurarSiAplica();
        }
        return Resultado.ok(sesion);
    }
}
