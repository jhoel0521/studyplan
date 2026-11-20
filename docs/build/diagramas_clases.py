"""Diagramas dibujados desde el código y desde el esquema de Room: clases, modelo lógico y secuencia.

Nada de lo que se muestra aquí se escribe a mano: las firmas de los métodos y los campos salen de los fuentes Java
(`java_fuentes.py`) y las tablas, de `app/schemas/.../<versión>.json`. Así el documento no se desfasa del código;
`cotejo_clases.py` lo comprueba.

Uso: python docs/build/diagramas_clases.py   (también lo invoca docs/build/diagramas.py)
Salida: docs/documento/recursos/diagramas/*.png y las secciones 4.2.1 y 4.2.2 del capítulo 4.
"""
import json
import re
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
import diagramas as dg  # noqa: E402
import java_fuentes as jf  # noqa: E402

RAIZ = dg.ROOT
CAPITULO_4 = RAIZ / "docs" / "documento" / "capitulos" / "04-diseno-del-software.md"
ESQUEMAS = RAIZ / "app" / "schemas" / "io.github.jhoel0521.studyplan.adapter.out.persistence.StudyPlanDatabase"

# ---------------------------------------------------------------- clases por paquete y por change
# add-configuracion-inicial
PRESENTACION_CONFIG = ["ActividadBase", "ActividadProtegida", "MainActivity", "PerfilInicialActivity",
                       "AjustesActivity", "AutenticacionActivity", "BorrarDatosActivity",
                       "PerfilInicialViewModel", "InicioViewModel", "ProteccionViewModel", "AjustesViewModel",
                       "BorrarDatosViewModel", "StudyPlanApplication", "FabricaViewModels", "ContadorActividades",
                       "AplicadorApariencia", "FormatoHoras"]
ENTRADA_CONFIG = ["BorrarDatos", "ConfigurarApariencia", "ConfigurarBloqueo", "ConfigurarPerfil", "ConsultarAjustes",
                  "ConsultarEstadoArranque", "ConsultarPerfil", "ControlarSesionBloqueo", "ObtenerResumenInicio"]
SERVICIOS_CONFIG = ["BorrarDatosService", "ConfigurarAparienciaService", "ConfigurarBloqueoService",
                    "ConfigurarPerfilService", "ConsultarAjustesService", "ConsultarEstadoArranqueService",
                    "ConsultarPerfilService", "ObtenerResumenInicioService", "SesionBloqueo"]
SALIDA_CONFIG = ["AjustesRepository", "AlmacenDatos", "DisponibilidadBloqueo", "PerfilRepository"]
DOMINIO_CONFIG = ["Ajustes", "AlcanceBorrado", "ErrorValidacion", "EstadoArranque", "Idioma", "Perfil", "Resultado",
                  "ResumenInicio", "Tema"]
PERSISTENCIA_CONFIG = ["AjustesDao", "AjustesEntity", "AjustesRepositoryRoom", "AlmacenDatosRoom", "Mapeadores",
                       "PerfilDao", "PerfilEntity", "PerfilRepositoryRoom", "StudyPlanDatabase"]
SEGURIDAD_CONFIG = ["DisponibilidadBloqueoBiometrico"]

# add-gestion-materias
PRESENTACION_MATERIAS = ["DestinoPrincipal", "MateriasActivity", "MateriaFormularioActivity", "ListaMateriasViewModel",
                         "MateriaFormularioViewModel", "MateriasAdapter", "FilaMateria", "EventoMateria",
                         "MensajesDeError", "SelectorColorDialogo", "ColorHex", "GrupoBotones",
                         # aplicar-linea-grafica-indigo (HU-03): color de materia como acento
                         "ColorMateria", "AcentoMateria"]
ENTRADA_MATERIAS = ["ArchivarMateria", "ConsultarMateria", "DesarchivarMateria", "EditarMateria", "EliminarMateria",
                    "ListarMaterias", "RegistrarMateria"]
SERVICIOS_MATERIAS = ["ArchivarMateriaService", "ConsultarMateriaService", "DesarchivarMateriaService",
                      "EditarMateriaService", "EliminarMateriaService", "ListarMateriasService", "NombresDeMateria",
                      "RegistrarMateriaService"]
SALIDA_MATERIAS = ["MateriaRepository", "SesionesDeMateria"]
DOMINIO_MATERIAS = ["ListadoMaterias", "Materia", "ResumenMateria"]
PERSISTENCIA_MATERIAS = ["MateriaDao", "MateriaEntity", "MateriaRepositoryRoom", "Migraciones"]

# add-sesiones-estudio
PRESENTACION_SESIONES = ["EstadoSesionVisual", "EventoSesion", "FormatoSesion", "SesionDetalleActivity",
                         "SesionDetalleViewModel", "SesionFormularioActivity", "SesionFormularioViewModel",
                         "SesionInicioAdapter", "RolColorEstado"]
ENTRADA_SESIONES = ["CompletarSesion", "ConsultarSesion", "EliminarSesion", "PlanificarSesion",
                    "RegistrarSesionPasada", "RegistrarSesionTardia", "ReprogramarSesion", "VencerSesionesPendientes"]
SERVICIOS_SESIONES = ["CompletarSesionService", "ConsultarSesionService", "EliminarSesionService",
                      "PlanificarSesionService", "RegistrarSesionPasadaService", "RegistrarSesionTardiaService",
                      "ReprogramarSesionService", "ValidadorSesiones", "VencerSesionesPendientesService"]
SALIDA_SESIONES = ["Reloj", "SesionRepository", "TramoDiarioRepository"]
DOMINIO_SESIONES = ["CodigoEstadoSesion", "ConflictoHorario", "DetalleSesion", "DetalleSesionInicio",
                    "EstadoCompletada", "EstadoCompletadaTardia", "EstadoEnCurso", "EstadoNoRealizada",
                    "EstadoPendiente", "EstadoReprogramada", "EstadoSesion", "Fecha", "Hora", "Instante",
                    "RepartidorPorDia", "ResultadoSesion", "SesionEstudio"]
PERSISTENCIA_SESIONES = ["RelojSistema", "SesionDao", "SesionEntity", "SesionRepositoryRoom",
                         "SesionesDeMateriaRoom", "TramoDiarioDao", "TramoDiarioEntity",
                         "TramoDiarioRepositoryRoom"]

# add-calendario-estudio
PRESENTACION_CALENDARIO = ["CalendarioActivity", "CalendarioMesAdapter", "CalendarioViewModel",
                           "PreferenciaVistaCalendario"]
ENTRADA_CALENDARIO = ["ConsultarCalendario"]
SERVICIOS_CALENDARIO = ["ConsultarCalendarioService"]
DOMINIO_CALENDARIO = ["BloqueHorarioCalendario", "CalculadorRangoHorario", "CeldaDiaMes", "DatosCalendario",
                      "FiltroCalendario", "ItemSesionCalendario", "PeriodoCalendario", "ResumenPeriodoCalendario",
                      "VistaCalendario"]

# add-tecnicas-estudio
PRESENTACION_TECNICAS = ["DialogoRecuperacionSesion", "EventoEstudioAdapter", "NotificadorSesion",
                         "ReceptorAvisosSesion", "RepartoSesionActivity", "RepartoSesionViewModel",
                         "SesionEnCursoActivity", "SesionEnCursoViewModel"]
ENTRADA_TECNICAS = ["ConfigurarAjustesEstudio", "ConsultarEjecucionSesion", "DecidirFinPlanificadoOInvasion",
                    "DetenerSesion", "EvaluarRecuperacionSesion", "IniciarSesion", "PausarReanudarSesion",
                    "RegistrarSalidaApp", "RepartirTramoSesion", "ResolverRegresoApp"]
SERVICIOS_TECNICAS = ["ConfigurarAjustesEstudioService", "ConsultarEjecucionSesionService",
                      "DecidirFinPlanificadoOInvasionService", "DetenerSesionService", "EjecucionHitos",
                      "EvaluarRecuperacionSesionService", "IniciarSesionService", "PausarReanudarSesionService",
                      "RegistrarSalidaAppService", "RepartirTramoSesionService", "ResolverRegresoAppService"]
SALIDA_TECNICAS = ["ControladorNoMolestar", "EventoEstudioRepository", "MarcadorSalidaApp",
                   "ProgramadorAvisosSesion"]
DOMINIO_TECNICAS = ["CalculadorPomodoro", "CalculadorRepartoTramo", "CalculadorTiempoReal", "CandidataInvasion",
                    "ConfiguracionPomodoro", "DiagnosticoRegresoApp", "EstadoPomodoroCalculado", "EventoEstudio",
                    "FasePomodoro", "IntervaloEfectivo", "PreferenciaNoMolestar", "ResultadoReparto",
                    "ResumenEjecucionSesion", "TecnicaEstudio", "TipoEventoEstudio"]
PERSISTENCIA_TECNICAS = ["EventoEstudioDao", "EventoEstudioEntity", "EventoEstudioRepositoryRoom"]
SISTEMA_TECNICAS = ["ControladorNoMolestarSistema", "MarcadorSalidaAppPrefs", "ProgramadorAvisosAlarmManager"]

PRESENTACION_METAS = ["FelicitacionMetasActivity", "HistorialMetasActivity", "HistorialMetasAdapter",
                      "HistorialMetasViewModel", "SelectorEstudioBottomSheet"]
ENTRADA_METAS = ["CambiarDeMateria", "ConsultarProgresoMetas", "DefinirMetaSemanal", "EstudiarAhora"]
SERVICIOS_METAS = ["CambiarDeMateriaService", "ConsultarProgresoMetasService", "DefinirMetaSemanalService",
                   "DetectorFelicitacionMetas", "EstudiarAhoraService"]
SALIDA_METAS = ["MetaSemanalRepository"]
DOMINIO_METAS = ["CalculadorProgresoMetas", "CierreSesion", "MetaAlcanzada", "MetaSemanal", "ProgresoMeta"]
PERSISTENCIA_METAS = ["MetaSemanalDao", "MetaSemanalEntity", "MetaSemanalRepositoryRoom"]

