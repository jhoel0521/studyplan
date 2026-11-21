package io.github.jhoel0521.studyplan.adapter.in.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.jhoel0521.studyplan.application.port.in.ConsultarAjustes;
import io.github.jhoel0521.studyplan.application.port.in.ConsultarSesion;
import io.github.jhoel0521.studyplan.application.port.in.ListarMaterias;
import io.github.jhoel0521.studyplan.application.port.in.PlanificarSesion;
import io.github.jhoel0521.studyplan.application.port.in.RegistrarSesionPasada;
import io.github.jhoel0521.studyplan.application.port.in.ReprogramarSesion;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.domain.enumeration.AntelacionRecordatorio;
import io.github.jhoel0521.studyplan.domain.readmodel.DetalleSesion;
import io.github.jhoel0521.studyplan.domain.result.ErrorValidacion;
import io.github.jhoel0521.studyplan.domain.valueobject.Fecha;
import io.github.jhoel0521.studyplan.domain.valueobject.Hora;
import io.github.jhoel0521.studyplan.domain.valueobject.Instante;
import io.github.jhoel0521.studyplan.domain.result.Resultado;
import io.github.jhoel0521.studyplan.domain.readmodel.ResultadoSesion;
import io.github.jhoel0521.studyplan.domain.readmodel.ResumenMateria;

/**
 * Estado y lógica de presentación del formulario de sesión en sus tres flujos: planificar una sesión
 * futura/actual, registrar una sesión pasada (hasta 3 días) y reprogramar una sesión pendiente o no realizada.
 *
 * <p>Implementa: HU-10, HU-11, HU-14, HU-34 y RNF-10.
 */
public class SesionFormularioViewModel extends ViewModel {

    private final PlanificarSesion planificarSesion;
    private final RegistrarSesionPasada registrarSesionPasada;
    private final ReprogramarSesion reprogramarSesion;
    private final ListarMaterias listarMaterias;
    private final ConsultarSesion consultarSesion;
    private final ConsultarAjustes consultarAjustes;
    private final Reloj reloj;
    private final Executor ejecutor;

    private final MutableLiveData<List<ResumenMateria>> materiasActivas = new MutableLiveData<>();
    private final MutableLiveData<DetalleSesion> sesionBase = new MutableLiveData<>();
    private final MutableLiveData<Fecha> fechaHoy = new MutableLiveData<>();
    private final MutableLiveData<AntelacionRecordatorio> recordatorioPorDefecto =
            new MutableLiveData<>();
    private final MutableLiveData<EventoSesion> evento = new MutableLiveData<>();
    private final AtomicBoolean enCurso = new AtomicBoolean(false);

    public SesionFormularioViewModel(
            PlanificarSesion planificarSesion,
            RegistrarSesionPasada registrarSesionPasada,
            ReprogramarSesion reprogramarSesion,
            ListarMaterias listarMaterias,
            ConsultarSesion consultarSesion,
            Reloj reloj,
            Executor ejecutor) {
        this(
                planificarSesion,
                registrarSesionPasada,
                reprogramarSesion,
                listarMaterias,
                consultarSesion,
                null,
                reloj,
                ejecutor);
    }

    public SesionFormularioViewModel(
            PlanificarSesion planificarSesion,
            RegistrarSesionPasada registrarSesionPasada,
            ReprogramarSesion reprogramarSesion,
            ListarMaterias listarMaterias,
            ConsultarSesion consultarSesion,
            ConsultarAjustes consultarAjustes,
            Reloj reloj,
            Executor ejecutor) {
        this.planificarSesion = planificarSesion;
        this.registrarSesionPasada = registrarSesionPasada;
        this.reprogramarSesion = reprogramarSesion;
        this.listarMaterias = listarMaterias;
        this.consultarSesion = consultarSesion;
        this.consultarAjustes = consultarAjustes;
        this.reloj = reloj;
        this.ejecutor = ejecutor;
    }

    public LiveData<List<ResumenMateria>> materiasActivas() {
        return materiasActivas;
    }

    public LiveData<DetalleSesion> sesionBase() {
        return sesionBase;
    }

    public LiveData<Fecha> fechaHoy() {
        return fechaHoy;
    }

