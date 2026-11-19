package io.github.jhoel0521.studyplan.fakes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.jhoel0521.studyplan.application.port.out.ProgramadorRecordatorios;

/**
 * Falso en memoria de {@link ProgramadorRecordatorios} para pruebas unitarias ({@code HU-34}).
 */
public final class FakeProgramadorRecordatorios implements ProgramadorRecordatorios {

    private final Map<Long, Long> programadas = new LinkedHashMap<>();
    private final List<Long> cancelaciones = new ArrayList<>();
    private int limpiezasTotales;

    @Override
    public void programar(long sesionId, long instanteDisparoUtcMilis) {
        programadas.put(sesionId, instanteDisparoUtcMilis);
    }

    @Override
    public void cancelar(long sesionId) {
        programadas.remove(sesionId);
        cancelaciones.add(sesionId);
    }

    @Override
    public void cancelarTodasNotificaciones() {
        programadas.clear();
        limpiezasTotales++;
    }

    public Long instanteDe(long sesionId) {
        return programadas.get(sesionId);
    }

    public boolean estaProgramada(long sesionId) {
        return programadas.containsKey(sesionId);
    }

    public int cantidadProgramadas() {
        return programadas.size();
    }

    public List<Long> cancelaciones() {
        return cancelaciones;
    }

    public int limpiezasTotales() {
        return limpiezasTotales;
    }
}
