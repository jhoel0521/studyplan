package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Collections;
import java.util.List;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.readmodel.DetalleSesionInicio;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.readmodel.ResumenInicio;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;

/**
 * Pantalla de Inicio y punto de entrada de la app: saludo, progreso real de la semana, acción
 * rápida para nueva sesión, tarjeta de próxima sesión pendiente y lista de sesiones del día actual.
 * La protección (primer uso y bloqueo) la resuelve {@link ActividadProtegida}.
 *
 * <p>Implementa: HU-02, HU-01, HU-04 y RNF-06.
 */
public class MainActivity extends ActividadProtegida {

    private InicioViewModel modelo;
    private TextView saludo;
    private TextView textoProgreso;
    private LinearProgressIndicator barraProgreso;
    private MaterialButton botonNuevaSesion;
    private MaterialCardView tarjetaProximaSesion;
    private View colorMateriaProxima;
    private TextView nombreMateriaProxima;
    private TextView horarioProxima;
    private ImageView iconoEstadoProxima;
    private TextView etiquetaEstadoProxima;
    private TextView estadoVacio;
    private RecyclerView listaSesionesHoy;
    private SesionInicioAdapter adaptadorSesionesHoy;

    @Nullable
    private ResumenInicio ultimoResumen;
    private MaterialCardView tarjetaSesionEnCurso;
    private TextView nombreMateriaEnCursoInicio;
    private ExtendedFloatingActionButton botonEstudiarAhora;
    private boolean hayMateriasActivas;
    private List<Materia> materiasSelector = Collections.emptyList();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        aplicarInsets();
        enlazarVistas();
        enlazarBarraInferior(DestinoPrincipal.INICIO);
        modelo = viewModel(InicioViewModel.class);
        modelo.resumen().observe(this, this::onResumen);
        modelo.hayMateriasActivas().observe(this, this::onHayMateriasActivas);
        modelo.materiasSelector().observe(this, this::onMateriasSelector);
        modelo.metaActualizada().observe(this, this::onMetaActualizada);
        modelo.errorAccion().observe(this, this::onErrorAccion);
        modelo.sesionIniciadaId().observe(this, this::onSesionIniciada);
    }

    private void enlazarVistas() {
        MaterialToolbar barraSuperior = findViewById(R.id.barraSuperior);
        saludo = findViewById(R.id.saludo);
        textoProgreso = findViewById(R.id.textoProgreso);
        barraProgreso = findViewById(R.id.barraProgreso);
        botonNuevaSesion = findViewById(R.id.botonNuevaSesion);
        MaterialButton botonEditarMeta = findViewById(R.id.botonEditarMetaSemana);
        MaterialButton botonHistorial = findViewById(R.id.botonHistorialMetas);
        botonEstudiarAhora = findViewById(R.id.botonEstudiarAhora);
        tarjetaSesionEnCurso = findViewById(R.id.tarjetaSesionEnCurso);
        nombreMateriaEnCursoInicio = findViewById(R.id.nombreMateriaEnCursoInicio);
        MaterialButton botonVolverSesion = findViewById(R.id.botonVolverSesionInicio);
        botonVolverSesion.setOnClickListener(this::onVolverSesionClick);
        tarjetaProximaSesion = findViewById(R.id.tarjetaProximaSesion);
        colorMateriaProxima = findViewById(R.id.colorMateriaProxima);
        nombreMateriaProxima = findViewById(R.id.nombreMateriaProxima);
        horarioProxima = findViewById(R.id.horarioProxima);
        iconoEstadoProxima = findViewById(R.id.iconoEstadoProxima);
        etiquetaEstadoProxima = findViewById(R.id.etiquetaEstadoProxima);
        estadoVacio = findViewById(R.id.estadoVacio);
        listaSesionesHoy = findViewById(R.id.listaSesionesHoy);

        adaptadorSesionesHoy = new SesionInicioAdapter(this::onSesionHoyClick);
        listaSesionesHoy.setLayoutManager(new LinearLayoutManager(this));
        listaSesionesHoy.setAdapter(adaptadorSesionesHoy);

        barraSuperior.setOnMenuItemClickListener(this::onMenuBarraSuperiorClick);
        botonNuevaSesion.setOnClickListener(this::onNuevaSesionClick);
        botonEditarMeta.setOnClickListener(this::onEditarMetaClick);
        botonHistorial.setOnClickListener(this::onHistorialMetasClick);
        botonEstudiarAhora.setOnClickListener(this::onEstudiarAhoraClick);
        tarjetaProximaSesion.setOnClickListener(this::onProximaSesionClick);
        AcentoMateria.comoBoton(tarjetaProximaSesion);
    }

    @Override
    protected void alEstarListo() {
        modelo.cargar();
    }

    private void onHayMateriasActivas(@Nullable Boolean activas) {
        hayMateriasActivas = Boolean.TRUE.equals(activas);
    }

    private void onMateriasSelector(@Nullable List<Materia> lista) {
        materiasSelector = lista == null ? Collections.emptyList() : lista;
    }

    private void onMetaActualizada(@Nullable Boolean actualizada) {
        if (Boolean.TRUE.equals(actualizada)) {
            modelo.consumirEventos();
            Snackbar.make(findViewById(R.id.main), R.string.meta_semanal_guardada, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void onErrorAccion(@Nullable ErrorValidacion error) {
        if (error == null) {
            return;
        }
        modelo.consumirEventos();
        int msg = error == ErrorValidacion.META_FUERA_DE_RANGO
                ? R.string.error_meta_fuera_de_rango
                : MensajesDeError.paraMateria(error);
        if (error == ErrorValidacion.SESION_YA_EN_CURSO) {
            msg = R.string.error_sesion_ya_en_curso;
        }
        Snackbar.make(findViewById(R.id.main), msg, Snackbar.LENGTH_LONG).show();
    }

    private void onSesionIniciada(@Nullable Long sesionId) {
        if (sesionId == null) {
            return;
        }
        modelo.consumirEventos();
        abrirSesionEnCurso(sesionId);
    }

    private void abrirSesionEnCurso(long sesionId) {
        Intent intento = new Intent(this, SesionEnCursoActivity.class);
        intento.putExtra(SesionEnCursoActivity.EXTRA_SESION_ID, sesionId);
        startActivity(intento);
    }

    /** «Volver a la sesión» de la tarjeta de Inicio (HU-02 «Sesión en curso visible»). */
    private void onVolverSesionClick(View vista) {
        if (ultimoResumen != null && ultimoResumen.sesionEnCurso() != null) {
            abrirSesionEnCurso(ultimoResumen.sesionEnCurso().sesionId());
        }
    }

    private void onEditarMetaClick(View vista) {
        TextInputLayout contenedor = new TextInputLayout(this);
        contenedor.setHint(getString(R.string.dialogo_meta_semanal_hint));
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        contenedor.setPadding(pad, pad / 2, pad, 0);
        TextInputEditText campo = new TextInputEditText(this);
        campo.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        campo.setMinHeight(Math.round(48 * getResources().getDisplayMetrics().density));
        if (ultimoResumen != null) {
            campo.setText(FormatoHoras.texto(ultimoResumen.metaSemanalHoras()));
        }
        contenedor.addView(campo);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialogo_meta_semanal_titulo)
                .setView(contenedor)
                .setPositiveButton(R.string.dialogo_meta_semanal_guardar, (d, w) -> {
                    String txt = campo.getText() == null ? "" : campo.getText().toString();
                    modelo.definirMetaSemana(txt);
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void onHistorialMetasClick(View vista) {
        startActivity(new Intent(this, HistorialMetasActivity.class));
    }

    /** Aviso de sesión En curso y texto del botón flotante (HU-02, HU-47; design.md, decisión 21). */
    private void mostrarSesionEnCurso(@Nullable DetalleSesionInicio enCurso) {
        if (enCurso == null) {
            tarjetaSesionEnCurso.setVisibility(View.GONE);
            botonEstudiarAhora.setText(R.string.accion_estudiar_ahora);
            botonEstudiarAhora.setContentDescription(getString(R.string.accion_estudiar_ahora));
            return;
        }
        nombreMateriaEnCursoInicio.setText(enCurso.nombreMateria());
        tarjetaSesionEnCurso.setVisibility(View.VISIBLE);
        botonEstudiarAhora.setText(R.string.inicio_volver_sesion);
        botonEstudiarAhora.setContentDescription(getString(R.string.inicio_volver_sesion));
    }

    private void onEstudiarAhoraClick(View vista) {
        // Con una sesión En curso no se pide materia ni se crea otra: se vuelve a ella (HU-47).
        if (ultimoResumen != null && ultimoResumen.sesionEnCurso() != null) {
            abrirSesionEnCurso(ultimoResumen.sesionEnCurso().sesionId());
            return;
        }
        SelectorEstudioBottomSheet.mostrar(
                this,
                R.string.selector_estudio_titulo,
                materiasSelector,
                TecnicaEstudio.LIBRE,
                new SelectorEstudioBottomSheet.Listener() {
                    @Override
                    public void alElegirMateria(
                            @Nullable Long materiaId, @Nullable Materia materia, TecnicaEstudio tecnica) {
                        DetalleSesionInicio pendiente = buscarPendienteHoy(materiaId, materia);
                        if (pendiente != null) {
                            mostrarDialogoSesionPendiente(pendiente, materiaId, tecnica);
                        } else {
                            modelo.iniciarEstudiarAhora(materiaId, tecnica);
                        }
                    }

                    @Override
                    public void alCrearNuevaMateria(String nombre, int color, TecnicaEstudio tecnica) {
                        modelo.iniciarEstudiarAhoraConNuevaMateria(nombre, color, tecnica);
                    }
                });
    }

    @Nullable
    private DetalleSesionInicio buscarPendienteHoy(
            @Nullable Long materiaId, @Nullable Materia materia) {
        if (ultimoResumen == null || materiaId == null || materia == null) {
            return null;
        }
        for (DetalleSesionInicio s : ultimoResumen.sesionesDeHoy()) {
            if (s.estado() == CodigoEstadoSesion.PENDIENTE
                    && s.nombreMateria().equals(materia.nombre())) {
                return s;
            }
        }
        return null;
    }

    private void mostrarDialogoSesionPendiente(
            DetalleSesionInicio pendiente, @Nullable Long materiaId, TecnicaEstudio tecnica) {
        // «Iniciar la planificada» en relleno y «Empezar sesión nueva» en contorno (design.md, decisión 7).
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_StudyPlan_MaterialAlertDialog_Destacado)
                .setTitle(getString(R.string.dialogo_sesion_pendiente_titulo, pendiente.nombreMateria()))
                .setMessage(getString(
                        R.string.dialogo_sesion_pendiente_msg,
                        pendiente.horaInicio().texto(),
                        pendiente.horaFin().texto()))
                .setPositiveButton(R.string.dialogo_sesion_pendiente_usar, (d, w) ->
                        modelo.iniciarSesionPlanificada(pendiente.sesionId(), tecnica))
                .setNegativeButton(R.string.dialogo_sesion_pendiente_nueva, (d, w) ->
                        modelo.iniciarEstudiarAhora(materiaId, tecnica))
                .show();
    }

    private void onResumen(ResumenInicio resumen) {
        ultimoResumen = resumen;
        mostrarSesionEnCurso(resumen.sesionEnCurso());
        saludo.setText(getString(R.string.inicio_saludo, resumen.nombre()));
        textoProgreso.setText(getString(R.string.inicio_progreso,
                FormatoHoras.texto(resumen.horasRealizadas()), FormatoHoras.texto(resumen.metaSemanalHoras())));
        barraProgreso.setProgress(resumen.porcentajeProgreso());

        DetalleSesionInicio proxima = resumen.proximaSesion();
        if (proxima != null) {
            AcentoMateria.barra(colorMateriaProxima, proxima.colorMateria());
            nombreMateriaProxima.setText(proxima.nombreMateria());
            horarioProxima.setText(FormatoSesion.rangoYDuracion(this, proxima, true));
            EstadoSesionVisual.aplicar(proxima.estado(), iconoEstadoProxima, etiquetaEstadoProxima);
            tarjetaProximaSesion.setVisibility(View.VISIBLE);
        } else {
            tarjetaProximaSesion.setVisibility(View.GONE);
        }

        boolean sinSesionesHoy = resumen.sesionesDeHoy().isEmpty();
        estadoVacio.setVisibility(sinSesionesHoy ? View.VISIBLE : View.GONE);
        listaSesionesHoy.setVisibility(sinSesionesHoy ? View.GONE : View.VISIBLE);
        adaptadorSesionesHoy.submitList(resumen.sesionesDeHoy());
    }

    private void onNuevaSesionClick(View vista) {
        if (!hayMateriasActivas) {
            Snackbar.make(
                            findViewById(R.id.main),
                            R.string.inicio_sin_materias_aviso,
                            Snackbar.LENGTH_LONG)
                    .setAction(R.string.inicio_ir_materias, this::onIrAMateriasClick)
                    .show();
            return;
        }
        Intent intento = new Intent(this, SesionFormularioActivity.class);
        intento.putExtra(SesionFormularioActivity.EXTRA_MODO, SesionFormularioActivity.MODO_PLANIFICAR);
        if (ultimoResumen != null && ultimoResumen.hoy() != null) {
            intento.putExtra(SesionFormularioActivity.EXTRA_FECHA, ultimoResumen.hoy().iso());
        }
        startActivity(intento);
    }

    private void onIrAMateriasClick(View vista) {
        startActivity(new Intent(this, MateriasActivity.class));
    }

    private void onProximaSesionClick(View vista) {
        if (ultimoResumen != null && ultimoResumen.proximaSesion() != null) {
            abrirDetalleSesion(ultimoResumen.proximaSesion().sesionId());
        }
    }

    private void onSesionHoyClick(DetalleSesionInicio sesion) {
        abrirDetalleSesion(sesion.sesionId());
    }

    private void abrirDetalleSesion(long sesionId) {
        Intent intento = new Intent(this, SesionDetalleActivity.class);
        intento.putExtra(SesionDetalleActivity.EXTRA_SESION_ID, sesionId);
        startActivity(intento);
    }

    private boolean onMenuBarraSuperiorClick(MenuItem opcion) {
        int id = opcion.getItemId();
        if (id == R.id.accion_reportes) {
            startActivity(new Intent(this, ReportesActivity.class));
            return true;
        }
        if (id == R.id.accion_ajustes) {
            startActivity(new Intent(this, AjustesActivity.class));
            return true;
        }
        return false;
    }
}
