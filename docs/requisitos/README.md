# Requisitos — cómo regenerar `SRS-v1.0.0.pdf` y `SRS-v1.0.0-para-firma.pdf`

`docs/requisitos/SRS.md` es la fuente editable única (44 requisitos). `SRS.tex`,
`SRS.pdf`, `SRS-v1.0.0.pdf`, `SRS-v1.0.0-para-firma.tex` y
`SRS-v1.0.0-para-firma.pdf` son derivados y deben regenerarse desde ahí
después de cualquier cambio al `.md` (nunca editar `SRS.tex` a mano).

Todos los comandos se ejecutan desde `docs/requisitos/`. Los pasos 2, 3 y 4
están automatizados en `tools/` para que la cadena sea reproducible y el PDF
entregado coincida exactamente con la fuente.

**Herramientas necesarias:** `pandoc` (3.11 en la última generación),
`pdflatex` (MiKTeX/TeX Live, con los paquetes `hyphenat` y `newunicodechar`)
y `python3`.

## 0. Auditoría de la fuente (antes de generar)

Comprueba mecánicamente que el SRS está completo y que no cita nada
inexistente: campos obligatorios de los **44 requisitos**, los tres estados
válidos de la sección 1.3.1, ausencia de identificadores con sufijo de letra,
referencias `HU-`/`CU-` reales, rutas de archivo y directorios que existen en
el repositorio, nombres de clases y métodos de prueba y de producción reales,
coherencia con la tabla de control de la sección 3.4, **igualdad exacta de
conjuntos de identificadores** con `docs/trazabilidad/matriz.csv` (imprime las
cuatro diferencias SRS/matriz/tabla 3.4, que deben ser vacías), ausencia de
menciones a otros proyectos y ausencia de las dos trampas de Markdown que
corrompen el PDF en silencio (líneas de continuación que empiezan por un
marcador de lista y *code spans* sin cerrar).

```bash
python3 tools/auditoria-srs.py SRS.md ../..
```

Debe terminar con `AUDITORIA MECANICA: OK - 0 hallazgos, 0 celdas FALTA.`
Complementariamente, el gate de CI del repositorio:

```bash
bash ../../scripts/validate-traceability.sh
```

## 1. Markdown -> LaTeX (pandoc)

Pandoc portable oficial (descargar de
https://github.com/jgm/pandoc/releases). Con pandoc en `PATH`:

```bash
pandoc SRS.md -o SRS.tex --toc --toc-depth=3 -V geometry:margin=2.5cm -V lang=es --standalone
```

> **Notas de formato de la fuente.** (1) Todo encabezado `#`/`##`/`###` del
> `.md` debe ir precedido de una línea en blanco, y todo separador `---` debe
> ir seguido de una. Sin la línea en blanco previa, pandoc absorbe el
> encabezado dentro del párrafo anterior (el requisito desaparece del índice y
> se imprime como texto corrido); sin la posterior, pandoc interpreta el `---`
> como bloque de metadatos YAML y aborta. (2) Ninguna línea de continuación
> dentro de una lista puede empezar por `-`, `*` o `+` seguidos de espacio:
> pandoc la lee como un ítem nuevo, lo que rompe cualquier *code span* abierto
> e invierte la tipografía del resto del párrafo. El paso 0 detecta ambos
> casos.

## 2. Ajustes de preámbulo (`tools/paso2-preambulo.py`)

```bash
python3 tools/paso2-preambulo.py SRS.tex
```

Inserta justo después de `\author{}`:

- `\usepackage[htt]{hyphenat}` — habilita el guionado también en la fuente
  teletype; sin esto, los nombres de prueba y de clase largos no se pueden
  partir y el texto se sale del margen.
- `\usepackage{newunicodechar}` con el mapeo de `≥`, `≤`, `→`, `↔`,
  `−` (U+2212) y `×`, que pdfTeX clásico no resuelve por defecto.

El script falla si queda algún carácter fuera de Latin-1 sin mapear.

## 3. Puntos de corte en `\texttt{}` (`tools/paso3-cortes-texttt.py`)

```bash
python3 tools/paso3-cortes-texttt.py SRS.tex
```

Inserta `\allowbreak{}` (penalización de corte sin guion) dentro de los
argumentos de `\texttt{}` en los separadores naturales de rutas e
identificadores: `/`, `,`, `;`, `.` seguido de alfanumérico, `-`, `\_`, las
fronteras camelCase, y cada 16 caracteres en cadenas sin separadores (por
ejemplo, un SHA de 40 caracteres). Sin este paso, rutas como
`Backend/src/main/java/com/biopet/integration/ExternalApiClient.java` se
salen del margen derecho hasta 244 pt.

Con los pasos 2 y 3 aplicados, la compilación deja **2 `Overfull \hbox`**
residuales de 6,1 pt y 13,0 pt (ambos por debajo de 5 mm) y **0
`Overfull \vbox`**.

## 4. LaTeX -> PDF oficial (3 pasadas)

Las 3 pasadas son necesarias por la tabla de contenidos y las referencias
cruzadas:

```bash
pdflatex -interaction=nonstopmode SRS.tex
pdflatex -interaction=nonstopmode SRS.tex
pdflatex -interaction=nonstopmode SRS.tex
cp SRS.pdf SRS-v1.0.0.pdf
```

## 5. PDF para firma `SRS-v1.0.0-para-firma.pdf`

Este es el documento que se envía para la firma del docente-director. Se
parte del `SRS.tex` recién generado (pasos 1 a 3) y se le añaden la portada
institucional y el bloque de aprobación:

```bash
python3 tools/paso4-para-firma.py . "Quevedo, 11 de septiembre de 2026"
pdflatex -interaction=nonstopmode SRS-v1.0.0-para-firma.tex
pdflatex -interaction=nonstopmode SRS-v1.0.0-para-firma.tex
pdflatex -interaction=nonstopmode SRS-v1.0.0-para-firma.tex
```

`tools/paso4-para-firma.py` reutiliza **verbatim** el bloque de metadatos
institucionales, la portada (universidad, facultad, carrera,
docente-director, los tres integrantes con correo y ORCID, y la URL del
repositorio) y la sección `APROBACIÓN DEL DOCENTE-DIRECTOR` que ya existían
en la versión anterior del archivo para firma; lo único que actualiza es la
fecha que se le pasa como segundo argumento. El script **aborta** si alguno
de esos datos obligatorios no aparece en el resultado, para que una
regeneración no pueda perder autores, correos, ORCID ni el repositorio.

La única diferencia de contenido entre `SRS.pdf` y
`SRS-v1.0.0-para-firma.pdf` es la portada, el párrafo "Estado de aprobación"
y el bloque de firma del final.

## 6. Limpieza

```bash
rm -f *.aux *.log *.out *.toc
```

**Estado de firma:** ninguno de los dos PDFs está firmado por el
docente-director. Ver "Estado de aprobación" en `SRS.md`. No generar ni
declarar una versión "firmada" sin la firma real recibida.