PRESENTACION_ESTADISTICAS = ["EstadisticasActivity", "EstadisticasAdapter", "EstadisticasViewModel", "TopicosUi"]
ENTRADA_ESTADISTICAS = ["ConsultarEstadisticas"]
SERVICIOS_ESTADISTICAS = ["ConsultarEstadisticasService"]
DOMINIO_ESTADISTICAS = ["CalculadorEstadisticas", "ConcentracionPeriodo", "DesgloseMateriaPeriodo", "EstadisticasPeriodo", "PeriodoEstadisticas", "ResumenTopico", "SalidaRegistrada", "TopicoEventoEstudio"]

PRESENTACION_REPORTES = ["ReportesActivity", "ReportesAdapter", "ReportesViewModel"]
ENTRADA_REPORTES = ["GenerarReporteEstudio"]
SERVICIOS_REPORTES = ["GenerarReporteEstudioService"]
SALIDA_REPORTES = ["ExportadorReporte", "ResultadoExportacion"]
DOMINIO_REPORTES = ["FormatoExportacionReporte", "GeneradorCsvReporte", "ReporteEstudio"]
SISTEMA_REPORTES = ["ExportadorReporteSistema"]

# add-recordatorios
ENTRADA_RECORDATORIOS = ["ConfigurarRecordatorioPorDefecto", "ReprogramarRecordatoriosPendientes"]
SERVICIOS_RECORDATORIOS = ["ConfigurarRecordatorioPorDefectoService", "ReprogramarRecordatoriosPendientesService"]
SALIDA_RECORDATORIOS = ["ProgramadorRecordatorios"]
DOMINIO_RECORDATORIOS = ["AntelacionRecordatorio", "AvisoRecordatorioPendiente"]

# Paquete (carpeta bajo io/github/jhoel0521/studyplan) -> todas las clases que deben aparecer en algún diagrama
CLASES_POR_PAQUETE = {
    "adapter/in/ui": PRESENTACION_CONFIG + PRESENTACION_MATERIAS + PRESENTACION_SESIONES + PRESENTACION_CALENDARIO + PRESENTACION_TECNICAS + PRESENTACION_METAS + PRESENTACION_ESTADISTICAS + PRESENTACION_REPORTES,
    "application/port/in": ENTRADA_CONFIG + ENTRADA_MATERIAS + ENTRADA_SESIONES + ENTRADA_CALENDARIO + ENTRADA_TECNICAS + ENTRADA_METAS + ENTRADA_ESTADISTICAS + ENTRADA_REPORTES + ENTRADA_RECORDATORIOS,
    "application/service": SERVICIOS_CONFIG + SERVICIOS_MATERIAS + SERVICIOS_SESIONES + SERVICIOS_CALENDARIO + SERVICIOS_TECNICAS + SERVICIOS_METAS + SERVICIOS_ESTADISTICAS + SERVICIOS_REPORTES + SERVICIOS_RECORDATORIOS,
    "application/port/out": SALIDA_CONFIG + SALIDA_MATERIAS + SALIDA_SESIONES + SALIDA_TECNICAS + SALIDA_METAS + SALIDA_REPORTES + SALIDA_RECORDATORIOS,
    "domain": DOMINIO_CONFIG + DOMINIO_MATERIAS + DOMINIO_SESIONES + DOMINIO_CALENDARIO + DOMINIO_TECNICAS + DOMINIO_METAS + DOMINIO_ESTADISTICAS + DOMINIO_REPORTES + DOMINIO_RECORDATORIOS,
    "adapter/out/persistence": PERSISTENCIA_CONFIG + PERSISTENCIA_MATERIAS + PERSISTENCIA_SESIONES + PERSISTENCIA_TECNICAS + PERSISTENCIA_METAS,
    "adapter/out/security": SEGURIDAD_CONFIG,
    "adapter/out/system": SISTEMA_TECNICAS + SISTEMA_REPORTES,
}


def caja_con_metodos(clase, x, y, w, estereotipo=None, con_campos=False):
    """Caja UML con los campos y los métodos reales de la clase, tomados del código."""
    atributos = jf.campos(clase) if con_campos else []
    return dg.UMLBox(clase, atributos, jf.metodos(clase), x, y, w=w, stereotype=estereotipo)


def caja_de_paquete(titulo, clases, x, y, w, estereotipo="paquete"):
    return dg.UMLBox(titulo, list(clases), None, x, y, w=w, stereotype=estereotipo)


def alto(cajas):
    return max(c.h for c in cajas)


# ---------------------------------------------------------------- clases de add-configuracion-inicial
def clases_configuracion():
    """Presentación con los métodos de cada clase (onCreate y listeners), núcleo e infraestructura por paquete."""
    W, H = 3240, 4200
    img = Image.new("RGB", (W, H), dg.WHITE)
    d = ImageDraw.Draw(img)
    xs, ancho = [40, 840, 1640, 2440], 760
    d.text((40, 20), "1. Presentación · adapter.in.ui (MVVM: Activity + ViewModel)", font=dg.font(34, True), fill=dg.BLACK)

    fila1 = [caja_con_metodos("ActividadBase", xs[0], 80, ancho, "abstract"),
             caja_con_metodos("ActividadProtegida", xs[1], 80, ancho, "abstract"),
             caja_con_metodos("MainActivity", xs[2], 80, ancho),
             caja_con_metodos("PerfilInicialActivity", xs[3], 80, ancho)]
    y2 = 80 + alto(fila1) + 70
    fila2 = [caja_con_metodos("AjustesActivity", xs[0], y2, ancho),
             caja_con_metodos("AutenticacionActivity", xs[1], y2, ancho),
             caja_con_metodos("BorrarDatosActivity", xs[2], y2, ancho),
             caja_de_paquete("ViewModels", ["PerfilInicialViewModel", "InicioViewModel", "ProteccionViewModel",
                                           "AjustesViewModel", "BorrarDatosViewModel"], xs[3], y2, ancho)]
    y3 = y2 + alto(fila2) + 70
    soporte = caja_de_paquete("Soporte de presentación", ["StudyPlanApplication (raíz de composición)",
                                                          "FabricaViewModels", "ContadorActividades",
                                                          "AplicadorApariencia", "FormatoHoras"], xs[0], y3, ancho)
    for c in fila1 + fila2 + [soporte]:
        c.draw(d)
    d.line([fila1[0].right(0.5), fila1[1].left(0.5)], fill=dg.BLACK, width=3)
    dg.arrow_head(d, fila1[1].left(0.5), fila1[0].right(0.5), open_=True)
    dg.label(d, ((fila1[0].x + fila1[0].w + fila1[1].x) / 2, fila1[0].y + fila1[0].h / 2 - 24), "extiende", bold=True)
    for i, linea in enumerate(["PerfilInicialActivity y AutenticacionActivity extienden ActividadBase;",
                               "MainActivity, AjustesActivity y BorrarDatosActivity extienden ActividadProtegida.",
                               "Cada Activity pide su ViewModel a FabricaViewModels (inyección manual)."]):
        d.text((xs[1], y3 + 10 + i * 36), linea, font=dg.font(24, italic=True), fill=dg.GRAY)

    y4 = y3 + soporte.h + 80
    d.line([40, y4 - 30, W - 40, y4 - 30], fill=dg.MID, width=3)
    d.text((40, y4), "2. Núcleo de la aplicación · Java puro, sin android.*", font=dg.font(34, True), fill=dg.BLACK)
    y4 += 60
    nucleo = [caja_de_paquete("domain", DOMINIO_CONFIG, xs[0], y4, ancho, "modelo y reglas"),
              caja_de_paquete("application.port.out", SALIDA_CONFIG, xs[1], y4, ancho, "interfaces: puertos de salida"),
              caja_de_paquete("application.service", SERVICIOS_CONFIG, xs[2], y4, ancho, "implementan"),
              caja_de_paquete("application.port.in", ENTRADA_CONFIG, xs[3], y4, ancho, "interfaces: casos de uso")]
    for c in nucleo:
        c.draw(d)
    dom, sal, ser, ent = nucleo
    dg.assoc(d, fila2[3].bottom(0.5), ent.top(0.5), "usan", None, None, name_off=(60, 0))
    dg.assoc(d, ser.right(0.3), ent.left(0.3), "implementan", None, None)
    dg.assoc(d, ser.left(0.3), sal.right(0.3), "usan", None, None)
    dg.assoc(d, sal.left(0.3), dom.right(0.3), "usan", None, None)

    y5 = y4 + alto(nucleo) + 90
    d.line([40, y5 - 30, W - 40, y5 - 30], fill=dg.MID, width=3)
    d.text((40, y5), "3. Infraestructura · adapter.out (clases de add-configuracion-inicial)", font=dg.font(34, True),
           fill=dg.BLACK)
    y5 += 60
    pers = caja_de_paquete("adapter.out.persistence", PERSISTENCIA_CONFIG, xs[0], y5, ancho * 2 + 40,
                           "Room: implementa PerfilRepository, AjustesRepository y AlmacenDatos")
    seg = caja_de_paquete("adapter.out.security", SEGURIDAD_CONFIG, xs[2], y5, ancho,
                          "BiometricManager: implementa DisponibilidadBloqueo")
    pers.draw(d)
    seg.draw(d)
    dg.assoc(d, sal.bottom(0.35), (sal.x + sal.w * 0.35, pers.y), "implementan", None, None, name_off=(80, 0))
    d.line([sal.bottom(0.8), seg.top(0.2)], fill=dg.BLACK, width=3)
    dg.label(d, ((sal.x + sal.w * 0.8 + seg.x + seg.w * 0.2) / 2, (sal.y + sal.h + seg.y) / 2), "implementa", bold=True)
    img.crop((0, 0, W, int(y5 + max(pers.h, seg.h) + 60))).save(dg.OUT / "diagrama_clases_configuracion.png",
                                                                 dpi=(200, 200))


