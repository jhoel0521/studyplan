package io.github.jhoel0521.studyplan.adapter.in.ui;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.jhoel0521.studyplan.adapter.out.persistence.AjustesRepositoryRoom;
import io.github.jhoel0521.studyplan.adapter.out.persistence.AlmacenDatosRoom;
import io.github.jhoel0521.studyplan.adapter.out.persistence.MateriaRepositoryRoom;
import io.github.jhoel0521.studyplan.adapter.out.persistence.PerfilRepositoryRoom;
import io.github.jhoel0521.studyplan.adapter.out.persistence.RelojSistema;
import io.github.jhoel0521.studyplan.adapter.out.persistence.SesionRepositoryRoom;
import io.github.jhoel0521.studyplan.adapter.out.persistence.SesionesDeMateriaRoom;
import io.github.jhoel0521.studyplan.adapter.out.persistence.StudyPlanDatabase;
import io.github.jhoel0521.studyplan.adapter.out.security.DisponibilidadBloqueoBiometrico;
import io.github.jhoel0521.studyplan.application.port.in.ArchivarMateria;
import io.github.jhoel0521.studyplan.application.port.in.BorrarDatos;
import io.github.jhoel0521.studyplan.application.port.in.CompletarSesion;
import io.github.jhoel0521.studyplan.application.port.in.ConfigurarApariencia;
import io.github.jhoel0521.studyplan.application.port.in.ConfigurarBloqueo;
import io.github.jhoel0521.studyplan.application.port.in.ConfigurarPerfil;
import io.github.jhoel0521.studyplan.application.port.in.ConsultarAjustes;
import io.github.jhoel0521.studyplan.application.port.in.ConsultarCalendario;
import io.github.jhoel0521.studyplan.application.port.in.ConsultarEstadoArranque;
import io.github.jhoel0521.studyplan.application.port.in.ConsultarMateria;
import io.github.jhoel0521.studyplan.application.port.in.ConsultarPerfil;
import io.github.jhoel0521.studyplan.application.port.in.ConsultarSesion;
import io.github.jhoel0521.studyplan.application.port.in.ControlarSesionBloqueo;
import io.github.jhoel0521.studyplan.application.port.in.DesarchivarMateria;
import io.github.jhoel0521.studyplan.application.port.in.EditarMateria;
import io.github.jhoel0521.studyplan.application.port.in.EliminarMateria;
import io.github.jhoel0521.studyplan.application.port.in.EliminarSesion;
import io.github.jhoel0521.studyplan.application.port.in.ListarMaterias;
import io.github.jhoel0521.studyplan.application.port.in.ObtenerResumenInicio;
import io.github.jhoel0521.studyplan.application.port.in.PlanificarSesion;
import io.github.jhoel0521.studyplan.application.port.in.RegistrarMateria;
import io.github.jhoel0521.studyplan.application.port.in.RegistrarSesionPasada;
import io.github.jhoel0521.studyplan.application.port.in.RegistrarSesionTardia;
import io.github.jhoel0521.studyplan.application.port.in.ReprogramarSesion;
import io.github.jhoel0521.studyplan.application.port.in.VencerSesionesPendientes;
import io.github.jhoel0521.studyplan.application.port.out.Reloj;
import io.github.jhoel0521.studyplan.application.port.out.SesionRepository;
import io.github.jhoel0521.studyplan.application.service.ArchivarMateriaService;
import io.github.jhoel0521.studyplan.application.service.BorrarDatosService;
import io.github.jhoel0521.studyplan.application.service.CompletarSesionService;
import io.github.jhoel0521.studyplan.application.service.ConfigurarAparienciaService;
import io.github.jhoel0521.studyplan.application.service.ConfigurarBloqueoService;
import io.github.jhoel0521.studyplan.application.service.ConfigurarPerfilService;
import io.github.jhoel0521.studyplan.application.service.ConsultarAjustesService;
import io.github.jhoel0521.studyplan.application.service.ConsultarCalendarioService;
import io.github.jhoel0521.studyplan.application.service.ConsultarEstadoArranqueService;
import io.github.jhoel0521.studyplan.application.service.ConsultarMateriaService;
import io.github.jhoel0521.studyplan.application.service.ConsultarPerfilService;
import io.github.jhoel0521.studyplan.application.service.ConsultarSesionService;
import io.github.jhoel0521.studyplan.application.service.DesarchivarMateriaService;
import io.github.jhoel0521.studyplan.application.service.EditarMateriaService;
import io.github.jhoel0521.studyplan.application.service.EliminarMateriaService;
import io.github.jhoel0521.studyplan.application.service.EliminarSesionService;
import io.github.jhoel0521.studyplan.application.service.ListarMateriasService;
import io.github.jhoel0521.studyplan.application.service.ObtenerResumenInicioService;
import io.github.jhoel0521.studyplan.application.service.PlanificarSesionService;
import io.github.jhoel0521.studyplan.application.service.RegistrarMateriaService;
import io.github.jhoel0521.studyplan.application.service.RegistrarSesionPasadaService;
import io.github.jhoel0521.studyplan.application.service.RegistrarSesionTardiaService;
import io.github.jhoel0521.studyplan.application.service.ReprogramarSesionService;
import io.github.jhoel0521.studyplan.application.service.SesionBloqueo;
import io.github.jhoel0521.studyplan.application.service.VencerSesionesPendientesService;
import io.github.jhoel0521.studyplan.domain.Ajustes;

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
    private SesionRepository sesionRepository;
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
    private FabricaViewModels fabrica;

    /** @return la aplicación a la que pertenece el contexto */
    public static StudyPlanApplication de(Context contexto) {
        return (StudyPlanApplication) contexto.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        componer();
        registerActivityLifecycleCallbacks(new ContadorActividades(sesionBloqueo));
        aplicarTemaGuardado();
    }

    private void componer() {
        ejecutor = Executors.newSingleThreadExecutor();
        StudyPlanDatabase base = StudyPlanDatabase.abrir(this);
        PerfilRepositoryRoom perfiles = new PerfilRepositoryRoom(base.perfilDao());
        AjustesRepositoryRoom ajustes = new AjustesRepositoryRoom(base.ajustesDao());
        AlmacenDatosRoom almacen = new AlmacenDatosRoom(base);
        MateriaRepositoryRoom materias = new MateriaRepositoryRoom(base.materiaDao());
        SesionRepositoryRoom sesiones = new SesionRepositoryRoom(base.sesionDao());
        SesionesDeMateriaRoom sesionesDeMateria = new SesionesDeMateriaRoom(base.sesionDao());
        RelojSistema relojSistema = new RelojSistema();
        DisponibilidadBloqueoBiometrico disponibilidad = new DisponibilidadBloqueoBiometrico(this);
        SesionBloqueo sesion = new SesionBloqueo();

        sesionBloqueo = sesion;
        reloj = relojSistema;
        sesionRepository = sesiones;
        vencerSesionesPendientes = new VencerSesionesPendientesService(sesiones, relojSistema);
        configurarPerfil = new ConfigurarPerfilService(perfiles);
        consultarPerfil = new ConsultarPerfilService(perfiles);
        consultarAjustes = new ConsultarAjustesService(ajustes);
        configurarApariencia = new ConfigurarAparienciaService(ajustes);
        configurarBloqueo = new ConfigurarBloqueoService(ajustes, disponibilidad, sesion);
        consultarEstadoArranque = new ConsultarEstadoArranqueService(perfiles, ajustes, sesion);
        obtenerResumenInicio = new ObtenerResumenInicioService(
                perfiles, sesiones, materias, relojSistema, vencerSesionesPendientes);
        borrarDatos = new BorrarDatosService(almacen, sesion);
        registrarMateria = new RegistrarMateriaService(materias);
        editarMateria = new EditarMateriaService(materias);
        consultarMateria = new ConsultarMateriaService(materias);
        listarMaterias = new ListarMateriasService(materias, sesiones);
        archivarMateria = new ArchivarMateriaService(materias);
        desarchivarMateria = new DesarchivarMateriaService(materias);
        eliminarMateria = new EliminarMateriaService(materias, sesionesDeMateria);
        planificarSesion = new PlanificarSesionService(sesiones, materias, relojSistema);
        registrarSesionPasada = new RegistrarSesionPasadaService(sesiones, materias, relojSistema);
        completarSesion = new CompletarSesionService(sesiones, relojSistema);
        registrarSesionTardia = new RegistrarSesionTardiaService(sesiones, relojSistema);
        reprogramarSesion = new ReprogramarSesionService(sesiones, materias, relojSistema);
        consultarSesion = new ConsultarSesionService(
                sesiones, materias, relojSistema, vencerSesionesPendientes);
        eliminarSesion = new EliminarSesionService(sesiones);
        consultarCalendario = new ConsultarCalendarioService(
                sesiones, materias, relojSistema, vencerSesionesPendientes);
        preferenciaVistaCalendario = new PreferenciaVistaCalendario(this);
        fabrica = new FabricaViewModels(this);
    }

    /** Lee el tema y el idioma guardados fuera del hilo principal y los aplica en el principal. */
    private void aplicarTemaGuardado() {
        Handler principal = new Handler(Looper.getMainLooper());
        ejecutor.execute(() -> {
            Ajustes guardados = consultarAjustes.ejecutar();
            principal.post(() -> {
                AplicadorApariencia.aplicarTema(guardados.tema());
                AplicadorApariencia.aplicarIdioma(guardados.idioma());
            });
        });
    }

    public ExecutorService ejecutor() {
        return ejecutor;
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

    /** Acceso interno al repositorio de sesiones para sembrar escenarios de prueba instrumentados. */
    SesionRepository sesionRepository() {
        return sesionRepository;
    }
}
