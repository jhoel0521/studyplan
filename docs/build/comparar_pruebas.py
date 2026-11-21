"""Compara las pruebas JVM ejecutadas con una línea base (RNF-15, change reorganizar-paquetes-avisos-documentacion).

Lee ``app/build/test-results/testDebugUnitTest/*.xml`` y compara el número y los nombres de las pruebas con
``docs/build/linea_base_pruebas.txt``. Los nombres se comparan sin paquete (solo ``Clase.metodo``), porque mover una
clase de carpeta no debe cambiar qué pruebas existen.

Uso:
    python docs/build/comparar_pruebas.py            compara con la línea base
    python docs/build/comparar_pruebas.py --guardar  guarda las pruebas actuales como línea base

Se retira al cerrar el change, junto con la línea base.
"""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
RESULTADOS = RAIZ / "app" / "build" / "test-results" / "testDebugUnitTest"
LINEA_BASE = Path(__file__).resolve().parent / "linea_base_pruebas.txt"
# Clases de prueba que el propio change añade (no existen en la línea base y no cuentan como diferencia).
AGREGADAS_POR_EL_CHANGE = {"EstructuraPaquetesTest", "MapasTest"}


def pruebas_actuales():
    """Devuelve la lista ordenada de ``Clase.metodo`` de las pruebas ejecutadas (con repeticiones si las hay)."""
    if not RESULTADOS.is_dir():
        sys.exit("No hay resultados en %s: corre ./gradlew test primero." % RESULTADOS)
    nombres = []
    for archivo in sorted(RESULTADOS.glob("TEST-*.xml")):
        for caso in ET.parse(archivo).getroot().iter("testcase"):
            clase = caso.get("classname", "").rsplit(".", 1)[-1]
            if clase in AGREGADAS_POR_EL_CHANGE:
                continue
            nombres.append("%s.%s" % (clase, caso.get("name")))
    return sorted(nombres)


def main():
    actuales = pruebas_actuales()
    if "--guardar" in sys.argv:
        LINEA_BASE.write_text("\n".join(actuales) + "\n", encoding="utf-8")
        print("Línea base guardada: %d pruebas en %s" % (len(actuales), LINEA_BASE.name))
        return 0
    if not LINEA_BASE.is_file():
        sys.exit("Falta %s: corre con --guardar para crearla." % LINEA_BASE.name)
    base = [l for l in LINEA_BASE.read_text(encoding="utf-8").splitlines() if l.strip()]
    faltan = sorted(set(base) - set(actuales))
    sobran = sorted(set(actuales) - set(base))
    if len(base) == len(actuales) and not faltan and not sobran:
        print("Pruebas iguales a la línea base: %d" % len(actuales))
        return 0
    print("Pruebas distintas de la línea base: base=%d, actuales=%d" % (len(base), len(actuales)))
    for n in faltan[:40]:
        print("  falta:", n)
    for n in sobran[:40]:
        print("  sobra:", n)
    return 1


if __name__ == "__main__":
    sys.exit(main())
