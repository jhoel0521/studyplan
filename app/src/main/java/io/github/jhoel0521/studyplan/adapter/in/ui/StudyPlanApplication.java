package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.jhoel0521.studyplan.adapter.out.persistence.repository.AjustesRepositoryRoom;
import io.github.jhoel0521.studyplan.adapter.out.persistence.repository.AlmacenDatosRoom;
import io.github.jhoel0521.studyplan.adapter.out.persistence.repository.EventoEstudioRepositoryRoom;
import io.github.jhoel0521.studyplan.adapter.out.persistence.repository.MateriaRepositoryRoom;
import io.github.jhoel0521.studyplan.adapter.out.persistence.repository.MetaSemanalRepositoryRoom;
import io.github.jhoel0521.studyplan.adapter.out.persistence.repository.PerfilRepositoryRoom;
import io.github.jhoel0521.studyplan.adapter.out.system.RelojSistema;
import io.github.jhoel0521.studyplan.adapter.out.persistence.repository.SesionRepositoryRoom;
import io.github.jhoel0521.studyplan.adapter.out.persistence.repository.SesionesDeMateriaRoom;
import io.github.jhoel0521.studyplan.adapter.out.persistence.StudyPlanDatabase;
import io.github.jhoel0521.studyplan.adapter.out.persistence.repository.TramoDiarioRepositoryRoom;
import io.github.jhoel0521.studyplan.adapter.out.security.DisponibilidadBloqueoBiometrico;
import io.github.jhoel0521.studyplan.adapter.out.system.ControladorNoMolestarSistema;
import io.github.jhoel0521.studyplan.adapter.out.system.ExportadorReporteSistema;
import io.github.jhoel0521.studyplan.adapter.out.system.MarcadorSalidaAppPrefs;
import io.github.jhoel0521.studyplan.adapter.out.system.ProgramadorAvisosAlarmManager;
import io.github.jhoel0521.studyplan.adapter.out.system.ProgramadorAvisosConNotificacion;
import io.github.jhoel0521.studyplan.application.port.in.materias.ArchivarMateria;
import io.github.jhoel0521.studyplan.application.port.in.configuracion.BorrarDatos;
import io.github.jhoel0521.studyplan.application.port.in.metas.CambiarDeMateria;
import io.github.jhoel0521.studyplan.application.port.in.sesiones.CompletarSesion;
import io.github.jhoel0521.studyplan.application.port.in.tecnicas.ConfigurarAjustesEstudio;
import io.github.jhoel0521.studyplan.application.port.in.configuracion.ConfigurarApariencia;
import io.github.jhoel0521.studyplan.application.port.in.configuracion.ConfigurarBloqueo;
import io.github.jhoel0521.studyplan.application.port.in.configuracion.ConfigurarPerfil;
import io.github.jhoel0521.studyplan.application.port.in.recordatorios.ConfigurarRecordatorioPorDefecto;
import io.github.jhoel0521.studyplan.application.port.in.configuracion.ConsultarAjustes;
import io.github.jhoel0521.studyplan.application.port.in.calendario.ConsultarCalendario;
import io.github.jhoel0521.studyplan.application.port.in.tecnicas.ConsultarEjecucionSesion;
import io.github.jhoel0521.studyplan.application.port.in.estadisticas.ConsultarEstadisticas;
import io.github.jhoel0521.studyplan.application.port.in.configuracion.ConsultarEstadoArranque;
import io.github.jhoel0521.studyplan.application.port.in.materias.ConsultarMateria;
import io.github.jhoel0521.studyplan.application.port.in.configuracion.ConsultarPerfil;
import io.github.jhoel0521.studyplan.application.port.in.metas.ConsultarProgresoMetas;
import io.github.jhoel0521.studyplan.application.port.in.sesiones.ConsultarSesion;
import io.github.jhoel0521.studyplan.application.port.in.configuracion.ControlarSesionBloqueo;
import io.github.jhoel0521.studyplan.application.port.in.tecnicas.DecidirFinPlanificadoOInvasion;
import io.github.jhoel0521.studyplan.application.port.in.metas.DefinirMetaSemanal;
import io.github.jhoel0521.studyplan.application.port.in.materias.DesarchivarMateria;
import io.github.jhoel0521.studyplan.application.port.in.tecnicas.DetenerSesion;
import io.github.jhoel0521.studyplan.application.port.in.materias.EditarMateria;
import io.github.jhoel0521.studyplan.application.port.in.materias.EliminarMateria;
import io.github.jhoel0521.studyplan.application.port.in.sesiones.EliminarSesion;
import io.github.jhoel0521.studyplan.application.port.in.metas.EstudiarAhora;
import io.github.jhoel0521.studyplan.application.port.in.tecnicas.EvaluarRecuperacionSesion;
import io.github.jhoel0521.studyplan.application.port.in.reportes.GenerarReporteEstudio;
import io.github.jhoel0521.studyplan.application.port.in.tecnicas.IniciarSesion;
import io.github.jhoel0521.studyplan.application.port.in.materias.ListarMaterias;
import io.github.jhoel0521.studyplan.application.port.in.configuracion.ObtenerResumenInicio;
import io.github.jhoel0521.studyplan.application.port.in.tecnicas.PausarReanudarSesion;
import io.github.jhoel0521.studyplan.application.port.in.sesiones.PlanificarSesion;
import io.github.jhoel0521.studyplan.application.port.in.materias.RegistrarMateria;
import io.github.jhoel0521.studyplan.application.port.in.tecnicas.AlternarNoMolestarSesion;
import io.github.jhoel0521.studyplan.application.port.in.tecnicas.RegistrarBloqueoPantalla;
import io.github.jhoel0521.studyplan.application.port.in.tecnicas.RegistrarDesbloqueo;
import io.github.jhoel0521.studyplan.application.port.in.tecnicas.RegistrarSalidaApp;
import io.github.jhoel0521.studyplan.application.port.in.sesiones.RegistrarSesionPasada;
import io.github.jhoel0521.studyplan.application.port.in.sesiones.RegistrarSesionTardia;
import io.github.jhoel0521.studyplan.application.port.in.tecnicas.RepartirTramoSesion;
import io.github.jhoel0521.studyplan.application.port.in.recordatorios.ReprogramarRecordatoriosPendientes;
import io.github.jhoel0521.studyplan.application.port.in.sesiones.ReprogramarSesion;
import io.github.jhoel0521.studyplan.application.port.in.tecnicas.ResolverRegresoApp;
import io.github.jhoel0521.studyplan.application.port.in.sesiones.VencerSesionesPendientes;
import io.github.jhoel0521.studyplan.application.port.out.persistence.AjustesRepository;
import io.github.jhoel0521.studyplan.application.port.out.system.ControladorNoMolestar;
import io.github.jhoel0521.studyplan.application.port.out.persistence.EventoEstudioRepository;
import io.github.jhoel0521.studyplan.application.port.out.system.ExportadorReporte;
import io.github.jhoel0521.studyplan.application.port.out.persistence.MateriaRepository;
import io.github.jhoel0521.studyplan.application.port.out.system.ProgramadorRecordatorios;
import io.github.jhoel0521.studyplan.application.port.out.system.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.persistence.SesionRepository;
import io.github.jhoel0521.studyplan.application.port.out.persistence.TramoDiarioRepository;
import io.github.jhoel0521.studyplan.application.service.materias.ArchivarMateriaService;
import io.github.jhoel0521.studyplan.application.service.configuracion.BorrarDatosService;
import io.github.jhoel0521.studyplan.application.service.metas.CambiarDeMateriaService;
import io.github.jhoel0521.studyplan.application.service.sesiones.CompletarSesionService;
import io.github.jhoel0521.studyplan.application.service.tecnicas.ConfigurarAjustesEstudioService;
import io.github.jhoel0521.studyplan.application.service.configuracion.ConfigurarAparienciaService;
import io.github.jhoel0521.studyplan.application.service.configuracion.ConfigurarBloqueoService;
import io.github.jhoel0521.studyplan.application.service.configuracion.ConfigurarPerfilService;
import io.github.jhoel0521.studyplan.application.service.recordatorios.ConfigurarRecordatorioPorDefectoService;
import io.github.jhoel0521.studyplan.application.service.configuracion.ConsultarAjustesService;
import io.github.jhoel0521.studyplan.application.service.calendario.ConsultarCalendarioService;
import io.github.jhoel0521.studyplan.application.service.tecnicas.ConsultarEjecucionSesionService;
import io.github.jhoel0521.studyplan.application.service.estadisticas.ConsultarEstadisticasService;
import io.github.jhoel0521.studyplan.application.service.configuracion.ConsultarEstadoArranqueService;
import io.github.jhoel0521.studyplan.application.service.materias.ConsultarMateriaService;
import io.github.jhoel0521.studyplan.application.service.configuracion.ConsultarPerfilService;
import io.github.jhoel0521.studyplan.application.service.metas.ConsultarProgresoMetasService;
import io.github.jhoel0521.studyplan.application.service.sesiones.ConsultarSesionService;
import io.github.jhoel0521.studyplan.application.service.tecnicas.DecidirFinPlanificadoOInvasionService;
import io.github.jhoel0521.studyplan.application.service.metas.DefinirMetaSemanalService;
import io.github.jhoel0521.studyplan.application.service.materias.DesarchivarMateriaService;
import io.github.jhoel0521.studyplan.application.service.tecnicas.DetenerSesionService;
import io.github.jhoel0521.studyplan.application.service.materias.EditarMateriaService;
import io.github.jhoel0521.studyplan.application.service.materias.EliminarMateriaService;
import io.github.jhoel0521.studyplan.application.service.sesiones.EliminarSesionService;
import io.github.jhoel0521.studyplan.application.service.metas.EstudiarAhoraService;
import io.github.jhoel0521.studyplan.application.service.tecnicas.EvaluarRecuperacionSesionService;
import io.github.jhoel0521.studyplan.application.service.reportes.GenerarReporteEstudioService;
import io.github.jhoel0521.studyplan.application.service.tecnicas.IniciarSesionService;
import io.github.jhoel0521.studyplan.application.service.materias.ListarMateriasService;
import io.github.jhoel0521.studyplan.application.service.configuracion.ObtenerResumenInicioService;
import io.github.jhoel0521.studyplan.application.service.tecnicas.PausarReanudarSesionService;
import io.github.jhoel0521.studyplan.application.service.sesiones.PlanificarSesionService;
import io.github.jhoel0521.studyplan.application.service.materias.RegistrarMateriaService;
import io.github.jhoel0521.studyplan.application.service.tecnicas.AlternarNoMolestarSesionService;
import io.github.jhoel0521.studyplan.application.service.tecnicas.RegistrarBloqueoPantallaService;
import io.github.jhoel0521.studyplan.application.service.tecnicas.RegistrarDesbloqueoService;
import io.github.jhoel0521.studyplan.application.service.tecnicas.RegistrarSalidaAppService;
import io.github.jhoel0521.studyplan.application.service.sesiones.RegistrarSesionPasadaService;
import io.github.jhoel0521.studyplan.application.service.sesiones.RegistrarSesionTardiaService;
import io.github.jhoel0521.studyplan.application.service.tecnicas.RepartirTramoSesionService;
import io.github.jhoel0521.studyplan.application.service.recordatorios.ReprogramarRecordatoriosPendientesService;
import io.github.jhoel0521.studyplan.application.service.sesiones.ReprogramarSesionService;
import io.github.jhoel0521.studyplan.application.service.tecnicas.ResolverRegresoAppService;
import io.github.jhoel0521.studyplan.application.service.configuracion.SesionBloqueo;
import io.github.jhoel0521.studyplan.application.service.sesiones.VencerSesionesPendientesService;
import io.github.jhoel0521.studyplan.domain.entity.Ajustes;
import io.github.jhoel0521.studyplan.adapter.in.ui.calendario.PreferenciaVistaCalendario;
import io.github.jhoel0521.studyplan.adapter.in.ui.configuracion.ObservadorProgresoWidget;
import io.github.jhoel0521.studyplan.adapter.in.ui.configuracion.WidgetProgresoProvider;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.AplicadorApariencia;
import io.github.jhoel0521.studyplan.adapter.in.ui.comun.ContadorActividades;
import io.github.jhoel0521.studyplan.adapter.in.ui.tecnicas.NotificadorSesion;
import io.github.jhoel0521.studyplan.adapter.in.ui.tecnicas.SesionEnCursoActivity;

