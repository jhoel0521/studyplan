package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.readmodel.BloqueHorarioCalendario;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.readmodel.DatosCalendario;
import io.github.jhoel0521.studyplan.domain.readmodel.DetalleSesionInicio;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.FiltroCalendario;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.readmodel.ItemSesionCalendario;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.valueobject.PeriodoCalendario;
import io.github.jhoel0521.studyplan.domain.enumeration.VistaCalendario;

/**
 * Pantalla del calendario de estudio con vistas {@code Día}, {@code Semana} y {@code Mes},
 * filtros por materia/archivadas/estados, leyenda de estados, creación mediante pulsación larga en
 * {@code fecha >= hoy} y apertura de detalle al tocar cualquier sesión (HU-04, HU-07, HU-08,
 * HU-13, HU-14, HU-16 a HU-21, RNF-02, RNF-06, RNF-10, RNF-12, RNF-19).
 */
public class CalendarioActivity extends ActividadProtegida
        implements CalendarioMesAdapter.AccionesCelda {

    private static final int HORAS_DEL_DIA = 24;
    private static final int ALTURA_HORA_DP = 56;
    private static final int ANCHO_EJE_HORA_DP = 48;
    private static final int MIN_DISTANCIA_SWIPE_PX = 100;
    private static final int MIN_VELOCIDAD_SWIPE_PX = 100;

    private CalendarioViewModel modelo;

    private MaterialToolbar barraCalendario;
    private MaterialButtonToggleGroup grupoVista;
    private MaterialButton botonPeriodoAnterior;
    private MaterialButton botonPeriodoSiguiente;
    private TextView textoTituloPeriodo;
    private TextView textoResumenPeriodo;
    private MaterialButton botonFiltroActivo;

    private LinearLayout contenedorVistaMes;
    private RecyclerView listaCeldasMes;
    private TextView textoTituloSesionesDia;
    private TextView textoSinSesionesDia;
    private RecyclerView listaSesionesDia;

    private LinearLayout contenedorVistaHorario;
    private LinearLayout cabeceraDiasHorario;
    private ScrollView scrollHorario;
    private LinearLayout capaFranjasHorario;
    private FrameLayout capaBloquesHorario;

    private CalendarioMesAdapter adaptadorMes;
    private SesionInicioAdapter adaptadorSesionesDia;
    private GestureDetector detectorSwipe;

    private DatosCalendario ultimosDatos;
    private boolean actualizandoSelectorVista;
    private int alturaHoraPx;
    private int anchoEjeHoraPx;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendario);
        aplicarInsets();
        enlazarBarraInferior(DestinoPrincipal.CALENDARIO);

        alturaHoraPx = dp(ALTURA_HORA_DP);
        anchoEjeHoraPx = dp(ANCHO_EJE_HORA_DP);

        enlazarVistas();
        conectarEventos();

        modelo = viewModel(CalendarioViewModel.class);
        modelo.datos().observe(this, this::onDatosActualizados);
    }

    @Override
    protected void alEstarListo() {
        modelo.cargar();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (detectorSwipe != null) {
            detectorSwipe.onTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void enlazarVistas() {
        barraCalendario = findViewById(R.id.barraCalendario);
        grupoVista = findViewById(R.id.grupoVistaCalendario);
        GrupoBotones.ajustarEnUnaLinea(grupoVista);
        botonPeriodoAnterior = findViewById(R.id.botonPeriodoAnterior);
        botonPeriodoSiguiente = findViewById(R.id.botonPeriodoSiguiente);
        textoTituloPeriodo = findViewById(R.id.textoTituloPeriodo);
        textoResumenPeriodo = findViewById(R.id.textoResumenPeriodo);
        botonFiltroActivo = findViewById(R.id.botonFiltroActivo);

        contenedorVistaMes = findViewById(R.id.contenedorVistaMes);
        listaCeldasMes = findViewById(R.id.listaCeldasMes);
        textoTituloSesionesDia = findViewById(R.id.textoTituloSesionesDia);
        textoSinSesionesDia = findViewById(R.id.textoSinSesionesDia);
        listaSesionesDia = findViewById(R.id.listaSesionesDia);

        contenedorVistaHorario = findViewById(R.id.contenedorVistaHorario);
        cabeceraDiasHorario = findViewById(R.id.cabeceraDiasHorario);
        scrollHorario = findViewById(R.id.scrollHorario);
        capaFranjasHorario = findViewById(R.id.capaFranjasHorario);
        capaBloquesHorario = findViewById(R.id.capaBloquesHorario);

        adaptadorMes = new CalendarioMesAdapter(this);
        listaCeldasMes.setLayoutManager(new GridLayoutManager(this, 7));
        listaCeldasMes.setAdapter(adaptadorMes);

        adaptadorSesionesDia = new SesionInicioAdapter(this::alTocarDetalleSesion);
        listaSesionesDia.setLayoutManager(new LinearLayoutManager(this));
        listaSesionesDia.setAdapter(adaptadorSesionesDia);
    }

    private void conectarEventos() {
        barraCalendario.setOnMenuItemClickListener(this::alPulsarMenu);
        grupoVista.addOnButtonCheckedListener(this::alCambiarSelectorVista);
        botonPeriodoAnterior.setOnClickListener(this::alPulsarPeriodoAnterior);
        botonPeriodoSiguiente.setOnClickListener(this::alPulsarPeriodoSiguiente);
        botonFiltroActivo.setOnClickListener(this::alPulsarLimpiarFiltro);

        detectorSwipe = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(
                    MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) {
                    return false;
                }
                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();
                if (Math.abs(dx) > Math.abs(dy)
                        && Math.abs(dx) > MIN_DISTANCIA_SWIPE_PX
                        && Math.abs(velocityX) > MIN_VELOCIDAD_SWIPE_PX) {
                    if (dx > 0) {
                        modelo.anterior();
                    } else {
                        modelo.siguiente();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private boolean alPulsarMenu(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.accion_hoy) {
            modelo.irAHoy();
            return true;
        }
        if (id == R.id.accion_filtros) {
            mostrarDialogoFiltros();
            return true;
        }
        if (id == R.id.accion_leyenda) {
            mostrarDialogoLeyenda();
            return true;
        }
        return false;
    }

    private void alCambiarSelectorVista(
            MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
        if (actualizandoSelectorVista || !isChecked) {
            return;
        }
        if (checkedId == R.id.botonVistaDia) {
            modelo.cambiarVista(VistaCalendario.DIA);
        } else if (checkedId == R.id.botonVistaSemana) {
            modelo.cambiarVista(VistaCalendario.SEMANA);
        } else if (checkedId == R.id.botonVistaMes) {
            modelo.cambiarVista(VistaCalendario.MES);
        }
    }

    private void alPulsarPeriodoAnterior(View v) {
        modelo.anterior();
    }

    private void alPulsarPeriodoSiguiente(View v) {
        modelo.siguiente();
    }

    private void alPulsarLimpiarFiltro(View v) {
        modelo.restablecerFiltro();
    }

    private void onDatosActualizados(@Nullable DatosCalendario datos) {
        if (datos == null) {
            return;
        }
        ultimosDatos = datos;
        PeriodoCalendario periodo = datos.periodo();

        actualizandoSelectorVista = true;
        if (periodo.vista() == VistaCalendario.DIA) {
            grupoVista.check(R.id.botonVistaDia);
        } else if (periodo.vista() == VistaCalendario.SEMANA) {
            grupoVista.check(R.id.botonVistaSemana);
        } else {
            grupoVista.check(R.id.botonVistaMes);
        }
        actualizandoSelectorVista = false;

        textoTituloPeriodo.setText(formatearTituloPeriodo(periodo));

        String planTxt = getString(
                R.string.calendario_horas_compacto,
                String.format(Locale.getDefault(), "%.1f", datos.resumen().horasPlanificadas()));
        String realTxt = getString(
                R.string.calendario_horas_compacto,
                String.format(Locale.getDefault(), "%.1f", datos.resumen().horasRealizadas()));
        textoResumenPeriodo.setText(getString(R.string.calendario_resumen_horas, planTxt, realTxt));
        botonFiltroActivo.setVisibility(
                datos.filtro().tieneRestricciones() ? View.VISIBLE : View.GONE);

        if (periodo.vista() == VistaCalendario.MES) {
            contenedorVistaMes.setVisibility(View.VISIBLE);
            contenedorVistaHorario.setVisibility(View.GONE);
            renderizarVistaMes(datos);
        } else {
            contenedorVistaMes.setVisibility(View.GONE);
            contenedorVistaHorario.setVisibility(View.VISIBLE);
            renderizarVistaHorario(datos);
        }
    }

    private void renderizarVistaMes(DatosCalendario datos) {
        adaptadorMes.submitList(datos.celdasMes());
        Fecha seleccionado = datos.periodo().diaSeleccionado();
        if (seleccionado == null) {
            textoTituloSesionesDia.setText(R.string.calendario_sin_dia_seleccionado);
            textoSinSesionesDia.setVisibility(View.GONE);
            adaptadorSesionesDia.submitList(new ArrayList<>());
            return;
        }
        textoTituloSesionesDia.setText(
                getString(R.string.calendario_sesiones_del_dia, seleccionado.iso()));
        List<ItemSesionCalendario> items = datos.sesionesDiaSeleccionado();
        List<DetalleSesionInicio> lista = new ArrayList<>(items.size());
        for (ItemSesionCalendario item : items) {
            lista.add(new DetalleSesionInicio(
                    item.sesionId(),
                    item.materiaId(),
                    item.nombreMateria(),
                    item.colorMateria(),
                    item.fecha(),
                    item.horaInicio(),
                    item.horaFin(),
                    item.duracionPlanificadaMinutos(),
                    item.duracionRealMinutos(),
                    item.codigoEstado()));
        }
        adaptadorSesionesDia.submitList(lista);
        textoSinSesionesDia.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void renderizarVistaHorario(DatosCalendario datos) {
        PeriodoCalendario periodo = datos.periodo();
        int columnas = periodo.vista() == VistaCalendario.DIA ? 1 : 7;
        Fecha inicio = periodo.rangoVisibleInicio();

        construirCabeceraDiasHorario(inicio, columnas, datos.hoy());
        construirRejillaFranjas(inicio, columnas, datos.hoy());

        capaBloquesHorario.post(() -> {
            posicionarBloquesYLineaActual(datos, columnas);
            int scrollY = Math.round(datos.minutoScrollInicial() * (alturaHoraPx / 60f));
            scrollHorario.scrollTo(0, Math.max(0, scrollY));
        });
    }

    private void construirCabeceraDiasHorario(Fecha inicio, int columnas, Fecha hoy) {
        cabeceraDiasHorario.removeAllViews();

        View espacioEje = new View(this);
        espacioEje.setLayoutParams(new LinearLayout.LayoutParams(anchoEjeHoraPx, 1));
        cabeceraDiasHorario.addView(espacioEje);

        int[] nombresDias = {
            R.string.calendario_dia_lunes,
            R.string.calendario_dia_martes,
            R.string.calendario_dia_miercoles,
            R.string.calendario_dia_jueves,
            R.string.calendario_dia_viernes,
            R.string.calendario_dia_sabado,
            R.string.calendario_dia_domingo
        };

        int colorHoy = MaterialColors.getColor(
                cabeceraDiasHorario, com.google.android.material.R.attr.colorPrimary);
        int colorNormal = MaterialColors.getColor(
                cabeceraDiasHorario, com.google.android.material.R.attr.colorOnSurface);

        for (int c = 0; c < columnas; c++) {
            Fecha dia = inicio.sumarDias(c);
            TextView etiqueta = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            etiqueta.setLayoutParams(lp);
            etiqueta.setGravity(Gravity.CENTER);
            int diaSemana = dia.diaDeSemanaIso() - 1;
            etiqueta.setText(getString(nombresDias[diaSemana]) + "\n" + dia.dia());
            etiqueta.setTextColor(dia.equals(hoy) ? colorHoy : colorNormal);
            etiqueta.setTextSize(12f);
            cabeceraDiasHorario.addView(etiqueta);
        }
    }

    private void construirRejillaFranjas(Fecha inicio, int columnas, Fecha hoy) {
        capaFranjasHorario.removeAllViews();
        int colorLinea = MaterialColors.getColor(
                capaFranjasHorario, com.google.android.material.R.attr.colorOutlineVariant);
        int colorEje = MaterialColors.getColor(
                capaFranjasHorario, com.google.android.material.R.attr.colorOnSurfaceVariant);

        for (int hora = 0; hora < HORAS_DEL_DIA; hora++) {
            LinearLayout fila = new LinearLayout(this);
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, alturaHoraPx));

            TextView eje = new TextView(this);
            eje.setLayoutParams(new LinearLayout.LayoutParams(
                    anchoEjeHoraPx, LinearLayout.LayoutParams.MATCH_PARENT));
            eje.setGravity(Gravity.TOP | Gravity.END);
            eje.setPadding(0, 0, dp(6), 0);
            eje.setText(String.format(Locale.US, "%02d:00", hora));
            eje.setTextSize(11f);
            eje.setTextColor(colorEje);
            eje.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            fila.addView(eje);

            for (int c = 0; c < columnas; c++) {
                Fecha fechaCelda = inicio.sumarDias(c);
                int horaFinal = hora;

                FrameLayout celda = new FrameLayout(this);
                celda.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
                celda.setContentDescription(getString(
                        R.string.calendario_franja_libre_desc,
                        String.format(Locale.US, "%02d:00", hora),
                        fechaCelda.iso()));

                View separadorHorizontal = new View(this);
                separadorHorizontal.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, dp(1)));
                separadorHorizontal.setBackgroundColor(colorLinea);
                separadorHorizontal.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                celda.addView(separadorHorizontal);

                View separadorVertical = new View(this);
                separadorVertical.setLayoutParams(new FrameLayout.LayoutParams(
                        dp(1), FrameLayout.LayoutParams.MATCH_PARENT));
                separadorVertical.setBackgroundColor(colorLinea);
                separadorVertical.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                celda.addView(separadorVertical);

                celda.setOnLongClickListener(v -> {
                    alMantenerPulsadaFranja(fechaCelda, horaFinal, hoy);
                    return true;
                });

                fila.addView(celda);
            }
            capaFranjasHorario.addView(fila);
        }
    }

    private void posicionarBloquesYLineaActual(DatosCalendario datos, int columnas) {
        capaBloquesHorario.removeAllViews();
        int anchoDisponible = capaBloquesHorario.getWidth() - anchoEjeHoraPx;
        if (anchoDisponible <= 0) {
            return;
        }
        float anchoColumna = anchoDisponible / (float) columnas;
        float pxPorMinuto = alturaHoraPx / 60f;
        boolean vistaDia = datos.periodo().vista() == VistaCalendario.DIA;

        LayoutInflater inflater = LayoutInflater.from(this);
        for (BloqueHorarioCalendario bloque : datos.bloquesHorario()) {
            ItemSesionCalendario item = bloque.item();
            View vista = inflater.inflate(R.layout.item_bloque_calendario, capaBloquesHorario, false);
            MaterialCardView tarjeta = vista.findViewById(R.id.tarjetaBloqueCalendario);
            View franja = vista.findViewById(R.id.franjaColorBloque);
            TextView textoMateria = vista.findViewById(R.id.textoMateriaBloque);
            TextView textoHorario = vista.findViewById(R.id.textoHorarioBloque);
            ImageView iconoEstado = vista.findViewById(R.id.iconoEstadoBloque);
            TextView textoEstado = vista.findViewById(R.id.textoEstadoBloque);

            // Color de materia solo en la franja de 4 dp, ajustado a la superficie; el borde del bloque queda en
            // colorOutlineVariant del estilo Widget.StudyPlan.Card.Compact (design.md, decisión 6).
            franja.setBackgroundColor(AcentoMateria.ajustado(franja, item.colorMateria()));
            textoMateria.setText(item.nombreMateria());
            EstadoSesionVisual.aplicar(item.codigoEstado(), iconoEstado, textoEstado);

            String horaFinTexto = item.terminaEnMedianoche()
                    ? "24:00"
                    : item.horaFin().texto();
            if (vistaDia) {
                String rango = getString(
                        R.string.sesion_rango_horario,
                        item.horaInicio().texto(),
                        horaFinTexto);
                if (item.esContinuacionDiaAnterior()) {
                    textoHorario.setText(getString(
                            R.string.calendario_bloque_empezo_dia,
                            rango,
                            nombreDiaSemana(item.fechaInicioOriginal().diaDeSemanaIso())));
                } else {
                    textoHorario.setText(rango);
                }
                textoHorario.setVisibility(View.VISIBLE);
            } else {
                textoHorario.setVisibility(View.GONE);
            }

            String estadoEtiqueta = getString(EstadoSesionVisual.etiquetaDe(item.codigoEstado()));
            tarjeta.setContentDescription(getString(
                    R.string.calendario_bloque_desc,
                    item.nombreMateria(),
                    item.horaInicio().texto(),
                    horaFinTexto,
                    estadoEtiqueta));

            int duracionMin = Math.max(20, bloque.minutoFin() - bloque.minutoInicio());
            int altoPx = Math.max(dp(26), Math.round(duracionMin * pxPorMinuto));
            int topPx = Math.round(bloque.minutoInicio() * pxPorMinuto);

            float anchoSubColumna = anchoColumna / Math.max(1, bloque.totalSolapamientos());
            int leftPx = anchoEjeHoraPx
                    + Math.round(bloque.diaColumna() * anchoColumna + bloque.indiceSolapamiento() * anchoSubColumna)
                    + dp(1);
            int anchoPx = Math.max(dp(24), Math.round(anchoSubColumna) - dp(2));

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(anchoPx, altoPx);
            lp.leftMargin = leftPx;
            lp.topMargin = topPx;
            vista.setLayoutParams(lp);

            tarjeta.setOnClickListener(v -> abrirDetalleSesion(item.sesionId()));
            capaBloquesHorario.addView(vista);
        }

        // Línea de hora actual en la columna de hoy (HU-17, HU-18)
        Fecha visibleInicio = datos.periodo().rangoVisibleInicio();
        Fecha visibleFin = datos.periodo().rangoVisibleFin();
        Fecha hoy = datos.hoy();
        if (!hoy.esAnteriorA(visibleInicio) && !hoy.esPosteriorA(visibleFin)) {
            int colHoy = vistaDia ? 0 : hoy.diaDeSemanaIso() - 1;
            Hora ahora = datos.ahora();
            View lineaActual = new View(this);
            int colorHoy = MaterialColors.getColor(
                    capaBloquesHorario, com.google.android.material.R.attr.colorError);
            lineaActual.setBackgroundColor(colorHoy);
            lineaActual.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            FrameLayout.LayoutParams lpLinea = new FrameLayout.LayoutParams(
                    Math.round(anchoColumna), dp(2));
            lpLinea.leftMargin = anchoEjeHoraPx + Math.round(colHoy * anchoColumna);
            lpLinea.topMargin = Math.round(ahora.minutosDesdeMedianoche() * pxPorMinuto);
            lineaActual.setLayoutParams(lpLinea);
            capaBloquesHorario.addView(lineaActual);
        }
    }

    @Override
    public void alTocarDia(Fecha fecha) {
        modelo.seleccionarDia(fecha);
    }

    @Override
    public void alMantenerPulsadoDia(Fecha fecha) {
        if (ultimosDatos != null && !fecha.esAnteriorA(ultimosDatos.hoy())) {
            abrirNuevaSesion(fecha, null, null);
        }
    }

    private void alMantenerPulsadaFranja(Fecha fecha, int hora, Fecha hoy) {
        if (!fecha.esAnteriorA(hoy)) {
            String horaInicio = String.format(Locale.US, "%02d:00", hora);
            String horaFin = hora >= 23
                    ? "23:59"
                    : String.format(Locale.US, "%02d:00", hora + 1);
            abrirNuevaSesion(fecha, horaInicio, horaFin);
        }
    }

    private void alTocarDetalleSesion(DetalleSesionInicio sesion) {
        abrirDetalleSesion(sesion.sesionId());
    }

    private void abrirDetalleSesion(long sesionId) {
        Intent intent = new Intent(this, SesionDetalleActivity.class);
        intent.putExtra(SesionDetalleActivity.EXTRA_SESION_ID, sesionId);
        startActivity(intent);
    }

    private void abrirNuevaSesion(
            Fecha fecha, @Nullable String horaInicio, @Nullable String horaFin) {
        Intent intent = new Intent(this, SesionFormularioActivity.class);
        intent.putExtra(SesionFormularioActivity.EXTRA_MODO, SesionFormularioActivity.MODO_PLANIFICAR);
        intent.putExtra(SesionFormularioActivity.EXTRA_FECHA, fecha.iso());
        if (horaInicio != null) {
            intent.putExtra(SesionFormularioActivity.EXTRA_HORA_INICIO, horaInicio);
        }
        if (horaFin != null) {
            intent.putExtra(SesionFormularioActivity.EXTRA_HORA_FIN, horaFin);
        }
        startActivity(intent);
    }

    private String nombreDiaSemana(int diaIso) {
        int resId;
        switch (diaIso) {
            case 1:
                resId = R.string.calendario_dia_nombre_1;
                break;
            case 2:
                resId = R.string.calendario_dia_nombre_2;
                break;
            case 3:
                resId = R.string.calendario_dia_nombre_3;
                break;
            case 4:
                resId = R.string.calendario_dia_nombre_4;
                break;
            case 5:
                resId = R.string.calendario_dia_nombre_5;
                break;
            case 6:
                resId = R.string.calendario_dia_nombre_6;
                break;
            default:
                resId = R.string.calendario_dia_nombre_7;
                break;
        }
        return getString(resId);
    }

    private void mostrarDialogoLeyenda() {
        View vista = LayoutInflater.from(this)
                .inflate(R.layout.dialogo_leyenda_calendario, null);
        LinearLayout contenedor = vista.findViewById(R.id.contenedorLeyendaEstados);

        agregarFilaLeyenda(contenedor, CodigoEstadoSesion.PENDIENTE, R.string.calendario_leyenda_pendiente_desc);
        agregarFilaLeyenda(contenedor, CodigoEstadoSesion.EN_CURSO, R.string.calendario_leyenda_en_curso_desc);
        agregarFilaLeyenda(contenedor, CodigoEstadoSesion.COMPLETADA, R.string.calendario_leyenda_completada_desc);
        agregarFilaLeyenda(contenedor, CodigoEstadoSesion.COMPLETADA_TARDIA, R.string.calendario_leyenda_tardia_desc);
        agregarFilaLeyenda(contenedor, CodigoEstadoSesion.NO_REALIZADA, R.string.calendario_leyenda_no_realizada_desc);
        agregarFilaLeyenda(contenedor, CodigoEstadoSesion.REPROGRAMADA, R.string.calendario_leyenda_reprogramada_desc);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.calendario_leyenda_titulo)
                .setView(vista)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void agregarFilaLeyenda(
            LinearLayout contenedor, CodigoEstadoSesion estado, @StringRes int descripcionRes) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setGravity(Gravity.CENTER_VERTICAL);
        fila.setPadding(0, dp(6), 0, dp(6));

        ImageView icono = new ImageView(this);
        LinearLayout.LayoutParams lpIcono = new LinearLayout.LayoutParams(dp(20), dp(20));
        lpIcono.setMarginEnd(dp(12));
        icono.setLayoutParams(lpIcono);
        icono.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        LinearLayout columnaTexto = new LinearLayout(this);
        columnaTexto.setOrientation(LinearLayout.VERTICAL);

        TextView titulo = new TextView(this);
        titulo.setTextAppearance(R.style.TextAppearance_StudyPlan_TitleSmall);

        TextView descripcion = new TextView(this);
        descripcion.setText(descripcionRes);
        descripcion.setTextAppearance(R.style.TextAppearance_StudyPlan_BodySmall);

        EstadoSesionVisual.aplicar(estado, icono, titulo);

        columnaTexto.addView(titulo);
        columnaTexto.addView(descripcion);
        fila.addView(icono);
        fila.addView(columnaTexto);
        contenedor.addView(fila);
    }

    private void mostrarDialogoFiltros() {
        if (ultimosDatos == null) {
            return;
        }
        View vista = LayoutInflater.from(this)
                .inflate(R.layout.dialogo_filtros_calendario, null);
        MaterialAutoCompleteTextView selectorMateria =
                vista.findViewById(R.id.selectorFiltroMateria);
        MaterialSwitch interruptorArchivadas =
                vista.findViewById(R.id.interruptorIncluirArchivadas);
        LinearLayout contenedorEstados = vista.findViewById(R.id.contenedorCheckEstados);

        FiltroCalendario actual = ultimosDatos.filtro();
        interruptorArchivadas.setChecked(actual.incluirArchivadas());

        List<Materia> materias = ultimosDatos.materiasDisponibles();
        List<String> opciones = new ArrayList<>();
        List<Long> idsOpciones = new ArrayList<>();
        opciones.add(getString(R.string.calendario_filtros_todas_materias));
        idsOpciones.add(null);

        int indiceSeleccionado = 0;
        for (Materia m : materias) {
            opciones.add(m.nombre());
            idsOpciones.add(m.id());
            if (actual.materiaId() != null && actual.materiaId().equals(m.id())) {
                indiceSeleccionado = opciones.size() - 1;
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, opciones);
        selectorMateria.setAdapter(adapter);
        selectorMateria.setText(opciones.get(indiceSeleccionado), false);

        final Long[] materiaElegida = {idsOpciones.get(indiceSeleccionado)};
        selectorMateria.setOnItemClickListener((parent, view, position, id) ->
                materiaElegida[0] = idsOpciones.get(position));

        Map<CodigoEstadoSesion, CheckBox> checksPorEstado = new EnumMap<>(CodigoEstadoSesion.class);
        for (CodigoEstadoSesion codigo : CodigoEstadoSesion.values()) {
            CheckBox check = new CheckBox(this);
            check.setText(EstadoSesionVisual.etiquetaDe(codigo));
            check.setMinHeight(dp(48));
            check.setChecked(actual.estadosVisibles().contains(codigo));
            contenedorEstados.addView(check);
            checksPorEstado.put(codigo, check);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.calendario_filtros_titulo)
                .setView(vista)
                .setNeutralButton(R.string.calendario_filtros_restablecer,
                        (dialog, which) -> modelo.restablecerFiltro())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.calendario_filtros_aplicar, (dialog, which) -> {
                    Set<CodigoEstadoSesion> elegidos = EnumSet.noneOf(CodigoEstadoSesion.class);
                    for (Map.Entry<CodigoEstadoSesion, CheckBox> entry : checksPorEstado.entrySet()) {
                        if (entry.getValue().isChecked()) {
                            elegidos.add(entry.getKey());
                        }
                    }
                    if (elegidos.isEmpty()) {
                        elegidos = EnumSet.allOf(CodigoEstadoSesion.class);
                    }
                    modelo.aplicarFiltro(FiltroCalendario.de(
                            materiaElegida[0],
                            interruptorArchivadas.isChecked(),
                            elegidos));
                })
                .show();
    }

    private String formatearTituloPeriodo(PeriodoCalendario periodo) {
        String[] meses = new DateFormatSymbols(Locale.getDefault()).getMonths();
        String[] mesesCortos = new DateFormatSymbols(Locale.getDefault()).getShortMonths();
        Fecha inicio = periodo.rangoVisibleInicio();
        Fecha fin = periodo.rangoVisibleFin();
        if (periodo.vista() == VistaCalendario.MES) {
            String nombreMes = meses[inicio.mes() - 1];
            if (!nombreMes.isEmpty()) {
                nombreMes = nombreMes.substring(0, 1).toUpperCase(Locale.getDefault())
                        + nombreMes.substring(1);
            }
            return nombreMes + " " + inicio.anio();
        }
        if (periodo.vista() == VistaCalendario.DIA) {
            return inicio.dia() + " " + mesesCortos[inicio.mes() - 1] + " " + inicio.anio();
        }
        if (inicio.mes() == fin.mes()) {
            return inicio.dia() + " – " + fin.dia() + " "
                    + mesesCortos[fin.mes() - 1] + " " + fin.anio();
        }
        return inicio.dia() + " " + mesesCortos[inicio.mes() - 1]
                + " – " + fin.dia() + " " + mesesCortos[fin.mes() - 1] + " " + fin.anio();
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }
}
