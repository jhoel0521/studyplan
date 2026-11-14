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
import io.github.jhoel0521.studyplan.adapter.out.persistence.SinSesionesDeMateria;
import io.github.jhoel0521.studyplan.adapter.out.persistence.StudyPlanDatabase;
import io.github.jhoel0521.studyplan.adapter.out.security.DisponibilidadBloqueoBiometrico;
import io.github.jhoel0521.studyplan.application.port.in.ArchivarMateria;
import io.github.jhoel0521.studyplan.application.port.in.BorrarDatos;
import io.github.jhoel0521.studyplan.application.port.in.ConfigurarApariencia;
import io.github.jhoel0521.studyplan.application.port.in.ConfigurarBloqueo;
import io.github.jhoel0521.studyplan.application.port.in.ConfigurarPerfil;
import io.github.jhoel0521.studyplan.application.port.in.ConsultarAjustes;
import io.github.jhoel0521.studyplan.application.port.in.ConsultarEstadoArranque;
import io.github.jhoel0521.studyplan.application.port.in.ConsultarMateria;
import io.github.jhoel0521.studyplan.application.port.in.ConsultarPerfil;
import io.github.jhoel0521.studyplan.application.port.in.ControlarSesionBloqueo;
import io.github.jhoel0521.studyplan.application.port.in.DesarchivarMateria;
import io.github.jhoel0521.studyplan.application.port.in.EditarMateria;
import io.github.jhoel0521.studyplan.application.port.in.EliminarMateria;
import io.github.jhoel0521.studyplan.application.port.in.ListarMaterias;
import io.github.jhoel0521.studyplan.application.port.in.ObtenerResumenInicio;
import io.github.jhoel0521.studyplan.application.port.in.RegistrarMateria;
import io.github.jhoel0521.studyplan.application.service.ArchivarMateriaService;
import io.github.jhoel0521.studyplan.application.service.BorrarDatosService;
import io.github.jhoel0521.studyplan.application.service.ConfigurarAparienciaService;
import io.github.jhoel0521.studyplan.application.service.ConfigurarBloqueoService;
import io.github.jhoel0521.studyplan.application.service.ConfigurarPerfilService;
import io.github.jhoel0521.studyplan.application.service.ConsultarAjustesService;
import io.github.jhoel0521.studyplan.application.service.ConsultarEstadoArranqueService;
import io.github.jhoel0521.studyplan.application.service.ConsultarMateriaService;
import io.github.jhoel0521.studyplan.application.service.ConsultarPerfilService;
import io.github.jhoel0521.studyplan.application.service.DesarchivarMateriaService;
import io.github.jhoel0521.studyplan.application.service.EditarMateriaService;
import io.github.jhoel0521.studyplan.application.service.EliminarMateriaService;
import io.github.jhoel0521.studyplan.application.service.ListarMateriasService;
import io.github.jhoel0521.studyplan.application.service.ObtenerResumenInicioService;
import io.github.jhoel0521.studyplan.application.service.RegistrarMateriaService;
import io.github.jhoel0521.studyplan.application.service.SesionBloqueo;
import io.github.jhoel0521.studyplan.domain.Ajustes;

/**
 * Raíz de composición: crea la base, los adaptadores y los servicios a mano y los expone a las Activities y a los
 * ViewModel. Un único hilo en segundo plano ejecuta todo el acceso a datos.
 *
 * <p>Implementa: HU-01, HU-02, HU-03, HU-04 y HU-38 (conecta los adaptadores con el núcleo).
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
        DisponibilidadBloqueoBiometrico disponibilidad = new DisponibilidadBloqueoBiometrico(this);
        SesionBloqueo sesion = new SesionBloqueo();

        sesionBloqueo = sesion;
        configurarPerfil = new ConfigurarPerfilService(perfiles);
        consultarPerfil = new ConsultarPerfilService(perfiles);
        consultarAjustes = new ConsultarAjustesService(ajustes);
        configurarApariencia = new ConfigurarAparienciaService(ajustes);
        configurarBloqueo = new ConfigurarBloqueoService(ajustes, disponibilidad, sesion);
        consultarEstadoArranque = new ConsultarEstadoArranqueService(perfiles, ajustes, sesion);
        obtenerResumenInicio = new ObtenerResumenInicioService(perfiles);
        borrarDatos = new BorrarDatosService(almacen, sesion);
        registrarMateria = new RegistrarMateriaService(materias);
        editarMateria = new EditarMateriaService(materias);
        consultarMateria = new ConsultarMateriaService(materias);
        listarMaterias = new ListarMateriasService(materias);
        archivarMateria = new ArchivarMateriaService(materias);
        desarchivarMateria = new DesarchivarMateriaService(materias);
        eliminarMateria = new EliminarMateriaService(materias, new SinSesionesDeMateria());
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
}
