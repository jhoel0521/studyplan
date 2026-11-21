package io.github.jhoel0521.studyplan.application.service;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.metas.EstudiarAhora;
import io.github.jhoel0521.studyplan.application.port.out.AjustesRepository;
import io.github.jhoel0521.studyplan.application.port.out.ControladorNoMolestar;
import io.github.jhoel0521.studyplan.application.port.out.EventoEstudioRepository;
import io.github.jhoel0521.studyplan.application.port.out.MarcadorSalidaApp;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.ProgramadorAvisosSesion;
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

/**
 * Implementa {@link EstudiarAhora}: garantiza la invariante de única sesión {@code EN_CURSO},
 * resuelve o crea la materia (incluyendo {@code Estudios varios}), arranca directamente en estado
 * {@code EN_CURSO} ({@code Nueva -> En curso}) y programa los hitos iniciales ({@code HU-47},
 * {@code RN-07}, {@code RN-32}).
 */
public final class EstudiarAhoraService implements EstudiarAhora {

    private static final int COLOR_GENERAL_POR_DEFECTO = 0xFF607D8B;

    private final SesionRepository sesiones;
    private final EventoEstudioRepository eventos;
    private final MateriaRepository materias;
    private final AjustesRepository ajustesRepo;
    private final ControladorNoMolestar noMolestar;
    private final ProgramadorAvisosSesion programador;
    private final MarcadorSalidaApp marcadorSalida;
    private final Reloj reloj;

    public EstudiarAhoraService(
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
            Long materiaId, TecnicaEstudio tecnica, PreferenciaNoMolestar preferenciaNoMolestar) {
        if (sesiones.buscarEnCurso().isPresent()) {
            return Resultado.error(ErrorValidacion.SESION_YA_EN_CURSO);
        }

        Resultado<Materia> materiaRes = resolverMateria(materiaId);
        if (!materiaRes.esValido()) {
            return Resultado.error(materiaRes.error());
        }
        return arrancarSesion(materiaRes.valor(), tecnica, preferenciaNoMolestar);
    }

    @Override
    public Resultado<SesionEstudio> ejecutarConNuevaMateria(
            String nombreMateria,
            int colorMateria,
            TecnicaEstudio tecnica,
            PreferenciaNoMolestar preferenciaNoMolestar) {
        if (sesiones.buscarEnCurso().isPresent()) {
            return Resultado.error(ErrorValidacion.SESION_YA_EN_CURSO);
        }
        Resultado<Materia> nuevaRes = crearMateriaAlVuelo(nombreMateria, colorMateria);
        if (!nuevaRes.esValido()) {
            return Resultado.error(nuevaRes.error());
        }
        return arrancarSesion(nuevaRes.valor(), tecnica, preferenciaNoMolestar);
    }

    private Resultado<Materia> resolverMateria(Long materiaId) {
        if (materiaId == null) {
            for (Materia m : materias.activas()) {
                if (m.esGeneral()) {
                    return Resultado.ok(m);
                }
            }
            Materia general = Materia.crearGeneral(COLOR_GENERAL_POR_DEFECTO);
            return Resultado.ok(materias.guardar(general));
        }
        Optional<Materia> encontrada = materias.obtener(materiaId);
        if (!encontrada.isPresent()) {
            return Resultado.error(ErrorValidacion.MATERIA_NO_ENCONTRADA);
        }
        Materia materia = encontrada.get();
        if (materia.archivada()) {
            return Resultado.error(ErrorValidacion.MATERIA_INACTIVA);
        }
        return Resultado.ok(materia);
    }

    private Resultado<Materia> crearMateriaAlVuelo(String nombre, int color) {
        Resultado<Materia> creada = Materia.crear(nombre, color, null);
        if (!creada.esValido()) {
            return creada;
        }
        if (NombresDeMateria.hayActivaConClave(materias, creada.valor().clave(), null)) {
            return Resultado.error(ErrorValidacion.NOMBRE_DUPLICADO);
        }
        return Resultado.ok(materias.guardar(creada.valor()));
    }

    private Resultado<SesionEstudio> arrancarSesion(
            Materia materia, TecnicaEstudio tecnica, PreferenciaNoMolestar preferenciaNoMolestar) {
        Ajustes ajustes = ajustesRepo.obtener();
        long ahoraMs = reloj.ahoraMilis();

        Resultado<SesionEstudio> sesionRes = SesionEstudio.estudiarAhora(
                materia.id(),
                reloj.ahoraInstante(),
                tecnica,
                ajustes.pomodoro(),
                preferenciaNoMolestar);
        if (!sesionRes.esValido()) {
            return sesionRes;
        }

        SesionEstudio guardada = sesiones.guardar(sesionRes.valor());
        marcadorSalida.limpiar();

        EventoEstudio ev = EventoEstudio.crear(
                guardada.id(),
                TipoEventoEstudio.INICIO,
                ahoraMs,
                reloj.fechaDe(ahoraMs),
                reloj.horaDe(ahoraMs),
                "").valor();
        EventoEstudio evGuardado = eventos.guardar(ev);

        if (guardada.debeActivarNoMolestar(ajustes.noMolestarGlobal()) && noMolestar.tienePermiso()) {
            noMolestar.aplicar(true);
        }

        EjecucionHitos.programarProximoHito(
                guardada,
                materia.nombre(),
                Collections.singletonList(evGuardado),
                ahoraMs,
                sesiones,
                programador,
                reloj);
        return Resultado.ok(guardada);
    }
}
