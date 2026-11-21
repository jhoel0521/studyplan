package io.github.jhoel0521.studyplan.application.service;

import io.github.jhoel0521.studyplan.application.port.in.ConfigurarApariencia;
import io.github.jhoel0521.studyplan.application.port.out.AjustesRepository;
import io.github.jhoel0521.studyplan.domain.enumeration.Idioma;
import io.github.jhoel0521.studyplan.domain.enumeration.Tema;

/**
 * Implementa {@link ConfigurarApariencia}: guarda el tema o el idioma elegido sin tocar los demás ajustes.
 *
 * <p>Implementa: HU-03, RNF-07 y RNF-08.
 */
public final class ConfigurarAparienciaService implements ConfigurarApariencia {

    private final AjustesRepository repositorio;

    public ConfigurarAparienciaService(AjustesRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void cambiarTema(Tema tema) {
        repositorio.guardar(repositorio.obtener().conTema(tema));
    }

    @Override
    public void cambiarIdioma(Idioma idioma) {
        repositorio.guardar(repositorio.obtener().conIdioma(idioma));
    }
}
