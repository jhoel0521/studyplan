package io.github.jhoel0521.studyplan.adapter.out.system;

import android.app.admin.DeviceAdminReceiver;

/**
 * Administrador del dispositivo de StudyPlan. Su única política es {@code force-lock}: apagar y
 * bloquear la pantalla cuando el estudiante toca el candado de la sesión (HU-50, RNF-18). No puede
 * ver contraseñas, borrar datos ni cambiar otras políticas.
 */
public final class ReceptorAdministradorBloqueo extends DeviceAdminReceiver {
}
