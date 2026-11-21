package io.github.jhoel0521.studyplan.adapter.in.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.graphics.Bitmap;
import android.content.Intent;
import android.content.res.Configuration;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.Tema;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TipoEventoEstudio;

/**
 * Prueba de humo de la línea gráfica: abre las pantallas que cambia el change
 * {@code aplicar-linea-grafica-indigo} en tema claro y oscuro, y comprueba que cada una se muestra
 * sin fallar y con el modo de noche pedido.
 *
 * <p>Verifica: HU-03 (escenarios «Tema» y «Línea gráfica en ambos temas»), RNF-07.
 */
@RunWith(AndroidJUnit4.class)
public class LineaGraficaEspressoTest {

    private static final int COLOR_MATERIA = 0xFFFB8C00;

    private Materia algebra;
    private Materia fisica;

    @Before
    public void conPerfilYMaterias() {
        Pruebas.reiniciarEstado();
        StudyPlanApplication app = Pruebas.app();
        app.configurarPerfil().ejecutar("Ana", 10);
        algebra = app.registrarMateria().ejecutar("Álgebra", COLOR_MATERIA, "Prof. A").valor();
        fisica = app.registrarMateria().ejecutar("Física", 0xFF1E88E5, null).valor();
    }

    @After
    public void restablecer() {
        aplicarTema(Tema.SISTEMA);
        Pruebas.reiniciarEstado();
    }

    @Test
    public void pantallasPrincipalesAbrenEnTemaClaro() throws Exception {
        recorrerPantallas(Tema.CLARO, Configuration.UI_MODE_NIGHT_NO);
    }

    @Test
    public void pantallasPrincipalesAbrenEnTemaOscuro() throws Exception {
        recorrerPantallas(Tema.OSCURO, Configuration.UI_MODE_NIGHT_YES);
    }

