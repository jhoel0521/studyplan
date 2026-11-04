"""Genera las specs delta de OpenSpec a partir de requisitos.py.

Uso:  python docs/build/gen_specs.py
Luego: openspec validate add-requisitos-base --strict
"""
import shutil
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from requisitos import (EPICAS, ESTADOS_FINALES, ESTADOS_NOMBRE, HISTORIAS, REGLAS, ROL,  # noqa: E402
                        TRANSICIONES)

ROOT = Path(__file__).resolve().parents[2]
CHANGE = ROOT / "openspec" / "changes" / "add-requisitos-base" / "specs"


def requirement(hu):
    lines = [
        f"### Requirement: {hu['id']} {hu['titulo']}",
        f"Como {ROL}, quiero {hu['quiero']} para {hu['para']}. "
        f"El sistema SHALL cumplir los escenarios siguientes "
        f"(prioridad {hu['prioridad']}, {hu['puntos']} pts).",
        "",
    ]
    for nombre, dado, cuando, entonces in hu["ca"]:
        lines += [f"#### Scenario: {nombre}", f"- **GIVEN** {dado}", f"- **WHEN** {cuando}",
                  f"- **THEN** {entonces}", ""]
    return lines


def maquina_estados():
    n = ESTADOS_NOMBRE
    finales = ", ".join(n[e] for e in ESTADOS_FINALES)
    lines = [
        "### Requirement: RN-07 Máquina de estados de la sesión",
        "El estado de una sesión SHALL cambiar únicamente por las transiciones listadas en los "
        "escenarios; cada estado conoce solo sus acciones y su estado siguiente. Los estados "
        f"finales ({finales}) MUST NOT admitir transiciones.",
        "",
    ]
    for origen, evento, destino, cond in TRANSICIONES:
        o = "una sesión nueva" if origen == "(nueva)" else f"una sesión en estado {n[origen]}"
        lines += [f"#### Scenario: {n.get(origen, 'Nueva')} → {n[destino]} ({evento})",
                  f"- **GIVEN** {o}", f"- **WHEN** ocurre el evento «{evento}»",
                  f"- **THEN** la sesión pasa a {n[destino]} — {cond}", ""]
    lines += ["#### Scenario: Estado final sin transiciones",
              "- **GIVEN** una sesión en estado Completada, Completada (registro tardío) o Reprogramada",
              "- **WHEN** se intenta cualquier evento de cambio de estado",
              "- **THEN** el sistema rechaza el cambio y la sesión conserva su estado", ""]
    return lines


def main():
    if CHANGE.exists():
        shutil.rmtree(CHANGE)
    for eid, (nombre, cap, proposito) in EPICAS.items():
        hus = [h for h in HISTORIAS if h["epica"] == eid]
        out = [f"# Spec Delta — {nombre} ({eid})", "", "## Purpose", "", proposito, "",
               "## ADDED Requirements", ""]
        if cap == "sesiones-estudio":
            out += maquina_estados()
        for hu in hus:
            out += requirement(hu)
        path = CHANGE / cap / "spec.md"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text("\n".join(out).rstrip() + "\n", encoding="utf-8")
        print(f"spec {cap}: {', '.join(h['id'] for h in hus)}")
    print(f"{len(HISTORIAS)} HU, {len(REGLAS)} RN")


if __name__ == "__main__":
    main()
