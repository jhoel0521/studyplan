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
                         "MensajesDeError", "SelectorColorDialogo", "ColorHex", "GrupoBotones"]
ENTRADA_MATERIAS = ["ArchivarMateria", "ConsultarMateria", "DesarchivarMateria", "EditarMateria", "EliminarMateria",
                    "ListarMaterias", "RegistrarMateria"]
SERVICIOS_MATERIAS = ["ArchivarMateriaService", "ConsultarMateriaService", "DesarchivarMateriaService",
                      "EditarMateriaService", "EliminarMateriaService", "ListarMateriasService", "NombresDeMateria",
                      "RegistrarMateriaService"]
SALIDA_MATERIAS = ["MateriaRepository", "SesionesDeMateria"]
DOMINIO_MATERIAS = ["ListadoMaterias", "Materia", "ResumenMateria"]
PERSISTENCIA_MATERIAS = ["MateriaDao", "MateriaEntity", "MateriaRepositoryRoom", "Migraciones",
                         "SinSesionesDeMateria"]

# Paquete (carpeta bajo io/github/jhoel0521/studyplan) -> todas las clases que deben aparecer en algún diagrama
CLASES_POR_PAQUETE = {
    "adapter/in/ui": PRESENTACION_CONFIG + PRESENTACION_MATERIAS,
    "application/port/in": ENTRADA_CONFIG + ENTRADA_MATERIAS,
    "application/service": SERVICIOS_CONFIG + SERVICIOS_MATERIAS,
    "application/port/out": SALIDA_CONFIG + SALIDA_MATERIAS,
    "domain": DOMINIO_CONFIG + DOMINIO_MATERIAS,
    "adapter/out/persistence": PERSISTENCIA_CONFIG + PERSISTENCIA_MATERIAS,
    "adapter/out/security": SEGURIDAD_CONFIG,
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
    W, H = 3240, 3000
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
    W, H = 3240, 3400
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
    for c in fila1 + fila2 + fila3:
        c.draw(d)
    dg.assoc(d, fila1[0].right(0.2), fila1[1].left(0.2), "abre", None, None)
    dg.assoc(d, fila1[2].right(0.2), fila1[3].left(0.2), "comparte la fábrica", None, None)
    ynota = y3 + alto(fila3) + 24
    d.text((xs[0], ynota), "MateriasActivity y MateriaFormularioActivity extienden ActividadProtegida; cada una pide su",
           font=dg.font(24, italic=True), fill=dg.GRAY)
    d.text((xs[0], ynota + 36), "ViewModel a FabricaViewModels. Los ViewModel usan los casos de uso (application.port.in). La barra de navegación se enlaza con DestinoPrincipal.",
           font=dg.font(24, italic=True), fill=dg.GRAY)

    y4 = ynota + 130
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
             caja_de_paquete("Migración y adaptador provisional", ["Migraciones (MIGRATION_1_2)",
                                                                  "SinSesionesDeMateria"], xs[3], y5, ancho)]
    for c in infra:
        c.draw(d)
    dg.assoc(d, nucleo[3].bottom(0.2), (infra[2].x + infra[2].w * 0.5, infra[2].y), "implementan", None, None,
             name_off=(100, 0))
    dg.assoc(d, infra[2].left(0.3), infra[1].right(0.3), "usa", None, None)
    dg.assoc(d, infra[1].left(0.3), infra[0].right(0.3), "lee y escribe", None, None)
    img.crop((0, 0, W, int(y5 + alto(infra) + 60))).save(dg.OUT / "diagrama_clases_materias.png", dpi=(200, 200))


