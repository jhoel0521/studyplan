package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.navigation.NavigationBarView;

import java.util.Optional;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.DiagnosticoRegresoApp;
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
    private DestinoPrincipal destinoActivo;
    private AlertDialog dialogoRecuperacion;

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
                comprobarRecuperacionSesionActiva();
        }
    }

    private void comprobarRecuperacionSesionActiva() {
        if (this instanceof RepartoSesionActivity
                || app().evaluarRecuperacionSesion() == null) {
            return;
        }
        if (app().contadorActividades() != null
                && !app().contadorActividades().consumirEvaluacionRecuperacion()) {
            return;
        }
        app().ejecutor().execute(() -> {
            Optional<DiagnosticoRegresoApp> diagOpt = app().evaluarRecuperacionSesion().ejecutar();
            if (!diagOpt.isPresent()) {
                return;
            }
            DiagnosticoRegresoApp diag = diagOpt.get();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (diag.vencidaPorTresDias()) {
                    Toast.makeText(this, R.string.recuperacion_vencida_aviso, Toast.LENGTH_LONG).show();
                    alEstarListo();
                    return;
                }
                if (dialogoRecuperacion == null || !dialogoRecuperacion.isShowing()) {
                    dialogoRecuperacion = DialogoRecuperacionSesion.mostrar(
                            this, diag, this::alEstarListo);
                }
            });
        });
    }

    /** Indica si ya hay un diálogo modal de recuperación abierto en esta pantalla. */
    protected boolean hayDialogoRecuperacionAbierto() {
        return dialogoRecuperacion != null && dialogoRecuperacion.isShowing();
    }

    /** Se llama cuando la app está lista (hay perfil y no hay nada que autenticar): aquí se cargan los datos. */
    protected void alEstarListo() {
    }

    /**
     * Prepara la barra de navegación reutilizable (inferior en teléfonos, riel lateral desde 600 dp): marca la pestaña
     * activa, habilita solo las pestañas implementadas y navega al elegir otra.
     *
     * <p>Regla de la pila de retroceso: siempre es {@code [Inicio, pestaña]}. Inicio se abre limpiando lo que tenga
     * encima; cualquier otra pestaña se inicia y, si la actual no es Inicio, esta se cierra. Atrás desde una pestaña
     * vuelve a Inicio y desde Inicio sale de la app.
     *
     * @param destinoActivo pestaña de la pantalla actual
     */
    protected void enlazarBarraInferior(DestinoPrincipal destinoActivo) {
        this.destinoActivo = destinoActivo;
        NavigationBarView barra = findViewById(R.id.barraInferior);
        for (DestinoPrincipal destino : DestinoPrincipal.values()) {
            barra.getMenu().findItem(destino.idMenu()).setEnabled(destino.implementado());
        }
        barra.setSelectedItemId(destinoActivo.idMenu());
        barra.setOnItemSelectedListener(this::onDestinoElegido);
    }

    private boolean onDestinoElegido(@NonNull MenuItem opcion) {
        DestinoPrincipal elegido = DestinoPrincipal.deIdMenu(opcion.getItemId());
        if (elegido == null || !elegido.implementado()) {
            return false;
        }
        if (elegido == destinoActivo) {
            return true;
        }
        Intent intento = new Intent(this, elegido.actividad());
        if (elegido == DestinoPrincipal.INICIO) {
            intento.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intento);
            return false;
        }
        startActivity(intento);
        if (destinoActivo != DestinoPrincipal.INICIO) {
            finish();
        }
        // Se devuelve false para que la barra de esta pantalla conserve su pestaña: Atrás vuelve a ella.
        return false;
    }
}
