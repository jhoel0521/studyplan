package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.widget.ImageViewCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Locale;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.application.port.in.DecidirFinPlanificadoOInvasion;
import io.github.jhoel0521.studyplan.domain.CalculadorPomodoro;
import io.github.jhoel0521.studyplan.domain.CandidataInvasion;
import io.github.jhoel0521.studyplan.domain.EstadoPomodoroCalculado;
import io.github.jhoel0521.studyplan.domain.FasePomodoro;
import io.github.jhoel0521.studyplan.domain.Materia;
import io.github.jhoel0521.studyplan.domain.MetaAlcanzada;
import io.github.jhoel0521.studyplan.domain.ResumenEjecucionSesion;
import io.github.jhoel0521.studyplan.domain.TecnicaEstudio;

/**
 * Pantalla protegida de ejecución en vivo ({@code W7}): cronómetro Libre o Pomodoro con
 * {@link Handler} visual únicamente entre {@code onStart} y {@code onStop} (sin
 * {@code ForegroundService}), pausar/reanudar, detener y tarjetas contextuales
 * (HU-04, HU-22 a HU-24, HU-42, HU-43, RNF-03, RNF-12).
 */
public class SesionEnCursoActivity extends ActividadProtegida {

    public static final String EXTRA_SESION_ID = "extra_sesion_id";
    private static final long TICK_MS = 1000L;

    private long sesionId;
    private SesionEnCursoViewModel modelo;

    private ImageView colorMateria;
    private TextView nombreMateria;
    private TextView textoFasePomodoro;
    private TextView textoCronometro;
    private TextView textoEstadoEnCurso;
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
        sesionId = getIntent().getLongExtra(EXTRA_SESION_ID, -1L);
        enlazarVistas();

        modelo = viewModel(SesionEnCursoViewModel.class);
        modelo.resumen().observe(this, this::onResumen);
        modelo.finalizada().observe(this, this::onFinalizada);
        modelo.nuevaSesionActiva().observe(this, this::onCambioASiguiente);
        modelo.metasAlcanzadas().observe(this, this::onMetasAlcanzadas);
        modelo.materiasSelector().observe(this, this::onMateriasParaCambio);
    }

    @Override
    protected void alEstarListo() {
        modelo.cargar(sesionId);
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

        ImageViewCompat.setImageTintList(colorMateria, ColorStateList.valueOf(r.colorMateria()));
        nombreMateria.setText(r.nombreMateria());
        textoEstadoEnCurso.setText(r.pausada()
                ? R.string.sesion_en_curso_pausada
                : R.string.sesion_en_curso_activa);
        botonPausarReanudar.setText(r.pausada()
                ? R.string.sesion_en_curso_reanudar
                : R.string.sesion_en_curso_pausar);
        textoEstadoNoMolestar.setVisibility(r.noMolestarActivo() ? View.VISIBLE : View.GONE);

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
        } else {
            textoCronometro.setText(formatearHms(milis));
        }
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
            NotificadorSesion.cancelarAvisoRetorno(this);
            finish();
        }
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
        finish();
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
