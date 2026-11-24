"""Genera la presentación de la defensa de StudyPlan (PowerPoint) a partir de los recursos del proyecto.

Uso: python docs/presentacion/generar_presentacion.py

Los datos de la portada (autor, universidad, materia, docente) se leen de docs/documento/.env, que no se versiona,
por eso el .pptx generado tampoco se versiona (ver .gitignore). Las cifras salen del trabajo verificado en el
repositorio: 456 pruebas unitarias, 327 clases cotejadas con los diagramas, 11 declaraciones de Play Console, etc.
"""
import subprocess
import sys
import tempfile
from pathlib import Path

from pptx import Presentation
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE
from pptx.enum.text import MSO_ANCHOR, PP_ALIGN
from pptx.util import Inches, Pt
from PIL import Image

RAIZ = Path(__file__).resolve().parents[2]
DOC = RAIZ / "docs" / "documento"
REC = DOC / "recursos"
SALIDA = RAIZ / "docs" / "presentacion" / "StudyPlan_Defensa.pptx"

INDIGO = RGBColor(0x43, 0x38, 0xC9)
INDIGO_OSC = RGBColor(0x2B, 0x23, 0x8F)
INDIGO_CLARO = RGBColor(0xDD, 0xDA, 0xFF)
VERDE = RGBColor(0x1B, 0x7F, 0x5C)
GRIS = RGBColor(0x3F, 0x44, 0x52)
GRIS_CLARO = RGBColor(0x80, 0x85, 0x94)
FONDO = RGBColor(0xF3, 0xF4, 0xFA)
BORDE = RGBColor(0xD9, 0xDB, 0xEE)
BLANCO = RGBColor(0xFF, 0xFF, 0xFF)
NARANJA = RGBColor(0xC2, 0x6A, 0x00)
FUENTE = "Calibri"
VIDEO = "https://youtu.be/v-vtEj7br2A"
POLITICA = "https://jhoel0521.github.io/studyplan-privacy/"


def leer_env():
    datos = {}
    ruta = DOC / ".env"
    if ruta.exists():
        for linea in ruta.read_text(encoding="utf-8").splitlines():
            linea = linea.strip()
            if linea and not linea.startswith("#") and "=" in linea:
                k, _, v = linea.partition("=")
                datos[k.strip()] = v.strip()
    return datos


ENV = leer_env()
prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)
TOTAL = 23
contador = {"n": 0}


# ------------------------------------------------------------------ utilidades
def forma(s, x, y, w, h, color, tipo=MSO_SHAPE.RECTANGLE, borde=None):
    f = s.shapes.add_shape(tipo, Inches(x), Inches(y), Inches(w), Inches(h))
    f.fill.solid()
    f.fill.fore_color.rgb = color
    if borde is None:
        f.line.fill.background()
    else:
        f.line.color.rgb = borde
        f.line.width = Pt(1)
    f.shadow.inherit = False
    return f


def texto(s, x, y, w, h, parrafos, ancla=MSO_ANCHOR.TOP, alinear=None):
    """parrafos: lista de (texto, tamaño, negrita, color, espacio_despues)."""
    tb = s.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.vertical_anchor = ancla
    tf.margin_left = tf.margin_right = Inches(0.05)
    tf.margin_top = tf.margin_bottom = Inches(0.02)
    for i, (t, tam, neg, col, esp) in enumerate(parrafos):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.space_after = Pt(esp)
        if alinear:
            p.alignment = alinear
        r = p.add_run()
        r.text = t
        r.font.name = FUENTE
        r.font.size = Pt(tam)
        r.font.bold = neg
        r.font.color.rgb = col
    return tb


def viñetas(s, x, y, w, h, items, tam=20, color=GRIS, espacio=8, marca="•"):
    parrafos = []
    for it in items:
        if isinstance(it, tuple):
            t, nivel = it
        else:
            t, nivel = it, 0
        sangria = "      " * nivel
        parrafos.append((f"{sangria}{marca if nivel == 0 else '–'}  {t}", tam - (2 if nivel else 0), False, color, espacio))
    return texto(s, x, y, w, h, parrafos)


def imagen(s, ruta, x, y, maxw, maxh, centrar=True):
    im = Image.open(ruta)
    w, h = im.size
    esc = min(maxw / (w / 100), maxh / (h / 100))
    ancho, alto = (w / 100) * esc, (h / 100) * esc
    px = x + (maxw - ancho) / 2 if centrar else x
    py = y + (maxh - alto) / 2 if centrar else y
    return s.shapes.add_picture(str(ruta), Inches(px), Inches(py), Inches(ancho), Inches(alto))


def notas(s, t):
    s.notes_slide.notes_text_frame.text = t


def nueva(titulo, subtitulo=None):
    contador["n"] += 1
    s = prs.slides.add_slide(prs.slide_layouts[6])
    forma(s, 0, 0, 13.333, 1.05, INDIGO)
    imagen(s, REC / "logos" / "play-store" / "icono-512.png", 0.35, 0.12, 0.8, 0.8)
    texto(s, 1.3, 0.1, 11.4, 0.6, [(titulo, 30, True, BLANCO, 0)], MSO_ANCHOR.MIDDLE)
    if subtitulo:
        texto(s, 1.3, 0.62, 11.4, 0.38, [(subtitulo, 15, False, INDIGO_CLARO, 0)], MSO_ANCHOR.MIDDLE)
    texto(s, 0.4, 7.08, 8, 0.3, [(f"StudyPlan · {ENV.get('SIGLA', '')} {ENV.get('MATERIA', '')}", 11, False, GRIS_CLARO, 0)])
    texto(s, 11.6, 7.08, 1.35, 0.3, [(f"{contador['n']} / {TOTAL}", 11, False, GRIS_CLARO, 0)], alinear=PP_ALIGN.RIGHT)
    return s


