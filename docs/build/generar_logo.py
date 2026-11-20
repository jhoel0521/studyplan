"""Genera los recursos del logo de StudyPlan desde docs/documento/recursos/logos/logo-app.jpg.

Uso: python docs/build/generar_logo.py. Escribe los recursos de app/src/main/res y los archivos de Google Play
en docs/documento/recursos/logos/play-store/. No implementa ninguna historia de usuario (RNF-07).
"""
import os
import numpy as np
from PIL import Image, ImageDraw, ImageFont

RAIZ = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')).replace(os.sep, '/') + '/'
FUENTE = RAIZ + 'docs/documento/recursos/logos/logo-app.jpg'
RES = RAIZ + 'app/src/main/res/'
TIENDA = RAIZ + 'docs/documento/recursos/logos/play-store/'
INDIGO = (0x43, 0x38, 0xCA)  # sp_action, token de .ux/standard/tokens.json

os.makedirs(TIENDA, exist_ok=True)
a = np.array(Image.open(FUENTE).convert('RGB')).astype(float)
fondo = np.median(a[:200, :200].reshape(-1, 3), axis=0)
dist = np.abs(a - fondo).sum(axis=2)

blanco = np.median(a[a.min(axis=2) > 240].reshape(-1, 3), axis=0)
mask_teal = (a[:, :, 1] > a[:, :, 0] + 60) & (dist > 150)
teal = np.median(a[mask_teal].reshape(-1, 3), axis=0)
print('fondo', fondo, 'blanco', blanco, 'teal', teal, 'pixeles teal', int(mask_teal.sum()))


def proyectar(color):
    v = color - fondo
    d = a - fondo
    t = (d @ v) / (v @ v)
    residuo = np.linalg.norm(d - t[..., None] * v, axis=2)
    return t, residuo


tb, rb = proyectar(blanco)
tt, rt = proyectar(teal)
es_teal = rt < rb
alfa = np.clip(np.where(es_teal, tt, tb), 0, 1)
alfa[alfa < 0.05] = 0
alfa[alfa > 0.97] = 1
color = np.where(es_teal[..., None], teal, blanco)

rgba = np.zeros(a.shape[:2] + (4,), dtype=np.uint8)
rgba[..., :3] = np.clip(color, 0, 255).astype(np.uint8)
rgba[..., 3] = (alfa * 255).astype(np.uint8)
simbolo = Image.fromarray(rgba, 'RGBA')

ys, xs = np.where(alfa > 0.5)
x0, x1, y0, y1 = xs.min(), xs.max(), ys.min(), ys.max()
cx, cy, ancho = (x0 + x1) / 2, (y0 + y1) / 2, x1 - x0
print('simbolo bbox', x0, x1, y0, y1, 'ancho', ancho)


def lienzo(tam, proporcion):
    """Cuadrado de `tam` px con el anillo ocupando `proporcion` del lado, centrado, sobre transparente."""
    lado = ancho / proporcion
    caja = (int(round(cx - lado / 2)), int(round(cy - lado / 2)),
            int(round(cx + lado / 2)), int(round(cy + lado / 2)))
    return simbolo.crop(caja).resize((tam, tam), Image.LANCZOS)


def silueta(img, rgb):
    """Un solo color con la forma del símbolo (alfa intacto)."""
    r = np.array(img)
    r[..., 0], r[..., 1], r[..., 2] = rgb
    return Image.fromarray(r, 'RGBA')


def sobre_indigo(tam, proporcion):
    base = Image.new('RGBA', (tam, tam), INDIGO + (255,))
    base.alpha_composite(lienzo(tam, proporcion))
    return base


def guardar(img, ruta, **kw):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    img.save(ruta, **kw)
    print('ok', ruta.replace(RAIZ, ''), img.size, img.mode, os.path.getsize(ruta), 'bytes')


# --- icono adaptativo: primer plano y monocromo (432 px de referencia x 2 = 864; se entrega a 1024)
PROP_ADAPTATIVO = 0.58  # 0.58 x 108 dp = 62,6 dp < 66 dp del círculo de seguridad
guardar(lienzo(1024, PROP_ADAPTATIVO), RES + 'drawable-nodpi/ic_launcher_foreground.png', optimize=True)
guardar(silueta(lienzo(1024, PROP_ADAPTATIVO), (255, 255, 255)),
        RES + 'drawable-nodpi/ic_launcher_monochrome.png', optimize=True)

# --- iconos heredados (API 24 y 25): cuadrado con esquinas redondeadas y redondo
tamanos = {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}
for dens, px in tamanos.items():
    grande = sobre_indigo(px * 4, 0.74)
    mascara = Image.new('L', grande.size, 0)
    ImageDraw.Draw(mascara).rounded_rectangle([0, 0, grande.size[0] - 1, grande.size[1] - 1],
                                              radius=int(grande.size[0] * 0.2), fill=255)
    cuadrado = Image.new('RGBA', grande.size, (0, 0, 0, 0))
    cuadrado.paste(grande, (0, 0), mascara)
    guardar(cuadrado.resize((px, px), Image.LANCZOS), RES + 'mipmap-%s/ic_launcher.webp' % dens,
            lossless=True, quality=100)
    mascara = Image.new('L', grande.size, 0)
    ImageDraw.Draw(mascara).ellipse([0, 0, grande.size[0] - 1, grande.size[1] - 1], fill=255)
    redondo = Image.new('RGBA', grande.size, (0, 0, 0, 0))
    redondo.paste(grande, (0, 0), mascara)
    guardar(redondo.resize((px, px), Image.LANCZOS), RES + 'mipmap-%s/ic_launcher_round.webp' % dens,
            lossless=True, quality=100)

# --- notificaciones: blanco sobre transparente, 24 dp
densidades = {'mdpi': 24, 'hdpi': 36, 'xhdpi': 48, 'xxhdpi': 72, 'xxxhdpi': 96}
for dens, px in densidades.items():
    guardar(silueta(lienzo(px * 4, 0.92), (255, 255, 255)).resize((px, px), Image.LANCZOS),
            RES + 'drawable-%s/ic_notificacion.png' % dens, optimize=True)

# --- Play Store: icono 512 x 512, PNG de 32 bits con alfa, cuadrado y a sangre completa
icono = sobre_indigo(512, 0.74)
guardar(icono, TIENDA + 'icono-512.png', optimize=True)

# --- Play Store: gráfico de función 1024 x 500, 24 bits, sin transparencia
grafico = Image.new('RGB', (1024, 500), INDIGO)
sim = lienzo(380, 0.90)
pegado = Image.new('RGBA', (1024, 500), INDIGO + (255,))
pegado.alpha_composite(sim, (70, 60))
grafico = pegado.convert('RGB')
tinta = ImageDraw.Draw(grafico)
ruta_fuente = RES + 'font/plus_jakarta_sans_extrabold.ttf'
ancho_texto_max = 1024 - 470 - 50
tam = 140
while tam > 40:
    fuente = ImageFont.truetype(ruta_fuente, tam)
    caja = tinta.textbbox((0, 0), 'StudyPlan', font=fuente)
    if caja[2] - caja[0] <= ancho_texto_max:
        break
    tam -= 4
alto_texto = caja[3] - caja[1]
tinta.text((470 - caja[0], (500 - alto_texto) / 2 - caja[1]), 'StudyPlan', font=fuente, fill=(255, 255, 255))
guardar(grafico, TIENDA + 'grafico-funcion-1024x500.png', optimize=True)
print('tamano de letra del gráfico', tam)