# ---------------------------------------------------------------- clases de add-gestion-materias
def clases_materias():
    """Clases de la épica E2: presentación con sus métodos, dominio con campos y métodos, puertos y adaptadores."""
    W, H = 3240, 5600
    img = Image.new("RGB", (W, H), dg.WHITE)
    d = ImageDraw.Draw(img)
    xs, ancho = [40, 840, 1640, 2440], 760
    d.text((40, 20), "1. Presentación · adapter.in.ui", font=dg.font(34, True), fill=dg.BLACK)

    fila1 = [caja_con_metodos("MateriasActivity", xs[0], 80, ancho),
             caja_con_metodos("MateriaFormularioActivity", xs[1], 80, ancho),
             caja_con_metodos("ListaMateriasViewModel", xs[2], 80, ancho),
             caja_con_metodos("MateriaFormularioViewModel", xs[3], 80, ancho)]
    y2 = 80 + alto(fila1) + 70
    fila2 = [caja_con_metodos("MateriasAdapter", xs[0], y2, ancho),
             caja_con_metodos("FilaMateria", xs[1], y2, ancho),
             caja_con_metodos("EventoMateria", xs[2], y2, ancho),
             caja_con_metodos("DestinoPrincipal", xs[3], y2, ancho, "enumeration")]
    y3 = y2 + alto(fila2) + 70
    fila3 = [caja_con_metodos("MensajesDeError", xs[0], y3, ancho),
             caja_con_metodos("SelectorColorDialogo", xs[1], y3, ancho),
             caja_con_metodos("ColorHex", xs[2], y3, ancho),
             caja_con_metodos("GrupoBotones", xs[3], y3, ancho)]
    y4f = y3 + alto(fila3) + 70
    fila4 = [caja_con_metodos("AcentoMateria", xs[0], y4f, ancho),
             caja_con_metodos("ColorMateria", xs[1], y4f, ancho)]
    for c in fila1 + fila2 + fila3 + fila4:
        c.draw(d)
    dg.assoc(d, fila1[0].right(0.2), fila1[1].left(0.2), "abre", None, None)
    dg.assoc(d, fila1[2].right(0.2), fila1[3].left(0.2), "comparte la fábrica", None, None)
    dg.assoc(d, fila4[0].right(0.3), fila4[1].left(0.3), "ajusta el color con", None, None)
    ynota = y4f + alto(fila4) + 24
    d.text((xs[0], ynota), "MateriasActivity y MateriaFormularioActivity extienden ActividadProtegida; cada una pide su",
           font=dg.font(24, italic=True), fill=dg.GRAY)
    d.text((xs[0], ynota + 36), "ViewModel a FabricaViewModels. Los ViewModel usan los casos de uso (application.port.in). La barra de navegación se enlaza con DestinoPrincipal.",
           font=dg.font(24, italic=True), fill=dg.GRAY)
    d.text((xs[0], ynota + 72), "AcentoMateria pinta el color de materia solo como barra, punto o chip (HU-03); ColorMateria es Java puro y lo ajusta a ≥ 3:1 contra la superficie del tema.",
           font=dg.font(24, italic=True), fill=dg.GRAY)

    y4 = ynota + 166
    d.line([40, y4 - 30, W - 40, y4 - 30], fill=dg.MID, width=3)
    d.text((40, y4), "2. Núcleo de la aplicación · Java puro, sin android.*", font=dg.font(34, True), fill=dg.BLACK)
    y4 += 60
    materia = caja_con_metodos("Materia", xs[0], y4, ancho, con_campos=True)
    nucleo = [materia,
              caja_de_paquete("application.port.in", ENTRADA_MATERIAS, xs[1], y4, ancho, "casos de uso"),
              caja_de_paquete("application.service", SERVICIOS_MATERIAS, xs[2], y4, ancho, "implementan"),
              caja_de_paquete("port.out y domain", ["MateriaRepository", "SesionesDeMateria", "ListadoMaterias",
                                                   "ResumenMateria"], xs[3], y4, ancho, "puertos de salida y modelo")]
    for c in nucleo:
        c.draw(d)
    dg.assoc(d, nucleo[2].left(0.3), nucleo[1].right(0.3), "implementan", None, None)
    dg.assoc(d, nucleo[2].right(0.3), nucleo[3].left(0.3), "usan", None, None)

    y5 = y4 + alto(nucleo) + 90
    d.line([40, y5 - 30, W - 40, y5 - 30], fill=dg.MID, width=3)
    d.text((40, y5), "3. Infraestructura · adapter.out.persistence", font=dg.font(34, True), fill=dg.BLACK)
    y5 += 60
    infra = [caja_con_metodos("MateriaEntity", xs[0], y5, ancho, "Entity", con_campos=True),
             caja_con_metodos("MateriaDao", xs[1], y5, ancho, "Dao"),
             caja_con_metodos("MateriaRepositoryRoom", xs[2], y5, ancho),
             caja_de_paquete("Migración y adaptador real", ["Migraciones (MIGRATION_1_2)",
                                                             "SesionesDeMateriaRoom (desde E3)"], xs[3], y5, ancho)]
    for c in infra:
        c.draw(d)
    dg.assoc(d, nucleo[3].bottom(0.2), (infra[2].x + infra[2].w * 0.5, infra[2].y), "implementan", None, None,
             name_off=(100, 0))
    dg.assoc(d, infra[2].left(0.3), infra[1].right(0.3), "usa", None, None)
    dg.assoc(d, infra[1].left(0.3), infra[0].right(0.3), "lee y escribe", None, None)
    img.crop((0, 0, W, int(y5 + alto(infra) + 60))).save(dg.OUT / "diagrama_clases_materias.png", dpi=(200, 200))


def clases_sesiones():
    """Clases de la épica E3: sesiones de estudio, patrón State GoF en dominio y persistencia Room v3."""
    W, H = 3760, 6400
    img = Image.new("RGB", (W, H), dg.WHITE)
    d = ImageDraw.Draw(img)
    d.text((40, 20), "1. Presentación · adapter.in.ui", font=dg.font(34, True), fill=dg.BLACK)

    xs1, w1 = [40, 980, 1920, 2860], 860
    fila1 = [caja_con_metodos("SesionFormularioActivity", xs1[0], 80, w1),
             caja_con_metodos("SesionFormularioViewModel", xs1[1], 80, w1),
             caja_con_metodos("SesionDetalleActivity", xs1[2], 80, w1),
             caja_con_metodos("SesionDetalleViewModel", xs1[3], 80, w1)]
    y2 = 80 + alto(fila1) + 70
    fila2 = [caja_con_metodos("SesionInicioAdapter", xs1[0], y2, w1),
             caja_con_metodos("EventoSesion", xs1[1], y2, w1),
             caja_con_metodos("FormatoSesion", xs1[2], y2, w1),
             caja_con_metodos("EstadoSesionVisual", xs1[3], y2, w1, "enumeration")]
    y3s = y2 + alto(fila2) + 70
    fila3s = [caja_con_metodos("RolColorEstado", xs1[3], y3s, w1, "enumeration")]
    for c in fila1 + fila2 + fila3s:
        c.draw(d)
    dg.assoc(d, fila1[0].right(0.18), fila1[1].left(0.18), "observa", None, None)
    dg.assoc(d, fila1[2].right(0.18), fila1[3].left(0.18), "observa", None, None)
    dg.assoc(d, fila2[3].bottom(0.5), (fila3s[0].x + fila3s[0].w * 0.5, fila3s[0].y), "rol de color", None, None,
             name_off=(90, 0))
    ynota = y3s + alto(fila3s) + 24
    d.text((40, ynota),
           "SesionFormularioActivity y SesionDetalleActivity heredan de ActividadProtegida (FLAG_SECURE y bloqueo HU-04); MainActivity abre ambas y enlaza SesionInicioAdapter.",
           font=dg.font(24, italic=True), fill=dg.GRAY)

    y4 = ynota + 100
    d.line([40, y4 - 30, W - 40, y4 - 30], fill=dg.MID, width=3)
    d.text((40, y4), "2. Núcleo de la aplicación · Java puro, sin android.*", font=dg.font(34, True), fill=dg.BLACK)
    y4 += 60
    xs2, ws2 = [40, 1280, 2240, 3020], [1100, 820, 640, 700]
    sesion = caja_con_metodos("SesionEstudio", xs2[0], y4, ws2[0], con_campos=True)
    estado = caja_con_metodos("EstadoSesion", xs2[1], y4, ws2[1], "State GoF")
    paquete_dom = caja_de_paquete(
        "Estados concretos y objetos de valor",
        ["EstadoPendiente", "EstadoEnCurso", "EstadoNoRealizada", "EstadoCompletada",
         "EstadoCompletadaTardia", "EstadoReprogramada", "CodigoEstadoSesion",
         "Fecha", "Hora", "ConflictoHorario", "ResultadoSesion",
         "DetalleSesion", "DetalleSesionInicio"],
        xs2[2], y4, ws2[2], "domain")
    paquete_app = caja_de_paquete(
        "Casos de uso, servicios y puertos",
        ENTRADA_SESIONES + ["---"] + SERVICIOS_SESIONES + ["---"] + SALIDA_SESIONES,
        xs2[3], y4, ws2[3], "application")
    nucleo = [sesion, estado, paquete_dom, paquete_app]
    for c in nucleo:
        c.draw(d)
    dg.assoc(d, sesion.right(0.35), estado.left(0.35), "RN-07", None, None)
    dg.assoc(d, paquete_dom.left(0.35), estado.right(0.35), "implementan", None, None)
    dg.assoc(d, paquete_app.left(0.35), paquete_dom.right(0.35), "orquesta", None, None)

    y5 = y4 + alto(nucleo) + 90
    d.line([40, y5 - 30, W - 40, y5 - 30], fill=dg.MID, width=3)
    d.text((40, y5), "3. Infraestructura · adapter.out.persistence", font=dg.font(34, True), fill=dg.BLACK)
    y5 += 60
    infra = [caja_con_metodos("SesionEntity", xs1[0], y5, w1, "Entity", con_campos=True),
             caja_con_metodos("SesionDao", xs1[1], y5, w1, "Dao"),
             caja_con_metodos("SesionRepositoryRoom", xs1[2], y5, w1),
             caja_de_paquete("Adaptadores y migración v3",
                             ["SesionesDeMateriaRoom", "RelojSistema", "Migraciones (MIGRATION_2_3)"],
                             xs1[3], y5, w1)]
    for c in infra:
        c.draw(d)
    dg.assoc(d, paquete_app.bottom(0.3), (infra[2].x + infra[2].w * 0.5, infra[2].y), "implementa", None, None,
             name_off=(100, 0))
    dg.assoc(d, infra[2].left(0.3), infra[1].right(0.3), "usa", None, None)
    dg.assoc(d, infra[1].left(0.3), infra[0].right(0.3), "lee y escribe", None, None)
    img.crop((0, 0, W, int(y5 + alto(infra) + 60))).save(dg.OUT / "diagrama_clases_sesiones.png", dpi=(200, 200))


