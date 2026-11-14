package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;

import com.google.android.material.navigation.NavigationBarView;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.EstadoArranque;

/**
 * Base de las Activities que muestran datos del estudiante. Al iniciarse pregunta qué corresponde: sin perfil abre
 * el primer uso, con el bloqueo activo y la sesión bloqueada abre la autenticación, y solo si todo está en orden
 * muestra su contenido. Hasta saberlo mantiene la pantalla oculta, para no mostrar datos sin autenticar.
 *
 * <p>Implementa: HU-04 (autenticar al abrir y al volver a primer plano), HU-01 (el primer uso no autentica) y
 * RNF-12 (la app nunca muestra datos sin autenticar).
 */
public abstract class ActividadProtegida extends ActividadBase {

    private ProteccionViewModel proteccion;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        proteccion = viewModel(ProteccionViewModel.class);
        proteccion.estado().observe(this, this::onEstadoArranque);
    }

    @Override
    protected void onStart() {
        super.onStart();
        findViewById(R.id.main).setVisibility(View.INVISIBLE);
        proteccion.comprobar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        protegerCapturas();
        if (!app().sesionBloqueo().estaBloqueada()) {
            findViewById(R.id.main).setVisibility(View.VISIBLE);
        }
    }

    /**
     * Con el bloqueo activo (o sin haber leído aún los ajustes) la ventana se marca como segura: Android no la muestra
     * en Recientes ni permite capturarla. Ocultar la vista al pausar no basta, porque Android toma la imagen antes.
     */
    protected void protegerCapturas() {
        if (app().sesionBloqueo().bloqueoActivo()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    private void onEstadoArranque(@Nullable EstadoArranque estado) {
        if (estado == null) {
            return;
        }
        switch (estado) {
            case SIN_PERFIL:
                proteccion.consumirEstado();
                Intent primerUso = new Intent(this, PerfilInicialActivity.class);
                primerUso.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(primerUso);
                break;
            case BLOQUEADA:
                proteccion.consumirEstado();
                startActivity(new Intent(this, AutenticacionActivity.class));
                break;
            default:
                proteccion.consumirEstado();
                protegerCapturas();
                if (!app().sesionBloqueo().estaBloqueada()) {
                    findViewById(R.id.main).setVisibility(View.VISIBLE);
                }
                alEstarListo();
        }
    }

    /** Se llama cuando la app está lista (hay perfil y no hay nada que autenticar): aquí se cargan los datos. */
    protected void alEstarListo() {
    }

    /**
     * Prepara la barra de navegación reutilizable (inferior en teléfonos, riel lateral desde 600 dp): marca la pestaña activa y deshabilita las pantallas que
     * aún no existen.
     *
     * @param itemActivo id del elemento del menú de la pantalla actual
     */
    protected void enlazarBarraInferior(@IdRes int itemActivo) {
        NavigationBarView barra = findViewById(R.id.barraInferior);
        barra.getMenu().findItem(R.id.nav_calendario).setEnabled(false);
        barra.getMenu().findItem(R.id.nav_estadisticas).setEnabled(false);
        barra.getMenu().findItem(R.id.nav_materias).setEnabled(false);
        barra.setSelectedItemId(itemActivo);
    }
}
