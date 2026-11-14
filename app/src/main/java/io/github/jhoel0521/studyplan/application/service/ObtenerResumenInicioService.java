package io.github.jhoel0521.studyplan.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.ObtenerResumenInicio;
import io.github.jhoel0521.studyplan.application.port.in.VencerSesionesPendientes;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.PerfilRepository;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.domain.DetalleSesionInicio;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.Perfil;
import io.github.jhoel0521.studyplan.domain.ResumenInicio;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;

/**
 * Implementa {@link ObtenerResumenInicio}: vence pendientes de días anteriores (RN-08), suma horas
 * reales de lunes a domingo de la semana actual (RN-11, RN-13), determina la próxima sesión
 * {@code PENDIENTE} y lista las sesiones del día (HU-02, RN-15).
 */
public final class ObtenerResumenInicioService implements ObtenerResumenInicio {

    private final PerfilRepository perfiles;
    private final SesionRepository sesiones;
    private final MateriaRepository materias;
    private final Reloj reloj;
    private final VencerSesionesPendientes vencerPendientes;

    public ObtenerResumenInicioService(
            PerfilRepository perfiles,
            SesionRepository sesiones,
            MateriaRepository materias,
            Reloj reloj,
            VencerSesionesPendientes vencerPendientes) {
        this.perfiles = Objects.requireNonNull(perfiles, "perfiles");
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.materias = Objects.requireNonNull(materias, "materias");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.vencerPendientes = Objects.requireNonNull(vencerPendientes, "vencerPendientes");
    }

    /** Constructor provisional hasta conectar el adaptador Room v3 en Bloque 3. */
    @Deprecated
    public ObtenerResumenInicioService(PerfilRepository perfiles) {
        this.perfiles = Objects.requireNonNull(perfiles, "perfiles");
        this.sesiones = null;
        this.materias = null;
        this.reloj = null;
        this.vencerPendientes = null;
    }

    @Override
    public Optional<ResumenInicio> ejecutar() {
        Optional<Perfil> perfilOpt = perfiles.obtener();
        if (!perfilOpt.isPresent()) {
            return Optional.empty();
        }
        Perfil perfil = perfilOpt.get();
        if (sesiones == null || materias == null || reloj == null || vencerPendientes == null) {
            return Optional.of(new ResumenInicio(perfil.nombre(), 0.0, perfil.metaSemanalHoras()));
        }

        vencerPendientes.ejecutar();

        Fecha hoy = reloj.hoy();
        Hora ahora = reloj.ahora();
        int minutosSemana = sesiones.sumarMinutosRealesEnRango(
                hoy.lunesDeSemana(), hoy.domingoDeSemana());
        double horasRealizadas = minutosSemana / 60.0;

        Map<Long, Materia> catalogo = cargarCatalogoMaterias();
        List<DetalleSesionInicio> sesionesDeHoy = new ArrayList<>();
        for (SesionEstudio s : sesiones.listarPorFecha(hoy)) {
            sesionesDeHoy.add(mapearDetalle(s, catalogo));
        }

        DetalleSesionInicio proxima = seleccionarProxima(hoy, ahora, catalogo);
        return Optional.of(new ResumenInicio(
                perfil.nombre(),
                horasRealizadas,
                perfil.metaSemanalHoras(),
                hoy,
                proxima,
                sesionesDeHoy));
    }

    private DetalleSesionInicio seleccionarProxima(
            Fecha hoy, Hora ahora, Map<Long, Materia> catalogo) {
        List<SesionEstudio> pendientes = sesiones.listarPendientesDesde(hoy);
        if (pendientes.isEmpty()) {
            return null;
        }
        for (SesionEstudio candidata : pendientes) {
            if (candidata.fecha().esPosteriorA(hoy)
                    || !ahora.esPosteriorA(candidata.horaFin())) {
                return mapearDetalle(candidata, catalogo);
            }
        }
        return mapearDetalle(pendientes.get(0), catalogo);
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
