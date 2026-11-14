"""Coteja el diagrama de clases con el código: cada clase del diagrama existe y cada clase de los paquetes aparece.

Uso: python docs/build/cotejo_clases.py
Termina con código 1 si hay diferencias.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import diagramas  # noqa: E402

RAIZ = diagramas.ROOT / "app" / "src" / "main" / "java" / "io" / "github" / "jhoel0521" / "studyplan"

GRUPOS = {
    "adapter/in/ui": diagramas.CLASES_PRESENTACION,
    "application/port/in": diagramas.CLASES_PUERTOS_ENTRADA,
    "application/service": diagramas.CLASES_SERVICIOS,
    "application/port/out": diagramas.CLASES_PUERTOS_SALIDA,
    "domain": diagramas.CLASES_DOMINIO,
    "adapter/out/persistence": diagramas.CLASES_PERSISTENCIA,
    "adapter/out/security": diagramas.CLASES_SEGURIDAD,
}


def main():
    errores = []
    total = 0
    for paquete, del_diagrama in GRUPOS.items():
        en_codigo = {f.stem for f in (RAIZ / paquete).glob("*.java")}
        total += len(en_codigo)
        for falta in sorted(set(del_diagrama) - en_codigo):
            errores.append(f"{paquete}: «{falta}» está en el diagrama y no existe en el código")
        for sobra in sorted(en_codigo - set(del_diagrama)):
            errores.append(f"{paquete}: «{sobra}» existe en el código y no aparece en el diagrama")
    todas = {f.stem for f in RAIZ.rglob("*.java")}
    cubiertas = {c for g in GRUPOS.values() for c in g}
    for sin_paquete in sorted(todas - cubiertas):
        errores.append(f"«{sin_paquete}» está en un paquete que el diagrama no cubre")
    if errores:
        print("\n".join(errores))
        sys.exit(1)
    print(f"Cotejo correcto: {total} clases en {len(GRUPOS)} paquetes, todas en el diagrama y ninguna de más.")


if __name__ == "__main__":
    main()
