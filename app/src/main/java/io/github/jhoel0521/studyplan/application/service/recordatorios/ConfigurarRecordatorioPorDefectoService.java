package io.github.jhoel0521.studyplan.application.service.recordatorios;

import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.recordatorios.ConfigurarRecordatorioPorDefecto;
import io.github.jhoel0521.studyplan.application.port.out.persistence.AjustesRepository;
import io.github.jhoel0521.studyplan.domain.entity.Ajustes;
import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;

/**
 * Persiste la antelación predeterminada en {@link Ajustes} sin alterar sesiones ya planificadas
 * ({@code HU-34}).
 */
public final class ConfigurarRecordatorioPorDefectoService
        implements ConfigurarRecordatorioPorDefecto {

    private final AjustesRepository ajustesRepo;

    public ConfigurarRecordatorioPorDefectoService(AjustesRepository ajustesRepo) {
        this.ajustesRepo = Objects.requireNonNull(ajustesRepo, "ajustesRepo");
    }

    @Override
    public Ajustes ejecutar(AntelacionRecordatorio antelacion) {
        AntelacionRecordatorio efectiva = antelacion == null
                ? AntelacionRecordatorio.SIN_RECORDATORIO
                : antelacion;
        Ajustes actualizado = ajustesRepo.obtener().conRecordatorioPorDefecto(efectiva);
        ajustesRepo.guardar(actualizado);
        return actualizado;
    }
}
