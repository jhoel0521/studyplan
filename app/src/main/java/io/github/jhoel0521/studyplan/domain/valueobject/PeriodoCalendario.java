package io.github.jhoel0521.studyplan.domain.valueobject;

import java.util.Objects;
import io.github.jhoel0521.studyplan.domain.enumeration.VistaCalendario;

/**
 * Value Object inmutable que representa el periodo activo, la vista y la fecha de foco
 * en el calendario de estudio (HU-16, HU-17, HU-18, RN-13).
 *
 * <p>Preserva el periodo y el día seleccionado al navegar y alternar entre {@code Día},
 * {@code Semana} y {@code Mes}.
 */
public final class PeriodoCalendario {

    private final VistaCalendario vista;
    private final Fecha fechaFoco;
    private final Fecha diaSeleccionado;

    private PeriodoCalendario(VistaCalendario vista, Fecha fechaFoco, Fecha diaSeleccionado) {
        this.vista = Objects.requireNonNull(vista, "vista");
        this.fechaFoco = Objects.requireNonNull(fechaFoco, "fechaFoco");
        this.diaSeleccionado = diaSeleccionado;
    }

    /** Periodo inicial por defecto en vista {@link VistaCalendario#SEMANA} centrado en {@code hoy}. */
    public static PeriodoCalendario inicial(Fecha hoy) {
        Objects.requireNonNull(hoy, "hoy");
        return new PeriodoCalendario(VistaCalendario.POR_DEFECTO, hoy, hoy);
    }

    /** Construye un periodo explícito. */
    public static PeriodoCalendario de(
            VistaCalendario vista, Fecha fechaFoco, Fecha diaSeleccionado) {
        return new PeriodoCalendario(vista, fechaFoco, diaSeleccionado);
    }

    public VistaCalendario vista() {
        return vista;
    }

    public Fecha fechaFoco() {
        return fechaFoco;
    }

    public Fecha diaSeleccionado() {
        return diaSeleccionado;
    }

    /** Primer día lógico del periodo visible (para resumen {@code Plan / Real}). */
    public Fecha rangoVisibleInicio() {
        switch (vista) {
            case DIA:
                return fechaFoco;
            case SEMANA:
                return fechaFoco.lunesDeSemana();
            case MES:
                return Fecha.de(fechaFoco.anio(), fechaFoco.mes(), 1).valor();
        }
        throw new AssertionError("Vista no soportada: " + vista);
    }

    /** Último día lógico del periodo visible (para resumen {@code Plan / Real}). */
    public Fecha rangoVisibleFin() {
        switch (vista) {
            case DIA:
                return fechaFoco;
            case SEMANA:
                return fechaFoco.domingoDeSemana();
            case MES:
                int ultimo = Fecha.diasDelMes(fechaFoco.anio(), fechaFoco.mes());
                return Fecha.de(fechaFoco.anio(), fechaFoco.mes(), ultimo).valor();
        }
        throw new AssertionError("Vista no soportada: " + vista);
    }

    /**
     * Primer día de consulta en base de datos; en {@code MES} incluye los días iniciales de la
     * primera fila lunes–domingo (RN-13).
     */
    public Fecha rangoConsultaInicio() {
        Fecha inicioVisible = rangoVisibleInicio();
        if (vista == VistaCalendario.MES) {
            return inicioVisible.lunesDeSemana();
        }
        return inicioVisible;
    }

    /**
     * Último día de consulta en base de datos; en {@code MES} incluye los días finales de la
     * última fila lunes–domingo (RN-13).
     */
    public Fecha rangoConsultaFin() {
        Fecha finVisible = rangoVisibleFin();
        if (vista == VistaCalendario.MES) {
            return finVisible.domingoDeSemana();
        }
        return finVisible;
    }

    public boolean perteneceAMesVisible(Fecha fecha) {
        return fecha != null
                && fecha.anio() == fechaFoco.anio()
                && fecha.mes() == fechaFoco.mes();
    }

    public boolean perteneceASemanaVisible(Fecha fecha) {
        if (fecha == null) {
            return false;
        }
        Fecha lunes = fechaFoco.lunesDeSemana();
        Fecha domingo = fechaFoco.domingoDeSemana();
        return !fecha.esAnteriorA(lunes) && !fecha.esPosteriorA(domingo);
    }

    /** Retrocede un día, una semana o un mes calendario según la vista activa. */
    public PeriodoCalendario anterior() {
        return desplazar(-1);
    }

    /** Avanza un día, una semana o un mes calendario según la vista activa. */
    public PeriodoCalendario siguiente() {
        return desplazar(1);
    }

    /** Selecciona un día concreto (en vista {@code DIA} también actualiza la fecha foco). */
    public PeriodoCalendario seleccionarDia(Fecha fecha) {
        Objects.requireNonNull(fecha, "fecha");
        if (vista == VistaCalendario.DIA) {
            return new PeriodoCalendario(vista, fecha, fecha);
        }
        return new PeriodoCalendario(vista, fechaFoco, fecha);
    }

