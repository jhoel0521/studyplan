package io.github.jhoel0521.studyplan.adapter.out.drive;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import io.github.jhoel0521.studyplan.adapter.in.ui.StudyPlanApplication;
import io.github.jhoel0521.studyplan.domain.result.ResultadoRespaldo;

/**
 * Tarea periódica de {@code WorkManager} que sube el respaldo a Drive. Solo corre con red ({@code CONNECTED}). Si
 * falla por red o por un error pasajero se reintenta con espera creciente; si la autorización caducó termina con
 * fallo (queda registrado para que Ajustes ofrezca «Volver a autorizar») y la siguiente ejecución periódica sigue
 * programada.
 *
 * <p>Implementa: HU-35 (escenario «Sin conexión») y RN-19.
 */
public final class WorkerRespaldoDrive extends Worker {

    public WorkerRespaldoDrive(@NonNull Context contexto, @NonNull WorkerParameters parametros) {
        super(contexto, parametros);
    }

    @NonNull
    @Override
    public Result doWork() {
        StudyPlanApplication aplicacion = (StudyPlanApplication) getApplicationContext();
        ResultadoRespaldo resultado = aplicacion.respaldarAhora().ejecutar();
        if (resultado == ResultadoRespaldo.OK) {
            return Result.success();
        }
        return debeReintentar(resultado) ? Result.retry() : Result.failure();
    }

    /** Solo los fallos de red o pasajeros se reintentan; los demás exigen que el estudiante actúe. */
    static boolean debeReintentar(ResultadoRespaldo resultado) {
        return resultado == ResultadoRespaldo.SIN_CONEXION;
    }
}
