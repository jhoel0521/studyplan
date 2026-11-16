package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.TipoEventoEstudio;

/**
 * Adaptador de la línea de tiempo de {@link EventoEstudio} en {@link SesionDetalleActivity}
 * (HU-39, W15).
 */
public class EventoEstudioAdapter extends ListAdapter<EventoEstudio, EventoEstudioAdapter.EventoHolder> {

    private static final DiffUtil.ItemCallback<EventoEstudio> COMPARADOR =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull EventoEstudio a, @NonNull EventoEstudio b) {
                    return a.instanteMilis() == b.instanteMilis() && a.tipo() == b.tipo();
                }

                @Override
                public boolean areContentsTheSame(@NonNull EventoEstudio a, @NonNull EventoEstudio b) {
                    return a.equals(b);
                }
            };

    public EventoEstudioAdapter() {
        super(COMPARADOR);
    }

    @NonNull
    @Override
    public EventoHolder onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        View v = LayoutInflater.from(padre.getContext())
                .inflate(R.layout.item_evento_estudio, padre, false);
        return new EventoHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EventoHolder holder, int posicion) {
        holder.enlazar(getItem(posicion));
    }

    static final class EventoHolder extends RecyclerView.ViewHolder {

        private final TextView textoHora;
        private final TextView textoTipo;
        private final TextView textoNota;

        EventoHolder(@NonNull View itemView) {
            super(itemView);
            textoHora = itemView.findViewById(R.id.textoHoraEvento);
            textoTipo = itemView.findViewById(R.id.textoTipoEvento);
            textoNota = itemView.findViewById(R.id.textoNotaEvento);
        }

        void enlazar(EventoEstudio evento) {
            textoHora.setText(FormatoSesion.horaEventoConZona(
                    evento, FormatoSesion.desfaseActualSistema()));
            textoTipo.setText(etiquetaDe(evento.tipo()));
            if (evento.nota() == null || evento.nota().isEmpty()) {
                textoNota.setVisibility(View.GONE);
            } else {
                textoNota.setText(evento.nota());
                textoNota.setVisibility(View.VISIBLE);
            }
        }

        @StringRes
        private static int etiquetaDe(TipoEventoEstudio tipo) {
            switch (tipo) {
                case INICIO:
                    return R.string.evento_inicio;
                case PAUSA:
                    return R.string.evento_pausa;
                case REANUDACION:
                    return R.string.evento_reanudacion;
                case SALIDA_APP:
                    return R.string.evento_salida_app;
                case REGRESO_APP:
                    return R.string.evento_regreso_app;
                case FIN_PLANIFICADO:
                    return R.string.evento_fin_planificado;
                case TIEMPO_EXTRA:
                    return R.string.evento_tiempo_extra;
                case ESTUDIO_APP_CERRADA:
                    return R.string.evento_estudio_app_cerrada;
                case CAMBIO_SESION:
                    return R.string.evento_cambio_sesion;
                case AJUSTE_MANUAL:
                    return R.string.evento_ajuste_manual;
                case FIN:
                default:
                    return R.string.evento_fin;
            }
        }
    }
}
