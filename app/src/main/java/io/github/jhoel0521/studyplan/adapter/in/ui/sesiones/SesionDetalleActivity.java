package io.github.jhoel0521.studyplan.adapter.in.ui.sesiones;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.readmodel.DetalleSesion;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.AcentoMateria;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.ActividadProtegida;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.EstadoSesionVisual;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.FormatoSesion;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.MensajesDeError;
import io.github.jhoel0521.studyplan.adapter.in.ui.configuracion.MainActivity;
import io.github.jhoel0521.studyplan.adapter.in.ui.tecnicas.DialogoRecuperacionSesion;
import io.github.jhoel0521.studyplan.adapter.in.ui.tecnicas.EventoEstudioAdapter;
import io.github.jhoel0521.studyplan.adapter.in.ui.tecnicas.NotificadorSesion;
import io.github.jhoel0521.studyplan.adapter.in.ui.tecnicas.SesionEnCursoActivity;

/**
 * Pantalla de detalle completo de una sesión de estudio y punto de transición de estados
 * (completar el mismo día, registrar tardíamente hasta 3 días, reprogramar o eliminar en Pendiente).
 *
 * <p>Implementa: HU-04, HU-12, HU-13, HU-14, HU-15, RNF-06, RNF-10 y RNF-12.
 */
public class SesionDetalleActivity extends ActividadProtegida {

    public static final String EXTRA_SESION_ID = "extra_sesion_id";

    private SesionDetalleViewModel modelo;
    private long sesionId = -1L;
    private Long destinoReprogramadaId;
    private boolean duracionInicialAplicada;
    @Nullable
    private AlertDialog dialogoEliminar;

