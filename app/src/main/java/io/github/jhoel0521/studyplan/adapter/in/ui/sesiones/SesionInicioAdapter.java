package io.github.jhoel0521.studyplan.adapter.in.ui.sesiones;

import java.util.function.Consumer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.readmodel.DetalleSesionInicio;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.AcentoMateria;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.EstadoSesionVisual;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.FormatoSesion;

/**
 * Adaptador de la lista de sesiones del día en la pantalla de Inicio, mostrando color de materia, horario,
 * duración e icono + etiqueta textual de estado (RNF-06).
 *
 * <p>Implementa: HU-02 y RNF-06.
 */
public class SesionInicioAdapter extends ListAdapter<DetalleSesionInicio, SesionInicioAdapter.SesionHolder> {

    private static final DiffUtil.ItemCallback<DetalleSesionInicio> COMPARADOR =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull DetalleSesionInicio a, @NonNull DetalleSesionInicio b) {
                    return a.sesionId() == b.sesionId();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull DetalleSesionInicio a, @NonNull DetalleSesionInicio b) {
                    return a.equals(b);
                }
            };

    private final Consumer<DetalleSesionInicio> alTocarSesion;

    public SesionInicioAdapter(Consumer<DetalleSesionInicio> alTocarSesion) {
        super(COMPARADOR);
        this.alTocarSesion = alTocarSesion;
    }

    @NonNull
    @Override
    public SesionHolder onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        View vista = LayoutInflater.from(padre.getContext())
                .inflate(R.layout.item_sesion_inicio, padre, false);
        return new SesionHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull SesionHolder holder, int posicion) {
        holder.enlazar(getItem(posicion));
    }

    /** Fila de sesión en Inicio. */
    final class SesionHolder extends RecyclerView.ViewHolder {
        private final View colorMateria;
        private final TextView nombreMateria;
        private final TextView horarioSesion;
        private final ImageView iconoEstado;
        private final TextView etiquetaEstado;

        SesionHolder(View vista) {
            super(vista);
            colorMateria = vista.findViewById(R.id.colorMateriaItem);
            nombreMateria = vista.findViewById(R.id.nombreMateriaItem);
            horarioSesion = vista.findViewById(R.id.horarioSesionItem);
            iconoEstado = vista.findViewById(R.id.iconoEstadoItem);
            etiquetaEstado = vista.findViewById(R.id.etiquetaEstadoItem);
        }

        void enlazar(DetalleSesionInicio sesion) {
            Context contexto = itemView.getContext();
            AcentoMateria.barra(colorMateria, sesion.colorMateria());
            nombreMateria.setText(sesion.nombreMateria());
            horarioSesion.setText(FormatoSesion.rangoYDuracion(contexto, sesion, false));
            EstadoSesionVisual.aplicar(sesion.estado(), iconoEstado, etiquetaEstado);
            itemView.setOnClickListener(this::onSesionClick);
            AcentoMateria.comoBoton(itemView);
        }

        private void onSesionClick(View vista) {
            int posicion = getAdapterPosition();
            if (posicion != RecyclerView.NO_POSITION) {
                alTocarSesion.accept(getItem(posicion));
            }
        }
    }
}