/**
 * Raíz de composición: crea la base, los adaptadores y los servicios a mano y los expone a las Activities y a los
 * ViewModel. Un único hilo en segundo plano ejecuta todo el acceso a datos.
 *
 * <p>Implementa: HU-01 a HU-04, HU-06 a HU-15, HU-38 y HU-46 (conecta los adaptadores con el núcleo).
 */
public class StudyPlanApplication extends Application {

    private ExecutorService ejecutor;
    private ConfigurarPerfil configurarPerfil;
    private ConsultarPerfil consultarPerfil;
    private ConsultarAjustes consultarAjustes;
    private ConfigurarApariencia configurarApariencia;
    private ConfigurarBloqueo configurarBloqueo;
    private ControlarSesionBloqueo sesionBloqueo;
    private ConsultarEstadoArranque consultarEstadoArranque;
    private ObtenerResumenInicio obtenerResumenInicio;
    private BorrarDatos borrarDatos;
    private RegistrarMateria registrarMateria;
    private EditarMateria editarMateria;
    private ConsultarMateria consultarMateria;
    private ListarMaterias listarMaterias;
    private ArchivarMateria archivarMateria;
    private DesarchivarMateria desarchivarMateria;
    private EliminarMateria eliminarMateria;
    private Reloj reloj;
    private AjustesRepository ajustesRepository;
    private MateriaRepository materiaRepository;
    private SesionRepository sesionRepository;
    private TramoDiarioRepository tramoDiarioRepository;
    private VencerSesionesPendientes vencerSesionesPendientes;
    private PlanificarSesion planificarSesion;
    private RegistrarSesionPasada registrarSesionPasada;
    private CompletarSesion completarSesion;
    private RegistrarSesionTardia registrarSesionTardia;
    private ReprogramarSesion reprogramarSesion;
    private ConsultarSesion consultarSesion;
    private EliminarSesion eliminarSesion;
    private ConsultarCalendario consultarCalendario;
    private PreferenciaVistaCalendario preferenciaVistaCalendario;
    private EventoEstudioRepository eventoEstudioRepository;
    private ControladorNoMolestar controladorNoMolestar;
    private ConfigurarAjustesEstudio configurarAjustesEstudio;
    private ConfigurarRecordatorioPorDefecto configurarRecordatorioPorDefecto;
    private ReprogramarRecordatoriosPendientes reprogramarRecordatoriosPendientes;
    private ProgramadorRecordatorios programadorRecordatorios;
    private IniciarSesion iniciarSesion;
    private PausarReanudarSesion pausarReanudarSesion;
    private DetenerSesion detenerSesion;
    private ConsultarEjecucionSesion consultarEjecucionSesion;
    private RegistrarSalidaApp registrarSalidaApp;
    private RegistrarBloqueoPantalla registrarBloqueoPantalla;
    private RegistrarDesbloqueo registrarDesbloqueo;
    private AlternarNoMolestarSesion alternarNoMolestarSesion;
    private EvaluarRecuperacionSesion evaluarRecuperacionSesion;
    private ResolverRegresoApp resolverRegresoApp;
    private DecidirFinPlanificadoOInvasion decidirFinPlanificadoOInvasion;
    private RepartirTramoSesion repartirTramoSesion;
    private DefinirMetaSemanal definirMetaSemanal;
    private ConsultarProgresoMetas consultarProgresoMetas;
    private EstudiarAhora estudiarAhora;
    private CambiarDeMateria cambiarDeMateria;
    private ConsultarEstadisticas consultarEstadisticas;
    private GenerarReporteEstudio generarReporteEstudio;
    private ExportadorReporte exportadorReporte;
    private ContadorActividades contadorActividades;
    private FabricaViewModels fabrica;