    private TextView textoSesionNoEncontrada;
    private LinearLayout contenedorDetalle;
    private ImageView colorMateriaDetalle;
    private TextView nombreMateriaDetalle;
    private ImageView iconoEstadoDetalle;
    private TextView etiquetaEstadoDetalle;
    private TextView textoHorarioDetalle;
    private TextView textoDuracionPlanificadaDetalle;
    private TextView textoDuracionRealDetalle;
    private TextView textoRecordatorioDetalle;
    private TextView textoNotasDetalle;
    private MaterialButton botonVerReprogramada;
    private TextView textoPlazoVencido;
    private TextInputLayout grupoDuracionRealDetalle;
    private TextInputEditText campoDuracionRealDetalle;
    private TextView textoErrorDetalle;
    private LinearLayout contenedorInicioSesion;
    private RadioGroup grupoTecnicaDetalle;
    private RadioGroup grupoDndDetalle;
    private MaterialButton botonIniciarSesion;
    private MaterialButton botonIrEnCurso;
    private MaterialButton botonRegistrarTardio;
    private MaterialButton botonReprogramarSesion;
    private MaterialButton botonEliminarSesion;
    private TextView textoSinEventosDetalle;
    private EventoEstudioAdapter adaptadorEventos;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sesion_detalle);
        aplicarInsets();
        sesionId = getIntent().getLongExtra(EXTRA_SESION_ID, -1L);
        enlazarVistas();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                cerrarOVolverAInicio();
            }
        });

        modelo = viewModel(SesionDetalleViewModel.class);
        modelo.detalle().observe(this, this::onDetalle);
        modelo.noEncontrada().observe(this, this::onNoEncontrada);
        modelo.evento().observe(this, this::onEvento);
        modelo.confirmandoEliminar().observe(this, this::onConfirmandoEliminar);
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        sesionId = intent.getLongExtra(EXTRA_SESION_ID, -1L);
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)
                && !app().sesionBloqueo().estaBloqueada()) {
            alEstarListo();
        }
    }

    @Override
    protected void alEstarListo() {
        if (sesionId > 0) {
            NotificadorSesion.cancelarRecordatorio(this, sesionId);
        }
        modelo.cargar(sesionId);
    }

    @Override
    protected void onDestroy() {
        cerrarDialogoEliminar();
        super.onDestroy();
    }

    private void enlazarVistas() {
        MaterialToolbar barraSuperior = findViewById(R.id.barraSuperior);
        textoSesionNoEncontrada = findViewById(R.id.textoSesionNoEncontrada);
        contenedorDetalle = findViewById(R.id.contenedorDetalle);
        colorMateriaDetalle = findViewById(R.id.colorMateriaDetalle);
        nombreMateriaDetalle = findViewById(R.id.nombreMateriaDetalle);
        iconoEstadoDetalle = findViewById(R.id.iconoEstadoDetalle);
        etiquetaEstadoDetalle = findViewById(R.id.etiquetaEstadoDetalle);
        textoHorarioDetalle = findViewById(R.id.textoHorarioDetalle);
        textoDuracionPlanificadaDetalle = findViewById(R.id.textoDuracionPlanificadaDetalle);
        textoDuracionRealDetalle = findViewById(R.id.textoDuracionRealDetalle);
        textoRecordatorioDetalle = findViewById(R.id.textoRecordatorioDetalle);
        textoNotasDetalle = findViewById(R.id.textoNotasDetalle);
        botonVerReprogramada = findViewById(R.id.botonVerReprogramada);
        textoPlazoVencido = findViewById(R.id.textoPlazoVencido);
        grupoDuracionRealDetalle = findViewById(R.id.grupoDuracionRealDetalle);
        campoDuracionRealDetalle = findViewById(R.id.campoDuracionRealDetalle);
        textoErrorDetalle = findViewById(R.id.textoErrorDetalle);
        contenedorInicioSesion = findViewById(R.id.contenedorInicioSesion);
        grupoTecnicaDetalle = findViewById(R.id.grupoTecnicaDetalle);
        grupoDndDetalle = findViewById(R.id.grupoDndDetalle);
        botonIniciarSesion = findViewById(R.id.botonIniciarSesion);
        botonIrEnCurso = findViewById(R.id.botonIrEnCurso);
        botonRegistrarTardio = findViewById(R.id.botonRegistrarTardio);
        botonReprogramarSesion = findViewById(R.id.botonReprogramarSesion);
        botonEliminarSesion = findViewById(R.id.botonEliminarSesion);
        textoSinEventosDetalle = findViewById(R.id.textoSinEventosDetalle);
        RecyclerView listaEventosDetalle = findViewById(R.id.listaEventosDetalle);
        adaptadorEventos = new EventoEstudioAdapter();
        listaEventosDetalle.setLayoutManager(new LinearLayoutManager(this));
        listaEventosDetalle.setAdapter(adaptadorEventos);

        barraSuperior.setNavigationOnClickListener(this::onVolverClick);
        botonVerReprogramada.setOnClickListener(this::onVerReprogramadaClick);
        botonIniciarSesion.setOnClickListener(this::onIniciarClick);
        botonIrEnCurso.setOnClickListener(this::onIrEnCursoClick);
        botonRegistrarTardio.setOnClickListener(this::onRegistrarTardioClick);
        botonReprogramarSesion.setOnClickListener(this::onReprogramarClick);
        botonEliminarSesion.setOnClickListener(this::onEliminarClick);
    }

    private void onVolverClick(View vista) {
        cerrarOVolverAInicio();
    }

    private void cerrarOVolverAInicio() {
        if (isTaskRoot()) {
            Intent inicio = new Intent(this, MainActivity.class);
            inicio.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(inicio);
        }
        finish();
    }

    private void onNoEncontrada(boolean noExiste) {
        if (noExiste) {
            contenedorDetalle.setVisibility(View.GONE);
            textoSesionNoEncontrada.setVisibility(View.VISIBLE);
        }
    }

    private void onDetalle(@Nullable DetalleSesion detalle) {
        if (detalle == null) {
            return;
        }
        textoSesionNoEncontrada.setVisibility(View.GONE);
        contenedorDetalle.setVisibility(View.VISIBLE);

        SesionEstudio s = detalle.sesion();
        AcentoMateria.punto(colorMateriaDetalle, detalle.colorMateria());
        nombreMateriaDetalle.setText(detalle.nombreMateria());
        EstadoSesionVisual.aplicar(s.codigoEstado(), iconoEstadoDetalle, etiquetaEstadoDetalle);

        textoHorarioDetalle.setText(FormatoSesion.horarioSesionConZona(
                s, FormatoSesion.desfaseActualSistema()));
        textoDuracionPlanificadaDetalle.setText(
                getString(R.string.sesion_detalle_planificada, s.duracionPlanificadaMinutos()));

        Integer real = s.duracionRealMinutos();
        if (real != null && real > 0) {
            textoDuracionRealDetalle.setText(getString(R.string.sesion_detalle_real, real));
            textoDuracionRealDetalle.setVisibility(View.VISIBLE);
        } else {
            textoDuracionRealDetalle.setVisibility(View.GONE);
        }

        if (s.planFinUtcMilis() != null) {
            textoRecordatorioDetalle.setText(getString(
                    R.string.detalle_recordatorio,
                    FormatoSesion.etiquetaAntelacion(this, s.antelacionRecordatorio())));
            textoRecordatorioDetalle.setVisibility(View.VISIBLE);
        } else {
            textoRecordatorioDetalle.setVisibility(View.GONE);
        }

        textoNotasDetalle.setText(!s.notas().trim().isEmpty()
                ? s.notas()
                : getString(R.string.sesion_detalle_sin_notas));

        SesionEstudio destino = detalle.sesionDestino();
        if (destino != null) {
            destinoReprogramadaId = destino.id();
            botonVerReprogramada.setText(getString(
                    R.string.sesion_detalle_enlace_destino,
                    destino.fecha().iso(),
                    destino.horaInicio().texto(),
                    destino.horaFin().texto()));
            botonVerReprogramada.setVisibility(View.VISIBLE);
        } else {
            destinoReprogramadaId = null;
            botonVerReprogramada.setVisibility(View.GONE);
        }

        boolean requiereDuracionEditable = detalle.puedeRegistrarTardio();
        grupoDuracionRealDetalle.setVisibility(requiereDuracionEditable ? View.VISIBLE : View.GONE);
        if (requiereDuracionEditable && !duracionInicialAplicada && textoDe(campoDuracionRealDetalle).isEmpty()) {
            duracionInicialAplicada = true;
            campoDuracionRealDetalle.setText(String.valueOf(s.duracionPlanificadaMinutos()));
        }

        textoPlazoVencido.setVisibility(detalle.noRealizadaFueraDePlazoTardio() ? View.VISIBLE : View.GONE);
        contenedorInicioSesion.setVisibility(detalle.puedeIniciar() ? View.VISIBLE : View.GONE);
        botonIrEnCurso.setVisibility(
                s.codigoEstado() == CodigoEstadoSesion.EN_CURSO ? View.VISIBLE : View.GONE);
        botonRegistrarTardio.setVisibility(detalle.puedeRegistrarTardio() ? View.VISIBLE : View.GONE);
        botonReprogramarSesion.setVisibility(detalle.puedeReprogramar() ? View.VISIBLE : View.GONE);
        botonEliminarSesion.setVisibility(detalle.puedeEliminar() ? View.VISIBLE : View.GONE);

        adaptadorEventos.submitList(detalle.eventos());
        textoSinEventosDetalle.setVisibility(
                detalle.eventos().isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onVerReprogramadaClick(View vista) {
        if (destinoReprogramadaId == null) {
            return;
        }
        Intent intento = new Intent(this, SesionDetalleActivity.class);
        intento.putExtra(EXTRA_SESION_ID, destinoReprogramadaId);
        startActivity(intento);
    }

    private void onIniciarClick(View vista) {
        limpiarErrores();
        TecnicaEstudio tecnica = grupoTecnicaDetalle.getCheckedRadioButtonId() == R.id.radioPomodoroDetalle
                ? TecnicaEstudio.POMODORO
                : TecnicaEstudio.LIBRE;
        int dndId = grupoDndDetalle.getCheckedRadioButtonId();
        PreferenciaNoMolestar pref = PreferenciaNoMolestar.HEREDAR_GLOBAL;
        if (dndId == R.id.radioDndActivar) {
            pref = PreferenciaNoMolestar.FORZAR_ACTIVAR;
        } else if (dndId == R.id.radioDndDesactivar) {
            pref = PreferenciaNoMolestar.FORZAR_DESACTIVAR;
        }
        modelo.iniciar(sesionId, tecnica, pref);
    }

    private void onIrEnCursoClick(View vista) {
        Intent intento = new Intent(this, SesionEnCursoActivity.class);
        intento.putExtra(SesionEnCursoActivity.EXTRA_SESION_ID, sesionId);
        startActivity(intento);
    }

    private void onRegistrarTardioClick(View vista) {
        limpiarErrores();
        modelo.registrarTardio(sesionId, textoDe(campoDuracionRealDetalle));
    }

    private void onReprogramarClick(View vista) {
        Intent intento = new Intent(this, SesionFormularioActivity.class);
        intento.putExtra(SesionFormularioActivity.EXTRA_MODO, SesionFormularioActivity.MODO_REPROGRAMAR);
        intento.putExtra(SesionFormularioActivity.EXTRA_SESION_ID, sesionId);
        startActivity(intento);
    }

    private void onEliminarClick(View vista) {
        modelo.pedirEliminar();
    }

    private void onConfirmandoEliminar(boolean mostrando) {
        if (!mostrando) {
            cerrarDialogoEliminar();
            return;
        }
        if (dialogoEliminar != null && dialogoEliminar.isShowing()) {
            return;
        }
        dialogoEliminar = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sesion_eliminar_titulo)
                .setMessage(R.string.sesion_eliminar_mensaje)
                .setNegativeButton(R.string.cancelar, this::onCancelarEliminarClick)
                .setPositiveButton(R.string.sesion_eliminar_confirmar, this::onConfirmarEliminarClick)
                .setOnCancelListener(this::onDialogoEliminarCancelado)
                .show();
    }

    private void onCancelarEliminarClick(DialogInterface dialogo, int boton) {
        modelo.cancelarEliminar();
    }

    private void onConfirmarEliminarClick(DialogInterface dialogo, int boton) {
        modelo.confirmarEliminar(sesionId);
    }

    private void onDialogoEliminarCancelado(DialogInterface dialogo) {
        modelo.cancelarEliminar();
    }

    private void cerrarDialogoEliminar() {
        if (dialogoEliminar != null && dialogoEliminar.isShowing()) {
            dialogoEliminar.dismiss();
        }
        dialogoEliminar = null;
    }

    private void onEvento(@Nullable EventoSesion evento) {
        if (evento == null) {
            return;
        }
        modelo.consumirEvento();
        if (evento.tipo() == EventoSesion.Tipo.ELIMINADA) {
            cerrarOVolverAInicio();
            return;
        }
        if (evento.tipo() == EventoSesion.Tipo.INICIADA) {
            Intent intento = new Intent(this, SesionEnCursoActivity.class);
            intento.putExtra(SesionEnCursoActivity.EXTRA_SESION_ID, sesionId);
            startActivity(intento);
            return;
        }
        if (evento.tipo() == EventoSesion.Tipo.ERROR && evento.error() != null) {
            if (evento.error() == ErrorValidacion.SESION_YA_EN_CURSO) {
                app().ejecutor().execute(() -> app().evaluarRecuperacionSesion().ejecutar().ifPresent(
                        diag -> runOnUiThread(() -> DialogoRecuperacionSesion.mostrar(
                                this, diag, this::alEstarListo))));
                return;
            }
            String mensaje = MensajesDeError.textoSesion(this, evento.error(), evento.conflicto());
            if (evento.error() == ErrorValidacion.DURACION_REAL_INVALIDA) {
                grupoDuracionRealDetalle.setError(mensaje);
            } else {
                textoErrorDetalle.setText(mensaje);
                textoErrorDetalle.setVisibility(View.VISIBLE);
            }
        }
    }

    private void limpiarErrores() {
        grupoDuracionRealDetalle.setError(null);
        textoErrorDetalle.setVisibility(View.GONE);
    }

    private static String textoDe(TextInputEditText campo) {
        Editable texto = campo.getText();
        return texto == null ? "" : texto.toString().trim();
    }
}
