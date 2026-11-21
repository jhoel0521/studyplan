package io.github.jhoel0521.studyplan.domain.readmodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.FiltroCalendario;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.valueobject.PeriodoCalendario;

/**
 * Resultado inmutable de la consulta del calendario para el periodo y filtro activos
 * (HU-16 a HU-21).
 */
public final class DatosCalendario {

    private final PeriodoCalendario periodo;
    private final FiltroCalendario filtro;
    private final ResumenPeriodoCalendario resumen;
    private final List<CeldaDiaMes> celdasMes;
    private final List<ItemSesionCalendario> sesionesDiaSeleccionado;
    private final List<BloqueHorarioCalendario> bloquesHorario;
    private final int minutoScrollInicial;
    private final List<Materia> materiasDisponibles;
    private final Fecha hoy;
    private final Hora ahora;

    public DatosCalendario(
            PeriodoCalendario periodo,
            FiltroCalendario filtro,
            ResumenPeriodoCalendario resumen,
            List<CeldaDiaMes> celdasMes,
            List<ItemSesionCalendario> sesionesDiaSeleccionado,
            List<BloqueHorarioCalendario> bloquesHorario,
            int minutoScrollInicial,
            List<Materia> materiasDisponibles,
            Fecha hoy,
            Hora ahora) {
        this.periodo = Objects.requireNonNull(periodo, "periodo");
        this.filtro = Objects.requireNonNull(filtro, "filtro");
        this.resumen = Objects.requireNonNull(resumen, "resumen");
        this.celdasMes = celdasMes == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(celdasMes));
        this.sesionesDiaSeleccionado = sesionesDiaSeleccionado == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(sesionesDiaSeleccionado));
        this.bloquesHorario = bloquesHorario == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(bloquesHorario));
        this.minutoScrollInicial = minutoScrollInicial;
        this.materiasDisponibles = materiasDisponibles == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(materiasDisponibles));
        this.hoy = Objects.requireNonNull(hoy, "hoy");
        this.ahora = Objects.requireNonNull(ahora, "ahora");
    }

    public PeriodoCalendario periodo() {
        return periodo;
    }

    public FiltroCalendario filtro() {
        return filtro;
    }

    public ResumenPeriodoCalendario resumen() {
        return resumen;
    }

    public List<CeldaDiaMes> celdasMes() {
        return celdasMes;
    }

    public List<ItemSesionCalendario> sesionesDiaSeleccionado() {
        return sesionesDiaSeleccionado;
    }

    public List<BloqueHorarioCalendario> bloquesHorario() {
        return bloquesHorario;
    }

    public int minutoScrollInicial() {
        return minutoScrollInicial;
    }

    public List<Materia> materiasDisponibles() {
        return materiasDisponibles;
    }

    public Fecha hoy() {
        return hoy;
    }

    public Hora ahora() {
        return ahora;
    }
}
