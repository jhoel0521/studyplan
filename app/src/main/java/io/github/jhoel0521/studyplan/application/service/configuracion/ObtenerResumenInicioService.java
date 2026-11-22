package io.github.jhoel0521.studyplan.application.service.configuracion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.configuracion.ObtenerResumenInicio;
import io.github.jhoel0521.studyplan.application.port.in.sesiones.VencerSesionesPendientes;
import io.github.jhoel0521.studyplan.application.port.out.persistence.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.persistence.MetaSemanalRepository;
import io.github.jhoel0521.studyplan.application.port.out.persistence.PerfilRepository;
import io.github.jhoel0521.studyplan.application.port.out.system.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.persistence.SesionRepository;
import io.github.jhoel0521.studyplan.application.port.out.persistence.TramoDiarioRepository;
import io.github.jhoel0521.studyplan.domain.readmodel.DetalleSesionInicio;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Instante;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.entity.MetaSemanal;
import io.github.jhoel0521.studyplan.domain.entity.Perfil;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.readmodel.ResumenInicio;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.application.service.comun.ValidadorSesiones;

/**
 * Implementa {@link ObtenerResumenInicio}: vence pendientes de días anteriores (RN-08), suma horas
 * reales de lunes a domingo de la semana actual (RN-11, RN-13), determina la próxima sesión
 * {@code PENDIENTE} y lista las sesiones del día (HU-02, RN-15, RN-33).
 */
public final class ObtenerResumenInicioService implements ObtenerResumenInicio {

    /** Cuántas próximas sesiones se calculan (Inicio usa la primera; el widget, hasta dos; HU-05). */
    private static final int PROXIMAS_MAXIMAS = 2;

    private final PerfilRepository perfiles;
    private final SesionRepository sesiones;
    private final MateriaRepository materias;
    private final TramoDiarioRepository tramos;
    private final MetaSemanalRepository metas;
    private final Reloj reloj;
    private final VencerSesionesPendientes vencerPendientes;

    public ObtenerResumenInicioService(
            PerfilRepository perfiles,
            SesionRepository sesiones,
            MateriaRepository materias,
            Reloj reloj,
            VencerSesionesPendientes vencerPendientes) {
        this(perfiles, sesiones, materias, null, null, reloj, vencerPendientes);
    }

    public ObtenerResumenInicioService(
            PerfilRepository perfiles,
            SesionRepository sesiones,
            MateriaRepository materias,
            TramoDiarioRepository tramos,
            Reloj reloj,
            VencerSesionesPendientes vencerPendientes) {
        this(perfiles, sesiones, materias, tramos, null, reloj, vencerPendientes);
    }

    public ObtenerResumenInicioService(
            PerfilRepository perfiles,
            SesionRepository sesiones,
            MateriaRepository materias,
            TramoDiarioRepository tramos,
            MetaSemanalRepository metas,
            Reloj reloj,
            VencerSesionesPendientes vencerPendientes) {
        this.perfiles = Objects.requireNonNull(perfiles, "perfiles");
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.materias = Objects.requireNonNull(materias, "materias");
        this.tramos = tramos;
        this.metas = metas;
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.vencerPendientes = Objects.requireNonNull(vencerPendientes, "vencerPendientes");
    }

    @Override
    public Optional<ResumenInicio> ejecutar() {
        Optional<Perfil> perfilOpt = perfiles.obtener();
        if (!perfilOpt.isPresent()) {
            return Optional.empty();
        }
        Perfil perfil = perfilOpt.get();

        vencerPendientes.ejecutar();

        Fecha hoy = reloj.hoy();
        Instante ahoraInst = reloj.ahoraInstante();
        Fecha lunes = hoy.lunesDeSemana();
        Fecha domingo = hoy.domingoDeSemana();
        int minutosSemana = tramos != null
                ? tramos.sumarMinutosEnRango(lunes, domingo)
                : sesiones.sumarMinutosRealesEnRango(lunes, domingo);
        double horasRealizadas = minutosSemana / 60.0;

        Map<Long, Materia> catalogo = cargarCatalogoMaterias();
        List<DetalleSesionInicio> sesionesDeHoy = new ArrayList<>();
        for (SesionEstudio s : sesiones.listarPorFecha(hoy)) {
            sesionesDeHoy.add(mapearDetalle(s, catalogo));
        }

        List<DetalleSesionInicio> proximas = seleccionarProximas(hoy, ahoraInst, catalogo, PROXIMAS_MAXIMAS);
        DetalleSesionInicio proxima = proximas.isEmpty() ? null : proximas.get(0);
        // Sesión En curso para el aviso de Inicio y el botón «Volver a la sesión» (HU-02, HU-47).
        DetalleSesionInicio enCurso = sesiones.buscarEnCurso()
                .map(s -> mapearDetalle(s, catalogo))
                .orElse(null);
        double metaSemanaHoras = resolverMetaSemanaActual(lunes, perfil);
        return Optional.of(new ResumenInicio(
                perfil.nombre(),
                horasRealizadas,
                metaSemanaHoras,
                hoy,
                proxima,
                sesionesDeHoy,
                enCurso,
                proximas));
    }

    private double resolverMetaSemanaActual(Fecha lunes, Perfil perfil) {
        if (metas == null) {
            return perfil.metaSemanalHoras();
        }
        Optional<MetaSemanal> existente = metas.buscar(lunes, null);
        if (existente.isPresent()) {
            return existente.get().horasObjetivo();
        }
        Resultado<MetaSemanal> creada = MetaSemanal.crear(lunes, null, perfil.metaSemanalHoras());
        if (creada.esValido()) {
            metas.guardar(creada.valor());
        }
        return perfil.metaSemanalHoras();
    }

    /** Las próximas pendientes en orden: empieza en la primera que aún no terminó (o la primera de la lista). */
    private List<DetalleSesionInicio> seleccionarProximas(
            Fecha hoy, Instante ahoraInst, Map<Long, Materia> catalogo, int maximo) {
        List<SesionEstudio> pendientes = sesiones.listarPendientesDesde(hoy);
        List<DetalleSesionInicio> proximas = new ArrayList<>();
        if (pendientes.isEmpty()) {
            return proximas;
        }
        int inicio = 0;
        for (int i = 0; i < pendientes.size(); i++) {
            Instante fin = pendientes.get(i).finInstante();
            if (fin != null && fin.esPosteriorA(ahoraInst)) {
                inicio = i;
                break;
            }
        }
        for (int i = inicio; i < pendientes.size() && proximas.size() < maximo; i++) {
            proximas.add(mapearDetalle(pendientes.get(i), catalogo));
        }
        return proximas;
    }

    private DetalleSesionInicio mapearDetalle(SesionEstudio sesion, Map<Long, Materia> catalogo) {
        Materia materia = catalogo.get(sesion.materiaId());
        if (materia == null) {
            materia = materias.obtener(sesion.materiaId()).orElse(null);
            if (materia != null) {
                catalogo.put(materia.id(), materia);
            }
        }
        return ValidadorSesiones.aDetalleInicio(sesion, materia);
    }

    private Map<Long, Materia> cargarCatalogoMaterias() {
        Map<Long, Materia> catalogo = new HashMap<>();
        for (Materia m : materias.activas()) {
            catalogo.put(m.id(), m);
        }
        for (Materia m : materias.archivadas()) {
            catalogo.put(m.id(), m);
        }
        return catalogo;
    }
}
