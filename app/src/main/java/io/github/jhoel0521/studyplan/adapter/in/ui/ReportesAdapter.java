package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.readmodel.DesgloseMateriaPeriodo;

/**
 * Adaptador de filas por materia del reporte tabular de estudio ({@code HU-32}, {@code RNF-06}).
 */
final class ReportesAdapter extends RecyclerView.Adapter<ReportesAdapter.Vista> {

    private final List<DesgloseMateriaPeriodo> elementos = new ArrayList<>();

    void actualizar(List<DesgloseMateriaPeriodo> nuevos) {
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
                .inflate(R.layout.item_reporte_materia, parent, false);
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
        private final TextView resumen;

        Vista(@NonNull View itemView) {
            super(itemView);
            indicadorColor = itemView.findViewById(R.id.indicadorColorMateriaReporte);
            nombre = itemView.findViewById(R.id.textoNombreMateriaReporte);
            resumen = itemView.findViewById(R.id.textoResumenMateriaReporte);
        }

        void enlazar(DesgloseMateriaPeriodo item) {
            Context ctx = itemView.getContext();
            AcentoMateria.punto(indicadorColor, item.colorMateria());
            String etiquetaNombre = EstadisticasAdapter.nombreVisible(ctx, item);
            nombre.setText(etiquetaNombre);

            String horasPlan = String.format(
                    Locale.getDefault(), "%.1f", item.minutosPlanificadosDeSesiones() / 60.0);
            String horasReal = String.format(Locale.getDefault(), "%.1f", item.horasReales());
            String cumplimiento = formatearCumplimiento(ctx, item.cumplimientoPlanificado());

            resumen.setText(ctx.getString(
                    R.string.reportes_fila_resumen,
                    horasPlan,
                    horasReal,
                    cumplimiento,
                    item.sesionesNoRealizadas()));
            itemView.setContentDescription(ctx.getString(
                    R.string.reportes_fila_cd,
                    etiquetaNombre,
                    horasPlan,
                    horasReal,
                    cumplimiento,
                    item.sesionesNoRealizadas()));
        }

        private static String formatearCumplimiento(Context ctx, Double valor) {
            if (valor == null) {
                return ctx.getString(R.string.estadisticas_sin_datos_metrica);
            }
            return ctx.getString(R.string.reportes_cumplimiento_porcentaje, Math.round(valor));
        }
    }
}
