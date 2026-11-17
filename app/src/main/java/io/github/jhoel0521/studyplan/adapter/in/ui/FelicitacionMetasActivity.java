package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.MetaAlcanzada;

/**
 * Pantalla única de felicitación cuando al cerrar una sesión se alcanzan una o más metas
 * semanales por primera vez ({@code HU-28}, {@code RN-16}).
 */
public class FelicitacionMetasActivity extends ActividadProtegida {

    public static final String EXTRA_LINEAS_METAS = "lineas_metas";

    public static Intent crearIntent(Context contexto, List<MetaAlcanzada> metas) {
        ArrayList<String> lineas = new ArrayList<>();
        if (metas != null) {
            for (MetaAlcanzada m : metas) {
                String realTxt = FormatoHoras.texto(m.horasRealizadas());
                String objTxt = FormatoHoras.texto(m.horasObjetivo());
                if (m.esMetaTotal()) {
                    lineas.add(contexto.getString(
                            R.string.felicitacion_meta_total_item, realTxt, objTxt));
                } else {
                    lineas.add(contexto.getString(
                            R.string.felicitacion_meta_materia_item,
                            m.nombreMateria(),
                            realTxt,
                            objTxt));
                }
            }
        }
        Intent intento = new Intent(contexto, FelicitacionMetasActivity.class);
        intento.putStringArrayListExtra(EXTRA_LINEAS_METAS, lineas);
        return intento;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_felicitacion_metas);
        aplicarInsets();

        LinearLayout contenedor = findViewById(R.id.contenedorListaMetasAlcanzadas);
        ArrayList<String> lineas = getIntent().getStringArrayListExtra(EXTRA_LINEAS_METAS);
        if (lineas != null) {
            for (String linea : lineas) {
                TextView tv = new TextView(this);
                tv.setText(linea);
                tv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
                tv.setPadding(0, 8, 0, 8);
                contenedor.addView(tv);
            }
        }

        MaterialButton btnVolver = findViewById(R.id.botonVolverInicioFelicitacion);
        btnVolver.setOnClickListener(this::onVolverInicioClick);
    }

    private void onVolverInicioClick(View vista) {
        Intent intento = new Intent(this, MainActivity.class);
        intento.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intento);
        finish();
    }
}