# ---------------------------------------------------------------- modelo lógico y diccionario de datos
DESCRIPCIONES = {
    ("perfil", "id"): "Identificador de la única fila del perfil (siempre 1, RN-01).",
    ("perfil", "nombre"): "Nombre del estudiante, sin espacios en los extremos (HU-01).",
    ("perfil", "metaSemanalHoras"): "Meta semanal predeterminada en horas, mayor que 0 y hasta 168 (RN-14).",
    ("ajustes", "id"): "Identificador de la única fila de ajustes (siempre 1).",
    ("ajustes", "tema"): "Tema visual: SISTEMA, CLARO u OSCURO (HU-03).",
    ("ajustes", "idioma"): "Idioma de la aplicación: ES o EN (HU-03).",
    ("ajustes", "bloqueoActivo"): "1 si la app pide huella o PIN al abrirse y al volver a primer plano (HU-04).",
    ("materia", "id"): "Identificador autogenerado de la materia.",
    ("materia", "nombre"): "Nombre saneado de la materia, de 1 a 50 caracteres (RN-02).",
    ("materia", "nombre_clave"): "Nombre sin acentos y en mayúsculas: decide la unicidad entre las materias activas (RN-02).",
    ("materia", "color"): "Color de la materia como entero ARGB (paleta provisional).",
    ("materia", "docente"): "Docente de la materia; vacío si no se indicó (texto libre opcional).",
    ("materia", "archivada"): "1 si la materia está archivada (HU-08); 0 si está activa.",
}
TABLAS_DE_NEGOCIO = {"perfil": "Perfil del estudiante", "ajustes": "Ajustes de la aplicación",
                     "materia": "Materias del estudiante"}


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
    img = Image.new("RGB", (W, 900), dg.WHITE)
    d = ImageDraw.Draw(img)
    xs, ancho, y = [40, 1120, 2200], 1000, 80
    cajas = []
    for i, entidad in enumerate(entidades):
        pk = entidad["primaryKey"]
        lineas = []
        for campo in entidad["fields"]:
            marca = "PK " if campo["columnName"] in pk["columnNames"] else "   "
            nulo = "" if campo.get("notNull") else "  (admite nulo)"
            lineas.append(f"{marca}{campo['columnName']}: {campo['affinity']}{nulo}")
        for indice in entidad.get("indices", []):
            lineas.append(f"índice {indice['name']} ({', '.join(indice['columnNames'])})")
        cajas.append(dg.UMLBox(entidad["tableName"], lineas, None, xs[i], y, w=ancho, stereotype="tabla"))
    d.text((40, 20), f"Modelo lógico · Room versión {version}", font=dg.font(34, True), fill=dg.BLACK)
    for c in cajas:
        c.draw(d)
    y_fin = y + alto(cajas) + 50
    for i, linea in enumerate(["Las tablas son independientes: todavía no hay claves foráneas.",
                               "La tabla de sesiones traerá la clave foránea materia_id hacia materia (change de sesiones).",
                               "perfil y ajustes tienen una sola fila (id fijo en 1)."]):
        d.text((40, y_fin + i * 38), linea, font=dg.font(24, italic=True), fill=dg.GRAY)
    img.crop((0, 0, W, int(y_fin + 140))).save(dg.OUT / "modelo_logico.png", dpi=(200, 200))
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


def verificar_mensajes():
    for _, _, _, real, _ in MENSAJES:
        if real is None:
            continue
        clase, metodo = real
        if metodo not in jf.nombres_de_metodos(clase):
            raise SystemExit(f"El diagrama de secuencia usa {clase}.{metodo}, que no existe en el código")


def secuencia_registrar_materia():
    verificar_mensajes()
    W, paso, margen = 3300, 100, 80
    n = len(PARTICIPANTES)
    H = 300 + paso * (len(MENSAJES) + 2)
    img = Image.new("RGB", (W, H), dg.WHITE)
    d = ImageDraw.Draw(img)
    xs = [margen + 200 + i * ((W - 2 * margen - 400) / (n - 1)) for i in range(n)]
    y_cabecera, y0 = 90, 260
    d.text((40, 20), "Secuencia · registrar una materia (HU-06)", font=dg.font(34, True), fill=dg.BLACK)
    for (nombre, _), x in zip(PARTICIPANTES, xs):
        caja = [x - 190, y_cabecera, x + 190, y_cabecera + 80]
        d.rectangle(caja, fill=dg.LIGHT, outline=dg.BLACK, width=3)
        d.text((x, y_cabecera + 40), nombre, font=dg.font(21, True), fill=dg.BLACK, anchor="mm")
        dg.dashed_line(d, [(x, y_cabecera + 80), (x, H - 60)], fill=dg.GRAY, width=2, dash=12, gap=10)
    y_fin = y0 + paso * len(MENSAJES)
    # zona del hilo en segundo plano (mensajes 3 a 15)
    d.rectangle([xs[2] - 230, y0 + paso * 2 - 40, xs[6] + 230, y0 + paso * 15 - 20], outline=dg.GRAY, width=3)
    d.text((xs[2] - 220, y0 + paso * 2 - 36), "hilo de fondo (ejecutor de un solo hilo)", font=dg.font(20, italic=True),
           fill=dg.GRAY)
    for i, (o, t, texto, _, retorno) in enumerate(MENSAJES):
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
    img.crop((0, 0, W, int(y_fin + 100))).save(dg.OUT / "secuencia_registrar_materia.png", dpi=(200, 200))


def generar():
    clases_configuracion()
    clases_materias()
    version, entidades = modelo_logico()
    actualizar_capitulo(version, entidades)
    secuencia_registrar_materia()


if __name__ == "__main__":
    generar()
    for f in ("diagrama_clases_configuracion", "diagrama_clases_materias", "modelo_logico", "secuencia_registrar_materia"):
        print((dg.OUT / f"{f}.png").relative_to(RAIZ))
