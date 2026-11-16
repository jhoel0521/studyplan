package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.Ajustes;
import io.github.jhoel0521.studyplan.domain.ConfiguracionPomodoro;
import io.github.jhoel0521.studyplan.domain.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.Idioma;
import io.github.jhoel0521.studyplan.domain.Tema;

/**
 * Pantalla de ajustes: tema, idioma, bloqueo con huella o PIN, técnicas de estudio (Pomodoro y
 * No molestar) y acceso a Privacidad (borrar datos).
 *
 * <p>Implementa: HU-03, HU-04, HU-23, HU-24, RN-17 y RN-18.
 */
public class AjustesActivity extends ActividadProtegida {

    private AjustesViewModel modelo;
    private RadioGroup grupoTema;
    private RadioGroup grupoIdioma;
    private MaterialSwitch interruptorBloqueo;
    private MaterialSwitch interruptorNoMolestarGlobal;
    private TextInputEditText campoPomodoroTrabajo;
    private TextInputEditText campoPomodoroCorto;
    private TextInputEditText campoPomodoroLargo;
    private TextInputEditText campoPomodoroCiclos;
    private TextView textoErrorPomodoro;
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
        modelo.pomodoroGuardado().observe(this, this::onPomodoroGuardado);
        modelo.errorPomodoro().observe(this, this::onErrorPomodoro);
    }

    private void enlazarVistas() {
        MaterialToolbar barraSuperior = findViewById(R.id.barraSuperior);
        grupoTema = findViewById(R.id.grupoTema);
        grupoIdioma = findViewById(R.id.grupoIdioma);
        interruptorBloqueo = findViewById(R.id.interruptorBloqueo);
        interruptorNoMolestarGlobal = findViewById(R.id.interruptorNoMolestarGlobal);
        campoPomodoroTrabajo = findViewById(R.id.campoPomodoroTrabajo);
        campoPomodoroCorto = findViewById(R.id.campoPomodoroCorto);
        campoPomodoroLargo = findViewById(R.id.campoPomodoroLargo);
        campoPomodoroCiclos = findViewById(R.id.campoPomodoroCiclos);
        textoErrorPomodoro = findViewById(R.id.textoErrorPomodoro);
        MaterialButton botonGuardarPomodoro = findViewById(R.id.botonGuardarPomodoro);
        MaterialButton botonPrivacidad = findViewById(R.id.botonPrivacidad);
        barraSuperior.setNavigationOnClickListener(this::onVolverClick);
        grupoTema.setOnCheckedChangeListener(this::onTemaCambiado);
        grupoIdioma.setOnCheckedChangeListener(this::onIdiomaCambiado);
        interruptorBloqueo.setOnCheckedChangeListener(this::onBloqueoCambiado);
        interruptorNoMolestarGlobal.setOnCheckedChangeListener(this::onNoMolestarCambiado);
        botonGuardarPomodoro.setOnClickListener(this::onGuardarPomodoroClick);
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
        interruptorNoMolestarGlobal.setChecked(ajustes.noMolestarGlobal());
        ConfiguracionPomodoro pom = ajustes.pomodoro();
        campoPomodoroTrabajo.setText(String.valueOf(pom.trabajoMinutos()));
        campoPomodoroCorto.setText(String.valueOf(pom.descansoCortoMinutos()));
        campoPomodoroLargo.setText(String.valueOf(pom.descansoLargoMinutos()));
        campoPomodoroCiclos.setText(String.valueOf(pom.ciclosHastaLargo()));
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

    private void onNoMolestarCambiado(CompoundButton boton, boolean activado) {
        if (actualizando) {
            return;
        }
        if (activado && !app().controladorNoMolestar().tienePermiso()
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.ajustes_no_molestar_dialogo_titulo)
                    .setMessage(R.string.ajustes_no_molestar_dialogo_msg)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.ajustes_no_molestar_ir, (d, w) ->
                            startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)))
                    .show();
        }
        guardarEstudioDesdeFormulario(false);
    }

    private void onGuardarPomodoroClick(View vista) {
        guardarEstudioDesdeFormulario(true);
    }

    private void guardarEstudioDesdeFormulario(boolean notificar) {
        int trabajo = parsearEntero(campoPomodoroTrabajo);
        int corto = parsearEntero(campoPomodoroCorto);
        int largo = parsearEntero(campoPomodoroLargo);
        int ciclos = parsearEntero(campoPomodoroCiclos);
        modelo.guardarEstudio(
                interruptorNoMolestarGlobal.isChecked(),
                trabajo,
                corto,
                largo,
                ciclos,
                notificar);
    }

    private void onPomodoroGuardado(@Nullable Boolean guardado) {
        if (guardado != null && guardado) {
            modelo.consumirPomodoroGuardado();
            Snackbar.make(findViewById(R.id.main), R.string.ajustes_pomodoro_guardado,
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    private void onErrorPomodoro(@Nullable ErrorValidacion error) {
        if (error == null) {
            textoErrorPomodoro.setVisibility(View.GONE);
        } else {
            textoErrorPomodoro.setText(R.string.error_pomodoro_invalido);
            textoErrorPomodoro.setVisibility(View.VISIBLE);
        }
    }

    private static int parsearEntero(TextInputEditText campo) {
        if (campo.getText() == null) {
            return -1;
        }
        try {
            return Integer.parseInt(campo.getText().toString().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
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