    /** Vuelve a {@code hoy} conservando la vista activa. */
    public PeriodoCalendario irAHoy(Fecha hoy) {
        Objects.requireNonNull(hoy, "hoy");
        return new PeriodoCalendario(vista, hoy, hoy);
    }

    /**
     * Cambia la vista conservando el foco del periodo visible y el día seleccionado cuando aplica.
     */
    public PeriodoCalendario cambiarVista(VistaCalendario nuevaVista, Fecha hoy) {
        Objects.requireNonNull(nuevaVista, "nuevaVista");
        if (nuevaVista == vista) {
            return this;
        }
        switch (vista) {
            case MES:
                return cambiarDesdeMes(nuevaVista, hoy);
            case SEMANA:
                return cambiarDesdeSemana(nuevaVista, hoy);
            case DIA:
                return cambiarDesdeDia(nuevaVista);
        }
        throw new AssertionError("Vista no soportada: " + vista);
    }

    private PeriodoCalendario desplazar(int delta) {
        switch (vista) {
            case DIA: {
                Fecha nuevoFoco = fechaFoco.sumarDias(delta);
                return new PeriodoCalendario(vista, nuevoFoco, nuevoFoco);
            }
            case SEMANA: {
                Fecha nuevoFoco = fechaFoco.sumarDias(delta * 7);
                Fecha nuevoDia = diaSeleccionado != null ? diaSeleccionado.sumarDias(delta * 7) : null;
                return new PeriodoCalendario(vista, nuevoFoco, nuevoDia);
            }
            case MES: {
                int mesCero = fechaFoco.anio() * 12 + (fechaFoco.mes() - 1) + delta;
                int nuevoAnio = Math.floorDiv(mesCero, 12);
                int nuevoMes = Math.floorMod(mesCero, 12) + 1;
                Fecha nuevoFoco = Fecha.de(nuevoAnio, nuevoMes, 1).valor();
                Fecha nuevoDia = (diaSeleccionado != null
                        && diaSeleccionado.anio() == nuevoAnio
                        && diaSeleccionado.mes() == nuevoMes)
                        ? diaSeleccionado
                        : null;
                return new PeriodoCalendario(vista, nuevoFoco, nuevoDia);
            }
        }
        throw new AssertionError("Vista no soportada: " + vista);
    }

    private PeriodoCalendario cambiarDesdeMes(VistaCalendario nuevaVista, Fecha hoy) {
        Fecha primeroDelMes = Fecha.de(fechaFoco.anio(), fechaFoco.mes(), 1).valor();
        if (nuevaVista == VistaCalendario.SEMANA) {
            if (perteneceAMesVisible(diaSeleccionado)) {
                return new PeriodoCalendario(VistaCalendario.SEMANA, diaSeleccionado, diaSeleccionado);
            }
            return new PeriodoCalendario(VistaCalendario.SEMANA, primeroDelMes, null);
        }
        // MES -> DIA
        Fecha focoDia;
        if (perteneceAMesVisible(diaSeleccionado)) {
            focoDia = diaSeleccionado;
        } else if (perteneceAMesVisible(hoy)) {
            focoDia = hoy;
        } else {
            focoDia = primeroDelMes;
        }
        return new PeriodoCalendario(VistaCalendario.DIA, focoDia, focoDia);
    }

    private PeriodoCalendario cambiarDesdeSemana(VistaCalendario nuevaVista, Fecha hoy) {
        Fecha pivote;
        if (perteneceASemanaVisible(diaSeleccionado)) {
            pivote = diaSeleccionado;
        } else if (perteneceASemanaVisible(hoy)) {
            pivote = hoy;
        } else {
            pivote = fechaFoco.lunesDeSemana();
        }
        if (nuevaVista == VistaCalendario.DIA) {
            return new PeriodoCalendario(VistaCalendario.DIA, pivote, pivote);
        }
        // SEMANA -> MES
        Fecha nuevoFoco = Fecha.de(pivote.anio(), pivote.mes(), 1).valor();
        Fecha nuevoDia = (diaSeleccionado != null
                && diaSeleccionado.anio() == pivote.anio()
                && diaSeleccionado.mes() == pivote.mes())
                ? diaSeleccionado
                : null;
        return new PeriodoCalendario(VistaCalendario.MES, nuevoFoco, nuevoDia);
    }

    private PeriodoCalendario cambiarDesdeDia(VistaCalendario nuevaVista) {
        if (nuevaVista == VistaCalendario.SEMANA) {
            return new PeriodoCalendario(VistaCalendario.SEMANA, fechaFoco, fechaFoco);
        }
        // DIA -> MES
        Fecha primeroDelMes = Fecha.de(fechaFoco.anio(), fechaFoco.mes(), 1).valor();
        return new PeriodoCalendario(VistaCalendario.MES, primeroDelMes, fechaFoco);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PeriodoCalendario)) {
            return false;
        }
        PeriodoCalendario otro = (PeriodoCalendario) o;
        return vista == otro.vista
                && Objects.equals(fechaFoco, otro.fechaFoco)
                && Objects.equals(diaSeleccionado, otro.diaSeleccionado);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vista, fechaFoco, diaSeleccionado);
    }
}
