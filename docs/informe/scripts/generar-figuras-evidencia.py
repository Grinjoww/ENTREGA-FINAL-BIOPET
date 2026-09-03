#!/usr/bin/env python3
"""
docs/informe/scripts/generar-figuras-evidencia.py

Regenera, desde evidencia CRUDA VERSIONADA (nunca desde valores
hardcodeados), las tres figuras de evidencia del capitulo "Pruebas y
calidad" del Informe Final de BIOPET que el docente identifico como
obsoletas (SUS, Maven, JaCoCo).

Ejecucion (desde la RAIZ del repositorio):

    python docs/informe/scripts/generar-figuras-evidencia.py

Fuentes de entrada (las unicas leidas; todas versionadas en Git):

    1. docs/mediciones/sus/sus-raw.csv
       18 registros de participantes con sus 10 respuestas Q1-Q10 y el
       puntaje SUS ya calculado. Este script RECALCULA cada puntaje desde
       Q1-Q10 (formula de Brooke, 1996) y lo valida contra la columna
       `sus_score`; si no coincide, se detiene con error (no continua con
       un dato sin validar). La media, desviacion tipica muestral,
       mediana e IC95% (t de Student) se calculan aqui mismo, no se citan
       de ningun reporte previo.

    2. docs/mediciones/sec/reproduccion-v1.0.0/mvn-clean-verify.txt
       Log RAW de `mvn clean verify` reproducido de forma independiente
       sobre el commit exacto del tag v1.0.0 (0d5cd525ce...). Se elige
       este archivo (y no Backend/target/, que no existe tras un clone
       limpio) porque es evidencia archivada y versionada. Se parsea la
       linea agregada final "[INFO] Tests run: N, Failures: F, Errors: E,
       Skipped: S" (la unica sin sufijo "-- in <clase>") y se confirma la
       presencia de "BUILD SUCCESS" en el propio log.

    3. docs/mediciones/jacoco/jacoco.csv
       Reporte CSV de JaCoCo (una fila por clase), generado por
       `jacoco-maven-plugin` y archivado por `scripts/archive-jacoco-evidence.sh`
       sin edicion manual. LINE y BRANCH se recalculan aqui sumando las
       columnas LINE_MISSED/LINE_COVERED y BRANCH_MISSED/BRANCH_COVERED de
       TODAS las filas (no se copia un porcentaje ya calculado); el numero
       de clases es, literalmente, el numero de filas de datos del CSV.

Ninguna cifra resultante (n, media, DT, mediana, IC95, total de pruebas,
LINE%, BRANCH%, numero de clases) esta escrita como constante en este
archivo: todas se derivan de los tres archivos anteriores en tiempo de
ejecucion. Las unicas constantes "duras" que contiene el script son
metodologicas y estan documentadas explicitamente: la tabla de valores
criticos t de Student (estandar, independiente de los datos) y los
umbrales de calidad del `pom.xml` historico (LINE >= 70 %, BRANCH >= 70 %).

Salida (se sobrescriben si ya existen; nunca se tocan las figuras
antiguas ni las de figuras/zaida/):

    docs/informe/figuras/jaime/06-sus-resultados-final.png
    docs/informe/figuras/jaime/07-maven-verify-final.png
    docs/informe/figuras/jaime/08-jacoco-resumen-final.png

Requisitos: Python 3.9+, matplotlib (backend "Agg", sin interfaz grafica;
NO usa seaborn). Determinista: misma entrada -> mismos PNG (bit a bit,
salvo metadatos de fecha de matplotlib). Falla con mensaje explicito y
codigo de salida distinto de 0 si falta un archivo de entrada o si los
datos son inconsistentes.
"""

from __future__ import annotations

import csv
import math
import re
import statistics
import sys
from pathlib import Path

import matplotlib

matplotlib.use("Agg")  # no GUI backend: funciona en Windows y Linux sin display
import matplotlib.pyplot as plt

# ---------------------------------------------------------------------
# Rutas (relativas a la raiz del repositorio; el script debe ejecutarse
# desde ahi, tal como documenta docs/informe/README.md).
# ---------------------------------------------------------------------
REPO_ROOT = Path(__file__).resolve().parents[3]
RUTA_SUS_CSV = REPO_ROOT / "docs/mediciones/sus/sus-raw.csv"
RUTA_MAVEN_LOG = REPO_ROOT / "docs/mediciones/sec/reproduccion-v1.0.0/mvn-clean-verify.txt"
RUTA_JACOCO_CSV = REPO_ROOT / "docs/mediciones/jacoco/jacoco.csv"

