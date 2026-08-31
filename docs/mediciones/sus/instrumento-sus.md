# Instrumento aplicado — System Usability Scale (SUS)

Fuente: Brooke, J. (1996). *SUS: A 'quick and dirty' usability scale*. En
P. W. Jordan, B. Thomas, B. A. Weerdmeester & I. L. McClelland (Eds.),
*Usability Evaluation in Industry* (pp. 189–194). Taylor and Francis.

Las diez preguntas se aplicaron **sin modificar la escala de cinco puntos**
(1 = Totalmente en desacuerdo, 5 = Totalmente de acuerdo), conforme al
Bloque C.3 de la Guía de la Tercera Entrega. Traducción funcional al
español utilizada en la aplicación del instrumento:

| # | Ítem (columna en sus-raw.csv) | Enunciado presentado al participante |
|---|---|---|
| 1 | `Q1_usaria_frecuentemente` | Creo que usaría este sistema con frecuencia. |
| 2 | `Q2_innecesariamente_complejo` | Encontré el sistema innecesariamente complejo. |
| 3 | `Q3_facil_de_usar` | Pensé que el sistema era fácil de usar. |
| 4 | `Q4_necesito_soporte_tecnico` | Creo que necesitaría el apoyo de una persona con conocimientos técnicos para poder usar este sistema. |
| 5 | `Q5_funciones_bien_integradas` | Encontré que las diversas funciones de este sistema estaban bien integradas. |
| 6 | `Q6_demasiada_inconsistencia` | Pensé que había demasiada inconsistencia en este sistema. |
| 7 | `Q7_aprendizaje_rapido` | Imagino que la mayoría de las personas aprenderían a usar este sistema muy rápidamente. |
| 8 | `Q8_engorroso_de_usar` | Encontré el sistema muy engorroso (poco práctico) de usar. |
| 9 | `Q9_confianza_al_usar` | Me sentí muy seguro/a usando el sistema. |
| 10 | `Q10_necesito_aprender_mucho_antes` | Necesité aprender muchas cosas antes de poder usar este sistema. |

## Escala

1 = Totalmente en desacuerdo · 2 = En desacuerdo · 3 = Neutral ·
4 = De acuerdo · 5 = Totalmente de acuerdo

## Método de cálculo del puntaje SUS (Brooke, 1996)

- Para los ítems **impares** (1, 3, 5, 7, 9): contribución = (valor de respuesta − 1).
- Para los ítems **pares** (2, 4, 6, 8, 10): contribución = (5 − valor de respuesta).
- Se suman las diez contribuciones (rango 0–40) y se multiplica por 2.5,
  obteniendo un puntaje final en el rango 0–100.

Este cálculo está implementado en `scripts/analisis-sus.py`, función
`calcular_puntaje_sus()`. El flujo real del script es:

1. Lee las 10 respuestas Q1–Q10 de cada fila de `sus-raw.csv`.
2. Calcula el puntaje SUS de esa fila con la fórmula de arriba
   (`calcular_puntaje_sus`).
3. Valida ese puntaje calculado contra el valor `sus_score` ya
   almacenado en la misma fila del CSV; si no coincide, el script se
   detiene con un error que identifica el código de participante
   afectado, en vez de continuar con un dato sin validar.
4. Usa el puntaje calculado (ya validado) para las estadísticas
   agregadas del reporte (`docs/mediciones/sus/REPORT.md`).

Una versión anterior de este documento afirmaba que existía una función
llamada `sus_score()` que realizaba este cálculo; esa función nunca
existió en el script (que hasta esta corrección solo leía el valor ya
almacenado en el CSV, sin recalcularlo). Esta sección se actualiza para
describir el comportamiento real, verificado por auditoría el
2026-08-31.
