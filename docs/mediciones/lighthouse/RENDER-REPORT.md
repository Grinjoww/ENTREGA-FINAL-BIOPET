# Lighthouse contra el despliegue público de Render — BIOPET

Corrida real ejecutada para el hallazgo de recalificación "Lighthouse final
contra Render" (P4/R1): a diferencia de la corrida del 2026-08-18
(`lhci-20260818-0538-*.json`, contra `http://localhost:4200`), esta corrida
audita el **despliegue público real** en `https://biopet-frontend.onrender.com`.

## Metodología

- **URL pública evaluada:** `https://biopet-frontend.onrender.com` (la misma
  declarada como `\urlfrontend` en `docs/informe/informe-final-v1.0.0.tex`).
- **Rutas auditadas (más de una, ninguna es la portada):** `/login` y
  `/mascotas`.
- **Corridas:** 3 por ruta y por perfil (mobile + desktop) = **12 corridas /
  12 JSON completos** (mínimo exigido por el criterio de aceptación: 6).
- **Configuración:** [`lighthouserc.render.js`](../../../lighthouserc.render.js)
  (perfil móvil, `throttlingMethod: simulate`) y
  [`lighthouserc.render.desktop.js`](../../../lighthouserc.render.desktop.js)
  (perfil desktop, preset `desktop` por defecto de Lighthouse). Mismos
  umbrales que las configuraciones contra localhost: Performance ≥ 80,
  Accessibility ≥ 90, Best Practices ≥ 90, SEO ≥ 90.
- **Script de ejecución:** [`scripts/run-lighthouse-render.sh`](../../../scripts/run-lighthouse-render.sh)
  — verifica que el sitio responda `200` en ambas rutas antes de auditar
  (para no correr en medio de un despliegue), y valida que `requestedUrl`
  de cada JSON generado apunte de verdad a `biopet-frontend.onrender.com`
  antes de archivarlo.
- **Versión y fecha exactas:** ver
  [`lhci-20260903-2102-render.meta.txt`](lhci-20260903-2102-render.meta.txt) —
  `lighthouseVersion` real (motor) tomado de los JSON: **12.1.0**; `@lhci/cli`:
  **0.14.0**; fecha ISO 8601: **2026-09-03T21:02:23Z**; commit corto:
  **12b1870**.

## Medias por perfil/ruta (calculadas desde los JSON, no a mano)

Calculadas con [`compute-render-averages.mjs`](compute-render-averages.mjs)
(`node docs/mediciones/lighthouse/compute-render-averages.mjs 20260903-2102`),
que promedia directamente los campos `categories.*.score` de los 12 JSON
crudos:

| Perfil  | Ruta        | n | Performance | Accessibility | Best Practices | SEO   |
|---|---|---:|---:|---:|---:|---:|
| desktop | `/login`    | 3 | 99,7        | 91,0           | 100,0           | 100,0 |
| desktop | `/mascotas` | 3 | 100,0       | 91,0           | 96,0            | 100,0 |
| mobile  | `/login`    | 3 | 100,0       | 91,0           | 100,0           | 100,0 |
| mobile  | `/mascotas` | 3 | 99,0        | 91,0           | 96,0            | 100,0 |

## Validación de umbrales

| Categoría | Umbral | Resultado (las 12 corridas) | Estado |
|---|---|---|---|
| Performance | ≥ 80 | 99,0 – 100,0 | **Cumplido** |
| Accessibility | ≥ 90 | 91,0 | **Cumplido** |
| Best Practices | ≥ 90 | 96,0 – 100,0 | **Cumplido** |
| SEO | ≥ 90 | 100,0 | **Cumplido** |

Las cuatro categorías cumplen su umbral en las 12 corridas, en ambos
perfiles y ambas rutas, contra el dominio público real.

## Evidencia cruda

Los 12 JSON completos (LHR — Lighthouse Result, formato máquina-legible)
y el archivo de metadatos:

