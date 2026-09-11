"""Auditoria mecanica del SRS de BIOPET contra el repositorio real.

Uso: python3 auditoria.py <ruta-al-SRS.md> <raiz-del-repositorio>
"""
import re
import sys
import pathlib
from collections import Counter

srs = pathlib.Path(sys.argv[1]).read_text(encoding="utf-8")
repo = pathlib.Path(sys.argv[2]).resolve()
errores = []

# --- 1. Bloques de requisito y campos obligatorios -------------------------
partes = re.split(r"^### (REQ-(?:F|NF)-\d{3}[ab]?) — (.+)$", srs, flags=re.M)
# Regla dura: el SRS no admite identificadores con sufijo de letra, porque
# romperian la igualdad de conjuntos con matriz.csv (ver seccion 1.3.2).
for sufijo in sorted(set(re.findall(r"REQ-(?:F|NF)-\d{3}[ab]", srs))):
    errores.append(f"Identificador con sufijo de letra prohibido: {sufijo}")
bloques = [(partes[i], partes[i + 1], partes[i + 2]) for i in range(1, len(partes), 3)]

CAMPOS = ["Tipo", "Prioridad", "Enunciado", "Rationale",
          "Criterios de aceptación", "Método de verificación",
          "Trazabilidad", "Estado"]
ESTADOS = {"verificado", "implementado", "pendiente"}

vistos, estados = set(), {}
for rid, _titulo, cuerpo in bloques:
    if rid in vistos:
        errores.append(f"ID duplicado como bloque: {rid}")
    vistos.add(rid)
    for campo in CAMPOS:
        if not re.search(r"^- \*\*" + re.escape(campo) + r":\*\*", cuerpo, flags=re.M):
            errores.append(f"{rid}: FALTA el campo '{campo}'")
    m = re.search(r"^- \*\*Estado:\*\* (.+)$", cuerpo, flags=re.M)
    if m:
        est = m.group(1).strip()
        estados[rid] = est
        if est not in ESTADOS:
            errores.append(f"{rid}: estado invalido '{est}'")
    enun = re.search(r"^- \*\*Enunciado:\*\*(.+?)^- \*\*Rationale", cuerpo, flags=re.M | re.S)
    if enun and "deberá" not in enun.group(1) and "deberán" not in enun.group(1):
        errores.append(f"{rid}: el enunciado no usa 'debera/deberan'")

print(f"[1] Bloques de requisito: {len(bloques)} | campos obligatorios: {len(CAMPOS)} "
      f"| celdas auditadas: {len(bloques) * len(CAMPOS)}")

# --- 2. Referencias HU / CU -----------------------------------------------
hu_txt = (repo / "docs/requisitos/historias/HistoriasUsuario.md").read_text(encoding="utf-8")
cu_txt = (repo / "docs/requisitos/casos-de-uso/CasosDeUso.md").read_text(encoding="utf-8")
hu_ok = set(re.findall(r"^## (HU-\d{3})", hu_txt, flags=re.M))
cu_ok = set(re.findall(r"^## (CU-\d{2,3})", cu_txt, flags=re.M))
hu_ref = set(re.findall(r"\bHU-\d{3}\b", srs))
cu_ref = set(re.findall(r"\bCU-\d{2,3}\b", srs))
for ref in sorted(hu_ref - hu_ok):
    errores.append(f"Referencia colgante a historia inexistente: {ref}")
for ref in sorted(cu_ref - cu_ok):
    errores.append(f"Referencia colgante a caso de uso inexistente: {ref}")
print(f"[2] HU referenciadas: {len(hu_ref)} (existen {len(hu_ok)}) | "
      f"CU referenciados: {len(cu_ref)} (existen {len(cu_ok)})")

# --- 3. Rutas de archivo citadas ------------------------------------------
EXT = ("java|md|sql|yml|yaml|json|ts|html|css|js|txt|sha256|csv|cff|xml|pdf|png|"
       "dot|puml|sh|ps1|mjs|bib")
rutas = {m.group(1) for m in re.finditer(r"`([A-Za-z0-9_./-]+\.(?:" + EXT + r"))`", srs)}
con_dir = sorted(r for r in rutas if "/" in r and "*" not in r)
faltantes = [r for r in con_dir if not (repo / r).exists()]
for r in faltantes:
    errores.append(f"Ruta citada que no existe: {r}")
print(f"[3] Rutas de archivo citadas: {len(con_dir)} | inexistentes: {len(faltantes)}")

# --- 4. Directorios citados -----------------------------------------------
dirs = {m.group(1) for m in re.finditer(
    r"`((?:docs|Backend|frontend|db|database|scripts|k6|tooling)/[A-Za-z0-9_./-]*/)`", srs)}
dirs_falt = [d for d in sorted(dirs) if not (repo / d).is_dir()]
for d in dirs_falt:
    errores.append(f"Directorio citado que no existe: {d}")
print(f"[4] Directorios citados: {len(dirs)} | inexistentes: {len(dirs_falt)}")