def tarjeta(s, x, y, w, h, titulo, items, color=INDIGO, tam=15, titulo_tam=18):
    forma(s, x, y, w, h, FONDO, MSO_SHAPE.ROUNDED_RECTANGLE, BORDE)
    forma(s, x + 0.2, y + 0.18, 0.09, 0.5, color)
    texto(s, x + 0.4, y + 0.12, w - 0.6, 0.62, [(titulo, titulo_tam, True, INDIGO_OSC, 0)], MSO_ANCHOR.MIDDLE)
    viñetas(s, x + 0.25, y + 0.85, w - 0.5, h - 0.95, items, tam=tam, espacio=5)


def cifra(s, x, y, w, numero, etiqueta, color=INDIGO):
    forma(s, x, y, w, 1.55, FONDO, MSO_SHAPE.ROUNDED_RECTANGLE, BORDE)
    texto(s, x, y + 0.08, w, 0.85, [(numero, 44, True, color, 0)], MSO_ANCHOR.MIDDLE, PP_ALIGN.CENTER)
    texto(s, x + 0.1, y + 0.92, w - 0.2, 0.6, [(etiqueta, 14, False, GRIS, 0)], alinear=PP_ALIGN.CENTER)


def tabla(s, x, y, w, filas, anchos, tam=14, alto_fila=0.42):
    n, m = len(filas), len(filas[0])
    gf = s.shapes.add_table(n, m, Inches(x), Inches(y), Inches(w), Inches(alto_fila * n))
    t = gf.table
    for j, a in enumerate(anchos):
        t.columns[j].width = Inches(a)
    for i, fila in enumerate(filas):
        t.rows[i].height = Inches(alto_fila)
        for j, val in enumerate(fila):
            c = t.cell(i, j)
            c.text = ""
            c.margin_left = c.margin_right = Inches(0.08)
            c.margin_top = c.margin_bottom = Inches(0.03)
            p = c.text_frame.paragraphs[0]
            r = p.add_run()
            r.text = val
            r.font.name = FUENTE
            r.font.size = Pt(tam)
            r.font.bold = (i == 0)
            r.font.color.rgb = BLANCO if i == 0 else GRIS
            c.fill.solid()
            c.fill.fore_color.rgb = INDIGO if i == 0 else (FONDO if i % 2 else BLANCO)
            c.vertical_anchor = MSO_ANCHOR.MIDDLE
    return t


def extraer_frame(video, segundo, destino):
    if not video.exists():
        return None
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-ss", str(segundo), "-i", str(video), "-frames:v", "1",
                    str(destino)], check=False)
    return destino if destino.exists() else None


# ------------------------------------------------------------------ 1. portada
contador["n"] += 1
s = prs.slides.add_slide(prs.slide_layouts[6])
forma(s, 0, 0, 13.333, 7.5, INDIGO)
forma(s, 0, 5.55, 13.333, 1.95, INDIGO_OSC)
forma(s, 0.6, 0.5, 2.35, 2.35, BLANCO, MSO_SHAPE.ROUNDED_RECTANGLE)
imagen(s, REC / "logos" / "logo.png", 0.75, 0.65, 2.05, 2.05)
forma(s, 10.38, 0.5, 2.35, 2.35, BLANCO, MSO_SHAPE.ROUNDED_RECTANGLE)
imagen(s, REC / "logos" / "play-store" / "icono-512.png", 10.53, 0.65, 2.05, 2.05)
texto(s, 3.2, 0.6, 7.0, 0.5, [(ENV.get("UNIVERSIDAD", ""), 18, True, INDIGO_CLARO, 0)], alinear=PP_ALIGN.CENTER)
texto(s, 3.2, 1.05, 7.0, 0.9, [(ENV.get("FACULTAD", ""), 18, False, INDIGO_CLARO, 0),
                               (ENV.get("CARRERA", ""), 18, False, INDIGO_CLARO, 0)], alinear=PP_ALIGN.CENTER)
texto(s, 0.6, 3.0, 12.1, 1.1, [("StudyPlan", 66, True, BLANCO, 0)], MSO_ANCHOR.MIDDLE, PP_ALIGN.CENTER)
texto(s, 1.2, 4.1, 10.9, 1.3,
      [("Aplicación móvil Android para gestionar y planificar el tiempo de estudio del estudiante universitario",
        24, False, INDIGO_CLARO, 0)], alinear=PP_ALIGN.CENTER)
texto(s, 0.8, 5.75, 11.7, 1.5,
      [(f"Defensa del proyecto · {ENV.get('MATERIA', '')} ({ENV.get('SIGLA', '')})", 20, True, BLANCO, 6),
       (f"Estudiante: {ENV.get('AUTOR', '')}", 20, False, BLANCO, 4),
       (f"Docente: {ENV.get('DOCENTE', '')}", 20, False, BLANCO, 0)], alinear=PP_ALIGN.CENTER)
notas(s, "Buenos días. Soy el autor de StudyPlan, una aplicación Android nativa en Java para planificar y medir el tiempo "
         "de estudio. Voy a presentar el problema, los objetivos, cómo se desarrolló, la arquitectura y los principios SOLID "
         "aplicados, y lo que logramos, incluida la publicación en Google Play y el uso auditado de inteligencia artificial.")