- `lhci-20260903-2102-render-mobile-login-run{0,1,2}.json`
- `lhci-20260903-2102-render-mobile-mascotas-run{3,4,5}.json`
- `lhci-20260903-2102-render-desktop-login-run{0,1,2}.json`
- `lhci-20260903-2102-render-desktop-mascotas-run{3,4,5}.json`
- `lhci-20260903-2102-render.meta.txt`

### Anonimización aplicada (hallazgo Z10, misma convención que `README.md` de esta carpeta)

`lhci autorun`, invocado vía `npx`, embebió la ruta local de caché de
`npx` (con el nombre de usuario del equipo de ejecución) dentro de
`errorStack` de 6 auditorías que fallaron a ejecutarse en las 12 corridas
(`largest-contentful-paint-element`, `lcp-lazy-loaded`, `layout-shifts`,
`non-composited-animations`, `prioritize-lcp-image`,
`render-blocking-resources` — fallo de esas auditorías puntuales, no del
resto del reporte; los puntajes de las 4 categorías no dependen de ellas
y no cambiaron). Se sustituyó \**únicamente**\* el nombre de usuario
literal por el marcador `USER_REDACTED` (48 ocurrencias por archivo, las
12 archivos parsean como JSON válido después del cambio y
`compute-render-averages.mjs` reproduce exactamente las mismas medias
que antes de la sustitución). Ningún puntaje, URL, timestamp ni
`requestedUrl`/`finalUrl` fue tocado.

| Archivo | SHA-256 antes (antes de redactar) | SHA-256 después |
|---|---|---|
| `lhci-20260903-2102-render-desktop-login-run0.json` | `5161ca3ca7a6…` | `7ae995ea2559…` |
| `lhci-20260903-2102-render-desktop-login-run1.json` | `94ce4d777d0a…` | `c20e6627cf70…` |
| `lhci-20260903-2102-render-desktop-login-run2.json` | `e56618dbed50…` | `eb43a93f5061…` |
| `lhci-20260903-2102-render-desktop-mascotas-run3.json` | `37e3945175ea…` | `367c658d3837…` |
| `lhci-20260903-2102-render-desktop-mascotas-run4.json` | `c621a73e9dd2…` | `f4c3d30c14dc…` |
| `lhci-20260903-2102-render-desktop-mascotas-run5.json` | `36a928e9ea12…` | `06297cc8ebb4…` |
| `lhci-20260903-2102-render-mobile-login-run0.json` | `ddb49fa31a3f…` | `cf84352d05b6…` |
| `lhci-20260903-2102-render-mobile-login-run1.json` | `ba8f1e9661d7…` | `34c04c367a44…` |
| `lhci-20260903-2102-render-mobile-login-run2.json` | `fe3af4c2e116…` | `40ba6769067c…` |
| `lhci-20260903-2102-render-mobile-mascotas-run3.json` | `60a40d05f8e8…` | `4fb23e919a4f…` |
| `lhci-20260903-2102-render-mobile-mascotas-run4.json` | `4ffab5f2bdd5…` | `96d5fe853a1a…` |
| `lhci-20260903-2102-render-mobile-mascotas-run5.json` | `e6213a943c2c…` | `c5205bc9d349…` |

Cada JSON conserva `requestedUrl` y `finalUrl` apuntando a
`https://biopet-frontend.onrender.com` (verificado individualmente en cada
uno por `scripts/run-lighthouse-render.sh` antes de archivarlo, y de forma
agregada por `compute-render-averages.mjs`), y `fetchTime` real por corrida
(entre `2026-09-03T21:02:43Z` y `2026-09-03T21:04:37Z`).

## Integración con el Makefile (pendiente, a cargo de Fred)

Este hallazgo (#22 del plan de recalificación) asigna a Fred conectar esta
corrida al `Makefile`, sin duplicar archivos. La forma de invocarla es:

```bash
bash scripts/run-lighthouse-render.sh
```

No requiere contenedor local corriendo (audita el despliegue público
directamente); solo red saliente hacia `biopet-frontend.onrender.com` y
Chrome instalado. Un target nuevo en el `Makefile` (p. ej.
`lighthouse-render`) que invoque exactamente ese comando es suficiente —
no se necesita ninguna otra pieza de este directorio para conectarlo.
