"""Genera docs/entrega1/StudyPlan_Entrega1.docx en formato APA 7.

Uso:
    python docs/build/generar_docx.py
    powershell -File docs/build/actualizar_word.ps1   # actualiza índices y exporta a PDF (necesita MS Word)

Los requisitos se leen de requisitos.py (fuente única compartida con las specs de OpenSpec).
"""
import re
import sys
from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK, WD_LINE_SPACING, WD_COLOR_INDEX
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

sys.path.insert(0, str(Path(__file__).parent))
from requisitos import (CALIDAD_POR_EPICA, EPICAS, ESTADOS_NOMBRE, HISTORIAS, REGLAS, RNF,  # noqa: E402
                        RNF_TRANSVERSALES, ROL, TRANSICIONES, VERSION)

ROOT = Path(__file__).resolve().parents[2]
DIAG = ROOT / "docs" / "diagramas"
OUT = ROOT / "docs" / "entrega1" / "StudyPlan_Entrega1.docx"

# Datos de portada: se leen de docs/build/.env (NO versionado; ver .env.example).
# Sin .env se usan marcadores genéricos resaltados en amarillo para completar a mano.
def _cargar_env(path):
    datos = {}
    if path.exists():
        for linea in path.read_text(encoding="utf-8").splitlines():
            linea = linea.strip()
            if linea and not linea.startswith("#") and "=" in linea:
                k, v = linea.split("=", 1)
                datos[k.strip()] = v.strip().strip('"').strip("'")
    return datos


_ENV = _cargar_env(Path(__file__).parent / ".env")


def _dato(clave, marcador):
    return _ENV.get(clave) or f"[[{marcador}]]"


AUTOR = _dato("AUTOR", "Nombre completo del estudiante")
UNIVERSIDAD = _dato("UNIVERSIDAD", "Universidad")
FACULTAD = _dato("FACULTAD", "Facultad")
CARRERA = _dato("CARRERA", "Carrera")
MATERIA = _dato("MATERIA", "Materia")
SIGLA = _dato("SIGLA", "Sigla")
DOCENTE = _dato("DOCENTE", "Nombre del docente")
FECHA_ENTREGA = _dato("FECHA_ENTREGA", "Fecha de entrega")

FONT = "Times New Roman"
LANG = "es-BO"
TEXT_W = 16.51  # cm (Carta con márgenes de 2,54 cm)

doc = Document()


# ------------------------------------------------------------------ estilos
def set_lang(rpr):
    lang = rpr.find(qn("w:lang"))
    if lang is None:
        lang = OxmlElement("w:lang")
        rpr.append(lang)
    lang.set(qn("w:val"), LANG)
    lang.set(qn("w:eastAsia"), LANG)


def style_font(st, size=12, bold=None, italic=None):
    st.font.name = FONT
    st.font.size = Pt(size)
    st.font.color.rgb = RGBColor(0, 0, 0)
    if bold is not None:
        st.font.bold = bold
    if italic is not None:
        st.font.italic = italic
    rpr = st.element.get_or_add_rPr()
    rfonts = rpr.find(qn("w:rFonts"))
    if rfonts is None:
        rfonts = OxmlElement("w:rFonts")
        rpr.append(rfonts)
    for k in ("w:ascii", "w:hAnsi", "w:cs", "w:eastAsia"):
        rfonts.set(qn(k), FONT)
    for k in ("w:asciiTheme", "w:hAnsiTheme", "w:cstheme", "w:eastAsiaTheme"):
        if rfonts.get(qn(k)) is not None:
            del rfonts.attrib[qn(k)]
    set_lang(rpr)


styles = doc.styles
normal = styles["Normal"]
style_font(normal)
pf = normal.paragraph_format
pf.line_spacing_rule = WD_LINE_SPACING.DOUBLE
pf.space_before = Pt(0)
pf.space_after = Pt(0)
pf.widow_control = True

body = styles.add_style("Cuerpo APA", 1)
body.base_style = normal
body.paragraph_format.first_line_indent = Cm(1.27)

for lvl, (align, bold, italic) in {1: (WD_ALIGN_PARAGRAPH.CENTER, True, False),
                                    2: (WD_ALIGN_PARAGRAPH.LEFT, True, False),
                                    3: (WD_ALIGN_PARAGRAPH.LEFT, True, True),
                                    4: (WD_ALIGN_PARAGRAPH.LEFT, True, False)}.items():
    st = styles[f"Heading {lvl}"]
    style_font(st, 12, bold, italic)
    st.paragraph_format.alignment = align
    st.paragraph_format.line_spacing_rule = WD_LINE_SPACING.DOUBLE
    st.paragraph_format.space_before = Pt(12 if lvl == 1 else 6)
    st.paragraph_format.space_after = Pt(0)
    st.paragraph_format.keep_with_next = True
    st.paragraph_format.first_line_indent = Cm(1.27 if lvl == 4 else 0)

cap = styles["Caption"]
style_font(cap, 12, False, False)
cap.paragraph_format.line_spacing_rule = WD_LINE_SPACING.DOUBLE
cap.paragraph_format.space_before = Pt(12)
cap.paragraph_format.space_after = Pt(0)
cap.paragraph_format.keep_with_next = True

tabla_txt = styles.add_style("Texto tabla", 1)
style_font(tabla_txt, 10)
tabla_txt.paragraph_format.line_spacing_rule = WD_LINE_SPACING.SINGLE
tabla_txt.paragraph_format.space_after = Pt(2)
tabla_txt.paragraph_format.first_line_indent = Cm(0)

for name in ("List Bullet", "List Number"):
    st = styles[name]
    style_font(st)
    st.paragraph_format.line_spacing_rule = WD_LINE_SPACING.DOUBLE

for i in range(1, 4):
    st = styles[f"TOC {i}"] if f"TOC {i}" in [s.name for s in styles] else styles.add_style(f"TOC {i}", 1)
    style_font(st)
    st.paragraph_format.left_indent = Cm(0.6 * (i - 1))
    st.paragraph_format.line_spacing_rule = WD_LINE_SPACING.ONE_POINT_FIVE

# ------------------------------------------------------------------ página
sec = doc.sections[0]
sec.page_width, sec.page_height = Cm(21.59), Cm(27.94)
for side in ("left_margin", "right_margin", "top_margin", "bottom_margin"):
    setattr(sec, side, Cm(2.54))


def add_field(par, instr, placeholder=""):
    r = par.add_run()
    b = OxmlElement("w:fldChar")
    b.set(qn("w:fldCharType"), "begin")
    r._r.append(b)
    r = par.add_run()
    t = OxmlElement("w:instrText")
    t.set(qn("xml:space"), "preserve")
    t.text = f" {instr} "
    r._r.append(t)
    r = par.add_run()
    s = OxmlElement("w:fldChar")
    s.set(qn("w:fldCharType"), "separate")
    r._r.append(s)
    pr = par.add_run(placeholder)
    r = par.add_run()
    e = OxmlElement("w:fldChar")
    e.set(qn("w:fldCharType"), "end")
    r._r.append(e)
    return pr


hdr = sec.header.paragraphs[0]
hdr.alignment = WD_ALIGN_PARAGRAPH.RIGHT
add_field(hdr, "PAGE", "1")

# ------------------------------------------------------------------ helpers de contenido
TOKEN = re.compile(r"(\*\*[^*]+\*\*|\*[^*]+\*|\[\[[^\]]+\]\])")


def runs(par, text, size=None):
    """Admite **negrita**, *cursiva* y [[marcador a completar]] (resaltado)."""
    for part in TOKEN.split(text):
        if not part:
            continue
        if part.startswith("**"):
            r = par.add_run(part[2:-2])
            r.bold = True
        elif part.startswith("[["):
            r = par.add_run(part[2:-2])
            r.font.highlight_color = WD_COLOR_INDEX.YELLOW
        elif part.startswith("*"):
            r = par.add_run(part[1:-1])
            r.italic = True
        else:
            r = par.add_run(part)
        if size:
            r.font.size = Pt(size)
    return par


def P(text, style="Cuerpo APA", align=None):
    p = runs(doc.add_paragraph(style=style), text)
    if align is not None:
        p.alignment = align
    return p


def H(level, text):
    return doc.add_heading(text, level)


def bullets(items, style="List Bullet"):
    for it in items:
        runs(doc.add_paragraph(style=style), it)


def page_break():
    doc.add_paragraph().add_run().add_break(WD_BREAK.PAGE)