    public LiveData<AntelacionRecordatorio> recordatorioPorDefecto() {
        return recordatorioPorDefecto;
    }

    public LiveData<EventoSesion> evento() {
        return evento;
    }

    /** Carga la fecha actual, recordatorio por defecto, materias activas y sesión base si aplica. */
    public void cargar(long sesionId) {
        ejecutor.execute(() -> {
            fechaHoy.postValue(reloj.hoy());
            AntelacionRecordatorio defecto = consultarAjustes == null
                    ? AntelacionRecordatorio.MINUTOS_15
                    : consultarAjustes.ejecutar().recordatorioPorDefecto();
            recordatorioPorDefecto.postValue(defecto);
            materiasActivas.postValue(listarMaterias.ejecutar().activas());
            if (sesionId > 0) {
                consultarSesion.ejecutar(sesionId).ifPresent(sesionBase::postValue);
            }
        });
    }

    /** Indica si el instante previo de recordatorio ya pasó antes del inicio de la sesión. */
    public boolean esVentanaRecordatorioVencida(
            String fechaIso, String horaInicioTxt, AntelacionRecordatorio antelacion) {
        if (antelacion == null || !antelacion.activo()) {
            return false;
        }
        Resultado<Fecha> fecha = Fecha.parsear(fechaIso);
        Resultado<Hora> inicio = Hora.parsear(horaInicioTxt);
        if (!fecha.esValido() || !inicio.esValido()) {
            return false;
        }
        Instante ahoraInst = reloj.ahoraInstante();
        long inicioMs = Instante.desdeFechaYHoraLocal(
                fecha.valor(), inicio.valor(), ahoraInst.desfaseMinutos()).utcMilis();
        long disparoMs = inicioMs - antelacion.milisAntes();
        long ahoraMs = ahoraInst.utcMilis();
        return disparoMs <= ahoraMs && ahoraMs < inicioMs;
    }

    /** Planifica una sesión nueva sin recordatorio (sobrecarga compatible). */
    public void guardarPlanificada(
            Long materiaId, String fechaIso, String horaInicioTxt, String horaFinTxt, String notas) {
        guardarPlanificada(
                materiaId,
                fechaIso,
                horaInicioTxt,
                horaFinTxt,
                notas,
                AntelacionRecordatorio.SIN_RECORDATORIO);
    }

    /** Planifica una sesión nueva con antelación de recordatorio (HU-10, HU-34). */
    public void guardarPlanificada(
            Long materiaId,
            String fechaIso,
            String horaInicioTxt,
            String horaFinTxt,
            String notas,
            AntelacionRecordatorio antelacion) {
        if (materiaId == null || materiaId <= 0) {
            evento.setValue(EventoSesion.error(ErrorValidacion.MATERIA_SESION_OBLIGATORIA));
            return;
        }
        Resultado<HorarioFormulario> horario = validarHorario(fechaIso, horaInicioTxt, horaFinTxt);
        if (!horario.esValido()) {
            evento.setValue(EventoSesion.error(horario.error()));
            return;
        }
        HorarioFormulario datos = horario.valor();
        AntelacionRecordatorio segura =
                antelacion == null ? AntelacionRecordatorio.SIN_RECORDATORIO : antelacion;
        ejecutarConCandado(() -> publicarResultado(
                planificarSesion.ejecutar(
                        materiaId, datos.fecha, datos.inicio, datos.fin, notas, segura),
                EventoSesion.Tipo.PLANIFICADA));
    }

    /** Registra una sesión pasada dentro de la ventana permitida de 3 días (HU-11). */
    public void guardarPasada(
            Long materiaId,
            String fechaIso,
            String horaInicioTxt,
            String horaFinTxt,
            String duracionRealTxt,
            String notas) {
        if (materiaId == null || materiaId <= 0) {
            evento.setValue(EventoSesion.error(ErrorValidacion.MATERIA_SESION_OBLIGATORIA));
            return;
        }
        Resultado<HorarioFormulario> horario = validarHorario(fechaIso, horaInicioTxt, horaFinTxt);
        if (!horario.esValido()) {
            evento.setValue(EventoSesion.error(horario.error()));
            return;
        }
        HorarioFormulario datos = horario.valor();
        int duracionReal = parsearEntero(duracionRealTxt);
        ejecutarConCandado(() -> publicarResultado(
                registrarSesionPasada.ejecutar(
                        materiaId, datos.fecha, datos.inicio, datos.fin, duracionReal, notas),
                EventoSesion.Tipo.PASADA_REGISTRADA));
    }

