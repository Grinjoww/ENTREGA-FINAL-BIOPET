#!/usr/bin/env python3
"""
scripts/analisis-sus.py

Script de análisis estadístico de la prueba de usabilidad SUS (System
Usability Scale) del sistema BIOPET.

Qué hace este script (verificado por auditoría, 2026-08-31):
  - Procesa registros PREVIAMENTE EXISTENTES en
    docs/mediciones/sus/sus-raw.csv (una fila por participante, con las
    10 respuestas SUS originales en escala 1-5 y el código de
    participante P01..P18).
  - NO genera participantes, respuestas ni puntajes: no hay generación
    de datos de ningún tipo en este archivo.
  - NO utiliza aleatoriedad: no importa `random` ni ninguna biblioteca
    de generación pseudoaleatoria. Para una misma entrada (mismo CSV),
    produce siempre la misma salida.
  - Calcula el puntaje SUS de cada participante directamente a partir de
    las respuestas Q1-Q10, con la fórmula estándar de Brooke (1996), y
    valida ese cálculo contra el valor `sus_score` ya almacenado en el
    CSV (ver `calcular_puntaje_sus` y `cargar_datos`). Si algún registro
    no coincide, el script se detiene con un error que identifica el
    código de participante afectado, en vez de continuar silenciosamente
    con un dato inconsistente.
  - Calcula estadísticas agregadas deterministas sobre los puntajes
    validados (media, desviación típica muestral, intervalo de confianza
    al 95 % vía t de Student, mediana por interpolación) y escribe
    docs/mediciones/sus/REPORT.md.

Una versión anterior de este docstring afirmaba que existía una
"semilla fija SEED=42 usada en la generación de datos sintéticos
didácticos". Esa afirmación no correspondía al comportamiento real del
script (nunca existió código de generación en este archivo, en ninguna
versión de su historial) y fue retirada en esta auditoría; no se
reconstruye aquí ninguna explicación histórica de por qué apareció esa
redacción, porque no puede verificarse desde el repositorio.

Uso:
    python3 scripts/analisis-sus.py

Dependencias: solo librería estándar de Python 3 (statistics, csv, math).
"""

import csv
import math
import statistics
import datetime
from pathlib import Path

RUTA_CSV = Path("docs/mediciones/sus/sus-raw.csv")
RUTA_REPORT = Path("docs/mediciones/sus/REPORT.md")

# Orden e identidad de las 10 preguntas SUS tal como están nombradas en
# sus-raw.csv, con su paridad (impar/par) según la fórmula de Brooke.
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

# Tolerancia para la comparación entre el puntaje calculado y el
# almacenado (ambos son múltiplos de 2.5; una diferencia mayor a esto
# indica una inconsistencia real, no un error de redondeo de punto
# flotante).
TOLERANCIA_SUS_SCORE = 1e-6

# Valores críticos t de Student al 95% de confianza (dos colas), tabla estándar
# indexada por grados de libertad (n-1). Se usa el valor exacto para el tamaño
# de muestra real de sus-raw.csv; si algún día df no está en la tabla, se usa
# el valor tabulado más cercano por defecto (conservador: el de df menor).
T_CRITICO_95 = {
    5: 2.571, 6: 2.447, 7: 2.365, 8: 2.306, 9: 2.262, 10: 2.228,
    11: 2.201, 12: 2.179, 13: 2.160, 14: 2.145, 15: 2.131, 16: 2.120,
    17: 2.110, 18: 2.101, 19: 2.093, 20: 2.086, 24: 2.064, 29: 2.045,
    30: 2.042, 40: 2.021, 60: 2.000, 120: 1.980,
}


def t_critico(df: int) -> float:
    if df in T_CRITICO_95:
        return T_CRITICO_95[df]
    disponibles = sorted(T_CRITICO_95)
    menores = [d for d in disponibles if d <= df]
    return T_CRITICO_95[max(menores)] if menores else T_CRITICO_95[disponibles[0]]


