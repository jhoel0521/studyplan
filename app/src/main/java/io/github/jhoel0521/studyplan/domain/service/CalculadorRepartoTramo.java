package io.github.jhoel0521.studyplan.domain.service;

import java.util.ArrayList;
import java.util.List;
import io.github.jhoel0521.studyplan.domain.entity.EventoEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.domain.readmodel.EstadoPomodoroCalculado;
import io.github.jhoel0521.studyplan.domain.readmodel.ResultadoReparto;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.valueobject.ConfiguracionPomodoro;
import io.github.jhoel0521.studyplan.domain.valueobject.IntervaloEfectivo;

/**
 * Segmenta los intervalos efectivos de una sesión respecto a un instante de corte sin convertir
 * pausas ni descansos Pomodoro en tiempo real (HU-44, RN-11, RN-28).
 */
public final class CalculadorRepartoTramo {

    private CalculadorRepartoTramo() {
    }

    /**
     * Reparte el tiempo efectivo de {@code eventos} en torno a {@code corteMilis}.
     */
    public static Resultado<ResultadoReparto> repartir(
            TecnicaEstudio tecnica,
            ConfiguracionPomodoro pomodoro,
            int duracionPlanificadaMinutos,
            List<EventoEstudio> eventos,
            long ahoraMilis,
            long corteMilis) {
        List<IntervaloEfectivo> intervalos =
                CalculadorTiempoReal.reconstruirIntervalos(eventos, ahoraMilis);
        if (intervalos.isEmpty()) {
            return Resultado.error(ErrorValidacion.CORTE_REPARTO_INVALIDO);
        }
        long inicioTramo = intervalos.get(0).inicioMilis();
        long finTramo = intervalos.get(intervalos.size() - 1).finMilis();
        if (corteMilis <= inicioTramo || corteMilis >= finTramo) {
            return Resultado.error(ErrorValidacion.CORTE_REPARTO_INVALIDO);
        }

        List<IntervaloEfectivo> primera = new ArrayList<>();
        List<IntervaloEfectivo> segunda = new ArrayList<>();
        for (IntervaloEfectivo iv : intervalos) {
            if (iv.finMilis() <= corteMilis) {
                primera.add(iv);
            } else if (iv.inicioMilis() >= corteMilis) {
                segunda.add(iv);
            } else {
                primera.add(new IntervaloEfectivo(iv.inicioMilis(), corteMilis, iv.esAppCerrada()));
                segunda.add(new IntervaloEfectivo(corteMilis, iv.finMilis(), iv.esAppCerrada()));
            }
        }

        long totalEfectivoMs = CalculadorTiempoReal.calcularMilisEfectivos(
                tecnica, pomodoro, duracionPlanificadaMinutos, eventos, ahoraMilis);
        long primeraBrutoMs = sumarMilis(primera);
        long totalBrutoMs = primeraBrutoMs + sumarMilis(segunda);

        long primeraEfectivoMs;
        if (tecnica == TecnicaEstudio.POMODORO) {
            long dentroPrimera = sumarDentroApp(primera);
            long cerradaPrimera = primeraBrutoMs - dentroPrimera;
            ConfiguracionPomodoro cfg =
                    pomodoro == null ? ConfiguracionPomodoro.porDefecto() : pomodoro;
            int objetivo = CalculadorPomodoro.pomodorosQueCabenEnPlan(duracionPlanificadaMinutos, cfg);
            EstadoPomodoroCalculado estadoPrimera = CalculadorPomodoro.calcularEstado(
                    dentroPrimera, cfg, objetivo, false);
            primeraEfectivoMs = Math.min(
                    totalEfectivoMs, estadoPrimera.milisTrabajoAcumulados() + cerradaPrimera);
        } else {
            primeraEfectivoMs = totalBrutoMs == 0 ? 0 : Math.min(totalEfectivoMs, primeraBrutoMs);
        }
        long segundaEfectivoMs = Math.max(0L, totalEfectivoMs - primeraEfectivoMs);

        return Resultado.ok(new ResultadoReparto(
                inicioTramo,
                corteMilis,
                finTramo,
                (int) (primeraEfectivoMs / 60_000L),
                (int) (segundaEfectivoMs / 60_000L)));
    }

    private static long sumarMilis(List<IntervaloEfectivo> lista) {
        long total = 0L;
        for (IntervaloEfectivo iv : lista) {
            total += iv.duracionMilis();
        }
        return total;
    }

    private static long sumarDentroApp(List<IntervaloEfectivo> lista) {
        long total = 0L;
        for (IntervaloEfectivo iv : lista) {
            if (!iv.esAppCerrada()) {
                total += iv.duracionMilis();
            }
        }
        return total;
    }
}