def clases_calendario():
    """Clases de la épica E4: calendario de estudio (vistas Día/Semana/Mes, filtros y consulta por rango)."""
    W, H = 3760, 4200
    img = Image.new("RGB", (W, H), dg.WHITE)
    d = ImageDraw.Draw(img)
    d.text((40, 20), "1. Presentación · adapter.in.ui", font=dg.font(34, True), fill=dg.BLACK)

    xs1, w1 = [40, 980, 1920, 2860], 860
    fila1 = [caja_con_metodos("CalendarioActivity", xs1[0], 80, w1),
             caja_con_metodos("CalendarioViewModel", xs1[1], 80, w1),
             caja_con_metodos("CalendarioMesAdapter", xs1[2], 80, w1),
             caja_con_metodos("PreferenciaVistaCalendario", xs1[3], 80, w1)]
    for c in fila1:
        c.draw(d)
    dg.assoc(d, fila1[0].right(0.2), fila1[1].left(0.2), "observa", None, None)
    dg.assoc(d, fila1[1].right(0.2), fila1[3].left(0.2), "persiste vista", None, None)

    y2 = 80 + alto(fila1) + 90
    d.line([40, y2 - 30, W - 40, y2 - 30], fill=dg.MID, width=3)
    d.text((40, y2), "2. Núcleo de la aplicación · Java puro, sin android.*", font=dg.font(34, True), fill=dg.BLACK)
    y2 += 60
    xs2, ws2 = [40, 1020, 1980, 2880], [900, 880, 820, 840]
    periodo = caja_con_metodos("PeriodoCalendario", xs2[0], y2, ws2[0], con_campos=True)
    calculador = caja_con_metodos("CalculadorRangoHorario", xs2[1], y2, ws2[1])
    paquete_dom = caja_de_paquete(
        "Modelos de lectura y filtro (domain)",
        ["VistaCalendario", "FiltroCalendario", "DatosCalendario", "ItemSesionCalendario",
         "BloqueHorarioCalendario", "CeldaDiaMes", "ResumenPeriodoCalendario"],
        xs2[2], y2, ws2[2], "domain")
    servicio = caja_con_metodos("ConsultarCalendarioService", xs2[3], y2, ws2[3])
    nucleo = [periodo, calculador, paquete_dom, servicio]
    for c in nucleo:
        c.draw(d)
    dg.assoc(d, servicio.left(0.3), paquete_dom.right(0.3), "construye", None, None)
    dg.assoc(d, calculador.right(0.3), paquete_dom.left(0.3), "genera", None, None)

    y3 = y2 + alto(nucleo) + 90
    d.line([40, y3 - 30, W - 40, y3 - 30], fill=dg.MID, width=3)
    d.text((40, y3), "3. Puertos e infraestructura Room v3 (sin migración)", font=dg.font(34, True), fill=dg.BLACK)
    y3 += 60
    infra = [caja_con_metodos("ConsultarCalendario", xs1[0], y3, w1, "port.in"),
             caja_con_metodos("SesionRepository", xs1[1], y3, w1, "port.out"),
             caja_con_metodos("SesionRepositoryRoom", xs1[2], y3, w1),
             caja_con_metodos("SesionDao", xs1[3], y3, w1, "Dao")]
    for c in infra:
        c.draw(d)
    dg.assoc(d, infra[2].left(0.3), infra[1].right(0.3), "implementa", None, None)
    dg.assoc(d, infra[2].right(0.3), infra[3].left(0.3), "listarPorRango", None, None)
    img.crop((0, 0, W, int(y3 + alto(infra) + 60))).save(dg.OUT / "diagrama_clases_calendario.png", dpi=(200, 200))


def clases_tecnicas():
    """Clases de la épica E5: técnicas de estudio, ejecución en vivo, recuperación y reparto."""
    W, H = 3760, 4400
    img = Image.new("RGB", (W, H), dg.WHITE)
    d = ImageDraw.Draw(img)
    d.text((40, 20), "1. Presentación · adapter.in.ui", font=dg.font(34, True), fill=dg.BLACK)

    xs1, w1 = [40, 980, 1920, 2860], 860
    fila1 = [caja_con_metodos("SesionEnCursoActivity", xs1[0], 80, w1),
             caja_con_metodos("SesionEnCursoViewModel", xs1[1], 80, w1),
             caja_con_metodos("RepartoSesionActivity", xs1[2], 80, w1),
             caja_con_metodos("RepartoSesionViewModel", xs1[3], 80, w1)]
    y1b = 80 + alto(fila1) + 50
    fila1b = [caja_con_metodos("DialogoRecuperacionSesion", xs1[0], y1b, w1),
              caja_con_metodos("EventoEstudioAdapter", xs1[1], y1b, w1),
              caja_con_metodos("ReceptorAvisosSesion", xs1[2], y1b, w1),
              caja_con_metodos("NotificadorSesion", xs1[3], y1b, w1)]
    for c in fila1 + fila1b:
        c.draw(d)
    dg.assoc(d, fila1[0].right(0.2), fila1[1].left(0.2), "observa", None, None)
    dg.assoc(d, fila1[2].right(0.2), fila1[3].left(0.2), "observa", None, None)

    y2 = y1b + alto(fila1b) + 90
    d.line([40, y2 - 30, W - 40, y2 - 30], fill=dg.MID, width=3)
    d.text((40, y2), "2. Núcleo de la aplicación · Java puro, sin android.*", font=dg.font(34, True), fill=dg.BLACK)
    y2 += 60
    nucleo = [
        caja_de_paquete("domain (modelos y calculadores)", DOMINIO_TECNICAS, xs1[0], y2, w1, "domain"),
        caja_de_paquete("application.port.in (casos de uso)", ENTRADA_TECNICAS, xs1[1], y2, w1, "port.in"),
        caja_de_paquete("application.service", SERVICIOS_TECNICAS, xs1[2], y2, w1, "implementan"),
        caja_de_paquete("application.port.out", SALIDA_TECNICAS, xs1[3], y2, w1, "port.out"),
    ]
    for c in nucleo:
        c.draw(d)
    dg.assoc(d, nucleo[2].left(0.3), nucleo[1].right(0.3), "implementan", None, None)
    dg.assoc(d, nucleo[2].right(0.3), nucleo[3].left(0.3), "usan", None, None)

    y3 = y2 + alto(nucleo) + 90
    d.line([40, y3 - 30, W - 40, y3 - 30], fill=dg.MID, width=3)
    d.text((40, y3), "3. Infraestructura · adapter.out.persistence y adapter.out.system", font=dg.font(34, True), fill=dg.BLACK)
    y3 += 60
    infra = [caja_con_metodos("EventoEstudioEntity", xs1[0], y3, w1, "Entity", con_campos=True),
             caja_con_metodos("EventoEstudioDao", xs1[1], y3, w1, "Dao"),
             caja_con_metodos("EventoEstudioRepositoryRoom", xs1[2], y3, w1),
             caja_de_paquete("adapter.out.system", SISTEMA_TECNICAS, xs1[3], y3, w1, "Android/System")]
    for c in infra:
        c.draw(d)
    dg.assoc(d, infra[2].right(0.3), nucleo[3].bottom(0.2), "implementa", None, None)
    dg.assoc(d, infra[3].top(0.5), nucleo[3].bottom(0.7), "implementan", None, None)
    img.crop((0, 0, W, int(y3 + alto(infra) + 60))).save(dg.OUT / "diagrama_clases_tecnicas.png", dpi=(200, 200))


def clases_metas():
    """Clases de la épica E6: metas semanales, estudio inmediato, cambio de materia y felicitación."""
    W, H = 3760, 4200
    img = Image.new("RGB", (W, H), dg.WHITE)
    d = ImageDraw.Draw(img)
    d.text((40, 20), "1. Presentación · adapter.in.ui", font=dg.font(34, True), fill=dg.BLACK)

    xs1, w1 = [40, 980, 1920, 2860], 860
    fila1 = [caja_con_metodos("HistorialMetasActivity", xs1[0], 80, w1),
             caja_con_metodos("HistorialMetasViewModel", xs1[1], 80, w1),
             caja_con_metodos("HistorialMetasAdapter", xs1[2], 80, w1),
             caja_con_metodos("FelicitacionMetasActivity", xs1[3], 80, w1)]
    y1b = 80 + alto(fila1) + 50
    fila1b = [caja_con_metodos("SelectorEstudioBottomSheet", xs1[0], y1b, w1)]
    for c in fila1 + fila1b:
        c.draw(d)
    dg.assoc(d, fila1[0].right(0.3), fila1[1].left(0.3), "observa", None, None)

    y2 = y1b + alto(fila1b) + 90
    d.line([40, y2 - 30, W - 40, y2 - 30], fill=dg.MID, width=3)
    d.text((40, y2), "2. Núcleo de la aplicación · Java puro, sin android.*", font=dg.font(34, True), fill=dg.BLACK)
    y2 += 60
    nucleo = [
        caja_de_paquete("domain (modelos y calculadores)", DOMINIO_METAS, xs1[0], y2, w1, "domain"),
        caja_de_paquete("application.port.in (casos de uso)", ENTRADA_METAS, xs1[1], y2, w1, "port.in"),
        caja_de_paquete("application.service", SERVICIOS_METAS, xs1[2], y2, w1, "implementan"),
        caja_de_paquete("application.port.out", SALIDA_METAS, xs1[3], y2, w1, "port.out"),
    ]
    for c in nucleo:
        c.draw(d)
    dg.assoc(d, nucleo[2].left(0.3), nucleo[1].right(0.3), "implementan", None, None)
    dg.assoc(d, nucleo[2].right(0.3), nucleo[3].left(0.3), "usan", None, None)

    y3 = y2 + alto(nucleo) + 90
    d.line([40, y3 - 30, W - 40, y3 - 30], fill=dg.MID, width=3)
    d.text((40, y3), "3. Infraestructura · adapter.out.persistence", font=dg.font(34, True), fill=dg.BLACK)
    y3 += 60
    infra = [caja_con_metodos("MetaSemanalEntity", xs1[0], y3, w1, "Entity", con_campos=True),
             caja_con_metodos("MetaSemanalDao", xs1[1], y3, w1, "Dao"),
             caja_con_metodos("MetaSemanalRepositoryRoom", xs1[2], y3, w1)]
    for c in infra:
        c.draw(d)
    dg.assoc(d, infra[2].right(0.3), nucleo[3].bottom(0.5), "implementa", None, None)
    img.crop((0, 0, W, int(y3 + alto(infra) + 60))).save(dg.OUT / "diagrama_clases_metas.png", dpi=(200, 200))