    /** @return la aplicación a la que pertenece el contexto */
    public static StudyPlanApplication de(Context contexto) {
        return (StudyPlanApplication) contexto.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        componer();
        contadorActividades = new ContadorActividades(
                sesionBloqueo, registrarSalidaApp, registrarBloqueoPantalla, registrarDesbloqueo, ejecutor, this);
        registerActivityLifecycleCallbacks(contadorActividades);
        aplicarTemaGuardado();
        // Restaura la notificación de la sesión En curso si el proceso se reinició (HU-49).
        ejecutor.execute(() -> NotificadorSesion.reconciliar(this));
    }

    private void componer() {
        ejecutor = Executors.newSingleThreadExecutor();
        StudyPlanDatabase base = StudyPlanDatabase.abrir(this);
        base.getInvalidationTracker().addObserver(
                new ObservadorProgresoWidget(ejecutor, () -> WidgetProgresoProvider.refrescarTodos(this)));
        PerfilRepositoryRoom perfiles = new PerfilRepositoryRoom(base.perfilDao());
        AjustesRepositoryRoom ajustes = new AjustesRepositoryRoom(base.ajustesDao());
        AlmacenDatosRoom almacen = new AlmacenDatosRoom(base);
        MateriaRepositoryRoom materias = new MateriaRepositoryRoom(base.materiaDao());
        SesionRepositoryRoom sesiones = new SesionRepositoryRoom(base.sesionDao());
        EventoEstudioRepositoryRoom eventos = new EventoEstudioRepositoryRoom(base.eventoEstudioDao());
        TramoDiarioRepositoryRoom tramos = new TramoDiarioRepositoryRoom(base.tramoDiarioDao());
        MetaSemanalRepositoryRoom metas = new MetaSemanalRepositoryRoom(base.metaSemanalDao());
        SesionesDeMateriaRoom sesionesDeMateria = new SesionesDeMateriaRoom(base.sesionDao());
        RelojSistema relojSistema = new RelojSistema();
        DisponibilidadBloqueoBiometrico disponibilidad = new DisponibilidadBloqueoBiometrico(this);
        ControladorNoMolestarSistema noMolestar = new ControladorNoMolestarSistema(this);
        ProgramadorAvisosAlarmManager programadorAlarmas = new ProgramadorAvisosAlarmManager(
                this, ReceptorAvisosSesion.class, SesionEnCursoActivity.class);
        programadorRecordatorios = programadorAlarmas;
        // Cada cambio de estado de la sesión refresca también su notificación (HU-49).
        ProgramadorAvisosConNotificacion programador = new ProgramadorAvisosConNotificacion(
                programadorAlarmas, sesionId -> NotificadorSesion.refrescar(this, sesionId));
        MarcadorSalidaAppPrefs marcadorSalida = new MarcadorSalidaAppPrefs(this);
        ExportadorReporteSistema exportadorSistema = new ExportadorReporteSistema(this);
        exportadorReporte = exportadorSistema;
        SesionBloqueo sesion = new SesionBloqueo();

        sesionBloqueo = sesion;
        reloj = relojSistema;
        ajustesRepository = ajustes;
        materiaRepository = materias;
        sesionRepository = sesiones;
        eventoEstudioRepository = eventos;
        tramoDiarioRepository = tramos;
        controladorNoMolestar = noMolestar;
        vencerSesionesPendientes = new VencerSesionesPendientesService(
                sesiones, eventos, noMolestar, programador, tramos, relojSistema);
        configurarPerfil = new ConfigurarPerfilService(perfiles);
        consultarPerfil = new ConsultarPerfilService(perfiles);
        consultarAjustes = new ConsultarAjustesService(ajustes);
        configurarApariencia = new ConfigurarAparienciaService(ajustes);
        configurarBloqueo = new ConfigurarBloqueoService(ajustes, disponibilidad, sesion);
        consultarEstadoArranque = new ConsultarEstadoArranqueService(perfiles, ajustes, sesion);
        obtenerResumenInicio = new ObtenerResumenInicioService(
                perfiles, sesiones, materias, tramos, metas, relojSistema, vencerSesionesPendientes);
        borrarDatos = new BorrarDatosService(
                almacen, sesion, marcadorSalida, noMolestar, exportadorSistema, programadorAlarmas);
        registrarMateria = new RegistrarMateriaService(materias, metas, relojSistema);
        editarMateria = new EditarMateriaService(materias, metas, relojSistema);
        consultarMateria = new ConsultarMateriaService(materias);
        listarMaterias = new ListarMateriasService(materias, sesiones, metas, tramos, relojSistema);
        archivarMateria = new ArchivarMateriaService(materias);
        desarchivarMateria = new DesarchivarMateriaService(materias);
        eliminarMateria = new EliminarMateriaService(materias, sesionesDeMateria);
        planificarSesion = new PlanificarSesionService(
                sesiones, materias, relojSistema, programadorAlarmas);
        registrarSesionPasada = new RegistrarSesionPasadaService(
                sesiones, materias, tramos, metas, perfiles, relojSistema);
        completarSesion = new CompletarSesionService(sesiones, tramos, relojSistema);
        registrarSesionTardia = new RegistrarSesionTardiaService(
                sesiones, tramos, metas, perfiles, materias, relojSistema);
        reprogramarSesion = new ReprogramarSesionService(
                sesiones, materias, relojSistema, programadorAlarmas);
        consultarSesion = new ConsultarSesionService(
                sesiones, materias, eventos, relojSistema, vencerSesionesPendientes);
        eliminarSesion = new EliminarSesionService(sesiones, programadorAlarmas);
        consultarCalendario = new ConsultarCalendarioService(
                sesiones, materias, tramos, relojSistema, vencerSesionesPendientes);
        preferenciaVistaCalendario = new PreferenciaVistaCalendario(this);

        configurarAjustesEstudio = new ConfigurarAjustesEstudioService(ajustes);
        configurarRecordatorioPorDefecto = new ConfigurarRecordatorioPorDefectoService(ajustes);
        reprogramarRecordatoriosPendientes = new ReprogramarRecordatoriosPendientesService(
                sesiones, materias, programadorAlarmas, relojSistema);
        iniciarSesion = new IniciarSesionService(
                sesiones,
                eventos,
                materias,
                ajustes,
                noMolestar,
                programador,
                marcadorSalida,
                relojSistema,
                programadorAlarmas);
        pausarReanudarSesion = new PausarReanudarSesionService(
                sesiones, eventos, materias, ajustes, noMolestar, programador, relojSistema);
        detenerSesion = new DetenerSesionService(
                sesiones, eventos, noMolestar, programador, marcadorSalida, tramos, metas, perfiles, materias, relojSistema);
        consultarEjecucionSesion = new ConsultarEjecucionSesionService(
                sesiones, eventos, materias, ajustes, noMolestar, relojSistema);
        registrarSalidaApp = new RegistrarSalidaAppService(
                sesiones, eventos, materias, marcadorSalida, relojSistema);
        registrarBloqueoPantalla = new RegistrarBloqueoPantallaService(sesiones, eventos, relojSistema);
        registrarDesbloqueo = new RegistrarDesbloqueoService(sesiones, eventos, relojSistema);
        alternarNoMolestarSesion = new AlternarNoMolestarSesionService(sesiones, eventos, ajustes, noMolestar);
        evaluarRecuperacionSesion = new EvaluarRecuperacionSesionService(
                sesiones, eventos, materias, noMolestar, programador, marcadorSalida, tramos, metas, perfiles, relojSistema);
        resolverRegresoApp = new ResolverRegresoAppService(
                sesiones, eventos, materias, ajustes, noMolestar, programador, marcadorSalida, tramos, metas, perfiles, relojSistema);
        decidirFinPlanificadoOInvasion = new DecidirFinPlanificadoOInvasionService(
                sesiones,
                eventos,
                materias,
                ajustes,
                noMolestar,
                programador,
                marcadorSalida,
                detenerSesion,
                tramos,
                relojSistema,
                programadorAlarmas);
        repartirTramoSesion = new RepartirTramoSesionService(
                sesiones,
                eventos,
                noMolestar,
                programador,
                marcadorSalida,
                tramos,
                relojSistema,
                programadorAlarmas);
        definirMetaSemanal = new DefinirMetaSemanalService(metas, relojSistema);
        consultarProgresoMetas = new ConsultarProgresoMetasService(
                metas, perfiles, materias, tramos, relojSistema);
        estudiarAhora = new EstudiarAhoraService(
                sesiones, eventos, materias, ajustes, noMolestar, programador, marcadorSalida, relojSistema);
        cambiarDeMateria = new CambiarDeMateriaService(
                sesiones, eventos, materias, ajustes, noMolestar, programador, marcadorSalida, tramos, relojSistema);
        consultarEstadisticas = new ConsultarEstadisticasService(
                vencerSesionesPendientes, tramos, sesiones, materias, eventos, relojSistema);
        generarReporteEstudio = new GenerarReporteEstudioService(
                vencerSesionesPendientes, tramos, sesiones, materias, relojSistema);
        fabrica = new FabricaViewModels(this);
    }

