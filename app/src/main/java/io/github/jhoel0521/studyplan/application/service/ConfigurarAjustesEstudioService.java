package io.github.jhoel0521.studyplan.application.service;

import java.util.Objects;

import io.github.jhoel0521.studyplan.application.port.in.tecnicas.ConfigurarAjustesEstudio;
import io.github.jhoel0521.studyplan.application.port.out.AjustesRepository;
import io.github.jhoel0521.studyplan.domain.entity.Ajustes;
import io.github.jhoel0521.studyplan.domain.valueobject.ConfiguracionPomodoro;
import io.github.jhoel0521.studyplan.domain.result.Resultado;

/**
 * Configura el modo No molestar global y los parámetros de Pomodoro en {@link Ajustes}
 * (HU-23, HU-24, RN-17, RN-18).
 */
public final class ConfigurarAjustesEstudioService implements ConfigurarAjustesEstudio {

    private final AjustesRepository ajustesRepo;

    public ConfigurarAjustesEstudioService(AjustesRepository ajustesRepo) {
        this.ajustesRepo = Objects.requireNonNull(ajustesRepo, "ajustesRepo");
    }

    @Override
    public Resultado<Ajustes> ejecutar(
            boolean noMolestarGlobal,
            int trabajoMinutos,
            int descansoCortoMinutos,
            int descansoLargoMinutos,
            int ciclosHastaLargo) {
        Resultado<ConfiguracionPomodoro> pom = ConfiguracionPomodoro.crear(
                trabajoMinutos, descansoCortoMinutos, descansoLargoMinutos, ciclosHastaLargo);
        if (!pom.esValido()) {
            return Resultado.error(pom.error());
        }
        Ajustes actualizado = ajustesRepo.obtener()
                .conNoMolestarGlobal(noMolestarGlobal)
                .conPomodoro(pom.valor());
        ajustesRepo.guardar(actualizado);
        return Resultado.ok(actualizado);
    }
}
