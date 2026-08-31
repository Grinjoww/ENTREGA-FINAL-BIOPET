# Reproducción de auditoría — tag `v1.0.0` (2026-08-31)

Esta carpeta contiene la salida de una **reproducción posterior**,
realizada para verificar el número de pruebas del backend que reporta
`mvn clean verify` sobre el commit exacto al que apunta el tag histórico
e inmutable `v1.0.0`. **No es evidencia de agosto de 2026** y no
reemplaza ni modifica ningún log archivado en esa fecha.

## Distinción explícita con la evidencia histórica

| | Evidencia histórica de agosto | Esta reproducción |
|---|---|---|
| Ruta | [`docs/mediciones/sec/raw/mvn-clean-verify.txt`](../raw/mvn-clean-verify.txt) | `docs/mediciones/sec/reproduccion-v1.0.0/mvn-clean-verify.txt` |
| Cuándo se generó | 2026-08-16 (commit `bb43baa`) | 2026-08-31 (esta auditoría) |
| Sobre qué commit corrió | El `HEAD` de ese momento (anterior al tag `v1.0.0`) | El commit exacto del tag `v1.0.0` |
| Resultado | 189 / 189 | 205 / 205 |
| Estado | Se conserva sin modificar, como registro histórico | Evidencia nueva, generada ahora, de la reproducibilidad del tag |

Ninguno de los dos logs es "más verdadero" que el otro: cada uno es
correcto **para el commit sobre el que corrió**. El log histórico de 189
no describe el estado del código en el tag `v1.0.0` porque ese commit
(`bb43baa`) es anterior a la adición de dos clases de prueba (ver más
abajo); esta reproducción sí corre exactamente sobre el commit del tag.

## Procedencia de esta reproducción

| Campo | Detalle |
|---|---|
| Fecha real de la reproducción | 2026-08-31 |
| Tag reproducido | `v1.0.0` |
| Commit completo | `0d5cd525ce648cca7219da204e16fa622e671a87` |
| Método | `git worktree add ../BIOPET-v1.0.0-audit v1.0.0` (detached HEAD confirmado sobre ese commit), ejecutado **fuera** de la rama de correcciones — no se tocó el árbol de trabajo del repositorio principal |
| Comando ejecutado | `mvn clean verify`, sin flags adicionales, sin `-DskipTests`, sin exclusiones, desde `Backend/` del worktree |
| Java | Temurin 21.0.11 |
| Maven | Apache Maven 3.9.16 |
| Servicios externos | Docker Desktop 4.83.0 (daemon accesible), usado por Testcontainers para las 4 clases de integración contra PostgreSQL real (`ResumenEspeciesIntegrationTest`, `TriggerActualizadoEnIntegrationTest`, `BiopetAppRolMinimoPrivilegiosIntegrationTest`, `ProcedimientosBiopetIntegrationTest`); el resto de la suite usa H2 en memoria (`src/test/resources/application-test.yml`); no se requirió Redis real |
| Configuración modificada | Ninguna — se ejecutó el `pom.xml` tal cual existe en el commit del tag |

## Resultado

```
[INFO] Tests run: 205, Failures: 0, Errors: 0, Skipped: 0
...
[INFO] BUILD SUCCESS
[INFO] Total time:  02:36 min
[INFO] Finished at: 2026-08-31T15:22:13-05:00
```

- **205** pruebas ejecutadas
- **0** fallos
- **0** errores
- **0** omitidas
- `jacoco:check`: "All coverage checks have been met."
- Resultado: `BUILD SUCCESS`

Verificado por tres vías independientes, todas coincidentes en 205:
consola Maven (línea de resumen agregado), suma manual de las 22 líneas
`Tests run:` por clase, y suma de los atributos `tests` de los 22
`target/surefire-reports/TEST-*.xml` generados por esa misma corrida (no
versionados en el repositorio; son artefacto regenerable, igual que en
`docs/mediciones/sec/jacoco-summary.md`).

## Archivo de esta carpeta

| Archivo | Contenido | SHA-256 |
|---|---|---|
| `mvn-clean-verify.txt` | Salida completa de consola de `mvn clean verify`, copia exacta y sin edición manual de la ejecución real | `a322743d8587bf4e69332ffbb46ee0bef38e1c66d02984ac4f1ea95e3cb4b231` |

SHA-256 calculado con `sha256sum` sobre el archivo tal como quedó
versionado en esta carpeta; verificado además contra el archivo temporal
de origen (`diff` sin diferencias) antes de copiarlo aquí.

## Explicación de la diferencia con el log histórico de 189

Comparando clase por clase el log histórico (`../raw/mvn-clean-verify.txt`,
commit `bb43baa`, 2026-08-16 22:34:30) contra esta reproducción:

- Las **20 clases de prueba** que ya existían en el log histórico
  reportan **exactamente el mismo número de tests**, clase por clase, en
  esta reproducción. Ninguna cambió.
- Aparecen **dos clases nuevas**, ausentes en el log histórico y
  presentes en el árbol del tag `v1.0.0`:
  - `com.biopet.repository.BiopetAppRolMinimoPrivilegiosIntegrationTest` — **4** pruebas
  - `com.biopet.repository.ProcedimientosBiopetIntegrationTest` — **12** pruebas
- `189 + 4 + 12 = 205`.
- Ambas clases fueron introducidas en el commit `5340b710850a86934a33548e21c72b98e699f96e`
  ("test(backend): integracion Testcontainers de los 6 SP y rol minimo
  biopet_app", 2026-08-16 22:59:34 -0500), verificado como ancestro del
  tag `v1.0.0` (`git merge-base --is-ancestor 5340b71 v1.0.0`); el commit
  `bb43baa` que archivó el log de 189 también es ancestro del tag, pero
  es anterior a `5340b71` en el mismo árbol de commits.

Esto es una comparación demostrada por clase (no una cercanía numérica):
el conjunto de clases del log de 189 más esas dos clases nuevas es,
exactamente, el conjunto de clases de esta reproducción.

## Objetivo de esta carpeta

Demostrar que el estado de código publicado bajo el tag `v1.0.0` es
reproducible de forma independiente y que el total de pruebas que ese
estado ejecuta con `mvn clean verify` es 205, sin alterar ni sustituir la
evidencia histórica de agosto. Ver también
[`docs/mediciones/TEST-COUNT-PROVENANCE.md`](../../TEST-COUNT-PROVENANCE.md)
para la trazabilidad completa de todas las cifras de pruebas del
proyecto.
