package io.github.jhoel0521.studyplan.adapter.in.ui;

import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

/**
 * Decide qué significa que la app se quede sin Activities en primer plano (HU-40, HU-50, RN-23): un bloqueo de
 * pantalla, una salida breve que no cuenta o una salida confirmada.
 *
 * <p>Línea de tiempo: al detenerse la última Activity se espera {@code esperaDecisionMs} (en algunos teléfonos la
 * pantalla se apaga un instante después). Si la pantalla está apagada es un bloqueo. Si no, empieza la ventana de
 * gracia de {@code graciaMs}: si el estudiante vuelve antes no pasa nada; si vence, o si al volver el reloj real
 * indica que ya venció (el temporizador no avanza con el teléfono dormido), se confirma la salida con la hora real
 * en que ocurrió.
 *
 * <p>Java puro, sin {@code android.*}, para probarlo en la JVM.
 */
final class SalidaConGracia {

    /** Qué ocurrió entre que la app se quedó sin Activities y volvió a primer plano. */
    enum Retorno {
        /** No se había salido de la app. */
        SIN_SALIDA,
        /** Volvió antes de que venciera la gracia: no hay evento ni pregunta. */
        DENTRO_DE_GRACIA,
        /** Había salido y la salida se confirmó (o se confirma ahora con la hora real). */
        SALIDA_CONFIRMADA,
        /** Volvió tras un bloqueo de pantalla. */
        DESBLOQUEO
    }

    /** Efectos que produce la decisión. */
    interface Destino {
        void registrarBloqueo();

        /**
         * @param instanteSalidaMs hora real en que salió
         * @param avisar {@code false} si el estudiante ya está de vuelta y no hace falta el aviso
         */
        void confirmarSalida(long instanteSalidaMs, boolean avisar);
    }

    private enum Fase { NINGUNA, DECIDIENDO, GRACIA, SALIDA_CONFIRMADA, BLOQUEADA }

    private final LongSupplier reloj;
    private final ProgramadorRetardado programador;
    private final BooleanSupplier pantallaApagada;
    private final Destino destino;
    private final long esperaDecisionMs;
    private final long graciaPorDefectoMs;

    private long graciaMs;
    private Fase fase = Fase.NINGUNA;
    private long instanteSalidaMs;

    private final Runnable decidir = this::decidir;
    private final Runnable vencerGracia = this::vencerGracia;

    SalidaConGracia(
            LongSupplier reloj,
            ProgramadorRetardado programador,
            BooleanSupplier pantallaApagada,
            Destino destino,
            long esperaDecisionMs,
            long graciaMs) {
        this.reloj = reloj;
        this.programador = programador;
        this.pantallaApagada = pantallaApagada;
        this.destino = destino;
        this.esperaDecisionMs = esperaDecisionMs;
        this.graciaPorDefectoMs = graciaMs;
        this.graciaMs = graciaMs;
    }

    /** Cambia la gracia (solo para pruebas con tiempos cortos). */
    void fijarGraciaMs(long nuevaGraciaMs) {
        this.graciaMs = nuevaGraciaMs;
    }

    /** Vuelve al estado inicial y a la gracia por defecto. */
    void reiniciar() {
        cancelarPendientes();
        fase = Fase.NINGUNA;
        graciaMs = graciaPorDefectoMs;
    }

    /** La última Activity se detuvo sin ser un cambio de configuración. */
    void ultimaActividadDetenida() {
        cancelarPendientes();
        instanteSalidaMs = reloj.getAsLong();
        fase = Fase.DECIDIENDO;
        programador.programar(decidir, esperaDecisionMs);
    }

    /** Una Activity volvió a iniciarse con la app sin Activities en primer plano. */
    Retorno actividadIniciada() {
        cancelarPendientes();
        Fase anterior = fase;
        fase = Fase.NINGUNA;
        switch (anterior) {
            case DECIDIENDO:
                return Retorno.DENTRO_DE_GRACIA;
            case GRACIA:
                if (reloj.getAsLong() - instanteSalidaMs >= graciaMs) {
                    destino.confirmarSalida(instanteSalidaMs, false);
                    return Retorno.SALIDA_CONFIRMADA;
                }
                return Retorno.DENTRO_DE_GRACIA;
            case SALIDA_CONFIRMADA:
                return Retorno.SALIDA_CONFIRMADA;
            case BLOQUEADA:
                return Retorno.DESBLOQUEO;
            case NINGUNA:
            default:
                return Retorno.SIN_SALIDA;
        }
    }

    private void decidir() {
        if (fase != Fase.DECIDIENDO) {
            return;
        }
        if (pantallaApagada.getAsBoolean()) {
            fase = Fase.BLOQUEADA;
            destino.registrarBloqueo();
            return;
        }
        fase = Fase.GRACIA;
        programador.programar(vencerGracia, Math.max(0L, graciaMs - esperaDecisionMs));
    }

    private void vencerGracia() {
        if (fase != Fase.GRACIA) {
            return;
        }
        fase = Fase.SALIDA_CONFIRMADA;
        destino.confirmarSalida(instanteSalidaMs, true);
    }

    private void cancelarPendientes() {
        programador.cancelar(decidir);
        programador.cancelar(vencerGracia);
    }
}
