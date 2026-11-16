package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.DetalleSesion;
import io.github.jhoel0521.studyplan.domain.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.Fecha;
import io.github.jhoel0521.studyplan.domain.Hora;
import io.github.jhoel0521.studyplan.domain.Resultado;
import io.github.jhoel0521.studyplan.domain.ResumenMateria;
import io.github.jhoel0521.studyplan.domain.SesionEstudio;

/**
 * Formulario para planificar sesiones futuras o de hoy, registrar sesiones pasadas (últimos 3 días) y
 * reprogramar sesiones pendientes o no realizadas, con soporte de selector Material o entrada directa y
 * conservación de estado ante rotación.
 *
 * <p>Implementa: HU-04, HU-10, HU-11, HU-14, RNF-10 y RNF-12.
 */
public class SesionFormularioActivity extends ActividadProtegida {

    public static final String EXTRA_MODO = "extra_modo";
    public static final String EXTRA_FECHA = "extra_fecha";
    public static final String EXTRA_HORA_INICIO = "extra_hora_inicio";
    public static final String EXTRA_HORA_FIN = "extra_hora_fin";
    public static final String EXTRA_SESION_ID = "extra_sesion_id";

    public static final String MODO_PLANIFICAR = "planificar";
    public static final String MODO_PASADA = "pasada";
    public static final String MODO_REPROGRAMAR = "reprogramar";

    private static final String ESTADO_MODO = "estado_modo";
    private static final String ESTADO_MATERIA_ID = "estado_materia_id";
    private static final String ESTADO_DURACION_EDITADA = "estado_duracion_editada";
    private static final String ESTADO_BASE_CARGADA = "estado_base_cargada";
    private static final String TAG_SELECTOR_FECHA = "selector_fecha_sesion";
    private static final String TAG_SELECTOR_HORA_INICIO = "selector_hora_inicio";
    private static final String TAG_SELECTOR_HORA_FIN = "selector_hora_fin";

    private SesionFormularioViewModel modelo;
    private MaterialToolbar barraSuperior;
    private RadioGroup grupoModo;
    private RadioButton radioPlanificar;
    private RadioButton radioPasada;
    private TextInputLayout grupoMateria;
    private MaterialAutoCompleteTextView selectorMateria;
    private TextInputLayout grupoFecha;
    private TextInputEditText campoFecha;
    private MaterialButton botonElegirFecha;
    private TextInputLayout grupoHoraInicio;
    private TextInputEditText campoHoraInicio;
    private MaterialButton botonElegirHoraInicio;
    private TextInputLayout grupoHoraFin;
    private TextInputEditText campoHoraFin;
    private MaterialButton botonElegirHoraFin;
    private TextView textoDuracionPlanificada;
    private TextInputLayout grupoDuracionReal;
    private TextInputEditText campoDuracionReal;
    private TextInputLayout grupoNotas;
    private TextInputEditText campoNotas;
    private TextView textoErrorFormulario;
    private MaterialButton botonGuardarSesion;