DIR_FIGURAS = REPO_ROOT / "docs/informe/figuras/jaime"
RUTA_FIG_SUS = DIR_FIGURAS / "06-sus-resultados-final.png"
RUTA_FIG_MAVEN = DIR_FIGURAS / "07-maven-verify-final.png"
RUTA_FIG_JACOCO = DIR_FIGURAS / "08-jacoco-resumen-final.png"

# ---------------------------------------------------------------------
# Constantes METODOLOGICAS (no son resultados de medicion; son criterios
# fijos, iguales a los ya documentados en scripts/analisis-sus.py y en
# Backend/pom.xml).
# ---------------------------------------------------------------------

# Valores criticos t de Student al 95% (dos colas), tabla estandar,
# indexada por grados de libertad (n-1). Idéntica a la usada en
# scripts/analisis-sus.py, para que el IC95% coincida exactamente con
# docs/mediciones/sus/REPORT.md.
T_CRITICO_95 = {
    5: 2.571, 6: 2.447, 7: 2.365, 8: 2.306, 9: 2.262, 10: 2.228,
    11: 2.201, 12: 2.179, 13: 2.160, 14: 2.145, 15: 2.131, 16: 2.120,
    17: 2.110, 18: 2.101, 19: 2.093, 20: 2.086, 24: 2.064, 29: 2.045,
    30: 2.042, 40: 2.021, 60: 2.000, 120: 1.980,
}

COLUMNAS_SUS = [
    "Q1_usaria_frecuentemente",
    "Q2_innecesariamente_complejo",
    "Q3_facil_de_usar",
    "Q4_necesito_soporte_tecnico",
    "Q5_funciones_bien_integradas",
    "Q6_demasiada_inconsistencia",
    "Q7_aprendizaje_rapido",
    "Q8_engorroso_de_usar",
    "Q9_confianza_al_usar",
    "Q10_necesito_aprender_mucho_antes",
]
TOLERANCIA_SUS_SCORE = 1e-6

# Umbrales de calidad declarados en Backend/pom.xml (jacoco-maven-plugin,
# execution "check"): LINE >= 70%, BRANCH >= 70%, COMPLEXITY >= 60%. Solo
# se usan LINE y BRANCH en la figura, tal como exige el docente (no se
# presenta COMPLEXITY con el mismo umbral que LINE/BRANCH).
UMBRAL_LINE = 70.0
UMBRAL_BRANCH = 70.0


class DatosInconsistentesError(RuntimeError):
    """Se lanza cuando el dato crudo no pasa una validacion interna."""


def _requerir_archivo(ruta: Path) -> Path:
    if not ruta.is_file():
        raise FileNotFoundError(
            f"No se encontro el archivo de evidencia requerido: {ruta}\n"
            "Este script SOLO lee evidencia versionada del repositorio; "
            "verifica que estas ejecutando desde la raiz del repositorio "
            "y que el archivo existe en el commit actual."
        )
    return ruta


# ---------------------------------------------------------------------
# 1. SUS
# ---------------------------------------------------------------------

def t_critico(df: int) -> float:
    if df in T_CRITICO_95:
        return T_CRITICO_95[df]
    disponibles = sorted(T_CRITICO_95)
    menores = [d for d in disponibles if d <= df]
    return T_CRITICO_95[max(menores)] if menores else T_CRITICO_95[disponibles[0]]


def _calcular_puntaje_sus(respuestas: dict) -> float:
    total = 0
    for indice, columna in enumerate(COLUMNAS_SUS, start=1):
        valor = int(respuestas[columna])
        if indice % 2 == 1:
            total += valor - 1
        else:
            total += 5 - valor
    return total * 2.5


