r"""Paso 2 del README de docs/requisitos: ajustes de preambulo para pdfTeX.

Inserta, justo despues de \author{} en el .tex generado por pandoc:
  a) el mapeo newunicodechar de los simbolos que pdfTeX clasico no resuelve;
  b) hyphenat con la opcion [htt], para que pdfTeX pueda partir tambien los
     identificadores y rutas largas compuestos en \texttt{} (nombres de
     pruebas, clases y rutas del repositorio), que de otro modo se salen del
     margen.

Uso: python3 paso2_preambulo.py <ruta-al-.tex>
"""
import pathlib
import sys
import unicodedata

tex = pathlib.Path(sys.argv[1])
t = tex.read_text(encoding="utf-8")

MAPA = {
    "\u2265": r"\ensuremath{\geq}",
    "\u2264": r"\ensuremath{\leq}",
    "\u2192": r"\ensuremath{\rightarrow}",
    "\u2194": r"\ensuremath{\leftrightarrow}",
    "\u2212": r"\ensuremath{-}",
    "\u00d7": r"\ensuremath{\times}",
}

bloque = [
    "",
    "% --- Corte de linea en identificadores y rutas largas --------------------",
    "% Los nombres de pruebas, clases y rutas del repositorio se componen en",
    "% \\texttt{} y con frecuencia son mas anchos que la linea; sin esto pdfTeX",
    "% no puede partirlos y el texto se sale del margen.",
    "\\usepackage[htt]{hyphenat}",
    "",
    "% --- Simbolos unicode que pdfTeX clasico no resuelve por defecto ---------",
    "\\usepackage{newunicodechar}",
]
for c, r in MAPA.items():
    bloque.append("\\newunicodechar{%s}{%s}" % (c, r))
bloque.append("")

if "newunicodechar" in t or "hyphenat" in t:
    sys.exit("El .tex ya tiene los ajustes de preambulo; abortado para no duplicar.")
if "\\author{}" not in t:
    sys.exit("No se encontro \\author{} en el .tex.")

t = t.replace("\\author{}", "\\author{}\n" + "\n".join(bloque), 1)
tex.write_text(t, encoding="utf-8")

seguros = set(MAPA) | {"\u2018", "\u2019", "\u201c", "\u201d", "\u2013",
                       "\u2014", "\u2026", "\u00a0", "\u00ad"}
problema = {}
for ch in t:
    if ord(ch) > 0x017F and ch not in seguros:
        problema[ch] = problema.get(ch, 0) + 1

print("Preambulo ajustado: hyphenat[htt] + newunicodechar (%d mapeos)." % len(MAPA))
if problema:
    print("CARACTERES UNICODE SIN MAPEAR:")
    for ch, n in sorted(problema.items()):
        print("  U+%04X (%s) x%d" % (ord(ch), unicodedata.name(ch, "?"), n))
    sys.exit(1)
print("Sin caracteres unicode sin mapear.")