    /** Lee el tema y el idioma guardados fuera del hilo principal y los aplica en el principal. */
    private void aplicarTemaGuardado() {
        Handler principal = new Handler(Looper.getMainLooper());
        ejecutor.execute(() -> {
            Ajustes guardados = consultarAjustes.ejecutar();
            principal.post(() -> {
                AplicadorApariencia.aplicarTema(guardados.tema());
                AplicadorApariencia.aplicarIdioma(this, guardados.idioma());
            });
        });
    }

    public java.util.concurrent.Executor ejecutor() {
        return ejecutor;
    }

    public <T> java.util.concurrent.Future<T> enviarTarea(java.util.concurrent.Callable<T> tarea) {
        return ejecutor.submit(tarea);
    }

    public java.util.concurrent.Future<?> enviarTarea(Runnable tarea) {
        return ejecutor.submit(tarea);
    }

    public FabricaViewModels fabrica() {
        return fabrica;
    }

    public ConfigurarPerfil configurarPerfil() {
        return configurarPerfil;
    }

    public ConsultarPerfil consultarPerfil() {
        return consultarPerfil;
    }

    public ConsultarAjustes consultarAjustes() {
        return consultarAjustes;
    }

    public ConfigurarApariencia configurarApariencia() {
        return configurarApariencia;
    }

