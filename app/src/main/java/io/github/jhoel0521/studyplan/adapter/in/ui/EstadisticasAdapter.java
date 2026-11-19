package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.DesgloseMateriaPeriodo;
import io.github.jhoel0521.studyplan.domain.ResumenTopico;

/**
 * Adaptadores de listas accesibles para la pantalla de Estadísticas ({@code HU-30}, {@code HU-45},
 * {@code RNF-06}).
 */
public final class EstadisticasAdapter {

    private EstadisticasAdapter() {
    }

    public static final class MateriasAdapter
            extends RecyclerView.Adapter<MateriasAdapter.Vista> {

        private final List<DesgloseMateriaPeriodo> elementos = new ArrayList<>();

        public void actualizar(List<DesgloseMateriaPeriodo> nuevos) {
            elementos.clear();
            if (nuevos != null) {
                elementos.addAll(nuevos);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Vista onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_estadistica_materia, parent, false);
            return new Vista(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Vista holder, int position) {
            holder.enlazar(elementos.get(position));
        }

        @Override
        public int getItemCount() {
            return elementos.size();
        }

        static final class Vista extends RecyclerView.ViewHolder {
            private final ImageView indicadorColor;
            private final TextView nombre;
            private final TextView detalle;

            Vista(@NonNull View itemView) {
                super(itemView);
                indicadorColor = itemView.findViewById(R.id.indicadorColorMateriaEstadistica);
                nombre = itemView.findViewById(R.id.textoNombreMateriaEstadistica);
                detalle = itemView.findViewById(R.id.textoDetalleMateriaEstadistica);
            }

            void enlazar(DesgloseMateriaPeriodo item) {
                Context ctx = itemView.getContext();
                ImageViewCompat.setImageTintList(
                        indicadorColor, ColorStateList.valueOf(item.colorMateria()));
                String etiquetaNombre = nombreVisible(ctx, item);
                nombre.setText(etiquetaNombre);
                String horas = String.format(Locale.getDefault(), "%.1f", item.horasReales());
                int porcentaje = (int) Math.round(item.porcentajeTiempo());
                detalle.setText(String.format(
                        Locale.getDefault(), "%s h · %d %%", horas, porcentaje));
                itemView.setContentDescription(ctx.getString(
                        R.string.estadisticas_materia_fila,
                        etiquetaNombre,
                        horas,
                        porcentaje));
            }
        }
    }

    static String nombreVisible(Context ctx, DesgloseMateriaPeriodo item) {
        String etiquetaNombre = item.esGeneral()
                ? ctx.getString(R.string.materia_general_nombre)
                : item.nombreMateria();
        if (item.archivada()) {
            etiquetaNombre = etiquetaNombre
                    + " (" + ctx.getString(R.string.estadisticas_materia_archivada) + ")";
        }
        return etiquetaNombre;
    }

    public static final class TopicosAdapter
            extends RecyclerView.Adapter<TopicosAdapter.Vista> {

        private final List<ResumenTopico> elementos = new ArrayList<>();

        public void actualizar(List<ResumenTopico> nuevos) {
            elementos.clear();
            if (nuevos != null) {
                elementos.addAll(nuevos);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Vista onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_estadistica_topico, parent, false);
            return new Vista(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Vista holder, int position) {
            holder.enlazar(elementos.get(position));
        }

        @Override
        public int getItemCount() {
            return elementos.size();
        }

        static final class Vista extends RecyclerView.ViewHolder {
            private final TextView nombre;
            private final TextView detalle;

            Vista(@NonNull View itemView) {
                super(itemView);
                nombre = itemView.findViewById(R.id.textoNombreTopicoEstadistica);
                detalle = itemView.findViewById(R.id.textoDetalleTopicoEstadistica);
            }

            void enlazar(ResumenTopico item) {
                Context ctx = itemView.getContext();
                String etiqueta = TopicosUi.etiqueta(ctx, item.topico());
                nombre.setText(etiqueta);
                detalle.setText(String.format(
                        Locale.getDefault(), "%d · %d min", item.cantidad(), item.minutos()));
                itemView.setContentDescription(ctx.getString(
                        R.string.estadisticas_topico_fila,
                        etiqueta,
                        item.cantidad(),
                        item.minutos()));
            }
        }
    }
}
