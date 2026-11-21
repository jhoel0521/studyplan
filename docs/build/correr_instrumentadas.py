"""Corre las clases de prueba instrumentadas una por una en el emulador (RNF-15, change reorganizar-paquetes-avisos-documentacion).

Cada clase se ejecuta por separado (`connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=...`)
porque varias clases en una sola llamada no se ejecutan de forma fiable y el emulador se queda sin memoria.

Uso (desde la raíz del repositorio, con el emulador ya iniciado):
    python docs/build/correr_instrumentadas.py                    todas las clases de prueba de androidTest
    python docs/build/correr_instrumentadas.py Migracion          solo las cuyo nombre contiene «Migracion»
Variable de entorno: ANDROID_SERIAL (por omisión emulator-5554).

Se retira al cerrar el change.
"""
import os
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
FUENTES = RAIZ / "app" / "src" / "androidTest" / "java"
RESULTADOS = RAIZ / "app" / "build" / "outputs" / "androidTest-results" / "connected"
EXCLUIDAS = {"Pruebas", "ExampleInstrumentedTest"}


def clases():
    resultado = []
    for ruta in sorted(FUENTES.rglob("*.java")):
        if ruta.stem in EXCLUIDAS:
            continue
        texto = ruta.read_text(encoding="utf-8")
        if "@Test" not in texto:
            continue
        paquete = re.search(r"^package\s+([\w.]+);", texto, re.M).group(1)
        resultado.append("%s.%s" % (paquete, ruta.stem))
    return resultado


def correr(clase, entorno):
    shutil.rmtree(RESULTADOS, ignore_errors=True)  # que no queden resultados de la clase anterior
    comando = [str(RAIZ / "gradlew.bat"), "connectedDebugAndroidTest", "--console=plain", "-q",
               "-Pandroid.testInstrumentationRunnerArguments.class=" + clase]
    subprocess.run(comando, cwd=RAIZ, env=entorno, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    for archivo in RESULTADOS.rglob("*.xml"):
        raiz = ET.parse(archivo).getroot()
        return int(raiz.get("tests", 0)), int(raiz.get("failures", 0)) + int(raiz.get("errors", 0))
    return 0, -1  # sin resultados: no corrió


def main():
    filtro = sys.argv[1] if len(sys.argv) > 1 else ""
    entorno = dict(os.environ, ANDROID_SERIAL=os.environ.get("ANDROID_SERIAL", "emulator-5554"))
    fallidas = 0
    total = 0
    for clase in clases():
        if filtro not in clase:
            continue
        pruebas, fallos = correr(clase, entorno)
        total += 1
        estado = "OK" if fallos == 0 and pruebas > 0 else "FALLA"
        if estado != "OK":
            fallidas += 1
        print("%-5s %3d pruebas, %d fallos  %s" % (estado, pruebas, max(fallos, 0), clase.rsplit(".", 1)[-1]), flush=True)
    print("%d clases, %d con fallos" % (total, fallidas))
    return 1 if fallidas else 0


if __name__ == "__main__":
    sys.exit(main())
