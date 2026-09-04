# Requisitos — cómo regenerar `SRS-v1.0.0.pdf`

`docs/requisitos/SRS.md` es la fuente editable única. `SRS.tex` y
`SRS-v1.0.0.pdf` son derivados y deben regenerarse desde ahí después de
cualquier cambio al `.md` (nunca editar `SRS.tex` a mano).

```bash
# 1. Markdown -> LaTeX (pandoc)
pandoc docs/requisitos/SRS.md -o docs/requisitos/SRS.tex \
  --toc --toc-depth=3 -V geometry:margin=2.5cm -V lang=es --standalone

# 2. Compatibilidad de unicode: pdfTeX clásico no resuelve ≥ (U+2265),
#    → (U+2192) ni ↔ (U+2194) por defecto. Insertar antes de \author{}:
#      \usepackage{newunicodechar}
#      \newunicodechar{≥}{\ensuremath{\geq}}
#      \newunicodechar{→}{\ensuremath{\rightarrow}}
#      \newunicodechar{↔}{\ensuremath{\leftrightarrow}}

# 3. LaTeX -> PDF (3 pasadas, por la tabla de contenidos y las referencias
#    cruzadas de la sección 3.3/4.1)
pdflatex -interaction=nonstopmode SRS.tex
pdflatex -interaction=nonstopmode SRS.tex
pdflatex -interaction=nonstopmode SRS.tex
# Renombrar/copiar la salida a SRS-v1.0.0.pdf
```

Herramientas usadas para generar la revisión v1.0.0 (2026-09-03): pandoc
3.5 (binario portable oficial, sin instalar en el sistema) y MiKTeX
(pdfTeX 4.18) ya presente en el equipo de trabajo. Cualquier distribución
TeX/pandoc equivalente produce el mismo resultado.

**Estado de firma:** `SRS-v1.0.0.pdf` no está firmado por el
docente-director. Ver "Estado de aprobación" en `SRS.md`/`SRS.tex`. No
generar ni declarar una versión "firmada" sin la firma real recibida.