def clases_estadisticas():
    """Clases de la épica E7: estadísticas de estudio, concentración, tópicos y blindaje de salidas."""
    W, H = 3960, 4400
    img = Image.new("RGB", (W, H), dg.WHITE)
    d = ImageDraw.Draw(img)
    d.text((40, 20), "1. Presentación · adapter.in.ui", font=dg.font(34, True), fill=dg.BLACK)

    xs1, ws1 = [40, 960, 1880, 2720], [860, 860, 780, 1200]
    fila1 = [caja_con_metodos("EstadisticasActivity", xs1[0], 80, ws1[0]),
             caja_con_metodos("EstadisticasViewModel", xs1[1], 80, ws1[1]),
             caja_con_metodos("EstadisticasAdapter", xs1[2], 80, ws1[2]),
             caja_con_metodos("TopicosUi", xs1[3], 80, ws1[3])]
    for c in fila1:
        c.draw(d)
    dg.assoc(d, fila1[0].right(0.25), fila1[1].left(0.25), "observa", None, None)
    dg.assoc(d, fila1[2].right(0.25), fila1[3].left(0.25), "etiquetas", None, None)

    ynota = 80 + alto(fila1) + 24
    d.text((40, ynota),
           "EstadisticasActivity hereda de ActividadProtegida y enlaza DestinoPrincipal.ESTADISTICAS; DialogoRecuperacionSesion (W13) y EventoEstudioAdapter muestran TopicosUi.",
           font=dg.font(24, italic=True), fill=dg.GRAY)

    y2 = ynota + 90
    d.line([40, y2 - 30, W - 40, y2 - 30], fill=dg.MID, width=3)
    d.text((40, y2), "2. Núcleo de la aplicación · Java puro, sin android.*", font=dg.font(34, True), fill=dg.BLACK)
    y2 += 60
    nucleo = [
        caja_de_paquete("domain (agregados, métricas y tópicos)", DOMINIO_ESTADISTICAS, xs1[0], y2, ws1[0], "domain"),
        caja_con_metodos("ConsultarEstadisticas", xs1[1], y2, ws1[1], "port.in"),
        caja_con_metodos("ConsultarEstadisticasService", xs1[2], y2, ws1[2]),
        caja_de_paquete("application.port.out (reutilizados)",
                        ["TramoDiarioRepository", "SesionRepository", "MateriaRepository",
                         "EventoEstudioRepository", "Reloj"], xs1[3], y2, ws1[3], "port.out"),
    ]
    for c in nucleo:
        c.draw(d)
    dg.assoc(d, nucleo[2].left(0.3), nucleo[1].right(0.3), "implementa", None, None)
    dg.assoc(d, nucleo[2].right(0.3), nucleo[3].left(0.3), "consulta", None, None)

    y3 = y2 + alto(nucleo) + 90
    d.line([40, y3 - 30, W - 40, y3 - 30], fill=dg.MID, width=3)
    d.text((40, y3), "3. Infraestructura · Room v7 (MIGRATION_6_7) y preferencias", font=dg.font(34, True), fill=dg.BLACK)
    y3 += 60
    infra = [caja_con_metodos("EventoEstudioEntity", xs1[0], y3, ws1[0], "Entity", con_campos=True),
             caja_con_metodos("EventoEstudioDao", xs1[1], y3, ws1[1], "Dao"),
             caja_con_metodos("EventoEstudioRepositoryRoom", xs1[2], y3, ws1[2]),
             caja_con_metodos("MarcadorSalidaAppPrefs", xs1[3], y3, ws1[3])]
    for c in infra:
        c.draw(d)
    dg.assoc(d, infra[2].left(0.3), infra[1].right(0.3), "JOIN rango UTC", None, None)
    img.crop((0, 0, W, int(y3 + alto(infra) + 60))).save(
        dg.OUT / "diagrama_clases_estadisticas.png", dpi=(200, 200))


def clases_reportes():
    """Clases de la épica E8: reporte de estudio por periodo y exportación nativa PDF/CSV."""
    W, H = 3760, 4200
    img = Image.new("RGB", (W, H), dg.WHITE)
    d = ImageDraw.Draw(img)
    d.text((40, 20), "1. Presentación · adapter.in.ui", font=dg.font(34, True), fill=dg.BLACK)

    xs1, ws1 = [40, 1280, 2480], [1160, 1140, 1240]
    fila1 = [caja_con_metodos("ReportesActivity", xs1[0], 80, ws1[0]),
             caja_con_metodos("ReportesViewModel", xs1[1], 80, ws1[1]),
             caja_con_metodos("ReportesAdapter", xs1[2], 80, ws1[2])]
    for c in fila1:
        c.draw(d)
    dg.assoc(d, fila1[0].right(0.3), fila1[1].left(0.3), "observa", None, None)

    ynota = 80 + alto(fila1) + 24
    d.text((40, ynota),
           "ReportesActivity hereda de ActividadProtegida; recibe periodo desde EstadisticasActivity o MainActivity y comparte formato de nombre con EstadisticasAdapter.",
           font=dg.font(24, italic=True), fill=dg.GRAY)

    y2 = ynota + 90
    d.line([40, y2 - 30, W - 40, y2 - 30], fill=dg.MID, width=3)
    d.text((40, y2), "2. Núcleo de la aplicación · Java puro, sin android.*", font=dg.font(34, True), fill=dg.BLACK)
    y2 += 60
    xs2, ws2 = [40, 960, 1880, 2760], [860, 860, 820, 960]
    nucleo = [
        caja_de_paquete("domain (reporte y CSV)", DOMINIO_REPORTES, xs2[0], y2, ws2[0], "domain"),
        caja_con_metodos("GenerarReporteEstudio", xs2[1], y2, ws2[1], "port.in"),
        caja_con_metodos("GenerarReporteEstudioService", xs2[2], y2, ws2[2]),
        caja_de_paquete("application.port.out",
                        ["ExportadorReporte", "ResultadoExportacion",
                         "TramoDiarioRepository (reutilizado)", "SesionRepository (reutilizado)",
                         "MateriaRepository (reutilizado)", "Reloj (reutilizado)"],
                        xs2[3], y2, ws2[3], "port.out"),
    ]
    for c in nucleo:
        c.draw(d)
    dg.assoc(d, nucleo[2].left(0.3), nucleo[1].right(0.3), "implementa", None, None)
    dg.assoc(d, nucleo[2].right(0.3), nucleo[3].left(0.3), "consulta", None, None)

    ynota_n = y2 + alto(nucleo) + 24
    d.text((40, ynota_n),
           "Reutiliza SesionEstudio.minutosPlanificadosEnRango, CalculadorEstadisticas.calcularReporte y DesgloseMateriaPeriodo (RN-31, RN-05, RN-08).",
           font=dg.font(24, italic=True), fill=dg.GRAY)

    y3 = ynota_n + 90
    d.line([40, y3 - 30, W - 40, y3 - 30], fill=dg.MID, width=3)
    d.text((40, y3), "3. Infraestructura · adapter.out.system (PDF A4, CSV BOM y FileProvider)",
           font=dg.font(34, True), fill=dg.BLACK)
    y3 += 60
    infra = [caja_con_metodos("ExportadorReporteSistema", xs2[0], y3, 1800)]
    for c in infra:
        c.draw(d)
    d.text((1900, y3 + 40),
           "• PdfDocument A4 (595×842 pt) independiente de densidad\n"
           "• GeneradorCsvReporte UTF-8 con BOM y mitigación CWE-1236\n"
           "• Caché privada (cacheDir/reportes/, retención 3 archivos)\n"
           "• BorrarDatosService invoca limpiarTemporales() (HU-38)",
           font=dg.font(24), fill=dg.GRAY)
    img.crop((0, 0, W, int(y3 + alto(infra) + 60))).save(
        dg.OUT / "diagrama_clases_reportes.png", dpi=(200, 200))


