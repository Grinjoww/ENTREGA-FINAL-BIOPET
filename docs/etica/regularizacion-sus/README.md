# Regularización documental — evidencia SUS

Esta carpeta contiene la constancia de regularización documental emitida
por el equipo del proyecto BIOPET sobre la evidencia de la evaluación de
usabilidad (System Usability Scale, SUS).

## Qué es

| Campo | Detalle |
|---|---|
| Archivo | [`CONSTANCIA-REGULARIZACION-SUS-BIOPET-2026-08-31.pdf`](CONSTANCIA-REGULARIZACION-SUS-BIOPET-2026-08-31.pdf) |
| Fecha | 2026-08-31 |
| Emitida por | Fred Adrián Beltrán Montiel, Jaime Josué Mariscal Cabrera y Zaida Melissa Taipe Mora — los tres integrantes del proyecto |
| Motivo | Auditoría documental posterior de la Entrega Final, que detectó una inconsistencia entre la documentación del proyecto (que afirmaba conservar formularios individuales de consentimiento informado firmados) y la evidencia realmente disponible |

## Qué NO es

- **No es un consentimiento informado individual de P01–P18.** No sustituye, representa ni reemplaza los formularios de consentimiento de cada participante.
- **No incorpora firmas ni datos de los participantes.** Solo está firmada por los tres integrantes del equipo, como responsables del proyecto.
- **No es evidencia retrospectiva fabricada.** No pretende demostrar que los consentimientos individuales existieron; documenta, de forma transparente, que actualmente no se dispone de ellos como evidencia verificable.

## Qué declara

1. Que la documentación original del proyecto (`docs/etica/ETHICS.md`, `docs/mediciones/sus/REPORT.md`, y secciones del informe) afirmaba que existían formularios de consentimiento firmados por cada uno de los 18 participantes, conservados fuera del repositorio.
2. Que, al revisar esa afirmación, el equipo constató que esos formularios firmados **no están actualmente disponibles** como evidencia verificable.
3. Que el conjunto de datos (`docs/mediciones/sus/sus-raw.csv`, 18 registros P01–P18) corresponde, según declaración del equipo, a participantes reales evaluados durante el desarrollo del proyecto.
4. Que la referencia previa a "generación de datos sintéticos didácticos" en `scripts/analisis-sus.py` no correspondía al comportamiento real del script (verificado por auditoría técnica).

## Alcance de esta regularización

No se generó ninguna evidencia nueva para "completar" lo que falta: no
se crearon consentimientos, firmas ni formularios retroactivos. El
objetivo de esta carpeta y de la constancia es exclusivamente la
**transparencia y la trazabilidad documental** — dejar registrado, de
forma verificable en el propio repositorio, qué se puede demostrar
técnicamente (los 18 registros y su reproducibilidad estadística, ver
[`docs/mediciones/sus/REPORT.md`](../../mediciones/sus/REPORT.md)) y qué
sigue siendo una declaración del equipo pendiente de respaldo documental
adicional (la identidad y el consentimiento de cada participante).

Ver también [`docs/etica/ETHICS.md`](../ETHICS.md) y
[`docs/mediciones/DATA-PROVENANCE.md`](../../mediciones/DATA-PROVENANCE.md)
(sección SUS) para la documentación relacionada, ya actualizada para
reflejar esta misma distinción.
