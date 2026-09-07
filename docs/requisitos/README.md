# Requisitos — cómo regenerar `SRS-v1.0.0.pdf` y `SRS-v1.0.0-para-firma.pdf`

`docs/requisitos/SRS.md` es la fuente editable única. `SRS.tex`,
`SRS-v1.0.0.pdf`, `SRS-v1.0.0-para-firma.tex` y
`SRS-v1.0.0-para-firma.pdf` son derivados y deben regenerarse desde ahí
después de cualquier cambio al `.md` (nunca editar `SRS.tex` a mano).

Para ejecutar todos los comandos desde `docs/requisitos/` (PowerShell en
Windows; con git-bash usar los mismos binarios y rutas adaptadas), hemos realizado los siguientes pasos:

## 1. Markdown -> LaTeX (pandoc)

Pandoc portable oficial (descargar de
https://github.com/jgm/pandoc/releases). Con pandoc en `PATH`:

```powershell
pandoc SRS.md -o SRS.tex --toc --toc-depth=3 -V geometry:margin=2.5cm -V lang=es --standalone
```

## 2. Compatibilidad de unicode

pdfTeX clásico no resuelve ≥ (U+2265), → (U+2192) ni ↔ (U+2194) por
defecto. Insertar antes de `\author{}` en el `SRS.tex` generado:

```latex
\usepackage{newunicodechar}
\newunicodechar{≥}{\ensuremath{\geq}}
\newunicodechar{→}{\ensuremath{\rightarrow}}
\newunicodechar{↔}{\ensuremath{\leftrightarrow}}
```

## 3. LaTeX -> PDF oficial (3 pasadas)

Las 3 pasadas son necesarias por la tabla de contenidos y las referencias
cruzadas de la sección 3.3/4.1:

```powershell
pdflatex -interaction=nonstopmode SRS.tex
pdflatex -interaction=nonstopmode SRS.tex
pdflatex -interaction=nonstopmode SRS.tex
Copy-Item SRS.pdf SRS-v1.0.0.pdf -Force
```

## 4. PDF para firma `SRS-v1.0.0-para-firma.pdf`

Este es el documento que se envía para la firma del docente-director. Se
parte del `SRS.tex` recién generado (paso 1).

**Estado de firma:** ninguno de los dos PDFs está firmado por el
docente-director. Ver "Estado de aprobación" en `SRS.md`. No generar ni
declarar una versión "firmada" sin la firma real recibida.
