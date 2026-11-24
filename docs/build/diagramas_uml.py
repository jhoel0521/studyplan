"""Diagramas UML de clases y de secuencia generados con PlantUML desde el código Java.

Las relaciones salen de los fuentes (extends, implements y tipos de los campos), no se escriben a mano.
Salida: docs/build/uml/*.puml (versionados) y los PNG en docs/documento/recursos/diagramas/.

Uso: python docs/build/diagramas_uml.py [--solo nombre]
Requiere docs/build/herramientas/plantuml.jar (no se versiona) y Java.
"""
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import diagramas_clases as dc  # noqa: E402
import java_fuentes as jf  # noqa: E402

RAIZ = dc.RAIZ
UML = RAIZ / "docs" / "build" / "uml"
SALIDA = RAIZ / "docs" / "documento" / "recursos" / "diagramas"
JAR = RAIZ / "docs" / "build" / "herramientas" / "plantuml.jar"
DOT = RAIZ / "docs" / "build" / "herramientas" / "gv" / "Graphviz-12.2.1-win64" / "bin" / "dot.exe"
JAVA = Path(r"C:\Program Files\Android\Android Studio\jbr\bin\java.exe")

CABECERA = """@startuml
skinparam backgroundColor white
skinparam shadowing false
skinparam defaultFontName Arial
skinparam defaultFontSize 13
skinparam classAttributeIconSize 0
skinparam nodesep 30
skinparam ranksep 90
left to right direction
skinparam class {
  BackgroundColor white
  BorderColor black
  ArrowColor black
}
skinparam package {
  BackgroundColor #F4F4F4
  BorderColor #555555
}
"""

# capa -> (nombre del paquete UML, color de fondo)
CAPAS = [
    ("presentacion", "adapter.in.ui", "#EAF1FB"),
    ("port_in", "application.port.in", "#EEF7EA"),
    ("service", "application.service", "#EEF7EA"),
    ("port_out", "application.port.out", "#EEF7EA"),
    ("dominio", "domain", "#FFF6E0"),
    ("persistencia", "adapter.out.persistence", "#F5EAF7"),
    ("seguridad", "adapter.out.security", "#F5EAF7"),
    ("drive", "adapter.out.drive", "#F5EAF7"),
    ("sistema", "adapter.out.system", "#F5EAF7"),
]

EPICAS = {
    "configuracion": "CONFIG", "materias": "MATERIAS", "sesiones": "SESIONES", "calendario": "CALENDARIO",
    "tecnicas": "TECNICAS", "metas": "METAS", "estadisticas": "ESTADISTICAS", "reportes": "REPORTES",
    "recordatorios": "RECORDATORIOS", "respaldo": "RESPALDO", "widget": "WIDGET",
}
TITULOS = {
    "configuracion": "Clases · E1 Perfil, inicio y ajustes", "materias": "Clases · E2 Materias",
    "sesiones": "Clases · E3 Sesiones de estudio", "calendario": "Clases · E4 Calendario",
    "tecnicas": "Clases · E5 Técnicas y eventos de estudio", "metas": "Clases · E6 Metas semanales",
    "estadisticas": "Clases · E7 Estadísticas", "reportes": "Clases · E8 Reportes",
    "recordatorios": "Clases · E9 Recordatorios", "respaldo": "Clases · E10 Respaldo y privacidad",
    "widget": "Clases · widget de progreso semanal",
}


def clases_de(epica, capa):
    sufijo = EPICAS[epica]
    nombre = {"presentacion": "PRESENTACION_", "port_in": "ENTRADA_", "service": "SERVICIOS_", "port_out": "SALIDA_",
              "dominio": "DOMINIO_", "persistencia": "PERSISTENCIA_", "seguridad": "SEGURIDAD_", "drive": "DRIVE_",
              "sistema": "SISTEMA_"}[capa] + sufijo
    return list(getattr(dc, nombre, []))


# ---------------------------------------------------------------- lectura del código
_DECL = re.compile(r"^(?:public\s+)?(?P<abs>abstract\s+)?(?:final\s+)?(?P<tipo>class|interface|enum)\s+(?P<n>\w+)"
                   r"(?:<[^{]*?>)?(?:\s+extends\s+(?P<ext>[\w.<>, ]+?))?(?:\s+implements\s+(?P<imp>[\w.<>, ]+?))?\s*\{",
                   re.M)


def declaracion(clase):
    texto = jf.fuente(clase).read_text(encoding="utf-8")
    for m in _DECL.finditer(texto):
        if m.group("n") == clase:
            def nombres(g):
                if not g:
                    return []
                limpio = re.sub(r"<[^<>]*(<[^<>]*>[^<>]*)*>", "", g)
                return [x.strip().split(".")[-1] for x in limpio.split(",") if x.strip()]
            ext, imp = nombres(m.group("ext")), nombres(m.group("imp"))
            if m.group("tipo") == "interface":
                return "interface", [], ext
            return m.group("tipo"), ext[:1], imp
    return "class", [], []


