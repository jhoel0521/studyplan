package io.github.jhoel0521.studyplan.application.port.in.configuracion;

import io.github.jhoel0521.studyplan.domain.enumeration.Idioma;
import io.github.jhoel0521.studyplan.domain.enumeration.Tema;

/**
 * Puerto de entrada (caso de uso): cambiar el tema y el idioma. La interfaz aplica el cambio a la pantalla; este caso
 * de uso solo lo guarda para que se conserve al reiniciar.
 *
 * <p>Implementa: HU-03 (Configurar tema e idioma), RNF-07 y RNF-08.
 */
public interface ConfigurarApariencia {

    void cambiarTema(Tema tema);

    void cambiarIdioma(Idioma idioma);
}
