package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.widget.TextViewCompat;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.application.port.in.ResolverRegresoApp;
import io.github.jhoel0521.studyplan.domain.readmodel.CierreSesion;
import io.github.jhoel0521.studyplan.domain.readmodel.DiagnosticoRegresoApp;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.enumeration.TopicoEventoEstudio;

/**
 * Diálogo {@code W13} presentado después de superar {@link ActividadProtegida} cuando existe una
 * sesión {@code EN_CURSO} con salida sin resolver o cierre abrupto (HU-41, RN-24).
 */
public final class DialogoRecuperacionSesion {

    private DialogoRecuperacionSesion() {
    }

    public static AlertDialog mostrar(
            ActividadProtegida actividad,
            DiagnosticoRegresoApp diagnostico,
            Runnable alResolver) {
        StudyPlanApplication app = actividad.app();
        View vista = LayoutInflater.from(actividad)
                .inflate(R.layout.dialogo_recuperacion_sesion, null);

        RadioGroup grupoOpciones = vista.findViewById(R.id.grupoOpcionesRecuperacion);
        TextInputLayout grupoHora = vista.findViewById(R.id.grupoHoraFinRecuperacion);
        TextInputEditText campoHora = vista.findViewById(R.id.campoHoraFinRecuperacion);
        TextView tituloTopicos = vista.findViewById(R.id.textoTituloTopicoRecuperacion);
        ChipGroup grupoTopicos = vista.findViewById(R.id.grupoTopicosRecuperacion);
        TextInputEditText campoNota = vista.findViewById(R.id.campoNotaRecuperacion);
        TextView textoError = vista.findViewById(R.id.textoErrorRecuperacion);

        Hora horaSalida = app.reloj().horaDe(diagnostico.instanteSalidaMilis());
        Hora horaActual = app.reloj().horaDe(diagnostico.instanteLimiteMaxMilis());
        campoHora.setText(horaActual.texto());

        grupoOpciones.setOnCheckedChangeListener((group, checkedId) -> {
            grupoHora.setVisibility(checkedId == R.id.radioHastaHora ? View.VISIBLE : View.GONE);
            if (checkedId == R.id.radioSigoEstudiando) {
                TopicosUi.poblarChips(
                        actividad,
                        grupoTopicos,
                        TopicosUi.topicosParaEstudioExterno(),
                        TopicoEventoEstudio.SIN_CLASIFICAR);
                tituloTopicos.setVisibility(View.VISIBLE);
                grupoTopicos.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.radioRetomarSinSumar) {
                TopicosUi.poblarChips(
                        actividad,
                        grupoTopicos,
                        TopicosUi.topicosParaInterrupcion(),
                        TopicoEventoEstudio.SIN_CLASIFICAR);
                tituloTopicos.setVisibility(View.VISIBLE);
                grupoTopicos.setVisibility(View.VISIBLE);
            } else {
                tituloTopicos.setVisibility(View.GONE);
                grupoTopicos.setVisibility(View.GONE);
            }
            textoError.setVisibility(View.GONE);
        });

        String titulo = diagnostico.esRecuperacionCierreAbrupto()
                ? actividad.getString(R.string.recuperacion_sigkill_titulo, horaSalida.texto())
                : actividad.getString(R.string.recuperacion_titulo, horaSalida.texto());

        // Título propio: el DialogTitle de AppCompat lo limita a dos líneas y con la fuente grande del sistema se
        // cortaba la pregunta (RNF-06, texto ampliado).
        TextView vistaTitulo = new TextView(actividad);
        TextViewCompat.setTextAppearance(vistaTitulo, R.style.TextAppearance_StudyPlan_HeadlineSmall);
        vistaTitulo.setTextColor(MaterialColors.getColor(
                vistaTitulo, com.google.android.material.R.attr.colorOnSurface));
        int margen = Math.round(24 * actividad.getResources().getDisplayMetrics().density);
        vistaTitulo.setPadding(margen, margen, margen, 0);
        vistaTitulo.setText(titulo);
        ViewCompat.setAccessibilityHeading(vistaTitulo, true);

        AlertDialog dialogo = new MaterialAlertDialogBuilder(actividad)
                .setCustomTitle(vistaTitulo)
                .setView(vista)
                .setCancelable(false)
                .setPositiveButton(R.string.recuperacion_confirmar, null)
                .create();

        dialogo.setOnShowListener(d -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    int elegido = grupoOpciones.getCheckedRadioButtonId();
                    ResolverRegresoApp.OpcionRegreso opcion;
                    Long hastaMilis = null;
                    if (elegido == R.id.radioHastaHora) {
                        opcion = ResolverRegresoApp.OpcionRegreso.HASTA_HORA;
                        String horaTxt = campoHora.getText() == null
                                ? "" : campoHora.getText().toString().trim();
                        Resultado<Hora> hRes = Hora.parsear(horaTxt);
                        if (!hRes.esValido()) {
                            textoError.setText(R.string.error_sesion_hora_invalida);
                            textoError.setVisibility(View.VISIBLE);
                            return;
                        }
                        Fecha fechaBase = app.reloj().fechaDe(diagnostico.instanteLimiteMaxMilis());
                        hastaMilis = app.reloj().instanteDe(fechaBase, hRes.valor());
                    } else if (elegido == R.id.radioSigoEstudiando) {
                        opcion = ResolverRegresoApp.OpcionRegreso.SIGO_ESTUDIANDO;
                    } else if (elegido == R.id.radioRetomarSinSumar) {
                        opcion = ResolverRegresoApp.OpcionRegreso.RETOMAR_SIN_SUMAR;
                    } else {
                        opcion = ResolverRegresoApp.OpcionRegreso.NO_SEGUI;
                    }

                    TopicoEventoEstudio topico = (opcion == ResolverRegresoApp.OpcionRegreso.SIGO_ESTUDIANDO
                            || opcion == ResolverRegresoApp.OpcionRegreso.RETOMAR_SIN_SUMAR)
                            ? TopicosUi.obtenerSeleccionado(grupoTopicos)
                            : TopicoEventoEstudio.SIN_CLASIFICAR;
                    String nota = campoNota.getText() == null
                            ? "" : campoNota.getText().toString().trim();
                    Long finalHasta = hastaMilis;
                    app.ejecutor().execute(() -> {
                        Resultado<CierreSesion> res = app.resolverRegresoApp().ejecutarConMetas(
                                diagnostico.sesionEnCurso().id(), opcion, finalHasta, nota, topico);
                        actividad.runOnUiThread(() -> {
                            if (!res.esValido()) {
                                textoError.setText(R.string.error_hora_declarada_rango);
                                textoError.setVisibility(View.VISIBLE);
                                return;
                            }
                            NotificadorSesion.cancelarAvisoRetorno(actividad);
                            dialogo.dismiss();
                            if ((opcion == ResolverRegresoApp.OpcionRegreso.SIGO_ESTUDIANDO
                                    || opcion == ResolverRegresoApp.OpcionRegreso.RETOMAR_SIN_SUMAR)
                                    && !(actividad instanceof SesionEnCursoActivity)) {
                                Intent i = new Intent(actividad, SesionEnCursoActivity.class);
                                i.putExtra(SesionEnCursoActivity.EXTRA_SESION_ID,
                                        diagnostico.sesionEnCurso().id());
                                actividad.startActivity(i);
                            } else if (!res.valor().metasAlcanzadas().isEmpty()) {
                                actividad.startActivity(FelicitacionMetasActivity.crearIntent(
                                        actividad, res.valor().metasAlcanzadas()));
                            } else if (alResolver != null) {
                                alResolver.run();
                            }
                        });
                    });
                }));

        dialogo.show();
        return dialogo;
    }
}
