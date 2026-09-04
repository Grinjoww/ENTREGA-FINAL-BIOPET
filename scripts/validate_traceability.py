import csv
import re
import sys

SRS = 'docs/requisitos/SRS.md'
MATRIZ = 'docs/trazabilidad/matriz.csv'
HISTORIAS = 'docs/requisitos/historias/HistoriasUsuario.md'
CASOS = 'docs/requisitos/casos-de-uso/CasosDeUso.md'

errores = []

# 1. Requisitos en SRS
srs_text = open(SRS, encoding='utf-8').read()
ids_srs = set(re.findall(r'REQ-(?:F|NF)-\d{3}', srs_text))

# 2. Filas matriz
with open(MATRIZ, newline='', encoding='utf-8') as f:
    filas = list(csv.DictReader(f))

ids_matriz = [fila['id_requisito'].strip() for fila in filas]
ids_matriz_set = set(ids_matriz)

# 2a. Duplicados
vistos = set()
for rid in ids_matriz:
    if rid in vistos:
        errores.append('Duplicado: ' + rid)
    vistos.add(rid)

# 2b. SRS en matriz
faltan = ids_srs - ids_matriz_set
for rid in sorted(faltan):
    errores.append(rid + ' en SRS pero no en matriz')

# 2c. Matriz no inventa
sobran = ids_matriz_set - ids_srs
for rid in sorted(sobran):
    errores.append(rid + ' en matriz pero no en SRS')

# 3. Cada fila con respaldo
for fila in filas:
    rid = fila['id_requisito'].strip()
    hu = fila.get('historia_usuario', '').strip()
    cu = fila.get('caso_de_uso', '').strip()
    prueba = fila.get('prueba_automatizada', '').strip()
    if not (hu or cu or prueba):
        errores.append(rid + ' sin historia/caso/prueba')

# 4. HU/CU existen
historias_texto = open(HISTORIAS, encoding='utf-8').read()
casos_texto = open(CASOS, encoding='utf-8').read()
hu_declaradas = set(re.findall(r'^## (HU-\d{3})', historias_texto, re.MULTILINE))
cu_declarados = set(re.findall(r'^## (CU-\d{2,3})', casos_texto, re.MULTILINE))

for fila in filas:
    rid = fila['id_requisito'].strip()
    hu = fila.get('historia_usuario', '').strip()
    cu = fila.get('caso_de_uso', '').strip()
    if hu:
        for hu_id in [h.strip() for h in hu.split(',') if h.strip()]:
            if hu_id not in hu_declaradas:
                errores.append(rid + ' ref HU ' + hu_id + ' inexistente')
    if cu:
        for cu_id in [c.strip() for c in cu.split(',') if c.strip()]:
            if cu_id not in cu_declarados:
                errores.append(rid + ' ref CU ' + cu_id + ' inexistente')

# 5. Must con estado
for fila in filas:
    if fila.get('prioridad_moscow', '').strip() == 'Must' and not fila.get('estado', '').strip():
        errores.append(fila['id_requisito'] + ' es Must sin estado')

if errores:
    print('VALIDACION FALLIDA:')
    for e in errores:
        print('  - ' + e)
    print('Total: ' + str(len(errores)))
    sys.exit(1)
else:
    print('VALIDACION OK: ' + str(len(ids_srs)) + ' requisitos del SRS, ' + str(len(filas)) + ' filas en matriz.csv, ' + str(len(hu_declaradas)) + ' historias y ' + str(len(cu_declarados)) + ' casos de uso consistentes entre sí.')