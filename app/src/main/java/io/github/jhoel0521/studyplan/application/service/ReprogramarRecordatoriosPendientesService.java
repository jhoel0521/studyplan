package io.github.jhoel0521.studyplan.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.recordatorios.ReprogramarRecordatoriosPendientes;
import io.github.jhoel0521.studyplan.application.port.out.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.ProgramadorRecordatorios;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.domain.readmodel.AvisoRecordatorioPendiente;
import io.github.jhoel0521.studyplan.domain.valueobject.Instante;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;

/**
 * Reprograma los avisos futuros de sesiones {@code PENDIENTE} vigentes y devuelve aquellos cuyo
 * disparo venció antes del inicio para notificarlos de inmediato ({@code HU-34}).
 */
public final class ReprogramarRecordatoriosPendientesService
        implements ReprogramarRecordatoriosPendientes {

    private final SesionRepository sesiones;
    private final MateriaRepository materias;
    private final ProgramadorRecordatorios programador;
    private final Reloj reloj;

    public ReprogramarRecordatoriosPendientesService(
            SesionRepository sesiones,
            MateriaRepository materias,
            ProgramadorRecordatorios programador,
            Reloj reloj) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
        this.materias = Objects.requireNonNull(materias, "materias");
        this.programador = Objects.requireNonNull(programador, "programador");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public List<AvisoRecordatorioPendiente> ejecutar() {
        Instante ahoraInst = reloj.ahoraInstante();
        long ahoraMs = ahoraInst.utcMilis();
        int desfaseActual = ahoraInst.desfaseMinutos();
        List<SesionEstudio> pendientes = sesiones.listarPendientesDesde(reloj.hoy());

        Map<Long, String> nombresMaterias = new HashMap<>();
        List<AvisoRecordatorioPendiente> inmediatos = new ArrayList<>();
        for (SesionEstudio sesion : pendientes) {
            Long disparo = sesion.instanteRecordatorioUtcMilis();
            if (disparo == null || sesion.id() == null) {
                continue;
            }
            if (disparo > ahoraMs) {
                programador.programar(sesion.id(), disparo);
            } else if (ahoraMs < sesion.planInicioUtcMilis()) {
                String nombreMateria = nombresMaterias.computeIfAbsent(
                        sesion.materiaId(),
                        id -> materias.obtener(id).map(Materia::nombre).orElse(""));
                inmediatos.add(new AvisoRecordatorioPendiente(
                        sesion.id(),
                        nombreMateria,
                        formatearHorario(sesion, desfaseActual)));
            }
        }
        return inmediatos;
    }

    private static String formatearHorario(SesionEstudio sesion, int desfaseActual) {
        String base = sesion.horaInicio().texto() + " \u2013 " + sesion.horaFin().texto();
        if (sesion.desfaseMinutos() == desfaseActual) {
            return base;
        }
        return base + " (" + sesion.inicioInstante().desfaseTexto() + ")";
    }
}
