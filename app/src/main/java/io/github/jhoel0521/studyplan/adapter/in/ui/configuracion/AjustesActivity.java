package io.github.jhoel0521.studyplan.adapter.in.ui.configuracion;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.adapter.out.system.BloqueoPantallaSistema;
import io.github.jhoel0521.studyplan.domain.entity.Ajustes;
import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.domain.valueobject.ConfiguracionPomodoro;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.enumeration.Idioma;
import io.github.jhoel0521.studyplan.domain.enumeration.Tema;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.ActividadProtegida;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.AplicadorApariencia;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.FormatoSesion;
import io.github.jhoel0521.studyplan.adapter.in.ui.tecnicas.NotificadorSesion;

/**
 * Pantalla de ajustes: tema, idioma, bloqueo con huella o PIN, técnicas de estudio (Pomodoro y
 * No molestar), recordatorios de sesiones y acceso a Privacidad (borrar datos).
 *
 * <p>Implementa: HU-03, HU-04, HU-23, HU-24, HU-34, RN-17, RN-18 y RNF-18.
 */
public class AjustesActivity extends ActividadProtegida {

    private AjustesViewModel modelo;
    private RadioGroup grupoTema;
    private RadioGroup grupoIdioma;
    private MaterialSwitch interruptorBloqueo;
    private MaterialButton botonQuitarPermisoBloqueo;
    private MaterialSwitch interruptorNoMolestarGlobal;
    private TextInputEditText campoPomodoroTrabajo;
    private TextInputEditText campoPomodoroCorto;
    private TextInputEditText campoPomodoroLargo;
    private TextInputEditText campoPomodoroCiclos;
    private TextView textoErrorPomodoro;
    private MaterialAutoCompleteTextView selectorRecordatorioDefecto;
    private LinearLayout contenedorPermisoNotificaciones;
    private LinearLayout contenedorPermisoAlarmas;
    @Nullable
    private AlertDialog dialogoPermiso;
    private AntelacionRecordatorio antelacionSeleccionada = AntelacionRecordatorio.MINUTOS_15;
    private boolean actualizando;