# --- 5. Pruebas automatizadas citadas -------------------------------------
metodos_test, clases_test = set(), set()
for p in (repo / "Backend/src/test").rglob("*.java"):
    clases_test.add(p.stem)
    for m in re.finditer(r"\bvoid ([a-zA-Z0-9_]+)\s*\(", p.read_text(encoding="utf-8")):
        metodos_test.add(m.group(1))

metodos_prod, clases_prod = set(), set()
for p in (repo / "Backend/src/main/java").rglob("*.java"):
    clases_prod.add(p.stem)
    txt = p.read_text(encoding="utf-8")
    for m in re.finditer(
            r"(?:public|private|protected)\s+[\w<>,?\[\]. ]+?\s+([a-zA-Z0-9_]+)\s*\(", txt):
        metodos_prod.add(m.group(1))
    # metodos declarados en interfaces (sin modificador de acceso), p. ej. Spring Data
    for m in re.finditer(r"^\s{4}[\w<>,?\[\]. ]+?\s+([a-zA-Z0-9_]+)\s*\([^;{]*\)\s*;", txt, flags=re.M):
        metodos_prod.add(m.group(1))

citados = {(c, m) for c, m in re.findall(r"`([A-Z][A-Za-z0-9]*Test)\.([a-zA-Z0-9_]+)`", srs)
           if m != "java"}
for clase, met in sorted(citados):
    if clase not in clases_test:
        errores.append(f"Clase de prueba inexistente: {clase}")
    elif met not in metodos_test:
        errores.append(f"Metodo de prueba inexistente: {clase}.{met}")
for clase in sorted(set(re.findall(r"`([A-Z][A-Za-z0-9]*Test)`", srs))):
    if clase not in clases_test:
        errores.append(f"Clase de prueba inexistente: {clase}")
print(f"[5] Pruebas citadas clase.metodo: {len(citados)} | "
      f"metodos de prueba en el repo: {len(metodos_test)} | "
      f"metodos de produccion: {len(metodos_prod)}")

# --- 6. Clases y metodos de produccion citados como `Clase.metodo` --------
TIPOS_EXTERNOS = {
    "Rol", "EstadoCita", "HttpSecurity", "String", "Duration", "Instant",
    "Optional", "Page", "Pageable", "List", "Boolean", "Long", "Integer",
    "SessionCreationPolicy", "ParameterMode", "HttpStatus", "MediaType",
}
EXTENSIONES = {"java", "md", "sql", "yml", "yaml", "json", "ts", "html", "css",
               "js", "txt", "sha256", "csv", "xml", "pdf", "png", "dot", "puml",
               "sh", "ps1", "mjs", "bib", "cff"}
ref_prod = set(re.findall(r"`([A-Z][A-Za-z0-9]+)\.([a-zA-Z][a-zA-Z0-9_]*)\(?", srs))
malas = []
for clase, met in sorted(ref_prod):
    if clase.endswith("Test") or clase in TIPOS_EXTERNOS or met in EXTENSIONES:
        continue
    if clase not in clases_prod:
        malas.append(f"Clase de produccion inexistente: {clase}")
    elif met not in metodos_prod and met[0].islower():
        malas.append(f"Metodo de produccion inexistente: {clase}.{met}")
errores.extend(malas)
print(f"[6] Referencias `Clase.metodo` de produccion: {len(ref_prod)} | invalidas: {len(malas)}")

# --- 7. Coherencia con la tabla de control 3.4 ----------------------------
tabla = dict(re.findall(
    r"^\| (REQ-(?:F|NF)-\d{3}[ab]?) \|(?:[^|]*\|){7} ([a-z]+) \|$", srs, flags=re.M))
for r in sorted(set(estados) - set(tabla)):
    errores.append(f"{r} tiene bloque pero no fila en la tabla de control 3.4")
for r in sorted(set(tabla) - set(estados)):
    errores.append(f"{r} tiene fila en la tabla 3.4 pero no bloque")
for r in sorted(set(estados) & set(tabla)):
    if estados[r] != tabla[r]:
        errores.append(f"{r}: estado '{estados[r]}' en el bloque vs '{tabla[r]}' en la tabla 3.4")
print(f"[7] Filas en la tabla de control 3.4: {len(tabla)} | bloques: {len(estados)}")

# --- 8. Coherencia con la matriz de trazabilidad --------------------------
import csv
with (repo / "docs/trazabilidad/matriz.csv").open(newline="", encoding="utf-8") as f:
    filas = list(csv.DictReader(f))
ids_matriz = [fila["id_requisito"].strip() for fila in filas]
if len(ids_matriz) != len(set(ids_matriz)):
    errores.append("Hay identificadores duplicados en matriz.csv")
ids_srs_base = set(re.findall(r"REQ-(?:F|NF)-\d{3}", srs))
for r in sorted(ids_srs_base - set(ids_matriz)):
    errores.append(f"{r} esta en el SRS pero no tiene fila en matriz.csv")
for r in sorted(set(ids_matriz) - ids_srs_base):
    errores.append(f"{r} esta en matriz.csv pero no en el SRS")
# Regla declarada en la seccion 4 del SRS: si todas las partes de un
# identificador base comparten estado, ese es el estado de la fila; si
# difieren, la fila queda en "implementado".
partes_por_base = {}
for rid, est in estados.items():
    partes_por_base.setdefault(re.sub(r"[ab]$", "", rid), set()).add(est)