    /** Reprograma una sesión pendiente o no realizada conservando la antelación previa (HU-14). */
    public void reprogramar(
            long sesionId,
            String nuevaFechaIso,
            String nuevaHoraInicioTxt,
            String nuevaHoraFinTxt,
            String notas) {
        Resultado<HorarioFormulario> horario = validarHorario(nuevaFechaIso, nuevaHoraInicioTxt, nuevaHoraFinTxt);
        if (!horario.esValido()) {
            evento.setValue(EventoSesion.error(horario.error()));
            return;
        }
        HorarioFormulario datos = horario.valor();
        ejecutarConCandado(() -> publicarResultado(
                reprogramarSesion.ejecutar(sesionId, datos.fecha, datos.inicio, datos.fin, notas),
                EventoSesion.Tipo.REPROGRAMADA));
    }

    /** Reprograma una sesión indicando explícitamente la nueva antelación de recordatorio (HU-14, HU-34). */
    public void reprogramar(
            long sesionId,
            String nuevaFechaIso,
            String nuevaHoraInicioTxt,
            String nuevaHoraFinTxt,
            String notas,
            AntelacionRecordatorio antelacion) {
        Resultado<HorarioFormulario> horario = validarHorario(nuevaFechaIso, nuevaHoraInicioTxt, nuevaHoraFinTxt);
        if (!horario.esValido()) {
            evento.setValue(EventoSesion.error(horario.error()));
            return;
        }
        HorarioFormulario datos = horario.valor();
        AntelacionRecordatorio segura =
                antelacion == null ? AntelacionRecordatorio.SIN_RECORDATORIO : antelacion;
        ejecutarConCandado(() -> publicarResultado(
                reprogramarSesion.ejecutar(
                        sesionId, datos.fecha, datos.inicio, datos.fin, notas, segura),
                EventoSesion.Tipo.REPROGRAMADA));
    }

    public void consumirEvento() {
        evento.setValue(null);
    }

    private static Resultado<HorarioFormulario> validarHorario(
            String fechaIso, String horaInicioTxt, String horaFinTxt) {
        Resultado<Fecha> fecha = Fecha.parsear(fechaIso);
        if (!fecha.esValido()) {
            return Resultado.error(fecha.error());
        }
        Resultado<Hora> inicio = Hora.parsear(horaInicioTxt, ErrorValidacion.HORA_INICIO_OBLIGATORIA);
        if (!inicio.esValido()) {
            return Resultado.error(inicio.error());
        }
        Resultado<Hora> fin = Hora.parsear(horaFinTxt, ErrorValidacion.HORA_FIN_OBLIGATORIA);
        if (!fin.esValido()) {
            return Resultado.error(fin.error());
        }
        if (fin.valor().equals(inicio.valor())) {
            return Resultado.error(ErrorValidacion.HORARIO_FIN_NO_POSTERIOR);
        }
        return Resultado.ok(new HorarioFormulario(fecha.valor(), inicio.valor(), fin.valor()));
    }

    private void ejecutarConCandado(Runnable tarea) {
        if (!enCurso.compareAndSet(false, true)) {
            return;
        }
        ejecutor.execute(() -> {
            try {
                tarea.run();
            } finally {
                enCurso.set(false);
            }
        });
    }

    private void publicarResultado(ResultadoSesion resultado, EventoSesion.Tipo tipoExito) {
        if (resultado.esValido()) {
            evento.postValue(EventoSesion.ok(tipoExito, resultado.sesion().id()));
        } else {
            evento.postValue(EventoSesion.fallo(resultado));
        }
    }

    private static int parsearEntero(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static final class HorarioFormulario {
        final Fecha fecha;
        final Hora inicio;
        final Hora fin;

        HorarioFormulario(Fecha fecha, Hora inicio, Hora fin) {
            this.fecha = fecha;
            this.inicio = inicio;
            this.fin = fin;
        }
    }
}
