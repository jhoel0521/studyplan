package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioGroup;

import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.Ajustes;
import io.github.jhoel0521.studyplan.domain.Idioma;
import io.github.jhoel0521.studyplan.domain.Tema;

/**
 * Pantalla de ajustes: tema, idioma, bloqueo con huella o PIN y acceso a Privacidad (borrar datos).
 *
 * <p>Implementa: HU-03 (tema e idioma) y HU-04 (bloqueo).
 */
public class AjustesActivity extends ActividadProtegida {

    private AjustesViewModel modelo;
    private RadioGroup grupoTema;
    private RadioGroup grupoIdioma;
    private MaterialSwitch interruptorBloqueo;
    private boolean actualizando;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);
        aplicarInsets();
        enlazarVistas();
        modelo = viewModel(AjustesViewModel.class);
        modelo.ajustes().observe(this, this::onAjustes);
        modelo.avisoSinBloqueo().observe(this, this::onAvisoSinBloqueo);
    }

    private void enlazarVistas() {
        MaterialToolbar barraSuperior = findViewById(R.id.barraSuperior);
        grupoTema = findViewById(R.id.grupoTema);
        grupoIdioma = findViewById(R.id.grupoIdioma);
        interruptorBloqueo = findViewById(R.id.interruptorBloqueo);
        MaterialButton botonPrivacidad = findViewById(R.id.botonPrivacidad);
        barraSuperior.setNavigationOnClickListener(this::onVolverClick);
        grupoTema.setOnCheckedChangeListener(this::onTemaCambiado);
        grupoIdioma.setOnCheckedChangeListener(this::onIdiomaCambiado);
        interruptorBloqueo.setOnCheckedChangeListener(this::onBloqueoCambiado);
        botonPrivacidad.setOnClickListener(this::onPrivacidadClick);
    }

    @Override
    protected void alEstarListo() {
        modelo.cargar();
    }

    private void onAjustes(Ajustes ajustes) {
        actualizando = true;
        grupoTema.check(idDeTema(ajustes.tema()));
        grupoIdioma.check(ajustes.idioma() == Idioma.EN ? R.id.idiomaIngles : R.id.idiomaEspanol);
        interruptorBloqueo.setChecked(ajustes.bloqueoActivo());
        actualizando = false;
        AplicadorApariencia.aplicarTema(ajustes.tema());
        protegerCapturas();
    }

    private void onAvisoSinBloqueo(@Nullable Boolean aviso) {
        if (aviso != null && aviso) {
            modelo.consumirAviso();
            Snackbar.make(findViewById(R.id.main), R.string.ajustes_sin_bloqueo, Snackbar.LENGTH_LONG).show();
        }
    }

    private void onVolverClick(View vista) {
        finish();
    }

    private void onTemaCambiado(RadioGroup grupo, int idElegido) {
        if (actualizando) {
            return;
        }
        if (idElegido == R.id.temaClaro) {
            modelo.elegirTema(Tema.CLARO);
        } else if (idElegido == R.id.temaOscuro) {
            modelo.elegirTema(Tema.OSCURO);
        } else {
            modelo.elegirTema(Tema.SISTEMA);
        }
    }

    private void onIdiomaCambiado(RadioGroup grupo, int idElegido) {
        if (actualizando) {
            return;
        }
        Idioma idioma = idElegido == R.id.idiomaIngles ? Idioma.EN : Idioma.ES;
        modelo.elegirIdioma(idioma);
        AplicadorApariencia.aplicarIdioma(idioma);
    }

    private void onBloqueoCambiado(CompoundButton boton, boolean activado) {
        if (actualizando) {
            return;
        }
        modelo.cambiarBloqueo(activado);
    }

    private void onPrivacidadClick(View vista) {
        startActivity(new Intent(this, BorrarDatosActivity.class));
    }

    private static int idDeTema(Tema tema) {
        switch (tema) {
            case CLARO:
                return R.id.temaClaro;
            case OSCURO:
                return R.id.temaOscuro;
            default:
                return R.id.temaSistema;
        }
    }
}
