# Informe académico — BIOPET (Entrega Final, `v1.0.0`)

Este directorio contiene el código fuente LaTeX editable de **dos**
informes:

| Informe | `.tex` | `.pdf` | Estado |
|---|---|---|---|
| **Informe Final (canónico)** | `informe-final-v1.0.0.tex` | `informe-final-v1.0.0.pdf` (**89 páginas**, verificado con `pdfinfo`) | Vigente — es el que se evalúa en la Entrega Final |
| Tercera Entrega (histórico) | `informe-entrega-3.tex` | `informe-entrega-3.pdf` (50 páginas) | Congelado — se conserva sin modificar como registro histórico, ver [sección dedicada](#histórico--tercera-entrega) |

**Este README documenta, en primer lugar, cómo compilar el Informe
Final.** Las instrucciones equivalentes de la Tercera Entrega se
conservan más abajo, claramente separadas, porque ese `.tex` histórico
sigue existiendo en el repositorio y alguien podría necesitar
recompilarlo — pero no son el procedimiento vigente.

## Requisitos

- Una instalación de **TeX Live** o **MiKTeX** con BibTeX clásico.
  No se usa ningún paquete experimental ni que requiera herramientas
  externas (no hay `minted`/Pygments, no hay `shell-escape`).
- Paquetes usados por `informe-final-v1.0.0.tex` (todos estándar,
  incluidos en cualquier instalación completa de TeX Live o instalados
  automáticamente bajo demanda en MiKTeX): `babel`, `geometry`,
  `underscore`, `graphicx`, `float`, `booktabs`, `longtable`, `array`,
  `caption`, `enumitem`, `textcomp`, `amsmath`/`amssymb`, `pdflscape`,
  `xcolor`, `microtype`, `listings`, `fancyhdr`, `hyperref`.

---

## Compilación del Informe Final (`informe-final-v1.0.0.tex`)

### Opción recomendada: `latexmk`

```bash
cd docs/informe
latexmk -pdf -interaction=nonstopmode -halt-on-error informe-final-v1.0.0.tex
```

### Alternativa: `pdflatex` + `bibtex` manual (4 pasos)

```bash
cd docs/informe
pdflatex -interaction=nonstopmode informe-final-v1.0.0.tex
bibtex informe-final-v1.0.0
pdflatex -interaction=nonstopmode informe-final-v1.0.0.tex
pdflatex -interaction=nonstopmode informe-final-v1.0.0.tex
```

(Se ejecuta `pdflatex` dos veces al final para resolver referencias
cruzadas, índice de contenidos/figuras/tablas/listados y citas de forma
estable, además de la primera pasada.)

El resultado (`informe-final-v1.0.0.pdf`) se genera **directamente en
`docs/informe/`** — a diferencia del flujo de la Tercera Entrega, aquí
no hace falta copiarlo a ningún otro nivel del árbol de directorios.

### Limpieza de auxiliares

```bash
cd docs/informe
latexmk -c informe-final-v1.0.0.tex
```

Borra `.aux`/`.log`/`.toc`/`.lof`/`.lot`/`.bbl`/`.blg`/`.out`/etc. (ver
`.gitignore` de esta carpeta); no toca `informe-final-v1.0.0.pdf` ni
ningún `.tex`.

### Comprobación del PDF generado

El Informe Final esperado tiene **89 páginas**. Para comprobarlo tras
compilar (requiere `pdfinfo`, incluido en TeX Live/MiKTeX):

```bash
pdfinfo informe-final-v1.0.0.pdf | grep Pages
```

Salida esperada: `Pages:           89`. Esta cifra fue verificada
directamente sobre el PDF versionado en este repositorio (no es una
estimación).

---

## Organización de las secciones (Informe Final)

```
docs/informe/
├── informe-final-v1.0.0.tex   # documento maestro vigente
├── informe-entrega-3.tex      # documento maestro histórico (Tercera Entrega, no modificar)
├── referencias.bib            # bibliografia IEEE compartida por ambos documentos (BibTeX clasico, bibliographystyle{ieeetr})
├── README.md                  # este archivo
├── .gitignore                 # auxiliares de compilacion (.aux/.log/.toc/...), no el PDF final
├── secciones-final/           # capitulos del Informe Final (18 archivos, ver tabla)
├── secciones/                 # capitulos historicos de la Tercera Entrega (13 archivos)
└── figuras/                   # evidencia compartida por ambos documentos
    ├── compartidas/
    ├── jaime/
    ├── fred/
    └── zaida/
```

`informe-final-v1.0.0.tex` incluye, en este orden, cada capítulo de
`secciones-final/` mediante `\input`:

| Capítulo | Archivo |
|---|---|
| Resumen / Abstract | `00-resumen-abstract.tex` |
| Introducción | `01-introduccion.tex` |
| Marco teórico | `01b-marco-teorico.tex` |
| Trabajos relacionados | `02-trabajos-relacionados.tex` |
| Metodología | `03-metodologia.tex` |
| Requisitos | `04-requisitos.tex` |
| Arquitectura | `05-arquitectura.tex` |
| Implementación | `06-implementacion.tex` |
| Pruebas y calidad | `07-pruebas-calidad.tex` |
| Seguridad | `08-seguridad.tex` |
| Despliegue y reproducibilidad | `09-despliegue-reproducibilidad.tex` |
| Trazabilidad | `10-trazabilidad.tex` |
| Resultados | `11-resultados.tex` |
| Discusión | `12-discusion.tex` |
| Amenazas y limitaciones | `13-amenazas-limitaciones.tex` |
| Conclusiones | `14-conclusiones.tex` |
| Declaraciones | `15-declaraciones.tex` |
| Anexos | `16-anexos.tex` |

Para editar el contenido de un capítulo, edita **solo** el archivo de
`secciones-final/` correspondiente; no es necesario tocar el documento
maestro para cambios de contenido.

## Cómo funcionan las evidencias (`\IfFileExists`)

Ambos documentos maestros (`informe-final-v1.0.0.tex` e
`informe-entrega-3.tex`) definen el mismo comando auxiliar:

```latex
\newcommand{\evidencia}[3]{%
  \IfFileExists{#1}{%
    \begin{figure}[H]
      \centering
      \includegraphics[width=0.82\textwidth]{#1}
      \caption{#2}
      \label{#3}
    \end{figure}
  }{}%
}
```

`\IfFileExists` es un comando nativo del núcleo de LaTeX (no requiere
ningún paquete). Si el archivo de la ruta indicada **no existe**, no se
imprime absolutamente nada: ni un hueco, ni un marcador visible, ni
texto de "captura pendiente". Si el archivo **sí existe**, la figura
aparece automáticamente con su caption y su label.

Las rutas de evidencia usadas actualmente por `secciones-final/` viven
en `figuras/compartidas/`, `figuras/jaime/`, `figuras/fred/` y
`figuras/zaida/` (mismas carpetas que la Tercera Entrega; algunos
archivos son compartidos entre ambos informes, otros son exclusivos del
Informe Final).

## Regeneración de figuras de evidencia

Tres figuras del capítulo "Pruebas y calidad" (SUS, `mvn clean verify` y
JaCoCo) se generan **automáticamente** desde evidencia cruda versionada,
en vez de mediante capturas de pantalla manuales:

```bash
python docs/informe/scripts/generar-figuras-evidencia.py
```

(ejecutar desde la **raíz del repositorio**; requiere Python 3.9+ y
`matplotlib`). El script falla con un mensaje explícito si falta alguna
fuente o si los datos son inconsistentes; no contiene ninguna cifra de
resultado hardcodeada. Fuentes que consume:

- `docs/mediciones/sus/sus-raw.csv` (18 participantes SUS) →
  `figuras/jaime/06-sus-resultados-final.png`
- `docs/mediciones/sec/reproduccion-v1.0.0/mvn-clean-verify.txt` (log
  crudo de `mvn clean verify` reproducido sobre el commit del tag
  `v1.0.0`) → `figuras/jaime/07-maven-verify-final.png`
- `docs/mediciones/jacoco/jacoco.csv` (reporte JaCoCo por clase) →
  `figuras/jaime/08-jacoco-resumen-final.png`

Las figuras históricas correspondientes (`figuras/jaime/01-maven-verify.png`,
`figuras/jaime/02-jacoco-resumen.png`, `figuras/zaida/05-sus-resultados.png`)
se conservan sin modificar como artefactos históricos; el informe ya no
las referencia como evidencia vigente (ver
`secciones-final/07-pruebas-calidad.tex`).

## No escribir secretos en el informe

**Nunca** incluyas en ningún `.tex`, en el `.bib`, en este README ni en
ninguna captura de `figuras/`: contraseñas con valor, JSON Web Tokens
completos (`eyJ...`), valores de cookies, el encabezado
`Authorization: Bearer` con valor real, claves privadas, ni el
identificador `jti` de un token real. Cuando una captura de pantalla
deba mostrar una cookie o un token, recorta o difumina el valor antes de
guardarla.

---

## Histórico — Tercera Entrega

> **Esta sección describe `informe-entrega-3.tex`, el documento maestro
> de la Tercera Entrega (`v0.9.0-rc`). Se conserva sin modificar como
> registro histórico y NO es el procedimiento de compilación del
> Informe Final** (ver [arriba](#compilación-del-informe-final-informe-final-v100tex)).
> Las cifras y capturas mencionadas en esta sección (por ejemplo,
> "109 pruebas" en la evidencia de Jaime) corresponden al estado del
> proyecto en la Tercera Entrega, no al estado final — ver
> [`docs/mediciones/TEST-COUNT-PROVENANCE.md`](../mediciones/TEST-COUNT-PROVENANCE.md)
> para la trazabilidad completa de las distintas cifras de pruebas que
> han existido en el proyecto.

### Compilación de `informe-entrega-3.tex`

#### Opción recomendada: `latexmk`

```bash
cd docs/informe
latexmk -pdf -interaction=nonstopmode -halt-on-error informe-entrega-3.tex
```

#### Alternativa: `pdflatex` + `bibtex` manual

```bash
cd docs/informe
pdflatex -interaction=nonstopmode informe-entrega-3.tex
bibtex informe-entrega-3
pdflatex -interaction=nonstopmode informe-entrega-3.tex
pdflatex -interaction=nonstopmode informe-entrega-3.tex
```

El PDF resultante (`informe-entrega-3.pdf`) se genera directamente en
`docs/informe/`, junto al `.tex` (el propio repositorio ya lo tiene
así versionado; una versión anterior de este README afirmaba
incorrectamente que debía copiarse un nivel arriba, a
`docs/informe-entrega-3.pdf` — eso no refleja la estructura real y fue
corregido aquí).

### Organización de las secciones (Tercera Entrega)

```
docs/informe/
├── informe-entrega-3.tex      # documento maestro historico (Tercera Entrega)
├── secciones/
│   ├── 01-resumen-ejecutivo.tex
│   ├── 02-estado-sistema.tex
│   ├── 03-arquitectura-c4.tex
│   ├── 04-trazabilidad.tex
│   ├── 05-protocolo-experimental.tex
│   ├── 06-resultados-jaime.tex
│   ├── 07-resultados-fred.tex
│   ├── 08-resultados-zaida.tex
│   ├── 09-amenazas-validez.tex
│   ├── 10-etica.tex
│   ├── 11-credit.tex
│   ├── 12-conclusiones.tex
│   └── 13-anexos.tex
```

Cada capítulo del PDF histórico corresponde a exactamente un archivo de
`secciones/`, incluido en `informe-entrega-3.tex` mediante `\input`.

### Lista de evidencias históricas de la Tercera Entrega

Las tablas de evidencias por integrante (rutas `figuras/<nombre>/NN-*.png`
y su descripción) que este README documentaba para la Tercera Entrega se
conservan sin cambios en el propio `.tex` histórico (comentarios
`% EVIDENCIA-<RESPONSABLE>-<NUMERO>` que preceden a cada
`\evidencia{...}` en `secciones/`); no se duplican aquí para evitar que
este README tenga dos fuentes de verdad sobre las mismas rutas. Para
consultarlas, revisa directamente `informe-entrega-3.tex` y los archivos
de `secciones/`.

**Nota sobre la cifra "109 pruebas" en `figuras/jaime/01-maven-verify.png`
(Tercera Entrega):** esa captura y su caption corresponden a la corrida
de `mvn clean verify` archivada como histórica en
[`docs/mediciones/sec/raw/historical-2026-08-01/mvn-clean-verify.txt`](../mediciones/sec/raw/historical-2026-08-01/mvn-clean-verify.txt).
No se actualizó para la Entrega Final porque pertenece al documento
histórico congelado; el Informe Final (`secciones-final/07-pruebas-calidad.tex`
y otros) usa una cifra distinta — ver
[`docs/mediciones/TEST-COUNT-PROVENANCE.md`](../mediciones/TEST-COUNT-PROVENANCE.md).
