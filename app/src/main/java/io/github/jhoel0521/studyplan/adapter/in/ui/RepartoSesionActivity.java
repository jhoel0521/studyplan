package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.ResultadoReparto;

/**
 * Pantalla de reparto proporcional de un tramo continuo ({@code W14}) entre la sesión actual y
 * la siguiente sesión invadida (HU-04, HU-44, RN-28, RNF-06, RNF-12).
 */
public class RepartoSesionActivity extends ActividadProtegida {

    public static final String EXTRA_SESION_ACTUAL_ID = "extra_sesion_actual_id";
    public static final String EXTRA_SESION_SIGUIENTE_ID = "extra_sesion_siguiente_id";

    private long sesionActualId;
    private long sesionSiguienteId;
    private RepartoSesionViewModel modelo;

    private View barraPrimera;
    private View barraSegunda;
    private View textoResumen;
    private TextView chipPrimera;
    private TextView chipSegunda;
    private View barraSesionCruzada;
    private TextView nombreSesionCruzada;
    private TextView textoHoraCorte;
    private Slider sliderCorte;
    private TextView textoError;
    private boolean actualizandoSlider;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reparto_sesion);
        aplicarInsets();
        sesionActualId = getIntent().getLongExtra(EXTRA_SESION_ACTUAL_ID, -1L);
        sesionSiguienteId = getIntent().getLongExtra(EXTRA_SESION_SIGUIENTE_ID, -1L);
        enlazarVistas();

        modelo = viewModel(RepartoSesionViewModel.class);
        modelo.estado().observe(this, this::onEstado);
        modelo.error().observe(this, this::onError);
        modelo.guardado().observe(this, this::onGuardado);
    }

    @Override
    protected void alEstarListo() {
        modelo.cargar(sesionActualId, sesionSiguienteId);
    }

    private void enlazarVistas() {
        MaterialToolbar barra = findViewById(R.id.barraSuperior);
        barraPrimera = findViewById(R.id.barraPrimeraReparto);
        barraSegunda = findViewById(R.id.barraSegundaReparto);
        textoResumen = findViewById(R.id.textoResumenReparto);
        chipPrimera = findViewById(R.id.chipPrimeraReparto);
        chipSegunda = findViewById(R.id.chipSegundaReparto);
        barraSesionCruzada = findViewById(R.id.barraSesionCruzadaReparto);
        nombreSesionCruzada = findViewById(R.id.nombreSesionCruzadaReparto);
        textoHoraCorte = findViewById(R.id.textoHoraCorteReparto);
        sliderCorte = findViewById(R.id.sliderCorteReparto);
        textoError = findViewById(R.id.textoErrorReparto);
        MaterialButton botonMenos = findViewById(R.id.botonMenos5Min);
        MaterialButton botonMas = findViewById(R.id.botonMas5Min);
        MaterialButton botonConfirmar = findViewById(R.id.botonConfirmarReparto);

        barra.setNavigationOnClickListener(this::onVolverClick);
        botonMenos.setOnClickListener(v -> modelo.desplazarCorteMinutos(-5));
        botonMas.setOnClickListener(v -> modelo.desplazarCorteMinutos(5));
        botonConfirmar.setOnClickListener(v -> modelo.confirmar());
        sliderCorte.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && !actualizandoSlider) {
                RepartoSesionViewModel.EstadoReparto est = modelo.estado().getValue();
                if (est != null) {
                    long nuevoCorte = est.reparto.inicioTramoMilis()
                            + Math.round(value) * 60_000L;
                    modelo.fijarCorteMilis(nuevoCorte);
                }
            }
        });
    }

    private void onEstado(@Nullable RepartoSesionViewModel.EstadoReparto est) {
        if (est == null) {
            return;
        }
        ResultadoReparto rep = est.reparto;
        // Colores de materia ajustados a la superficie del tema (design.md, decisión 6).
        barraPrimera.setBackgroundColor(AcentoMateria.ajustado(barraPrimera, est.colorPrimera));
        barraSegunda.setBackgroundColor(AcentoMateria.ajustado(barraSegunda, est.colorSegunda));
        AcentoMateria.barra(barraSesionCruzada, est.colorSegunda);
        nombreSesionCruzada.setText(est.nombreSegunda);

        LinearLayout.LayoutParams lp1 = (LinearLayout.LayoutParams) barraPrimera.getLayoutParams();
        lp1.weight = Math.max(1f, rep.minutosPrimeraSesion());
        barraPrimera.setLayoutParams(lp1);

        LinearLayout.LayoutParams lp2 = (LinearLayout.LayoutParams) barraSegunda.getLayoutParams();
        lp2.weight = Math.max(1f, rep.minutosSegundaSesion());
        barraSegunda.setLayoutParams(lp2);

        chipPrimera.setText(getString(
                R.string.reparto_chip_minutos, est.nombrePrimera, rep.minutosPrimeraSesion()));
        chipSegunda.setText(getString(
                R.string.reparto_chip_minutos, est.nombreSegunda, rep.minutosSegundaSesion()));
        AcentoMateria.chip(chipPrimera, est.colorPrimera);
        AcentoMateria.chip(chipSegunda, est.colorSegunda);
        textoResumen.setContentDescription(getString(
                R.string.reparto_resumen,
                est.nombrePrimera,
                rep.minutosPrimeraSesion(),
                est.nombreSegunda,
                rep.minutosSegundaSesion()));

        Hora horaCorte = app().reloj().horaDe(rep.corteMilis());
        textoHoraCorte.setText(getString(R.string.reparto_hora_corte, horaCorte.texto()));

        long totalMin = Math.max(10L, (rep.finTramoMilis() - rep.inicioTramoMilis()) / 60_000L);
        long pasoMax = (totalMin / 5L) * 5L;
        if (pasoMax >= 10L) {
            actualizandoSlider = true;
            sliderCorte.setValueFrom(5f);
            sliderCorte.setValueTo(pasoMax - 5f > 5f ? pasoMax : 10f);
            sliderCorte.setStepSize(5f);
            float actualMin = ((rep.corteMilis() - rep.inicioTramoMilis()) / 60_000L / 5L) * 5f;
            float clamped = Math.max(sliderCorte.getValueFrom(),
                    Math.min(sliderCorte.getValueTo(), actualMin));
            sliderCorte.setValue(clamped);
            actualizandoSlider = false;
        }
    }

    private void onError(@Nullable ErrorValidacion err) {
        if (err == null) {
            textoError.setVisibility(View.GONE);
        } else {
            textoError.setText(R.string.error_corte_reparto);
            textoError.setVisibility(View.VISIBLE);
        }
    }

    private void onGuardado(boolean ok) {
        if (ok) {
            NotificadorSesion.cancelarAvisoRetorno(this);
            finish();
        }
    }

    private void onVolverClick(View v) {
        finish();
    }
}