def calcular_puntaje_sus(respuestas: dict) -> float:
    """Calcula el puntaje SUS (0-100) de un participante a partir de sus
    10 respuestas Likert (1-5), siguiendo la fórmula estándar de Brooke
    (1996), ya documentada en docs/mediciones/sus/instrumento-sus.md:

      - Ítems impares (1, 3, 5, 7, 9): contribución = valor - 1.
      - Ítems pares   (2, 4, 6, 8, 10): contribución = 5 - valor.
      - Se suman las diez contribuciones (rango 0-40) y se multiplica
        por 2.5, dando un puntaje final en el rango 0-100.

    `respuestas` debe tener las 10 claves de COLUMNAS_SUS con valores
    enteros 1-5.
    """
    total = 0
    for indice, columna in enumerate(COLUMNAS_SUS, start=1):
        valor = int(respuestas[columna])
        if indice % 2 == 1:  # ítem impar
            total += valor - 1
        else:  # ítem par
            total += 5 - valor
    return total * 2.5


def cargar_datos(ruta: Path):
    """Lee sus-raw.csv, calcula el puntaje SUS de cada fila desde Q1-Q10
    y lo valida contra el `sus_score` ya almacenado en el CSV. Usa el
    valor CALCULADO (no el almacenado) para el resto del análisis. Si
    algún registro no coincide dentro de la tolerancia, detiene la
    ejecución con un error que identifica el código de participante
    afectado, en vez de seguir adelante con un dato sin validar."""
    filas = []
    with ruta.open(newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            calculado = calcular_puntaje_sus(row)
            almacenado = float(row["sus_score"])
            if abs(calculado - almacenado) > TOLERANCIA_SUS_SCORE:
                raise ValueError(
                    f"Inconsistencia en sus_score para el participante "
                    f"{row['codigo_participante']}: calculado desde Q1-Q10 = "
                    f"{calculado}, almacenado en el CSV = {almacenado}. "
                    "Revisa la fila correspondiente en sus-raw.csv antes de "
                    "continuar; este script no continúa con datos sin validar."
                )
            row["sus_score"] = calculado
            row["edad"] = int(row["edad"])
            filas.append(row)
    return filas


def intervalo_confianza_95(scores):
    n = len(scores)
    media = statistics.mean(scores)
    desviacion = statistics.stdev(scores)  # desviación típica muestral (n-1)
    error_estandar = desviacion / math.sqrt(n)
    t_crit = t_critico(n - 1)
    margen = t_crit * error_estandar
    return media, desviacion, (media - margen, media + margen), margen, t_crit


def percentil(scores, p):
    """Percentil por interpolación lineal (método usado por numpy 'linear')."""
    datos = sorted(scores)
    n = len(datos)
    if n == 1:
        return datos[0]
    k = (n - 1) * (p / 100)
    f = math.floor(k)
    c = math.ceil(k)
    if f == c:
        return datos[int(k)]
    d0 = datos[int(f)] * (c - k)
    d1 = datos[int(c)] * (k - f)
    return d0 + d1


def clasificacion_sus(score):
    """Clasificación cualitativa estándar (Bangor, Kortum & Miller 2009 / escala de adjetivos SUS)."""
    if score >= 84.1:
        return "Excelente"
    elif score >= 68:
        return "Bueno"
    elif score >= 51:
        return "Aceptable (marginal)"
    elif score >= 25:
        return "Pobre"
    else:
        return "Deficiente"


def generar_reporte(filas):
    scores = [f["sus_score"] for f in filas]
    n = len(scores)
    media, desviacion, (ic_inf, ic_sup), margen, t_crit = intervalo_confianza_95(scores)
    p50 = percentil(scores, 50)
    minimo = min(scores)
    maximo = max(scores)
    codigos_ordenados = sorted(filas, key=lambda f: f["codigo_participante"])
    primer_codigo = codigos_ordenados[0]["codigo_participante"]
    ultimo_codigo = codigos_ordenados[-1]["codigo_participante"]
    peor_participante = min(filas, key=lambda f: f["sus_score"])

    lineas = []
    lineas.append("# Reporte de usabilidad — System Usability Scale (SUS)")
    lineas.append("")
    lineas.append(f"**Sistema evaluado:** BIOPET — Sistema Integral de Gestión Veterinaria")
    lineas.append(f"**Fecha del análisis:** {datetime.date.today().isoformat()}")
    lineas.append(f"**Fuente de datos:** `docs/mediciones/sus/sus-raw.csv`")
    lineas.append(
        f"**Script de análisis:** `scripts/analisis-sus.py` — calcula el "
        "puntaje SUS de cada participante desde sus respuestas Q1-Q10 "
        "(fórmula de Brooke) y lo valida contra `sus_score`; determinista, "
        "sin generación de datos ni aleatoriedad."
    )
    lineas.append(f"**Instrumento:** System Usability Scale de Brooke (1996), 10 ítems, escala Likert de 5 puntos, sin modificar.")
    lineas.append(f"**Tamaño de muestra:** n = {n} registros (códigos {primer_codigo}–{ultimo_codigo}).")
    lineas.append("")
    lineas.append("## Procedencia y estado de la evidencia")
    lineas.append("")
    lineas.append(
        "- **Hecho técnicamente verificable:** existen "
        f"{n} registros ({primer_codigo}–{ultimo_codigo}) en `sus-raw.csv`, "
        "con sus 10 respuestas Q1-Q10 archivadas; el puntaje `sus_score` de "
        "cada registro es matemáticamente reproducible desde esas respuestas "
        "(verificado por este mismo script, que se detiene con error si "
        "alguno no coincide); las estadísticas agregadas de este reporte se "
        "calculan de forma determinista a partir de esos puntajes."
    )
    lineas.append(
        "- **Declaración del equipo:** los registros corresponden a "
        "participantes reales a quienes se aplicó la evaluación durante el "
        "desarrollo del proyecto."
    )
    lineas.append(
        "- **Limitación documental:** actualmente no se dispone de "
        "evidencia verificable de los formularios individuales de "
        "consentimiento informado que la documentación original de este "
        "proyecto afirmaba conservar fuera del repositorio. Esta situación "
        "se documentó mediante una constancia de regularización firmada por "
        "los responsables del proyecto "
        "(`docs/etica/regularizacion-sus/CONSTANCIA-REGULARIZACION-SUS-BIOPET-2026-08-31.pdf`), "
        "la cual **no sustituye** consentimientos individuales — ver "
        "`docs/etica/regularizacion-sus/README.md` y `docs/etica/ETHICS.md`."
    )
    lineas.append(
        "- Tarea común de onboarding realizada por cada participante: inicio "
        "de sesión, alta de una mascota, edición de sus datos, eliminación "
        "lógica y cierre de sesión."
    )
    lineas.append(
        "- Cuestionario SUS de 10 preguntas originales aplicado inmediatamente "
        "después de completar la tarea."
    )
    lineas.append("")
    lineas.append("## Resultados agregados")
    lineas.append("")
    lineas.append(f"| Métrica | Valor |")
    lineas.append(f"|---|---|")
    lineas.append(f"| Media (SUS Score) | **{media:.2f}** / 100 |")
    lineas.append(f"| Desviación típica (muestral, n-1) | {desviacion:.2f} |")
    lineas.append(f"| Intervalo de confianza 95 % | [{ic_inf:.2f}, {ic_sup:.2f}] (margen ± {margen:.2f}) |")
    lineas.append(f"| Mediana (p50) | {p50:.2f} |")
    lineas.append(f"| Mínimo | {minimo:.2f} |")
    lineas.append(f"| Máximo | {maximo:.2f} |")
    lineas.append(f"| Clasificación cualitativa de la media | **{clasificacion_sus(media)}** (escala de adjetivos Bangor, Kortum & Miller 2009) |")
    lineas.append("")
    lineas.append("> Nota metodológica: el intervalo de confianza se calculó con la distribución")
    lineas.append(f"> t de Student para n-1 = {n-1} grados de libertad (t_crítico = {t_crit}), ")
    lineas.append("> apropiado para muestras pequeñas (n < 30), en lugar de la aproximación normal (z).")
    lineas.append("")
    lineas.append("## Resultados por participante")
    lineas.append("")
    lineas.append("| Código | Edad | Sexo | Experiencia web | Dispositivo | Puntaje SUS |")
    lineas.append("|---|---|---|---|---|---|")
    for f in filas:
        lineas.append(f"| {f['codigo_participante']} | {f['edad']} | {f['sexo']} | {f['experiencia_web']} | {f['dispositivo']} | {f['sus_score']:.1f} |")
    lineas.append("")
    lineas.append("## Distribución de la muestra (variables demográficas)")
    lineas.append("")
    edades = [f["edad"] for f in filas]
    lineas.append(f"- Edad: media {statistics.mean(edades):.1f} años (rango {min(edades)}–{max(edades)}).")
    sexos = {}
    for f in filas:
        sexos[f["sexo"]] = sexos.get(f["sexo"], 0) + 1
    lineas.append(f"- Sexo: " + ", ".join(f"{k}={v}" for k, v in sexos.items()) + ".")
    experiencias = {}
    for f in filas:
        experiencias[f["experiencia_web"]] = experiencias.get(f["experiencia_web"], 0) + 1
    lineas.append(f"- Experiencia previa con aplicaciones web: " + ", ".join(f"{k}={v}" for k, v in experiencias.items()) + ".")
    dispositivos = {}
    for f in filas:
        dispositivos[f["dispositivo"]] = dispositivos.get(f["dispositivo"], 0) + 1
    lineas.append(f"- Dispositivo utilizado: " + ", ".join(f"{k}={v}" for k, v in dispositivos.items()) + ".")
    lineas.append("")
    lineas.append("## Interpretación")
    lineas.append("")
    lineas.append(
        f"La media obtenida ({media:.2f}) se ubica en la categoría **{clasificacion_sus(media)}** "
        "de la escala de adjetivos SUS, con un intervalo de confianza al 95 % que "
        f"{'no incluye' if ic_inf > 68 else 'incluye'} el umbral de referencia de 68 puntos "
        "(considerado 'por encima del promedio' en la literatura de Bangor et al., 2008). "
    )
    lineas.append(
        f"El participante con menor puntaje ({peor_participante['codigo_participante']}, "
        f"{peor_participante['sus_score']:.1f}) declaró experiencia web "
        f"'{peor_participante['experiencia_web']}', lo que es consistente con la literatura de "
        "usabilidad: la curva de aprendizaje inicial afecta más a usuarios sin experiencia "
        "digital previa. Se recomienda para la Entrega Final incorporar una fase de orientación "
        "breve antes de la tarea para usuarios de perfil similar."
    )
    lineas.append("")
    lineas.append("## Amenazas a la validez")
    lineas.append("")
    umbral_texto = (
        f"Tamaño de muestra n={n}, por encima del mínimo de 15 recomendado para la Entrega "
        "Final; el margen de error del intervalo de confianza se redujo respecto a la muestra "
        "inicial (n=10) de la Tercera Entrega."
        if n >= 15 else
        f"Tamaño de muestra n={n}, todavía por debajo del mínimo de 15 recomendado para la "
        "Entrega Final; estimaciones estables pero con margen de error todavía amplio."
    )
    lineas.append(f"- {umbral_texto}")
    lineas.append("- Participantes reclutados por conveniencia (círculo cercano al equipo), no aleatorizados; posible sesgo de complacencia.")
    lineas.append("- Prueba realizada en un único entorno controlado; no se evaluó variabilidad de red o dispositivos de gama baja.")
    lineas.append(
        "- La procedencia de los participantes como personas reales es una "
        "declaración del equipo, no un hecho verificable de forma "
        "independiente desde el repositorio; los formularios individuales de "
        "consentimiento informado mencionados en la documentación original no "
        "están actualmente disponibles como evidencia — ver "
        "`docs/etica/regularizacion-sus/`."
    )
    lineas.append("")

    RUTA_REPORT.parent.mkdir(parents=True, exist_ok=True)
    RUTA_REPORT.write_text("\n".join(lineas), encoding="utf-8")
    print(f"Reporte generado en {RUTA_REPORT}")
    print(f"Media: {media:.2f} | DE: {desviacion:.2f} | IC95%: [{ic_inf:.2f}, {ic_sup:.2f}]")


if __name__ == "__main__":
    datos = cargar_datos(RUTA_CSV)
    generar_reporte(datos)
