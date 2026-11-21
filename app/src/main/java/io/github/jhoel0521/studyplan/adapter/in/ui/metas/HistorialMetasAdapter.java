package io.github.jhoel0521.studyplan.adapter.in.ui.metas;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Objects;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.readmodel.ProgresoMeta;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.FormatoHoras;

/**
 * Adaptador para la lista cronológica inversa de metas semanales ({@code HU-27}, {@code RN-33}).
 */
public class HistorialMetasAdapter extends ListAdapter<ProgresoMeta, HistorialMetasAdapter.Holder> {

    private static final DiffUtil.ItemCallback<ProgresoMeta> COMPARADOR =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull ProgresoMeta a, @NonNull ProgresoMeta b) {
                    return a.semanaInicio().equals(b.semanaInicio())
                            && Objects.equals(a.materiaId(), b.materiaId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull ProgresoMeta a, @NonNull ProgresoMeta b) {
                    return Double.compare(a.horasRealizadas(), b.horasRealizadas()) == 0
                            && Double.compare(a.horasObjetivo(), b.horasObjetivo()) == 0
                            && a.estimada() == b.estimada();
                }
            };

    public HistorialMetasAdapter() {
        super(COMPARADOR);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historial_meta, parent, false);
        return new Holder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.enlazar(getItem(position));
    }

    public static final class Holder extends RecyclerView.ViewHolder {

        private final TextView tituloSemana;
        private final TextView subtituloMeta;
        private final TextView etiquetaEstado;
        private final LinearProgressIndicator barraProgreso;
        private final TextView textoProgreso;

        Holder(View vista) {
            super(vista);
            tituloSemana = vista.findViewById(R.id.tituloSemanaHistorial);
            subtituloMeta = vista.findViewById(R.id.subtituloMetaHistorial);
            etiquetaEstado = vista.findViewById(R.id.etiquetaEstadoHistorial);
            barraProgreso = vista.findViewById(R.id.barraHistorialMeta);
            textoProgreso = vista.findViewById(R.id.textoProgresoHistorial);
        }

        void enlazar(ProgresoMeta item) {
            Context ctx = itemView.getContext();
            Fecha lunes = item.semanaInicio();
            Fecha domingo = lunes.domingoDeSemana();
            tituloSemana.setText(ctx.getString(
                    R.string.historial_semana_rango, lunes.iso(), domingo.iso()));
            subtituloMeta.setText(item.esMetaTotal()
                    ? ctx.getString(R.string.historial_meta_total_titulo)
                    : item.nombreMateria());

            if (item.estimada()) {
                etiquetaEstado.setText(R.string.historial_meta_estimada);
            } else if (item.cumplida()) {
                etiquetaEstado.setText(R.string.historial_meta_cumplida);
            } else {
                etiquetaEstado.setText(R.string.historial_meta_en_progreso);
            }

            barraProgreso.setProgressCompat(item.porcentaje(), false);
            String realTxt = FormatoHoras.texto(item.horasRealizadas());
            String objTxt = FormatoHoras.texto(item.horasObjetivo());
            textoProgreso.setText(ctx.getString(R.string.materia_meta_progreso, realTxt, objTxt));
        }
    }
}