    private final ActivityResultLauncher<String> lanzadorPermisoNotificaciones =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    this::onResultadoPermisoNotificaciones);

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
        selectorRecordatorioDefecto = findViewById(R.id.selectorRecordatorioDefecto);
        contenedorPermisoNotificaciones = findViewById(R.id.contenedorPermisoNotificaciones);
        contenedorPermisoAlarmas = findViewById(R.id.contenedorPermisoAlarmas);
        MaterialButton botonAbrirAjustesNotificaciones =
                findViewById(R.id.botonAbrirAjustesNotificaciones);
        MaterialButton botonAbrirAjustesAlarmas = findViewById(R.id.botonAbrirAjustesAlarmas);
        MaterialButton botonPrivacidad = findViewById(R.id.botonPrivacidad);
        botonQuitarPermisoBloqueo = findViewById(R.id.botonQuitarPermisoBloqueo);

        selectorRecordatorioDefecto.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                FormatoSesion.etiquetasAntelacion(this)));

        barraSuperior.setNavigationOnClickListener(this::onVolverClick);
        grupoTema.setOnCheckedChangeListener(this::onTemaCambiado);
        grupoIdioma.setOnCheckedChangeListener(this::onIdiomaCambiado);
        interruptorBloqueo.setOnCheckedChangeListener(this::onBloqueoCambiado);
        interruptorNoMolestarGlobal.setOnCheckedChangeListener(this::onNoMolestarCambiado);
        botonGuardarPomodoro.setOnClickListener(this::onGuardarPomodoroClick);
        selectorRecordatorioDefecto.setOnItemClickListener(this::onRecordatorioDefectoElegido);
        botonAbrirAjustesNotificaciones.setOnClickListener(this::onAbrirAjustesNotificacionesClick);
        botonAbrirAjustesAlarmas.setOnClickListener(this::onAbrirAjustesAlarmasClick);
        botonPrivacidad.setOnClickListener(this::onPrivacidadClick);
        botonQuitarPermisoBloqueo.setOnClickListener(this::onQuitarPermisoBloqueoClick);
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarBannersPermisos();
        botonQuitarPermisoBloqueo.setVisibility(
                new BloqueoPantallaSistema(this).estaActivo() ? View.VISIBLE : View.GONE);
    }

    /** Retira el permiso de bloquear la pantalla para poder desinstalar la app sin pasos extra (HU-50). */
    private void onQuitarPermisoBloqueoClick(View vista) {
        new BloqueoPantallaSistema(this).quitarPermiso();
        botonQuitarPermisoBloqueo.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        cerrarDialogoPermiso();
        super.onDestroy();
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
        antelacionSeleccionada = ajustes.recordatorioPorDefecto();
        selectorRecordatorioDefecto.setText(
                FormatoSesion.etiquetaAntelacion(this, antelacionSeleccionada), false);
        actualizarBannersPermisos();
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
        AplicadorApariencia.aplicarIdioma(this, idioma);
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
        if (activado && !app().controladorNoMolestar().tienePermiso()) {
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

    private void onRecordatorioDefectoElegido(
            AdapterView<?> padre, View vista, int posicion, long id) {
        AntelacionRecordatorio[] valores = AntelacionRecordatorio.values();
        if (posicion < 0 || posicion >= valores.length) {
            return;
        }
        antelacionSeleccionada = valores[posicion];
        modelo.elegirRecordatorioPorDefecto(antelacionSeleccionada);
        actualizarBannersPermisos();
        if (!antelacionSeleccionada.activo()) {
            return;
        }
        if (!NotificadorSesion.tienePermisoNotificaciones(this)) {
            mostrarDialogoExplicacionNotificaciones();
        } else if (!NotificadorSesion.puedeProgramarAlarmasExactas(this)) {
            mostrarDialogoExplicacionAlarmas();
        }
    }

    private void onAbrirAjustesNotificacionesClick(View vista) {
        NotificadorSesion.abrirAjustesNotificaciones(this);
    }

    private void onAbrirAjustesAlarmasClick(View vista) {
        NotificadorSesion.abrirAjustesAlarmasExactas(this);
    }

    private void actualizarBannersPermisos() {
        if (contenedorPermisoNotificaciones == null || contenedorPermisoAlarmas == null) {
            return;
        }
        boolean mostrar = antelacionSeleccionada.activo();
        boolean faltaNotif = mostrar && !NotificadorSesion.tienePermisoNotificaciones(this);
        boolean faltaAlarmas = mostrar && !NotificadorSesion.puedeProgramarAlarmasExactas(this);
        contenedorPermisoNotificaciones.setVisibility(faltaNotif ? View.VISIBLE : View.GONE);
        contenedorPermisoAlarmas.setVisibility(faltaAlarmas ? View.VISIBLE : View.GONE);
    }

    private void mostrarDialogoExplicacionNotificaciones() {
        cerrarDialogoPermiso();
        dialogoPermiso = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.permiso_notificaciones_dialogo_titulo)
                .setMessage(R.string.permiso_notificaciones_dialogo_msg)
                .setNegativeButton(R.string.accion_ahora_no, null)
                .setPositiveButton(R.string.accion_continuar, this::onConfirmarPermisoNotificaciones)
                .show();
    }

    private void onConfirmarPermisoNotificaciones(DialogInterface dialogo, int boton) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lanzadorPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            NotificadorSesion.abrirAjustesNotificaciones(this);
        }
    }

    private void onResultadoPermisoNotificaciones(Boolean concedido) {
        actualizarBannersPermisos();
        if (Boolean.TRUE.equals(concedido)
                && antelacionSeleccionada.activo()
                && !NotificadorSesion.puedeProgramarAlarmasExactas(this)) {
            mostrarDialogoExplicacionAlarmas();
        }
    }

    private void mostrarDialogoExplicacionAlarmas() {
        cerrarDialogoPermiso();
        dialogoPermiso = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.permiso_alarmas_dialogo_titulo)
                .setMessage(R.string.permiso_alarmas_dialogo_msg)
                .setNegativeButton(R.string.accion_ahora_no, null)
                .setPositiveButton(R.string.accion_continuar, this::onConfirmarAjustesAlarmas)
                .show();
    }

    private void onConfirmarAjustesAlarmas(DialogInterface dialogo, int boton) {
        NotificadorSesion.abrirAjustesAlarmasExactas(this);
    }

    private void cerrarDialogoPermiso() {
        if (dialogoPermiso != null) {
            dialogoPermiso.dismiss();
            dialogoPermiso = null;
        }
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
