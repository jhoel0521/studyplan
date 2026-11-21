package io.github.jhoel0521.studyplan.adapter.in.ui.calendario;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import java.util.List;
import java.util.Locale;

import io.github.jhoel0521.studyplan.R;
import io.github.jhoel0521.studyplan.domain.readmodel.CeldaDiaMes;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;

/**
 * Adaptador de la cuadrícula mensual de 7 columnas (lunes a domingo) con indicadores de color de
 * materias, suma compacta de horas y soporte de toque corto y pulsación larga (HU-16, HU-20, RN-13).
 */
public class CalendarioMesAdapter extends ListAdapter<CeldaDiaMes, CalendarioMesAdapter.CeldaHolder> {

    public interface AccionesCelda {
        void alTocarDia(Fecha fecha);

        void alMantenerPulsadoDia(Fecha fecha);
    }

    private static final int MAX_PUNTOS_COLOR = 4;

    private static final DiffUtil.ItemCallback<CeldaDiaMes> COMPARADOR =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull CeldaDiaMes a, @NonNull CeldaDiaMes b) {
                    return a.fecha().equals(b.fecha());
                }

                @Override
                public boolean areContentsTheSame(@NonNull CeldaDiaMes a, @NonNull CeldaDiaMes b) {
                    return a.delMesActual() == b.delMesActual()
                            && a.esHoy() == b.esHoy()
                            && a.esSeleccionado() == b.esSeleccionado()
                            && a.minutosTotalesDia() == b.minutosTotalesDia()
                            && a.cantidadSesiones() == b.cantidadSesiones()
                            && a.coloresMaterias().equals(b.coloresMaterias());
                }
            };

    private final AccionesCelda acciones;

    public CalendarioMesAdapter(AccionesCelda acciones) {
        super(COMPARADOR);
        this.acciones = acciones;
    }

    @NonNull
    @Override
    public CeldaHolder onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        View vista = LayoutInflater.from(padre.getContext())
                .inflate(R.layout.item_celda_mes, padre, false);
        return new CeldaHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull CeldaHolder holder, int posicion) {
        holder.enlazar(getItem(posicion), acciones);
    }

    public static final class CeldaHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView tarjeta;
        private final TextView textoNumero;
        private final LinearLayout contenedorPuntos;
        private final TextView textoHoras;

        CeldaHolder(@NonNull View itemView) {
            super(itemView);
            tarjeta = itemView.findViewById(R.id.tarjetaCeldaMes);
            textoNumero = itemView.findViewById(R.id.textoNumeroDiaMes);
            contenedorPuntos = itemView.findViewById(R.id.contenedorPuntosColorMes);
            textoHoras = itemView.findViewById(R.id.textoHorasDiaMes);
        }

        void enlazar(CeldaDiaMes celda, AccionesCelda acciones) {
            Context ctx = itemView.getContext();
            textoNumero.setText(String.valueOf(celda.fecha().dia()));
            tarjeta.setAlpha(celda.delMesActual() ? 1.0f : 0.45f);

            int colorFondo = celda.esSeleccionado()
                    ? MaterialColors.getColor(tarjeta, com.google.android.material.R.attr.colorPrimaryContainer)
                    : MaterialColors.getColor(tarjeta, com.google.android.material.R.attr.colorSurface);
            int colorBorde = celda.esHoy()
                    ? MaterialColors.getColor(tarjeta, com.google.android.material.R.attr.colorPrimary)
                    : MaterialColors.getColor(tarjeta, com.google.android.material.R.attr.colorOutlineVariant);

            tarjeta.setCardBackgroundColor(colorFondo);
            tarjeta.setStrokeColor(colorBorde);
            tarjeta.setStrokeWidth(celda.esHoy() ? dp(ctx, 2) : dp(ctx, 1));

            contenedorPuntos.removeAllViews();
            List<Integer> colores = celda.coloresMaterias();
            int limite = Math.min(colores.size(), MAX_PUNTOS_COLOR);
            int tamPunto = dp(ctx, 6);
            int margen = dp(ctx, 1);
            for (int i = 0; i < limite; i++) {
                ImageView punto = new ImageView(ctx);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tamPunto, tamPunto);
                lp.setMargins(margen, 0, margen, 0);
                punto.setLayoutParams(lp);
                punto.setImageResource(R.drawable.ic_circulo);
                punto.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                ImageViewCompat.setImageTintList(punto, ColorStateList.valueOf(colores.get(i)));
                contenedorPuntos.addView(punto);
            }
            if (colores.size() > MAX_PUNTOS_COLOR) {
                TextView mas = new TextView(ctx);
                mas.setText(ctx.getString(R.string.calendario_mas_materias, colores.size() - MAX_PUNTOS_COLOR));
                mas.setTextSize(9f);
                contenedorPuntos.addView(mas);
            }

            if (celda.minutosTotalesDia() > 0) {
                String horasTexto = ctx.getString(
                        R.string.calendario_horas_compacto,
                        String.format(Locale.getDefault(), "%.1f", celda.minutosTotalesDia() / 60.0));
                textoHoras.setText(horasTexto);
                textoHoras.setVisibility(View.VISIBLE);
                tarjeta.setContentDescription(ctx.getString(
                        R.string.calendario_celda_desc,
                        celda.fecha().iso(),
                        celda.cantidadSesiones(),
                        horasTexto));
            } else {
                textoHoras.setText("");
                textoHoras.setVisibility(View.INVISIBLE);
                tarjeta.setContentDescription(ctx.getString(
                        R.string.calendario_celda_vacia_desc, celda.fecha().iso()));
            }

            tarjeta.setOnClickListener(v -> acciones.alTocarDia(celda.fecha()));
            tarjeta.setOnLongClickListener(v -> {
                acciones.alMantenerPulsadoDia(celda.fecha());
                return true;
            });
        }

        private static int dp(Context ctx, int valor) {
            return Math.round(valor * ctx.getResources().getDisplayMetrics().density);
        }
    }
}
