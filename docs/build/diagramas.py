"""Genera los diagramas de la Entrega 1 con Pillow (sin dependencias externas).

Uso: python docs/build/diagramas.py
Salida: docs/documento/recursos/diagramas/*.png
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "docs" / "documento" / "recursos" / "diagramas"
OUT.mkdir(parents=True, exist_ok=True)

FONTS = Path("C:/Windows/Fonts")


def font(size, bold=False, italic=False):
    name = "arial"
    if bold and italic:
        name = "arialbi"
    elif bold:
        name = "arialbd"
    elif italic:
        name = "ariali"
    return ImageFont.truetype(str(FONTS / f"{name}.ttf"), size)


def sym(size):
    return ImageFont.truetype(str(FONTS / "seguisym.ttf"), size)


BLACK = (20, 20, 20)
GRAY = (120, 120, 120)
LIGHT = (225, 225, 225)
MID = (190, 190, 190)
WHITE = (255, 255, 255)


def text_w(d, s, f):
    return d.textlength(s, font=f)


def dashed_line(d, pts, fill=BLACK, width=3, dash=14, gap=10):
    for (x1, y1), (x2, y2) in zip(pts, pts[1:]):
        length = math.hypot(x2 - x1, y2 - y1)
        if length == 0:
            continue
        ux, uy = (x2 - x1) / length, (y2 - y1) / length
        pos = 0
        while pos < length:
            end = min(pos + dash, length)
            d.line([(x1 + ux * pos, y1 + uy * pos), (x1 + ux * end, y1 + uy * end)],
                   fill=fill, width=width)
            pos += dash + gap


def arrow_head(d, p_from, p_to, size=22, open_=True, fill=BLACK):
    ang = math.atan2(p_to[1] - p_from[1], p_to[0] - p_from[0])
    a1 = (p_to[0] - size * math.cos(ang - 0.45), p_to[1] - size * math.sin(ang - 0.45))
    a2 = (p_to[0] - size * math.cos(ang + 0.45), p_to[1] - size * math.sin(ang + 0.45))
    if open_:
        d.line([a1, p_to, a2], fill=fill, width=3)
    else:
        d.polygon([a1, p_to, a2], fill=fill)


def diamond(d, p_at, p_dir, size=24, filled=True):
    ang = math.atan2(p_dir[1] - p_at[1], p_dir[0] - p_at[0])
    tip = p_at
    mid = (p_at[0] + size * math.cos(ang), p_at[1] + size * math.sin(ang))
    back = (p_at[0] + 2 * size * math.cos(ang), p_at[1] + 2 * size * math.sin(ang))
    l = (mid[0] + size * 0.5 * math.cos(ang + math.pi / 2), mid[1] + size * 0.5 * math.sin(ang + math.pi / 2))
    r = (mid[0] + size * 0.5 * math.cos(ang - math.pi / 2), mid[1] + size * 0.5 * math.sin(ang - math.pi / 2))
    d.polygon([tip, l, back, r], fill=BLACK if filled else WHITE, outline=BLACK)
    return back


# ---------------------------------------------------------------- modelo de dominio
class UMLBox:
    PAD = 14
    LINE = 30

    def __init__(self, name, attrs, ops, x, y, w=600, stereotype=None):
        self.name, self.attrs, self.ops = name, attrs, ops
        self.x, self.y, self.w = x, y, w
        self.stereotype = stereotype
        head = (2 if stereotype else 1) * 34 + 2 * self.PAD
        body = max(1, len(attrs)) * self.LINE + 2 * self.PAD
        tail = (len(ops) * self.LINE + 2 * self.PAD) if ops is not None else 0
        self.h = head + body + tail
        self.head, self.body = head, body

    # puntos de anclaje
    def left(self, f=0.5):
        return (self.x, self.y + self.h * f)

    def right(self, f=0.5):
        return (self.x + self.w, self.y + self.h * f)

    def top(self, f=0.5):
        return (self.x + self.w * f, self.y)

    def bottom(self, f=0.5):
        return (self.x + self.w * f, self.y + self.h)

    def draw(self, d):
        x, y, w = self.x, self.y, self.w
        d.rectangle([x + 8, y + 8, x + w + 8, y + self.h + 8], fill=MID)  # sombra
        d.rectangle([x, y, x + w, y + self.h], fill=WHITE, outline=BLACK, width=3)
        d.rectangle([x, y, x + w, y + self.head], fill=LIGHT, outline=BLACK, width=3)
        cy = y + self.PAD
        if self.stereotype:
            s = f"«{self.stereotype}»"
            d.text((x + (w - text_w(d, s, font(24, italic=True))) / 2, cy), s, font=font(24, italic=True), fill=BLACK)
            cy += 34
        d.text((x + (w - text_w(d, self.name, font(28, True))) / 2, cy), self.name, font=font(28, True), fill=BLACK)
        cy = y + self.head + self.PAD
        for a in self.attrs:
            d.text((x + 18, cy), a, font=font(22), fill=BLACK)
            cy += self.LINE
        if self.ops is not None:
            ly = y + self.head + self.body
            d.line([x, ly, x + w, ly], fill=BLACK, width=3)
            cy = ly + self.PAD
            for o in self.ops:
                d.text((x + 18, cy), o, font=font(22), fill=BLACK)
                cy += self.LINE


def label(d, xy, s, size=22, bold=False, anchor="mm", bg=True):
    f = font(size, bold)
    if bg:
        w = text_w(d, s, f)
        cx, cy = xy
        if anchor == "mm":
            d.rectangle([cx - w / 2 - 4, cy - size / 2 - 3, cx + w / 2 + 4, cy + size / 2 + 3], fill=WHITE)
    d.text(xy, s, font=f, fill=BLACK, anchor=anchor)


def assoc(d, p1, p2, name=None, m1=None, m2=None, name_off=(0, -26), m_off=26):
    d.line([p1, p2], fill=BLACK, width=3)
    if name:
        label(d, ((p1[0] + p2[0]) / 2 + name_off[0], (p1[1] + p2[1]) / 2 + name_off[1]), name, bold=True)
    horiz = abs(p2[0] - p1[0]) > abs(p2[1] - p1[1])
    if m1:
        label(d, (p1[0] + (28 if horiz else -34) * (1 if p2[0] >= p1[0] or not horiz else -1),
                  p1[1] + (m_off if horiz else (28 if p2[1] > p1[1] else -28))), m1)
    if m2:
        label(d, (p2[0] + (-40 if horiz and p2[0] > p1[0] else (40 if horiz else -40)),
                  p2[1] + (m_off if horiz else (-28 if p2[1] > p1[1] else 28))), m2)


def modelo_dominio():
    W, H = 3080, 2000
    img = Image.new("RGB", (W, H), WHITE)
    d = ImageDraw.Draw(img)
    c0, c1, c2, c3 = 40, 820, 1620, 2460
    r1, r2 = 40, 640

    aj = UMLBox("Ajustes", ["- tema: Tema {CLARO, OSCURO, SISTEMA}", "- idioma: String {es, en}",
                            "- bloqueoBiometrico: boolean", "- noMolestarGlobal: boolean",
                            "- recordatorioMin: int {5, 15, 30}", "- pomodoroTrabajoMin: int = 25",
                            "- pomodoroDescansoCortoMin: int = 5", "- pomodoroDescansoLargoMin: int = 15",
                            "- pomodoroCiclosHastaLargo: int = 4"], None, c0, r1, w=600)
    est = UMLBox("Estudiante", ["- id: long", "- nombre: String", "- metaPredeterminadaHoras: int"],
                 ["+ saludo(): String"], c1, r1, w=580)
    mat = UMLBox("Materia", ["- id: long", "- nombre: String", "- color: int", "- docente: String",
                             "- archivada: boolean"],
                 ["+ horasAcumuladas(): double", "+ archivar(): void"], c2, r1, w=620)
    dis = UMLBox("DistribucionMateria", ["- horas: double", "- porcentaje: double"],
                 ["+ etiqueta(): String"], c3, r1, w=600)
    resp = UMLBox("ConfiguracionRespaldo", ["- frecuencia: FrecuenciaRespaldo", "  {NUNCA, DIARIA, SEMANAL,",
                                            "   MENSUAL, SOLO_MANUAL}", "- cuentaGoogle: String",
                                            "- cifrado: boolean", "- ultimoRespaldo: LocalDateTime"],
                  ["+ respaldar(): void", "+ restaurar(clave: char[]): void", "+ borrar(alcance: AlcanceBorrado)"],
                  c0, aj.y + aj.h + 110, w=600)
    meta = UMLBox("MetaSemanal", ["- id: long", "- semanaInicio: LocalDate", "- horasObjetivo: int",
                                  "- felicitacionMostrada: boolean"],
                  ["+ progreso(horas: double): double", "+ estaCumplida(horas: double): boolean"], c1, r2, w=580)
    ses = UMLBox("SesionEstudio", ["- id: long", "- fecha: LocalDate", "- horaInicio: LocalTime",
                                   "- horaFin: LocalTime", "- estado: EstadoSesion",
                                   "- tecnica: Tecnica {LIBRE, POMODORO}", "- duracionRealMin: Integer",
                                   "- forzarNoMolestar: Boolean", "- notas: String"],
                 ["+ duracionPlanificadaMin(): int", "+ iniciar(): void", "+ completar(min: int): void",
                  "+ registrarTardio(min: int): void", "+ reprogramar(f, hi, hf): SesionEstudio",
                  "+ vencer(): void", "+ seSolapaCon(otra: SesionEstudio): boolean"], c2, r2, w=620)
    rep = UMLBox("Reporte", ["- periodoInicio: LocalDate", "- periodoFin: LocalDate",
                             "- tipo: TipoPeriodo {SEMANAL, MENSUAL}"],
                 ["+ horasPlanificadas(): double", "+ horasReales(): double",
                  "+ cumplimiento(): double", "+ exportar(formato): File"], c3, r2, w=600)
    estado = UMLBox("EstadoSesion", ["PENDIENTE", "EN_CURSO", "COMPLETADA", "COMPLETADA_TARDIA",
                                     "NO_REALIZADA", "REPROGRAMADA"], None, c3 + 60, rep.y + rep.h + 90,
                    w=480, stereotype="enumeration")
    rec = UMLBox("Recordatorio", ["- id: long", "- minutosAntes: int {5, 15, 30}", "- activo: boolean"],
                 ["+ horaDisparo(): LocalDateTime"], c2 + 200, ses.y + ses.h + 110, w=520)
    ev = UMLBox("EventoEstudio", ["- id: long", "- tipo: TipoEvento", "  {INICIO, PAUSA, REANUDACION,",
                                  "   SALIDA_APP, REGRESO_APP,", "   FIN_PLANIFICADO, TIEMPO_EXTRA,",
                                  "   ESTUDIO_APP_CERRADA, CAMBIO_SESION,", "   AJUSTE_MANUAL, FIN}",
                                  "- instante: Instant", "- nota: String"],
                ["+ dentroDeApp(): boolean"], c1, ses.y + ses.h + 40, w=580)

    # Ajustes 1 —◆ Estudiante
    y = est.y + 70
    back = diamond(d, (est.x, y), (aj.x + aj.w, y))
    assoc(d, back, (aj.x + aj.w, y), "configura", None, "1", name_off=(0, -26))
    # Ajustes ◆— ConfiguracionRespaldo
    x = aj.x + aj.w / 2
    back = diamond(d, (x, aj.y + aj.h), (x, resp.y))
    assoc(d, back, (x, resp.y), "respalda con", None, "1", name_off=(90, 0))
    # Estudiante — Materia
    y = est.y + 110
    assoc(d, (est.x + est.w, y), (mat.x, y), "cursa", "1", "0..*")
    # Estudiante — MetaSemanal
    x = est.x + est.w / 2
    assoc(d, (x, est.y + est.h), (x, meta.y), "define", "1", "0..*", name_off=(56, 0))
    # MetaSemanal 0..* — 0..1 Materia (meta por materia; null = meta total)
    xm, xmat = meta.x + meta.w - 70, mat.x + 90
    ymid = (est.y + est.h + meta.y) / 2 + 40
    d.line([(xm, meta.y), (xm, ymid), (xmat, ymid), (xmat, mat.y + mat.h)], fill=BLACK, width=3)
    label(d, ((xm + xmat) / 2 + 20, ymid - 24), "de (opcional)", bold=True)
    label(d, (xm + 40, meta.y - 24), "0..*")
    label(d, (xmat - 40, mat.y + mat.h + 26), "0..1")
    # Materia ◆— SesionEstudio
    x = mat.x + mat.w * 0.6
    back = diamond(d, (x, mat.y + mat.h), (x, ses.y))
    assoc(d, back, (x, ses.y), "agrupa", None, "0..*", name_off=(60, 0))
    label(d, (x - 24, mat.y + mat.h + 62), "1")
    # DistribucionMateria — Materia
    y = dis.y + dis.h / 2
    d.line([(dis.x, y), (mat.x + mat.w, y)], fill=BLACK, width=3)
    arrow_head(d, (dis.x, y), (mat.x + mat.w, y))
    label(d, ((mat.x + mat.w + dis.x) / 2, y - 26), "de", bold=True)
    label(d, (mat.x + mat.w + 26, y + 24), "1")
    label(d, (dis.x - 40, y + 24), "0..*")
    # Reporte ◆— DistribucionMateria
    x = dis.x + dis.w / 2
    back = diamond(d, (x, rep.y), (x, dis.y + dis.h))
    assoc(d, back, (x, dis.y + dis.h), "contiene", None, "1..*", name_off=(70, 0))
    label(d, (x - 24, rep.y - 62), "1")
    # Reporte ..> SesionEstudio
    y = rep.y + rep.h * 0.35
    dashed_line(d, [(rep.x, y), (ses.x + ses.w, y)])
    arrow_head(d, (rep.x, y), (ses.x + ses.w, y))
    label(d, ((ses.x + ses.w + rep.x) / 2, y - 26), "«calcula desde»")
    # MetaSemanal ..> SesionEstudio
    y = meta.y + meta.h * 0.55
    dashed_line(d, [(meta.x + meta.w, y), (ses.x, y)])
    arrow_head(d, (meta.x + meta.w, y), (ses.x, y))
    label(d, ((meta.x + meta.w + ses.x) / 2, y - 26), "«suma horas»")
    # SesionEstudio ..> EstadoSesion
    y = estado.y + 60
    dashed_line(d, [(ses.x + ses.w, y), (estado.x, y)])
    arrow_head(d, (ses.x + ses.w, y), (estado.x, y))
    label(d, ((ses.x + ses.w + estado.x) / 2, y - 26), "«usa»")
    # SesionEstudio — Recordatorio
    x = rec.x + rec.w * 0.6
    assoc(d, (x, ses.y + ses.h), (x, rec.y), "avisa", "1", "0..1", name_off=(56, 0))
    # autoasociación: reprogramada en
    bx = ses.x
    y1, y2 = ses.y + ses.h - 160, ses.y + ses.h - 40
    d.line([(bx, y1), (bx - 90, y1), (bx - 90, y2), (bx, y2)], fill=BLACK, width=3)
    arrow_head(d, (bx - 90, y2), (bx, y2))
    label(d, (bx - 200, (y1 + y2) / 2), "reprograma en", bold=True)
    label(d, (bx - 60, y1 - 22), "origen 0..1", size=18)
    label(d, (bx - 60, y2 + 22), "nueva 0..1", size=18)

    # SesionEstudio ◆— 0..* EventoEstudio
    sx = ses.x + 60
    ey = ev.y + 70
    back = diamond(d, (sx, ses.y + ses.h), (sx, ey))
    d.line([back, (sx, ey), (ev.x + ev.w, ey)], fill=BLACK, width=3)
    label(d, (sx + 26, ses.y + ses.h + 62), "1")
    label(d, (ev.x + ev.w + 44, ey - 24), "0..*")
    label(d, ((sx + ev.x + ev.w) / 2 - 10, ey + 26), "registra", bold=True)

    for b in (aj, est, mat, dis, resp, meta, ses, rep, estado, rec, ev):
        b.draw(d)
    d.text((W - 30, H - 30), "Modelo de dominio conceptual — StudyPlan v0.1.0", font=font(22, italic=True),
           fill=GRAY, anchor="rd")
    img.save(OUT / "modelo_dominio.png", dpi=(200, 200))


# ---------------------------------------------------------------- máquina de estados
def maquina_estados():
    W, H = 2300, 1380
    img = Image.new("RGB", (W, H), WHITE)
    d = ImageDraw.Draw(img)
    bw, bh = 400, 120
    S = {
        "PENDIENTE": (430, 640), "EN_CURSO": (1130, 300), "COMPLETADA": (1850, 300),
        "REPROGRAMADA": (1130, 640), "NO_REALIZADA": (1130, 1040), "COMPLETADA_TARDIA": (1850, 1040),
    }
    names = {"PENDIENTE": "Pendiente", "EN_CURSO": "En curso", "COMPLETADA": "Completada",
             "REPROGRAMADA": "Reprogramada", "NO_REALIZADA": "No realizada",
             "COMPLETADA_TARDIA": "Completada\n(registro tardío)"}

    def L(k):
        return (S[k][0] - bw / 2, S[k][1])

    def R(k):
        return (S[k][0] + bw / 2, S[k][1])

    def T(k, dx=0):
        return (S[k][0] + dx, S[k][1] - bh / 2)

    def B(k, dx=0):
        return (S[k][0] + dx, S[k][1] + bh / 2)

    def arrow(pts, text=None, at=None, dashed=False, size=22):
        (dashed_line if dashed else lambda dd, p, **k: dd.line(p, fill=BLACK, width=3))(d, pts)
        arrow_head(d, pts[-2], pts[-1], size=24, open_=False)
        if text:
            label(d, at, text, size=size)

    def final(c):
        d.ellipse([c[0] - 24, c[1] - 24, c[0] + 24, c[1] + 24], outline=BLACK, width=4)
        d.ellipse([c[0] - 13, c[1] - 13, c[0] + 13, c[1] + 13], fill=BLACK)

    # inicio
    d.ellipse([80 - 20, 640 - 20, 80 + 20, 640 + 20], fill=BLACK)
    arrow([(100, 640), L("PENDIENTE")], "planificar", (155, 610))
    # Pendiente -> En curso
    arrow([T("PENDIENTE", 120), (S["PENDIENTE"][0] + 120, 300), L("EN_CURSO")], "iniciar", (760, 270))
    # En curso self loop
    x, y = S["EN_CURSO"][0], S["EN_CURSO"][1] - bh / 2
    d.line([(x - 80, y), (x - 80, y - 80), (x + 80, y - 80), (x + 80, y)], fill=BLACK, width=3)
    arrow_head(d, (x + 80, y - 80), (x + 80, y), size=24, open_=False)
    label(d, (x, y - 110), "pausar / reanudar")
    # En curso -> Completada
    arrow([R("EN_CURSO"), L("COMPLETADA")], "detener · declarar fin", (1490, 270), size=19)
    label(d, (1490, 332), "cambiar de sesión · plazo vencido", size=19)
    # Pendiente -> Completada (mismo día)
    arrow([T("PENDIENTE", -80), (S["PENDIENTE"][0] - 80, 110), (S["COMPLETADA"][0], 110), T("COMPLETADA")],
          "marcar completada [mismo día]", (1130, 80))
    # Pendiente -> Reprogramada
    arrow([(S["PENDIENTE"][0] + bw / 2, 620), (S["REPROGRAMADA"][0] - bw / 2, 620)], "reprogramar",
          (780, 592))
    # Reprogramada ..> nueva Pendiente
    arrow([(S["REPROGRAMADA"][0] - bw / 2, 670), (S["PENDIENTE"][0] + bw / 2, 670)],
          "«crea nueva sesión»", (780, 702), dashed=True, size=20)
    # Pendiente -> No realizada
    arrow([B("PENDIENTE"), (S["PENDIENTE"][0], 1040), L("NO_REALIZADA")], "fin del día [automático]",
          (690, 1010))
    # No realizada -> Reprogramada
    arrow([T("NO_REALIZADA"), B("REPROGRAMADA")], "reprogramar", (1235, 840))
    # No realizada -> Completada tardía
    arrow([R("NO_REALIZADA"), L("COMPLETADA_TARDIA")], "registrar tardío [≤ 3 días]", (1490, 1010))
    # Registro de sesión pasada
    d.ellipse([2220 - 20, 1250 - 20, 2220 + 20, 1250 + 20], fill=BLACK)
    arrow([(2200, 1250), (S["COMPLETADA_TARDIA"][0] + 120, 1250), B("COMPLETADA_TARDIA", 120)],
          "registrar sesión pasada [≤ 3 días]", (2040, 1290), size=20)
    # finales
    final((2190, 300))
    arrow([R("COMPLETADA"), (2166, 300)])
    final((1560, 760))
    d.line([(S["REPROGRAMADA"][0] + bw / 2, 640), (1560, 640), (1560, 736)], fill=BLACK, width=3)
    arrow_head(d, (1560, 640), (1560, 736), size=24, open_=False)
    final((2190, 1040))
    arrow([R("COMPLETADA_TARDIA"), (2166, 1040)])

    for k, (cx, cy) in S.items():
        fill = LIGHT if k in ("COMPLETADA", "COMPLETADA_TARDIA", "REPROGRAMADA") else WHITE
        d.rounded_rectangle([cx - bw / 2 + 8, cy - bh / 2 + 8, cx + bw / 2 + 8, cy + bh / 2 + 8], 40, fill=MID)
        d.rounded_rectangle([cx - bw / 2, cy - bh / 2, cx + bw / 2, cy + bh / 2], 40, fill=fill, outline=BLACK,
                            width=4)
        lines = names[k].split("\n")
        for i, ln in enumerate(lines):
            d.text((cx, cy + (i - (len(lines) - 1) / 2) * 38), ln, font=font(30 if i == 0 else 24, i == 0),
                   fill=BLACK, anchor="mm")
    d.text((40, H - 40), "Relleno gris = estado final · [condición] · «crea nueva sesión»: la nueva sesión "
           "queda enlazada a la original (RN-09)", font=font(22, italic=True), fill=GRAY, anchor="ld")
    img.save(OUT / "maquina_estados.png", dpi=(200, 200))


# ---------------------------------------------------------------- flujo entre pantallas
def flujo_pantallas():
    """Diagrama de estados de la navegación. Cada estado es una pantalla del producto y cada flecha una acción del
    usuario. Parte de la primera apertura y cierra el ciclo en «Borrar mis datos». Las pantallas se derivan de los
    objetivos (OG y OE2 a OE6) y de las HU; los wireframes W1-W15 son solo referencia."""
    W, H = 3600, 2450
    img = Image.new("RGB", (W, H), WHITE)
    d = ImageDraw.Draw(img)
    BW = 540
    B = {}

    def caja(clave, cx, cy, titulo, sub, lineas, oe, fill=WHITE):
        h = 24 + 40 + 32 + len(lineas) * 31 + 18
        B[clave] = (cx, cy, BW, h)
        x0, y0, x1, y1 = cx - BW / 2, cy - h / 2, cx + BW / 2, cy + h / 2
        d.rounded_rectangle([x0 + 8, y0 + 8, x1 + 8, y1 + 8], 34, fill=MID)
        d.rounded_rectangle([x0, y0, x1, y1], 34, fill=fill, outline=BLACK, width=4)
        y = y0 + 22
        d.text((cx, y), titulo, font=font(31, True), fill=BLACK, anchor="mt")
        d.text((cx, y + 40), sub, font=font(21, italic=True), fill=GRAY, anchor="mt")
        d.line([x0 + 30, y + 76, x1 - 30, y + 76], fill=MID, width=2)
        for i, ln in enumerate(lineas):
            d.text((x0 + 28, y + 92 + i * 31), ln, font=font(23), fill=BLACK, anchor="lt")
        tw = text_w(d, oe, font(20, True)) + 20
        d.rounded_rectangle([x1 - tw - 12, y0 + 12, x1 - 12, y0 + 46], 12, fill=BLACK)
        d.text((x1 - 12 - tw / 2, y0 + 29), oe, font=font(20, True), fill=WHITE, anchor="mm")

    def top(k, dx=0):
        cx, cy, w, h = B[k]
        return (cx + dx, cy - h / 2)

    def bot(k, dx=0):
        cx, cy, w, h = B[k]
        return (cx + dx, cy + h / 2)

    def izq(k, dy=0):
        cx, cy, w, h = B[k]
        return (cx - w / 2, cy + dy)

    def der(k, dy=0):
        cx, cy, w, h = B[k]
        return (cx + w / 2, cy + dy)

    def flecha(pts, discontinua=False, doble=False):
        pts = [(float(x), float(y)) for x, y in pts]
        if discontinua:
            dashed_line(d, pts, width=4)
        else:
            d.line(pts, fill=BLACK, width=4, joint="curve")
        arrow_head(d, pts[-2], pts[-1], size=26)
        if doble:
            arrow_head(d, pts[1], pts[0], size=26)

    def rotulo(x, y, texto, ancla="mm", negrita=False):
        lineas = texto.split("\n")
        f = font(22, negrita)
        w = max(text_w(d, s, f) for s in lineas) + 16
        h = len(lineas) * 28 + 6
        x0 = x - w / 2 if ancla == "mm" else (x if ancla == "lm" else x - w)
        d.rectangle([x0, y - h / 2, x0 + w, y + h / 2], fill=WHITE)
        for i, s in enumerate(lineas):
            d.text((x0 + 8, y - h / 2 + 3 + i * 28), s, font=f, fill=BLACK, anchor="lt")

    C0, C1, C2, C3, C4 = 520, 1140, 1760, 2380, 3000
    Y1, Y2, Y3, Y4 = 250, 720, 1400, 2060

    # grupo de navegación inferior: el usuario pasa libremente entre las 4 pestañas
    gx0, gy0, gx1, gy1 = 845, 520, 3300, 905
    d.rounded_rectangle([gx0, gy0, gx1, gy1], 40, fill=(246, 246, 246), outline=GRAY, width=3)
    d.text((gx0 + 24, gy1 - 14), "Navegación inferior, siempre visible (hoy solo Inicio y Materias están habilitadas)",
           font=font(22, italic=True), fill=GRAY, anchor="lb")

    caja("perfil", C0, Y1, "Perfil inicial", "Primer uso · HU-01",
         ["Pide nombre y meta semanal", "Nombre vacío: «Ingresa tu nombre»", "También: restaurar copia (HU-37)"],
         "OG", LIGHT)
    caja("auth", C0, Y2, "Autenticación", "HU-04",
         ["Huella o PIN del teléfono", "Solo si el bloqueo está activo", "Se pide al volver a primer plano"],
         "OE6", LIGHT)
    caja("inicio", C1, Y2 + 20, "Inicio", "HU-02, HU-25, HU-27",
         ["Horas realizadas / meta semanal", "Próxima sesión y sesiones de hoy", "Acceso rápido «+ Nueva sesión»"],
         "OG", LIGHT)
    caja("cal", C2, Y2 + 20, "Calendario", "HU-16 a HU-21",
         ["Vistas Día · Semana · Mes", "Mes: tocar un día abre el Día", "Semana y Día: crear sesión"],
         "OE3")
    caja("est", C3, Y2 + 20, "Estadísticas", "HU-29 a HU-31, HU-45",
         ["Horas y comparación semanal", "Cumplimiento y distribución", "Concentración y respeto del tiempo"],
         "OE5")
    caja("mat", C4, Y2 + 20, "Materias", "HU-06, HU-08, HU-09, HU-46",
         ["Lista alfabética con horas", "«Archivadas (n)»: tocar despliega", "Botón «+» nueva materia"],
         "OE2", LIGHT)

    caja("borrar", 1280, Y1, "Borrar mis datos", "HU-38",
         ["Elegir: teléfono, Drive o ambos", "Doble confirmación (RN-21)", "Cancelar no borra nada"], "OE6", LIGHT)
    caja("ajustes", 2000, Y1, "Ajustes", "HU-03, HU-04, HU-23, HU-24",
         ["Tema e idioma", "Bloqueo con huella o PIN", "Pomodoro, No molestar y Privacidad"], "OG", LIGHT)
    caja("resp", 2760, Y1, "Copia de seguridad", "HU-35 a HU-37",
         ["Cuenta Google y frecuencia", "Cifrado con contraseña (opcional)", "Respaldar ahora · Restaurar"], "OE6")

    caja("det", C0, Y3, "Detalle de sesión", "HU-14, HU-39",
         ["Línea de tiempo de eventos", "Salidas, regresos y tiempo extra", "Enlace a la sesión reprogramada"],
         "OE4")
    caja("reg", C1, Y3, "Registrar sesión", "HU-10, HU-11, HU-34",
         ["Materia, fecha, horas y aviso", "Fin > inicio; sin cruces (RN-04, 05)", "Fecha pasada ≤ 3 días: tardío"],
         "OE2")
    caja("ses", C2, Y3, "Sesión en curso", "HU-22 a HU-24 · estados RN-07",
         ["Cronómetro libre o Pomodoro", "Pausar / reanudar · Detener", "No molestar (opcional)"], "OE4")
    caja("rep", C3, Y3, "Reporte", "HU-32, HU-33",
         ["Plan vs. real por materia", "Sesiones no realizadas", "Compartir PDF o CSV"], "OE5")
    caja("fmat", C4, Y3, "Formulario de materia", "HU-06 a HU-08, HU-26, HU-46",
         ["Nombre, color y docente (meta: E6)", "Nombre duplicado: se rechaza", "Archivar o desarchivar · Eliminar"],
         "OE2", LIGHT)

    caja("volver", 1250, Y4, "Al volver a la app", "HU-40, HU-41",
         ["«¿Seguiste estudiando?»", "Sí, hasta HH:MM → Completada", "Sigo estudiando → continúa"], "OE4")
    caja("repartir", 2270, Y4, "Decidir al fin del tiempo", "HU-42 a HU-44",
         ["Fin planificado: Sigo / Terminé", "Hora de otra sesión: cambiar", "Repartir el tramo → Completadas"],
         "OE4")
    caja("meta", 3000, Y4, "Meta alcanzada", "HU-28",
         ["Mensaje «¡Sigue así!»", "Una sola vez por semana (RN-16)", "Botón «Volver al inicio»"], "OE5")

    # entrada: primera apertura y aperturas siguientes
    dot = (90, 485)
    d.ellipse([dot[0] - 20, dot[1] - 20, dot[0] + 20, dot[1] + 20], fill=BLACK)
    flecha([dot, (90, Y1), izq("perfil")])
    flecha([dot, (90, Y2), izq("auth")])
    rotulo(170, 380, "primera\napertura")
    rotulo(170, 600, "siguientes\naperturas")
    flecha([bot("perfil", 120), (640, 485), (940, 485), (940, top("inicio")[1])])
    rotulo(790, 462, "Confirmar", "mm", True)
    flecha([der("auth"), izq("inicio", -20)])
    rotulo(818, 676, "ok")
    d.text((112, 522), "Sin bloqueo activo: directo a Inicio.", font=font(20, italic=True), fill=GRAY, anchor="lt")

    # ciclo de vida de los datos: Ajustes > Privacidad > Borrar > primer uso
    flecha([izq("ajustes"), der("borrar")])
    rotulo(1630, Y1 - 36, "Privacidad", "mm")
    flecha([izq("borrar"), der("perfil")])
    rotulo(900, Y1 - 52, "vuelve al\nprimer uso", "mm", True)
    flecha([der("ajustes"), izq("resp")], discontinua=True)
    rotulo(2380, Y1 - 36, "copia")
    flecha([(1300, top("inicio")[1]), (1300, 470), (1900, 470), (1900, bot("ajustes")[1])], discontinua=True)
    rotulo(1600, 447, "Ajustes (a confirmar cómo se llega)")

    # pantallas hijas
    flecha([(C1, gy1), top("reg")])
    rotulo(C1 + 14, 1000, "«+ Nueva sesión» (Inicio) o\ncrear desde Semana o Día (HU-20)", "lm")
    flecha([(C2 + 70, gy1), top("ses", 70)])
    rotulo(C2 + 90, 1010, "Iniciar sesión: desde la\nlista o la notificación de\nrecordatorio (HU-34)", "lm")
    flecha([top("ses", -70), (C2 - 70, gy1)])
    rotulo(C2 - 90, 1130, "Detener sin tramo\ncontinuo: Completada\ny vuelve a la pestaña", "rm")
    flecha([(C3, gy1), top("rep")], discontinua=True)
    rotulo(C3 + 14, 1050, "«Ver reporte»\n(a confirmar)", "lm")
    flecha([(C4, gy1), top("fmat")])
    rotulo(C4 + 14, 1050, "«+» o tocar\nuna materia", "lm")
    flecha([(950, gy1), (950, 1010), (C0, 1010), top("det")], discontinua=True)
    rotulo(740, 988, "abrir una sesión terminada (a confirmar)")

    # la sesión en curso: salir de la app, decisiones de fin y meta alcanzada
    flecha([bot("ses", -110), (C2 - 110, 1810), (1250, 1810), top("volver")], doble=True)
    rotulo(1480, 1783, "sale de la app (HU-40) ↔\nvuelve (Sigo estudiando)")
    flecha([bot("ses", 110), (C2 + 110, 1810), (2270, 1810), top("repartir")], doble=True)
    rotulo(2075, 1852, "fin planificado, otra sesión\no Detener con tramo ↔ Seguir")
    flecha([bot("ses", 200), (C2 + 200, 1700), (3000, 1700), top("meta")])
    rotulo(2480, 1673, "Completada alcanza la meta semanal", "mm")
    flecha([der("meta"), (3470, Y4), (3470, 810), (gx1, 810)])
    rotulo(3470, 1500, "«Volver al inicio»:\nabre la pestaña\nInicio", "mm", True)

    # leyenda
    d.text((60, H - 140), "Cada caja es una pantalla; cada flecha, una acción del usuario. Se derivan de los objetivos y de las "
           "historias de usuario (los wireframes W1 a W15 son solo referencia).", font=font(23, italic=True),
           fill=GRAY, anchor="ld")
    d.text((60, H - 105), "Línea continua: aparece en las specs. Discontinua: deducida, por confirmar con el Product Owner. "
           "Flecha doble: ida y vuelta. ● inicio. Fondo gris: pantalla ya implementada (una Activity por pantalla).",
           font=font(23, italic=True), fill=GRAY, anchor="ld")
    d.text((60, H - 70), "OG objetivo general · OE2 materias y sesiones · OE3 calendario · OE4 concentración · "
           "OE5 metas, estadísticas y reportes · OE6 protección de datos (OE1 y OE7 son de proceso: sin pantallas).",
           font=font(23, italic=True), fill=GRAY, anchor="ld")
    img.save(OUT / "flujo_pantallas.png", dpi=(200, 200))


# ---------------------------------------------------------------- arquitectura hexagonal
def arquitectura_hexagonal():
    """Arquitectura hexagonal (puertos y adaptadores) de StudyPlan en tres capas. Las dependencias apuntan hacia el
    núcleo: ni el dominio ni la aplicación importan android.*."""
    W, H = 3560, 1500
    img = Image.new("RGB", (W, H), WHITE)
    d = ImageDraw.Draw(img)

    def caja(x0, y0, x1, y1, titulo, lineas, fill=WHITE, tam=29):
        d.rounded_rectangle([x0 + 7, y0 + 7, x1 + 7, y1 + 7], 28, fill=MID)
        d.rounded_rectangle([x0, y0, x1, y1], 28, fill=fill, outline=BLACK, width=4)
        d.text(((x0 + x1) / 2, y0 + 18), titulo, font=font(tam, True), fill=BLACK, anchor="mt")
        for i, ln in enumerate(lineas):
            d.text((x0 + 26, y0 + 18 + tam + 20 + i * 31), ln, font=font(23), fill=BLACK, anchor="lt")

    def flecha(pts, discontinua=False):
        pts = [(float(x), float(y)) for x, y in pts]
        if discontinua:
            dashed_line(d, pts, width=4)
        else:
            d.line(pts, fill=BLACK, width=4)
        arrow_head(d, pts[-2], pts[-1], size=26)

    LA0, LA1 = 40, 680          # adaptadores de entrada
    LP0, LP1 = 740, 1120        # puertos de entrada
    RP0, RP1 = 2560, 2940       # puertos de salida
    RA0, RA1 = 3000, 3520       # adaptadores de salida
    cx, hw = 1840, 650          # hexágono del núcleo (punta izquierda 1190, derecha 2490)

    for x0, x1, t in ((LA0, LA1, "CAPA 1 · Presentación (entrada)"),
                      (LP0 - 20, RP1 + 20, "CAPA 2 · Núcleo de la aplicación (puertos, aplicación y dominio)"),
                      (RA0, RA1, "CAPA 3 · Infraestructura (salida)")):
        d.line([x0, 118, x1, 118], fill=BLACK, width=5)
        d.text(((x0 + x1) / 2, 84), t, font=font(28, True), fill=BLACK, anchor="mm")

    ent = [
        ("Activity / Fragment", ["onCreate, onStart, onResume…", "onClick y listeners con nombre", "Diálogo BiometricPrompt (bloqueo)",
                              "Solo delegan: sin lógica de negocio"]),
        ("ViewModel + LiveData", ["Estado de la pantalla", "Llama a los casos de uso", "Sin Context ni Views"]),
        ("Layouts XML (Material 3)", ["activity_*.xml · fragment_*.xml", "Enlazados con su Activity por", "setContentView y los ids de las vistas"]),
        ("Disparadores del sistema", ["Widget · notificaciones", "WorkManager y AlarmManager"]),
    ]
    sal = [
        ("Persistencia: Room (SQLite)", ["Entity, DAO y mapper a dominio", "Offline-first · fuera del hilo principal"]),
        ("Respaldo: Google Drive", ["appDataFolder · cifrado AES-256-GCM", "Único uso de INTERNET"]),
        ("Seguridad del dispositivo", ["Disponibilidad de huella o PIN", "BiometricManager · Android Keystore"]),
        ("Avisos y reloj del sistema", ["Notificaciones · AlarmManager", "WorkManager · hora del dispositivo"]),
    ]
    Y0, sep = 200, 55
    y = Y0
    ys_ent = []
    for t, ls in ent:
        h = 18 + 29 + 20 + len(ls) * 31 + 22
        caja(LA0, y, LA1, y + h, t, ls)
        ys_ent.append(y + h / 2)
        y += h + sep
    y_fin = y - sep
    y = Y0
    ys_sal = []
    for t, ls in sal:
        h = 18 + 29 + 20 + len(ls) * 31 + 22
        caja(RA0, y, RA1, y + h, t, ls)
        ys_sal.append(y + h / 2)
        y += h + sep

    caja(LP0, Y0, LP1, y_fin, "Puertos de entrada",
         ["Interfaces de los casos de", "uso. Los adaptadores los", "llaman.", "", "ConfigurarPerfil", "ConfigurarBloqueo",
          "PlanificarSesión", "CompletarSesión", "ObtenerEstadísticas", "…"], fill=LIGHT)
    caja(RP0, Y0, RP1, y_fin, "Puertos de salida",
         ["Interfaces que exige el", "núcleo. Los adaptadores", "las implementan.", "", "PerfilRepository", "SesionRepository",
          "RespaldoRemoto", "DisponibilidadBloqueo", "Notificador", "…"], fill=LIGHT)

    cy = (Y0 + y_fin) / 2
    hh = 480
    hexa = [(cx - hw, cy), (cx - hw / 2, cy - hh), (cx + hw / 2, cy - hh), (cx + hw, cy), (cx + hw / 2, cy + hh),
            (cx - hw / 2, cy + hh)]
    d.polygon([(x + 8, y + 8) for x, y in hexa], fill=MID)
    d.polygon(hexa, fill=(247, 247, 247), outline=BLACK, width=5)
    caja(cx - 320, cy - 420, cx + 320, cy - 170, "Aplicación",
         ["Servicios que implementan los casos", "de uso y orquestan el dominio a", "través de los puertos"], tam=30)
    caja(cx - 320, cy - 120, cx + 320, cy + 400, "Dominio (Java puro)",
         ["Perfil · Materia · SesiónEstudio", "MetaSemanal · Ajustes", "Máquina de estados de la sesión (RN-07)",
          "Reglas de negocio RN-xx", "Sin android.*: se prueba con JUnit"], tam=30)
    d.text((cx, cy + hh - 28), "inversión de dependencias (la D de SOLID)", font=font(22, italic=True), fill=GRAY,
           anchor="mm")

    # las dependencias apuntan hacia el núcleo
    for ye in ys_ent:
        flecha([(LA1, ye), (LP0, ye)])
    flecha([(LP1, cy), (cx - hw, cy)])
    flecha([(cx + hw, cy), (RP0, cy)])
    for ys in ys_sal:
        flecha([(RA0, ys), (RP1, ys)], discontinua=True)

    # franja inferior
    yb = y_fin + 90
    d.line([40, yb, W - 40, yb], fill=MID, width=3)
    d.text((60, yb + 30), "Paquetes (io.github.jhoel0521.studyplan):", font=font(26, True), fill=BLACK, anchor="lt")
    d.text((60, yb + 75), "adapter.in.ui  →  application.port.in  ·  application.service  ·  domain  ·  application.port.out  ←  "
           "adapter.out.persistence · adapter.out.drive · adapter.out.security", font=font(25), fill=BLACK, anchor="lt")
    notas = ["Regla de dependencia: las flechas continuas apuntan hacia el núcleo. Línea discontinua: el adaptador implementa un "
             "puerto de salida.",
             "MVVM sigue vigente dentro de la capa 1: Activity o Fragment y ViewModel son el adaptador de entrada.",
             "Los nombres de puertos son ejemplos del dominio; los definitivos se fijan en el diseño de cada change."]
    for i, n in enumerate(notas):
        d.text((60, yb + 135 + i * 38), n, font=font(23, italic=True), fill=GRAY, anchor="lt")
    img.save(OUT / "arquitectura_hexagonal.png", dpi=(200, 200))


# ---------------------------------------------------------------- ciclo Scrum + SDD
def ciclo_scrum_sdd():
    W, H = 2120, 1440
    img = Image.new("RGB", (W, H), WHITE)
    d = ImageDraw.Draw(img)
    cx, cy, rx, ry = W / 2, H / 2 + 10, 800, 500
    pasos = [
        ("1. Product Backlog", "Historias HU-xx priorizadas", "H"),
        ("2. Sprint Planning", "Selección de HU del sprint", "H"),
        ("3. /opsx:propose", "proposal · specs · design · tasks", "IA"),
        ("4. Revisión de specs", "Aprobación humana del change", "H"),
        ("5. /opsx:apply", "La IA genera código Java + pruebas", "IA"),
        ("6. Revisión y pruebas", "revisor-java · JUnit · Lint", "H+IA"),
        ("7. Sprint Review / Retro", "Demo al Product Owner", "H"),
        ("8. /opsx:archive", "Specs → fuente de verdad", "IA"),
    ]
    n = len(pasos)
    bw, bh = 440, 140
    centers = []
    for i in range(n):
        ang = -math.pi / 2 + 2 * math.pi * i / n
        centers.append((cx + rx * math.cos(ang), cy + ry * math.sin(ang)))

    # flechas
    for i in range(n):
        a, b = centers[i], centers[(i + 1) % n]
        ang = math.atan2(b[1] - a[1], b[0] - a[0])

        def edge(p, sign):
            # punto aproximado en el borde del rectángulo
            dx, dy = math.cos(ang) * sign, math.sin(ang) * sign
            t = min((bw / 2) / abs(dx) if dx else 1e9, (bh / 2) / abs(dy) if dy else 1e9)
            return (p[0] + dx * (t + 12), p[1] + dy * (t + 12))
        p1, p2 = edge(a, 1), edge(b, -1)
        d.line([p1, p2], fill=BLACK, width=4)
        arrow_head(d, p1, p2, size=26, open_=False)

    for (t, s, who), (x, y) in zip(pasos, centers):
        fill = LIGHT if "IA" == who else WHITE
        if who == "H+IA":
            fill = (240, 240, 240)
        d.rounded_rectangle([x - bw / 2 + 8, y - bh / 2 + 8, x + bw / 2 + 8, y + bh / 2 + 8], 22, fill=MID)
        d.rounded_rectangle([x - bw / 2, y - bh / 2, x + bw / 2, y + bh / 2], 22, fill=fill, outline=BLACK,
                            width=4 if "H" in who else 2)
        d.text((x, y - 28), t, font=font(32, True), fill=BLACK, anchor="mm")
        d.text((x, y + 18), s, font=font(23), fill=BLACK, anchor="mm")
        tag = {"H": "Humano", "IA": "IA", "H+IA": "Humano + IA"}[who]
        d.text((x + bw / 2 - 16, y + bh / 2 - 14), tag, font=font(19, italic=True), fill=GRAY, anchor="rb")

    # centro: bitácora
    d.ellipse([cx - 260, cy - 120, cx + 260, cy + 120], fill=WHITE, outline=BLACK, width=3)
    d.text((cx, cy - 34), "Auditoría IA", font=font(34, True), fill=BLACK, anchor="mm")
    d.text((cx, cy + 10), "docs/auditoria-ia/bitacora.md", font=font(22), fill=BLACK, anchor="mm")
    d.text((cx, cy + 46), "registra cada paso", font=font(22, italic=True), fill=GRAY, anchor="mm")
    for i in (2, 4, 5, 7):
        x, y = centers[i]
        ang = math.atan2(y - cy, x - cx)
        dx, dy = math.cos(ang), math.sin(ang)
        # salida en el borde de la elipse y llegada al borde de la caja
        t0 = 1 / math.sqrt((dx / 260) ** 2 + (dy / 120) ** 2)
        tb = min((bw / 2) / abs(dx) if dx else 1e9, (bh / 2) / abs(dy) if dy else 1e9)
        L = math.hypot(x - cx, y - cy) - tb - 10
        dashed_line(d, [(cx + dx * (t0 + 8), cy + dy * (t0 + 8)), (cx + dx * L, cy + dy * L)],
                    fill=GRAY, width=2)
    d.text((40, H - 40), "Borde grueso = decisión humana · Relleno gris = artefacto generado por IA",
           font=font(22, italic=True), fill=GRAY, anchor="ld")
    d.text((40, 40), "Sprint de 1 semana", font=font(26, True), fill=GRAY)
    img.save(OUT / "ciclo_scrum_sdd.png", dpi=(200, 200))


# ---------------------------------------------------------------- wireframes baja fidelidad
PW, PH = 520, 1040


class Phone:
    def __init__(self, d, x, y, titulo, back=False):
        self.d, self.x, self.y = d, x, y
        d.rounded_rectangle([x, y, x + PW, y + PH], 48, fill=WHITE, outline=BLACK, width=5)
        d.rounded_rectangle([x + PW / 2 - 70, y + 14, x + PW / 2 + 70, y + 34], 10, fill=MID)
        d.text((x + 40, y + 52), "9:41", font=font(20), fill=GRAY)
        d.text((x + PW - 40, y + 50), "▮▮ ◔", font=sym(20), fill=GRAY, anchor="ra")
        hy = y + 90
        if back:
            d.text((x + 34, hy), "←", font=font(34, True), fill=BLACK)
        d.text((x + (90 if back else 34), hy + 2), titulo, font=font(32, True), fill=BLACK)
        self.cy = hy + 70

    def l(self, dx):
        return self.x + 30 + dx

    def text(self, s, size=22, bold=False, dx=0, color=BLACK):
        self.d.text((self.l(dx), self.cy), s, font=font(size, bold), fill=color)
        self.cy += size + 14

    def lines(self, n=2, w=0.7):
        for i in range(n):
            ww = (PW - 60) * (w if i == 0 else w * 0.6)
            self.d.rectangle([self.l(0), self.cy, self.l(0) + ww, self.cy + 12], fill=MID)
            self.cy += 24

    def box(self, h, label_=None, fill=WHITE, radius=14, inner=None):
        d = self.d
        d.rounded_rectangle([self.l(0), self.cy, self.x + PW - 30, self.cy + h], radius, fill=fill,
                            outline=BLACK, width=2)
        top = self.cy
        if label_:
            d.text((self.l(16), self.cy + 14), label_, font=font(22, True), fill=BLACK)
        if inner:
            inner(top)
        self.cy += h + 18
        return top

    def field(self, lbl, val, icon=None):
        self.d.text((self.l(0), self.cy), lbl, font=font(19), fill=GRAY)
        self.cy += 26
        top = self.box(56)
        if icon:
            self.d.text((self.l(16), top + 12), icon, font=sym(26), fill=BLACK)
        self.d.text((self.l(56), top + 16), val, font=font(22), fill=BLACK)
        self.d.text((self.x + PW - 56, top + 12), "▾", font=sym(26), fill=BLACK)

    def button(self, s, filled=True):
        d = self.d
        d.rounded_rectangle([self.l(0), self.cy, self.x + PW - 30, self.cy + 62], 31,
                            fill=(70, 70, 70) if filled else WHITE, outline=BLACK, width=2)
        d.text((self.x + PW / 2, self.cy + 31), s, font=font(24, True), fill=WHITE if filled else BLACK,
               anchor="mm")
        self.cy += 80

    def progress(self, frac, dx=16, top=None):
        y = top if top is not None else self.cy
        x0, x1 = self.l(dx), self.x + PW - 30 - dx
        self.d.rounded_rectangle([x0, y, x1, y + 14], 7, fill=LIGHT, outline=GRAY)
        self.d.rounded_rectangle([x0, y, x0 + (x1 - x0) * frac, y + 14], 7, fill=(90, 90, 90))

    def list_item(self, title, sub, mark="○", right=None):
        def inner(top):
            self.d.text((self.l(18), top + 18), mark, font=sym(30), fill=BLACK)
            self.d.text((self.l(70), top + 14), title, font=font(22, True), fill=BLACK)
            self.d.text((self.l(70), top + 44), sub, font=font(19), fill=GRAY)
            if right:
                self.d.rounded_rectangle([self.x + PW - 200, top + 24, self.x + PW - 46, top + 58], 17,
                                         outline=BLACK, width=2)
                self.d.text((self.x + PW - 123, top + 41), right, font=font(17), fill=BLACK, anchor="mm")
        self.box(80, inner=inner)

    def nav(self, active):
        d = self.d
        y = self.y + PH - 110
        d.line([self.x + 10, y, self.x + PW - 10, y], fill=GRAY, width=2)
        items = ["Inicio", "Calendario", "Estadíst.", "Materias"]
        for i, it in enumerate(items):
            cx = self.x + PW * (i + 0.5) / 4
            fill = (70, 70, 70) if it == active else WHITE
            d.rounded_rectangle([cx - 18, y + 18, cx + 18, y + 54], 6, fill=fill, outline=BLACK, width=2)
            d.text((cx, y + 72), it, font=font(17, it == active), fill=BLACK, anchor="ma")


def caption(d, x, y, s, hu):
    d.text((x + PW / 2, y + PH + 30), s, font=font(28, True), fill=BLACK, anchor="ma")
    d.text((x + PW / 2, y + PH + 70), hu, font=font(22, italic=True), fill=GRAY, anchor="ma")


def wf_inicio(d, x, y):
    p = Phone(d, x, y, "¡Hola, [Nombre]!")
    d.ellipse([x + PW - 100, y + 84, x + PW - 40, y + 144], outline=BLACK, width=2)
    d.line([x + PW - 100, y + 84, x + PW - 40, y + 144], fill=MID, width=2)
    d.line([x + PW - 40, y + 84, x + PW - 100, y + 144], fill=MID, width=2)
    top = p.box(130, "Meta de esta semana")
    d.text((p.l(16), top + 50), "12 / 20 horas", font=font(30, True), fill=BLACK)
    p.progress(0.6, top=top + 100)
    p.text("Próxima sesión", 22, True)
    p.list_item("[Materia]", "Hoy · 10:00 – 11:30", mark="▢")
    p.button("+ Nueva sesión")
    p.text("Sesiones de hoy", 22, True)
    p.list_item("[Materia A]", "08:00 – 09:00", mark="✓")
    p.list_item("[Materia B]", "10:00 – 11:30", mark="○")
    p.nav("Inicio")


def wf_materias(d, x, y):
    p = Phone(d, x, y, "Materias")
    for nombre, h in [("[Materia A]", "6,5 h"), ("[Materia B]", "4,0 h"), ("[Materia C]", "3,0 h"),
                      ("[Materia D]", "1,5 h")]:
        def inner(top, nombre=nombre, h=h):
            d.rectangle([p.l(18), top + 22, p.l(54), top + 58], fill=MID, outline=BLACK)
            d.text((p.l(72), top + 18), nombre, font=font(22, True), fill=BLACK)
            d.text((p.l(72), top + 48), "[Docente]", font=font(18), fill=GRAY)
            d.text((x + PW - 60, top + 28), h, font=font(22, True), fill=BLACK, anchor="ra")
        p.box(84, inner=inner)
    p.text("Archivadas (2)", 20, color=GRAY)
    d.text((p.l(150), p.cy - 36), "▾", font=sym(22), fill=GRAY)
    d.ellipse([x + PW - 130, y + PH - 230, x + PW - 40, y + PH - 140], fill=(70, 70, 70), outline=BLACK)
    d.text((x + PW - 85, y + PH - 185), "+", font=font(48, True), fill=WHITE, anchor="mm")
    p.nav("Materias")


def wf_registrar(d, x, y):
    p = Phone(d, x, y, "Registrar sesión", back=True)
    p.field("Materia", "[Materia]", "▢")
    p.field("Fecha", "Hoy, dd/mm/aaaa", "▦")
    p.field("Hora de inicio", "10:00", "◷")
    p.field("Hora de fin", "11:30", "◷")
    p.field("Recordatorio", "15 min antes", "◔")
    p.d.text((p.l(0), p.cy - 8), "Fecha pasada (≤ 3 días) → registro tardío", font=font(17, italic=True),
             fill=GRAY)
    p.cy += 26
    p.d.text((p.l(0), p.cy), "Notas (opcional)", font=font(19), fill=GRAY)
    p.cy += 26
    top = p.box(84)
    d.rectangle([p.l(16), top + 20, p.l(260), top + 32], fill=MID)
    p.button("Guardar")


def wf_calendario(d, x, y):
    p = Phone(d, x, y, "Calendario")
    d.text((p.l(0), p.cy), "‹", font=font(34, True), fill=BLACK)
    d.text((x + PW / 2, p.cy + 4), "[Mes Año]", font=font(24, True), fill=BLACK, anchor="ma")
    d.text((x + PW - 40, p.cy), "›", font=font(34, True), fill=BLACK, anchor="ra")
    p.cy += 56
    cw = (PW - 60) / 7
    for i, s in enumerate("LMXJVSD"):
        d.text((p.l(0) + cw * (i + 0.5), p.cy), s, font=font(19), fill=GRAY, anchor="ma")
    p.cy += 38
    day = 1
    for r in range(5):
        for c in range(7):
            if (r == 0 and c < 1) or day > 30:
                continue
            cx, cy = p.l(0) + cw * (c + 0.5), p.cy + r * 58
            if day == 21:
                d.ellipse([cx - 24, cy - 6, cx + 24, cy + 42], fill=(70, 70, 70))
            d.text((cx, cy + 4), str(day), font=font(20, day == 21), fill=WHITE if day == 21 else BLACK,
                   anchor="ma")
            if day in (3, 8, 10, 15, 17, 22, 24):
                d.ellipse([cx - 4, cy + 32, cx + 4, cy + 40], fill=BLACK)
            day += 1
        if r == 4:
            p.cy += 5 * 58 + 10
    p.text("Sesiones del día", 22, True)
    p.list_item("[Materia A]", "08:00 – 09:00", "✓", "Completada")
    p.list_item("[Materia B]", "10:00 – 11:30", "○", "Pendiente")
    p.nav("Calendario")


def wf_estadisticas(d, x, y):
    p = Phone(d, x, y, "Estadísticas")
    w = (PW - 60) / 2
    for i, s in enumerate(["Semana", "Mes"]):
        d.rounded_rectangle([p.l(0) + i * w, p.cy, p.l(0) + (i + 1) * w, p.cy + 46], 23,
                            fill=(70, 70, 70) if i == 0 else WHITE, outline=BLACK, width=2)
        d.text((p.l(0) + (i + 0.5) * w, p.cy + 23), s, font=font(20, True), fill=WHITE if i == 0 else BLACK,
               anchor="mm")
    p.cy += 66
    top = p.box(120, "Total de horas")
    d.text((p.l(16), top + 50), "15 h", font=font(32, True), fill=BLACK)
    d.text((p.l(120), top + 60), "↑ 25 % vs. semana anterior", font=font(18), fill=GRAY)
    top = p.box(100, "Meta semanal  12 / 20 h")
    p.progress(0.6, top=top + 62)
    p.text("Distribución por materia", 22, True)
    cx, cy, r = p.l(110), p.cy + 115, 100
    angs = [0, 144, 216, 288, 324, 360]
    shades = [(60, 60, 60), (110, 110, 110), (160, 160, 160), (200, 200, 200), (230, 230, 230)]
    for a0, a1, s in zip(angs, angs[1:], shades):
        d.pieslice([cx - r, cy - r, cx + r, cy + r], a0 - 90, a1 - 90, fill=s, outline=WHITE, width=2)
    d.ellipse([cx - 52, cy - 52, cx + 52, cy + 52], fill=WHITE)
    for i, (s, pc) in enumerate(zip(["[Mat. A]", "[Mat. B]", "[Mat. C]", "[Mat. D]", "Otras"],
                                    ["40%", "20%", "20%", "10%", "10%"])):
        ly = cy - 95 + i * 40
        d.rectangle([cx + 130, ly, cx + 150, ly + 20], fill=shades[i], outline=BLACK)
        d.text((cx + 162, ly - 2), s, font=font(19), fill=BLACK)
        d.text((x + PW - 40, ly - 2), pc, font=font(19), fill=BLACK, anchor="ra")
    p.cy += 250
    p.button("Ver reporte", filled=False)
    p.nav("Estadíst.")


def wf_reporte(d, x, y):
    p = Phone(d, x, y, "Reporte semanal", back=True)
    p.text("dd/mm – dd/mm/aaaa", 20, color=GRAY)
    cols = [0, 190, 290, 380]
    hdr = ["Materia", "Plan", "Real", "%"]
    for c, s in zip(cols, hdr):
        d.text((p.l(c), p.cy), s, font=font(20, True), fill=BLACK)
    p.cy += 34
    d.line([p.l(0), p.cy, x + PW - 30, p.cy], fill=BLACK, width=2)
    p.cy += 12
    rows = [("[Mat. A]", "8", "6,5", "81"), ("[Mat. B]", "5", "4,0", "80"), ("[Mat. C]", "4", "3,0", "75"),
            ("[Mat. D]", "3", "1,5", "50")]
    for row in rows:
        for c, s in zip(cols, row):
            d.text((p.l(c), p.cy), s, font=font(20), fill=BLACK)
        p.cy += 40
    d.line([p.l(0), p.cy, x + PW - 30, p.cy], fill=BLACK, width=2)
    p.cy += 12
    for c, s in zip(cols, ["Total", "20", "15", "75"]):
        d.text((p.l(c), p.cy), s, font=font(20, True), fill=BLACK)
    p.cy += 60
    top = p.box(150, "Cumplimiento general")
    p.progress(0.75, top=top + 70)
    d.text((p.l(16), top + 100), "75 % de lo planificado", font=font(19), fill=GRAY)
    p.cy += 150
    w = (PW - 60 - 20) / 2
    for i, s in enumerate(["Compartir PDF", "Compartir CSV"]):
        d.rounded_rectangle([p.l(0) + i * (w + 20), p.cy, p.l(0) + i * (w + 20) + w, p.cy + 62], 31,
                            outline=BLACK, width=2)
        d.text((p.l(0) + i * (w + 20) + w / 2, p.cy + 31), s, font=font(20, True), fill=BLACK, anchor="mm")


def wireframes(nombre, pantallas):
    gap = 90
    W = len(pantallas) * PW + (len(pantallas) + 1) * gap
    H = PH + 220
    img = Image.new("RGB", (W, H), WHITE)
    d = ImageDraw.Draw(img)
    for i, (fn, cap, hu) in enumerate(pantallas):
        x = gap + i * (PW + gap)
        fn(d, x, 40)
        caption(d, x, 40, cap, hu)
    img.save(OUT / nombre, dpi=(200, 200))


DARK = (70, 70, 70)
SHADES = [(90, 90, 90), (140, 140, 140), (185, 185, 185), (215, 215, 215)]
ICON = {"C": "✓", "P": "○", "N": "✕", "R": "↻", "T": "◷"}


def toggle(d, x, y, on):
    d.rounded_rectangle([x, y, x + 64, y + 34], 17, fill=DARK if on else WHITE, outline=BLACK, width=2)
    cx = x + 47 if on else x + 17
    d.ellipse([cx - 12, y + 5, cx + 12, y + 29], fill=WHITE, outline=BLACK, width=2)


def tabs(p, items, active, h=46):
    d = p.d
    w = (PW - 60) / len(items)
    for i, s in enumerate(items):
        d.rounded_rectangle([p.l(0) + i * w, p.cy, p.l(0) + (i + 1) * w, p.cy + h], 23,
                            fill=DARK if s == active else WHITE, outline=BLACK, width=2)
        d.text((p.l(0) + (i + 0.5) * w, p.cy + h / 2), s, font=font(20, True),
               fill=WHITE if s == active else BLACK, anchor="mm")
    p.cy += h + 16


def cal_header(p, active, rango):
    d = p.d
    tabs(p, ["Día", "Semana", "Mes"], active)
    d.text((p.l(0), p.cy), "‹", font=font(30, True), fill=BLACK)
    d.text((p.x + PW / 2, p.cy + 4), rango, font=font(22, True), fill=BLACK, anchor="ma")
    d.text((p.x + PW - 40, p.cy), "›", font=font(30, True), fill=BLACK, anchor="ra")
    p.cy += 44
    d.rounded_rectangle([p.l(0), p.cy, p.l(210), p.cy + 36], 18, outline=BLACK, width=2)
    d.text((p.l(14), p.cy + 6), "Materia: Todas", font=font(18), fill=BLACK)
    d.text((p.l(186), p.cy + 4), "▾", font=sym(20), fill=BLACK)
    d.text((p.x + PW - 32, p.cy + 8), "Plan 20 h · Real 12 h", font=font(18, True), fill=BLACK, anchor="ra")
    p.cy += 52


def wf_cal_mes(d, x, y):
    p = Phone(d, x, y, "Calendario")
    cal_header(p, "Mes", "[Mes Año]")
    cw = (PW - 60) / 7
    for i, s in enumerate("LMXJVSD"):
        d.text((p.l(0) + cw * (i + 0.5), p.cy), s, font=font(18), fill=GRAY, anchor="ma")
    p.cy += 32
    marks = {3: "C", 8: "C", 10: "N", 15: "C", 17: "R", 20: "T", 21: "P", 22: "P", 24: "P"}
    day = 1
    for r in range(5):
        for c in range(7):
            if (r == 0 and c < 1) or day > 30:
                continue
            cx, cy = p.l(0) + cw * (c + 0.5), p.cy + r * 56
            if day == 21:
                d.ellipse([cx - 22, cy - 6, cx + 22, cy + 38], fill=DARK)
            d.text((cx, cy + 2), str(day), font=font(19, day == 21), fill=WHITE if day == 21 else BLACK,
                   anchor="ma")
            if day in marks:
                d.text((cx, cy + 30), ICON[marks[day]], font=sym(15), fill=BLACK, anchor="ma")
            day += 1
    p.cy += 5 * 56 + 10
    p.text("Sesiones del 21", 21, True)
    p.list_item("[Materia A]", "08:00 – 09:00", "✓", "Completada")
    p.list_item("[Materia B]", "10:00 – 11:30", "○", "Pendiente")
    p.nav("Calendario")


def wf_cal_semana(d, x, y):
    p = Phone(d, x, y, "Calendario")
    cal_header(p, "Semana", "L 18 – D 24")
    gx = p.l(44)
    cw = (p.x + PW - 30 - gx) / 7
    for i, s in enumerate(["L18", "M19", "X20", "J21", "V22", "S23", "D24"]):
        d.text((gx + cw * (i + 0.5), p.cy), s, font=font(16, s == "J21"), fill=BLACK, anchor="ma")
    p.cy += 28
    top, rh = p.cy, 42
    for i, h in enumerate(range(8, 20)):
        yy = top + i * rh
        d.text((p.l(0), yy - 8), f"{h:02d}", font=font(15), fill=GRAY)
        d.line([gx, yy, p.x + PW - 30, yy], fill=LIGHT, width=1)
    for i in range(8):
        d.line([gx + cw * i, top, gx + cw * i, top + 12 * rh], fill=LIGHT, width=1)
    blocks = [(0, 8, 1.0, 0, "C"), (0, 15, 1.5, 1, "C"), (1, 10, 1.5, 2, "N"), (2, 9, 2.0, 0, "R"),
              (2, 14, 1.0, 3, "T"), (3, 8, 1.0, 1, "C"), (3, 10, 1.5, 0, "P"), (4, 16, 2.0, 2, "P"),
              (5, 10, 2.5, 1, "P")]
    for col, h, dur, shade, st in blocks:
        bx0, by0 = gx + cw * col + 3, top + (h - 8) * rh + 2
        d.rounded_rectangle([bx0, by0, bx0 + cw - 6, by0 + dur * rh - 4], 6, fill=SHADES[shade], outline=BLACK)
        d.text((bx0 + 4, by0 + 2), ICON[st], font=sym(16), fill=WHITE if shade < 2 else BLACK)
    nowy = top + (11.3 - 8) * rh
    d.line([gx + cw * 3, nowy, gx + cw * 4, nowy], fill=BLACK, width=3)
    d.ellipse([gx + cw * 3 - 5, nowy - 5, gx + cw * 3 + 5, nowy + 5], fill=BLACK)
    p.cy = top + 12 * rh + 8
    d.text((p.l(0), p.cy), "Tocar hueco libre = nueva sesión", font=font(17, italic=True), fill=GRAY)
    p.nav("Calendario")


def wf_cal_dia(d, x, y):
    p = Phone(d, x, y, "Calendario")
    cal_header(p, "Día", "Jueves 21")
    top, rh = p.cy, 56
    gx = p.l(70)
    for i, h in enumerate(range(8, 17)):
        yy = top + i * rh
        d.text((p.l(0), yy - 9), f"{h:02d}:00", font=font(16), fill=GRAY)
        d.line([gx, yy, p.x + PW - 30, yy], fill=LIGHT, width=1)
    blocks = [(8, 1.0, 1, "C", "[Materia A]", "08:00 – 09:00 · Completada"),
              (10, 1.5, 0, "P", "[Materia B]", "10:00 – 11:30 · Pendiente"),
              (14, 1.0, 3, "T", "[Materia C]", "14:00 – 15:00 · Tardía")]
    for h, dur, shade, st, t, sub in blocks:
        by0 = top + (h - 8) * rh + 2
        d.rounded_rectangle([gx + 4, by0, p.x + PW - 34, by0 + dur * rh - 4], 8, fill=SHADES[shade],
                            outline=BLACK)
        col = WHITE if shade < 2 else BLACK
        d.text((gx + 14, by0 + 6), ICON[st] + "  " + t, font=sym(19), fill=col)
        if dur > 1:
            d.text((gx + 14, by0 + 36), sub, font=font(16), fill=col)
    nowy = top + (11.3 - 8) * rh
    d.line([gx, nowy, p.x + PW - 30, nowy], fill=BLACK, width=3)
    d.ellipse([gx - 6, nowy - 6, gx + 6, nowy + 6], fill=BLACK)
    d.text((gx + 8, nowy + 4), "ahora", font=font(15, True), fill=BLACK)
    p.cy = top + 9 * rh + 6
    d.text((p.l(0), p.cy), "Leyenda: ✓ completada ○ pendiente ✕ no realizada ↻ reprogramada ◷ tardía",
           font=sym(14), fill=GRAY)
    p.nav("Calendario")


def wf_sesion(d, x, y):
    p = Phone(d, x, y, "Sesión en curso", back=True)
    p.text("[Materia A] · 10:00 – 11:30", 22, True)
    tabs(p, ["Libre", "Pomodoro"], "Pomodoro", h=42)
    cx, cy, r = p.x + PW / 2, p.cy + 150, 140
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=LIGHT, width=18)
    d.arc([cx - r, cy - r, cx + r, cy + r], -90, 160, fill=DARK, width=18)
    d.text((cx, cy - 20), "18:42", font=font(56, True), fill=BLACK, anchor="mm")
    d.text((cx, cy + 34), "Trabajo · 2 de 4", font=font(20), fill=GRAY, anchor="mm")
    p.cy = cy + r + 34
    w = (PW - 60 - 20) / 2
    for i, s in enumerate(["Pausar", "Detener"]):
        d.rounded_rectangle([p.l(0) + i * (w + 20), p.cy, p.l(0) + i * (w + 20) + w, p.cy + 62], 31,
                            fill=DARK if i else WHITE, outline=BLACK, width=2)
        d.text((p.l(0) + i * (w + 20) + w / 2, p.cy + 31), s, font=font(22, True),
               fill=WHITE if i else BLACK, anchor="mm")
    p.cy += 86

    def row(label_, sub, on):
        top = p.box(84)
        d.text((p.l(16), top + 14), label_, font=font(21, True), fill=BLACK)
        d.text((p.l(16), top + 46), sub, font=font(17), fill=GRAY)
        toggle(d, p.x + PW - 116, top + 24, on)
    row("No molestar", "Global: activado · forzar en esta sesión", True)
    p.text("Tiempo real acumulado: 43 min", 19, color=GRAY)
    p.text("Solo los bloques de trabajo suman horas", 17, color=GRAY)


def wf_estadisticas2(d, x, y):
    p = Phone(d, x, y, "Estadísticas")
    tabs(p, ["Semana", "Mes"], "Semana")
    top = p.box(110, "Total de horas")
    d.text((p.l(16), top + 50), "15 h", font=font(32, True), fill=BLACK)
    d.text((p.l(120), top + 60), "↑ 25 % vs. semana anterior", font=font(18), fill=GRAY)
    top = p.box(132, "Cumplimiento  75 %")
    p.progress(0.75, top=top + 56)
    d.text((p.l(16), top + 86), "✓ 8   ◷ 1   ✕ 2   ↻ 1", font=sym(20), fill=BLACK)
    p.text("Distribución por materia", 22, True)
    cx, cy, r = p.l(105), p.cy + 105, 95
    angs = [0, 144, 216, 288, 324, 360]
    shades = [(60, 60, 60), (110, 110, 110), (160, 160, 160), (200, 200, 200), (230, 230, 230)]
    for a0, a1, s in zip(angs, angs[1:], shades):
        d.pieslice([cx - r, cy - r, cx + r, cy + r], a0 - 90, a1 - 90, fill=s, outline=WHITE, width=2)
    d.ellipse([cx - 50, cy - 50, cx + 50, cy + 50], fill=WHITE)
    for i, (s, pc) in enumerate(zip(["[Mat. A]", "[Mat. B]", "[Mat. C]", "[Mat. D]", "Otras"],
                                    ["40%", "20%", "20%", "10%", "10%"])):
        ly = cy - 90 + i * 38
        d.rectangle([cx + 125, ly, cx + 145, ly + 20], fill=shades[i], outline=BLACK)
        d.text((cx + 156, ly - 2), s, font=font(18), fill=BLACK)
        d.text((x + PW - 40, ly - 2), pc, font=font(18), fill=BLACK, anchor="ra")
    p.cy += 225
    p.button("Ver reporte", filled=False)
    p.nav("Estadíst.")


def wf_ajustes(d, x, y):
    p = Phone(d, x, y, "Ajustes", back=True)
    rows = [("Tema", "Sistema", None), ("Idioma", "Español", None), ("Bloqueo con huella / PIN", None, True),
            ("No molestar en sesiones", None, True), ("Pomodoro", "25 / 5 / 15 · 4", None),
            ("Recordatorio", "15 min antes", None), ("Copia de seguridad", "Semanal", None),
            ("Borrar mis datos", "", None)]
    for t, v, tog in rows:
        top = p.cy
        d.text((p.l(0), top + 14), t, font=font(21, t == "Borrar mis datos"), fill=BLACK)
        if tog is not None:
            toggle(d, p.x + PW - 100, top + 10, tog)
        else:
            d.text((p.x + PW - 60, top + 16), v, font=font(19), fill=GRAY, anchor="ra")
            d.text((p.x + PW - 34, top + 10), "›", font=font(28), fill=BLACK, anchor="ra")
        p.cy += 64
        d.line([p.l(0), p.cy - 6, p.x + PW - 30, p.cy - 6], fill=LIGHT, width=2)


def wf_respaldo(d, x, y):
    p = Phone(d, x, y, "Copia de seguridad", back=True)
    top = p.box(150, "Último respaldo")
    d.text((p.l(16), top + 52), "Hoy 02:00 · 1,2 MB", font=font(21), fill=BLACK)
    d.text((p.l(16), top + 84), "Google Drive · [cuenta@gmail.com]", font=font(18), fill=GRAY)
    d.text((p.l(16), top + 112), "Cifrado con contraseña ✓", font=sym(18), fill=GRAY)
    p.button("Respaldar ahora")
    p.field("Frecuencia", "Semanal", None)
    p.d.text((p.l(0), p.cy - 8), "Nunca · Diaria · Semanal · Mensual · Solo manual", font=font(16), fill=GRAY)
    p.cy += 22
    p.field("Contraseña (opcional)", "Activada", None)
    p.button("Restaurar desde Drive", filled=False)


def wf_borrar(d, x, y):
    p = Phone(d, x, y, "Privacidad", back=True)
    d.rectangle([x + 6, y + 160, x + PW - 6, y + PH - 60], fill=(235, 235, 235))
    bx0, by0 = x + 40, y + 260
    d.rounded_rectangle([bx0, by0, x + PW - 40, by0 + 520], 24, fill=WHITE, outline=BLACK, width=3)
    d.text((bx0 + 24, by0 + 24), "¿Qué quieres borrar?", font=font(24, True), fill=BLACK)
    opts = ["Solo datos del teléfono", "Solo respaldo en Drive", "Teléfono y Drive"]
    for i, o in enumerate(opts):
        yy = by0 + 90 + i * 64
        d.ellipse([bx0 + 24, yy, bx0 + 54, yy + 30], outline=BLACK, width=2)
        if i == 2:
            d.ellipse([bx0 + 32, yy + 8, bx0 + 46, yy + 22], fill=BLACK)
        d.text((bx0 + 70, yy + 2), o, font=font(20), fill=BLACK)
    d.text((bx0 + 24, by0 + 300), "Esta acción es irreversible.", font=font(19, True), fill=BLACK)
    d.text((bx0 + 24, by0 + 330), "Se pedirá una segunda confirmación.", font=font(18), fill=GRAY)
    w = (PW - 80 - 68) / 2
    for i, s in enumerate(["Cancelar", "Borrar"]):
        xx = bx0 + 24 + i * (w + 20)
        d.rounded_rectangle([xx, by0 + 420, xx + w, by0 + 476], 28, fill=DARK if i else WHITE, outline=BLACK,
                            width=2)
        d.text((xx + w / 2, by0 + 448), s, font=font(20, True), fill=WHITE if i else BLACK, anchor="mm")


def wf_volviste(d, x, y):
    p = Phone(d, x, y, "StudyPlan")
    d.rectangle([x + 6, y + 160, x + PW - 6, y + PH - 60], fill=(235, 235, 235))
    bx0, by0 = x + 34, y + 200
    d.rounded_rectangle([bx0, by0, x + PW - 34, by0 + 700], 24, fill=WHITE, outline=BLACK, width=3)
    d.text((bx0 + 22, by0 + 22), "Saliste de la app a las 8:45", font=font(23, True), fill=BLACK)
    d.text((bx0 + 22, by0 + 58), "[Materia A] sigue En curso", font=font(19), fill=GRAY)
    d.text((bx0 + 22, by0 + 100), "¿Seguiste estudiando?", font=font(21, True), fill=BLACK)
    opts = ["No, terminé al salir (8:45)", "Sí, estudié hasta las:", "Sigo estudiando ahora"]
    for i, o in enumerate(opts):
        yy = by0 + 150 + i * 70
        d.ellipse([bx0 + 22, yy, bx0 + 52, yy + 30], outline=BLACK, width=2)
        if i == 1:
            d.ellipse([bx0 + 30, yy + 8, bx0 + 44, yy + 22], fill=BLACK)
        d.text((bx0 + 66, yy + 2), o, font=font(19), fill=BLACK)
    d.rounded_rectangle([bx0 + 290, by0 + 214, bx0 + 420, by0 + 258], 10, outline=BLACK, width=2)
    d.text((bx0 + 304, by0 + 222), "11:00  ◷", font=sym(20), fill=BLACK)
    d.text((bx0 + 22, by0 + 380), "Nota (opcional)", font=font(18), fill=GRAY)
    d.rounded_rectangle([bx0 + 22, by0 + 410, x + PW - 56, by0 + 470], 10, outline=BLACK, width=2)
    d.text((bx0 + 38, by0 + 428), "Estudié en grupo", font=font(19), fill=BLACK)
    d.text((bx0 + 22, by0 + 500), "Se registrará como «estudio con", font=font(17, italic=True), fill=GRAY)
    d.text((bx0 + 22, by0 + 524), "la app cerrada» (8:45 – 11:00)", font=font(17, italic=True), fill=GRAY)
    d.rounded_rectangle([bx0 + 22, by0 + 600, x + PW - 56, by0 + 660], 30, fill=DARK, outline=BLACK, width=2)
    d.text(((bx0 + x + PW - 34) / 2, by0 + 630), "Confirmar", font=font(22, True), fill=WHITE, anchor="mm")


def wf_repartir(d, x, y):
    p = Phone(d, x, y, "Detener sesión", back=True)
    p.text("Estudiaste 10:00 – 12:10", 22, True)
    p.text("2 h 10 min en total · 1 h 10 min extra", 19, color=GRAY)
    p.text("Tu sesión siguiente se cruzó:", 20, True)
    p.list_item("[Inglés]", "Planificada 11:00 – 12:00", "○", "Pendiente")
    p.text("Repartir el tiempo", 21, True)
    x0, x1, yb = p.l(0), p.x + PW - 30, p.cy + 10
    cut = x0 + (x1 - x0) * 0.5
    d.rectangle([x0, yb, cut, yb + 46], fill=(90, 90, 90), outline=BLACK, width=2)
    d.rectangle([cut, yb, x1, yb + 46], fill=(200, 200, 200), outline=BLACK, width=2)
    d.text(((x0 + cut) / 2, yb + 23), "Materia A", font=font(18, True), fill=WHITE, anchor="mm")
    d.text(((cut + x1) / 2, yb + 23), "Inglés", font=font(18, True), fill=BLACK, anchor="mm")
    d.ellipse([cut - 16, yb + 7, cut + 16, yb + 39], fill=WHITE, outline=BLACK, width=3)
    d.text((x0, yb + 58), "10:00", font=font(16), fill=GRAY)
    d.text((cut, yb + 58), "11:05", font=font(16, True), fill=BLACK, anchor="ma")
    d.text((x1, yb + 58), "12:10", font=font(16), fill=GRAY, anchor="ra")
    p.cy = yb + 100
    p.text("Materia A: 65 min · Inglés: 65 min", 19)
    p.button("Repartir y completar ambas")
    p.button("Todo a [Materia A]", filled=False)


def wf_detalle(d, x, y):
    p = Phone(d, x, y, "Detalle de sesión", back=True)
    p.text("[Materia A] · Completada", 21, True)
    mets = [("Concentración", "92 %"), ("Interrup./h", "1,5"), ("Puntualidad", "+12 min"),
            ("Respeto tiempo", "108 %"), ("Fuera de app", "14 min"), ("App cerrada", "0 min")]
    w = (PW - 60 - 20) / 2
    for i, (k, v) in enumerate(mets):
        cx0 = p.l(0) + (i % 2) * (w + 20)
        cy0 = p.cy + (i // 2) * 92
        d.rounded_rectangle([cx0, cy0, cx0 + w, cy0 + 80], 12, outline=BLACK, width=2)
        d.text((cx0 + 14, cy0 + 10), k, font=font(17), fill=GRAY)
        d.text((cx0 + 14, cy0 + 36), v, font=font(26, True), fill=BLACK)
    p.cy += 3 * 92 + 10
    p.text("Eventos", 21, True)
    evs = [("10:12", "Inicio"), ("10:40", "Salida de la app (llamada)"), ("10:54", "Regreso a la app"),
           ("11:00", "Fin planificado · ¿Sigues? → Sigo"), ("11:00", "Tiempo extra"), ("11:17", "Fin")]
    for i, (h, e) in enumerate(evs):
        yy = p.cy + i * 46
        d.ellipse([p.l(70), yy + 6, p.l(84), yy + 20], fill=BLACK)
        if i < len(evs) - 1:
            d.line([p.l(77), yy + 20, p.l(77), yy + 52], fill=GRAY, width=2)
        d.text((p.l(0), yy), h, font=font(18, True), fill=BLACK)
        d.text((p.l(100), yy), e, font=font(18), fill=BLACK)


WIREFRAMES = [
    ("wireframes_1.png", [(wf_inicio, "W1 · Inicio", "HU-01, HU-02, HU-27"),
                          (wf_materias, "W2 · Materias", "HU-06 a HU-09"),
                          (wf_registrar, "W3 · Registrar sesión", "HU-10, HU-11, HU-34")]),
    ("wireframes_2.png", [(wf_cal_mes, "W4 · Calendario: Mes", "HU-16, HU-19, HU-21"),
                          (wf_cal_semana, "W5 · Calendario: Semana", "HU-17, HU-19, HU-20"),
                          (wf_cal_dia, "W6 · Calendario: Día", "HU-18, HU-19, HU-20")]),
    ("wireframes_3.png", [(wf_sesion, "W7 · Sesión en curso", "HU-22, HU-23, HU-24"),
                          (wf_estadisticas2, "W8 · Estadísticas", "HU-29, HU-30, HU-31"),
                          (wf_reporte, "W9 · Reporte", "HU-32, HU-33")]),
    ("wireframes_4.png", [(wf_ajustes, "W10 · Ajustes", "HU-03, HU-04, HU-23, HU-24"),
                          (wf_respaldo, "W11 · Copia de seguridad", "HU-35, HU-36, HU-37"),
                          (wf_borrar, "W12 · Borrar mis datos", "HU-38")]),
    ("wireframes_5.png", [(wf_volviste, "W13 · Al volver a la app", "HU-40, HU-41"),
                          (wf_repartir, "W14 · Detener y repartir", "HU-42, HU-43, HU-44"),
                          (wf_detalle, "W15 · Detalle y eventos", "HU-39, HU-45")]),
]


if __name__ == "__main__":
    modelo_dominio()
    maquina_estados()
    flujo_pantallas()
    arquitectura_hexagonal()
    ciclo_scrum_sdd()
    for nombre, pantallas in WIREFRAMES:
        wireframes(nombre, pantallas)
    import diagramas_clases  # clases, modelo lógico y secuencia: se dibujan desde el código y el esquema de Room
    diagramas_clases.generar()
    for f in sorted(OUT.glob("*.png")):
        print(f.relative_to(ROOT))
