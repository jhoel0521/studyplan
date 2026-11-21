"""Mapa clase -> carpeta destino de la reorganización de paquetes (RNF-15).

Herramienta TEMPORAL del change ``reorganizar-paquetes-avisos-documentacion``: sirve solo para ejecutar el movimiento
con ``docs/build/mover_paquetes.py`` y se borra al cerrar la etapa 3 (queda en el historial de git). Después la verdad
son las carpetas, vigiladas por ``EstructuraPaquetesTest``.

Reglas (decisiones 2 y 3 del diseño del change):
  * domain: orden de preguntas, la primera que se cumple gana
        enum -> enumeration | forma parte de la máquina de estados -> state | entidad -> entity
        | regla sin estado -> service | dato para la pantalla -> readmodel | si no, valueobject
    con dos excepciones: ErrorValidacion -> result y CodigoEstadoSesion -> state.
  * application y adapter.in.ui: la épica sale de las listas de docs/build/diagramas_clases.py; lo compartido va a comun.
  * port.out: persistence (repositorios) o system (el resto). adapter.out.persistence: entity, dao, repository.
"""
import re
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
BASE_MAIN = RAIZ / "app" / "src" / "main" / "java" / "io" / "github" / "jhoel0521" / "studyplan"
BASE_TEST = RAIZ / "app" / "src" / "test" / "java" / "io" / "github" / "jhoel0521" / "studyplan"
BASE_ANDROID_TEST = RAIZ / "app" / "src" / "androidTest" / "java" / "io" / "github" / "jhoel0521" / "studyplan"
PAQUETE_RAIZ = "io.github.jhoel0521.studyplan"

# carpetas de main que se reorganizan (relativas a BASE_MAIN, con "/")
CAPAS = ["domain", "application/port/in", "application/port/out", "application/service",
         "adapter/in/ui", "adapter/out/persistence"]

EPICAS = {"CONFIG": "configuracion", "MATERIAS": "materias", "SESIONES": "sesiones", "CALENDARIO": "calendario",
          "TECNICAS": "tecnicas", "METAS": "metas", "ESTADISTICAS": "estadisticas", "REPORTES": "reportes",
          "RECORDATORIOS": "recordatorios"}
CAPA_DE_LISTA = {"PRESENTACION": "adapter/in/ui", "ENTRADA": "application/port/in",
                 "SERVICIOS": "application/service", "SALIDA": "application/port/out",
                 "PERSISTENCIA": "adapter/out/persistence", "SISTEMA": "adapter/out/system",
                 "SEGURIDAD": "adapter/out/security"}

# Lo que no se mueve (decisión 4): lo que el manifiesto nombra y el sistema persiste, y la base de datos.
NO_SE_MUEVE = {
    "adapter/in/ui": {"StudyPlanApplication", "FabricaViewModels", "ReceptorAvisosSesion", "ServicioSesionEnCurso"},
    "adapter/out/persistence": {"StudyPlanDatabase", "Migraciones"},
}

# Clases públicas que sirven a varias épicas y por eso van a comun aunque una lista las ponga en una sola.
COMUN_EXPLICITO = {
    "adapter/in/ui": {"ActividadBase", "ActividadProtegida", "DestinoPrincipal", "AplicadorApariencia",
                      "ContadorActividades"},
}

ENUM_A_RESULT = {"ErrorValidacion"}
ENUM_A_STATE = {"CodigoEstadoSesion"}
ENTIDADES = {"Materia", "SesionEstudio", "EventoEstudio", "Perfil", "MetaSemanal", "Ajustes"}
VALUE_OBJECTS = {"Fecha", "Hora", "Instante", "ConfiguracionPomodoro", "IntervaloEfectivo", "FiltroCalendario",
                 "PeriodoCalendario", "PeriodoEstadisticas"}
SERVICIOS_DOMINIO = {"RepartidorPorDia", "GeneradorCsvReporte"}
RESULT = {"Resultado"}


def listas_por_epica():
    """Lee las listas por épica de diagramas_clases.py: capa -> clase -> conjunto de épicas."""
    texto = (RAIZ / "docs" / "build" / "diagramas_clases.py").read_text(encoding="utf-8")
    resultado = {}
    for nombre, cuerpo in re.findall(r"^([A-Z_]+)\s*=\s*\[(.*?)\]\s*$", texto, re.S | re.M):
        m = re.match(r"([A-Z]+)_([A-Z]+)$", nombre)
        if not m or m.group(1) not in CAPA_DE_LISTA or m.group(2) not in EPICAS:
            continue
        capa = CAPA_DE_LISTA[m.group(1)]
        for clase in re.findall(r'"([^"]+)"', cuerpo):
            resultado.setdefault(capa, {}).setdefault(clase, set()).add(EPICAS[m.group(2)])
    return resultado


def tipo_dominio(nombre, texto):
    """Carpeta de una clase de domain según el orden de decisión del diseño."""
    if nombre in ENUM_A_RESULT:
        return "result"
    if nombre in ENUM_A_STATE:
        return "state"
    if re.search(r"\benum\s+" + nombre + r"\b", texto):
        return "enumeration"
    if nombre == "EstadoSesion" or re.search(r"implements\s+EstadoSesion\b", texto):
        return "state"
    if nombre in ENTIDADES:
        return "entity"
    if nombre.startswith("Calculador") or nombre in SERVICIOS_DOMINIO:
        return "service"
    if nombre in RESULT:
        return "result"
    if nombre in VALUE_OBJECTS:
        return "valueobject"
    return "readmodel"


def carpeta_port_out(nombre):
    if nombre.endswith("Repository") or nombre in {"AlmacenDatos", "SesionesDeMateria"}:
        return "persistence"
    return "system"


def carpeta_persistencia(nombre):
    if nombre.endswith("Entity"):
        return "entity"
    if nombre.endswith("Dao"):
        return "dao"
    return "repository"


def destino_main(capa, nombre, texto, listas):
    """Devuelve (capa_destino, subcarpeta) de una clase de main. Lanza ValueError si no se puede decidir."""
    if capa == "domain":
        return "domain", tipo_dominio(nombre, texto)
    if capa in NO_SE_MUEVE and nombre in NO_SE_MUEVE[capa]:
        return capa, ""
    if capa == "adapter/out/persistence":
        if nombre == "RelojSistema":
            return "adapter/out/system", ""
        return capa, carpeta_persistencia(nombre)
    if capa == "application/port/out":
        return capa, carpeta_port_out(nombre)
    if nombre in COMUN_EXPLICITO.get(capa, set()):
        return capa, "comun"
    epicas = listas.get(capa, {}).get(nombre, set())
    if len(epicas) == 1:
        return capa, next(iter(epicas))
    if len(epicas) > 1:
        return capa, "comun"
    raise ValueError("%s/%s no figura en ninguna lista por épica" % (capa, nombre))