    public ConfigurarBloqueo configurarBloqueo() {
        return configurarBloqueo;
    }

    public ControlarSesionBloqueo sesionBloqueo() {
        return sesionBloqueo;
    }

    public ConsultarEstadoArranque consultarEstadoArranque() {
        return consultarEstadoArranque;
    }

    public ObtenerResumenInicio obtenerResumenInicio() {
        return obtenerResumenInicio;
    }

    public BorrarDatos borrarDatos() {
        return borrarDatos;
    }

    public RegistrarMateria registrarMateria() {
        return registrarMateria;
    }

    public EditarMateria editarMateria() {
        return editarMateria;
    }

    public ConsultarMateria consultarMateria() {
        return consultarMateria;
    }

    public ListarMaterias listarMaterias() {
        return listarMaterias;
    }

    public ArchivarMateria archivarMateria() {
        return archivarMateria;
    }

    public DesarchivarMateria desarchivarMateria() {
        return desarchivarMateria;
    }

    public EliminarMateria eliminarMateria() {
        return eliminarMateria;
    }

    public Reloj reloj() {
        return reloj;
    }

    public VencerSesionesPendientes vencerSesionesPendientes() {
        return vencerSesionesPendientes;
    }

    public PlanificarSesion planificarSesion() {
        return planificarSesion;
    }

