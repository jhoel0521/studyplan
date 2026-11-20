package io.github.jhoel0521.studyplan.adapter.out.system;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/**
 * Bloquea la pantalla del teléfono con el permiso de administrador del dispositivo limitado a
 * {@code force-lock} (HU-50, RNF-18). Permite consultar, solicitar y retirar ese permiso.
 */
public final class BloqueoPantallaSistema {

    private final DevicePolicyManager politicas;
    private final ComponentName administrador;

    public BloqueoPantallaSistema(Context contexto) {
        Context app = contexto.getApplicationContext();
        this.politicas = (DevicePolicyManager) app.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.administrador = new ComponentName(app, ReceptorAdministradorBloqueo.class);
    }

    /** Indica si StudyPlan ya es administrador del dispositivo y puede bloquear la pantalla. */
    public boolean estaActivo() {
        return politicas != null && politicas.isAdminActive(administrador);
    }

    /** Apaga y bloquea la pantalla. @return {@code false} si falta el permiso y no se hizo nada */
    public boolean bloquear() {
        if (!estaActivo()) {
            return false;
        }
        politicas.lockNow();
        return true;
    }

    /** Retira el permiso para que el estudiante pueda desinstalar la app sin pasos extra. */
    public void quitarPermiso() {
        if (estaActivo()) {
            politicas.removeActiveAdmin(administrador);
        }
    }

    /** Intent de la pantalla del sistema donde el estudiante concede el permiso, con su explicación. */
    public Intent intentSolicitarPermiso(String explicacion) {
        Intent intento = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intento.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, administrador);
        intento.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explicacion);
        return intento;
    }
}
