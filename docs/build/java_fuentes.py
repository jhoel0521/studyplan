"""Lee los fuentes Java de la app para que los diagramas se dibujen desde el código y no se desfasen.

Uso (módulo): ``metodos("MainActivity")`` devuelve las firmas de los métodos de la clase, por ejemplo
``["# onCreate(Bundle): void", "- enlazarVistas(): void"]``.
"""
import re
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2] / "app" / "src" / "main" / "java" / "io" / "github" / "jhoel0521" / "studyplan"

_METODO = re.compile(
    r"^    (?P<mods>(?:(?:public|protected|private|static|final|abstract|synchronized|default)\s+)*)"
    r"(?P<gen><[^>]+>\s+)?(?P<ret>[\w.]+(?:<[^>]*>)?(?:\[\])?)\s+(?P<nombre>\w+)\((?P<params>[^)]*)\)"
    r"\s*(?:throws[\w, .]+)?\s*[;{]",
    re.M)
_PALABRAS_NO_METODO = {"return", "new", "else", "throw", "case"}


def fuente(clase):
    """Devuelve la ruta del archivo de la clase o ``None`` si no existe."""
    for ruta in RAIZ.rglob(clase + ".java"):
        return ruta
    return None


def _tipo_de_parametro(parametro):
    parametro = re.sub(r"@\w+(\([^)]*\))?\s*", "", parametro).replace("final ", "").strip()
    partes = parametro.rsplit(" ", 1)
    return partes[0].strip() if len(partes) == 2 else parametro


def _visibilidad(mods):
    if "private" in mods:
        return "-"
    if "protected" in mods:
        return "#"
    if "public" in mods:
        return "+"
    return "~"


def metodos(clase, incluir_privados=True):
    """Firmas de los métodos declarados al nivel de la clase (no los de clases anidadas), en orden de aparición."""
    ruta = fuente(clase)
    if ruta is None:
        raise FileNotFoundError(f"No existe el fuente de {clase}")
    texto = ruta.read_text(encoding="utf-8")
    es_interfaz = re.search(r"\binterface\s+" + clase + r"\b", texto) is not None
    resultado = []
    for m in _METODO.finditer(texto):
        if m.group("ret") in _PALABRAS_NO_METODO or m.group("nombre") == clase:
            continue
        mods = m.group("mods")
        visibilidad = "+" if es_interfaz and not mods.strip() else _visibilidad(mods)
        if visibilidad == "-" and not incluir_privados:
            continue
        parametros = [p for p in (_tipo_de_parametro(x) for x in m.group("params").split(",")) if p]
        resultado.append(f"{visibilidad} {m.group('nombre')}({', '.join(parametros)}): {m.group('ret')}")
    return resultado


def nombres_de_metodos(clase):
    """Solo los nombres de los métodos de la clase (para el cotejo)."""
    return [firma.split("(")[0].split(" ")[1] for firma in metodos(clase)]


def campos(clase):
    """Campos de instancia declarados (con su tipo) al nivel de la clase, por ejemplo ``['- long id']``."""
    ruta = fuente(clase)
    texto = ruta.read_text(encoding="utf-8")
    resultado = []
    patron = re.compile(r"^    (?P<mods>(?:(?:public|protected|private|static|final|volatile)\s+)*)"
                        r"(?P<tipo>[\w.]+(?:<[^>]*>)?(?:\[\])?)\s+(?P<nombre>\w+)\s*(?:=[^;]+)?;", re.M)
    for m in patron.finditer(texto):
        mods = m.group("mods")
        if "static" in mods or m.group("tipo") in _PALABRAS_NO_METODO:
            continue
        resultado.append(f"{_visibilidad(mods)} {m.group('nombre')}: {m.group('tipo')}")
    return resultado
