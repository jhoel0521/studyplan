package io.github.jhoel0521.studyplan.application.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.metas.CambiarDeMateria;
import io.github.jhoel0521.studyplan.application.port.out.AjustesRepository;
import io.github.jhoel0521.studyplan.application.port.out.ControladorNoMolestar;
import io.github.jhoel0521.studyplan.application.port.out.EventoEstudioRepository;
import io.github.jhoel0521.studyplan.application.port.out.MarcadorSalidaApp;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.ProgramadorAvisosSesion;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.application.port.out.TramoDiarioRepository;
import io.github.jhoel0521.studyplan.domain.entity.Ajustes;
import io.github.jhoel0521.studyplan.domain.service.CalculadorTiempoReal;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.service.RepartidorPorDia;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio;

/**
 * Implementa {@link CambiarDeMateria}: cierra la sesión {@code EN_CURSO} guardando su tiempo real y
 * tramos diarios (sin disparar felicitación de meta, {@code RN-16}) e inicia inmediatamente una
 * nueva sesión sin plan sobre la materia destino ({@code HU-48}).
 */
public final class CambiarDeMateriaService implements CambiarDeMateria {

    private static final int COLOR_GENERAL_POR_DEFECTO = 0xFF607D8B;

    private final SesionRepository sesiones;
    private final EventoEstudioRepository eventos;
    private final MateriaRepository materias;
    private final AjustesRepository ajustesRepo;
    private final ControladorNoMolestar noMolestar;
    private final ProgramadorAvisosSesion programador;
    private final MarcadorSalidaApp marcadorSalida;
    private final TramoDiarioRepository tramos;
    private final Reloj reloj;