def no_toc_title(text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run(text)
    r.bold = True
    return p


counters = {"Tabla": 0, "Figura": 0}
keys = {}


def caption(kind, title, key=None):
    counters[kind] += 1
    if key:
        keys[(kind[0], key)] = counters[kind]
    p = doc.add_paragraph(style="Caption")
    r = p.add_run(f"{kind} ")
    r.bold = True
    fr = add_field(p, f"SEQ {kind} \\* ARABIC", str(counters[kind]))
    fr.bold = True
    p.add_run().add_break(WD_BREAK.LINE)
    t = p.add_run(title)
    t.italic = True
    return counters[kind]


def note(text):
    p = runs(doc.add_paragraph(style="Texto tabla"), "*Nota.* " + text)
    p.paragraph_format.space_before = Pt(4)
    p.paragraph_format.space_after = Pt(12)
    return p


def set_cell_border(cell, **kw):
    tcPr = cell._tc.get_or_add_tcPr()
    borders = tcPr.find(qn("w:tcBorders"))
    if borders is None:
        borders = OxmlElement("w:tcBorders")
        tcPr.append(borders)
    for edge, val in kw.items():
        el = OxmlElement(f"w:{edge}")
        el.set(qn("w:val"), val)
        el.set(qn("w:sz"), "8")
        el.set(qn("w:space"), "0")
        el.set(qn("w:color"), "000000")
        borders.append(el)


def shade(cell, fill="E7E6E6"):
    tcPr = cell._tc.get_or_add_tcPr()
    s = OxmlElement("w:shd")
    s.set(qn("w:val"), "clear")
    s.set(qn("w:color"), "auto")
    s.set(qn("w:fill"), fill)
    tcPr.append(s)


def set_widths(table, widths):
    table.autofit = False
    for row in table.rows:
        for c, w in zip(row.cells, widths):
            c.width = Cm(w)


def repeat_header(row):
    trPr = row._tr.get_or_add_trPr()
    el = OxmlElement("w:tblHeader")
    el.set(qn("w:val"), "true")
    trPr.append(el)


def cell_text(cell, text, bold=False, size=10):
    cell.paragraphs[0].style = styles["Texto tabla"]
    lines = text.split("\n")
    for i, line in enumerate(lines):
        p = cell.paragraphs[0] if i == 0 else cell.add_paragraph(style="Texto tabla")
        runs(p, f"**{line}**" if bold and line else line, size)


def apa_table(headers, rows, widths, size=10):
    """Tabla APA: solo bordes horizontales (superior, bajo el encabezado, inferior)."""
    t = doc.add_table(rows=1 + len(rows), cols=len(headers))
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    for c, h in zip(t.rows[0].cells, headers):
        cell_text(c, h, bold=True, size=size)
        set_cell_border(c, top="single", bottom="single")
    repeat_header(t.rows[0])
    for r, data in zip(t.rows[1:], rows):
        for c, v in zip(r.cells, data):
            cell_text(c, str(v), size=size)
    for c in t.rows[-1].cells:
        set_cell_border(c, bottom="single")
    set_widths(t, widths)
    return t


def grid_table(rows, widths, size=10, label_col=True):
    t = doc.add_table(rows=len(rows), cols=len(widths))
    t.style = doc.styles["Table Grid"]
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    for r, data in zip(t.rows, rows):
        for i, (c, v) in enumerate(zip(r.cells, data)):
            cell_text(c, v, bold=(label_col and i == 0), size=size)
            if label_col and i == 0:
                shade(c)
    set_widths(t, widths)
    return t


def figure(path, width_cm, title, nota=None, key=None):
    caption("Figura", title, key)
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.keep_with_next = bool(nota)
    p.add_run().add_picture(str(path), width=Cm(width_cm))
    if nota:
        note(nota)


# =================================================================== PORTADA
for _ in range(3):
    doc.add_paragraph()
t = doc.add_paragraph()
t.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = t.add_run("StudyPlan: Aplicación Móvil Android para Gestionar y Planificar el Tiempo de Estudio "
              "del Estudiante Universitario")
r.bold = True
t2 = doc.add_paragraph()
t2.alignment = WD_ALIGN_PARAGRAPH.CENTER
t2.add_run(f"Primera entrega (versión del producto {VERSION}): Introducción, Marco Teórico, Requisitos y "
           "Auditoría IA")
doc.add_paragraph()
for line in [AUTOR,
             f"{UNIVERSIDAD} — {FACULTAD}, {CARRERA}",
             f"{SIGLA} {MATERIA}",
             f"Docente: {DOCENTE}",
             FECHA_ENTREGA]:
    p = runs(doc.add_paragraph(), line)
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
page_break()

# =================================================================== PRELIMINARES
no_toc_title("Lista de Tablas")
p = doc.add_paragraph()
add_field(p, 'TOC \\h \\z \\c "Tabla"', "Haga clic derecho → Actualizar campo para generar la lista de tablas.")
page_break()
no_toc_title("Lista de Figuras")
p = doc.add_paragraph()
add_field(p, 'TOC \\h \\z \\c "Figura"', "Haga clic derecho → Actualizar campo para generar la lista de figuras.")
page_break()
no_toc_title("Índice de Contenidos")
p = doc.add_paragraph()
add_field(p, 'TOC \\o "1-4" \\h \\z \\u', "Haga clic derecho → Actualizar campo para generar el índice.")
page_break()

# =================================================================== CAPÍTULO 1
H(1, "Capítulo 1. Introducción")
H(2, "1.1. Introducción y Contexto del Problema")
P("La vida universitaria exige que el estudiante administre de forma autónoma su tiempo entre clases, "
  "trabajos prácticos, exámenes y, con frecuencia, actividades laborales o familiares. A diferencia de "
  "la educación secundaria, donde el horario lo estructura la institución, en la universidad gran "
  "parte del aprendizaje ocurre en horas de estudio independiente que el propio estudiante debe "
  "planificar, ejecutar y evaluar.")
P("La literatura muestra que esta autogestión es un problema real. En un metaanálisis sobre la "
  "procrastinación, Steel (2007) estima que entre el 80 % y el 95 % de los estudiantes universitarios "
  "posterga sus tareas académicas, y relaciona este comportamiento con un menor rendimiento. Los "
  "estudios sobre gestión del tiempo indican, a su vez, que la planificación a corto plazo predice "
  "mejores calificaciones (Britton & Tesser, 1991) y que la percepción de control sobre el propio "
  "tiempo se asocia con mayor rendimiento y menor estrés (Macan et al., 1990).")
P("En la práctica, muchos estudiantes planifican de manera informal (cuadernos, notas sueltas o "
  "calendarios genéricos) y no registran cuántas horas estudian realmente frente a las que "
  "planificaron, ni qué sesiones dejaron de cumplir. Sin ese dato no pueden saber a qué materias "
  "dedican poco tiempo, si avanzan hacia sus objetivos o si su plan es realista. Las aplicaciones de "
  "calendario o de tareas existentes no están orientadas a medir horas de estudio por materia ni a "
  "comparar lo planificado con lo ejecutado.")
P("El presente proyecto propone **StudyPlan**, una aplicación móvil nativa para Android de "
  "**autocontrol del estudio**. Permite al estudiante gestionar sus materias, planificar sesiones en un "
  "calendario de estudio (vistas de día, semana y mes), ejecutar las sesiones con técnicas como "
  "Pomodoro, fijar metas semanales y consultar reportes y estadísticas que reflejan honestamente lo "
  "estudiado y lo no estudiado. El proyecto se desarrolla con asistencia de inteligencia artificial "
  "(IA) mediante desarrollo guiado por especificaciones (*Spec-Driven Development*) y documenta y "
  "audita cada decisión, de modo que el estudiante pueda defender técnicamente todo artefacto generado.")

H(2, "1.2. Justificación del Proyecto")
P("**Justificación académica.** El proyecto aplica el ciclo completo de ingeniería de software: "
  "levantamiento y especificación de requisitos, diseño, implementación y pruebas. Usa una metodología "
  "ágil (Scrum) con artefactos ágiles coherentes, historias de usuario y criterios de aceptación, sin "
  "mezclar técnicas de metodologías tradicionales.")
P("**Justificación técnica.** Se desarrolla en Java nativo para Android, el lenguaje enseñado en la "
  "materia, con componentes oficiales de Android Jetpack (Room, ViewModel, LiveData, WorkManager) y "
  "Material Design 3. La aplicación es *offline-first*: funciona por completo sin conexión, sin "
  "servidor propio, y solo usa internet para la copia de seguridad opcional en el Google Drive del "
  "propio usuario. El uso de OpenSpec permite que la IA genere código a partir de especificaciones "
  "validadas y que cada línea de código pueda rastrearse hasta una historia de usuario.")
P("**Justificación social.** Una herramienta gratuita que ayuda a planificar el estudio y a "
  "visualizar el progreso fortalece la autorregulación del aprendizaje (Zimmerman, 2002) y el "
  "establecimiento de metas (Locke & Latham, 2002), factores asociados al éxito académico. La "
  "aplicación se publicará en Google Play.")
P("**Justificación profesional.** Ante la pregunta del docente sobre qué diferencia a un programador "
  "de otra persona que usa IA, este proyecto sostiene que la diferencia está en **especificar, validar "
  "y defender**. La sección *Auditoría IA* muestra un caso concreto: la IA redactó requisitos sin "
  "consultar y el estudiante los rechazó y los levantó mediante una entrevista.")

H(2, "1.3. Objetivos")
H(3, "1.3.1. Objetivo General")
P("Desarrollar una aplicación móvil nativa para Android, en lenguaje Java, que permita a los "
  "estudiantes universitarios gestionar sus materias, planificar y ejecutar sesiones de estudio en un "
  "calendario, establecer metas semanales y consultar reportes y estadísticas de sus horas de estudio, "
  "aplicando Scrum y desarrollo guiado por especificaciones con asistencia auditada de inteligencia "
  "artificial.")
H(3, "1.3.2. Objetivos Específicos")
bullets([
    "Levantar y especificar los requisitos funcionales como historias de usuario con criterios de "
    "aceptación, las reglas de negocio del dominio y los requisitos no funcionales según ISO/IEC "
    "25010:2023, versionándolos como especificaciones en OpenSpec.",
    "Implementar la gestión de materias y la planificación de sesiones de estudio gobernadas por una "
    "máquina de estados que conserve el historial de cumplimiento.",
    "Implementar un calendario de estudio con vistas de día, semana tipo horario y mes, que muestre el "
    "estado de cada sesión y permita planificar desde él.",
    "Implementar técnicas de apoyo a la concentración: cronómetro, Pomodoro configurable y modo No "
    "molestar.",
    "Implementar metas semanales totales y por materia, estadísticas y reportes exportables en PDF y CSV.",
    "Proteger los datos del estudiante mediante copia de seguridad configurable en Google Drive con "
    "cifrado opcional, bloqueo biométrico y borrado selectivo.",
    "Validar la aplicación con pruebas unitarias, pruebas instrumentadas y una evaluación de "
    "usabilidad con estudiantes, y documentar y auditar el uso de IA durante todo el desarrollo.",
])

H(2, "1.4. Alcance del Proyecto")
P(f"La versión {VERSION} de StudyPlan, primer entregable funcional, comprende:")
bullets([
    "Aplicación Android nativa (Java) para Android 7.0 (API 24) o superior, en español e inglés, con "
    "tema claro y oscuro.",
    "Un único rol de usuario, el estudiante, sin cuentas propias. Los datos se almacenan en el "
    "dispositivo (SQLite mediante Room) y la aplicación funciona sin conexión (*offline-first*).",
    "Módulos: perfil, inicio y ajustes (bloqueo con huella o PIN y widget); gestión de materias; "
    "sesiones de estudio con máquina de estados; calendario de estudio (día, semana y mes); técnicas y "
    "eventos de estudio (cronómetro por marcas de tiempo, Pomodoro, No molestar, tiempo extra y estudio "
    "con la app cerrada); metas semanales; estadísticas y métricas de concentración; reportes; "
    "recordatorios; y respaldo y privacidad.",
    "Copia de seguridad en el Google Drive del propio usuario, con frecuencia configurable y cifrado "
    "opcional con contraseña. Es el único uso de internet.",
    "Publicación en Google Play con el identificador permanente *io.github.jhoel0521.studyplan*.",
])
P("Quedan **fuera del alcance** de esta versión: roles adicionales (tutor o docente), un servidor o "
  "backend propio, la sincronización en tiempo real entre dispositivos, las versiones para iOS y web, "
  "la integración con calendarios externos y la gamificación avanzada. Estas funciones podrán "
  "evaluarse en versiones posteriores.")
P("En cuanto a la documentación, esta primera entrega cubre los capítulos 1 a 3.2.2 del índice del "
  "proyecto, más la sección *Auditoría IA*, un punto especial que se agrega al índice por indicación "
  "del docente para documentar el uso de inteligencia artificial.")
page_break()

# =================================================================== CAPÍTULO 2
H(1, "Capítulo 2. Marco Teórico y Referencial")
H(2, "2.1. Marco Teórico")
H(3, "2.1.1. Gestión del tiempo de estudio")
P("La gestión del tiempo es el conjunto de conductas orientadas a usar el tiempo de forma efectiva "
  "para alcanzar objetivos: fijar metas y prioridades, planificar y supervisar la ejecución. Macan et "
  "al. (1990) encontraron que, en estudiantes universitarios, la percepción de control del tiempo se "
  "relaciona con un mejor promedio, mayor satisfacción y menor tensión. Britton y Tesser (1991) "
  "mostraron que la planificación de corto plazo y la actitud hacia el tiempo predicen el promedio "
  "acumulado mejor que las pruebas de aptitud. StudyPlan implementa estas conductas: planificar "
  "sesiones en un calendario, fijar metas semanales y supervisar el cumplimiento.")
H(3, "2.1.2. Procrastinación académica")
P("Steel (2007) define la procrastinación como el retraso voluntario de una acción prevista pese a "
  "esperar consecuencias negativas por ello. Su metaanálisis la describe como un fallo de "
  "autorregulación que disminuye cuando las tareas son concretas y cercanas en el tiempo. Por ello la "
  "aplicación descompone el estudio en sesiones con fecha y hora, emite recordatorios y registra como "
  "*No realizada* toda sesión no cumplida, para que el estudiante tome conciencia de sus "
  "postergaciones.")
H(3, "2.1.3. Establecimiento de metas")
P("Según la teoría del establecimiento de metas de Locke y Latham (2002), las metas específicas y "
  "desafiantes producen mejor desempeño que metas vagas como «hacer lo mejor posible», siempre que "
  "exista retroalimentación sobre el progreso. La meta semanal de horas, total y por materia, con su "
  "barra de progreso y la felicitación al cumplirla, aplica directamente este principio.")
H(3, "2.1.4. Aprendizaje autorregulado")
P("Zimmerman (2002) describe el aprendizaje autorregulado como un ciclo de tres fases: previsión "
  "(planificar y fijar metas), ejecución (autocontrol y autoobservación) y autorreflexión (evaluar "
  "los resultados). Los módulos de StudyPlan se corresponden con estas fases: calendario y metas "
  "(previsión), sesión en curso con cronómetro, Pomodoro y No molestar (ejecución), y reportes y "
  "estadísticas de cumplimiento (autorreflexión).")
H(3, "2.1.5. Técnica Pomodoro y control de distracciones")
P("La técnica Pomodoro propone trabajar en bloques cortos de concentración, tradicionalmente de 25 "
  "minutos, separados por descansos de 5 minutos y por un descanso largo cada cuatro bloques (Cirillo, "
  "2018). StudyPlan la implementa con tiempos configurables y suma como horas de estudio solo los "
  "bloques de trabajo. Como complemento, el modo No molestar silencia las notificaciones del teléfono "
  "durante la sesión para reducir las interrupciones.")
P("El estudio real no siempre coincide con el plan: el estudiante puede inspirarse y estudiar horas de "
  "más, atender una llamada, o dejar el celular y seguir estudiando en grupo. Por eso StudyPlan registra "
  "**eventos de estudio** con marca de tiempo (inicio, pausas, salidas y regresos a la app, fin "
  "planificado, tiempo extra, cambios de materia) y deja que el estudiante declare el tiempo estudiado "
  "con la app cerrada. A partir de esos eventos calcula métricas de concentración, puntualidad y "
  "respeto del tiempo, que alimentan la fase de autorreflexión descrita por Zimmerman (2002).")
H(3, "2.1.6. Ingeniería de requisitos ágil")
P("En los métodos ágiles los requisitos se expresan como **historias de usuario**: descripciones breves "
  "de una funcionalidad desde la perspectiva del usuario con el formato «Como [rol], quiero "
  "[funcionalidad] para [beneficio]» (Cohn, 2004). Una buena historia cumple los criterios **INVEST**: "
  "independiente, negociable, valiosa, estimable, pequeña y comprobable (Wake, 2003). Cada historia se "
  "completa con **criterios de aceptación** escritos como *Dado / Cuando / Entonces*, forma que proviene "
  "del desarrollo guiado por comportamiento (North, 2006) y permite convertir cada criterio en una "
  "prueba. Las historias se priorizan con **MoSCoW** (Must, Should, Could, Won't; Clegg & Barker, 1994) "
  "y se estiman en puntos de historia. Las **reglas de negocio** del dominio se documentan aparte y se "
  "referencian desde los criterios de aceptación. Así una misma regla, por ejemplo el límite de tres "
  "días para el registro tardío, no se repite ni se contradice entre historias.")
P("Los requisitos no funcionales se expresan como **criterios de calidad** según el modelo de calidad "
  "del producto ISO/IEC 25010:2023, que define nueve características: adecuación funcional, eficiencia "
  "de desempeño, compatibilidad, capacidad de interacción (antes usabilidad), fiabilidad, seguridad, "
  "mantenibilidad, flexibilidad (antes portabilidad) y protección (*safety*) (International "
  "Organization for Standardization [ISO], 2023).")
H(3, "2.1.7. Máquinas de estado y patrón State")
P("Una máquina de estados describe los estados posibles de un objeto y los eventos que provocan las "
  "transiciones entre ellos. El patrón de diseño **State** (Gamma et al., 1994) implementa esta idea "
  "encapsulando cada estado en una clase propia: cada estado sabe únicamente qué acciones admite y a "
  "qué estado pasa, sin conocer el contexto completo. En StudyPlan la sesión de estudio sigue una "
  "máquina de estados (sección 3.2.1) que impide transiciones inválidas y conserva el registro "
  "histórico de lo no estudiado.")
H(3, "2.1.8. Desarrollo guiado por especificaciones con IA")
P("El *Spec-Driven Development* (SDD) es un enfoque en el que la especificación del comportamiento se "
  "escribe y se aprueba **antes** del código, y se convierte en el contrato contra el cual se genera y "
  "verifica la implementación. Con asistentes de IA, el SDD reduce el riesgo de que el modelo invente "
  "requisitos o se desvíe del alcance, porque el asistente trabaja sobre un documento versionado y "
  "validable. OpenSpec es una herramienta de código abierto que implementa este flujo mediante "
  "*changes* (propuesta, especificaciones delta, diseño y tareas) que, una vez implementados, se "
  "archivan y actualizan las especificaciones vigentes del sistema (Fission-AI, 2026).")

H(2, "2.2. Marco Tecnológico")
P("La ⟦T:tecno⟧ resume las tecnologías seleccionadas. La justificación de cada elección se detalla a "
  "continuación y en el documento de diseño del *change* de OpenSpec.")
caption("Tabla", "Tecnologías seleccionadas para StudyPlan", "tecno")
apa_table(["Capa / necesidad", "Tecnología", "Versión / nivel", "Uso en el proyecto"], [
    ["Lenguaje", "Java", "Java 11 (compatibilidad del proyecto)", "Todo el código de la aplicación"],
    ["Plataforma", "Android SDK", "minSdk 24 · targetSdk 37", "Ejecución en Android 7.0 o superior"],
    ["IDE y build", "Android Studio · Gradle (Kotlin DSL)", "AGP 9.4.1 · version catalog", "Compilación, emulación, depuración"],
    ["Arquitectura", "MVVM con Android Jetpack", "ViewModel, LiveData", "Separación de UI y lógica"],
    ["Persistencia", "Room sobre SQLite", "Jetpack Room", "Base de datos local y offline"],
    ["Interfaz", "Views XML + Material Design 3", "Material Components", "Componentes, tema claro/oscuro"],
    ["Calendario", "Vistas propias (RecyclerView y Canvas)", "Java puro", "Vistas de día, semana y mes"],
    ["Gráficos", "MPAndroidChart", "3.1.0", "Gráfico de anillo y barras"],
    ["Segundo plano", "WorkManager / AlarmManager", "Jetpack WorkManager", "Recordatorios, respaldos, cierre del día"],
    ["Respaldo", "Google Drive API (appDataFolder) · Credential Manager", "Alcance drive.appdata", "Copia de seguridad del usuario"],
    ["Seguridad", "javax.crypto (AES-256-GCM, PBKDF2) · androidx.biometric", "Java SE / Jetpack", "Cifrado del respaldo y bloqueo"],
    ["Sistema", "NotificationManager · AppWidgetProvider", "Android SDK", "No molestar y widget"],
    ["Ciclo de vida", "ProcessLifecycleOwner (lifecycle-process)", "Jetpack Lifecycle", "Detectar salida y regreso a la app"],
    ["Fechas", "java.time con desugaring", "Core library desugaring", "LocalDate/LocalTime en API 24"],
    ["Pruebas", "JUnit 4 · Espresso", "4.13.2 · 3.5.1", "Pruebas unitarias e instrumentadas"],
    ["IA y especificación", "Claude Code · OpenSpec", "Claude Opus 5.5 · OpenSpec 1.14.3", "Generación asistida y SDD"],
], [3.0, 4.6, 3.9, 5.0], size=9)
note("Las versiones de la plataforma corresponden a la plantilla generada por Android Studio el 4 de "
     "noviembre de 2026 (gradle/libs.versions.toml). Las demás dependencias se agregarán en los sprints "
     "de implementación, con aprobación humana previa.")

H(3, "2.2.1. Java y la plataforma Android")
P("Android admite Java y Kotlin. Desde 2019 Google promueve Kotlin como lenguaje preferente (Android "
  "Developers, s.f.-d), pero Java mantiene soporte completo en el SDK, en Android Studio y en Jetpack. "
  "Se eligió **Java** porque es el lenguaje exigido y enseñado en la materia, lo que permite al "
  "estudiante comprender y defender cada línea generada. Para usar java.time con minSdk 24 se habilita "
  "el *desugaring* de bibliotecas de Java 8+ (Android Developers, s.f.-e). La ⟦T:lenguajes⟧ compara las "
  "alternativas evaluadas.")
caption("Tabla", "Comparación de alternativas de lenguaje y framework", "lenguajes")
apa_table(["Criterio", "Java nativo", "Kotlin nativo", "Flutter (Dart)"], [
    ["Exigido / enseñado en la materia", "Sí", "No", "No"],
    ["Aplicación nativa Android", "Sí", "Sí", "No (motor propio de renderizado)"],
    ["Soporte de Jetpack (Room, WorkManager)", "Completo", "Completo y preferente", "Mediante plugins"],
    ["Capacidad del estudiante para defender el código", "Alta", "Media", "Baja"],
    ["Multiplataforma (iOS)", "No", "No (salvo KMP)", "Sí"],
    ["Decisión", "**Seleccionado**", "Descartado", "Descartado"],
], [6.0, 3.3, 3.6, 3.6])
note("Elaboración propia. La decisión del lenguaje fue humana y quedó registrada en la bitácora de "
     "Auditoría IA.")
H(3, "2.2.2. Arquitectura MVVM y Android Jetpack")
P("La *Guía de arquitectura de apps* de Android recomienda separar la interfaz de usuario de la capa de "
  "datos, con una fuente única de verdad y un flujo de datos unidireccional (Android Developers, "
  "s.f.-a). StudyPlan adopta el patrón Modelo–Vista–Modelo de Vista (MVVM): las *Activities* y "
  "*Fragments* solo muestran datos y capturan eventos; los *ViewModel* mantienen el estado de pantalla "
  "y sobreviven a la rotación; los *Repository* encapsulan el acceso a datos; y los *DAO* de Room "
  "ejecutan las consultas. Las reglas de negocio, incluida la máquina de estados, viven en una capa de "
  "dominio en Java puro, sin dependencias de Android, que puede probarse con JUnit.")
H(3, "2.2.3. Room y SQLite")
P("Room es una capa de abstracción sobre SQLite que valida las consultas SQL en tiempo de compilación, "
  "obliga a usar consultas parametrizadas y gestiona migraciones de esquema (Android Developers, "
  "s.f.-b). Los datos de StudyPlan son relacionales (una materia agrupa muchas sesiones) y deben "
  "funcionar sin conexión, por lo que Room resulta más adecuado que SharedPreferences (no relacional) "
  "o Firebase (requiere conexión y almacena datos en servidores de terceros).")
H(3, "2.2.4. Material Design 3")
P("Material Design 3 es el sistema de diseño de Google que define componentes, color, tipografía, "
  "temas claro y oscuro y pautas de accesibilidad (Google, s.f.). Su uso garantiza una interfaz "
  "familiar para el usuario de Android y apoya los criterios de accesibilidad (RNF-06) y estética "
  "(RNF-07).")
H(3, "2.2.5. Servicios del sistema: WorkManager, No molestar, biometría y widget")
P("WorkManager es la API recomendada para trabajo en segundo plano diferible y garantizado (Android "
  "Developers, s.f.-c). Se usará para los recordatorios, el respaldo periódico y el cierre automático "
  "del día (paso de sesiones pendientes a no realizadas). El modo No molestar se controla mediante la "
  "política de interrupciones de *NotificationManager*, que requiere que el usuario conceda el acceso. "
  "El bloqueo de la aplicación usa *BiometricPrompt*, que admite huella, rostro o el PIN del "
  "dispositivo (Android Developers, s.f.-f). El widget se implementa con *AppWidgetProvider*, "
  "compatible con Java. MPAndroidChart (Jahoda, 2019) dibuja los gráficos de estadísticas. Para los "
  "eventos de estudio no se usa un proceso que cuente segundos: se guardan marcas de tiempo en Room y "
  "el cronómetro solo se dibuja cuando la app está visible. *ProcessLifecycleOwner* detecta cuándo la "
  "app pasa a segundo plano y vuelve, para registrar las salidas y regresos y avisar al estudiante.")
H(3, "2.2.6. Copia de seguridad en Google Drive y cifrado")
P("La copia de seguridad se inspira en la de WhatsApp: el usuario vincula su cuenta Google, elige una "
  "frecuencia (nunca, diaria, semanal, mensual o solo manual) y puede proteger el archivo con una "
  "contraseña. Se usa la carpeta de datos de aplicación de Google Drive (*appDataFolder*), oculta al "
  "usuario y accesible solo por la aplicación (Google for Developers, s.f.). Si el usuario define una "
  "contraseña, el archivo se cifra en el teléfono con AES-256 en modo GCM, con una clave derivada de "
  "la contraseña mediante PBKDF2 (Turan et al., 2010). Así, ni Google ni el desarrollador pueden leer "
  "el contenido.")
H(3, "2.2.7. Herramientas de IA: Claude Code y OpenSpec")
P("Claude Code es un asistente de programación agéntico de Anthropic que opera en la terminal y el IDE: "
  "lee el repositorio, ejecuta comandos y propone cambios que el desarrollador aprueba (Anthropic, "
  "2026). OpenSpec (versión 1.14.3) se integra con Claude Code mediante comandos (/opsx:propose, "
  "/opsx:apply, /opsx:archive) y *skills* instaladas en las carpetas .claude/ y .agents/ del "
  "repositorio. Las reglas de trabajo de los agentes están en AGENTS.md y CLAUDE.md. Su uso concreto se "
  "documenta en la sección *Auditoría IA*.")
page_break()

# =================================================================== CAPÍTULO 3
H(1, "Capítulo 3. Requisitos y Análisis del Sistema")
H(2, "3.1. Metodología de Desarrollo Adoptada")
P("Se adopta **Scrum** (Schwaber & Sutherland, 2020), adaptado a un equipo de un desarrollador asistido "
  "por IA, y combinado con **desarrollo guiado por especificaciones** mediante OpenSpec. Scrum aporta "
  "el marco de gestión (roles, eventos y artefactos); OpenSpec aporta la disciplina técnica que obliga "
  "a especificar y aprobar el comportamiento antes de que la IA genere código. Ambos son coherentes con "
  "los valores del Manifiesto Ágil (Beck et al., 2001).")
H(3, "3.1.1. Roles")
caption("Tabla", "Roles de Scrum adaptados al proyecto", "roles")
apa_table(["Rol Scrum", "Responsable", "Responsabilidades en StudyPlan"], [
    ["Product Owner", "Estudiante (con el docente como cliente académico)",
     "Decide los requisitos y reglas de negocio, prioriza el Product Backlog y acepta o rechaza los incrementos"],
    ["Scrum Master", "Estudiante", "Vela por el proceso, elimina impedimentos y mantiene la bitácora de IA"],
    ["Developers", "Estudiante + asistente de IA (Claude Code)",
     "El estudiante decide, especifica y revisa; la IA pregunta, propone specs y genera código bajo supervisión"],
], [3.2, 5.0, 8.3])
H(3, "3.1.2. Eventos y artefactos")
bullets([
    "**Sprints de una semana**, con *Sprint Planning* (selección de historias), revisión diaria del "
    "avance, *Sprint Review* (demostración del incremento) y *Retrospectiva*.",
    f"**Product Backlog**: las historias de usuario HU-01 a HU-{len(HISTORIAS):02d} (sección 3.2.1), "
    "priorizadas con MoSCoW y estimadas en puntos de historia (serie de Fibonacci).",
    "**Sprint Backlog**: un *change* de OpenSpec por épica o grupo de historias, cuyo tasks.md contiene "
    "las tareas del sprint.",
    f"**Incremento**: APK funcional que cumple la *Definition of Done*. El primer entregable funcional "
    f"es la versión {VERSION}.",
])
H(3, "3.1.3. Flujo Scrum + SDD")
P("La ⟦F:ciclo⟧ muestra cómo se integra OpenSpec en cada sprint. La regla central es que **ningún código "
  "entra al repositorio sin una especificación aprobada por el humano**. Cada paso queda registrado en "
  "la bitácora de Auditoría IA.")
figure(DIAG / "ciclo_scrum_sdd.png", 15.5, "Ciclo de trabajo de un sprint con Scrum y desarrollo guiado por especificaciones",
       "Los pasos con borde grueso son decisiones humanas y los rellenos en gris, pasos ejecutados por la "
       "IA. Elaboración propia.", key="ciclo")
H(3, "3.1.4. Definition of Done")
P("Una historia de usuario se considera terminada cuando:")
bullets([
    "Todos sus escenarios (criterios de aceptación) del spec de OpenSpec se cumplen y tienen una prueba "
    "asociada, y las reglas de negocio que cita (RN-xx) están probadas con JUnit.",
    "Compila sin errores (./gradlew assembleDebug), las pruebas pasan (./gradlew test) y Android Lint no "
    "reporta errores.",
    "Cumple los criterios de calidad (RNF) asignados a su épica y los transversales (sección 3.2.2).",
    "El código fue revisado por el estudiante (con apoyo del subagente revisor-java) y el estudiante "
    "puede explicarlo.",
    "La sesión de IA quedó registrada en la bitácora y el *change* fue archivado con openspec archive.",
])
H(3, "3.1.5. Coherencia metodológica")
P("Para evitar un error frecuente en proyectos académicos, la mezcla de métodos ágiles con métodos "
  "tradicionales, este proyecto usa **solo artefactos ágiles**. Los requisitos funcionales se expresan "
  "como historias de usuario con criterios de aceptación, **no** como casos de uso ni como "
  "especificación IEEE 830; las reglas de negocio se referencian desde los criterios de aceptación; y "
  "los no funcionales se expresan como criterios de calidad vinculados a la *Definition of Done*. Por "
  "coherencia, se propone al docente que la sección 3.3.1 del índice (*Casos de uso*) se aborde en la "
  "siguiente entrega como **mapa de historias de usuario** (*User Story Mapping*; Patton, 2014), técnica "
  "ágil equivalente para analizar el recorrido del usuario.")

H(2, "3.2. Requisitos")
P("**Técnica de levantamiento.** Los requisitos se levantaron mediante una **entrevista estructurada** con el Product Owner (el "
  "estudiante), conducida por el asistente de IA en cinco rondas de preguntas cerradas con opción de "
  "respuesta libre. Las preguntas partieron del enunciado del docente, el mockup de referencia y la "
  "consigna *Modelado de requerimientos*. Una primera versión de los requisitos, redactada por la IA sin "
  "consultar, fue **rechazada** por el Product Owner (ver *Auditoría IA*). La ⟦T:entrevista⟧ resume las "
  "decisiones obtenidas.")
caption("Tabla", "Decisiones del Product Owner obtenidas en la entrevista de levantamiento", "entrevista")
apa_table(["Tema", "Pregunta", "Decisión del Product Owner", "Requisitos derivados"], [
    ["Arquitectura", "¿Necesita servidor o internet?", "Solo local y offline; copia de seguridad cifrada en Google; el usuario puede borrar su información", "RNF-09, RNF-12, HU-35 a HU-38"],
    ["Usuarios", "¿Quiénes usan la app?", "Es una app de autocontrol: solo el estudiante en la v0.1.0", "RN-01"],
    ["Metas", "¿Meta total o por materia?", "Ambas, independientes entre sí", "HU-25, HU-26, RN-14"],
    ["Alcance", "¿Qué funciones extra incluir?", "Recordatorios, cronómetro, Pomodoro, No molestar, exportar, mensaje motivacional; todo en la v0.1.0", "HU-22 a HU-24, HU-28, HU-33, HU-34"],
    ["Respaldo", "¿Cómo respaldar?", "Como WhatsApp: frecuencia Nunca, Diaria, Semanal, Mensual o Solo manual; contraseña opcional", "HU-35, HU-36, RN-19, RN-20"],
    ["Sesiones", "¿Registrar fechas pasadas?", "Sí, con límite de 3 días", "HU-11, RN-06"],
    ["Sesiones", "¿Se permiten cruces de horario?", "No, se bloquea", "RN-05"],
    ["No molestar", "¿Cómo se activa?", "Configuración global, que puede forzarse en cada sesión", "HU-24, RN-18"],
    ["Borrado", "¿Qué se borra?", "El usuario elige: teléfono, Drive o ambos", "HU-38, RN-21"],
    ["Estados", "¿Qué pasa con una sesión vencida?", "Pasa a No realizada; puede reprogramarse conservando el registro; máquina de estados donde cada estado solo conoce su acción y su siguiente estado; registro tardío como «Completada (registro tardío)»", "RN-07 a RN-10, HU-13, HU-14"],
    ["Pomodoro", "¿Qué se configura?", "Clásico configurable (25/5/15 cada 4)", "HU-23, RN-17"],
    ["Calidad", "¿Qué exigencias de calidad?", "Modo oscuro, bloqueo con huella/PIN, multiidioma, widget", "HU-03 a HU-05, RNF-07, RNF-08"],
    ["Calendario", "¿Qué vistas?", "Página clave según el docente: vistas de día, semana tipo horario y mes, estados, creación desde el calendario, resumen y filtro", "HU-16 a HU-21"],
    ["Fin planificado", "¿Qué pasa si el cronómetro sigue después de la hora de fin?", "Sigue y pregunta «¿Sigues estudiando?»; el resto es tiempo extra", "HU-42, RN-25"],
    ["Duración", "¿Real distinta de la planificada?", "Se registra lo real (39 min o 3 h 28 min), sin estados parciales", "HU-22, RN-26"],
    ["Cruce real", "¿Si el tiempo extra invade otra sesión?", "Avisar y decidir; poder seguir con la otra materia sin tocar el celular", "HU-43, HU-44, RN-27, RN-28"],
    ["Pomodoro extra", "¿Y si quiero seguir tras el plan?", "Ofrecer ciclos extra o tiempo extra libre", "HU-23"],
    ["Eventos", "¿Qué pasa si salgo o cierro la app?", "Registrar eventos con marcas de tiempo, sin un proceso contando; avisar para volver; al volver, el usuario decide hasta qué hora estudió (evento «estudio con la app cerrada»)", "HU-39 a HU-41, RN-22 a RN-24"],
    ["Métricas", "¿Qué medir con los eventos?", "Concentración, puntualidad, respeto del tiempo, tiempo fuera de la app", "HU-45, RN-30"],
], [2.2, 3.4, 6.6, 4.3], size=9)
note("Entrevista realizada el 4 de noviembre de 2026. El registro completo está en "
     "docs/auditoria-ia/bitacora.md.")
P("Además de las respuestas cerradas, el Product Owner narró **escenarios reales de estudio**. Cada "
  "escenario se transformó en historias de usuario y en criterios de aceptación verificables "
  "(⟦T:escenarios⟧). Esta técnica, partir de situaciones concretas del usuario, es la que mejor reveló las "
  "reglas del dominio que la IA no había previsto.")
caption("Tabla", "Escenarios narrados por el Product Owner y requisitos derivados", "escenarios")
apa_table(["Escenario narrado (resumen)", "Historias y criterios derivados", "Reglas"], [
    ["«Una sesión que no estudié no puede desaparecer: hay que dejar registro de que ese día no estudié, aunque la reprograme.»",
     "HU-13 (No realizada automática), HU-14 (reprogramar conservando el registro)", "RN-08, RN-09"],
    ["«Si registro tarde lo que estudié, que diga completada con registro tardío.»",
     "HU-11, HU-13 (escenario Registro tardío)", "RN-06, RN-10"],
    ["«Me inspiré: tenía 1 hora planificada y estudié 39 min, o 3 h 28 min.»",
     "HU-22 (escenario Duración distinta), HU-42 (tiempo extra)", "RN-25, RN-26"],
    ["«Inicié la sesión, me concentré tanto que olvidé el celular y seguí estudiando.»",
     "HU-41 (declarar estudio con la app cerrada), HU-42", "RN-24, RN-29"],
    ["«Me llamaron a las 8:45, salí de la app, cerré la app por completo y volví a las 11:00, a las 17:00 o al día siguiente; estuve estudiando en grupo.»",
     "HU-22 (escenario App cerrada por completo), HU-40 (aviso al salir), HU-41 (escenarios Volver más tarde y Al día siguiente)",
     "RN-22, RN-23, RN-24, RN-29"],
    ["«Sabía que tenía 1 h de Matemáticas y 1 h de Inglés; terminé Matemáticas y seguí con Inglés sin tocar el celular.»",
     "HU-43 (cambiar de sesión), HU-44 (repartir el tramo continuo)", "RN-27, RN-28"],
    ["«Hay que medir qué tan concentrado estoy y qué tanto respeto el tiempo de estudio.»",
     "HU-39 (eventos de estudio), HU-45 (métricas)", "RN-30"],
    ["«El calendario de estudio es una página muy importante, la mencionó el docente.»",
     "HU-16 a HU-21 (vistas de día, semana y mes, estados, creación, filtro y resumen)", "RN-13"],
], [6.2, 6.8, 3.5], size=9)
note("Las frases son paráfrasis fieles de los mensajes del Product Owner durante la sesión del 4 de "
     "noviembre de 2026.")
P("El rol de usuario es único: el **estudiante**, persona que cursa materias y desea planificar y "
  "controlar sus horas de estudio. Los requisitos se organizan en diez épicas (⟦T:epicas⟧) y se "
  "mantienen en una fuente única (docs/build/requisitos.py) desde la cual se generan las "
  "especificaciones de OpenSpec y las tablas de este documento. Así el documento y el contrato que usa "
  "la IA no pueden contradecirse.")
caption("Tabla", "Épicas del producto y su especificación en OpenSpec", "epicas")
apa_table(["Épica", "Nombre", "Historias", "Especificación (OpenSpec)"],
          [[eid, n, ", ".join(h["id"] for h in HISTORIAS if h["epica"] == eid), f"specs/{cap}/spec.md"]
           for eid, (n, cap, _) in EPICAS.items()],
          [1.5, 4.3, 5.2, 5.5])
note("Las rutas son relativas a openspec/changes/add-requisitos-base/.")

H(3, "3.2.1. Requisitos Funcionales")
P("Los requisitos funcionales se expresan como historias de usuario. La ⟦T:backlog⟧ presenta el *Product "
  "Backlog* completo y las tablas siguientes detallan cada historia con sus criterios de aceptación; "
  "cada criterio corresponde a un *Scenario* verificable del spec de OpenSpec. Al final de esta sección "
  "se presentan las reglas de negocio (⟦T:reglas⟧) y la máquina de estados de la sesión de estudio "
  "(⟦F:estados⟧ y ⟦T:transiciones⟧), que los criterios de aceptación referencian como RN-xx.")
caption("Tabla", f"Product Backlog de StudyPlan v{VERSION}", "backlog")
apa_table(["ID", "Épica", "Historia de usuario", "Prioridad", "Pts."],
          [[h["id"], h["epica"], h["titulo"], h["prioridad"], h["puntos"]] for h in HISTORIAS],
          [1.6, 1.4, 9.5, 2.2, 1.8])
tot = sum(h["puntos"] for h in HISTORIAS)
must = sum(h["puntos"] for h in HISTORIAS if h["prioridad"] == "Must")
note(f"Prioridad MoSCoW: Must = imprescindible; Should = importante; Could = deseable. Estimación en "
     f"puntos de historia (Fibonacci). Total: {len(HISTORIAS)} historias, {tot} puntos ({must} puntos Must). "
     f"Por decisión del Product Owner, todas forman parte de la versión {VERSION}; la prioridad ordena "
     "los sprints.")

for eid, (nombre, cap, _) in EPICAS.items():
    hus = [h for h in HISTORIAS if h["epica"] == eid]
    caption("Tabla", f"Historias de usuario de la épica {eid}: {nombre}")
    rows = []
    for h in hus:
        crit = "\n".join(f"{i}. **{n}.** *Dado* {d}, *cuando* {c}, *entonces* {e}."
                         for i, (n, d, c, e) in enumerate(h["ca"], 1))
        rows += [
            [h["id"], f"**{h['titulo']}**"],
            ["Historia", f"Como {ROL}, quiero {h['quiero']} para {h['para']}."],
            ["Prioridad / Est.", f"{h['prioridad']} · {h['puntos']} puntos"],
            ["Criterios de aceptación", crit],
        ]
    calidad = ", ".join(CALIDAD_POR_EPICA[eid] + RNF_TRANSVERSALES)
    rows.append(["Criterios de calidad", f"{calidad} (ver ⟦T:rnf⟧)"])
    rows.append(["Spec OpenSpec", f"openspec/changes/add-requisitos-base/specs/{cap}/spec.md"])
    t = grid_table(rows, [3.4, 13.1])
    for i in range(0, len(rows) - 2, 4):
        for c in t.rows[i].cells:
            shade(c, "D0CECE")
    doc.add_paragraph()

H(4, "3.2.1.1. Reglas de negocio y máquina de estados de la sesión")
P("Las reglas de negocio expresan restricciones del dominio que deben cumplirse en toda la aplicación, "
  "independientemente de la pantalla. La columna *Origen* indica si la regla la dictó el Product Owner "
  "en la entrevista, si la propuso la IA y fue aceptada, o si se derivó de otra regla y queda pendiente "
  "de confirmación.")
caption("Tabla", "Reglas de negocio del dominio", "reglas")
apa_table(["ID", "Regla", "Origen"], [[a, b, c] for a, b, c in REGLAS], [1.6, 11.4, 3.5], size=9)
P("La regla RN-07 establece que el ciclo de vida de una sesión de estudio es una **máquina de estados** "
  "(⟦F:estados⟧). Siguiendo el patrón State, cada estado conoce únicamente las acciones que admite y "
  "su estado siguiente. Reprogramar no borra la sesión original: la marca como *Reprogramada* y crea "
  "una sesión nueva enlazada, de modo que queda constancia del día en que no se estudió.")
figure(DIAG / "maquina_estados.png", 16.0, "Máquina de estados de la sesión de estudio",
       "Los estados en gris son finales. Las condiciones van entre corchetes. Elaboración propia a partir "
       "de la entrevista con el Product Owner.", key="estados")
caption("Tabla", "Transiciones permitidas de la sesión de estudio", "transiciones")
apa_table(["Estado origen", "Evento", "Estado destino", "Condición / efecto"],
          [[ESTADOS_NOMBRE.get(o, "Sesión nueva"), e, ESTADOS_NOMBRE[d], c] for o, e, d, c in TRANSICIONES],
          [3.4, 3.6, 3.8, 5.7], size=9)
note("Cualquier evento no listado es rechazado. Completada, Completada (registro tardío) y Reprogramada "
     "son estados finales. Solo Completada y Completada (registro tardío) suman horas reales (RN-11).")

H(3, "3.2.2. Requisitos No Funcionales")
P("Los requisitos no funcionales se definen como **criterios de calidad** según las características del "
  "modelo ISO/IEC 25010:2023 (ISO, 2023). Cada uno tiene una métrica medible y un método de "
  "verificación, para comprobarlo objetivamente al cerrar cada sprint como parte de la *Definition of "
  "Done*. La ⟦T:cobertura⟧ resume la cobertura por característica y la ⟦T:rnf⟧ detalla cada requisito.")
caption("Tabla", "Cobertura de las características de calidad ISO/IEC 25010:2023", "cobertura")
carac = {}
for r in RNF:
    carac.setdefault(r[1], []).append(r[0])
justif = {
    "Adecuación funcional": "Cubierta por los criterios de aceptación de cada HU (sección 3.2.1)",
    "Protección (safety)": "No aplica: la aplicación no controla procesos con riesgo físico",
}
rows = [[k, ", ".join(v)] for k, v in carac.items()] + [[k, v] for k, v in justif.items()]
apa_table(["Característica", "Requisitos"], rows, [6.0, 10.5])
note("Las características capacidad de interacción y flexibilidad reemplazan en la edición de 2023 a "
     "las de usabilidad y portabilidad de la edición de 2011.")
caption("Tabla", "Requisitos no funcionales (criterios de calidad)", "rnf")
apa_table(["ID", "Característica / subcaracterística", "Requisito", "Métrica / criterio", "Verificación"],
          [[r[0], f"{r[1]} / {r[2]}", r[3], r[4], r[5]] for r in RNF],
          [1.6, 3.3, 3.2, 5.2, 3.2], size=9)
note("SUS = System Usability Scale (Brooke, 1996). Los valores objetivo son metas del proyecto y se "
     "verificarán en el Capítulo 5 (Pruebas y Validación).")
P("Como adelanto de la consigna *Modelado de requerimientos*, el Anexo A presenta el modelo de dominio "
  "conceptual y el prototipo de baja fidelidad asociado a las historias de usuario. El análisis "
  "completo (sección 3.3) corresponde a la siguiente entrega.")
page_break()

# =================================================================== AUDITORÍA IA
H(1, "Auditoría IA")
P("Esta sección responde a la indicación del docente de que todo uso de inteligencia artificial (IA) "
  "en el proyecto debe quedar documentado. Por eso se agrega al índice como un punto especial y forma "
  "parte de este documento, con el mismo peso que los capítulos del proyecto. Aquí se declara con qué "
  "herramientas se trabajó (A.1), qué límites se fijaron (A.2), cómo se controló el flujo de trabajo "
  "(A.3), **qué hizo la IA**, **qué decidió el estudiante** y **cómo se verificó cada resultado** (A.4), "
  "qué riesgos se identificaron (A.5) y qué aprendizaje deja la experiencia (A.6). Su propósito es "
  "doble: que el estudiante pueda defender cada artefacto generado y que el docente pueda auditar el "
  "proceso.")
H(2, "A.1. Declaración de uso de IA")
caption("Tabla", "Herramientas de IA utilizadas", "herramientas")
apa_table(["Herramienta", "Versión / modelo", "Uso", "Periodo"], [
    ["Claude Code (Anthropic)", "Modelo Claude Opus 5.5", "Asistente agéntico: entrevista de requisitos, redacción, specs, scripts y documentación", "Desde el 04/11/2026"],
    ["OpenSpec (Fission-AI)", "CLI 1.14.3, esquema spec-driven", "Estructura y validación de especificaciones (SDD)", "Desde el 04/11/2026"],
], [3.8, 3.8, 6.4, 2.5])
note("Todo contenido generado por IA fue revisado por el estudiante antes de la entrega. Las referencias "
     "bibliográficas propuestas por la IA deben verificarse una por una (DOI o URL) antes de la entrega final.")

H(2, "A.2. Protocolo de trabajo humano–IA")
P("El protocolo define límites claros entre lo que decide el humano y lo que ejecuta la IA. Está escrito "
  "en AGENTS.md (reglas para cualquier agente) y CLAUDE.md (reglas de Claude Code) en la raíz del "
  "repositorio, y se refuerza en openspec/config.yaml, que inyecta el contexto del proyecto en cada "
  "instrucción que OpenSpec entrega a la IA. A raíz del incidente descrito en A.4, se agregó la regla "
  "explícita: **la IA no inventa requisitos; si falta información, pregunta**.")
caption("Tabla", "Reparto de responsabilidades entre el estudiante y la IA", "reparto")
apa_table(["Decide / hace el estudiante", "Propone / ejecuta la IA"], [
    ["Definir requisitos, reglas de negocio y criterios de calidad", "Conducir la entrevista y redactar a partir de las respuestas"],
    ["Elegir stack (Java nativo), package name y metodología", "Presentar alternativas con ventajas y desventajas"],
    ["Aprobar o rechazar cada change de OpenSpec", "Redactar proposal, specs, design y tasks"],
    ["Revisar, aceptar y defender el código generado", "Generar código Java y pruebas a partir de specs aprobadas"],
    ["Verificar referencias, versiones y métricas", "Ejecutar validaciones y registrar cada sesión en la bitácora"],
], [8.25, 8.25])
P("Reglas no negociables para la IA: no cambiar el lenguaje (Java), el applicationId ni el minSdk; no "
  "agregar dependencias sin aprobación; no inventar requisitos; no hacer commits sin pedido explícito; "
  "y no implementar nada que no tenga un *change* aprobado.")

H(2, "A.3. Sistema OpenSpec en el proyecto")
P("OpenSpec se inicializó con el comando openspec init --tools claude,agents --language es. El comando "
  "creó la carpeta openspec/ (specs/ para las especificaciones vigentes y changes/ para los cambios en "
  "curso), el archivo openspec/config.yaml y las *skills* y comandos para Claude Code (.claude/) y para "
  "otros agentes (.agents/). La ⟦T:estructura⟧ describe la estructura del repositorio.")
caption("Tabla", "Estructura del repositorio StudyPlan", "estructura")
apa_table(["Ruta", "Contenido"], [
    ["app/", "Módulo Android en Java (io.github.jhoel0521.studyplan), creado por el estudiante con Android Studio"],
    ["openspec/config.yaml", "Contexto y reglas del proyecto que OpenSpec entrega a la IA en cada artefacto"],
    ["openspec/changes/add-requisitos-base/", f"Change de esta entrega: proposal.md, design.md, tasks.md y {len(EPICAS)} specs"],
    ["openspec/specs/", "Especificaciones vigentes (se llenan al archivar cada change)"],
    ["AGENTS.md · CLAUDE.md", "Reglas para agentes de IA (comunes y propias de Claude Code)"],
    [".claude/agents/", "Subagentes: analista-requisitos, revisor-java, auditor-ia"],
    [".claude/commands/opsx/ · .claude/skills/", "Comandos /opsx:propose, apply, archive, explore, sync, update"],
    [".agents/skills/", "Las mismas skills de OpenSpec para otros agentes compatibles"],
    ["docs/build/", "requisitos.py (fuente única), gen_specs.py, diagramas.py, generar_docx.py, actualizar_word.ps1"],
    ["docs/diagramas/", "Modelo de dominio, máquina de estados, ciclo Scrum+SDD y wireframes"],
    ["docs/auditoria-ia/bitacora.md", "Bitácora detallada de cada sesión con IA"],
    ["docs/entrega1/", "Este documento"],
], [6.0, 10.5])
P("El ciclo de vida de un requisito en OpenSpec es: (a) /opsx:propose crea un *change* con la propuesta "
  "(por qué), las especificaciones delta (qué), el diseño (cómo) y las tareas; (b) el humano revisa y "
  "aprueba; (c) /opsx:apply implementa las tareas y las marca como completadas; (d) openspec validate "
  "--strict verifica el formato; y (e) /opsx:archive fusiona las especificaciones en openspec/specs/, que "
  "pasan a ser la fuente de verdad del sistema.")
P("Cada historia de usuario se transforma en un *Requirement* y cada criterio de aceptación en un "
  "*Scenario*. La máquina de estados (RN-07) también se especifica como *Requirement*, con un escenario "
  "por transición. Ejemplo de specs/sesiones-estudio/spec.md:")
ej = [
    "### Requirement: HU-10 Planificar sesión de estudio",
    "Como estudiante, quiero registrar una sesión con materia, fecha, ... El sistema SHALL",
    "cumplir los escenarios siguientes (prioridad Must, 5 pts).",
    "#### Scenario: Cruce bloqueado",
    "- **GIVEN** que existe una sesión de 10:00 a 11:30 ese día",
    "- **WHEN** el estudiante registra otra de 11:00 a 12:00 el mismo día",
    "- **THEN** el sistema bloquea el registro e indica con qué sesión se cruza (RN-05)",
]
for line in ej:
    p = doc.add_paragraph(style="Texto tabla")
    r = p.add_run(line)
    r.font.name = "Consolas"
    r.font.size = Pt(9)
    p.paragraph_format.left_indent = Cm(1.27)
doc.add_paragraph()
P("Evidencia de validación: openspec validate add-requisitos-base --strict respondió «Change "
  "'add-requisitos-base' is valid» tanto en la primera versión de los requisitos como en la versión "
  "corregida tras la entrevista.")

H(2, "A.4. Bitácora de decisiones")
P("La ⟦T:bitacora⟧ resume las decisiones de la primera sesión de trabajo. La más relevante es la número 8: "
  "la IA redactó 19 historias, reglas de dominio y requisitos no funcionales **sin preguntar** al "
  "Product Owner, y además asumió por su cuenta que la aplicación no usaría internet. El estudiante lo "
  "detectó y lo rechazó, y los requisitos se rehicieron mediante entrevista. La bitácora completa está "
  "en docs/auditoria-ia/bitacora.md.")
caption("Tabla", "Resumen de la bitácora de la sesión del 4 de noviembre de 2026", "bitacora")
apa_table(["#", "Solicitud / propuesta", "Resultado de la IA", "Decisión humana y justificación", "Verificación"], [
    ["1", "Planificar la primera entrega a partir del índice, la consigna y el mockup",
     "Plan de trabajo y preguntas sobre formato, stack y metodología",
     "**Modificado.** El estudiante eligió Word APA, Scrum + SDD e incluir las HU", "Respuestas del estudiante"],
    ["2", "Elegir el stack tecnológico", "Ofreció React Native, Flutter, Tauri o Laravel",
     "**Rechazado.** El docente pidió una app nativa; se eligió Java porque se enseña en clase", "Respuesta del estudiante"],
    ["3", "Incluir HU y casos de uso", "Recomendó HU con criterios de aceptación",
     "**Aceptado con restricción.** Por advertencia del docente, no se mezclan HU con casos de uso", "Respuesta del estudiante"],
    ["4", "Estructura del proyecto", "Propuso una carpeta de documentación separada",
     "**Modificado.** La raíz es el proyecto Android Studio, con docs/, openspec/, AGENTS.md y CLAUDE.md dentro", "Proyecto creado por el estudiante"],
    ["5", "Package name para Google Play", "Explicó que es permanente y propuso io.github.jhoel0521.studyplan",
     "**Aceptado.** Basado en la cuenta de GitHub del estudiante, que creó el proyecto en Android Studio", "applicationId en app/build.gradle.kts"],
    ["6", "Inicializar OpenSpec", "Ejecutó openspec init; la versión 1.14.3 usa config.yaml en lugar de project.md",
     "**Aceptado.** El contexto del proyecto se escribió en config.yaml", "openspec status"],
    ["7", "Generar diagramas", "Intentó descargar PlantUML; la política de permisos lo bloqueó",
     "**Aceptado.** Diagramas generados con Pillow, reproducibles con diagramas.py", "Revisión visual de los PNG"],
    ["8", "Redactar requisitos (v1)", "19 HU, reglas y 16 RNF redactados sin consultar; supuso «sin internet»",
     "**Rechazado.** «No me preguntaste reglas de dominio, requisitos funcionales y no funcionales». Se exigió entrevista", "Revisión del estudiante"],
    ["9", "Entrevista de levantamiento", "5 rondas de preguntas (⟦T:entrevista⟧)",
     "**Decidido por el humano.** Offline-first con backup tipo WhatsApp, máquina de estados, metas total y por materia, etc.", "Registro en la bitácora"],
    ["10", "Calendario de estudio", "Vista mensual básica (HU-10 v1)",
     "**Modificado.** El docente lo destacó: vistas de día, semana y mes, estados, creación, filtro (HU-16 a HU-21)", "Revisión de los wireframes W4–W6"],
    ["11", "Escenarios de estudio real", "La v2 solo decía «Detener guarda la duración real»",
     "**Modificado.** El PO narró escenarios (inspiración, llamada, app cerrada, estudio en grupo, cambio de materia) que originaron HU-39 a HU-45 y RN-22 a RN-30", "openspec validate --strict"],
    ["12", "Diseño del cronómetro", "Preguntó si contar tiempo con un proceso en segundo plano",
     "**Decidido por el humano.** «Registrar timestamps, mostrar el cronómetro solo en la app»; coincide con RNF-03 (batería)", "Contrastado con RNF-03"],
    ["13", "Actualizar índice del Word", "El script de automatización de Word (COM) se colgó dos veces",
     "**Pendiente.** El estudiante actualiza los campos en Word (Ctrl+A, F9)", "Campos actualizados a mano"],
    ["14", "Requisitos v3 y documento", f"{len(HISTORIAS)} HU, {len(REGLAS)} RN, {len(RNF)} RNF, {len(EPICAS)} specs validadas",
     "**Pendiente** de revisión humana (tareas 2.1 a 2.3 de tasks.md)", "openspec validate --strict"],
], [0.7, 3.4, 4.4, 5.2, 2.8], size=8)

H(2, "A.5. Riesgos del uso de IA y mitigación")
caption("Tabla", "Riesgos identificados en el uso de IA y su mitigación", "riesgos")
apa_table(["Riesgo", "Ejemplo", "Mitigación"], [
    ["Requisitos inventados", "Ocurrió en esta entrega: la IA redactó HU, reglas y RNF sin preguntar",
     "Entrevista obligatoria; regla en AGENTS.md; columna «Origen» en las reglas de negocio"],
    ["Supuestos no declarados", "Asumir «sin internet» cuando el usuario quería respaldo en la nube",
     "Toda suposición se marca como «Derivada (confirmar)» y se pregunta"],
    ["Alucinación de referencias o datos", "Citas, DOI o versiones inexistentes",
     "Verificación humana de cada referencia; solo fuentes con DOI o URL oficial"],
    ["Desvío del stack", "Generar Kotlin o Jetpack Compose por costumbre",
     "Restricción en config.yaml y AGENTS.md; revisión con revisor-java"],
    ["Código inseguro o de baja calidad", "SQL concatenado, consultas en el hilo principal, cifrado débil",
     "RNF-12, RNF-14 y RNF-15, Lint, StrictMode y revisión humana"],
    ["Pérdida de autoría y comprensión", "Entregar código que el estudiante no entiende",
     "La Definition of Done exige que el estudiante explique el código; defensa oral"],
], [4.0, 5.5, 7.0])

H(2, "A.6. Reflexión: ¿qué diferencia a un programador en la era de la IA?")
P("El docente plantea que, en la era de la IA y los agentes, cualquiera puede pedir código a una "
  "máquina, del mismo modo que un médico podría pedírselo. Esta primera entrega ofrece un ejemplo "
  "concreto de la respuesta. La IA produjo en minutos un conjunto de requisitos bien redactado, con "
  "formato correcto y validado por OpenSpec, pero **no era el sistema que el usuario quería**. Suponía "
  "una aplicación sin internet, sin respaldo, sin máquina de estados, sin Pomodoro y con un calendario "
  "mínimo. Detectar que faltaba preguntar, exigir la entrevista y tomar decisiones de dominio fue "
  "trabajo humano: cómo tratar una sesión no realizada, cómo conservar el registro o cuándo es válido "
  "un registro tardío. Lo mismo ocurrió con los escenarios de estudio real (inspirarse, olvidar el "
  "celular, estudiar en grupo): la IA no los había previsto, y fueron el origen de siete historias "
  "nuevas.")
P("El valor del programador está en **especificar** qué debe hacer el sistema con criterios "
  "verificables, **validar** que lo generado cumple ese contrato y **defender** cada decisión técnica. "
  "Un médico con IA puede obtener código que compila, pero no sabría juzgar si responde al problema "
  "correcto. El desarrollo guiado por especificaciones convierte ese criterio profesional en un proceso "
  "explícito, auditable y repetible.")
page_break()

# =================================================================== REFERENCIAS
H(1, "Referencias")
REFS = [
    "Android Developers. (s.f.-a). *Guía de arquitectura de apps*. Google. https://developer.android.com/topic/architecture",
    "Android Developers. (s.f.-b). *Cómo guardar contenido en una base de datos local con Room*. Google. https://developer.android.com/training/data-storage/room",
    "Android Developers. (s.f.-c). *Trabajo persistente con WorkManager*. Google. https://developer.android.com/develop/background-work/background-tasks/persistent",
    "Android Developers. (s.f.-d). *Kotlin y Android*. Google. https://developer.android.com/kotlin",
    "Android Developers. (s.f.-e). *Compatibilidad con API de lenguaje Java 8 y versiones posteriores*. Google. https://developer.android.com/studio/write/java8-support",
    "Android Developers. (s.f.-f). *Cómo mostrar un diálogo de autenticación biométrica*. Google. https://developer.android.com/training/sign-in/biometric-auth",
    "Anthropic. (2026). *Claude Code* [Software]. https://claude.com/claude-code",
    "Beck, K., Beedle, M., van Bennekum, A., Cockburn, A., Cunningham, W., Fowler, M., Grenning, J., Highsmith, J., Hunt, A., Jeffries, R., Kern, J., Marick, B., Martin, R. C., Mellor, S., Schwaber, K., Sutherland, J., & Thomas, D. (2001). *Manifiesto por el desarrollo ágil de software*. https://agilemanifesto.org/iso/es/manifesto.html",
    "Britton, B. K., & Tesser, A. (1991). Effects of time-management practices on college grades. *Journal of Educational Psychology, 83*(3), 405–410. https://doi.org/10.1037/0022-0663.83.3.405",
    "Brooke, J. (1996). SUS: A “quick and dirty” usability scale. En P. W. Jordan, B. Thomas, B. A. Weerdmeester, & I. L. McClelland (Eds.), *Usability evaluation in industry* (pp. 189–194). Taylor & Francis.",
    "Cirillo, F. (2018). *The Pomodoro Technique: The life-changing time-management system*. Currency.",
    "Clegg, D., & Barker, R. (1994). *Case method fast-track: A RAD approach*. Addison-Wesley.",
    "Cohn, M. (2004). *User stories applied: For agile software development*. Addison-Wesley.",
    "Fission-AI. (2026). *OpenSpec* (Versión 1.14.3) [Software]. GitHub. https://github.com/Fission-AI/OpenSpec",
    "Gamma, E., Helm, R., Johnson, R., & Vlissides, J. (1994). *Design patterns: Elements of reusable object-oriented software*. Addison-Wesley.",
    "Google. (s.f.). *Material Design 3*. https://m3.material.io/",
    "Google for Developers. (s.f.). *Store application-specific data* [Google Drive API]. https://developers.google.com/drive/api/guides/appdata",
    "International Organization for Standardization. (2023). *Systems and software engineering — Systems and software Quality Requirements and Evaluation (SQuaRE) — Product quality model* (ISO/IEC Norma n.º 25010:2023).",
    "Jahoda, P. (2019). *MPAndroidChart* (Versión 3.1.0) [Software]. GitHub. https://github.com/PhilJay/MPAndroidChart",
    "Locke, E. A., & Latham, G. P. (2002). Building a practically useful theory of goal setting and task motivation: A 35-year odyssey. *American Psychologist, 57*(9), 705–717. https://doi.org/10.1037/0003-066X.57.9.705",
    "Macan, T. H., Shahani, C., Dipboye, R. L., & Phillips, A. P. (1990). College students' time management: Correlations with academic performance and stress. *Journal of Educational Psychology, 82*(4), 760–768. https://doi.org/10.1037/0022-0663.82.4.760",
    "North, D. (2006). Introducing BDD. *Better Software*. https://dannorth.net/introducing-bdd/",
    "Patton, J. (2014). *User story mapping: Discover the whole story, build the right product*. O'Reilly Media.",
    "Schwaber, K., & Sutherland, J. (2020). *La Guía Scrum: La guía definitiva de Scrum: Las reglas del juego*. https://scrumguides.org/",
    "Steel, P. (2007). The nature of procrastination: A meta-analytic and theoretical review of quintessential self-regulatory failure. *Psychological Bulletin, 133*(1), 65–94. https://doi.org/10.1037/0033-2909.133.1.65",
    "Turan, M. S., Barker, E., Burr, W., & Chen, L. (2010). *Recommendation for password-based key derivation: Part 1: Storage applications* (NIST Special Publication 800-132). National Institute of Standards and Technology. https://doi.org/10.6028/NIST.SP.800-132",
    "Wake, B. (2003). *INVEST in good stories, and SMART tasks*. XP123. https://xp123.com/articles/invest-in-good-stories-and-smart-tasks/",
    "Zimmerman, B. J. (2002). Becoming a self-regulated learner: An overview. *Theory Into Practice, 41*(2), 64–70. https://doi.org/10.1207/s15430421tip4102_2",
]
for ref in REFS:
    p = runs(doc.add_paragraph(), ref)
    p.paragraph_format.left_indent = Cm(1.27)
    p.paragraph_format.first_line_indent = Cm(-1.27)
page_break()

# =================================================================== ANEXOS
H(1, "Anexos")
H(2, "Anexo A: Modelo de Dominio y Prototipo de Baja Fidelidad")
P("Este anexo adelanta los productos de la consigna *Modelado de requerimientos*: el modelo de dominio "
  "(diagrama de clases conceptual) y el prototipo de baja fidelidad. Su análisis detallado corresponde a "
  "la sección 3.3.3 de la siguiente entrega.")
figure(DIAG / "modelo_dominio.png", 16.5, "Modelo de dominio conceptual de StudyPlan",
       "Reporte y DistribucionMateria son conceptos calculados a partir de las sesiones y no se almacenan. "
       "Una MetaSemanal sin materia es la meta total. Elaboración propia.", key="dominio")
P("Las clases principales son: **Estudiante**, el único usuario local, con sus **Ajustes** (tema, "
  "idioma, bloqueo, No molestar y Pomodoro) y su **ConfiguracionRespaldo**; **Materia**, que agrupa por "
  "composición las **SesionEstudio**, cada una con fecha, horario, técnica, un **EstadoSesion** "
  "gobernado por la máquina de estados (⟦F:estados⟧) y un **Recordatorio** opcional. La "
  "autoasociación *reprograma en* enlaza una sesión reprogramada con la nueva. **MetaSemanal** "
  "representa la meta total (sin materia) o una meta por materia, y su progreso se calcula sumando las "
  "horas de las sesiones completadas. **Reporte** contiene una **DistribucionMateria** por cada materia "
  "del periodo.")
figure(DIAG / "wireframes_1.png", 16.0, "Prototipo de baja fidelidad: Inicio, Materias y Registrar sesión",
       "Debajo de cada pantalla se indican las historias de usuario que representa. Elaboración propia a "
       "partir del mockup de referencia del proyecto.")
figure(DIAG / "wireframes_2.png", 16.0, "Prototipo de baja fidelidad del calendario de estudio: vistas de mes, semana y día",
       "Íconos de estado: ✓ completada, ○ pendiente, ✕ no realizada, ↻ reprogramada, ◷ registro tardío. "
       "Elaboración propia.")
figure(DIAG / "wireframes_3.png", 16.0, "Prototipo de baja fidelidad: Sesión en curso (Pomodoro), Estadísticas y Reporte",
       "Elaboración propia.")
figure(DIAG / "wireframes_4.png", 16.0, "Prototipo de baja fidelidad: Ajustes, Copia de seguridad y Borrar mis datos",
       "Elaboración propia.")
figure(DIAG / "wireframes_5.png", 16.0, "Prototipo de baja fidelidad: Al volver a la app, Detener y repartir, Detalle y eventos",
       "W13 implementa la declaración de estudio con la app cerrada; W15 muestra la línea de tiempo de "
       "eventos y las métricas de concentración. Elaboración propia.")

H(2, "Anexo C: Matriz de Trazabilidad")
caption("Tabla", "Trazabilidad entre historias de usuario, especificaciones, prototipo y criterios de calidad", "traza")
wf = {"HU-01": "W1", "HU-02": "W1", "HU-27": "W1, W8", "HU-03": "W10", "HU-04": "W10", "HU-05": "—",
      "HU-06": "W2", "HU-07": "W2", "HU-08": "W2", "HU-09": "W2", "HU-10": "W3", "HU-11": "W3",
      "HU-12": "W4–W6", "HU-13": "W4–W6", "HU-14": "W4–W6", "HU-15": "W4–W6", "HU-16": "W4",
      "HU-17": "W5", "HU-18": "W6", "HU-19": "W4–W6", "HU-20": "W5, W6", "HU-21": "W4–W6",
      "HU-22": "W7", "HU-23": "W7, W10", "HU-24": "W7, W10", "HU-25": "W1", "HU-26": "W2",
      "HU-28": "—", "HU-29": "W8", "HU-30": "W8", "HU-31": "W8", "HU-32": "W9", "HU-33": "W9",
      "HU-34": "W3, W10", "HU-35": "W11", "HU-36": "W11", "HU-37": "W11", "HU-38": "W12",
      "HU-39": "W15", "HU-40": "W13", "HU-41": "W13", "HU-42": "W14", "HU-43": "W14", "HU-44": "W14",
      "HU-45": "W15"}
rn_re = re.compile(r"RN-\d+")
apa_table(["HU", "Spec (capacidad)", "Esc.", "Wireframe", "Reglas", "RNF de la épica"],
          [[h["id"], EPICAS[h["epica"]][1], len(h["ca"]), wf.get(h["id"], "—"),
            ", ".join(sorted(set(rn_re.findall(" ".join(" ".join(c) for c in h["ca"]))))) or "—",
            ", ".join(CALIDAD_POR_EPICA[h["epica"]])] for h in HISTORIAS],
          [1.5, 3.4, 1.1, 2.2, 3.0, 5.3], size=9)
note(f"Total: {len(HISTORIAS)} historias y {sum(len(h['ca']) for h in HISTORIAS)} escenarios, más "
     f"{len(TRANSICIONES) + 1} escenarios de la máquina de estados (RN-07). Las columnas de código y "
     "prueba se agregarán en cada sprint de implementación.")

H(2, "Anexo E: Glosario de Términos")
GLOS = [
    ("Change (OpenSpec)", "Propuesta de cambio con proposal, specs delta, design y tasks, que se aprueba antes de implementarse."),
    ("Criterio de aceptación", "Condición verificable, escrita como Dado / Cuando / Entonces, que una historia debe cumplir para considerarse terminada."),
    ("DAO", "Data Access Object: interfaz de Room que define las consultas a la base de datos."),
    ("Definition of Done", "Lista acordada de condiciones que todo incremento debe cumplir para considerarse terminado."),
    ("Épica", "Agrupación de historias de usuario relacionadas con un mismo objetivo funcional."),
    ("Estudio con la app cerrada", "Tramo de estudio que el estudiante declara al volver a la app, después de haber salido de ella con una sesión en curso."),
    ("Evento de estudio", "Registro con marca de tiempo de algo que ocurre en una sesión: inicio, pausa, salida o regreso a la app, fin planificado, tiempo extra, cambio de sesión o fin."),
    ("Historia de usuario", "Descripción breve de una funcionalidad desde la perspectiva del usuario: «Como… quiero… para…»."),
    ("Máquina de estados", "Modelo que define los estados de un objeto y los eventos que provocan transiciones entre ellos."),
    ("MoSCoW", "Técnica de priorización: Must (debe), Should (debería), Could (podría), Won't (no se hará ahora)."),
    ("MVVM", "Modelo–Vista–Modelo de Vista: patrón que separa la interfaz de la lógica de presentación y de los datos."),
    ("Offline-first", "Diseño en el que la aplicación funciona por completo sin conexión y solo usa la red para funciones opcionales."),
    ("Patrón State", "Patrón de diseño en el que cada estado es una clase que conoce sus acciones y su estado siguiente."),
    ("Pomodoro", "Técnica de estudio en bloques de concentración (25 min) separados por descansos cortos y largos."),
    ("Registro tardío", "Registro de una sesión ya estudiada, hasta 3 días después de su fecha, en estado «Completada (registro tardío)»."),
    ("Regla de negocio", "Restricción del dominio que el sistema debe cumplir en todas sus funciones (RN-xx)."),
    ("Room", "Biblioteca de Android Jetpack que abstrae SQLite y valida las consultas en compilación."),
    ("SDD", "Spec-Driven Development: desarrollo en el que la especificación aprobada precede y guía al código."),
    ("Tiempo extra", "Tiempo estudiado después de la hora de fin planificada de una sesión."),
    ("Spec", "Especificación de comportamiento de una capacidad del sistema, con Requirements y Scenarios."),
    ("Sprint", "Iteración de duración fija (una semana en este proyecto) que produce un incremento usable."),
    ("Wireframe", "Prototipo de baja fidelidad que muestra la estructura de una pantalla sin diseño visual final."),
]
apa_table(["Término", "Definición"], [[a, b] for a, b in GLOS], [4.0, 12.5])

# ------------------------------------------------------------------ referencias cruzadas
UNRESOLVED = []


def resolve(par):
    for r in par.runs:
        if "⟦" in r.text:
            def rep(m):
                kind, key = m.group(1), m.group(2)
                n = keys.get((kind, key))
                if n is None:
                    UNRESOLVED.append(key)
                    return m.group(0)
                return f"{'Tabla' if kind == 'T' else 'Figura'} {n}"
            txt = re.sub(r"⟦([TF]):(\w+)⟧", rep, r.text)
            r.text = txt


for par in doc.paragraphs:
    resolve(par)
for tb in doc.tables:
    for row in tb.rows:
        for cell in row.cells:
            for par in cell.paragraphs:
                resolve(par)
assert not UNRESOLVED, UNRESOLVED

OUT.parent.mkdir(parents=True, exist_ok=True)
doc.core_properties.title = f"StudyPlan v{VERSION} — Primera entrega"
doc.core_properties.language = LANG
doc.save(OUT)
print(f"OK {OUT.relative_to(ROOT)}  tablas={counters['Tabla']} figuras={counters['Figura']}")