def cargar_sus():
    ruta = _requerir_archivo(RUTA_SUS_CSV)
    filas = []
    with ruta.open(newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        columnas_faltantes = set(COLUMNAS_SUS + ["sus_score", "codigo_participante"]) - set(reader.fieldnames or [])
        if columnas_faltantes:
            raise DatosInconsistentesError(
                f"{ruta}: faltan columnas esperadas: {sorted(columnas_faltantes)}"
            )
        for row in reader:
            calculado = _calcular_puntaje_sus(row)
            almacenado = float(row["sus_score"])
            if abs(calculado - almacenado) > TOLERANCIA_SUS_SCORE:
                raise DatosInconsistentesError(
                    f"{ruta}: inconsistencia en sus_score para "
                    f"{row['codigo_participante']}: calculado desde Q1-Q10 = "
                    f"{calculado}, almacenado = {almacenado}."
                )
            filas.append(calculado)
    if not filas:
        raise DatosInconsistentesError(f"{ruta}: no contiene ningun registro de participante.")
    return filas


def estadisticas_sus(scores):
    n = len(scores)
    media = statistics.mean(scores)
    desviacion = statistics.stdev(scores)
    error_estandar = desviacion / math.sqrt(n)
    t_crit = t_critico(n - 1)
    margen = t_crit * error_estandar
    mediana = statistics.median(scores)
    return {
        "n": n,
        "mean": media,
        "sd": desviacion,
        "median": mediana,
        "ci_low": media - margen,
        "ci_high": media + margen,
    }


def generar_figura_sus(scores, stats):
    fig, ax = plt.subplots(figsize=(7, 5), dpi=150)
    bp = ax.boxplot(
        scores,
        vert=True,
        widths=0.35,
        patch_artist=True,
        showmeans=True,
        meanline=True,
    )
    for box in bp["boxes"]:
        box.set(facecolor="#a8d5e2", edgecolor="#1b4965")
    for median_line in bp["medians"]:
        median_line.set(color="#1b4965", linewidth=1.5)

    # Individual scores as jittered dots for visual distribution detail.
    import random

    rng = random.Random(0)  # fixed seed: purely cosmetic jitter, deterministic
    xs = [1 + rng.uniform(-0.08, 0.08) for _ in scores]
    ax.scatter(xs, scores, color="#1b4965", alpha=0.6, zorder=3, s=28)

    ax.set_ylim(0, 100)
    ax.set_ylabel("SUS score (0-100)")
    ax.set_xticks([1])
    ax.set_xticklabels(["BIOPET participants"])
    ax.set_title("System Usability Scale (SUS) results")

    summary = (
        f"n = {stats['n']}\n"
        f"Mean = {stats['mean']:.2f}\n"
        f"SD = {stats['sd']:.2f}\n"
        f"Median = {stats['median']:.2f}\n"
        f"95% CI = [{stats['ci_low']:.2f}, {stats['ci_high']:.2f}]"
    )
    ax.text(
        0.98, 0.02, summary,
        transform=ax.transAxes,
        fontsize=9,
        va="bottom", ha="right",
        bbox=dict(boxstyle="round", facecolor="white", edgecolor="#1b4965", alpha=0.9),
    )
    ax.grid(axis="y", linestyle=":", alpha=0.4)
    fig.tight_layout()
    DIR_FIGURAS.mkdir(parents=True, exist_ok=True)
    fig.savefig(RUTA_FIG_SUS)
    plt.close(fig)


# ---------------------------------------------------------------------
# 2. Maven
# ---------------------------------------------------------------------

RE_SUMMARY = re.compile(
    r"^\[INFO\] Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)\s*$"
)


def cargar_maven():
    ruta = _requerir_archivo(RUTA_MAVEN_LOG)
    texto = ruta.read_text(encoding="utf-8", errors="replace")
    lineas = texto.splitlines()

    resumenes = [m for m in (RE_SUMMARY.match(l) for l in lineas) if m]
    if not resumenes:
        raise DatosInconsistentesError(
            f"{ruta}: no se encontro ninguna linea agregada "
            "'[INFO] Tests run: N, Failures: F, Errors: E, Skipped: S' "
            "(sin sufijo de clase). No se puede derivar el total de pruebas."
        )
    # La linea agregada final del reactor es la ultima que aparece en el log.
    total, failures, errors, skipped = (int(g) for g in resumenes[-1].groups())

    if "BUILD SUCCESS" not in texto:
        raise DatosInconsistentesError(
            f"{ruta}: no contiene 'BUILD SUCCESS'; no se genera la figura "
            "con un build que no fue exitoso."
        )
    if failures != 0 or errors != 0:
        raise DatosInconsistentesError(
            f"{ruta}: el log reporta Failures={failures} Errors={errors}; "
            "se esperaba una suite completamente en verde."
        )

    return {"total": total, "failures": failures, "errors": errors, "skipped": skipped}


def generar_figura_maven(m):
    fig, ax = plt.subplots(figsize=(7, 3.2), dpi=150)
    ax.axis("off")

    ax.text(
        0.02, 0.85, "Backend test suite — mvn clean verify",
        fontsize=14, fontweight="bold", transform=ax.transAxes,
    )
    ax.text(
        0.02, 0.62,
        f"Total tests run: {m['total']}",
        fontsize=13, transform=ax.transAxes,
    )
    ax.text(
        0.02, 0.42,
        f"Failures: {m['failures']}    Errors: {m['errors']}    Skipped: {m['skipped']}",
        fontsize=12, transform=ax.transAxes,
    )
    ax.text(
        0.02, 0.15,
        "BUILD SUCCESS",
        fontsize=14, fontweight="bold", color="#1a7f37",
        transform=ax.transAxes,
        bbox=dict(boxstyle="round", facecolor="#e6f4ea", edgecolor="#1a7f37"),
    )
    fig.tight_layout()
    DIR_FIGURAS.mkdir(parents=True, exist_ok=True)
    fig.savefig(RUTA_FIG_MAVEN)
    plt.close(fig)


# ---------------------------------------------------------------------
# 3. JaCoCo
# ---------------------------------------------------------------------

def cargar_jacoco():
    ruta = _requerir_archivo(RUTA_JACOCO_CSV)
    filas = []
    with ruta.open(newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        requeridas = {"CLASS", "LINE_MISSED", "LINE_COVERED", "BRANCH_MISSED", "BRANCH_COVERED"}
        faltantes = requeridas - set(reader.fieldnames or [])
        if faltantes:
            raise DatosInconsistentesError(f"{ruta}: faltan columnas esperadas: {sorted(faltantes)}")
        for row in reader:
            filas.append(row)
    if not filas:
        raise DatosInconsistentesError(f"{ruta}: no contiene ninguna fila de clase.")

    line_missed = sum(int(r["LINE_MISSED"]) for r in filas)
    line_covered = sum(int(r["LINE_COVERED"]) for r in filas)
    branch_missed = sum(int(r["BRANCH_MISSED"]) for r in filas)
    branch_covered = sum(int(r["BRANCH_COVERED"]) for r in filas)

    line_total = line_missed + line_covered
    branch_total = branch_missed + branch_covered
    if line_total == 0 or branch_total == 0:
        raise DatosInconsistentesError(f"{ruta}: totales LINE/BRANCH en cero; datos incompletos.")

    return {
        "classes": len(filas),
        "line_pct": 100 * line_covered / line_total,
        "branch_pct": 100 * branch_covered / branch_total,
    }


def generar_figura_jacoco(j):
    fig, ax = plt.subplots(figsize=(6, 5), dpi=150)
    labels = ["LINE", "BRANCH"]
    values = [j["line_pct"], j["branch_pct"]]
    colors = ["#1b4965", "#5fa8d3"]

    bars = ax.bar(labels, values, color=colors, width=0.5)
    ax.axhline(UMBRAL_LINE, color="#c1121f", linestyle="--", linewidth=1.5,
               label=f"Quality gate threshold = {UMBRAL_LINE:.0f}%")
    ax.set_ylim(0, 100)
    ax.set_ylabel("Coverage (%)")
    ax.set_title(f"JaCoCo coverage summary — {j['classes']} classes")

    for bar, value in zip(bars, values):
        ax.text(
            bar.get_x() + bar.get_width() / 2, value + 2,
            f"{value:.2f}%", ha="center", va="bottom", fontsize=11, fontweight="bold",
        )

    ax.legend(loc="lower right", fontsize=9)
    ax.grid(axis="y", linestyle=":", alpha=0.4)
    fig.tight_layout()
    DIR_FIGURAS.mkdir(parents=True, exist_ok=True)
    fig.savefig(RUTA_FIG_JACOCO)
    plt.close(fig)


# ---------------------------------------------------------------------
# main
# ---------------------------------------------------------------------

def main() -> int:
    try:
        sus_scores = cargar_sus()
        sus_stats = estadisticas_sus(sus_scores)
        maven_stats = cargar_maven()
        jacoco_stats = cargar_jacoco()
    except (FileNotFoundError, DatosInconsistentesError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1

    generar_figura_sus(sus_scores, sus_stats)
    generar_figura_maven(maven_stats)
    generar_figura_jacoco(jacoco_stats)

    print("Figuras generadas desde evidencia versionada:")
    print(f"  SUS    -> n={sus_stats['n']} mean={sus_stats['mean']:.2f} "
          f"sd={sus_stats['sd']:.2f} median={sus_stats['median']:.2f} "
          f"CI95=[{sus_stats['ci_low']:.2f}, {sus_stats['ci_high']:.2f}] "
          f"-> {RUTA_FIG_SUS.relative_to(REPO_ROOT)}")
    print(f"  Maven  -> total={maven_stats['total']} failures={maven_stats['failures']} "
          f"errors={maven_stats['errors']} skipped={maven_stats['skipped']} "
          f"-> {RUTA_FIG_MAVEN.relative_to(REPO_ROOT)}")
    print(f"  JaCoCo -> classes={jacoco_stats['classes']} "
          f"LINE={jacoco_stats['line_pct']:.2f}% BRANCH={jacoco_stats['branch_pct']:.2f}% "
          f"-> {RUTA_FIG_JACOCO.relative_to(REPO_ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