# ------------------------------------------------------------------ 2. agenda
s = nueva("Contenido de la presentación")
elementos = ["Introducción y problema", "Objetivos y alcance", "Desarrollo: metodología y requisitos",
             "Arquitectura hexagonal y principios SOLID", "Funcionalidades, interfaz y respaldo en Drive",
             "Calidad, IA auditada y publicación en Google Play", "Objetivos logrados y conclusiones"]
for i, e in enumerate(elementos):
    y = 1.45 + i * 0.78
    c = forma(s, 1.1, y, 0.6, 0.6, INDIGO, MSO_SHAPE.OVAL)
    texto(s, 1.1, y, 0.6, 0.6, [(str(i + 1), 22, True, BLANCO, 0)], MSO_ANCHOR.MIDDLE, PP_ALIGN.CENTER)
    texto(s, 2.0, y, 9.5, 0.6, [(e, 24, False, GRIS, 0)], MSO_ANCHOR.MIDDLE)
notas(s, "Sigo el orden pedido: introducción, objetivos, desarrollo, arquitectura y SOLID. Después muestro la aplicación, "
         "la calidad, el uso de IA y la publicación, y cierro con los objetivos logrados.")

# ------------------------------------------------------------------ 3. introducción
s = nueva("Introducción: el problema", "La autogestión del tiempo de estudio en la universidad")
viñetas(s, 0.6, 1.4, 7.6, 5.3, [
    "En la universidad gran parte del aprendizaje es estudio independiente que el propio estudiante planifica, ejecuta y evalúa.",
    "Planificar a corto plazo predice mejores calificaciones (Britton & Tesser, 1991).",
    "Sentir control sobre el propio tiempo se asocia con más rendimiento y menos estrés (Macan et al., 1990).",
    "Muchos planifican de manera informal y no registran cuánto estudian realmente frente a lo planificado.",
    "Las apps de calendario o tareas no miden horas de estudio por materia ni comparan lo planificado con lo ejecutado.",
], tam=19, espacio=12)
forma(s, 8.6, 1.5, 4.2, 4.6, INDIGO, MSO_SHAPE.ROUNDED_RECTANGLE)
texto(s, 8.6, 1.75, 4.2, 1.3, [("80–95 %", 56, True, BLANCO, 0)], MSO_ANCHOR.MIDDLE, PP_ALIGN.CENTER)
texto(s, 8.9, 3.1, 3.6, 1.8,
      [("de los estudiantes universitarios posterga sus tareas académicas, con menor rendimiento asociado", 18, False,
        INDIGO_CLARO, 0)], alinear=PP_ALIGN.CENTER)
texto(s, 8.9, 5.0, 3.6, 0.9, [("Steel (2007), metaanálisis sobre procrastinación", 13, False, INDIGO_CLARO, 0)],
      alinear=PP_ALIGN.CENTER)
notas(s, "El problema es real y está documentado: entre el 80 y el 95 por ciento de los universitarios posterga tareas, según "
         "Steel (2007). Lo que falta es una herramienta que mida lo estudiado de verdad frente a lo planificado. Esa es la "
         "brecha que cubre StudyPlan.")

# ------------------------------------------------------------------ 4. propuesta
s = nueva("La propuesta: StudyPlan", "Una app de autocontrol del estudio, nativa y sin conexión")
funciones = ["Materias con color y metas", "Sesiones con estados", "Calendario: día, semana y mes", "Pomodoro y No molestar",
             "Metas semanales", "Estadísticas y reportes PDF/CSV", "Recordatorios y widget de progreso",
             "Copia de seguridad en Drive"]
for i, f in enumerate(funciones):
    col, fila = i % 2, i // 2
    x, y = 0.6 + col * 4.1, 1.5 + fila * 1.25
    forma(s, x, y, 3.9, 1.05, FONDO, MSO_SHAPE.ROUNDED_RECTANGLE, BORDE)
    forma(s, x + 0.15, y + 0.15, 0.12, 0.75, INDIGO)
    texto(s, x + 0.4, y, 3.4, 1.05, [(f, 18, True, INDIGO_OSC, 0)], MSO_ANCHOR.MIDDLE)