def _tipos(texto):
    return set(re.findall(r"[A-Z]\w+", texto))


def tipos_de_campos(clase):
    return set().union(*[_tipos(c.split(": ", 1)[1]) for c in jf.campos(clase)]) if jf.campos(clase) else set()


def miembros(clase, tipo, capa):
    """Líneas de la caja: campos (entidades) y los métodos del contrato, con tope para que la caja sea legible."""
    lineas = []
    if capa == "persistencia" and clase.endswith("Entity"):
        lineas += [c.replace("- ", "+ ", 1) for c in jf.campos(clase)][:10]
    elif tipo == "enum":
        pass
    metodos = [m for m in jf.metodos(clase, incluir_privados=False) if m[0] in "+#"]
    if capa == "presentacion":
        metodos = [m for m in metodos if " on" not in m[:5] or "onCreate" in m]
    if capa == "service":
        metodos = [m for m in metodos if "ejecutar" in m] or metodos
    tope = 12 if tipo == "interface" else 6
    if lineas:
        lineas.append("--")
    lineas += metodos[:tope]
    if len(metodos) > tope:
        lineas.append(f"... (+{len(metodos) - tope})")
    return lineas


def clase_puml(clase, capa):
    tipo, _, _ = declaracion(clase)
    abstracta = re.search(r"abstract\s+class\s+" + clase + r"\b", jf.fuente(clase).read_text(encoding="utf-8"))
    if tipo == "interface":
        cab = f"interface {clase}"
    elif tipo == "enum":
        cab = f"enum {clase}"
    elif abstracta:
        cab = f"abstract class {clase}"
    else:
        cab = f"class {clase}"
        if clase.endswith("Entity"):
            cab += " <<Entity>>"
        elif clase.endswith("Dao"):
            cab += " <<Dao>>"
    cuerpo = miembros(clase, tipo, capa)
    return cab + " {\n" + "".join(f"  {l}\n" for l in cuerpo) + "}"


def diagrama_clases(epica):
    capas = {c: clases_de(epica, c) for c, _, _ in CAPAS}
    todas = {c: capa for capa, lst in capas.items() for c in lst}
    nombres = set(todas)
    out = [CABECERA, f"title {TITULOS[epica]}\n"]
    for capa, paquete, color in CAPAS:
        if not capas[capa]:
            continue
        out.append(f'package "{paquete}" as p_{capa} {color} {{')
        out += ["  " + l if l else l for c in sorted(capas[capa]) for l in clase_puml(c, capa).split("\n")]
        out.append("}\n")
    relaciones = []
    for c, capa in todas.items():
        tipo, ext, imp = declaracion(c)
        for e in ext:
            if e in nombres:
                relaciones.append(f"{c} --|> {e}")
        for i in imp:
            if i in nombres:
                relaciones.append(f"{c} ..|> {i}")
        implicitos = set(ext) | set(imp)
        for t in sorted(tipos_de_campos(c) & nombres):
            if t == c or t in implicitos:
                continue
            destino = todas[t]
            if destino == "dominio" and capa != "dominio":
                continue
            if capa == "persistencia" and t.endswith("Entity") is False and destino == "persistencia" and False:
                continue
            relaciones.append(f"{c} --> {t}")
    vistas = []
    for r in relaciones:
        if r not in vistas:
            vistas.append(r)
    out += vistas
    out.append("@enduml")
    return "\n".join(out)


# ---------------------------------------------------------------- render
TEXTOS = None  # si es un dict, renderizar solo guarda el texto (lo usa el cotejo)


def renderizar(nombre, texto):
    if TEXTOS is not None:
        TEXTOS[nombre] = texto
        return
    import os
    SALIDA.mkdir(parents=True, exist_ok=True)
    UML.mkdir(parents=True, exist_ok=True)
    ruta = UML / f"{nombre}.puml"
    ruta.write_text(texto, encoding="utf-8")
    entorno = dict(os.environ, GRAPHVIZ_DOT=str(DOT))
    r = subprocess.run([str(JAVA), "-DPLANTUML_LIMIT_SIZE=16384", "-jar", str(JAR), "-tpng", "-charset", "UTF-8",
                        "-o", str(SALIDA), str(ruta)], capture_output=True, text=True, env=entorno)
    if r.returncode != 0:
        raise SystemExit(f"PlantUML falló con {nombre}: {r.stdout}{r.stderr}")


def generar(solo=None):
    for epica in EPICAS:
        nombre = f"diagrama_clases_{epica}"
        if solo and solo != nombre:
            continue
        renderizar(nombre, diagrama_clases(epica))
        if TEXTOS is None:
            print(nombre)


