package io.github.jhoel0521.studyplan.adapter.in.ui.configuracion;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.github.jhoel0521.studyplan.domain.readmodel.DetalleSesionInicio;
import io.github.jhoel0521.studyplan.domain.readmodel.ResumenInicio;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;

/**
 * Qué muestra el widget de progreso semanal 1×4: el estado, el porcentaje y las próximas sesiones (hasta 2).
 *
 * <p>Es Java puro (sin {@code android.*}) para poder probarlo en la JVM; los textos con formato los pone
 * {@link WidgetProgresoProvider} desde los recursos.
 *
 * <p>Implementa: HU-05 (RNF-19).
 */
public final class ModeloWidgetProgreso {

    /** Situación que decide el contenido del widget. */
    public enum Estado {
        /** Hay meta y próxima sesión. */
        NORMAL,
        /** Todavía no hay perfil: se invita a abrir la app. */
        SIN_PERFIL,
        /** El perfil no tiene meta semanal. */
        SIN_META,
        /** Hay meta pero no hay próximas sesiones. */
        SIN_SESION
    }

    private final Estado estado;
    private final int porcentaje;
    private final double horasRealizadas;
    private final double metaHoras;
    private final List<DetalleSesionInicio> proximas;
    private final Fecha hoy;

    private ModeloWidgetProgreso(
            Estado estado,
            int porcentaje,
            double horasRealizadas,
            double metaHoras,
            List<DetalleSesionInicio> proximas,
            Fecha hoy) {
        this.estado = estado;
        this.porcentaje = porcentaje;
        this.horasRealizadas = horasRealizadas;
        this.metaHoras = metaHoras;
        this.proximas = proximas;
        this.hoy = hoy;
    }

    /** @param resumen el resumen de Inicio, o vacío si no hay perfil */
    public static ModeloWidgetProgreso de(Optional<ResumenInicio> resumen) {
        if (resumen == null || !resumen.isPresent()) {
            return new ModeloWidgetProgreso(Estado.SIN_PERFIL, 0, 0, 0, Collections.emptyList(), null);
        }
        ResumenInicio r = resumen.get();
        List<DetalleSesionInicio> proximas = r.proximasSesiones();
        Estado estado;
        if (r.metaSemanalHoras() <= 0) {
            estado = Estado.SIN_META;
        } else if (proximas.isEmpty()) {
            estado = Estado.SIN_SESION;
        } else {
            estado = Estado.NORMAL;
        }
        return new ModeloWidgetProgreso(
                estado, r.porcentajeProgreso(), r.horasRealizadas(), r.metaSemanalHoras(), proximas, r.hoy());
    }

    public Estado estado() {
        return estado;
    }

    /** @return el avance entre 0 y 100 (0 si no hay meta) */
    public int porcentaje() {
        return porcentaje;
    }

    public double horasRealizadas() {
        return horasRealizadas;
    }

    public double metaHoras() {
        return metaHoras;
    }

    /** @return {@code true} si hay perfil y meta, así que el anillo y «horas / meta» tienen sentido */
    public boolean hayProgreso() {
        return estado == Estado.NORMAL || estado == Estado.SIN_SESION;
    }

    /** @return el día de hoy según el resumen, o {@code null} si no lo trae */
    public Fecha hoy() {
        return hoy;
    }

    /** @return las próximas sesiones en orden (0, 1 o 2); vacía si no hay o si no hay perfil */
    public List<DetalleSesionInicio> proximas() {
        return proximas;
    }
}