def clases_recordatorios():
    """Clases de la épica E9: recordatorios locales de sesiones y permisos RNF-18."""
    W, H = 3760, 4200
    img = Image.new("RGB", (W, H), dg.WHITE)
    d = ImageDraw.Draw(img)
    d.text((40, 20), "1. Presentación · adapter.in.ui (W5, W6, W7, W10 y notificaciones)",
           font=dg.font(34, True), fill=dg.BLACK)

    xs1, ws1 = [40, 820, 1660, 2900], [740, 800, 1180, 820]
    fila1 = [caja_con_metodos("AjustesActivity", xs1[0], 80, ws1[0]),
             caja_con_metodos("SesionFormularioActivity", xs1[1], 80, ws1[1]),
             caja_con_metodos("NotificadorSesion", xs1[2], 80, ws1[2]),
             caja_con_metodos("ReceptorAvisosSesion", xs1[3], 80, ws1[3])]
    for c in fila1:
        c.draw(d)
    dg.assoc(d, fila1[3].left(0.3), fila1[2].right(0.3), "emite", None, None)

    y2 = 80 + alto(fila1) + 90
    d.line([40, y2 - 30, W - 40, y2 - 30], fill=dg.MID, width=3)
    d.text((40, y2), "2. Núcleo de la aplicación · Java puro, sin android.*", font=dg.font(34, True), fill=dg.BLACK)
    y2 += 60
    xs2, ws2 = [40, 980, 1920, 2860], [880, 880, 880, 860]
    nucleo = [
        caja_de_paquete("domain (recordatorios)", DOMINIO_RECORDATORIOS + ["Ajustes (ampliado)", "SesionEstudio (ampliada)"], xs2[0], y2, ws2[0], "domain"),
        caja_de_paquete("application.port.in", ENTRADA_RECORDATORIOS, xs2[1], y2, ws2[1], "port.in"),
        caja_de_paquete("application.service", SERVICIOS_RECORDATORIOS + ["Planificar / Reprogramar / Mutadores"], xs2[2], y2, ws2[2], "service"),
        caja_con_metodos("ProgramadorRecordatorios", xs2[3], y2, ws2[3], "port.out"),
    ]
    for c in nucleo:
        c.draw(d)
    dg.assoc(d, nucleo[2].left(0.3), nucleo[1].right(0.3), "implementa", None, None)
    dg.assoc(d, nucleo[2].right(0.3), nucleo[3].left(0.3), "programa/cancela", None, None)

    y3 = y2 + alto(nucleo) + 90
    d.line([40, y3 - 30, W - 40, y3 - 30], fill=dg.MID, width=3)
    d.text((40, y3), "3. Infraestructura · Room v8 (MIGRATION_7_8) y AlarmManager",
           font=dg.font(34, True), fill=dg.BLACK)
    y3 += 60
    infra = [caja_con_metodos("AjustesEntity", 40, y3, 880, "Entity", con_campos=True),
             caja_con_metodos("SesionEntity", 980, y3, 880, "Entity", con_campos=True),
             caja_con_metodos("ProgramadorAvisosAlarmManager", 1920, y3, 1800)]
    for c in infra:
        c.draw(d)
    img.crop((0, 0, W, int(y3 + alto(infra) + 60))).save(
        dg.OUT / "diagrama_clases_recordatorios.png", dpi=(200, 200))


# ---------------------------------------------------------------- modelo lógico y diccionario de datos
DESCRIPCIONES = {
    ("perfil", "id"): "Identificador de la única fila del perfil (siempre 1, RN-01).",
    ("perfil", "nombre"): "Nombre del estudiante, sin espacios en los extremos (HU-01).",
    ("perfil", "metaSemanalHoras"): "Meta semanal predeterminada en horas, mayor que 0 y hasta 168 (RN-14).",
    ("ajustes", "id"): "Identificador de la única fila de ajustes (siempre 1).",
    ("ajustes", "tema"): "Tema visual: SISTEMA, CLARO u OSCURO (HU-03).",
    ("ajustes", "idioma"): "Idioma de la aplicación: ES o EN (HU-03).",
    ("ajustes", "bloqueoActivo"): "1 si la app pide huella o PIN al abrirse y al volver a primer plano (HU-04).",
    ("ajustes", "no_molestar_global"): "1 si el modo No molestar se activa de forma predeterminada al estudiar (HU-24).",
    ("ajustes", "pomodoro_trabajo_min"): "Duración en minutos de cada bloque de trabajo Pomodoro (HU-23, RN-22).",
    ("ajustes", "pomodoro_descanso_corto_min"): "Duración en minutos del descanso corto Pomodoro (HU-23, RN-22).",
    ("ajustes", "pomodoro_descanso_largo_min"): "Duración en minutos del descanso largo Pomodoro (HU-23, RN-22).",
    ("ajustes", "pomodoro_ciclos_hasta_largo"): "Cantidad de ciclos de trabajo hasta un descanso largo Pomodoro (HU-23, RN-22).",
    ("ajustes", "recordatorio_minutos"): "Antelación predeterminada en minutos para avisos previos al planificar (0 = sin recordatorio, 5, 15 o 30; valor inicial 15, HU-34).",
    ("materia", "id"): "Identificador autogenerado de la materia.",
    ("materia", "nombre"): "Nombre saneado de la materia, de 1 a 50 caracteres (RN-02).",
    ("materia", "nombre_clave"): "Nombre sin acentos y en mayúsculas: decide la unicidad entre las materias activas (RN-02).",
    ("materia", "color"): "Color de la materia como entero ARGB (paleta provisional).",
    ("materia", "docente"): "Docente de la materia; vacío si no se indicó (texto libre opcional).",
    ("materia", "archivada"): "1 si la materia está archivada (HU-08); 0 si está activa.",
    ("materia", "meta_semanal_horas"): "Meta semanal opcional propia de la materia en horas (> 0 y ≤ 168, RN-14, HU-26).",
    ("materia", "es_general"): "1 para la materia general protegida «Estudios varios» (RN-32); 0 para materias del estudiante.",
    ("sesion_estudio", "id"): "Identificador autogenerado de la sesión de estudio.",
    ("sesion_estudio", "materia_id"): "Identificador de la materia asociada; clave foránea a materia(id) con borrado restringido (RN-03).",
    ("sesion_estudio", "plan_inicio_utc_milis"): "Instante absoluto de inicio planificado o real de arranque en milisegundos UTC (RN-04, RN-05, HU-47).",
    ("sesion_estudio", "plan_fin_utc_milis"): "Instante absoluto de fin planificado en milisegundos UTC; nulo en sesiones sin plan iniciadas con «Estudiar ahora» (HU-47).",
    ("sesion_estudio", "desfase_minutos"): "Desfase horario local en minutos respecto a UTC vigente al planificar (para preservar el historial ante viajes).",
    ("sesion_estudio", "fecha"): "Fecha planificada o ejecutada en formato ISO AAAA-MM-DD (HU-10, HU-11).",
    ("sesion_estudio", "hora_inicio"): "Hora de inicio en formato HH:mm de 24 horas (RN-04).",
    ("sesion_estudio", "hora_fin"): "Hora de fin en formato HH:mm posterior a la de inicio (RN-04).",
    ("sesion_estudio", "duracion_planificada_minutos"): "Duración planificada calculada en minutos entre hora_inicio y hora_fin.",
    ("sesion_estudio", "duracion_real_minutos"): "Duración real estudiada en minutos (entre 1 y 1440); nula mientras no se complete (HU-12, HU-13).",
    ("sesion_estudio", "estado"): "Código de estado según el patrón State (RN-07): PENDIENTE, EN_CURSO, NO_REALIZADA, COMPLETADA, COMPLETADA_TARDIA o REPROGRAMADA.",
    ("sesion_estudio", "notas"): "Notas u observaciones opcionales de la sesión (hasta 500 caracteres).",
    ("sesion_estudio", "sesion_origen_id"): "Identificador de la sesión original cuando procede de una reprogramación (HU-14); nulo si no aplica.",
    ("sesion_estudio", "tecnica"): "Técnica elegida al iniciar: LIBRE o POMODORO; nula hasta que inicia (HU-22, HU-23).",
    ("sesion_estudio", "forzar_no_molestar"): "Preferencia de No molestar de la sesión: HEREDAR_GLOBAL, FORZAR_ACTIVAR o FORZAR_DESACTIVAR (HU-24).",
    ("sesion_estudio", "pomodoro_trabajo_min"): "Minutos de trabajo de la instantánea Pomodoro congelada al iniciar (RN-22); nula si no aplica.",
    ("sesion_estudio", "pomodoro_descanso_corto_min"): "Minutos de descanso corto de la instantánea Pomodoro congelada al iniciar (RN-22); nula si no aplica.",
    ("sesion_estudio", "pomodoro_descanso_largo_min"): "Minutos de descanso largo de la instantánea Pomodoro congelada al iniciar (RN-22); nula si no aplica.",
    ("sesion_estudio", "pomodoro_ciclos_hasta_largo"): "Ciclos hasta descanso largo de la instantánea Pomodoro congelada al iniciar (RN-22); nula si no aplica.",
    ("sesion_estudio", "recordatorio_minutos"): "Antelación del recordatorio previo en minutos (5, 15 o 30) o nula cuando está desactivado (HU-34).",
    ("evento_estudio", "id"): "Identificador autogenerado del evento de ejecución.",
    ("evento_estudio", "sesion_id"): "Identificador de la sesión asociada; clave foránea a sesion_estudio(id) con ON DELETE CASCADE (HU-39).",
    ("evento_estudio", "tipo"): "Tipo de evento cronológico: INICIO, PAUSA, REANUDACION, FIN, CAMBIO_FASE, SALIDA_APP o REGRESO_APP (HU-39, RN-23).",
    ("evento_estudio", "instante_milis"): "Marca temporal exacta en milisegundos UTC desde epoch (RN-23).",
    ("evento_estudio", "desfase_minutos"): "Desfase horario local en minutos respecto a UTC en el momento del evento (HU-39).",
    ("evento_estudio", "fecha"): "Fecha local del evento en formato ISO AAAA-MM-DD.",
    ("evento_estudio", "hora"): "Hora local del evento en formato HH:mm.",
    ("evento_estudio", "nota"): "Nota breve o detalle opcional del evento (por ejemplo, fase Pomodoro o resolución de regreso).",
    ("evento_estudio", "topico"): "Clave canónica del tópico asociado a pausa, salida o estudio fuera de la app (HU-22, HU-39, HU-41, HU-45, RN-30); nula en registros previos a v7.",
    ("tramo_diario", "sesion_id"): "Identificador de la sesión asociada; parte de la clave primaria y FK a sesion_estudio(id) con ON DELETE CASCADE (RN-31).",
    ("tramo_diario", "fecha_local"): "Fecha calendario local ISO AAAA-MM-DD en la que ocurrió el estudio (RN-31).",
    ("tramo_diario", "minutos"): "Minutos reales contabilizables ocurridos en esa fecha local (RN-31).",
    ("meta_semanal", "id"): "Identificador autogenerado del snapshot semanal.",
    ("meta_semanal", "semana_inicio"): "Fecha ISO del lunes de la semana (RN-13, RN-33).",
    ("meta_semanal", "materia_id"): "Nulo para la meta semanal total; FK a materia(id) con ON DELETE CASCADE para metas por materia (HU-25, HU-26).",
    ("meta_semanal", "horas_objetivo"): "Objetivo semanal congelado en horas para esa semana (RN-14, RN-33).",
    ("meta_semanal", "felicitada"): "1 si la felicitación por esta meta ya se mostró en esa semana (RN-16); 0 en caso contrario.",
}
TABLAS_DE_NEGOCIO = {"perfil": "Perfil del estudiante", "ajustes": "Ajustes de la aplicación",
                     "materia": "Materias del estudiante", "sesion_estudio": "Sesiones de estudio",
                     "evento_estudio": "Eventos de ejecución de sesiones",
                     "tramo_diario": "Minutos reales por día local (RN-31)",
                     "meta_semanal": "Instantáneas de metas semanales (RN-33)"}


