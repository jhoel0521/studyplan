package io.github.jhoel0521.studyplan.adapter.in.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.jhoel0521.studyplan.adapter.out.system.ProgramadorAvisosAlarmManager;
import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.domain.state.CodigoEstadoSesion;
import io.github.jhoel0521.studyplan.domain.valueobject.Instante;
import io.github.jhoel0521.studyplan.domain.entity.Materia;
import io.github.jhoel0521.studyplan.domain.enumeration.PreferenciaNoMolestar;
import io.github.jhoel0521.studyplan.domain.entity.SesionEstudio;
import io.github.jhoel0521.studyplan.domain.enumeration.TecnicaEstudio;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.Pruebas;
import io.github.jhoel0521.studyplan.adapter.in.ui.tecnicas.NotificadorSesion;

/**
 * Pruebas instrumentadas de {@link ReceptorAvisosSesion}, registro de acciones en el manifiesto y
 * composición en {@link StudyPlanApplication} para recordatorios de sesiones ({@code HU-34},
 * {@code RNF-18}).
 */
@RunWith(AndroidJUnit4.class)
public final class ReceptorAvisosSesionRecordatorioInstrumentalTest {

    private Context context;
    private StudyPlanApplication app;
    private NotificationManager nm;

    @Before
    public void preparar() {
        Pruebas.reiniciarEstado();
        context = ApplicationProvider.getApplicationContext();
        app = StudyPlanApplication.de(context);
        nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .grantRuntimePermission(
                            context.getPackageName(), Manifest.permission.POST_NOTIFICATIONS);
        }
        NotificadorSesion.cancelarTodosRecordatorios(context);
    }

    @After
    public void limpiar() {
        NotificadorSesion.cancelarTodosRecordatorios(context);
        Pruebas.reiniciarEstado();
    }

    @Test
    public void manifestRegistraTodasLasAccionesDeRecordatorioYSesionDetalleParent() throws Exception {
        PackageManager pm = context.getPackageManager();
        for (String accion : new String[] {
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        }) {
            Intent query = new Intent(accion).setPackage(context.getPackageName());
            List<ResolveInfo> receivers = pm.queryBroadcastReceivers(query, 0);
            assertTrue(
                    "Falta receiver para " + accion,
                    contieneReceptor(receivers, ReceptorAvisosSesion.class.getName()));
        }

        Intent recQuery = new Intent(ProgramadorAvisosAlarmManager.ACCION_RECORDATORIO_SESION)
                .setData(Uri.parse("studyplan://recordatorio/1"))
                .setPackage(context.getPackageName());
        assertTrue(contieneReceptor(
                pm.queryBroadcastReceivers(recQuery, 0),
                ReceptorAvisosSesion.class.getName()));
    }

    @Test
    public void alarmaEmiteNotificacionSoloSiSesionSiguePendienteAntesDelInicio() throws Exception {
        Materia materia = crearMateria("Cálculo");
        long ahoraMs = app.reloj().ahoraMilis();
        int desfase = app.reloj().ahoraInstante().desfaseMinutos();
        SesionEstudio pendiente = crearSesion(
                materia.id(),
                ahoraMs + 25 * 60_000L,
                ahoraMs + 85 * 60_000L,
                desfase,
                CodigoEstadoSesion.PENDIENTE,
                AntelacionRecordatorio.MINUTOS_15);
        SesionEstudio completada = crearSesion(
                materia.id(),
                ahoraMs + 100 * 60_000L,
                ahoraMs + 160 * 60_000L,
                desfase,
                CodigoEstadoSesion.COMPLETADA,
                AntelacionRecordatorio.MINUTOS_15);

        ReceptorAvisosSesion receptor = new ReceptorAvisosSesion();
        Intent ok = new Intent(ProgramadorAvisosAlarmManager.ACCION_RECORDATORIO_SESION)
                .setData(Uri.parse("studyplan://recordatorio/" + pendiente.id()))
                .putExtra(ProgramadorAvisosAlarmManager.EXTRA_SESION_ID, pendiente.id());
        receptor.onReceive(context, ok);
        Intent cerrada = new Intent(ProgramadorAvisosAlarmManager.ACCION_RECORDATORIO_SESION)
                .setData(Uri.parse("studyplan://recordatorio/" + completada.id()))
                .putExtra(ProgramadorAvisosAlarmManager.EXTRA_SESION_ID, completada.id());
        receptor.onReceive(context, cerrada);

        esperarEjecutor();
        assertTrue(esperarNotificacion(NotificadorSesion.idRecordatorio(pendiente.id()), true));
        assertFalse(estaActiva(NotificadorSesion.idRecordatorio(completada.id())));
    }

    @Test
    public void bootPackageReplacedYPermisoReprogramanFuturasYEmitenInmediatasEnVentanaVencida()
            throws Exception {
        Materia materia = crearMateria("Física");
        long ahoraMs = app.reloj().ahoraMilis();
        int desfase = app.reloj().ahoraInstante().desfaseMinutos();

        // Ventana vencida (inicia en 10 min, antelación 15 min -> disparo 5 min atrás)
        SesionEstudio vencidaAntesInicio = crearSesion(
                materia.id(),
                ahoraMs + 10 * 60_000L,
                ahoraMs + 70 * 60_000L,
                desfase,
                CodigoEstadoSesion.PENDIENTE,
                AntelacionRecordatorio.MINUTOS_15);
        // Futura (inicia en 120 min, antelación 15 min -> disparo en +105 min)
        SesionEstudio futura = crearSesion(
                materia.id(),
                ahoraMs + 120 * 60_000L,
                ahoraMs + 180 * 60_000L,
                desfase,
                CodigoEstadoSesion.PENDIENTE,
                AntelacionRecordatorio.MINUTOS_15);

        ReceptorAvisosSesion receptor = new ReceptorAvisosSesion();
        receptor.onReceive(context, new Intent(Intent.ACTION_MY_PACKAGE_REPLACED));
        esperarEjecutor();

        assertTrue(esperarNotificacion(
                NotificadorSesion.idRecordatorio(vencidaAntesInicio.id()), true));
        assertFalse(estaActiva(NotificadorSesion.idRecordatorio(futura.id())));
        assertNotNull(buscarPendingIntentRecordatorio(futura.id()));
        assertNull(buscarPendingIntentRecordatorio(vencidaAntesInicio.id()));

        // El cambio de permiso de alarmas exactas y BOOT_COMPLETED ejecutan la misma ruta sin duplicar
        NotificadorSesion.cancelarTodosRecordatorios(context);
        assertTrue(esperarNotificacion(
                NotificadorSesion.idRecordatorio(vencidaAntesInicio.id()), false));
        receptor.onReceive(
                context,
                new Intent(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED));
        esperarEjecutor();
        assertTrue(esperarNotificacion(
                NotificadorSesion.idRecordatorio(vencidaAntesInicio.id()), true));
        assertNotNull(buscarPendingIntentRecordatorio(futura.id()));
    }

    private Materia crearMateria(String nombre) throws Exception {
        return app.ejecutor()
                .submit(() -> app.materiaRepository()
                        .guardar(Materia.crear(nombre, 0xFF1565C0, null).valor()))
                .get(5, TimeUnit.SECONDS);
    }

    private SesionEstudio crearSesion(
            long materiaId,
            long inicioMs,
            long finMs,
            int desfaseMinutos,
            CodigoEstadoSesion estado,
            AntelacionRecordatorio antelacion)
            throws Exception {
        SesionEstudio sesion = SesionEstudio.restaurar(
                null,
                materiaId,
                inicioMs,
                finMs,
                desfaseMinutos,
                (int) ((finMs - inicioMs) / Instante.MILIS_POR_MINUTO),
                estado == CodigoEstadoSesion.COMPLETADA ? 60 : null,
                estado,
                "",
                null,
                TecnicaEstudio.LIBRE,
                PreferenciaNoMolestar.HEREDAR_GLOBAL,
                null,
                null,
                null,
                null,
                antelacion);
        return app.ejecutor()
                .submit(() -> app.sesionRepository().guardar(sesion))
                .get(5, TimeUnit.SECONDS);
    }

    private void esperarEjecutor() throws Exception {
        app.ejecutor().submit(() -> {}).get(5, TimeUnit.SECONDS);
    }

    private PendingIntent buscarPendingIntentRecordatorio(long sesionId) {
        Intent intent = new Intent(context, ReceptorAvisosSesion.class)
                .setAction(ProgramadorAvisosAlarmManager.ACCION_RECORDATORIO_SESION)
                .setData(Uri.parse("studyplan://recordatorio/" + sesionId))
                .putExtra(ProgramadorAvisosAlarmManager.EXTRA_SESION_ID, sesionId);
        return PendingIntent.getBroadcast(
                context,
                (int) (sesionId ^ (sesionId >>> 32)),
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
    }

    private static boolean contieneReceptor(List<ResolveInfo> lista, String fqcn) {
        for (ResolveInfo info : lista) {
            if (info.activityInfo != null && fqcn.equals(info.activityInfo.name)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("BusyWait") // Sondeo acotado por Binder asíncrono de NotificationManagerService
    private boolean esperarNotificacion(int id, boolean activaEsperada) {
        long limite = System.currentTimeMillis() + 3000L;
        while (System.currentTimeMillis() < limite) {
            if (estaActiva(id) == activaEsperada) {
                return true;
            }
            try {
                Thread.sleep(25L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return estaActiva(id) == activaEsperada;
    }

    private boolean estaActiva(int id) {
        for (StatusBarNotification sbn : nm.getActiveNotifications()) {
            if (sbn.getId() == id) {
                return true;
            }
        }
        return false;
    }
}
