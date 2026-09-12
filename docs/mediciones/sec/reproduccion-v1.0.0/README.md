# Reproducción de auditoría — tag `v1.0.0` (2026-08-31)

Esta carpeta contiene la salida de una **reproducción posterior**,
realizada para verificar el número de pruebas del backend que reporta
`mvn clean verify` sobre el commit histórico `0d5cd52`, al que apuntó el
tag `v1.0.0` durante el cierre de la Entrega Final original (18-ago-2026).
Conforme a la rúbrica de esta recalificación, ese mismo tag se
actualizará al cierre para identificar el commit final evaluado; esta
reproducción documenta el commit histórico específico sobre el que
corrió, no el estado actual o futuro del puntero del tag. **No es
evidencia de agosto de 2026** y no reemplaza ni modifica ningún log
archivado en esa fecha.

## Distinción explícita con la evidencia histórica

| | Evidencia histórica de agosto | Esta reproducción |
|---|---|---|
| Ruta | [`docs/mediciones/sec/raw/mvn-clean-verify.txt`](../raw/mvn-clean-verify.txt) | `docs/mediciones/sec/reproduccion-v1.0.0/mvn-clean-verify.txt` |
| Cuándo se generó | 2026-08-16 (commit `bb43baa`) | 2026-08-31 (esta auditoría) |
| Sobre qué commit corrió | El `HEAD` de ese momento (anterior al commit histórico `0d5cd52`) | El commit histórico `0d5cd52` (al que apuntó `v1.0.0` durante el cierre original) |
| Resultado | 189 / 189 | 205 / 205 |
| Estado | Se conserva sin modificar, como registro histórico | Evidencia generada en 2026-08-31 sobre ese commit histórico específico |

Ninguno de los dos logs es "más verdadero" que el otro: cada uno es
correcto **para el commit sobre el que corrió**. El log histórico de 189
no describe el estado del código en el commit histórico `0d5cd52` porque
el commit sobre el que corrió (`bb43baa`) es anterior a la adición de dos
clases de prueba (ver más abajo); esta reproducción sí corre exactamente
sobre `0d5cd52`.

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
| Configuración modificada | Ninguna — se ejecutó el `pom.xml` tal cual existe en el commit histórico `0d5cd52` |

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

`mvn-clean-verify.txt` es evidencia generada el 2026-08-31, **posterior**
al tag `v1.0.0` (18-ago): no es un archivo que existiera dentro del
commit `0d5cd52`, sino la salida de reproducir ese commit histórico
después, en un `worktree` aparte (ver "Procedencia de esta reproducción"
arriba). El commit `0d5cd52` es el estado de código reproducido; este
archivo documenta esa reproducción, no forma parte de su árbol.

| Archivo | Contenido | SHA-256 (contenido versionado, terminación de línea LF) |
|---|---|---|
| `mvn-clean-verify.txt` | Salida completa de consola de `mvn clean verify`, copia exacta y sin edición manual de la ejecución real | `8de47f9dee17894314787d04033dea5b604d6c9cf0db59d31fefb98b15c30705` |

SHA-256 calculado sobre los bytes tal como los almacena Git (blob, LF),
con `git cat-file blob <id> | sha256sum` — no sobre el archivo del
árbol de trabajo, que en un checkout de Windows con `core.autocrlf=true`
puede convertir las terminaciones de línea a CRLF y producir un
SHA-256 distinto para el mismo contenido lógico (ver
`.gitattributes`, que fija `eol=lf` para esta ruta).

## Explicación de la diferencia con el log histórico de 189

Comparando clase por clase el log histórico (`../raw/mvn-clean-verify.txt`,
commit `bb43baa`, 2026-08-16 22:34:30) contra esta reproducción:

- Las **20 clases de prueba** que ya existían en el log histórico
  reportan **exactamente el mismo número de tests**, clase por clase, en
  esta reproducción. Ninguna cambió.
- Aparecen **dos clases nuevas**, ausentes en el log histórico y
  presentes en el árbol del commit histórico `0d5cd52`:
  - `com.biopet.repository.BiopetAppRolMinimoPrivilegiosIntegrationTest` — **4** pruebas
  - `com.biopet.repository.ProcedimientosBiopetIntegrationTest` — **12** pruebas
- `189 + 4 + 12 = 205`.
- Ambas clases fueron introducidas en el commit `5340b710850a86934a33548e21c72b98e699f96e`
  ("test(backend): integracion Testcontainers de los 6 SP y rol minimo
  biopet_app", 2026-08-16 22:59:34 -0500), verificado como ancestro del
  commit histórico `0d5cd52` con el comando ejecutado en su momento
  (`git merge-base --is-ancestor 5340b71 v1.0.0`, cuando `v1.0.0` apuntaba
  todavía a `0d5cd52`; equivalente a `git merge-base --is-ancestor 5340b71 0d5cd52`);
  el commit `bb43baa` que archivó el log de 189 también es ancestro de
  `0d5cd52`, pero es anterior a `5340b71` en el mismo árbol de commits.

Esto es una comparación demostrada por clase (no una cercanía numérica):
el conjunto de clases del log de 189 más esas dos clases nuevas es,
exactamente, el conjunto de clases de esta reproducción.

## Objetivo de esta carpeta

Demostrar que el estado de código del commit histórico `0d5cd52`
(al que apuntó el tag `v1.0.0` durante el cierre de la Entrega Final
original) es reproducible de forma independiente y que el total de
pruebas que ese estado ejecuta con `mvn clean verify` es 205, sin
alterar ni sustituir la evidencia histórica de agosto. Ver también
[`docs/mediciones/TEST-COUNT-PROVENANCE.md`](../../TEST-COUNT-PROVENANCE.md)
para la trazabilidad completa de todas las cifras de pruebas del
proyecto.
