package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import io.github.jhoel0521.studyplan.R;

/**
 * Clase base de todas las Activities: dibuja de borde a borde respetando las barras del sistema y da acceso a la
 * raíz de composición y a los ViewModel.
 *
 * <p>Implementa: HU-01 a HU-04 y HU-38 (comportamiento común de las pantallas). El layout de cada Activity usa
 * {@code R.id.main} como vista raíz.
 */
public abstract class ActividadBase extends AppCompatActivity {

    /** Conserva el manejo de insets de la plantilla: el relleno de la raíz deja libres las barras del sistema. */
    protected void aplicarInsets() {
        EdgeToEdge.enable(this);
        View raiz = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(raiz, this::onInsets);
    }

    private WindowInsetsCompat onInsets(View vista, WindowInsetsCompat insets) {
        Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
        // Las pantallas con barra de navegación (inferior o riel) la dejan reservar su propio espacio bajo los botones
        // del sistema: si además se rellenara aquí, quedaría una franja en blanco doble.
        int abajo = vista.findViewById(R.id.barraInferior) != null ? 0 : barras.bottom;
        vista.setPadding(barras.left, barras.top, barras.right, abajo);
        return insets;
    }

    protected StudyPlanApplication app() {
        return StudyPlanApplication.de(this);
    }

    protected <T extends androidx.lifecycle.ViewModel> T viewModel(Class<T> clase) {
        return new ViewModelProvider(this, app().fabrica()).get(clase);
    }
}
