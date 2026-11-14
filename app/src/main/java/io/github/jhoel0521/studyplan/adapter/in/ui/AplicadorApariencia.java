package io.github.jhoel0521.studyplan.adapter.in.ui;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import io.github.jhoel0521.studyplan.domain.Idioma;
import io.github.jhoel0521.studyplan.domain.Tema;

/**
 * Aplica el tema y el idioma elegidos. Debe llamarse en el hilo principal.
 *
 * <p>Implementa: HU-03 (RNF-07 tema claro, oscuro y del sistema; RNF-08 español e inglés).
 */
final class AplicadorApariencia {

    private AplicadorApariencia() {
    }

    static void aplicarTema(Tema tema) {
        int modo;
        switch (tema) {
            case CLARO:
                modo = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case OSCURO:
                modo = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            default:
                modo = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        AppCompatDelegate.setDefaultNightMode(modo);
    }

    static void aplicarIdioma(Idioma idioma) {
        String etiqueta = idioma == Idioma.EN ? "en" : "es";
        if (!AppCompatDelegate.getApplicationLocales().toLanguageTags().equals(etiqueta)) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(etiqueta));
        }
    }
}