    public RegistrarSesionPasada registrarSesionPasada() {
        return registrarSesionPasada;
    }

    public CompletarSesion completarSesion() {
        return completarSesion;
    }

    public RegistrarSesionTardia registrarSesionTardia() {
        return registrarSesionTardia;
    }

    public ReprogramarSesion reprogramarSesion() {
        return reprogramarSesion;
    }

    public ConsultarSesion consultarSesion() {
        return consultarSesion;
    }

    public EliminarSesion eliminarSesion() {
        return eliminarSesion;
    }

    public ConsultarCalendario consultarCalendario() {
        return consultarCalendario;
    }

    public PreferenciaVistaCalendario preferenciaVistaCalendario() {
        return preferenciaVistaCalendario;
    }

    public EventoEstudioRepository eventoEstudioRepository() {
        return eventoEstudioRepository;
    }

    public ControladorNoMolestar controladorNoMolestar() {
        return controladorNoMolestar;
    }

    public ConfigurarAjustesEstudio configurarAjustesEstudio() {
        return configurarAjustesEstudio;
    }

    public ConfigurarRecordatorioPorDefecto configurarRecordatorioPorDefecto() {
        return configurarRecordatorioPorDefecto;
    }

    public ReprogramarRecordatoriosPendientes reprogramarRecordatoriosPendientes() {
        return reprogramarRecordatoriosPendientes;
    }

