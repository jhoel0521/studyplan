package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.List;
import java.util.Locale;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.application.port.in.DecidirFinPlanificadoOInvasion;
import io.github.jhoel0521.studyplan.domain.CalculadorPomodoro;
import io.github.jhoel0521.studyplan.domain.CandidataInvasion;
import io.github.jhoel0521.studyplan.domain.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.EstadoPomodoroCalculado;
import io.github.jhoel0521.studyplan.domain.FasePomodoro;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.MetaAlcanzada;
import io.github.jhoel0521.studyplan.domain.ResumenEjecucionSesion;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.TecnicaEstudio;

/**
 * Pantalla protegida de ejecución en vivo ({@code W7}): cronómetro Libre o Pomodoro con
 * {@link Handler} visual únicamente entre {@code onStart} y {@code onStop} (sin
 * {@code ForegroundService}), pausar/reanudar, detener, tarjetas contextuales y arranque directo
 * desde recordatorios ({@code HU-04}, {@code HU-22} a {@code HU-24}, {@code HU-34}, {@code HU-42},
 * {@code HU-43}, {@code RNF-03}, {@code RNF-12}).
 */
public class SesionEnCursoActivity extends ActividadProtegida {

    public static final String EXTRA_SESION_ID = "extra_sesion_id";
    public static final String EXTRA_INICIAR_DESDE_RECORDATORIO =
            "extra_iniciar_desde_recordatorio";
    private static final String ESTADO_SESION_ID = "estado_sesion_id";
    private static final String ESTADO_INICIAR_RECORDATORIO = "estado_iniciar_recordatorio";
    private static final long TICK_MS = 1000L;

    private long sesionId;
    private boolean iniciarDesdeRecordatorio;
    @Nullable
    private AlertDialog dialogoErrorInicio;
    private SesionEnCursoViewModel modelo;

    private ImageView colorMateria;
    private TextView nombreMateria;
    private TextView textoFasePomodoro;
    private TextView textoCronometro;
    private TextView textoEstadoEnCurso;
    private CircularProgressIndicator anilloProgreso;
    private TextView textoEstadoNoMolestar;
    private MaterialButton botonPausarReanudar;
    private MaterialButton botonDetener;
    private MaterialCardView tarjetaExtensionPomodoro;
    private MaterialCardView tarjetaFinPlanificado;
    private MaterialCardView tarjetaInvasionSesion;
    private TextView textoInvasionTitulo;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ResumenEjecucionSesion ultimoResumen;
    private long instanteCargaMs;
    private int ticksDesdeRecarga;

