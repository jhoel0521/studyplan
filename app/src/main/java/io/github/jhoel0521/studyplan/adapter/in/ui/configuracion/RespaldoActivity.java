package io.github.jhoel0521.studyplan.adapter.in.ui.configuracion;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Date;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.ActividadProtegida;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.ProcesoAutorizacionDrive;
import io.github.jhoel0521.studyplan.domain.enumeration.FrecuenciaRespaldo;
import io.github.jhoel0521.studyplan.domain.readmodel.ConfiguracionRespaldo;
import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;

/**
 * Pantalla Ajustes > Copia de seguridad: vincular la cuenta de Google, elegir la frecuencia, definir o quitar la
 * contraseña (con la advertencia de que no se puede recuperar), respaldar ahora, restaurar y ver cuándo y cuánto pesó
 * el último respaldo. Si el último intento falló, ofrece volver a autorizar.
 *
 * <p>Implementa: HU-35, HU-36, HU-37, RN-19 y RN-20.
 */
public class RespaldoActivity extends ActividadProtegida implements ProcesoAutorizacionDrive.Escucha {

    private RespaldoViewModel modelo;
    private EditText campoDialogo;
    private ProcesoAutorizacionDrive autorizacion;
    private TextView textoCuenta;
    private TextView textoFallo;
    private TextView textoContrasena;
    private TextView textoUltimoRespaldo;
    private RadioGroup grupoFrecuencia;
    private MaterialButton botonVincular;
    private MaterialButton botonDesvincular;
    private MaterialButton botonDefinirContrasena;
    private MaterialButton botonQuitarContrasena;
    private MaterialButton botonRespaldarAhora;
    private MaterialButton botonRestaurar;
    private LinearProgressIndicator indicadorOcupado;
    private boolean actualizandoFrecuencia;
    private ConfiguracionRespaldo ultimaConfiguracion;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_respaldo);
        aplicarInsets();
        enlazarVistas();
        autorizacion = new ProcesoAutorizacionDrive(this, this);
        modelo = viewModel(RespaldoViewModel.class);
        modelo.configuracion().observe(this, this::onConfiguracion);
        modelo.ocupado().observe(this, this::onOcupado);
        modelo.evento().observe(this, this::onEvento);
    }

    @Override
    protected void onResume() {
        super.onResume();
        modelo.cargar();
    }

    private void enlazarVistas() {
        MaterialToolbar barraSuperior = findViewById(R.id.barraSuperior);
        barraSuperior.setNavigationOnClickListener(this::onVolverClick);
        textoCuenta = findViewById(R.id.textoCuenta);
        textoFallo = findViewById(R.id.textoFallo);
        textoContrasena = findViewById(R.id.textoContrasena);
        textoUltimoRespaldo = findViewById(R.id.textoUltimoRespaldo);
        grupoFrecuencia = findViewById(R.id.grupoFrecuencia);
        indicadorOcupado = findViewById(R.id.indicadorOcupado);
        botonVincular = findViewById(R.id.botonVincular);
        botonDesvincular = findViewById(R.id.botonDesvincular);
        botonDefinirContrasena = findViewById(R.id.botonDefinirContrasena);
        botonQuitarContrasena = findViewById(R.id.botonQuitarContrasena);
        botonRespaldarAhora = findViewById(R.id.botonRespaldarAhora);
        botonRestaurar = findViewById(R.id.botonRestaurar);
        botonVincular.setOnClickListener(this::onVincularClick);
        botonDesvincular.setOnClickListener(this::onDesvincularClick);
        botonDefinirContrasena.setOnClickListener(this::onDefinirContrasenaClick);
        botonQuitarContrasena.setOnClickListener(this::onQuitarContrasenaClick);
        botonRespaldarAhora.setOnClickListener(this::onRespaldarAhoraClick);
        botonRestaurar.setOnClickListener(this::onRestaurarClick);
        grupoFrecuencia.setOnCheckedChangeListener(this::onFrecuenciaElegida);
    }

    // ------------------------------------------------------------------------------------------ eventos

    private void onVolverClick(View vista) {
        finish();
    }

    private void onVincularClick(View vista) {
        autorizacion.iniciar();
    }

    private void onDesvincularClick(View vista) {
        modelo.desvincular();
    }

    private void onFrecuenciaElegida(RadioGroup grupo, int idElegido) {
        if (actualizandoFrecuencia || idElegido == View.NO_ID) {
            return;
        }
        modelo.cambiarFrecuencia(frecuenciaDe(idElegido));
    }

    @Override
    public void alAutorizar(String correo) {
        modelo.vincular(correo);
    }

    @Override
    public void alFallar() {
        mostrar(R.string.respaldo_err_autorizacion_fallida);
    }

    private void onDefinirContrasenaClick(View vista) {
        campoDialogo = campoContrasena();
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.respaldo_contrasena_titulo)
                .setMessage(R.string.respaldo_contrasena_advertencia)
                .setView(campoDialogo)
                .setPositiveButton(R.string.respaldo_entiendo, this::onContrasenaConfirmada)
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void onContrasenaConfirmada(DialogInterface dialogo, int boton) {
        modelo.definirContrasena(textoDe(campoDialogo));
    }

    private void onQuitarContrasenaClick(View vista) {
        modelo.quitarContrasena();
    }

    private void onRespaldarAhoraClick(View vista) {
        modelo.respaldarAhora();
    }

    private void onRestaurarClick(View vista) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.respaldo_restaurar_titulo)
                .setMessage(R.string.respaldo_restaurar_mensaje)
                .setPositiveButton(R.string.respaldo_restaurar_continuar, this::onRestauracionConfirmada)
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void onRestauracionConfirmada(DialogInterface dialogo, int boton) {
        modelo.restaurar(null);
    }

    // ------------------------------------------------------------------------------------------ estado

    private void onConfiguracion(@Nullable ConfiguracionRespaldo configuracion) {
        if (configuracion == null) {
            return;
        }
        ultimaConfiguracion = configuracion;
        boolean hayCuenta = configuracion.hayCuenta();
        textoCuenta.setText(hayCuenta
                ? getString(R.string.respaldo_cuenta, configuracion.correo())
                : getString(R.string.respaldo_sin_cuenta));
        botonVincular.setText(configuracion.ultimoIntentoFallo()
                && configuracion.ultimoResultado() == ResultadoRespaldo.AUTORIZACION_REQUERIDA
                ? R.string.respaldo_reautorizar : R.string.respaldo_vincular);
        botonVincular.setVisibility(hayCuenta && !requiereReautorizar(configuracion) ? View.GONE : View.VISIBLE);
        botonDesvincular.setVisibility(hayCuenta ? View.VISIBLE : View.GONE);
        textoContrasena.setText(configuracion.conContrasena()
                ? R.string.respaldo_contrasena_si : R.string.respaldo_contrasena_no);
        botonDefinirContrasena.setText(configuracion.conContrasena()
                ? R.string.respaldo_cambiar_contrasena : R.string.respaldo_definir_contrasena);
        botonQuitarContrasena.setVisibility(configuracion.conContrasena() ? View.VISIBLE : View.GONE);
        actualizandoFrecuencia = true;
        grupoFrecuencia.check(idDe(configuracion.frecuencia()));
        actualizandoFrecuencia = false;
        pintarUltimoRespaldo(configuracion);
        actualizarHabilitados(Boolean.TRUE.equals(modelo.ocupado().getValue()));
    }

    private static boolean requiereReautorizar(ConfiguracionRespaldo configuracion) {
        return configuracion.ultimoIntentoFallo()
                && configuracion.ultimoResultado() == ResultadoRespaldo.AUTORIZACION_REQUERIDA;
    }

    private void pintarUltimoRespaldo(ConfiguracionRespaldo configuracion) {
        if (configuracion.ultimoRespaldo() == null) {
            textoUltimoRespaldo.setText(R.string.respaldo_ultimo_ninguno);
        } else {
            String fecha = DateFormat.getMediumDateFormat(this).format(new Date(
                    configuracion.ultimoRespaldo().fechaUtcMilis()))
                    + " " + DateFormat.getTimeFormat(this).format(new Date(
                    configuracion.ultimoRespaldo().fechaUtcMilis()));
            String tamano = Formatter.formatShortFileSize(this, configuracion.ultimoRespaldo().tamanoBytes());
            textoUltimoRespaldo.setText(getString(configuracion.ultimoRespaldo().cifrado()
                    ? R.string.respaldo_ultimo_cifrado : R.string.respaldo_ultimo, fecha, tamano));
        }
        if (configuracion.ultimoIntentoFallo()) {
            textoFallo.setText(getString(R.string.respaldo_ultimo_fallo, getString(mensajeDe(
                    configuracion.ultimoResultado()))));
            textoFallo.setVisibility(View.VISIBLE);
        } else {
            textoFallo.setVisibility(View.GONE);
        }
    }

    private void onOcupado(@Nullable Boolean ocupado) {
        boolean enCurso = Boolean.TRUE.equals(ocupado);
        indicadorOcupado.setVisibility(enCurso ? View.VISIBLE : View.GONE);
        actualizarHabilitados(enCurso);
    }

    private void actualizarHabilitados(boolean enCurso) {
        boolean hayCuenta = ultimaConfiguracion != null && ultimaConfiguracion.hayCuenta();
        botonRespaldarAhora.setEnabled(hayCuenta && !enCurso);
        botonRestaurar.setEnabled(hayCuenta && !enCurso);
    }

    private void onEvento(@Nullable RespaldoViewModel.Evento evento) {
        if (evento == null) {
            return;
        }
        modelo.consumirEvento();
        switch (evento.tipo) {
            case VINCULADA:
                mostrar(R.string.respaldo_ok_vinculada);
                break;
            case DESVINCULADA:
                mostrar(R.string.respaldo_ok_desvinculada);
                break;
            case CONTRASENA_GUARDADA:
                mostrar(R.string.respaldo_ok_contrasena);
                break;
            case RESPALDADO:
                mostrar(R.string.respaldo_ok_respaldado);
                break;
            case RESTAURADO:
                mostrar(R.string.respaldo_ok_restaurado);
                break;
            default:
                onError(evento.resultado);
                break;
        }
    }

    private void onError(ResultadoRespaldo resultado) {
        if (resultado == ResultadoRespaldo.CONTRASENA_REQUERIDA
                || resultado == ResultadoRespaldo.CONTRASENA_INCORRECTA) {
            pedirContrasenaDeRestauracion(resultado);
            return;
        }
        mostrar(mensajeDe(resultado));
    }

    private void pedirContrasenaDeRestauracion(ResultadoRespaldo motivo) {
        campoDialogo = campoContrasena();
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.respaldo_restaurar_titulo)
                .setMessage(motivo == ResultadoRespaldo.CONTRASENA_INCORRECTA
                        ? R.string.respaldo_err_contrasena_incorrecta : R.string.respaldo_pedir_contrasena)
                .setView(campoDialogo)
                .setPositiveButton(R.string.respaldo_restaurar_continuar, this::onContrasenaDeRestauracion)
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void onContrasenaDeRestauracion(DialogInterface dialogo, int boton) {
        modelo.restaurar(textoDe(campoDialogo));
    }

    private void mostrar(@StringRes int mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }

    // ------------------------------------------------------------------------------------------ ayudantes

    private EditText campoContrasena() {
        EditText campo = new EditText(this);
        campo.setHint(R.string.respaldo_contrasena_hint);
        campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        campo.setMinHeight(getResources().getDimensionPixelSize(R.dimen.button_height));
        return campo;
    }

    private static char[] textoDe(EditText campo) {
        return campo.getText().toString().toCharArray();
    }

    private static int idDe(FrecuenciaRespaldo frecuencia) {
        switch (frecuencia) {
            case DIARIA:
                return R.id.frecuenciaDiaria;
            case SEMANAL:
                return R.id.frecuenciaSemanal;
            case MENSUAL:
                return R.id.frecuenciaMensual;
            case SOLO_MANUAL:
                return R.id.frecuenciaManual;
            default:
                return R.id.frecuenciaNunca;
        }
    }

    private static FrecuenciaRespaldo frecuenciaDe(int id) {
        if (id == R.id.frecuenciaDiaria) {
            return FrecuenciaRespaldo.DIARIA;
        } else if (id == R.id.frecuenciaSemanal) {
            return FrecuenciaRespaldo.SEMANAL;
        } else if (id == R.id.frecuenciaMensual) {
            return FrecuenciaRespaldo.MENSUAL;
        } else if (id == R.id.frecuenciaManual) {
            return FrecuenciaRespaldo.SOLO_MANUAL;
        }
        return FrecuenciaRespaldo.NUNCA;
    }

    /** Texto para cada resultado; lo usan también el primer uso y el borrado. */
    @StringRes
    static int mensajeDe(ResultadoRespaldo resultado) {
        switch (resultado) {
            case SIN_CUENTA:
                return R.string.respaldo_err_sin_cuenta;
            case SIN_CONEXION:
                return R.string.respaldo_err_sin_conexion;
            case AUTORIZACION_REQUERIDA:
                return R.string.respaldo_err_autorizacion;
            case SIN_RESPALDO:
                return R.string.respaldo_err_sin_respaldo;
            case CONTRASENA_REQUERIDA:
                return R.string.respaldo_err_contrasena_requerida;
            case CONTRASENA_INCORRECTA:
                return R.string.respaldo_err_contrasena_incorrecta;
            case VERSION_NO_SOPORTADA:
                return R.string.respaldo_err_version;
            case RESPALDO_CORRUPTO:
                return R.string.respaldo_err_corrupto;
            default:
                return R.string.respaldo_err_drive;
        }
    }
}
