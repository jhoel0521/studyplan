package io.github.jhoel0521.studyplan.adapter.out.drive;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import io.github.jhoel0521.studyplan.application.port.out.system.ProgramadorRespaldo;
import io.github.jhoel0521.studyplan.domain.enumeration.FrecuenciaRespaldo;

import java.util.concurrent.TimeUnit;

/**
 * Adaptador de salida: programa el respaldo periódico (1, 7 o 30 días) con {@code WorkManager} y la restricción de red
 * conectada. Reprogramar reemplaza la tarea anterior.
 *
 * <p>Implementa: HU-35 y RN-19.
 */
public final class ProgramadorRespaldoWorkManager implements ProgramadorRespaldo {

    static final String NOMBRE_TAREA = "studyplan_respaldo_periodico";

    private final Context contexto;

    public ProgramadorRespaldoWorkManager(Context contexto) {
        this.contexto = contexto.getApplicationContext();
    }

    @Override
    public void programar(FrecuenciaRespaldo frecuencia) {
        if (!frecuencia.esAutomatica()) {
            throw new IllegalArgumentException("La frecuencia " + frecuencia + " no es automática");
        }
        Constraints condiciones = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        PeriodicWorkRequest peticion = new PeriodicWorkRequest.Builder(
                WorkerRespaldoDrive.class, frecuencia.periodoDias(), TimeUnit.DAYS)
                .setConstraints(condiciones)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(contexto)
                .enqueueUniquePeriodicWork(NOMBRE_TAREA, ExistingPeriodicWorkPolicy.UPDATE, peticion);
    }

    @Override
    public void cancelar() {
        WorkManager.getInstance(contexto).cancelUniqueWork(NOMBRE_TAREA);
    }
}