# ---------------------------------------------------------------- secuencias con actores
def _secuencia(nombre, titulo, participantes, mensajes, actores_extra=()):
    """participantes: [(nombre, clase|None|'actor:Etiqueta')]; el índice 0 es el actor que inicia."""
    o = ["@startuml", "skinparam backgroundColor white", "skinparam shadowing false", "skinparam defaultFontName Arial",
         "skinparam defaultFontSize 13", "skinparam sequenceMessageAlign left", "skinparam maxMessageSize 220", f"title {titulo}"]
    for i, (n, clase) in enumerate(participantes):
        if clase is None or (isinstance(clase, str) and clase.startswith("actor:")):
            et = n if clase is None else clase[6:]
            o.append(f'actor "{et}" as P{i}')
        elif n.endswith("Activity"):
            o.append(f'boundary "{n}" as P{i}')
        elif n.endswith("Dao") or n.endswith("Room"):
            o.append(f'database "{n}" as P{i}')
        elif n.endswith("Service") or n.endswith("ViewModel"):
            o.append(f'control "{n}" as P{i}')
        else:
            o.append(f'entity "{n}" as P{i}')
    dc.verificar_mensajes(mensajes)
    o.append("autonumber")
    for orig, dest, texto, _, ret in mensajes:
        flecha = "-->" if ret else "->"
        if orig == dest:
            o.append(f"P{orig} -> P{orig} : {texto}")
        else:
            o.append(f"P{orig} {flecha} P{dest} : {texto}")
    o.append("@enduml")
    renderizar(nombre, "\n".join(o))
    if TEXTOS is None:
        print(nombre)


def _desplazar(mensajes, n=1):
    return [(a + n, b + n, t, r, ret) for a, b, t, r, ret in mensajes]


def generar_secuencias():
    registrar = [
        (0, 1, "abre el formulario «Nueva materia»", ("MateriaFormularioActivity", "onCreate"), False),
        (0, 1, "escribe el nombre de la materia", None, False),
        (0, 1, "escribe el docente (opcional)", None, False),
        (0, 1, "escribe la meta semanal de horas", None, False),
        (0, 1, "elige el color: onColorClick(View)", ("MateriaFormularioActivity", "onColorClick"), False),
    ]
    _secuencia("secuencia_registrar_materia", "Secuencia · registrar una materia (HU-06)",
               dc.PARTICIPANTES, registrar + dc.MENSAJES)
    planificar = [
        (0, 1, "abre el formulario «Nueva sesión»", ("SesionFormularioActivity", "onCreate"), False),
        (0, 1, "elige la materia: onMateriaElegida(...)", ("SesionFormularioActivity", "onMateriaElegida"), False),
        (0, 1, "elige la fecha: onElegirFechaClick(View)", ("SesionFormularioActivity", "onElegirFechaClick"), False),
        (0, 1, "elige la hora de inicio: onElegirHoraInicioClick(View)",
         ("SesionFormularioActivity", "onElegirHoraInicioClick"), False),
        (0, 1, "elige la hora de fin: onElegirHoraFinClick(View)",
         ("SesionFormularioActivity", "onElegirHoraFinClick"), False),
        (0, 1, "escribe las notas (opcional)", None, False),
    ]
    _secuencia("secuencia_planificar_sesion", "Secuencia · planificar una sesión de estudio (HU-10, RN-05 y RN-07)",
               dc.PARTICIPANTES_PLANIFICAR_SESION, planificar + dc.MENSAJES_PLANIFICAR_SESION)
    part = [("Estudiante", None)] + dc.PARTICIPANTES_CALENDARIO
    msg = ([(0, 1, "abre la pestaña Calendario", ("CalendarioActivity", "onCreate"), False),
            (0, 1, "elige la vista: día, semana o mes", None, False)] + _desplazar(dc.MENSAJES_CALENDARIO))
    _secuencia("secuencia_consultar_calendario", "Secuencia · consultar calendario de estudio (HU-16 a HU-21, RN-08, RN-11)",
               part, msg)
    part = [("Estudiante", None)] + dc.PARTICIPANTES_EJECUCION + [("Android (AlarmManager)", "actor:Android (AlarmManager)")]
    msg = ([(0, 1, "abre el detalle de la sesión pendiente", ("SesionDetalleActivity", "onCreate"), False),
            (0, 1, "pulsa «Iniciar sesión»: onIniciarClick(View)", ("SesionDetalleActivity", "onIniciarClick"), False)]
           + _desplazar(dc.MENSAJES_EJECUCION)
           + [(6, 7, "programa la alarma del siguiente hito", None, False),
              (7, 6, "al vencer, dispara el aviso (ReceptorAvisosSesion)", None, True)])
    _secuencia("secuencia_ejecucion_sesion", "Secuencia · iniciar y ejecutar sesión en vivo (HU-22 a HU-24, HU-39, RN-22)",
               part, msg)


if __name__ == "__main__":
    solo = sys.argv[2] if len(sys.argv) > 2 and sys.argv[1] == "--solo" else None
    generar(solo)
    if not solo:
        generar_secuencias()
