package io.github.jhoel0521.studyplan.adapter.in.ui.comun;

import android.app.LocaleManager;
import android.content.Context;
import android.os.Build;
import android.os.LocaleList;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

import io.github.jhoel0521.studyplan.domain.enumeration.Idioma;
import io.github.jhoel0521.studyplan.domain.enumeration.Tema;

/**
 * Aplica el tema y el idioma elegidos. Debe llamarse en el hilo principal.
 *
 * <p>Implementa: HU-03 (RNF-07 tema claro, oscuro y del sistema; RNF-08 español e inglés).
 */
public final class AplicadorApariencia {

    public static final String PREFS_APARIENCIA = "apariencia_ui_prefs";
    private static final String CLAVE_IDIOMA = "idioma_tag";

    private AplicadorApariencia() {
    }

    public static void aplicarTema(Tema tema) {
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

    public static void aplicarIdioma(Idioma idioma) {
        aplicarIdioma(null, idioma);
    }

    public static void aplicarIdioma(Context context, Idioma idioma) {
        String etiqueta = idioma == Idioma.EN ? "en" : "es";
        if (context != null) {
            context.getSharedPreferences(PREFS_APARIENCIA, Context.MODE_PRIVATE)
                    .edit()
                    .putString(CLAVE_IDIOMA, etiqueta)
                    .apply();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                LocaleManager manager = context.getSystemService(LocaleManager.class);
                if (manager != null && !etiqueta.equals(manager.getApplicationLocales().toLanguageTags())) {
                    manager.setApplicationLocales(LocaleList.forLanguageTags(etiqueta));
                }
            }
        }
        if (!AppCompatDelegate.getApplicationLocales().toLanguageTags().equals(etiqueta)) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(etiqueta));
        }
    }

    @Nullable
    public static Locale idiomaActivo(Context base) {
        LocaleListCompat localesApp = AppCompatDelegate.getApplicationLocales();
        if (!localesApp.isEmpty() && localesApp.get(0) != null) {
            return localesApp.get(0);
        }
        if (base != null) {
            String guardado = base.getSharedPreferences(PREFS_APARIENCIA, Context.MODE_PRIVATE)
                    .getString(CLAVE_IDIOMA, null);
            if (guardado != null && !guardado.trim().isEmpty()) {
                return Locale.forLanguageTag(guardado);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                LocaleManager manager = base.getSystemService(LocaleManager.class);
                if (manager != null) {
                    LocaleList lista = manager.getApplicationLocales();
                    if (!lista.isEmpty() && lista.get(0) != null) {
                        return lista.get(0);
                    }
                }
            }
        }
        return null;
    }
}