def _ultimo_esquema():
    versiones = sorted(ESQUEMAS.glob("*.json"), key=lambda p: int(p.stem))
    ruta = versiones[-1]
    return int(ruta.stem), json.loads(ruta.read_text(encoding="utf-8"))["database"]


def _restricciones(campo, pk):
    partes = []
    if campo["fieldPath"] in pk["columnNames"] or campo["columnName"] in pk["columnNames"]:
        partes.append("PK" + (" autoincremental" if pk.get("autoGenerate") else ""))
    if campo.get("notNull"):
        partes.append("NOT NULL")
    return ", ".join(partes) if partes else "Admite nulo"


def modelo_logico():
    """Dibuja las tablas del esquema más reciente de Room y devuelve (versión, entidades)."""
    version, base = _ultimo_esquema()
    entidades = base["entities"]
    for entidad in entidades:
        for campo in entidad["fields"]:
            if (entidad["tableName"], campo["columnName"]) not in DESCRIPCIONES:
                raise SystemExit(f"Falta la descripción de {entidad['tableName']}.{campo['columnName']} en DESCRIPCIONES")
    W = 3240
    img = Image.new("RGB", (W, 2200), dg.WHITE)
    d = ImageDraw.Draw(img)
    xs_fila1, ancho_fila1, y1 = [40, 1120, 2200], 1000, 80

    def caja_entidad(entidad, x, y, w):
        pk = entidad["primaryKey"]
        fk_cols = {c for fk in entidad.get("foreignKeys", []) for c in fk.get("columns", [])}
        lineas = []
        for campo in entidad["fields"]:
            nombre_col = campo["columnName"]
            if nombre_col in pk["columnNames"]:
                marca = "PK "
            elif nombre_col in fk_cols:
                marca = "FK "
            else:
                marca = "   "
            nulo = "" if campo.get("notNull") else "  (admite nulo)"
            lineas.append(f"{marca}{nombre_col}: {campo['affinity']}{nulo}")
        for indice in entidad.get("indices", []):
            lineas.append(f"índice {indice['name']} ({', '.join(indice['columnNames'])})")
        return dg.UMLBox(entidad["tableName"], lineas, None, x, y, w=w, stereotype="tabla")

    cajas_top = [caja_entidad(entidades[i], xs_fila1[i], y1, ancho_fila1) for i in range(min(3, len(entidades)))]
    cajas = list(cajas_top)
    y_fin = y1 + alto(cajas_top) + 50
    if len(entidades) > 3:
        y2 = y1 + alto(cajas_top) + 60
        for idx, entidad in enumerate(entidades[3:]):
            caja_extra = caja_entidad(entidad, 40 + (idx % 2) * 1620, y2 + (idx // 2) * 560, 1560)
            cajas.append(caja_extra)
        y_fin = max(c.y + c.h for c in cajas) + 50
    d.text((40, 20), f"Modelo lógico · Room versión {version}", font=dg.font(34, True), fill=dg.BLACK)
    for c in cajas:
        c.draw(d)
    notas = [
        "sesion_estudio.materia_id referencia a materia(id) con ON DELETE RESTRICT (RN-03: no se elimina una materia con sesiones).",
        "sesion_estudio.sesion_origen_id referencia a sesion_estudio(id) con ON DELETE SET NULL (HU-14: enlace de reprogramación).",
        "evento_estudio.sesion_id referencia a sesion_estudio(id) con ON DELETE CASCADE (HU-39: línea de tiempo de ejecución).",
        "tramo_diario.sesion_id referencia a sesion_estudio(id) con ON DELETE CASCADE (RN-31: corte diario estricto).",
        "perfil y ajustes mantienen una única fila (id fijo en 1).",
    ]
    for i, linea in enumerate(notas):
        d.text((40, y_fin + i * 38), linea, font=dg.font(24, italic=True), fill=dg.GRAY)
    img.crop((0, 0, W, int(y_fin + 40 + len(notas) * 38))).save(dg.OUT / "modelo_logico.png", dpi=(200, 200))
    return version, entidades


def texto_modelo_logico(version):
    return (
        f"El esquema de Room está en la versión {version} y se exporta como JSON a `app/schemas/`; el diagrama y el "
        "diccionario de datos se generan desde ese archivo con `python docs/build/diagramas_clases.py`, de modo que no "
        "pueden desfasarse del esquema real. La @figura:modelo_logico muestra las tablas, sus claves y sus índices.\n\n"
        "![Modelo lógico de la base de datos de StudyPlan](../recursos/diagramas/modelo_logico.png){#modelo_logico ancho=16.5}\n\n"
        "Nota: Elaboración propia, generada desde el esquema de Room.\n")


def texto_diccionario(entidades):
    bloques = ["Cada tabla se describe en una tabla del documento. La columna *Restricciones* sale del esquema de Room y la "
               "descripción, de las reglas de negocio (RN) y las historias de usuario (HU) que la originan.\n"]
    for i, entidad in enumerate(entidades):
        nombre = entidad["tableName"]
        titulo = TABLAS_DE_NEGOCIO.get(nombre, nombre)
        bloques.append(f"Tabla: Diccionario de datos de la tabla {nombre} ({titulo}) {{#diccionario_{nombre} anchos=3.4,2.2,3.6,6.8 tam=9}}\n")
        filas = ["| Campo | Tipo | Restricciones | Descripción |", "|---|---|---|---|"]
        for campo in entidad["fields"]:
            filas.append(f"| {campo['columnName']} | {campo['affinity']} | {_restricciones(campo, entidad['primaryKey'])} | "
                         f"{DESCRIPCIONES[(nombre, campo['columnName'])]} |")
        bloques.append("\n".join(filas) + "\n")
    return "\n".join(bloques)


def reemplazar_seccion(texto, encabezado, nuevo):
    """Reemplaza el contenido de un encabezado ### hasta el siguiente encabezado ## o ###."""
    patron = re.compile(r"(^### " + re.escape(encabezado) + r"\n\n)(.*?)(?=^#{2,3} )", re.S | re.M)
    if not patron.search(texto):
        raise SystemExit(f"No se encontró la sección «{encabezado}» en el capítulo 4")
    return patron.sub(lambda m: m.group(1) + nuevo.rstrip("\n") + "\n\n", texto, count=1)


def actualizar_capitulo(version, entidades):
    texto = CAPITULO_4.read_text(encoding="utf-8")
    texto = reemplazar_seccion(texto, "Modelo Lógico", texto_modelo_logico(version))
    texto = reemplazar_seccion(texto, "Diccionario de Datos", texto_diccionario(entidades))
    CAPITULO_4.write_text(texto, encoding="utf-8")


# ---------------------------------------------------------------- secuencia «registrar materia»
# (índice de participante origen, destino, texto, clase.método real a verificar o None, es_retorno)
PARTICIPANTES = [("Estudiante", None), ("MateriaFormularioActivity", "MateriaFormularioActivity"),
                 ("MateriaFormularioViewModel", "MateriaFormularioViewModel"),
                 ("RegistrarMateriaService", "RegistrarMateriaService"), ("Materia", "Materia"),
                 ("MateriaRepositoryRoom", "MateriaRepositoryRoom"), ("MateriaDao", "MateriaDao")]
MENSAJES = [
    (0, 1, "pulsa «Guardar»: onGuardarClick(View)", ("MateriaFormularioActivity", "onGuardarClick"), False),
    (1, 2, "guardar(Long, String, int, String)", ("MateriaFormularioViewModel", "guardar"), False),
    (2, 2, "ejecutor.execute(...) · hilo en segundo plano", None, False),
    (2, 3, "ejecutar(String, int, String)", ("RegistrarMateriaService", "ejecutar"), False),
    (3, 4, "crear(String, int, String)", ("Materia", "crear"), False),
    (4, 3, "Resultado<Materia> (nombre saneado y clave)", None, True),
    (3, 5, "activas()  ·  regla RN-02 (NombresDeMateria.hayActivaConClave)", ("MateriaRepositoryRoom", "activas"), False),
    (5, 6, "activas()", ("MateriaDao", "activas"), False),
    (6, 5, "List<MateriaEntity>", None, True),
    (5, 3, "List<Materia>", None, True),
    (3, 5, "guardar(Materia)", ("MateriaRepositoryRoom", "guardar"), False),
    (5, 6, "insertar(MateriaEntity)", ("MateriaDao", "insertar"), False),
    (6, 5, "id: long", None, True),
    (5, 3, "Materia con id", None, True),
    (3, 2, "Resultado<Materia>", None, True),
    (2, 1, "evento.postValue(GUARDADA)  →  onEvento(EventoMateria)", ("MateriaFormularioActivity", "onEvento"), False),
    (1, 1, "finish()", None, False),
]


PARTICIPANTES_PLANIFICAR_SESION = [
    ("Estudiante", None),
    ("SesionFormularioActivity", "SesionFormularioActivity"),
    ("SesionFormularioViewModel", "SesionFormularioViewModel"),
    ("PlanificarSesionService", "PlanificarSesionService"),
    ("SesionEstudio / State", "SesionEstudio"),
    ("SesionRepositoryRoom", "SesionRepositoryRoom"),
    ("SesionDao", "SesionDao"),
]
MENSAJES_PLANIFICAR_SESION = [
    (0, 1, "pulsa «Guardar»: onGuardarClick(View)", ("SesionFormularioActivity", "onGuardarClick"), False),
    (1, 2, "guardarPlanificada(materiaId, fecha, inicio, fin, notas)",
     ("SesionFormularioViewModel", "guardarPlanificada"), False),
    (2, 2, "ejecutor.execute(...) · hilo en segundo plano", None, False),
    (2, 3, "ejecutar(materiaId, Fecha, Hora, Hora, String)", ("PlanificarSesionService", "ejecutar"), False),
    (3, 5, "listarPorFecha(Fecha) · ValidadorSesiones (RN-05)", ("SesionRepositoryRoom", "listarPorFecha"), False),
    (5, 6, "listarPorRangoUtc(desde, hasta)", ("SesionDao", "listarPorRangoUtc"), False),
    (6, 5, "List<SesionEntity>", None, True),
    (5, 3, "List<SesionEstudio> (sin cruce horario)", None, True),
    (3, 4, "planificar(..., Fecha hoy) · EstadoPendiente (RN-07)", ("SesionEstudio", "planificar"), False),
    (4, 3, "Resultado<SesionEstudio>", None, True),
    (3, 5, "guardar(SesionEstudio)", ("SesionRepositoryRoom", "guardar"), False),
    (5, 6, "insertar(SesionEntity)", ("SesionDao", "insertar"), False),
    (6, 5, "id: long", None, True),
    (5, 3, "SesionEstudio con id", None, True),
    (3, 2, "ResultadoSesion.ok(sesion)", None, True),
    (2, 1, "evento.postValue(PLANIFICADA)  →  onEvento(EventoSesion)",
     ("SesionFormularioActivity", "onEvento"), False),
    (1, 1, "finish()", None, False),
]


def verificar_mensajes(mensajes=MENSAJES):
    for _, _, _, real, _ in mensajes:
        if real is None:
            continue
        clase, metodo = real
        if metodo not in jf.nombres_de_metodos(clase):
            raise SystemExit(f"El diagrama de secuencia usa {clase}.{metodo}, que no existe en el código")


def _dibujar_secuencia(titulo, participantes, mensajes, fondo_inicio, fondo_fin, archivo):
    verificar_mensajes(mensajes)
    W, paso, margen = 3400, 100, 80
    n = len(participantes)
    H = 300 + paso * (len(mensajes) + 2)
    img = Image.new("RGB", (W, H), dg.WHITE)
    d = ImageDraw.Draw(img)
    xs = [margen + 200 + i * ((W - 2 * margen - 400) / (n - 1)) for i in range(n)]
    y_cabecera, y0 = 90, 260
    d.text((40, 20), titulo, font=dg.font(34, True), fill=dg.BLACK)
    for (nombre, _), x in zip(participantes, xs):
        caja = [x - 195, y_cabecera, x + 195, y_cabecera + 80]
        d.rectangle(caja, fill=dg.LIGHT, outline=dg.BLACK, width=3)
        d.text((x, y_cabecera + 40), nombre, font=dg.font(20, True), fill=dg.BLACK, anchor="mm")
        dg.dashed_line(d, [(x, y_cabecera + 80), (x, H - 60)], fill=dg.GRAY, width=2, dash=12, gap=10)
    y_fin = y0 + paso * len(mensajes)
    d.rectangle([xs[2] - 230, y0 + paso * fondo_inicio - 40, xs[-1] + 230, y0 + paso * fondo_fin - 20],
                outline=dg.GRAY, width=3)
    d.text((xs[2] - 220, y0 + paso * fondo_inicio - 36), "hilo de fondo (ejecutor de un solo hilo)",
           font=dg.font(20, italic=True), fill=dg.GRAY)
    for i, (o, t, texto, _, retorno) in enumerate(mensajes):
        y = y0 + paso * i + 20
        x1, x2 = xs[o], xs[t]
        if o == t:
            d.line([(x1, y - 14), (x1 + 70, y - 14), (x1 + 70, y + 14), (x1, y + 14)], fill=dg.BLACK, width=3)
            dg.arrow_head(d, (x1 + 70, y + 14), (x1, y + 14), open_=True)
            d.text((x1 + 85, y - 14), f"{i + 1}. {texto}", font=dg.font(21), fill=dg.BLACK, anchor="lm")
            continue
        if retorno:
            dg.dashed_line(d, [(x1, y), (x2, y)], fill=dg.BLACK, width=3)
        else:
            d.line([(x1, y), (x2, y)], fill=dg.BLACK, width=3)
        dg.arrow_head(d, (x1, y), (x2, y), open_=True)
        d.text(((x1 + x2) / 2, y - 24), f"{i + 1}. {texto}", font=dg.font(21, italic=retorno), fill=dg.BLACK, anchor="mm")
    img.crop((0, 0, W, int(y_fin + 100))).save(dg.OUT / archivo, dpi=(200, 200))


def secuencia_registrar_materia():
    _dibujar_secuencia(
        "Secuencia · registrar una materia (HU-06)",
        PARTICIPANTES,
        MENSAJES,
        2,
        15,
        "secuencia_registrar_materia.png",
    )


def secuencia_planificar_sesion():
    _dibujar_secuencia(
        "Secuencia · planificar una sesión de estudio (HU-10, RN-05 y RN-07)",
        PARTICIPANTES_PLANIFICAR_SESION,
        MENSAJES_PLANIFICAR_SESION,
        2,
        15,
        "secuencia_planificar_sesion.png",
    )


PARTICIPANTES_CALENDARIO = [
    ("CalendarioActivity", "adapter.in.ui"),
    ("CalendarioViewModel", "adapter.in.ui"),
    ("ConsultarCalendarioService", "application"),
    ("VencerSesionesPendientesService", "application"),
    ("SesionRepositoryRoom", "adapter.out"),
    ("SesionDao", "Room"),
]

MENSAJES_CALENDARIO = [
    (0, 1, "cargar() / cambiarVista(vista)", ("CalendarioViewModel", "cargar"), False),
    (1, 2, "ejecutar(periodo, filtro)", ("ConsultarCalendarioService", "ejecutar"), False),
    (2, 3, "ejecutar() [RN-08 vencer pendientes]", ("VencerSesionesPendientesService", "ejecutar"), False),
    (3, 2, "int vencidas", None, True),
    (2, 4, "listarPorRango(desde, hasta)", ("SesionRepositoryRoom", "listarPorRango"), False),
    (4, 5, "listarPorRangoUtc(desdeUtc, hastaUtc)", ("SesionDao", "listarPorRangoUtc"), False),
    (5, 4, "List<SesionEntity>", None, True),
    (4, 2, "List<SesionEstudio>", None, True),
    (2, 2, "filtrar, construir bloques/celdas y resumen Plan/Real", None, False),
    (2, 1, "DatosCalendario", None, True),
    (1, 0, "LiveData<DatosCalendario> → onDatosActualizados()", ("CalendarioActivity", "onDatosActualizados"), True),
]


def secuencia_consultar_calendario():
    _dibujar_secuencia(
        "Secuencia · consultar calendario de estudio (HU-16 a HU-21, RN-08, RN-11)",
        PARTICIPANTES_CALENDARIO,
        MENSAJES_CALENDARIO,
        1,
        10,
        "secuencia_consultar_calendario.png",
    )


PARTICIPANTES_EJECUCION = [
    ("SesionDetalleActivity", "adapter.in.ui"),
    ("IniciarSesionService", "application"),
    ("SesionRepositoryRoom", "adapter.out"),
    ("EventoEstudioRepositoryRoom", "adapter.out"),
    ("ControladorNoMolestarSistema", "adapter.out.system"),
    ("ProgramadorAvisosAlarmManager", "adapter.out.system"),
]

MENSAJES_EJECUCION = [
    (0, 1, "ejecutar(sesionId, tecnica, prefDnd)", ("IniciarSesionService", "ejecutar"), False),
    (1, 2, "buscarPorId(sesionId) / guardar(sesionEnCurso)", ("SesionRepositoryRoom", "guardar"), False),
    (2, 1, "SesionEstudio (EN_CURSO + snapshot)", None, True),
    (1, 3, "guardar(EventoEstudio.INICIO)", ("EventoEstudioRepositoryRoom", "guardar"), False),
    (1, 4, "aplicar(activar) [HU-24, RNF-18]", ("ControladorNoMolestarSistema", "aplicar"), False),
    (1, 5, "programarSiguienteHito(sesionId, instante, hito)", ("ProgramadorAvisosAlarmManager", "programarSiguienteHito"), False),
    (1, 0, "Resultado<SesionEstudio> -> abre SesionEnCursoActivity", None, True),
]


def secuencia_ejecucion_sesion():
    _dibujar_secuencia(
        "Secuencia · iniciar y ejecutar sesión en vivo (HU-22 a HU-24, HU-39, RN-22)",
        PARTICIPANTES_EJECUCION,
        MENSAJES_EJECUCION,
        1,
        6,
        "secuencia_ejecucion_sesion.png",
    )


def generar():
    clases_configuracion()
    clases_materias()
    clases_sesiones()
    clases_calendario()
    clases_tecnicas()
    clases_metas()
    clases_estadisticas()
    clases_reportes()
    clases_recordatorios()
    version, entidades = modelo_logico()
    actualizar_capitulo(version, entidades)
    secuencia_registrar_materia()
    secuencia_planificar_sesion()
    secuencia_consultar_calendario()
    secuencia_ejecucion_sesion()


if __name__ == "__main__":
    generar()
    for f in ("diagrama_clases_configuracion", "diagrama_clases_materias", "diagrama_clases_sesiones",
              "diagrama_clases_calendario", "diagrama_clases_tecnicas", "diagrama_clases_metas",
              "diagrama_clases_estadisticas", "diagrama_clases_reportes", "diagrama_clases_recordatorios",
              "modelo_logico", "secuencia_registrar_materia", "secuencia_planificar_sesion",
              "secuencia_consultar_calendario", "secuencia_ejecucion_sesion"):
        print((dg.OUT / f"{f}.png").relative_to(RAIZ))
