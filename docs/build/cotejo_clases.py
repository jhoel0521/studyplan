"""Coteja los diagramas con el código y con el esquema de Room.

Comprueba tres cosas:
  1. cada clase de los diagramas existe en el código y cada clase de los paquetes aparece en algún diagrama;
  2. los PNG de clases, modelo lógico y secuencia coinciden con lo que se dibujaría hoy (las firmas de métodos y las
     tablas salen del código y de app/schemas, así que un método renombrado o una columna nueva los deja desactualizados);
  3. las secciones 4.2.1 y 4.2.2 del capítulo 4 coinciden con el esquema más reciente.

Uso: python docs/build/cotejo_clases.py
Termina con código 1 si hay diferencias; se corrigen con python docs/build/diagramas_clases.py.
"""
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import diagramas as dg  # noqa: E402
import diagramas_clases as dc  # noqa: E402

RAIZ = dc.jf.RAIZ
IMAGENES = ["diagrama_clases_configuracion", "diagrama_clases_materias", "diagrama_clases_sesiones",
            "diagrama_clases_calendario", "diagrama_clases_estadisticas", "diagrama_clases_reportes",
            "diagrama_clases_recordatorios", "modelo_logico", "secuencia_registrar_materia",
            "secuencia_planificar_sesion", "secuencia_consultar_calendario"]


def cotejar_clases(errores):
    total = 0
    for paquete, del_diagrama in dc.CLASES_POR_PAQUETE.items():
        en_codigo = {f.stem for f in (RAIZ / paquete).rglob("*.java")}
        total += len(en_codigo)
        for falta in sorted(set(del_diagrama) - en_codigo):
            errores.append(f"{paquete}: «{falta}» está en el diagrama y no existe en el código")
        for sobra in sorted(en_codigo - set(del_diagrama)):
            errores.append(f"{paquete}: «{sobra}» existe en el código y no aparece en ningún diagrama")
    todas = {f.stem for f in RAIZ.rglob("*.java")}
    cubiertas = {c for g in dc.CLASES_POR_PAQUETE.values() for c in g}
    for sin_paquete in sorted(todas - cubiertas):
        errores.append(f"«{sin_paquete}» está en un paquete que los diagramas no cubren")
    return total


def cotejar_imagenes(errores):
    carpeta_real = dg.OUT
    with tempfile.TemporaryDirectory() as temporal:
        dg.OUT = Path(temporal)
        try:
            dc.clases_configuracion()
            dc.clases_materias()
            dc.clases_sesiones()
            dc.clases_calendario()
            dc.clases_estadisticas()
            dc.clases_reportes()
            dc.clases_recordatorios()
            dc.modelo_logico()
            dc.secuencia_registrar_materia()
            dc.secuencia_planificar_sesion()
            dc.secuencia_consultar_calendario()
        finally:
            dg.OUT = carpeta_real
        for nombre in IMAGENES:
            nuevo = (Path(temporal) / f"{nombre}.png").read_bytes()
            actual = (carpeta_real / f"{nombre}.png")
            if not actual.exists() or actual.read_bytes() != nuevo:
                errores.append(f"{nombre}.png está desactualizado respecto al código o al esquema")


def cotejar_capitulo(errores):
    version, entidades = dc._ultimo_esquema()[0], dc._ultimo_esquema()[1]["entities"]
    texto = dc.CAPITULO_4.read_text(encoding="utf-8")
    for encabezado, esperado in (("Modelo Lógico", dc.texto_modelo_logico(version)),
                                 ("Diccionario de Datos", dc.texto_diccionario(entidades))):
        actualizado = dc.reemplazar_seccion(texto, encabezado, esperado)
        if actualizado != texto:
            errores.append(f"La sección «{encabezado}» del capítulo 4 no coincide con el esquema de Room")


def main():
    errores = []
    total = cotejar_clases(errores)
    cotejar_imagenes(errores)
    cotejar_capitulo(errores)
    if errores:
        print("\n".join(errores))
        sys.exit(1)
    print(f"Cotejo correcto: {total} clases en {len(dc.CLASES_POR_PAQUETE)} paquetes, diagramas y capítulo al día.")


if __name__ == "__main__":
    main()
