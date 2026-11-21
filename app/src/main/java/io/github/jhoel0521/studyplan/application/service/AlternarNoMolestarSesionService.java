package io.github.jhoel0521.studyplan.application.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.jhoel0521.studyplan.application.port.in.AlternarNoMolestarSesion;
import io.github.jhoel0521.studyplan.application.port.out.AjustesRepository;
import io.github.jhoel0521.studyplan.application.port.out.ControladorNoMolestar;
import io.github.jhoel0521.studyplan.application.port.out.EventoEstudioRepository;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.domain.service.CalculadorTiempoReal;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;

/**
 * Alterna No molestar solo para la sesión {@code EN_CURSO} cambiando su preferencia a
 * {@code FORZAR_ACTIVAR} o {@code FORZAR_DESACTIVAR} y aplicando o restaurando el filtro del
 * dispositivo (HU-24, RN-18, RNF-18).
 */
public final class AlternarNoMolestarSesionService implements AlternarNoMolestarSesion {

    private final SesionRepository sesiones;
    private final EventoEstudioRepository eventos;
    private final AjustesRepository ajustes;
    private final ControladorNoMolestar noMolestar;

    public AlternarNoMolestarSesionService(
            SesionRepository sesiones,
            EventoEstudioRepository eventos,
            AjustesRepository ajustes,
            ControladorNoMolestar noMolestar) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.ajustes = Objects.requireNonNull(ajustes, "ajustes");
        this.noMolestar = Objects.requireNonNull(noMolestar, "noMolestar");
    }

    @Override
    public Resultado<Boolean> ejecutar(long sesionId) {
        Optional<SesionEstudio> enCurso = sesiones.buscarEnCurso();
        if (!enCurso.isPresent() || enCurso.get().id() != sesionId) {
            return Resultado.error(ErrorValidacion.SESION_NO_ENCONTRADA);
        }
        if (!noMolestar.tienePermiso()) {
            return Resultado.error(ErrorValidacion.SIN_PERMISO_NO_MOLESTAR);
        }
        SesionEstudio sesion = enCurso.get();
        List<EventoEstudio> lista = eventos.listarPorSesion(sesionId);
        boolean pausada = CalculadorTiempoReal.estaPausadaActualmente(lista);
        boolean global = ajustes.obtener().noMolestarGlobal();
        // Misma regla que el estado que muestra la pantalla: en pausa No molestar figura como apagado.
        boolean estabaActivo = !pausada && sesion.debeActivarNoMolestar(global);

        if (estabaActivo) {
            sesiones.actualizar(sesion.conPreferenciaNoMolestar(PreferenciaNoMolestar.FORZAR_DESACTIVAR));
            noMolestar.restaurarSiAplica();
            return Resultado.ok(false);
        }
        sesiones.actualizar(sesion.conPreferenciaNoMolestar(PreferenciaNoMolestar.FORZAR_ACTIVAR));
        if (!pausada) {
            noMolestar.aplicar(true);
        }
        return Resultado.ok(true);
    }
}
