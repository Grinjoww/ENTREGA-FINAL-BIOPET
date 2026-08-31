# Procedencia de las cifras de número total de pruebas — BIOPET

Este documento existe porque distintos archivos del repositorio citan
totales distintos de pruebas del backend a lo largo del tiempo (**109,
166, 189, 205**). El objetivo es dejar trazabilidad explícita de dónde
sale cada número — con commit, fecha y archivo de evidencia — y, ahora
que el tag histórico `v1.0.0` fue reproducido de forma independiente,
dejar establecido cuál es el resultado final verificado.

Ver también [`DATA-PROVENANCE.md`](DATA-PROVENANCE.md) (procedencia de
los datos de medición en general) y
[`docs/informe/README.md`](../informe/README.md) (compilación del
informe, que cita la cifra vigente en `secciones-final/`).

## Resumen

| Total | Fuente principal | Fecha/etapa | Evidencia | Clasificación |
|---:|---|---|---|---|
| 109 | `docs/mediciones/sec/raw/historical-2026-08-01/mvn-clean-verify.txt` (línea `[INFO] Tests run: 109, Failures: 0, Errors: 0, Skipped: 0`) | 2026-08-01 (commit `13300a8`, 2026-07-31, "test(security): automatiza evidencias OWASP reales"; archivo movido a subcarpeta `historical-2026-08-01/` en el commit `bb43baa`, 2026-08-16) | Log crudo archivado en el repositorio, íntegro | **Histórica — verificada en log archivado.** Corrida baseline de la Tercera Entrega (`v0.9.0-rc`). Coincide con `docs/mediciones/DATA-DICTIONARY.md` y con `docs/informe/secciones/` (capítulos de `informe-entrega-3.tex`, ver [histórico en `docs/informe/README.md`](../informe/README.md#histórico--tercera-entrega)). |
| 166 | `docs/checklists/incose2023-req.md` (línea 55, "166 tests verdes"); `docs/u4/informe/secciones/06-revision-cruzada.tex` y `08-conclusiones.tex`; `docs/u4/revisiones/REVISION-FAJARDO.md`; `docs/mediciones/DATA-PROVENANCE.md` (histórico, antes de la corrección aplicada en esta auditoría) | `docs/u4/` y `REVISION-FAJARDO.md`: commits del 2026-08-10. `incose2023-req.md`: commit `5bc4c9f`, 2026-08-16. Aparición en `DATA-PROVENANCE.md`: commit `10a165c`, 2026-08-17 | **No se localizó ningún log crudo (`Tests run: 166` o equivalente) archivado en el repositorio.** Solo aparece en texto narrativo/tablas. `docs/u4/` es un informe de una entrega distinta (Unidad IV, revisión cruzada entre pares), no el informe final de BIOPET. | **Narrativa/intermedia, sin log crudo localizado — no se presenta como cifra final.** Es consistente cronológicamente como punto intermedio entre 109 (2026-08-01) y 189 (2026-08-16), pero no hay evidencia cruda que la confirme de forma independiente. No se convierte en 205 ni en ninguna otra cifra: se deja documentada tal como aparece en cada fuente, marcada como no verificada con log crudo. |
| 189 | `docs/mediciones/sec/raw/mvn-clean-verify.txt` (línea `[INFO] Tests run: 189, Failures: 0, Errors: 0, Skipped: 0`) | 2026-08-16 22:34:30 -0500 (commit `bb43baa`, "docs: archivar evidencias OWASP ZAP y analisis estatico") | Log crudo archivado en el repositorio, íntegro. Citado también en `docs/mediciones/sec/jacoco-summary.md`, `docs/mediciones/sec/static-analysis/README.md`, `docs/arquitectura/ISO-25010.md`, `docs/observaciones/OBSERVACIONES.md`, `docs/informe/borradores/jaime/metodologia-y-amenazas.md` | **Evidencia histórica válida — pero de un estado del código anterior al commit del tag `v1.0.0`, no del resultado final.** El commit `bb43baa` es ancestro de `v1.0.0` (verificado con `git merge-base --is-ancestor`), pero 25 minutos después el commit `5340b71` (2026-08-16 22:59:34) agregó dos clases de prueba nuevas que **sí** quedaron incluidas en el tag `v1.0.0`. El log de 189 nunca se volvió a archivar después de ese commit. Se conserva sin modificar como evidencia histórica de ese punto exacto en el tiempo — no se borra ni se sobrescribe. |
| 205 | `docs/informe/secciones-final/*.tex` (Informe Final); ahora también `docs/mediciones/sec/reproduccion-v1.0.0/mvn-clean-verify.txt` (reproducción de auditoría) | Cifra integrada al Informe Final en el commit `ba41e11`, 2026-08-18. **Reproducción verificada el 2026-08-31** sobre el commit exacto del tag `v1.0.0` (`0d5cd525ce648cca7219da204e16fa622e671a87`), en worktree independiente, sin modificar código ni tests | `docs/mediciones/sec/reproduccion-v1.0.0/mvn-clean-verify.txt` + `docs/mediciones/sec/reproduccion-v1.0.0/README.md` (detalle completo, SHA-256, comparación clase por clase) | **RESULTADO FINAL REPRODUCIBLE de `v1.0.0` — verificado de forma independiente por tres vías coincidentes: consola Maven (`Tests run: 205, Failures: 0, Errors: 0, Skipped: 0`), suma manual de las 22 líneas por clase, y suma de los atributos `tests` de los 22 `target/surefire-reports/TEST-*.xml` generados en esa misma corrida. BUILD SUCCESS.** |

## Explicación verificada de la diferencia 189 → 205

Esta sección documenta únicamente lo que fue demostrado por comparación
directa, clase por clase, entre el log histórico de 189 y la
reproducción de 205 (detalle completo en
[`docs/mediciones/sec/reproduccion-v1.0.0/README.md`](sec/reproduccion-v1.0.0/README.md)):

- **HECHO VERIFICADO:** las 20 clases de prueba presentes en el log
  histórico de 189 reportan, cada una, el mismo número de tests en la
  reproducción de `v1.0.0`. Ninguna clase existente cambió su conteo.
- **HECHO VERIFICADO:** dos clases están presentes en el árbol del tag
  `v1.0.0` (confirmado con `git ls-tree v1.0.0`) y **ausentes** en el
  árbol del commit que archivó el log de 189 (confirmado con
  `git ls-tree bb43baa`):
  - `BiopetAppRolMinimoPrivilegiosIntegrationTest` — 4 pruebas
  - `ProcedimientosBiopetIntegrationTest` — 12 pruebas
- **HECHO VERIFICADO (aritmética exacta, no aproximación):** `189 + 4 + 12 = 205`. La diferencia total es **16** pruebas nuevas, no 18.
- **EVIDENCIA HISTÓRICA (orden temporal, verificado con `git log` y `git merge-base --is-ancestor`):** el commit `bb43baa` (log de 189) y el commit `5340b71` (agrega las dos clases nuevas) son ambos ancestros del tag `v1.0.0`; `5340b71` ocurrió 25 minutos después de `bb43baa`. Esto establece el orden y la pertenencia al árbol del tag, **no** que un commit "causara" la cifra final — la cifra final es simplemente la suma de las pruebas presentes en el commit del tag, que es posterior a ambos.

No se plantea ninguna otra hipótesis (por ejemplo, un conteo estático de
anotaciones `@Test` sobre el código fuente) como explicación de esta
diferencia: la explicación anterior ya está demostrada por reproducción
real de Maven/Surefire y no requiere apoyo adicional. Una versión previa
de este documento estimaba la diferencia en 18 pruebas mediante un
conteo estático de `@Test` sobre la rama de correcciones (no sobre el
tag); esa estimación queda descartada porque la reproducción real sobre
el commit exacto del tag demuestra que son 16, no 18, y el conteo
estático de anotaciones no debe usarse para justificar el resultado real
de una corrida de Maven/Surefire.

## Qué queda cerrado y qué sigue pendiente

- **Cerrado:** el total reproducible de pruebas para el tag `v1.0.0` es
  **205**, con evidencia archivada y verificable de forma independiente.
- **Pendiente (fuera del alcance de esta auditoría):** reconciliar el
  texto de `docs/mediciones/DATA-PROVENANCE.md` §4 y la cifra 166 citada
  en documentos de etapas intermedias — ya marcados como históricos/no
  verificados, sin necesidad de reproducirlos porque no se presentan
  como el resultado final.
