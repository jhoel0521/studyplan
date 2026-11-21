package io.github.jhoel0521.studyplan.adapter.in.ui.materias;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.ActividadProtegida;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.FormatoHoras;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.GrupoBotones;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.MensajesDeError;

/**
 * Formulario de materia: registrar una nueva o editar una existente (nombre, docente opcional y color, que se elige en un
 * modal con selector, campo hexadecimal y colores rápidos) y, al editar, archivarla o desarchivarla y eliminarla con confirmación.
 *
 * <p>Implementa: HU-06 (registrar), HU-07 (editar), HU-08 (archivar y eliminar) y HU-46 (desarchivar).
 */
public class MateriaFormularioActivity extends ActividadProtegida {

    /** Extra con el id de la materia a editar; sin él, el formulario registra una nueva. */
    public static final String EXTRA_ID = "materia_id";

    private static final String ESTADO_COLOR = "color_elegido";
    private static final String ESTADO_CARGADO = "datos_cargados";
    private static final long SIN_ID = -1;

    private MateriaFormularioViewModel modelo;
    private TextInputLayout contenedorNombre;
    private TextInputEditText campoNombre;
    private TextInputEditText campoDocente;
    private TextInputLayout contenedorMeta;
    private TextInputEditText campoMeta;
    private MaterialButton botonArchivar;
    private MaterialButton botonEliminar;
    private ImageView vistaPreviaColor;
    private TextView codigoColor;
    private SelectorColorDialogo selectorColor;
    private int colorElegido;
    private Materia materiaCargada;
    private AlertDialog dialogoAbierto;
    private long idEdicion = SIN_ID;
    private boolean datosCargados;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_materia_formulario);
        aplicarInsets();
        idEdicion = getIntent().getLongExtra(EXTRA_ID, SIN_ID);
        datosCargados = savedInstanceState != null && savedInstanceState.getBoolean(ESTADO_CARGADO, false);
        enlazarVistas();
        colorElegido = savedInstanceState != null
                ? savedInstanceState.getInt(ESTADO_COLOR, colorPorDefecto()) : colorPorDefecto();
        mostrarColor();
        modelo = viewModel(MateriaFormularioViewModel.class);
        modelo.materia().observe(this, this::onMateria);
        modelo.evento().observe(this, this::onEvento);
        modelo.confirmandoEliminar().observe(this, this::onConfirmandoEliminar);
        if (idEdicion != SIN_ID) {
            modelo.cargar(idEdicion);
        }
    }

    private void enlazarVistas() {
        MaterialToolbar barraSuperior = findViewById(R.id.barraSuperior);
        contenedorNombre = findViewById(R.id.contenedorNombreMateria);
        campoNombre = findViewById(R.id.campoNombreMateria);
        campoDocente = findViewById(R.id.campoDocenteMateria);
        contenedorMeta = findViewById(R.id.contenedorMetaMateria);
        campoMeta = findViewById(R.id.campoMetaMateria);
        vistaPreviaColor = findViewById(R.id.vistaPreviaColor);
        codigoColor = findViewById(R.id.codigoColor);
        View filaColor = findViewById(R.id.filaColor);
        filaColor.setOnClickListener(this::onColorClick);
        MaterialButton botonGuardar = findViewById(R.id.botonGuardarMateria);
        botonArchivar = findViewById(R.id.botonArchivarMateria);
        botonEliminar = findViewById(R.id.botonEliminarMateria);
        boolean edicion = idEdicion != SIN_ID;
        barraSuperior.setTitle(edicion ? R.string.materia_editar : R.string.materia_nueva);
        barraSuperior.setNavigationOnClickListener(this::onVolverClick);
        botonGuardar.setOnClickListener(this::onGuardarClick);
        botonArchivar.setOnClickListener(this::onArchivarClick);
        botonEliminar.setOnClickListener(this::onEliminarClick);
        // Acciones solo con icono: el tooltip repite el nombre que lee TalkBack (design.md, decisión 18).
        TooltipCompat.setTooltipText(botonGuardar, botonGuardar.getContentDescription());
        TooltipCompat.setTooltipText(botonArchivar, botonArchivar.getContentDescription());
        TooltipCompat.setTooltipText(botonEliminar, botonEliminar.getContentDescription());
        botonArchivar.setVisibility(edicion ? View.VISIBLE : View.GONE);
        botonEliminar.setVisibility(edicion ? View.VISIBLE : View.GONE);
        if (edicion) {
            GrupoBotones.unir(botonGuardar, botonArchivar, botonEliminar);
        } else {
            GrupoBotones.unir(botonGuardar);
        }
        // Hasta que cargue la materia no se puede archivar ni eliminar: la acción dependería de su estado.
        botonArchivar.setEnabled(false);
        botonEliminar.setEnabled(false);
    }

    private int colorPorDefecto() {
        return ContextCompat.getColor(this, R.color.materia_azul);
    }

    /** Muestra el color elegido en la fila del formulario: círculo, código y descripción para accesibilidad. */
    private void mostrarColor() {
        String codigo = "#" + ColorHex.formatear(colorElegido);
        vistaPreviaColor.setColorFilter(colorElegido);
        codigoColor.setText(codigo);
        findViewById(R.id.filaColor).setContentDescription(getString(R.string.materia_color_descripcion, codigo));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle estado) {
        super.onSaveInstanceState(estado);
        estado.putInt(ESTADO_COLOR, colorElegido);
        estado.putBoolean(ESTADO_CARGADO, datosCargados);
    }

    @Override
    protected void onDestroy() {
        cerrarDialogo();
        if (selectorColor != null) {
            selectorColor.cerrar();
            selectorColor = null;
        }
        super.onDestroy();
    }

    private void onMateria(@Nullable Materia materia) {
        if (materia == null) {
            return;
        }
        materiaCargada = materia;
        if (!datosCargados) {
            campoNombre.setText(materia.nombre());
            campoDocente.setText(materia.docente());
            if (materia.metaSemanalHoras() != null) {
                campoMeta.setText(FormatoHoras.texto(materia.metaSemanalHoras()));
            }
            colorElegido = 0xFF000000 | materia.color();
            mostrarColor();
            datosCargados = true;
        }
        if (materia.esGeneral()) {
            botonArchivar.setVisibility(View.GONE);
            botonEliminar.setVisibility(View.GONE);
        } else {
            // Solo icono (decisión 18): el estado cambia el icono, el nombre que lee TalkBack y el tooltip.
            boolean archivada = materia.archivada();
            botonArchivar.setIconResource(archivada ? R.drawable.ic_desarchivar : R.drawable.ic_archivar);
            String accion = getString(archivada ? R.string.materia_desarchivar : R.string.materia_archivar);
            botonArchivar.setContentDescription(accion);
            TooltipCompat.setTooltipText(botonArchivar, accion);
            botonArchivar.setEnabled(true);
            botonEliminar.setEnabled(true);
        }
    }

    private void onEvento(@Nullable EventoMateria evento) {
        if (evento == null) {
            return;
        }
        modelo.consumirEvento();
        if (evento.tipo() != EventoMateria.Tipo.ERROR) {
            finish();
            return;
        }
        ErrorValidacion error = evento.error();
        int mensaje = MensajesDeError.paraMateria(error);
        if (error == ErrorValidacion.NOMBRE_VACIO || error == ErrorValidacion.NOMBRE_MUY_LARGO
                || error == ErrorValidacion.NOMBRE_DUPLICADO) {
            contenedorNombre.setError(getString(mensaje));
        } else if (error == ErrorValidacion.META_FUERA_DE_RANGO) {
            contenedorMeta.setError(getString(mensaje));
        } else {
            Snackbar.make(findViewById(R.id.main), mensaje, Snackbar.LENGTH_LONG).show();
        }
    }

    private void onConfirmandoEliminar(Boolean confirmando) {
        cerrarDialogo();
        if (Boolean.TRUE.equals(confirmando) && materiaCargada != null) {
            dialogoAbierto = new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.materia_eliminar_titulo)
                    .setMessage(getString(R.string.materia_eliminar_mensaje, materiaCargada.nombre()))
                    .setPositiveButton(R.string.materia_eliminar_confirmar, this::onEliminarConfirmado)
                    .setNegativeButton(R.string.cancelar, this::onEliminarCancelado)
                    .setOnCancelListener(this::onDialogoCancelado)
                    .show();
        }
    }

    private void onColorClick(View vista) {
        selectorColor = new SelectorColorDialogo(this, colorElegido, this::onColorElegido);
        selectorColor.mostrar();
    }

    private void onColorElegido(int color) {
        colorElegido = 0xFF000000 | color;
        mostrarColor();
    }

    private void onVolverClick(View vista) {
        finish();
    }

    private void onGuardarClick(View vista) {
        contenedorNombre.setError(null);
        contenedorMeta.setError(null);
        Long id = idEdicion == SIN_ID ? null : idEdicion;
        modelo.guardar(id, textoDe(campoNombre), colorElegido, textoDe(campoDocente), textoDe(campoMeta));
    }

    private void onArchivarClick(View vista) {
        if (materiaCargada != null && materiaCargada.archivada()) {
            modelo.desarchivar(idEdicion);
        } else {
            modelo.archivar(idEdicion);
        }
    }

    private void onEliminarClick(View vista) {
        modelo.pedirEliminar();
    }

    private void onEliminarConfirmado(DialogInterface dialogo, int boton) {
        modelo.confirmarEliminar(idEdicion);
    }

    private void onEliminarCancelado(DialogInterface dialogo, int boton) {
        modelo.cancelarEliminar();
    }

    private void onDialogoCancelado(DialogInterface dialogo) {
        modelo.cancelarEliminar();
    }

    private void cerrarDialogo() {
        if (dialogoAbierto != null) {
            dialogoAbierto.setOnDismissListener(null);
            dialogoAbierto.dismiss();
            dialogoAbierto = null;
        }
    }

    private static String textoDe(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString();
    }
}