    public CambiarDeMateriaService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            MateriaRepository materias,
            AjustesRepository ajustesRepo,
            ControladorNoMolestar noMolestar,
            ProgramadorAvisosSesion programador,
            MarcadorSalidaApp marcadorSalida,
            TramoDiarioRepository tramos,
            Reloj reloj) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.materias = Objects.requireNonNull(materias, "materias");
        this.ajustesRepo = Objects.requireNonNull(ajustesRepo, "ajustesRepo");
        this.noMolestar = Objects.requireNonNull(noMolestar, "noMolestar");
        this.programador = Objects.requireNonNull(programador, "programador");
        this.marcadorSalida = Objects.requireNonNull(marcadorSalida, "marcadorSalida");
        this.tramos = tramos;
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public Resultado<SesionEstudio> ejecutar(
            long sesionActualId, Long nuevaMateriaId, TecnicaEstudio tecnicaNueva) {
        Resultado<SesionEstudio> actualRes = validarSesionEnCurso(sesionActualId);
        if (!actualRes.esValido()) {
            return actualRes;
        }
        Resultado<Materia> destinoRes = resolverMateria(nuevaMateriaId);
        if (!destinoRes.esValido()) {
            return Resultado.error(destinoRes.error());
        }
        return realizarCambio(actualRes.valor(), destinoRes.valor(), tecnicaNueva);
    }

    @Override
    public Resultado<SesionEstudio> ejecutarConNuevaMateria(
            long sesionActualId,
            String nombreMateria,
            int colorMateria,
            TecnicaEstudio tecnicaNueva) {
        Resultado<SesionEstudio> actualRes = validarSesionEnCurso(sesionActualId);
        if (!actualRes.esValido()) {
            return actualRes;
        }
        Resultado<Materia> creada = Materia.crear(nombreMateria, colorMateria, null);
        if (!creada.esValido()) {
            return Resultado.error(creada.error());
        }
        if (NombresDeMateria.hayActivaConClave(materias, creada.valor().clave(), null)) {
            return Resultado.error(ErrorValidacion.NOMBRE_DUPLICADO);
        }
        Materia guardada = materias.guardar(creada.valor());
        return realizarCambio(actualRes.valor(), guardada, tecnicaNueva);
    }

    private Resultado<SesionEstudio> validarSesionEnCurso(long sesionActualId) {
        Optional<SesionEstudio> encontrada = sesiones.buscarPorId(sesionActualId);
        if (!encontrada.isPresent()) {
            return Resultado.error(ErrorValidacion.SESION_NO_ENCONTRADA);
        }
        SesionEstudio sesion = encontrada.get();
        if (sesion.codigoEstado() != CodigoEstadoSesion.EN_CURSO) {
            return Resultado.error(ErrorValidacion.TRANSICION_ESTADO_NO_PERMITIDA);
        }
        return Resultado.ok(sesion);
    }

    private Resultado<Materia> resolverMateria(Long materiaId) {
        if (materiaId == null) {
            for (Materia m : materias.activas()) {
                if (m.esGeneral()) {
                    return Resultado.ok(m);
                }
            }
            return Resultado.ok(materias.guardar(Materia.crearGeneral(COLOR_GENERAL_POR_DEFECTO)));
        }
        Optional<Materia> encontrada = materias.obtener(materiaId);
        if (!encontrada.isPresent()) {
            return Resultado.error(ErrorValidacion.MATERIA_NO_ENCONTRADA);
        }
        if (encontrada.get().archivada()) {
            return Resultado.error(ErrorValidacion.MATERIA_INACTIVA);
        }
        return Resultado.ok(encontrada.get());
    }

    private Resultado<SesionEstudio> realizarCambio(
            SesionEstudio actual, Materia destino, TecnicaEstudio tecnicaNueva) {
        long ahoraMs = reloj.ahoraMilis();
        List<EventoEstudio> listaEventos = new ArrayList<>(eventos.listarPorSesion(actual.id()));
        EventoEstudio finEv = EventoEstudio.crear(
                actual.id(),
                TipoEventoEstudio.FIN,
                ahoraMs,
                reloj.fechaDe(ahoraMs),
                reloj.horaDe(ahoraMs),
                "").valor();
        listaEventos.add(finEv);

        int minutos = CalculadorTiempoReal.calcularMinutosReales(
                actual.tecnica(),
                actual.configuracionPomodoroSnapshot(),
                actual.duracionPlanificadaMinutos(),
                listaEventos,
                ahoraMs);
        Resultado<SesionEstudio> detenidaRes = actual.detener(minutos);
        if (!detenidaRes.esValido()) {
            return detenidaRes;
        }
        eventos.guardar(finEv);
        sesiones.actualizar(detenidaRes.valor());
        if (tramos != null) {
            tramos.guardarTramos(
                    actual.id(),
                    RepartidorPorDia.repartir(
                            detenidaRes.valor(),
                            listaEventos,
                            ahoraMs,
                            minutos));
        }
        programador.cancelarAvisos(actual.id());

        Ajustes ajustes = ajustesRepo.obtener();
        TecnicaEstudio tecnicaEfectiva = tecnicaNueva != null ? tecnicaNueva : actual.tecnica();
        Resultado<SesionEstudio> nuevaRes = SesionEstudio.estudiarAhora(
                destino.id(),
                reloj.ahoraInstante(),
                tecnicaEfectiva,
                ajustes.pomodoro(),
                actual.preferenciaNoMolestar());
        if (!nuevaRes.esValido()) {
            return nuevaRes;
        }
        SesionEstudio nueva = sesiones.guardar(nuevaRes.valor());
        marcadorSalida.limpiar();

        EventoEstudio inicioEv = EventoEstudio.crear(
                nueva.id(),
                TipoEventoEstudio.INICIO,
                ahoraMs,
                reloj.fechaDe(ahoraMs),
                reloj.horaDe(ahoraMs),
                "").valor();
        EventoEstudio inicioGuardado = eventos.guardar(inicioEv);

        if (nueva.debeActivarNoMolestar(ajustes.noMolestarGlobal()) && noMolestar.tienePermiso()) {
            noMolestar.aplicar(true);
        }

        EjecucionHitos.programarProximoHito(
                nueva,
                destino.nombre(),
                Collections.singletonList(inicioGuardado),
                ahoraMs,
                sesiones,
                programador,
                reloj);
        return Resultado.ok(nueva);
    }
}
