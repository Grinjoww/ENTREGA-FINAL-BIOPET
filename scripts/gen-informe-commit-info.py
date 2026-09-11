#!/usr/bin/env python3
"""Genera docs/informe/commit-info.tex: un fragmento LaTeX minimo con el
hash corto y la fecha del commit HEAD actual, para que la caratula de
recalificacion (docs/informe/caratula-entrega-final.tex) muestre el
commit realmente evaluado en cada compilacion, sin hardcodear un valor
que quedaria obsoleto en cuanto se mueva el tag v1.0.0.

Multiplataforma (Windows/macOS/Linux): usa solo la biblioteca estandar
de Python 3 (subprocess), sin dependencias externas ni Bash/WSL.

No requiere shell-escape ni ningun paquete LaTeX adicional: el .tex
generado se incluye con \\IfFileExists + \\input (nucleo de LaTeX, mismo
mecanismo que ya usa \\evidencia en informe-final-v1.0.0.tex).

Uso (desde la raiz del repositorio, ANTES de compilar la caratula):
    python scripts/gen-informe-commit-info.py

El archivo generado NO se versiona (ver docs/informe/.gitignore): debe
regenerarse en cada checkout/commit que se quiera reflejar, incluido el
checkout final sobre el que se mueva v1.0.0.
"""

import subprocess
import sys
from pathlib import Path

MESES_ES = {
    "01": "enero", "02": "febrero", "03": "marzo", "04": "abril",
    "05": "mayo", "06": "junio", "07": "julio", "08": "agosto",
    "09": "septiembre", "10": "octubre", "11": "noviembre", "12": "diciembre",
}


def git(*args):
    return subprocess.run(
        ["git", *args], check=True, capture_output=True, text=True
    ).stdout.strip()


def fecha_es(fecha_iso):
    """'2026-09-11' -> '11 de septiembre de 2026' (sin depender del locale)."""
    aaaa, mm, dd = fecha_iso.split("-")
    mes = MESES_ES.get(mm, mm)
    dia = dd[1:] if dd.startswith("0") else dd
    return f"{dia} de {mes} de {aaaa}"


def main():
    try:
        hash_corto = git("rev-parse", "--short", "HEAD")
        fecha_iso = git("log", "-1", "--format=%ad", "--date=format:%Y-%m-%d")
    except (subprocess.CalledProcessError, FileNotFoundError) as exc:
        print(f"[gen-informe-commit-info] ERROR: no se pudo leer Git ({exc})",
              file=sys.stderr)
        return 1

    fecha = fecha_es(fecha_iso)

    repo_root = Path(__file__).resolve().parent.parent
    out_path = repo_root / "docs" / "informe" / "commit-info.tex"
    contenido = (
        "% Generado automaticamente por scripts/gen-informe-commit-info.py\n"
        "% a partir del commit HEAD real. NO editar a mano; no se versiona.\n"
        f"\\renewcommand{{\\commitactual}}{{{hash_corto}}}\n"
        f"\\renewcommand{{\\fechaactual}}{{Quevedo, {fecha}}}\n"
    )
    out_path.write_text(contenido, encoding="utf-8")

    print(f"[gen-informe-commit-info] Generado {out_path} "
          f"(commit {hash_corto}, {fecha})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
