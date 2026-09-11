r"""Paso 4 del README de docs/requisitos: copia para firma.

Genera SRS-v1.0.0-para-firma.tex a partir del SRS.tex recien producido,
reutilizando VERBATIM el bloque de metadatos institucionales, la portada
(universidad, facultad, carrera, docente-director, integrantes con correo y
ORCID, URL del repositorio) y la seccion de aprobacion que ya existian en la
version anterior del archivo para firma. Lo unico que se actualiza de la
portada es la fecha de emision.

Uso: python3 paso4_para_firma.py <dir-docs/requisitos> <fecha-ciudad>
"""
import pathlib
import re
import sys

d = pathlib.Path(sys.argv[1])
ciudad_fecha = sys.argv[2]

base = d / "SRS.tex"
firma = d / "SRS-v1.0.0-para-firma.tex"
anterior = firma.read_text(encoding="utf-8")
t = base.read_text(encoding="utf-8")

# --- 1. Extraer de la version anterior los bloques que deben conservarse ---
m_meta = re.search(
    r"(% --- Metadatos institucionales.*?\\newcommand\{\\ciudadfecha\}\{[^}]*\})",
    anterior, re.S)
if not m_meta:
    sys.exit("No se encontro el bloque de metadatos institucionales previo.")
meta = m_meta.group(1)

m_port = re.search(
    r"(% === PORTADA INSTITUCIONAL.*?\\end\{titlepage\})", anterior, re.S)
if not m_port:
    sys.exit("No se encontro la portada institucional previa.")
portada = m_port.group(1)

m_aprob = re.search(
    r"(\\section\{APROBACIÓN DEL DOCENTE-DIRECTOR\}.*?)\n\\end\{document\}",
    anterior, re.S)
if not m_aprob:
    sys.exit("No se encontro la seccion de aprobacion previa.")
aprobacion = m_aprob.group(1)

# Comprobaciones de integridad: no se puede perder ningun dato de la portada
OBLIGATORIOS = [
    "Universidad T", "Facultad de Ciencias de la Computaci", "Carrera de Software",
    "Guerrero Ulloa Gleiston Ciceron", "Fred Adri", "Jaime Josu", "Zaida Melissa",
    "fbeltranm@uteq.edu.ec", "jmariscalc@uteq.edu.ec", "ztaipem@uteq.edu.ec",
    "0009-0007-8303-2137", "0009-0007-2206-3941", "0009-0000-1227-3258",
    "https://github.com/Grinjoww/ENTREGA-FINAL-BIOPET",
]
for req in OBLIGATORIOS:
    if req not in meta + portada:
        sys.exit("La portada perderia un dato obligatorio: %r" % req)

# --- 2. Actualizar unicamente la fecha de la portada -----------------------
meta = re.sub(r"\\newcommand\{\\ciudadfecha\}\{[^}]*\}",
              "\\\\newcommand{\\\\ciudadfecha}{%s}" % ciudad_fecha, meta)

# --- 3. Insertar metadatos y portada en el .tex nuevo ---------------------
if "\\date{}" not in t:
    sys.exit("No se encontro \\date{} en SRS.tex.")
t = t.replace("\\date{}", "\\date{}\n\n" + meta, 1)

if "\\begin{document}" not in t:
    sys.exit("No se encontro \\begin{document} en SRS.tex.")
t = t.replace("\\begin{document}", "\\begin{document}\n\n" + portada, 1)

# --- 4. Reemplazar el parrafo de estado de aprobacion ---------------------
m_estado_ant = re.search(
    r"\\textbf\{Estado de aprobación:\} documento preparado.*?\n\n", anterior, re.S)
if not m_estado_ant:
    sys.exit("No se encontro el parrafo de estado de aprobacion previo.")
estado_firma = m_estado_ant.group(0)

m_estado_new = re.search(
    r"\\textbf\{Estado de aprobación:\} este documento .*?\n\n", t, re.S)
if not m_estado_new:
    sys.exit("No se encontro el parrafo de estado de aprobacion en SRS.tex.")
t = t[:m_estado_new.start()] + estado_firma + t[m_estado_new.end():]

# --- 5. Anexar la seccion de aprobacion ----------------------------------
t = t.replace("\\end{document}", aprobacion + "\n\n\\end{document}", 1)

firma.write_text(t, encoding="utf-8")
print("SRS-v1.0.0-para-firma.tex regenerado (portada, firmas y fecha %s)." % ciudad_fecha)