estados_base = {b: (s.pop() if len(s) == 1 else "implementado")
                for b, s in ((b, set(v)) for b, v in partes_por_base.items())}
for fila in filas:
    rid = fila["id_requisito"].strip()
    est = fila["estado"].strip()
    if est not in ESTADOS:
        errores.append(f"matriz.csv: {rid} usa el estado invalido '{est}'")
    elif rid in estados_base and est != estados_base[rid]:
        errores.append(f"matriz.csv: {rid} estado '{est}' vs '{estados_base[rid]}' en el SRS")
    if not (fila["historia_usuario"].strip() or fila["caso_de_uso"].strip()
            or fila["prueba_automatizada"].strip()):
        errores.append(f"matriz.csv: {rid} sin historia, caso de uso ni prueba")
    for hu in [h.strip() for h in fila["historia_usuario"].split(",") if h.strip()]:
        if hu not in hu_ok:
            errores.append(f"matriz.csv: {rid} referencia {hu}, que no existe")
    for cu in [c.strip() for c in fila["caso_de_uso"].split(",") if c.strip()]:
        if cu not in cu_ok:
            errores.append(f"matriz.csv: {rid} referencia {cu}, que no existe")
print(f"[8] Filas en matriz.csv: {len(filas)} | IDs en el SRS: {len(ids_srs_base)}")

# --- 8b. Comparacion explicita de los tres conjuntos de identificadores ----
ids_bloques = set(estados)
ids_tabla34 = set(tabla)
ids_matriz_set = set(ids_matriz)
print()
print("    COMPARACION DE CONJUNTOS DE IDENTIFICADORES")
print(f"      IDs del SRS (bloques seccion 3) : {len(ids_bloques)}")
print(f"      IDs de la tabla de control 3.4  : {len(ids_tabla34)}")
print(f"      IDs de matriz.csv               : {len(ids_matriz_set)}")
for nombre, a, b in (("SRS - matriz", ids_bloques, ids_matriz_set),
                     ("matriz - SRS", ids_matriz_set, ids_bloques),
                     ("SRS - tabla 3.4", ids_bloques, ids_tabla34),
                     ("tabla 3.4 - SRS", ids_tabla34, ids_bloques)):
    dif = sorted(a - b)
    print(f"      {nombre:16}: {'VACIO' if not dif else dif}")
    if dif:
        errores.append(f"Conjunto {nombre} no esta vacio: {dif}")

# --- 8c. Contaminacion de otro proyecto -----------------------------------
contaminacion = re.findall(r"(?i)biopet[ -]?v2|biopetv2", srs)
if contaminacion:
    errores.append(f"El SRS menciona otro proyecto (BIOPET-V2): {len(contaminacion)} ocurrencia(s)")
print(f"[8c] Menciones a BIOPET-V2 en el SRS: {len(contaminacion)}")

# --- 8d. Trampas de formato Markdown que corrompen el PDF en silencio -----
# (i) Linea de continuacion de una lista que empieza por un marcador de lista:
#     pandoc la interpreta como un item nuevo, lo que parte los code spans y
#     el resto del parrafo sale con la tipografia invertida.
lineas_md = srs.splitlines()
trampas = []
dentro = False
for n, linea in enumerate(lineas_md, start=1):
    if not linea.strip():
        dentro = False
        continue
    if re.match(r"^\s*(?:[-*+]|\d+\.)\s", linea):
        dentro = True
        continue
    if dentro and re.match(r"^\s+[-*+]\s", linea):
        trampas.append((n, linea.strip()[:70]))
for n, l in trampas:
    errores.append(f"Linea {n}: continuacion de lista que empieza por marcador "
                   f"(pandoc la lee como item nuevo y rompe el parrafo): {l}")

# (ii) Numero impar de acentos graves en un bloque: code span sin cerrar.
impares = []
for bloque in re.split(r"\n\s*\n", srs):
    if bloque.lstrip().startswith("```"):
        continue
    if bloque.count("`") % 2:
        impares.append(bloque.strip().splitlines()[0][:70])
for b in impares:
    errores.append(f"Bloque con numero impar de acentos graves (code span sin cerrar): {b}")
print(f"[8d] Trampas de formato Markdown: {len(trampas)} continuacion(es) de lista, "
      f"{len(impares)} code span(s) sin cerrar")

# --- 9. Resumen por estado ------------------------------------------------
c = Counter(estados.values())
base = {re.sub(r"[ab]$", "", r) for r in estados}
print(f"[9] Estados: " + "  ".join(f"{k}={v}" for k, v in sorted(c.items()))
      + f" | identificadores base: {len(base)}")

# --- Resultado ------------------------------------------------------------
print()
if errores:
    print("AUDITORIA MECANICA: FALLOS")
    for e in errores:
        print("  -", e)
    print(f"\nTotal: {len(errores)} hallazgo(s).")
    sys.exit(1)
print("AUDITORIA MECANICA: OK - 0 hallazgos, 0 celdas FALTA.")