    private final Runnable tareaTick = new Runnable() {
        @Override
        public void run() {
            actualizarCronometroVisual();
            ticksDesdeRecarga++;
            if (ticksDesdeRecarga >= 15) {
                ticksDesdeRecarga = 0;
                modelo.cargar(sesionId);
            }
            handler.postDelayed(this, TICK_MS);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sesion_en_curso);
        aplicarInsets();
        if (savedInstanceState != null) {
            sesionId = savedInstanceState.getLong(ESTADO_SESION_ID, -1L);
            iniciarDesdeRecordatorio = savedInstanceState.getBoolean(ESTADO_INICIAR_RECORDATORIO, false);
        } else {
            leerIntent(getIntent());
        }
        enlazarVistas();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                cerrarOVolverAInicio();
            }
        });

        modelo = viewModel(SesionEnCursoViewModel.class);
        modelo.resumen().observe(this, this::onResumen);
        modelo.finalizada().observe(this, this::onFinalizada);
        modelo.nuevaSesionActiva().observe(this, this::onCambioASiguiente);
        modelo.metasAlcanzadas().observe(this, this::onMetasAlcanzadas);
        modelo.materiasSelector().observe(this, this::onMateriasParaCambio);
        modelo.errorInicioDirecto().observe(this, this::onErrorInicioDirecto);
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        leerIntent(intent);
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)
                && !app().sesionBloqueo().estaBloqueada()) {
            procesarEntrada();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ESTADO_SESION_ID, sesionId);
        outState.putBoolean(ESTADO_INICIAR_RECORDATORIO, iniciarDesdeRecordatorio);
    }

    @Override
    protected void onDestroy() {
        cerrarDialogoErrorInicio();
        super.onDestroy();
    }

    @Override
    protected void alEstarListo() {
        procesarEntrada();
    }

    private void leerIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        sesionId = intent.getLongExtra(EXTRA_SESION_ID, -1L);
        iniciarDesdeRecordatorio = intent.getBooleanExtra(EXTRA_INICIAR_DESDE_RECORDATORIO, false);
    }

    private void procesarEntrada() {
        if (sesionId > 0) {
            NotificadorSesion.cancelarRecordatorio(this, sesionId);
        }
        if (iniciarDesdeRecordatorio) {
            iniciarDesdeRecordatorio = false;
            getIntent().removeExtra(EXTRA_INICIAR_DESDE_RECORDATORIO);
            modelo.cargarOIniciarDesdeRecordatorio(sesionId);
        } else {
            modelo.cargar(sesionId);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        handler.removeCallbacks(tareaTick);
        handler.postDelayed(tareaTick, TICK_MS);
    }

    @Override
    protected void onStop() {
        handler.removeCallbacks(tareaTick);
        super.onStop();
    }

    private void enlazarVistas() {
        MaterialToolbar barraSuperior = findViewById(R.id.barraSuperior);
        colorMateria = findViewById(R.id.colorMateriaEnCurso);
        anilloProgreso = findViewById(R.id.anilloProgresoEnCurso);
        nombreMateria = findViewById(R.id.nombreMateriaEnCurso);
        textoFasePomodoro = findViewById(R.id.textoFasePomodoro);
        textoCronometro = findViewById(R.id.textoCronometro);
        textoEstadoEnCurso = findViewById(R.id.textoEstadoEnCurso);
        textoEstadoNoMolestar = findViewById(R.id.textoEstadoNoMolestar);
        botonPausarReanudar = findViewById(R.id.botonPausarReanudar);
        botonDetener = findViewById(R.id.botonDetenerEnCurso);
        tarjetaExtensionPomodoro = findViewById(R.id.tarjetaExtensionPomodoro);
        tarjetaFinPlanificado = findViewById(R.id.tarjetaFinPlanificado);
        tarjetaInvasionSesion = findViewById(R.id.tarjetaInvasionSesion);
        textoInvasionTitulo = findViewById(R.id.textoInvasionTitulo);

        MaterialButton botonMasPomodoro = findViewById(R.id.botonMasUnPomodoro);
        MaterialButton botonExtraLibre = findViewById(R.id.botonTiempoExtraLibre);
        MaterialButton botonSigo = findViewById(R.id.botonSigoEstudiando);
        MaterialButton botonTermineFin = findViewById(R.id.botonTermineFinPlan);
        MaterialButton botonCambiar = findViewById(R.id.botonCambiarSiguiente);
        MaterialButton botonCambiarMateria = findViewById(R.id.botonCambiarMateria);
        MaterialButton botonRepartir = findViewById(R.id.botonRepartirTramo);

        barraSuperior.setNavigationOnClickListener(this::onVolverClick);
        botonPausarReanudar.setOnClickListener(this::onPausarReanudarClick);
        botonDetener.setOnClickListener(this::onDetenerClick);
        botonCambiarMateria.setOnClickListener(this::onCambiarMateriaClick);
        botonMasPomodoro.setOnClickListener(v -> modelo.decidir(
                sesionId, DecidirFinPlanificadoOInvasion.DecisionFinOInvasion.MAS_UN_POMODORO, null));
        botonExtraLibre.setOnClickListener(v -> modelo.decidir(
                sesionId, DecidirFinPlanificadoOInvasion.DecisionFinOInvasion.TIEMPO_EXTRA_LIBRE, null));
        botonSigo.setOnClickListener(v -> modelo.decidir(
                sesionId, DecidirFinPlanificadoOInvasion.DecisionFinOInvasion.SIGO_ESTUDIANDO, null));
        botonTermineFin.setOnClickListener(v -> modelo.decidir(
                sesionId, DecidirFinPlanificadoOInvasion.DecisionFinOInvasion.TERMINE, null));
        botonCambiar.setOnClickListener(this::onCambiarSiguienteClick);
        botonRepartir.setOnClickListener(this::onRepartirClick);
    }

    private void onResumen(@Nullable ResumenEjecucionSesion r) {
        if (r == null) {
            return;
        }
        ultimoResumen = r;
        instanteCargaMs = System.currentTimeMillis();
        ticksDesdeRecarga = 0;

        AcentoMateria.punto(colorMateria, r.colorMateria());
        nombreMateria.setText(r.nombreMateria());
        textoEstadoEnCurso.setText(r.pausada()
                ? R.string.sesion_en_curso_pausada
                : R.string.sesion_en_curso_activa);
        botonPausarReanudar.setText(r.pausada()
                ? R.string.sesion_en_curso_reanudar
                : R.string.sesion_en_curso_pausar);
        textoEstadoNoMolestar.setVisibility(r.noMolestarActivo() ? View.VISIBLE : View.GONE);
        aplicarColorDeFase(r);

        if (r.sesion().tecnica() == TecnicaEstudio.POMODORO && r.estadoPomodoro() != null) {
            EstadoPomodoroCalculado pom = r.estadoPomodoro();
            int objetivo = CalculadorPomodoro.pomodorosQueCabenEnPlan(
                    r.sesion().duracionPlanificadaMinutos(),
                    r.sesion().configuracionPomodoroSnapshot());
            textoFasePomodoro.setText(textoDeFase(pom.faseActual(), pom.cicloActual(), objetivo));
            textoFasePomodoro.setVisibility(View.VISIBLE);
        } else {
            textoFasePomodoro.setVisibility(View.GONE);
        }

        tarjetaExtensionPomodoro.setVisibility(
                r.mostrarExtensionPomodoro() ? View.VISIBLE : View.GONE);
        tarjetaFinPlanificado.setVisibility(
                r.mostrarFinPlanificado() && !r.mostrarExtensionPomodoro() ? View.VISIBLE : View.GONE);

        CandidataInvasion inv = r.candidataInvasion();
        if (inv != null) {
            textoInvasionTitulo.setText(getString(
                    R.string.sesion_en_curso_invasion_titulo,
                    inv.nombreMateria(),
                    inv.horaInicio().texto(),
                    inv.horaFin().texto()));
            tarjetaInvasionSesion.setVisibility(View.VISIBLE);
        } else {
            tarjetaInvasionSesion.setVisibility(View.GONE);
        }

        actualizarCronometroVisual();
    }

    private void actualizarCronometroVisual() {
        if (ultimoResumen == null) {
            return;
        }
        long delta = (ultimoResumen.pausada() || ultimoResumen.salidaPendienteResolver())
                ? 0L
                : Math.max(0L, System.currentTimeMillis() - instanteCargaMs);
        long milis = ultimoResumen.milisEfectivos() + delta;
        if (ultimoResumen.sesion().tecnica() == TecnicaEstudio.POMODORO
                && ultimoResumen.estadoPomodoro() != null
                && ultimoResumen.estadoPomodoro().faseActual() != FasePomodoro.TIEMPO_EXTRA_LIBRE) {
            long restante = Math.max(
                    0L, ultimoResumen.estadoPomodoro().milisRestantesEnFase() - delta);
            textoCronometro.setText(formatearHms(restante));
            actualizarAnillo(restante, duracionFaseMs(ultimoResumen));
        } else {
            textoCronometro.setText(formatearHms(milis));
            actualizarAnillo(0L, 0L);
        }
    }

    /**
     * Color del anillo y de la etiqueta de estado según la fase (design.md, decisión 5b): en pausa
     * {@code colorWarning}, en descanso {@code colorSecondary} y estudiando {@code colorPrimary}. La etiqueta de
     * texto acompaña siempre al color (RNF-06).
     */
    private void aplicarColorDeFase(ResumenEjecucionSesion r) {
        int atributo;
        if (r.pausada()) {
            atributo = R.attr.colorWarning;
        } else if (esDescanso(r)) {
            atributo = com.google.android.material.R.attr.colorSecondary;
        } else {
            atributo = com.google.android.material.R.attr.colorPrimary;
        }
        int color = MaterialColors.getColor(anilloProgreso, atributo);
        anilloProgreso.setIndicatorColor(color);
        textoEstadoEnCurso.setTextColor(color);
    }

    private static boolean esDescanso(ResumenEjecucionSesion r) {
        if (r.sesion().tecnica() != TecnicaEstudio.POMODORO || r.estadoPomodoro() == null) {
            return false;
        }
        FasePomodoro fase = r.estadoPomodoro().faseActual();
        return fase == FasePomodoro.DESCANSO_CORTO || fase == FasePomodoro.DESCANSO_LARGO;
    }

    /** Duración configurada de la fase Pomodoro actual (RN-17), o 0 si no aplica (Libre o tiempo extra). */
    private static long duracionFaseMs(ResumenEjecucionSesion r) {
        if (r.sesion().tecnica() != TecnicaEstudio.POMODORO || r.estadoPomodoro() == null
                || r.sesion().configuracionPomodoroSnapshot() == null) {
            return 0L;
        }
        int minutos;
        switch (r.estadoPomodoro().faseActual()) {
            case TRABAJO:
                minutos = r.sesion().configuracionPomodoroSnapshot().trabajoMinutos();
                break;
            case DESCANSO_CORTO:
                minutos = r.sesion().configuracionPomodoroSnapshot().descansoCortoMinutos();
                break;
            case DESCANSO_LARGO:
                minutos = r.sesion().configuracionPomodoroSnapshot().descansoLargoMinutos();
                break;
            default:
                minutos = 0;
        }
        return minutos * 60_000L;
    }

    /** Progreso de la fase (0 a 1000); sin duración conocida (Libre o tiempo extra) el anillo queda completo. */
    private void actualizarAnillo(long restanteMs, long duracionMs) {
        int progreso = duracionMs <= 0L
                ? anilloProgreso.getMax()
                : (int) Math.round(anilloProgreso.getMax() * (1.0 - Math.min(1.0, (double) restanteMs / duracionMs)));
        anilloProgreso.setProgressCompat(progreso, false);
    }

    private String textoDeFase(FasePomodoro fase, int ciclo, int objetivo) {
        switch (fase) {
            case DESCANSO_CORTO:
                return getString(R.string.sesion_en_curso_fase_descanso_corto, ciclo, objetivo);
            case DESCANSO_LARGO:
                return getString(R.string.sesion_en_curso_fase_descanso_largo, ciclo, objetivo);
            case TIEMPO_EXTRA_LIBRE:
                return getString(R.string.sesion_en_curso_fase_extra);
            case TRABAJO:
            default:
                return getString(R.string.sesion_en_curso_fase_trabajo, ciclo, objetivo);
        }
    }

    private static String formatearHms(long milis) {
        long segTotal = Math.max(0L, milis / 1000L);
        long h = segTotal / 3600L;
        long m = (segTotal % 3600L) / 60L;
        long s = segTotal % 60L;
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s);
    }

    private void onFinalizada(boolean termino) {
        if (termino) {
            cerrarOVolverAInicio();
        }
    }

    private void onErrorInicioDirecto(@Nullable ErrorValidacion error) {
        if (error == null) {
            return;
        }
        modelo.consumirErrorInicioDirecto();
        if (error == ErrorValidacion.SESION_YA_EN_CURSO) {
            resolverOtraSesionEnCurso();
            return;
        }
        android.widget.Toast.makeText(
                        this,
                        MensajesDeError.textoSesion(this, error, null),
                        android.widget.Toast.LENGTH_LONG)
                .show();
        cerrarOVolverAInicio();
    }

    private void resolverOtraSesionEnCurso() {
        if (hayDialogoRecuperacionAbierto()
                || (dialogoErrorInicio != null && dialogoErrorInicio.isShowing())) {
            return;
        }
        app().ejecutor().execute(() -> {
            java.util.Optional<io.github.jhoel0521.studyplan.domain.DiagnosticoRegresoApp> diagOpt =
                    app().evaluarRecuperacionSesion().ejecutar();
            if (diagOpt.isPresent()) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed() && !hayDialogoRecuperacionAbierto()) {
                        DialogoRecuperacionSesion.mostrar(this, diagOpt.get(), this::alEstarListo);
                    }
                });
                return;
            }
            java.util.Optional<SesionEstudio> activaOpt = app().sesionRepository().buscarEnCurso();
            if (!activaOpt.isPresent()) {
                runOnUiThread(this::cerrarOVolverAInicio);
                return;
            }
            SesionEstudio activa = activaOpt.get();
            String materiaActiva = app().materiaRepository()
                    .obtener(activa.materiaId())
                    .map(Materia::nombre)
                    .orElse("");
            runOnUiThread(() -> mostrarDialogoOtraSesionActiva(activa.id(), materiaActiva));
        });
    }

    private void mostrarDialogoOtraSesionActiva(long otraSesionId, String materiaActiva) {
        if (isFinishing() || isDestroyed() || hayDialogoRecuperacionAbierto()
                || (dialogoErrorInicio != null && dialogoErrorInicio.isShowing())) {
            return;
        }
        dialogoErrorInicio = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.error_sesion_ya_en_curso)
                .setMessage(getString(R.string.sesion_en_curso_otra_activa_msg, materiaActiva))
                .setNegativeButton(R.string.cancelar, (d, w) -> cerrarOVolverAInicio())
                .setPositiveButton(R.string.sesion_en_curso_ir_a_activa, (d, w) -> {
                    sesionId = otraSesionId;
                    iniciarDesdeRecordatorio = false;
                    getIntent().putExtra(EXTRA_SESION_ID, otraSesionId);
                    getIntent().removeExtra(EXTRA_INICIAR_DESDE_RECORDATORIO);
                    modelo.cargar(otraSesionId);
                })
                .setOnCancelListener(d -> cerrarOVolverAInicio())
                .show();
    }

    private void cerrarDialogoErrorInicio() {
        if (dialogoErrorInicio != null && dialogoErrorInicio.isShowing()) {
            dialogoErrorInicio.dismiss();
        }
        dialogoErrorInicio = null;
    }

    private void cerrarOVolverAInicio() {
        NotificadorSesion.cancelarAvisoRetorno(this);
        if (isTaskRoot()) {
            Intent inicio = new Intent(this, MainActivity.class);
            inicio.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(inicio);
        }
        finish();
    }

    private void onCambioASiguiente(@Nullable Long nuevoId) {
        if (nuevoId == null || nuevoId <= 0) {
            return;
        }
        sesionId = nuevoId;
        modelo.cargar(sesionId);
    }

    private void onMetasAlcanzadas(@Nullable List<MetaAlcanzada> metas) {
        if (metas == null || metas.isEmpty()) {
            return;
        }
        NotificadorSesion.cancelarAvisoRetorno(this);
        startActivity(FelicitacionMetasActivity.crearIntent(this, metas));
        finish();
    }

    private void onCambiarMateriaClick(View vista) {
        modelo.cargarMateriasParaCambio();
    }

    private void onMateriasParaCambio(@Nullable List<Materia> materias) {
        if (materias == null) {
            return;
        }
        TecnicaEstudio tecnicaActual = ultimoResumen != null
                ? ultimoResumen.sesion().tecnica()
                : TecnicaEstudio.LIBRE;
        SelectorEstudioBottomSheet.mostrar(
                this,
                R.string.selector_estudio_titulo_cambio,
                materias,
                tecnicaActual,
                new SelectorEstudioBottomSheet.Listener() {
                    @Override
                    public void alElegirMateria(
                            @Nullable Long materiaId, @Nullable Materia materia, TecnicaEstudio tecnica) {
                        modelo.cambiarDeMateria(sesionId, materiaId, tecnica);
                    }

                    @Override
                    public void alCrearNuevaMateria(
                            String nombre, int color, TecnicaEstudio tecnica) {
                        modelo.cambiarACreada(sesionId, nombre, color, tecnica);
                    }
                });
    }

    private void onVolverClick(View v) {
        cerrarOVolverAInicio();
    }

    private void onPausarReanudarClick(View v) {
        if (ultimoResumen != null && ultimoResumen.pausada()) {
            mostrarDialogoTopicoReanudar();
        } else {
            modelo.pausarOReanudar(sesionId);
        }
    }

    private void mostrarDialogoTopicoReanudar() {
        android.widget.LinearLayout contenedor = new android.widget.LinearLayout(this);
        contenedor.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        contenedor.setPadding(pad, pad, pad, 0);

        com.google.android.material.chip.ChipGroup grupo =
                new com.google.android.material.chip.ChipGroup(this);
        TopicosUi.poblarChips(
                this,
                grupo,
                TopicosUi.topicosParaInterrupcion(),
                io.github.jhoel0521.studyplan.domain.TopicoEventoEstudio.SIN_CLASIFICAR);
        contenedor.addView(grupo);

        com.google.android.material.textfield.TextInputLayout campoLayout =
                new com.google.android.material.textfield.TextInputLayout(this);
        campoLayout.setHint(getString(R.string.topico_detalle_opcional));
        com.google.android.material.textfield.TextInputEditText campoNota =
                new com.google.android.material.textfield.TextInputEditText(this);
        campoNota.setMinHeight((int) (48 * getResources().getDisplayMetrics().density));
        campoLayout.addView(campoNota);
        contenedor.addView(campoLayout);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.topico_pausa_titulo)
                .setView(contenedor)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.topico_reanudar_confirmar, (d, w) -> {
                    io.github.jhoel0521.studyplan.domain.TopicoEventoEstudio topico =
                            TopicosUi.obtenerSeleccionado(grupo);
                    String nota = campoNota.getText() == null ? "" : campoNota.getText().toString();
                    modelo.reanudarConTopico(sesionId, topico, nota);
                })
                .show();
    }

    private void onDetenerClick(View v) {
        modelo.detener(sesionId);
    }

    private void onCambiarSiguienteClick(View v) {
        if (ultimoResumen != null && ultimoResumen.candidataInvasion() != null) {
            modelo.decidir(
                    sesionId,
                    DecidirFinPlanificadoOInvasion.DecisionFinOInvasion.CAMBIAR_A_SIGUIENTE,
                    ultimoResumen.candidataInvasion().sesionId());
        }
    }

    private void onRepartirClick(View v) {
        if (ultimoResumen != null && ultimoResumen.candidataInvasion() != null) {
            Intent intent = new Intent(this, RepartoSesionActivity.class);
            intent.putExtra(RepartoSesionActivity.EXTRA_SESION_ACTUAL_ID, sesionId);
            intent.putExtra(
                    RepartoSesionActivity.EXTRA_SESION_SIGUIENTE_ID,
                    ultimoResumen.candidataInvasion().sesionId());
            startActivity(intent);
        }
    }
}