for i, f in enumerate(["app_inicio.png", "app_calendario.png", "app_estadisticas.png"]):
    imagen(s, REC / "figuras" / f, 8.95 + (i % 2) * 1.95, 1.4 + (i // 2) * 0.0, 1.85, 4.0) if False else None
imagen(s, REC / "figuras" / "app_inicio.png", 8.95, 1.35, 1.95, 4.2)
imagen(s, REC / "figuras" / "app_calendario.png", 10.95, 1.35, 1.95, 4.2)
texto(s, 8.95, 5.65, 1.95, 0.4, [("Inicio", 13, False, GRIS_CLARO, 0)], alinear=PP_ALIGN.CENTER)
texto(s, 10.95, 5.65, 1.95, 0.4, [("Calendario", 13, False, GRIS_CLARO, 0)], alinear=PP_ALIGN.CENTER)
texto(s, 0.6, 6.55, 12, 0.45, [("Todo funciona sin conexión; internet solo se usa para la copia de seguridad opcional.", 16, True,
                                INDIGO, 0)])
notas(s, "StudyPlan permite registrar materias, planificar sesiones que pasan por estados, verlas en un calendario, estudiar "
         "con Pomodoro y No molestar, fijar metas semanales y consultar estadísticas y reportes. Es offline-first: solo usa "
         "internet para una copia de seguridad opcional en el Drive del propio usuario.")

# ------------------------------------------------------------------ 5. objetivos
s = nueva("Objetivos", "Objetivo general y objetivos específicos")
forma(s, 0.6, 1.3, 12.1, 1.35, INDIGO, MSO_SHAPE.ROUNDED_RECTANGLE)
texto(s, 0.85, 1.32, 11.6, 1.3,
      [("Objetivo general: desarrollar una app Android nativa en Java para gestionar materias, planificar y ejecutar sesiones "
        "de estudio, fijar metas semanales y consultar reportes, aplicando Scrum y desarrollo guiado por especificaciones "
        "con asistencia de IA auditada.", 17, False, BLANCO, 0)], MSO_ANCHOR.MIDDLE)
especificos = [
    "Levantar y especificar requisitos como HU, reglas de negocio y RNF (ISO/IEC 25010:2023) en OpenSpec.",
    "Materias y sesiones gobernadas por una máquina de estados con historial de cumplimiento.",
    "Calendario con vistas de día, semana y mes.",
    "Cronómetro, Pomodoro configurable y modo No molestar.",
    "Metas semanales, estadísticas y reportes en PDF y CSV.",
    "Copia de seguridad en Drive con cifrado opcional, bloqueo biométrico y borrado selectivo.",
    "Validar con pruebas y documentar y auditar el uso de IA.",
]
for i, e in enumerate(especificos):
    col, fila = i // 4, i % 4
    x, y = 0.6 + col * 6.15, 2.95 + fila * 0.98
    forma(s, x, y, 0.5, 0.5, INDIGO_CLARO, MSO_SHAPE.OVAL)
    texto(s, x, y, 0.5, 0.5, [(str(i + 1), 16, True, INDIGO_OSC, 0)], MSO_ANCHOR.MIDDLE, PP_ALIGN.CENTER)
    texto(s, x + 0.65, y - 0.1, 5.35, 0.9, [(e, 15, False, GRIS, 0)], MSO_ANCHOR.MIDDLE)
notas(s, "El objetivo general resume el proyecto. Los siete específicos cubren requisitos, sesiones con estados, calendario, "
         "técnicas de concentración, metas y reportes, protección de datos y validación con auditoría de IA. En el documento "
         "la evaluación de usabilidad con estudiantes queda como trabajo por completar.")

# ------------------------------------------------------------------ 6. tecnologías
s = nueva("Alcance y decisiones técnicas", "Stack fijado por el Product Owner")
tabla(s, 0.6, 1.4, 12.1, [
    ["Capa", "Tecnología", "Decisión"],
    ["Lenguaje", "Java", "Nativo; no Kotlin ni Compose"],
    ["Interfaz", "Views XML + Material Design 3", "Tema claro y oscuro, español e inglés"],
    ["Arquitectura", "Hexagonal en 3 capas + MVVM", "Lógica de negocio fuera de las Activities"],
    ["Datos", "Room (SQLite), un usuario local", "Offline-first; sin backend propio ni Firebase"],
    ["Respaldo", "Google Drive (carpeta de la app) + AES-256-GCM", "Opcional y apagado por defecto"],
    ["Gráficos y tareas", "MPAndroidChart, WorkManager, AlarmManager", "Un solo servicio en primer plano por sesión"],
    ["Plataforma", "minSdk 24 · targetSdk 37", "io.github.jhoel0521.studyplan"],
], [2.3, 5.1, 4.7], tam=15, alto_fila=0.62)
notas(s, "Estas decisiones las fijó el Product Owner y la IA no puede cambiarlas por iniciativa propia. Java nativo, vistas XML con "
         "Material 3, Room para los datos offline y WorkManager para recordatorios. El respaldo en Drive es opcional.")

# ------------------------------------------------------------------ 7. metodología
s = nueva("Desarrollo: metodología", "Scrum + desarrollo guiado por especificaciones (OpenSpec)")
imagen(s, REC / "diagramas" / "ciclo_scrum_sdd.png", 0.5, 1.3, 7.2, 5.5)
viñetas(s, 8.0, 1.5, 4.9, 5.3, [
    "Ningún código sin una especificación aprobada.",
    "Flujo: propuesta, diseño, tareas, implementación, validación y archivo.",
    "Trazabilidad: HU → spec → código → prueba.",
    "Cada clase y prueba menciona la HU que implementa.",
    "Commits con formato feat(HU-07).",
    "La IA propone y el humano decide.",
], tam=18, espacio=11)
notas(s, "Usamos Scrum con historias de usuario y OpenSpec para especificar. Primero se aprueba la propuesta, luego se implementa "
         "tarea por tarea y se valida con openspec validate --strict. Así cada línea de código se rastrea hasta una historia "
         "de usuario.")

# ------------------------------------------------------------------ 8. requisitos
s = nueva("Desarrollo: requisitos", "Especificados como historias de usuario, reglas y criterios de calidad")
cifra(s, 0.6, 1.4, 2.0, "46", "historias de usuario (línea base)")
cifra(s, 2.8, 1.4, 2.0, "30", "reglas de negocio")
cifra(s, 0.6, 3.15, 2.0, "19", "requisitos no funcionales")
cifra(s, 2.8, 3.15, 2.0, "10", "épicas con su spec OpenSpec")
texto(s, 0.6, 4.95, 4.2, 1.5, [("Los RNF siguen ISO/IEC 25010:2023, con métrica y método de verificación.", 15, False, GRIS, 0)])
tabla(s, 5.2, 1.4, 7.6, [
    ["Épica", "Contenido"],
    ["E1", "Perfil, inicio y ajustes"], ["E2", "Gestión de materias"], ["E3", "Sesiones de estudio"],
    ["E4", "Calendario de estudio"], ["E5", "Técnicas y eventos de estudio"], ["E6", "Metas semanales"],
    ["E7", "Estadísticas"], ["E8", "Reportes"], ["E9", "Recordatorios"], ["E10", "Respaldo y privacidad"],
], [1.2, 6.4], tam=14, alto_fila=0.5)
notas(s, "Los requisitos los decide el Product Owner. Hay 46 historias de usuario en la línea base, 30 reglas de negocio y 19 "
         "requisitos no funcionales según ISO 25010. Se agrupan en diez épicas, cada una con su especificación en OpenSpec. "
         "Después de la línea base se añadieron historias, por ejemplo el control de la sesión desde la notificación.")

# ------------------------------------------------------------------ 9. arquitectura
s = nueva("Arquitectura hexagonal", "Puertos y adaptadores en tres capas")
imagen(s, REC / "diagramas" / "arquitectura_hexagonal.png", 0.4, 1.2, 12.5, 3.9)
tarjeta(s, 0.6, 5.2, 3.9, 1.75, "Presentación", ["Activity o Fragment + ViewModel (MVVM)", "Solo enlaza vistas y delega"], INDIGO, 13, 16)
tarjeta(s, 4.7, 5.2, 3.9, 1.75, "Núcleo", ["Casos de uso y dominio en Java puro", "Sin android.*"], VERDE, 13, 16)
tarjeta(s, 8.8, 5.2, 3.9, 1.75, "Infraestructura", ["Room, Drive, huella o PIN", "El núcleo no depende de ellos"], NARANJA, 13, 16)
notas(s, "La arquitectura es hexagonal. La presentación depende del núcleo y la infraestructura también depende del núcleo, nunca "
         "al revés. El dominio y la aplicación son Java puro, sin imports de Android, y eso lo verifica una prueba "
         "automática. Por eso las reglas, como la máquina de estados, se prueban con JUnit sin emulador.")

# ------------------------------------------------------------------ 10. paquetes
s = nueva("Estructura de paquetes y reglas verificadas", "io.github.jhoel0521.studyplan")
tabla(s, 0.6, 1.4, 7.2, [
    ["Paquete", "Contenido"],
    ["domain", "entity, valueobject, enumeration, state, service, result, readmodel"],
    ["application.port.in.<épica>", "Puertos de entrada: un caso de uso por interfaz"],
    ["application.port.out", "persistence y system (por ejemplo Reloj)"],
    ["application.service.<épica>", "Un servicio por caso de uso"],
    ["adapter.in.ui.<épica>", "Activities y ViewModels"],
    ["adapter.out", "persistence, drive, security, system"],
], [2.9, 4.3], tam=14, alto_fila=0.68)
tarjeta(s, 8.2, 1.4, 4.5, 2.45, "ArquitecturaTest", ["Cero imports de android.* en domain y application", "Falla si el núcleo toca un adaptador"], VERDE, 14, 17)
tarjeta(s, 8.2, 4.05, 4.5, 2.45, "EstructuraPaquetesTest", ["Cada clase en la carpeta que dice qué es", "Se rompe si una clase está mal ubicada"], INDIGO, 14, 17)
notas(s, "La estructura de paquetes no es una convención informal: dos pruebas la hacen cumplir. ArquitecturaTest comprueba que "
         "el núcleo no importe Android y EstructuraPaquetesTest que cada clase esté en su carpeta. Si alguien rompe la "
         "arquitectura, las pruebas fallan.")

# ------------------------------------------------------------------ 11. SOLID
s = nueva("Principios SOLID aplicados", "Cada principio se ve en una clase concreta del proyecto")
tarjeta(s, 0.6, 1.35, 6.0, 2.65, "S · Responsabilidad única",
        ["Un caso de uso por puerto de entrada y por servicio", "Las Activity solo enlazan vistas y delegan al ViewModel"], INDIGO, 18, 20)
tarjeta(s, 6.75, 1.35, 6.0, 2.65, "O y L · Abierto/cerrado y Liskov",
        ["Patrón State en domain.state: EstadoPendiente, EstadoEnCurso, EstadoCompletada…", "Polimorfismo en lugar de switch dispersos"], VERDE, 18, 20)
tarjeta(s, 0.6, 4.2, 6.0, 2.65, "I · Segregación de interfaces",
        ["Puertos pequeños: MateriaRepository, SesionRepository, SesionesDeMateria, PreferenciasRepository, Reloj"], NARANJA, 18, 20)
tarjeta(s, 6.75, 4.2, 6.0, 2.65, "D · Inversión de dependencias",
        ["domain y application no importan android.*", "La hora actual entra por el puerto Reloj", "Room y Drive son adaptadores detrás de puertos"], INDIGO_OSC, 18, 20)
notas(s, "SRP: cada caso de uso tiene su puerto y su servicio. OCP y LSP: el ciclo de vida de la sesión usa el patrón State con una "
         "clase por estado. ISP: los puertos de salida son pequeños, por ejemplo Reloj o SesionRepository. DIP: el núcleo "
         "depende de interfaces; Room, Drive y la hora del sistema entran como adaptadores. Todo está verificado por pruebas.")

# ------------------------------------------------------------------ 12. State
s = nueva("Patrón State: ciclo de vida de la sesión", "Regla de negocio RN-07")
imagen(s, REC / "diagramas" / "maquina_estados.png", 0.4, 1.3, 7.9, 5.5)
viñetas(s, 8.5, 1.5, 4.4, 5.3, [
    "Una clase por estado en domain.state.",
    "EstadoPendiente, EstadoEnCurso, EstadoCompletada, EstadoCompletadaTardia, EstadoNoRealizada y EstadoReprogramada.",
    "Las transiciones válidas viven en cada estado.",
    "Se prueban con JUnit, sin emulador.",
], tam=17, espacio=10)
notas(s, "Una sesión puede estar pendiente, en curso, completada, completada tarde, no realizada o reprogramada. Cada estado es una "
         "clase que decide qué transiciones permite. Así evitamos condicionales dispersos y la regla se prueba aislada.")

# ------------------------------------------------------------------ 13. modelo de datos
s = nueva("Modelo de datos", "Room v8 · esquema exportado y diagrama generado desde él")
imagen(s, REC / "diagramas" / "modelo_logico.png", 0.4, 1.25, 12.5, 5.2)
texto(s, 0.6, 6.5, 12, 0.5, [("El diagrama y el diccionario de datos se generan del esquema real, por eso no pueden desfasarse del código.",
                              15, True, INDIGO, 0)])
notas(s, "La base es Room, versión 8, con tablas para perfil, ajustes, materias, sesiones, eventos, tramos diarios y metas. El "
         "diagrama sale del esquema JSON exportado; un script lo regenera y otro comprueba que coincida con el código.")

# ------------------------------------------------------------------ 14. interfaz
s = nueva("La aplicación en el teléfono", "Capturas reales, con el nombre y el correo ocultos")
caps = [("app_inicio.png", "Inicio"), ("app_calendario.png", "Calendario"), ("app_estadisticas.png", "Estadísticas"),
        ("app_materias.png", "Materias"), ("app_respaldo.png", "Copia de seguridad")]
for i, (f, t) in enumerate(caps):
    x = 0.55 + i * 2.5
    imagen(s, REC / "figuras" / f, x, 1.3, 2.3, 4.85)
    texto(s, x, 6.2, 2.3, 0.4, [(t, 15, True, INDIGO_OSC, 0)], alinear=PP_ALIGN.CENTER)
texto(s, 0.6, 6.6, 12.1, 0.4, [("Material Design 3 · tema claro y oscuro · español e inglés", 14, False, GRIS_CLARO, 0)], alinear=PP_ALIGN.CENTER)
notas(s, "Estas son pantallas reales de la app en un teléfono Android: inicio con la meta semanal, calendario, estadísticas, "
         "materias y la pantalla de copia de seguridad. Se les ocultó el nombre y el correo.")

# ------------------------------------------------------------------ 15. ejecución de la sesión
s = nueva("Ejecución de una sesión de estudio", "Notificación con controles, Pomodoro y widget (HU-49)")
video = DOC / "dist" / "StudyPlan-demo.mp4"
alt = Path.home() / "Documents" / "studyplan-release" / "StudyPlan-demo.mp4"
video = video if video.exists() else alt
tmp = Path(tempfile.mkdtemp())
frames = [extraer_frame(video, t, tmp / f"f{t}.png") for t in (181, 200, 240)]
for i, f in enumerate(frames):
    if f:
        imagen(s, f, 0.55 + i * 2.15, 1.3, 2.0, 4.3)
texto(s, 0.55, 5.7, 6.3, 0.9, [("Panel de notificaciones · sesión en curso · widget de progreso (emulador, datos de ejemplo)", 13, False,
                                GRIS_CLARO, 0)])
viñetas(s, 7.2, 1.4, 5.7, 5.4, [
    "La notificación muestra la materia y el tiempo, con Pausar, Detener y No molestar.",
    "Un único servicio en primer plano, solo mientras hay una sesión En curso.",
    "Pomodoro configurable y modo No molestar.",
    "Widget con el progreso semanal en la pantalla de inicio.",
    "Reportes por periodo exportables a PDF y CSV.",
], tam=18, espacio=11)
notas(s, "Al empezar una sesión la app muestra una notificación persistente con los botones Pausar, Detener y No molestar, también "
         "en la pantalla de bloqueo. Hay un widget de progreso semanal y reportes exportables. En el video de demostración se "
         "ve este recorrido completo.")

# ------------------------------------------------------------------ 16. respaldo
s = nueva("Respaldo y privacidad (épica 10)", "HU-35 a HU-38 · opcional, apagado por defecto")
pasos = [("Base Room", "Instantánea de las tablas"), ("JSON", "Serializador en Java puro"),
         ("AES-256-GCM", "Clave por PBKDF2 (120 000 iteraciones)"), ("Google Drive", "Carpeta oculta de la app")]
for i, (a, b) in enumerate(pasos):
    x = 0.6 + i * 3.15
    forma(s, x, 1.45, 2.8, 1.5, INDIGO if i != 2 else VERDE, MSO_SHAPE.ROUNDED_RECTANGLE)
    texto(s, x, 1.55, 2.8, 0.65, [(a, 21, True, BLANCO, 0)], MSO_ANCHOR.MIDDLE, PP_ALIGN.CENTER)
    texto(s, x + 0.1, 2.15, 2.6, 0.75, [(b, 13, False, INDIGO_CLARO, 0)], alinear=PP_ALIGN.CENTER)
    if i < 3:
        texto(s, x + 2.78, 1.85, 0.4, 0.6, [("→", 26, True, INDIGO, 0)], MSO_ANCHOR.MIDDLE, PP_ALIGN.CENTER)
viñetas(s, 0.7, 3.3, 12, 3.5, [
    "Contraseña opcional guardada cifrada con Android KeyStore, para los respaldos automáticos.",
    "Restauración transaccional: si el respaldo trae referencias rotas, no se toca nada.",
    "Frecuencia: nunca, diaria, semanal, mensual o solo manual, con WorkManager y red conectada.",
    "Borrado por alcance (teléfono, Drive o ambos) con doble confirmación.",
    "Probado en un teléfono real: se borraron los datos y se restauraron desde Drive.",
], tam=18, espacio=10)
notas(s, "La copia toma una instantánea de la base, la convierte a JSON y, si el estudiante define una contraseña, la cifra con "
         "AES-256-GCM antes de subirla a la carpeta oculta de la app en su Drive. La restauración es transaccional y valida "
         "las claves foráneas antes de confirmar. El Product Owner lo probó en su teléfono: borró los datos y los restauró.")

# ------------------------------------------------------------------ 17. calidad
s = nueva("Calidad y verificación", "Pruebas, análisis estático e integración continua")
cifra(s, 0.6, 1.4, 2.85, "456", "pruebas unitarias sin fallos")
cifra(s, 3.7, 1.4, 2.85, "0", "errores de lint")
cifra(s, 6.8, 1.4, 2.85, "327", "clases cotejadas con los diagramas")
cifra(s, 9.9, 1.4, 2.85, "11", "declaraciones de Play Console")
viñetas(s, 0.7, 3.4, 12, 3.5, [
    "Pruebas unitarias en JUnit para el núcleo, y pruebas instrumentadas en emulador para Room, pantallas y respaldo.",
    "Flujo de CI en GitHub Actions: pruebas y lint en cada cambio.",
    "Release firmado por etiqueta: paquete AAB y APK, con verificación de la firma (apksigner).",
    "Especificaciones validadas con openspec validate --strict.",
], tam=18, espacio=10)
notas(s, "Hay 456 pruebas unitarias en verde y el lint no da errores. El CI ejecuta la compuerta de calidad y el release se firma "
         "por etiqueta. Dato para responder con honestidad: en el emulador se ejecutaron 164 pruebas instrumentadas y hubo "
         "3 fallos ajenos a la épica de respaldo, 2 que ya fallaban antes y 1 inestable por el foco de la ventana.")

# ------------------------------------------------------------------ 18. IA
s = nueva("Inteligencia artificial auditada", "Claude Code y OpenSpec, con la bitácora como evidencia")
tarjeta(s, 0.6, 1.35, 6.0, 5.4, "Cómo se usó", [
    "Claude Code para specs, código, pruebas y documentación.",
    "OpenSpec para especificar y validar.",
    "Más de 170 entradas en la bitácora: solicitud, resultado de la IA, decisión humana y verificación.",
    "Referencias, DOI y versiones propuestas por la IA se verifican a mano.",
], INDIGO, 19, 21)
tarjeta(s, 6.75, 1.35, 6.0, 5.4, "Errores de la IA que se registraron", [
    "Redactó requisitos sin preguntar y supuso «sin internet»: rechazado.",
    "Una corrección que no bastó al vincular la cuenta de Google.",
    "Actuó sobre el teléfono sin permiso expreso.",
    "Afirmó sin comprobar que el logo dependía de la aprobación de Google.",
], NARANJA, 19, 21)
notas(s, "Toda interacción con la IA se documenta: A.1 declaración de uso, A.2 protocolo, A.3 OpenSpec, A.4 bitácora, A.5 riesgos "
         "y A.6 reflexión. Lo importante es que los errores de la IA también se registran: son la evidencia de que hubo "
         "control humano. La IA propone y el humano decide.")

# ------------------------------------------------------------------ 19. publicación
s = nueva("Publicación en Google Play", "Versión 0.1.0 en prueba interna; la revisión de Google queda pendiente y documentada")
imagen(s, REC / "figuras" / "play_prueba_interna.jpg", 0.5, 1.3, 6.2, 3.3)
imagen(s, REC / "figuras" / "play_declaraciones_al_dia.jpg", 6.9, 1.3, 6.0, 3.3)
viñetas(s, 0.7, 4.75, 12, 2.2, [
    "Clave de subida propia, release firmado por el CI, app creada y versión 1000 (0.1.0) en prueba interna.",
    "11 declaraciones de contenido al día, ficha con ícono, gráfico y capturas, y video de demostración público.",
    "Pendiente: envío a revisión, prueba cerrada y clientes OAuth de Drive para la versión instalada desde Play.",
], tam=16, espacio=7)
notas(s, "La app está creada en Play Console y la versión 0.1.0 firmada está en prueba interna. Las once declaraciones están al día. "
         "Esta presentación no afirma que esté publicada para el público: el botón de enviar a revisión sigue desactivado por "
         "una causa que no se pudo determinar, y Play pide además una prueba cerrada. Todo está en el capítulo 5.")

# ------------------------------------------------------------------ 20. demostración
s = nueva("Demostración", "Recorrido de 6 min 36 s grabado en un emulador, con datos de ejemplo")
forma(s, 1.2, 1.6, 10.9, 2.0, FONDO, MSO_SHAPE.ROUNDED_RECTANGLE, BORDE)
texto(s, 1.2, 1.7, 10.9, 0.8, [("Video de demostración", 28, True, INDIGO_OSC, 0)], MSO_ANCHOR.MIDDLE, PP_ALIGN.CENTER)
texto(s, 1.2, 2.5, 10.9, 0.9, [(VIDEO, 30, True, INDIGO, 0)], MSO_ANCHOR.MIDDLE, PP_ALIGN.CENTER)
viñetas(s, 1.5, 4.0, 10.4, 3.0, [
    "Calendario, estadísticas y materias.",
    "Creación de una sesión e inicio de Pomodoro.",
    "Notificación con Pausar, Detener y No molestar, y pantalla de bloqueo.",
    "Widget de progreso, reportes, ajustes, copia de seguridad y privacidad.",
], tam=19, espacio=9)
notas(s, "Aquí se puede mostrar el video o abrirlo desde el enlace. Recorre el calendario, las estadísticas, la sesión con la "
         "notificación y los controles, el widget, los reportes y la copia de seguridad.")

# ------------------------------------------------------------------ 21. objetivos logrados
s = nueva("Objetivos logrados", "Tres objetivos cumplidos")
logros = [
    ("1", "Copia de seguridad en Google Drive", ["Respaldo y restauración probados en un teléfono real", "Cifrado opcional AES-256-GCM", "Borrado en teléfono, Drive o ambos"], "OBJETIVO CUMPLIDO"),
    ("2", "Publicación en Google Play", ["App y versión 0.1.0 firmada en prueba interna", "11 declaraciones y ficha con logo", "Pendiente y documentado: revisión de Google"], "CUMPLIDO: PENDIENTE Y DOCUMENTADO"),
    ("3", "Documento final", ["8 capítulos y 140 páginas", "43 tablas, 39 figuras y 0 pendientes", "Capítulo de publicación y Auditoría IA"], "OBJETIVO CUMPLIDO"),
]
for i, (n, t, its, sello) in enumerate(logros):
    x = 0.55 + i * 4.19
    forma(s, x, 1.35, 3.95, 5.3, FONDO, MSO_SHAPE.ROUNDED_RECTANGLE, BORDE)
    forma(s, x + 0.25, 1.6, 0.62, 0.62, INDIGO, MSO_SHAPE.OVAL)
    texto(s, x + 0.25, 1.6, 0.62, 0.62, [(n, 24, True, BLANCO, 0)], MSO_ANCHOR.MIDDLE, PP_ALIGN.CENTER)
    texto(s, x + 1.0, 1.5, 2.85, 0.85, [(t, 19, True, INDIGO_OSC, 0)], MSO_ANCHOR.MIDDLE)
    viñetas(s, x + 0.25, 2.6, 3.5, 2.8, its, tam=15, espacio=9, marca="✔")
    sl = forma(s, x + 0.25, 5.85, 3.45, 0.5, VERDE, MSO_SHAPE.ROUNDED_RECTANGLE)
    texto(s, x + 0.25, 5.85, 3.45, 0.5, [(sello, 12, True, BLANCO, 0)], MSO_ANCHOR.MIDDLE, PP_ALIGN.CENTER)
notas(s, "Los tres objetivos planteados están cumplidos. El respaldo en Drive funciona y se probó en un teléfono real. La "
         "publicación se cumplió en su versión 'pendiente y documentada': la app está en prueba interna y falta la revisión de "
         "Google. El documento está terminado, con cero marcas pendientes.")

# ------------------------------------------------------------------ 22. conclusiones
s = nueva("Conclusiones y trabajo futuro")
tarjeta(s, 0.6, 1.35, 6.0, 5.4, "Conclusiones", [
    "La diferencia está en especificar, validar y defender, no en escribir código.",
    "Una arquitectura hexagonal con pruebas que la hacen cumplir permitió crecer a diez épicas sin romper el núcleo.",
    "Offline-first con un respaldo opcional y cifrado protege los datos del estudiante.",
    "Documentar los errores de la IA demuestra el control humano del proyecto.",
], VERDE, 19, 22)
tarjeta(s, 6.75, 1.35, 6.0, 5.4, "Trabajo futuro", [
    "Enviar la ficha a revisión de Google y ejecutar la prueba cerrada.",
    "Crear los clientes OAuth de Drive para la versión instalada desde Play.",
    "Completar la evaluación de usabilidad con estudiantes.",
    "Traducir la ficha de la tienda al inglés.",
], INDIGO, 19, 22)
notas(s, "La conclusión es que el valor está en especificar, validar y defender. La arquitectura con pruebas que la protegen nos "
         "permitió crecer sin romper el núcleo. Lo que queda: la revisión de Google, la prueba cerrada, los clientes OAuth de "
         "Drive y completar la evaluación de usabilidad.")

# ------------------------------------------------------------------ 23. cierre
contador["n"] += 1
s = prs.slides.add_slide(prs.slide_layouts[6])
forma(s, 0, 0, 13.333, 7.5, INDIGO)
forma(s, 5.2, 0.6, 2.9, 2.9, BLANCO, MSO_SHAPE.ROUNDED_RECTANGLE)
imagen(s, REC / "logos" / "play-store" / "icono-512.png", 5.35, 0.75, 2.6, 2.6)
texto(s, 0.6, 3.75, 12.1, 1.2, [("¡Gracias!", 60, True, BLANCO, 0)], MSO_ANCHOR.MIDDLE, PP_ALIGN.CENTER)
texto(s, 0.6, 4.95, 12.1, 0.7, [("¿Preguntas?", 30, False, INDIGO_CLARO, 0)], MSO_ANCHOR.MIDDLE, PP_ALIGN.CENTER)
texto(s, 0.6, 5.9, 12.1, 1.3,
      [(f"Video: {VIDEO}", 16, False, BLANCO, 3),
       (f"Política de privacidad: {POLITICA}", 16, False, BLANCO, 3),
       ("Código: github.com/jhoel0521/studyplan", 16, False, BLANCO, 0)], alinear=PP_ALIGN.CENTER)
notas(s, "Gracias. Quedo atento a las preguntas. Si piden detalle: arquitectura y SOLID están en el capítulo 4, la publicación en "
         "el capítulo 5 y la auditoría de IA en el anexo A.")

assert contador["n"] == TOTAL, (contador["n"], TOTAL)
prs.save(str(SALIDA))
print("ok", SALIDA, f"{contador['n']} láminas")
