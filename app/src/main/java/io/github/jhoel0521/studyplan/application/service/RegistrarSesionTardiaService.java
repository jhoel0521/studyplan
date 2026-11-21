package io.github.jhoel0521.studyplan.application.service;

import java.util.List;
import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.RegistrarSesionTardia;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.MetaSemanalRepository;
import io.github.jhoel0521.studyplan.application.port.out.PerfilRepository;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.application.port.out.TramoDiarioRepository;
import io.github.jhoel0521.studyplan.domain.readmodel.CierreSesion;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.readmodel.MetaAlcanzada;
import io.github.jhoel0521.studyplan.domain.service.RepartidorPorDia;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;

/**
 * Implementa {@link RegistrarSesionTardia}: convierte una sesión {@code NO_REALIZADA} de hace
 * 3 días o menos en {@code COMPLETADA_TARDIA} y detecta metas alcanzadas (HU-13, HU-28, RN-07,
 * RN-10, RN-16, RN-26, RN-31).
 */
public final class RegistrarSesionTardiaService implements RegistrarSesionTardia {

    private final SesionRepository sesiones;
    private final TramoDiarioRepository tramos;
    private final MetaSemanalRepository metas;
    private final PerfilRepository perfiles;
    private final MateriaRepository materias;
    private final Reloj reloj;

    public RegistrarSesionTardiaService(SesionRepository sesiones, Reloj reloj) {
        this(sesiones, null, null, null, null, reloj);
    }

    public RegistrarSesionTardiaService(
            SesionRepository sesiones, TramoDiarioRepository tramos, Reloj reloj) {
        this(sesiones, tramos, null, null, null, reloj);
    }

    public RegistrarSesionTardiaService(
            SesionRepository sesiones,
            TramoDiarioRepository tramos,
            MetaSemanalRepository metas,
            PerfilRepository perfiles,
            MateriaRepository materias,
            Reloj reloj) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.tramos = tramos;
        this.metas = metas;
        this.perfiles = perfiles;
        this.materias = materias;
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public Resultado<SesionEstudio> ejecutar(long sesionId, int duracionRealMinutos) {
        Resultado<CierreSesion> res = ejecutarConMetas(sesionId, duracionRealMinutos);
        return res.esValido() ? Resultado.ok(res.valor().sesion()) : Resultado.error(res.error());
    }

    @Override
    public Resultado<CierreSesion> ejecutarConMetas(long sesionId, int duracionRealMinutos) {
        SesionEstudio sesion = sesiones.buscarPorId(sesionId).orElse(null);
        if (sesion == null) {
            return Resultado.error(ErrorValidacion.SESION_NO_ENCONTRADA);
        }
        Resultado<SesionEstudio> tardia =
                sesion.registrarTardio(reloj.hoy(), duracionRealMinutos);
        if (!tardia.esValido()) {
            return Resultado.error(tardia.error());
        }
        DetectorFelicitacionMetas.InstantaneaAntes antes =
                DetectorFelicitacionMetas.capturarAntes(tramos, metas, reloj);
        sesiones.actualizar(tardia.valor());
        if (tramos != null) {
            tramos.guardarTramos(
                    sesionId,
                    RepartidorPorDia.repartir(tardia.valor(), null, 0L, duracionRealMinutos));
        }
        List<MetaAlcanzada> alcanzadas = DetectorFelicitacionMetas.detectarYMarcar(
                antes, metas, perfiles, materias, tramos);
        return Resultado.ok(new CierreSesion(tardia.valor(), alcanzadas));
    }
}
