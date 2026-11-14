package io.github.jhoel0521.studyplan.adapter.in.ui;

import java.util.function.Consumer;

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
import io.github.jhoel0521.studyplan.domain.Materia;

/**
 * Adaptador de la lista de materias: filas de materias activas, encabezado plegable de archivadas y filas de
 * archivadas (atenuadas y sin horas).
 *
 * <p>Implementa: HU-09 (lista con color, nombre, docente y horas) y HU-08 (archivadas).
 */
public class MateriasAdapter extends ListAdapter<FilaMateria, RecyclerView.ViewHolder> {

    private static final int TIPO_MATERIA = 0;
    private static final int TIPO_ENCABEZADO = 1;

    private static final DiffUtil.ItemCallback<FilaMateria> COMPARADOR = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull FilaMateria a, @NonNull FilaMateria b) {
            return a.tipo() == b.tipo() && a.idEstable() == b.idEstable();
        }

        @Override
        public boolean areContentsTheSame(@NonNull FilaMateria a, @NonNull FilaMateria b) {
            return a.equals(b);
        }
    };

    private final Consumer<Materia> alTocarMateria;
    private final Runnable alTocarEncabezado;

    public MateriasAdapter(Consumer<Materia> alTocarMateria, Runnable alTocarEncabezado) {
        super(COMPARADOR);
        this.alTocarMateria = alTocarMateria;
        this.alTocarEncabezado = alTocarEncabezado;
    }

    @Override
    public int getItemViewType(int posicion) {
        return getItem(posicion).tipo() == FilaMateria.Tipo.ENCABEZADO_ARCHIVADAS ? TIPO_ENCABEZADO : TIPO_MATERIA;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        LayoutInflater inflador = LayoutInflater.from(padre.getContext());
        if (tipo == TIPO_ENCABEZADO) {
            return new EncabezadoHolder(inflador.inflate(R.layout.item_encabezado_archivadas, padre, false));
        }
        return new MateriaHolder(inflador.inflate(R.layout.item_materia, padre, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int posicion) {
        FilaMateria fila = getItem(posicion);
        if (holder instanceof EncabezadoHolder) {
            ((EncabezadoHolder) holder).enlazar(fila);
        } else {
            ((MateriaHolder) holder).enlazar(fila);
        }
    }

    /** Fila de una materia, activa o archivada. */
    private final class MateriaHolder extends RecyclerView.ViewHolder {
        private final ImageView color;
        private final TextView nombre;
        private final TextView docente;
        private final TextView horas;

        MateriaHolder(View vista) {
            super(vista);
            color = vista.findViewById(R.id.colorMateria);
            nombre = vista.findViewById(R.id.nombreMateria);
            docente = vista.findViewById(R.id.docenteMateria);
            horas = vista.findViewById(R.id.horasMateria);
        }

        void enlazar(FilaMateria fila) {
            Materia materia = fila.materia();
            boolean activa = fila.tipo() == FilaMateria.Tipo.ACTIVA;
            color.setColorFilter(materia.color());
            nombre.setText(materia.nombre());
            docente.setText(materia.docente());
            docente.setVisibility(materia.docente() == null ? View.GONE : View.VISIBLE);
            horas.setVisibility(activa ? View.VISIBLE : View.GONE);
            horas.setText(itemView.getContext().getString(R.string.materias_horas,
                    FormatoHoras.texto(fila.horasRealizadas())));
            itemView.setAlpha(activa ? 1f : 0.6f);
            itemView.setOnClickListener(this::onMateriaClick);
        }

        private void onMateriaClick(View vista) {
            int posicion = getAdapterPosition();
            if (posicion != RecyclerView.NO_POSITION) {
                alTocarMateria.accept(getItem(posicion).materia());
            }
        }
    }

    /** Encabezado «Archivadas (n)» que despliega o pliega la sección. */
    private final class EncabezadoHolder extends RecyclerView.ViewHolder {
        private final TextView titulo;
        private final TextView indicador;

        EncabezadoHolder(View vista) {
            super(vista);
            titulo = vista.findViewById(R.id.tituloArchivadas);
            indicador = vista.findViewById(R.id.indicadorArchivadas);
        }

        void enlazar(FilaMateria fila) {
            titulo.setText(itemView.getContext().getString(R.string.materias_archivadas, fila.cantidadArchivadas()));
            indicador.setText(fila.expandida() ? R.string.indicador_desplegado : R.string.indicador_plegado);
            itemView.setContentDescription(itemView.getContext().getString(
                    fila.expandida() ? R.string.materias_archivadas_desplegadas : R.string.materias_archivadas_plegadas,
                    fila.cantidadArchivadas()));
            itemView.setOnClickListener(this::onEncabezadoClick);
        }

        private void onEncabezadoClick(View vista) {
            alTocarEncabezado.run();
        }
    }
}