    /**
     * El anillo y la etiqueta de la sesión en curso cambian de color con la fase (design.md, decisión 5b):
     * estudiando en {@code colorPrimary} y en pausa en {@code colorWarning}, siempre con su etiqueta de texto.
     */
    @Test
    public void anilloDeSesionEnCursoCambiaDeColorConLaFase() throws Exception {
        for (Tema tema : new Tema[] {Tema.CLARO, Tema.OSCURO}) {
            aplicarTema(tema);
            StudyPlanApplication app = Pruebas.app();
            SesionEstudio pomodoro = app.estudiarAhora().ejecutar(
                    algebra.id(), TecnicaEstudio.POMODORO, PreferenciaNoMolestar.HEREDAR_GLOBAL).valor();
            app.contadorActividades().consumirEvaluacionRecuperacion();
            Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SesionEnCursoActivity.class);
            intent.putExtra(SesionEnCursoActivity.EXTRA_SESION_ID, pomodoro.id());
            try (ActivityScenario<Activity> escenario = ActivityScenario.launch(intent)) {
                Pruebas.esperarVisible(withId(R.id.anilloProgresoEnCurso));
                onView(withId(R.id.textoEstadoEnCurso)).check(matches(withText(R.string.sesion_en_curso_activa)));
                comprobarColorDelAnillo(escenario, com.google.android.material.R.attr.colorPrimary);
                capturar("en_curso_pomodoro", tema);
                onView(withId(R.id.botonDetenerEnCurso)).perform(androidx.test.espresso.action.ViewActions.scrollTo());
                capturar("en_curso_controles", tema);

                tocar(R.id.botonPausarReanudar);
                Pruebas.esperarVisible(withText(R.string.sesion_en_curso_pausada));
                comprobarColorDelAnillo(escenario, R.attr.colorWarning);
                capturar("en_curso_pausa", tema);

                tocar(R.id.botonDetenerEnCurso);
            }
            Pruebas.reiniciarEstado();
            conPerfilYMaterias();
        }
    }

    /** HU-02 «Nombre largo»: el saludo ocupa como máximo dos líneas y no desplaza el resto de Inicio. */
    @Test
    public void saludoConNombreLargoNoRompeInicio() throws Exception {
        StringBuilder largo = new StringBuilder();
        while (largo.length() < 200) {
            largo.append("Nombre muy largo ");
        }
        Pruebas.reiniciarEstado();
        Pruebas.app().configurarPerfil().ejecutar(largo.toString().trim(), 10);
        for (Tema tema : new Tema[] {Tema.CLARO, Tema.OSCURO}) {
            aplicarTema(tema);
            try (ActivityScenario<MainActivity> escenario = ActivityScenario.launch(MainActivity.class)) {
                Pruebas.esperarVisible(withId(R.id.tarjetaProgreso));
                escenario.onActivity(actividad -> {
                    android.widget.TextView saludo = actividad.findViewById(R.id.saludo);
                    assertEquals("El saludo no pasa de dos líneas", true, saludo.getLineCount() <= 2);
                    assertEquals("El texto completo llega al lector de pantalla", true,
                            saludo.getText().toString().contains(largo.toString().trim()));
                });
                capturar("inicio_nombre_largo", tema);
            }
        }
    }

    private static void comprobarColorDelAnillo(ActivityScenario<Activity> escenario, int atributo) {
        escenario.onActivity(actividad -> {
            CircularProgressIndicator anillo = actividad.findViewById(R.id.anilloProgresoEnCurso);
            int esperado = MaterialColors.getColor(anillo, atributo);
            assertEquals("Color del anillo", esperado, anillo.getIndicatorColor()[0]);
        });
    }

    private void recorrerPantallas(Tema tema, int modoNocheEsperado) throws Exception {
        aplicarTema(tema);
        StudyPlanApplication app = Pruebas.app();
        Fecha hoy = app.reloj().hoy();
        android.content.Context contexto = ApplicationProvider.getApplicationContext();

        // Inicio (con una sesión completada y otra pendiente hoy) y selector «Estudiar ahora».
        SesionEstudio hecha = app.planificarSesion().ejecutar(
                algebra.id(), hoy, Hora.de(6, 0).valor(), Hora.de(7, 0).valor(), "").sesion();
        app.completarSesion().ejecutar(hecha.id(), 60);
        app.planificarSesion().ejecutar(
                fisica.id(), hoy.sumarDias(1), Hora.de(9, 0).valor(), Hora.de(10, 0).valor(), "");
        try (ActivityScenario<MainActivity> escenario = ActivityScenario.launch(MainActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.botonNuevaSesion));
            comprobarModoNoche(escenario, modoNocheEsperado);
            capturar("inicio", tema);
            onView(withId(R.id.botonEstudiarAhora)).perform(click());
            Pruebas.esperarVisible(withId(R.id.tituloSelectorEstudio));
            capturar("selector", tema);
            onView(withText("Álgebra")).perform(click());
            capturar("selector_elegida", tema);
        }

        // Estadísticas, Materias y Calendario.
        abrir(EstadisticasActivity.class, R.id.barraEstadisticas, modoNocheEsperado);
        capturarUltima("estadisticas", tema);
        abrir(MateriasActivity.class, R.id.barraSuperiorMaterias, modoNocheEsperado);
        capturarUltima("materias", tema);
        Intent formulario = new Intent(contexto, MateriaFormularioActivity.class);
        formulario.putExtra(MateriaFormularioActivity.EXTRA_ID, algebra.id());
        abrir(formulario, R.id.botonGuardarMateria, modoNocheEsperado);
        capturarUltima("materia_formulario", tema);

        // Pantallas que reciben la línea gráfica por herencia (tarea 5.5): solo abren y se capturan.
        abrir(ReportesActivity.class, R.id.contenidoReportes, modoNocheEsperado);
        capturarUltima("reportes", tema);
        abrir(AjustesActivity.class, R.id.barraSuperior, modoNocheEsperado);
        capturarUltima("ajustes", tema);
        abrir(SesionFormularioActivity.class, R.id.barraSuperior, modoNocheEsperado);
        capturarUltima("sesion_formulario", tema);
        abrir(HistorialMetasActivity.class, R.id.barraSuperior, modoNocheEsperado);
        capturarUltima("historial_metas", tema);
        // Calendario: el bloque horario lleva el color de la materia solo en su franja; el borde es neutro.
        try (ActivityScenario<CalendarioActivity> escenario = ActivityScenario.launch(CalendarioActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.barraCalendario));
            comprobarModoNoche(escenario, modoNocheEsperado);
            Pruebas.esperarVisible(allOf(
                    withId(R.id.tarjetaBloqueCalendario), hasDescendant(withText("Álgebra"))));
            escenario.onActivity(actividad -> {
                com.google.android.material.card.MaterialCardView bloque =
                        actividad.findViewById(R.id.tarjetaBloqueCalendario);
                assertEquals("Borde del bloque horario",
                        MaterialColors.getColor(bloque, com.google.android.material.R.attr.colorOutlineVariant),
                        bloque.getStrokeColorStateList().getDefaultColor());
            });
            capturar("calendario", tema);
        }

        // Detalle de una sesión planificada.
        SesionEstudio planificada = app.planificarSesion().ejecutar(
                algebra.id(), hoy, Hora.de(7, 0).valor(), Hora.de(8, 0).valor(), "").sesion();
        Intent detalle = new Intent(contexto, SesionDetalleActivity.class);
        detalle.putExtra(SesionDetalleActivity.EXTRA_SESION_ID, planificada.id());
        abrir(detalle, R.id.contenedorDetalle, modoNocheEsperado);
        capturarUltima("detalle", tema);

        // Reparto de un tramo entre una sesión en curso y la siguiente.
        SesionEstudio primera = app.planificarSesion().ejecutar(
                algebra.id(), hoy, Hora.de(10, 0).valor(), Hora.de(11, 0).valor(), "").sesion();
        SesionEstudio segunda = app.planificarSesion().ejecutar(
                fisica.id(), hoy, Hora.de(11, 0).valor(), Hora.de(12, 0).valor(), "").sesion();
        SesionEstudio enCurso = primera.iniciar(TecnicaEstudio.LIBRE, null).valor();
        app.sesionRepository().actualizar(enCurso);
        long inicioMs = app.reloj().ahoraMilis() - 90 * 60_000L;
        app.eventoEstudioRepository().guardar(EventoEstudio.crear(
                primera.id(),
                TipoEventoEstudio.INICIO,
                inicioMs,
                app.reloj().fechaDe(inicioMs),
                app.reloj().horaDe(inicioMs),
                "").valor());
        Intent reparto = new Intent(contexto, RepartoSesionActivity.class);
        reparto.putExtra(RepartoSesionActivity.EXTRA_SESION_ACTUAL_ID, primera.id());
        reparto.putExtra(RepartoSesionActivity.EXTRA_SESION_SIGUIENTE_ID, segunda.id());
        abrir(reparto, R.id.botonMas5Min, modoNocheEsperado);
        capturarUltima("reparto", tema);

        // Sesión en curso: la misma sesión iniciada para el reparto (solo puede haber una en curso).
        Intent sesion = new Intent(contexto, SesionEnCursoActivity.class);
        sesion.putExtra(SesionEnCursoActivity.EXTRA_SESION_ID, primera.id());
        // Primero aparece el diálogo «Al volver» (W13) porque la sesión lleva 90 min iniciada;
        // se responde «Sigo estudiando» y queda a la vista el cronómetro.
        try (ActivityScenario<Activity> escenario = ActivityScenario.launch(sesion)) {
            Pruebas.esperarVisible(withId(R.id.grupoOpcionesRecuperacion));
            comprobarModoNoche(escenario, modoNocheEsperado);
            capturar("al_volver", tema);
            tocar(R.id.radioHastaHora);
            capturar("al_volver_hora", tema);
            // Elegir «hasta una hora» muestra el campo de hora y puede abrir el teclado: se cierra para que el
            // siguiente toque encuentre la ventana con foco.
            androidx.test.espresso.Espresso.closeSoftKeyboard();
            tocar(R.id.radioSigoEstudiando);
            onView(withId(android.R.id.button1)).perform(click());
            Pruebas.esperarVisible(withId(R.id.textoCronometro));
            capturar("en_curso", tema);
        }

        // Inicio con la sesión En curso: aviso «Volver a la sesión» (HU-02, decisión 21).
        // Al cerrar la sesión la prueba «sale» de la app con la sesión En curso, así que Inicio pregunta primero
        // «¿seguiste estudiando?» (RN-23); se responde «Sigo estudiando» y queda el aviso de sesión en curso.
        try (ActivityScenario<MainActivity> escenario = ActivityScenario.launch(MainActivity.class)) {
            Pruebas.esperarVisible(withId(R.id.grupoOpcionesRecuperacion));
            onView(withId(R.id.radioSigoEstudiando)).perform(click());
            onView(withId(android.R.id.button1)).perform(click());
            // «Sigo estudiando» abre la sesión en curso; Atrás vuelve a Inicio, que muestra el aviso.
            Pruebas.esperarVisible(withId(R.id.textoCronometro));
            androidx.test.espresso.Espresso.pressBack();
            Pruebas.esperarVisible(withId(R.id.tarjetaSesionEnCurso));
            comprobarModoNoche(escenario, modoNocheEsperado);
            capturar("inicio_en_curso", tema);
        }
    }

    /** Captura tomada con la pantalla abierta por el último {@code abrir(...)}, guardada al pedirla. */
    private static Bitmap ultimaCaptura;

    private static <A extends Activity> void abrir(Class<A> clase, int idVisible, int modoNoche)
            throws InterruptedException {
        try (ActivityScenario<A> escenario = ActivityScenario.launch(clase)) {
            Pruebas.esperarVisible(withId(idVisible));
            comprobarModoNoche(escenario, modoNoche);
            ultimaCaptura = tomarCaptura();
        }
    }

    private static void abrir(Intent intent, int idVisible, int modoNoche)
            throws InterruptedException {
        try (ActivityScenario<Activity> escenario = ActivityScenario.launch(intent)) {
            Pruebas.esperarVisible(withId(idVisible));
            comprobarModoNoche(escenario, modoNoche);
            ultimaCaptura = tomarCaptura();
        }
    }

    /**
     * Capturas opcionales para la revisión visual (tarea 5.4): solo con el argumento de instrumentación
     * {@code capturas=true}. Se guardan en {@code Android/data/<paquete>/files/capturas/}.
     */
    /**
     * Pulsa una vista desplazándose antes hasta ella: con la fuente al 200 % y 360 dp los controles quedan
     * por debajo del borde y Espresso exige que se vea al menos el 90 %. Si la vista no está en un
     * desplazable y ya es visible, el desplazamiento no hace falta.
     */
    private static void tocar(int id) {
        try {
            onView(withId(id)).perform(androidx.test.espresso.action.ViewActions.scrollTo());
        } catch (RuntimeException e) {
            // no está en un desplazable: se intenta el toque directo
        }
        onView(withId(id)).perform(click());
    }

    private static boolean capturasPedidas() {
        return "true".equals(InstrumentationRegistry.getArguments().getString("capturas"));
    }

    private static Bitmap tomarCaptura() {
        if (!capturasPedidas()) {
            return null;
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        return InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
    }

    private static void capturar(String nombre, Tema tema) {
        guardar(tomarCaptura(), nombre, tema);
    }

    private static void capturarUltima(String nombre, Tema tema) {
        guardar(ultimaCaptura, nombre, tema);
        ultimaCaptura = null;
    }

    private static void guardar(Bitmap captura, String nombre, Tema tema) {
        if (captura == null) {
            return;
        }
        File carpeta = new File(
                ApplicationProvider.getApplicationContext().getExternalFilesDir(null), "capturas");
        if (!carpeta.isDirectory() && !carpeta.mkdirs()) {
            return;
        }
        File archivo = new File(carpeta, nombre + "_" + tema.name().toLowerCase(java.util.Locale.ROOT) + ".png");
        try (OutputStream salida = new FileOutputStream(archivo)) {
            captura.compress(Bitmap.CompressFormat.PNG, 100, salida);
        } catch (IOException e) {
            // Las capturas son una ayuda de revisión: su fallo no invalida la prueba de humo.
        }
    }

    private static <A extends Activity> void comprobarModoNoche(
            ActivityScenario<A> escenario, int modoNocheEsperado) {
        escenario.onActivity(actividad -> {
            assertEquals(
                    modoNocheEsperado,
                    actividad.getResources().getConfiguration().uiMode
                            & Configuration.UI_MODE_NIGHT_MASK);
            comprobarAlturaDeBotones(actividad);
        });
    }

    /**
     * Regla del Product Owner (design.md, decisión 17): con la fuente normal, todos los botones con texto visibles
     * miden {@code @dimen/button_height}. Con la fuente ampliada del sistema no se comprueba.
     */
    private static void comprobarAlturaDeBotones(Activity actividad) {
        if (actividad.getResources().getConfiguration().fontScale > 1.0f) {
            return;
        }
        int esperado = actividad.getResources().getDimensionPixelSize(R.dimen.button_height);
        java.util.List<String> distintos = new java.util.ArrayList<>();
        recorrerBotones(actividad.getWindow().getDecorView(), esperado, distintos, actividad);
        if (!distintos.isEmpty()) {
            throw new AssertionError("Botones con otra altura que button_height (" + esperado + " px): "
                    + String.join(", ", distintos));
        }
    }

    private static void recorrerBotones(
            android.view.View vista, int esperado, java.util.List<String> distintos, Activity actividad) {
        if (vista instanceof com.google.android.material.button.MaterialButton) {
            com.google.android.material.button.MaterialButton boton =
                    (com.google.android.material.button.MaterialButton) vista;
            boolean conTexto = boton.getText() != null && boton.getText().length() > 0;
            if (boton.isShown() && conTexto && boton.getHeight() > 0 && boton.getHeight() != esperado) {
                String nombre = boton.getId() == android.view.View.NO_ID
                        ? boton.getText().toString()
                        : actividad.getResources().getResourceEntryName(boton.getId());
                distintos.add(nombre + "=" + boton.getHeight() + " px");
            }
        }
        if (vista instanceof android.view.ViewGroup) {
            android.view.ViewGroup grupo = (android.view.ViewGroup) vista;
            for (int i = 0; i < grupo.getChildCount(); i++) {
                recorrerBotones(grupo.getChildAt(i), esperado, distintos, actividad);
            }
        }
    }

    private static void aplicarTema(Tema tema) {
        Pruebas.app().configurarApariencia().cambiarTema(tema);
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() -> AplicadorApariencia.aplicarTema(tema));
    }
}