    public ProgramadorRecordatorios programadorRecordatorios() {
        return programadorRecordatorios;
    }

    public IniciarSesion iniciarSesion() {
        return iniciarSesion;
    }

    public PausarReanudarSesion pausarReanudarSesion() {
        return pausarReanudarSesion;
    }

    public DetenerSesion detenerSesion() {
        return detenerSesion;
    }

    public ConsultarEjecucionSesion consultarEjecucionSesion() {
        return consultarEjecucionSesion;
    }

    public EvaluarRecuperacionSesion evaluarRecuperacionSesion() {
        return evaluarRecuperacionSesion;
    }

    public ResolverRegresoApp resolverRegresoApp() {
        return resolverRegresoApp;
    }

    public DecidirFinPlanificadoOInvasion decidirFinPlanificadoOInvasion() {
        return decidirFinPlanificadoOInvasion;
    }

    public RepartirTramoSesion repartirTramoSesion() {
        return repartirTramoSesion;
    }

    public DefinirMetaSemanal definirMetaSemanal() {
        return definirMetaSemanal;
    }

    public ConsultarProgresoMetas consultarProgresoMetas() {
        return consultarProgresoMetas;
    }

    public EstudiarAhora estudiarAhora() {
        return estudiarAhora;
    }

    public CambiarDeMateria cambiarDeMateria() {
        return cambiarDeMateria;
    }

    public ConsultarEstadisticas consultarEstadisticas() {
        return consultarEstadisticas;
    }

    public GenerarReporteEstudio generarReporteEstudio() {
        return generarReporteEstudio;
    }

    public ExportadorReporte exportadorReporte() {
        return exportadorReporte;
    }

    public AlternarNoMolestarSesion alternarNoMolestarSesion() {
        return alternarNoMolestarSesion;
    }

    public ContadorActividades contadorActividades() {
        return contadorActividades;
    }

    /** Acceso interno al repositorio de sesiones para sembrar escenarios de prueba instrumentados. */
    public SesionRepository sesionRepository() {
        return sesionRepository;
    }

    public AjustesRepository ajustesRepository() {
        return ajustesRepository;
    }

    public MateriaRepository materiaRepository() {
        return materiaRepository;
    }

    public TramoDiarioRepository tramoDiarioRepository() {
        return tramoDiarioRepository;
    }
}