    private String modoActual = MODO_PLANIFICAR;
    private long sesionBaseId = -1L;
    private long materiaSeleccionadaId = -1L;
    private boolean duracionRealEditadaManualmente;
    private boolean actualizandoDuracionAutomatica;
    private boolean sesionBaseAplicada;
    private List<ResumenMateria> listaMaterias = Collections.emptyList();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sesion_formulario);
        aplicarInsets();
        leerParametrosIniciales(savedInstanceState);
        enlazarVistas();
        conectarSelectoresDialogoAbiertos();
        actualizarAspectoSegunModo();

        modelo = viewModel(SesionFormularioViewModel.class);
        modelo.materiasActivas().observe(this, this::onMateriasActivas);
        modelo.fechaHoy().observe(this, this::onFechaHoy);
        modelo.sesionBase().observe(this, this::onSesionBase);
        modelo.evento().observe(this, this::onEvento);

        if (savedInstanceState == null) {
            String fechaInicial = getIntent().getStringExtra(EXTRA_FECHA);
            if (fechaInicial != null && !fechaInicial.isEmpty()) {
                campoFecha.setText(fechaInicial);
            }
            if (MODO_PLANIFICAR.equals(modoActual)) {
                String horaInicioInicial = getIntent().getStringExtra(EXTRA_HORA_INICIO);
                if (horaInicioInicial != null && !horaInicioInicial.isEmpty()) {
                    campoHoraInicio.setText(horaInicioInicial);
                }
                String horaFinInicial = getIntent().getStringExtra(EXTRA_HORA_FIN);
                if (horaFinInicial != null && !horaFinInicial.isEmpty()) {
                    campoHoraFin.setText(horaFinInicial);
                }
            }
        }
    }

    @Override
    protected void alEstarListo() {
        modelo.cargar(MODO_REPROGRAMAR.equals(modoActual) ? sesionBaseId : -1L);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ESTADO_MODO, modoActual);
        outState.putLong(ESTADO_MATERIA_ID, materiaSeleccionadaId);
        outState.putBoolean(ESTADO_DURACION_EDITADA, duracionRealEditadaManualmente);
        outState.putBoolean(ESTADO_BASE_CARGADA, sesionBaseAplicada);
    }

    private void leerParametrosIniciales(@Nullable Bundle savedInstanceState) {
        sesionBaseId = getIntent().getLongExtra(EXTRA_SESION_ID, -1L);
        if (savedInstanceState != null) {
            modoActual = savedInstanceState.getString(ESTADO_MODO, MODO_PLANIFICAR);
            materiaSeleccionadaId = savedInstanceState.getLong(ESTADO_MATERIA_ID, -1L);
            duracionRealEditadaManualmente = savedInstanceState.getBoolean(ESTADO_DURACION_EDITADA, false);
            sesionBaseAplicada = savedInstanceState.getBoolean(ESTADO_BASE_CARGADA, false);
            return;
        }
        String extraModo = getIntent().getStringExtra(EXTRA_MODO);
        if (MODO_PASADA.equals(extraModo) || MODO_REPROGRAMAR.equals(extraModo)) {
            modoActual = extraModo;
        } else {
            modoActual = MODO_PLANIFICAR;
        }
    }

    private void enlazarVistas() {
        barraSuperior = findViewById(R.id.barraSuperior);
        grupoModo = findViewById(R.id.grupoModo);
        radioPlanificar = findViewById(R.id.radioPlanificar);
        radioPasada = findViewById(R.id.radioPasada);
        grupoMateria = findViewById(R.id.grupoMateria);
        selectorMateria = findViewById(R.id.selectorMateria);
        grupoFecha = findViewById(R.id.grupoFecha);
        campoFecha = findViewById(R.id.campoFecha);
        botonElegirFecha = findViewById(R.id.botonElegirFecha);
        grupoHoraInicio = findViewById(R.id.grupoHoraInicio);
        campoHoraInicio = findViewById(R.id.campoHoraInicio);
        botonElegirHoraInicio = findViewById(R.id.botonElegirHoraInicio);
        grupoHoraFin = findViewById(R.id.grupoHoraFin);
        campoHoraFin = findViewById(R.id.campoHoraFin);
        botonElegirHoraFin = findViewById(R.id.botonElegirHoraFin);
        textoDuracionPlanificada = findViewById(R.id.textoDuracionPlanificada);
        grupoDuracionReal = findViewById(R.id.grupoDuracionReal);
        campoDuracionReal = findViewById(R.id.campoDuracionReal);
        grupoNotas = findViewById(R.id.grupoNotas);
        campoNotas = findViewById(R.id.campoNotas);
        textoErrorFormulario = findViewById(R.id.textoErrorFormulario);
        botonGuardarSesion = findViewById(R.id.botonGuardarSesion);

        barraSuperior.setNavigationOnClickListener(this::onVolverClick);
        grupoModo.setOnCheckedChangeListener(this::onCambioModo);
        selectorMateria.setOnItemClickListener(this::onMateriaElegida);
        botonElegirFecha.setOnClickListener(this::onElegirFechaClick);
        botonElegirHoraInicio.setOnClickListener(this::onElegirHoraInicioClick);
        botonElegirHoraFin.setOnClickListener(this::onElegirHoraFinClick);
        botonGuardarSesion.setOnClickListener(this::onGuardarClick);

        campoHoraInicio.addTextChangedListener(new ObservadorCambioHorario());
        campoHoraFin.addTextChangedListener(new ObservadorCambioHorario());
        campoDuracionReal.addTextChangedListener(new ObservadorCambioDuracionReal());
    }

    private void onVolverClick(View vista) {
        finish();
    }

    private void onCambioModo(RadioGroup grupo, int idSeleccionado) {
        if (MODO_REPROGRAMAR.equals(modoActual)) {
            return;
        }
        modoActual = idSeleccionado == R.id.radioPasada ? MODO_PASADA : MODO_PLANIFICAR;
        actualizarAspectoSegunModo();
        actualizarDuracionPlanificadaYReal();
    }

    private void actualizarAspectoSegunModo() {
        if (MODO_REPROGRAMAR.equals(modoActual)) {
            barraSuperior.setTitle(R.string.sesion_form_titulo_reprogramar);
            grupoModo.setVisibility(View.GONE);
            grupoMateria.setEnabled(false);
            grupoDuracionReal.setVisibility(View.GONE);
            return;
        }
        grupoModo.setVisibility(View.VISIBLE);
        grupoMateria.setEnabled(true);
        boolean esPasada = MODO_PASADA.equals(modoActual);
        barraSuperior.setTitle(esPasada
                ? R.string.sesion_form_titulo_pasada
                : R.string.sesion_form_titulo_planificar);
        radioPlanificar.setChecked(!esPasada);
        radioPasada.setChecked(esPasada);
        grupoDuracionReal.setVisibility(esPasada ? View.VISIBLE : View.GONE);
    }

    private void onMateriasActivas(List<ResumenMateria> materias) {
        listaMaterias = materias != null ? materias : Collections.emptyList();
        List<String> nombres = new ArrayList<>(listaMaterias.size());
        String seleccionado = null;
        for (ResumenMateria item : listaMaterias) {
            nombres.add(item.materia().nombre());
            if (item.materia().id() == materiaSeleccionadaId) {
                seleccionado = item.materia().nombre();
            }
        }
        ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, nombres);
        selectorMateria.setAdapter(adaptador);
        grupoMateria.setHelperText(listaMaterias.isEmpty() && !MODO_REPROGRAMAR.equals(modoActual)
                ? getString(R.string.sesion_form_sin_materias)
                : null);
        if (seleccionado != null) {
            selectorMateria.setText(seleccionado, false);
        }
    }

    private void onMateriaElegida(AdapterView<?> padre, View vista, int posicion, long id) {
        if (posicion >= 0 && posicion < listaMaterias.size()) {
            materiaSeleccionadaId = listaMaterias.get(posicion).materia().id();
            grupoMateria.setError(null);
        }
    }

    private void onFechaHoy(Fecha hoy) {
        if (hoy != null && !MODO_REPROGRAMAR.equals(modoActual)
                && textoDe(campoFecha).isEmpty()) {
            campoFecha.setText(hoy.iso());
        }
    }

    private void onSesionBase(DetalleSesion detalle) {
        if (detalle == null || sesionBaseAplicada) {
            return;
        }
        sesionBaseAplicada = true;
        SesionEstudio s = detalle.sesion();
        materiaSeleccionadaId = s.materiaId();
        selectorMateria.setText(detalle.nombreMateria(), false);
        campoFecha.setText(s.fecha().iso());
        campoHoraInicio.setText(s.horaInicio().texto());
        campoHoraFin.setText(s.horaFin().texto());
        campoNotas.setText(s.notas());
        actualizarDuracionPlanificadaYReal();
    }

    private void onElegirFechaClick(View vista) {
        MaterialDatePicker.Builder<Long> constructor = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.sesion_form_elegir_fecha);
        Resultado<Fecha> actual = Fecha.parsear(textoDe(campoFecha));
        if (actual.esValido()) {
            constructor.setSelection(fechaAUtcMillis(actual.valor()));
        }
        MaterialDatePicker<Long> selector = constructor.build();
        selector.addOnPositiveButtonClickListener(this::onFechaSeleccionada);
        selector.show(getSupportFragmentManager(), TAG_SELECTOR_FECHA);
    }

    @SuppressWarnings("unchecked")
    private void conectarSelectoresDialogoAbiertos() {
        var fFecha = getSupportFragmentManager().findFragmentByTag(TAG_SELECTOR_FECHA);
        if (fFecha instanceof MaterialDatePicker) {
            ((MaterialDatePicker<Long>) fFecha).addOnPositiveButtonClickListener(this::onFechaSeleccionada);
        }
        var fInicio = getSupportFragmentManager().findFragmentByTag(TAG_SELECTOR_HORA_INICIO);
        if (fInicio instanceof MaterialTimePicker) {
            ((MaterialTimePicker) fInicio).addOnPositiveButtonClickListener(this::onHoraInicioConfirmada);
        }
        var fFin = getSupportFragmentManager().findFragmentByTag(TAG_SELECTOR_HORA_FIN);
        if (fFin instanceof MaterialTimePicker) {
            ((MaterialTimePicker) fFin).addOnPositiveButtonClickListener(this::onHoraFinConfirmada);
        }
    }

    private void onFechaSeleccionada(Long seleccionUtcMillis) {
        if (seleccionUtcMillis == null) {
            return;
        }
        Calendar calendario = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendario.setTimeInMillis(seleccionUtcMillis);
        Resultado<Fecha> fecha = Fecha.de(
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH) + 1,
                calendario.get(Calendar.DAY_OF_MONTH));
        if (fecha.esValido()) {
            campoFecha.setText(fecha.valor().iso());
            grupoFecha.setError(null);
        }
    }

    private static long fechaAUtcMillis(Fecha fecha) {
        Calendar calendario = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendario.clear();
        calendario.set(fecha.anio(), fecha.mes() - 1, fecha.dia());
        return calendario.getTimeInMillis();
    }

    private void onElegirHoraInicioClick(View vista) {
        mostrarSelectorHora(campoHoraInicio, R.string.sesion_form_elegir_hora_inicio, TAG_SELECTOR_HORA_INICIO);
    }

    private void onElegirHoraFinClick(View vista) {
        mostrarSelectorHora(campoHoraFin, R.string.sesion_form_elegir_hora_fin, TAG_SELECTOR_HORA_FIN);
    }

    private void mostrarSelectorHora(TextInputEditText destino, int tituloRes, String tag) {
        Resultado<Hora> actual = Hora.parsear(textoDe(destino));
        int horaInicial = actual.esValido() ? actual.valor().hora() : 9;
        int minutoInicial = actual.esValido() ? actual.valor().minuto() : 0;
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(horaInicial)
                .setMinute(minutoInicial)
                .setTitleText(tituloRes)
                .build();
        picker.addOnPositiveButtonClickListener(
                TAG_SELECTOR_HORA_INICIO.equals(tag)
                        ? this::onHoraInicioConfirmada
                        : this::onHoraFinConfirmada);
        picker.show(getSupportFragmentManager(), tag);
    }

    private void onHoraInicioConfirmada(View vista) {
        var fragmento = getSupportFragmentManager().findFragmentByTag(TAG_SELECTOR_HORA_INICIO);
        if (fragmento instanceof MaterialTimePicker) {
            aplicarHoraDePicker((MaterialTimePicker) fragmento, campoHoraInicio);
        }
    }

    private void onHoraFinConfirmada(View vista) {
        var fragmento = getSupportFragmentManager().findFragmentByTag(TAG_SELECTOR_HORA_FIN);
        if (fragmento instanceof MaterialTimePicker) {
            aplicarHoraDePicker((MaterialTimePicker) fragmento, campoHoraFin);
        }
    }

    private void aplicarHoraDePicker(MaterialTimePicker picker, TextInputEditText destino) {
        Resultado<Hora> hora = Hora.de(picker.getHour(), picker.getMinute());
        if (hora.esValido()) {
            destino.setText(hora.valor().texto());
        }
    }

    private void actualizarDuracionPlanificadaYReal() {
        Resultado<Hora> inicio = Hora.parsear(
                textoDe(campoHoraInicio), ErrorValidacion.HORA_INICIO_OBLIGATORIA);
        Resultado<Hora> fin = Hora.parsear(
                textoDe(campoHoraFin), ErrorValidacion.HORA_FIN_OBLIGATORIA);
        if (!inicio.esValido() || !fin.esValido() || !fin.valor().esPosteriorA(inicio.valor())) {
            textoDuracionPlanificada.setText(R.string.sesion_form_duracion_planificada_invalida);
            return;
        }
        int minutos = inicio.valor().minutosHasta(fin.valor());
        textoDuracionPlanificada.setText(
                getString(R.string.sesion_form_duracion_planificada, minutos));
        if (MODO_PASADA.equals(modoActual) && !duracionRealEditadaManualmente) {
            actualizandoDuracionAutomatica = true;
            campoDuracionReal.setText(String.valueOf(minutos));
            actualizandoDuracionAutomatica = false;
        }
    }

    private void onGuardarClick(View vista) {
        limpiarErrores();
        String fechaTxt = textoDe(campoFecha);
        String inicioTxt = textoDe(campoHoraInicio);
        String finTxt = textoDe(campoHoraFin);
        String notasTxt = textoDe(campoNotas);

        if (MODO_REPROGRAMAR.equals(modoActual)) {
            modelo.reprogramar(sesionBaseId, fechaTxt, inicioTxt, finTxt, notasTxt);
        } else if (MODO_PASADA.equals(modoActual)) {
            modelo.guardarPasada(
                    materiaSeleccionadaId,
                    fechaTxt,
                    inicioTxt,
                    finTxt,
                    textoDe(campoDuracionReal),
                    notasTxt);
        } else {
            modelo.guardarPlanificada(materiaSeleccionadaId, fechaTxt, inicioTxt, finTxt, notasTxt);
        }
    }

    private void onEvento(@Nullable EventoSesion evento) {
        if (evento == null) {
            return;
        }
        modelo.consumirEvento();
        if (evento.tipo() != EventoSesion.Tipo.ERROR) {
            finish();
            return;
        }
        mostrarError(evento);
    }

    private void mostrarError(EventoSesion evento) {
        ErrorValidacion error = evento.error();
        if (error == null) {
            return;
        }
        String mensaje = MensajesDeError.textoSesion(this, error, evento.conflicto());
        switch (error) {
            case MATERIA_SESION_OBLIGATORIA:
            case MATERIA_NO_ENCONTRADA:
            case MATERIA_INACTIVA:
                grupoMateria.setError(mensaje);
                break;
            case FECHA_SESION_OBLIGATORIA:
            case FECHA_SESION_FORMATO_INVALIDO:
            case FECHA_PLANIFICACION_PASADA:
            case FECHA_PASADA_FUERA_DE_VENTANA:
                grupoFecha.setError(mensaje);
                break;
            case HORA_INICIO_OBLIGATORIA:
                grupoHoraInicio.setError(mensaje);
                break;
            case HORA_FIN_OBLIGATORIA:
            case HORARIO_FIN_NO_POSTERIOR:
                grupoHoraFin.setError(mensaje);
                break;
            case HORA_FORMATO_INVALIDO:
                boolean inicioInvalida = !Hora.parsear(textoDe(campoHoraInicio)).esValido();
                boolean finInvalida = !Hora.parsear(textoDe(campoHoraFin)).esValido();
                if (inicioInvalida || !finInvalida) {
                    grupoHoraInicio.setError(mensaje);
                }
                if (finInvalida || !inicioInvalida) {
                    grupoHoraFin.setError(mensaje);
                }
                break;
            case DURACION_REAL_INVALIDA:
                grupoDuracionReal.setError(mensaje);
                break;
            case NOTAS_SESION_DEMASIADO_LARGAS:
                grupoNotas.setError(mensaje);
                break;
            default:
                textoErrorFormulario.setText(mensaje);
                textoErrorFormulario.setVisibility(View.VISIBLE);
        }
    }

    private void limpiarErrores() {
        grupoMateria.setError(null);
        grupoFecha.setError(null);
        grupoHoraInicio.setError(null);
        grupoHoraFin.setError(null);
        grupoDuracionReal.setError(null);
        grupoNotas.setError(null);
        textoErrorFormulario.setVisibility(View.GONE);
    }

    private static String textoDe(TextInputEditText campo) {
        Editable texto = campo.getText();
        return texto == null ? "" : texto.toString().trim();
    }

    private final class ObservadorCambioHorario implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            actualizarDuracionPlanificadaYReal();
        }
    }

    private final class ObservadorCambioDuracionReal implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (!actualizandoDuracionAutomatica && s != null && s.length() > 0) {
                duracionRealEditadaManualmente = true;
            }
        }
    }
}
