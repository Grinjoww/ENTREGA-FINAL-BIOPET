# Z4 — Validación de cabeceras de seguridad sin romper el frontend

Registro de la validación del hallazgo de recalificación "Validar cabeceras
de seguridad y no romper el frontend".

**Actualización 2026-09-04 — reasignación de Jaime.** El plan original
(Zaida valida, Fred implementa las cuatro cabeceras "en la capa de
hosting") asumía que las cabeceras se agregarían del lado del backend/
despliegue general. Jaime pidió expresamente que Zaida revise y, si
corresponde, configure las cabeceras **específicas del frontend
desplegado** (`biopet-frontend.onrender.com`, servido por el nginx propio
del contenedor del frontend — `frontend/nginx.conf.template` — que nunca
tuvo cabeceras configuradas, a diferencia del backend que ya las tenía
desde antes en `SecurityConfig.java`, ver
`docs/mediciones/sec/A05-security-headers.md`). No hay conflicto de zonas:
son dos orígenes HTTP distintos (`biopet-frontend...` vs
`biopet-backend-dh5e...`), cada uno con su propia configuración de
cabeceras. Este documento pasa de "esperar a Fred" a documentar el cambio
que Zaida hizo directamente en su zona (frontend).

## 1. Baseline pre-merge (paso 27)

Ejecutado **antes** de que Fred integre las cabeceras, sobre el frontend
actual, para tener una referencia de que el build ya pasaba independiente
del cambio de Fred.

- **Fecha/hora (UTC-05:00):** 2026-09-03 16:16 (commit `5543b62`, rama
  `fix/zaida-frontend-docs-recalificacion`).
- **Comandos:** `npm ci` seguido de `npm run build` (`ng build --configuration production`) en `frontend/`.
- **`npm ci`:** 803 paquetes instalados sin error (fallo de instalación =
  0). Vulnerabilidades reportadas por `npm audit` (55: 4 low / 18 moderate /
  32 high / 1 critical) son deuda técnica preexistente de dependencias de
  Angular 17, no relacionada con este hallazgo ni con las cabeceras; no se
  tocó ninguna dependencia.
- **`npm run build`:** compiló sin errores ni warnings de bundle. Salida:

  ```text
  Initial chunk files   | Names         |  Raw size | Estimated transfer size
  main-3Z2PSGOJ.js      | main          | 280.73 kB |                70.17 kB
  polyfills-FFHMD2TL.js | polyfills     |  33.71 kB |                11.02 kB
  styles-6M66J5WN.css   | styles        |   2.21 kB |               779 bytes
                        | Initial total | 316.65 kB |                81.94 kB
  Output location: frontend/dist/biopet-frontend
  Application bundle generation complete. [3.905 seconds]
  ```

- **Resultado:** ✅ **Build local pasa** — condición previa del criterio de
  aceptación, confirmada antes de que exista ninguna cabecera nueva que
  pudiera romperlo.

## 2. Estado de la dependencia (Fred F2)

Verificado el **2026-09-03 21:17 UTC** contra el despliegue público:

```bash
curl -sI https://biopet-frontend.onrender.com/login
```

```text
HTTP/1.1 200 OK
Date: Thu, 03 Sep 2026 21:17:29 GMT
Content-Type: text/html
Connection: keep-alive
cf-cache-status: DYNAMIC
last-modified: Tue, 18 Aug 2026 05:54:02 GMT
rndr-id: 34674af1-9b6e-4f0c
Server: cloudflare
vary: Accept-Encoding
x-render-origin-server: nginx/1.25.5
etag: W/"6a83f37a-32c"
CF-RAY: a357c811a984180a-BOG
alt-svc: h3=":443"; ma=86400
```

**Ninguna de las cuatro cabeceras exigidas está presente todavía**
(`Strict-Transport-Security`, `Content-Security-Policy`,
`X-Frame-Options`, `X-Content-Type-Options`): Fred (F2) aún no ha
desplegado el cambio en la capa de hosting.

## 3. Cambio implementado (2026-09-04) — cabeceras del frontend

**Archivo tocado:** `frontend/nginx.conf.template` — únicamente el bloque
`location /` (no se tocó `location /api/`, que hace `proxy_pass` al
backend y cuyas respuestas siguen llegando con las cabeceras que el
backend ya emite, sin duplicarlas ni pisarlas).

**Cabeceras agregadas** (con `always`, para que se emitan también en
respuestas de error):

| Cabecera | Valor |
|---|---|
| `X-Frame-Options` | `DENY` |
| `X-Content-Type-Options` | `nosniff` |
| `Referrer-Policy` | `no-referrer` |
| `Content-Security-Policy` | `default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'` |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains; preload` |

**Por qué la CSP no es tan estricta como la del backend** (que no usa
`'unsafe-inline'`, ver `A05-security-headers.md`): inspeccioné el HTML
real generado por `npm run build`
(`frontend/dist/biopet-frontend/browser/index.html`) y confirmé que
Angular/Critters inyecta un `<style>` inline (CSS crítico) y un atributo
`onload="this.media='all'"` en el `<link>` de estilos — sin
`'unsafe-inline'` en `style-src`/`script-src`, la aplicación se
renderizaría sin hojas de estilo. `connect-src 'self'` es suficiente
porque las llamadas a `/api/*` son mismo origen (proxy interno de este
mismo nginx hacia el backend), nunca cross-origin desde el navegador.
Detalle completo de cada directiva, comentado en el propio archivo.

### Verificación local (antes de tocar Render)

1. `npm ci` + `npm run build` en `frontend/` — ✅ pasó limpio (mismo
   bundle exacto que el baseline de la sección 1: 316,65 kB / 81,94 kB).
2. Build de la imagen Docker real del frontend
   (`docker build ./frontend`) — ✅ exitosa.
3. Contenedor levantado localmente (`docker run`, puerto 18081) y
   verificado con `curl -I`:
   - `GET /` (index.html) → **200**, las cinco cabeceras presentes con
     los valores exactos de la tabla de arriba.
   - `GET /main-3Z2PSGOJ.js` (bundle real) → **200**, mismas cabeceras.
   - `GET /login` (ruta de Angular, no un archivo real) → **200**, sirve
     `index.html` vía `try_files` — el *fallback* de rutas de la SPA
     sigue funcionando con las cabeceras nuevas.
   - `GET /ruta-inexistente` → **200** (mismo fallback SPA, comportamiento
     sin cambios respecto a antes de agregar cabeceras).
4. Contenedor e imágenes de prueba eliminados tras la verificación
   (nada quedó corriendo, nada se subió a ningún registro).

**Lo que NO pude verificar (limitación real de esta sesión):** no tengo
navegador/herramienta de automatización disponible aquí, así que no pude
abrir la app en un navegador real y confirmar visualmente que Angular
renderiza sin errores de CSP en la consola (paso 30 del plan). El análisis
de la sección anterior (inspección directa del HTML de build) da alta
confianza de que la CSP elegida no rompe nada, pero **no sustituye una
verificación visual real en el navegador**.

## 4. Pendiente — validación contra producción (pasos 28–32)

Todavía **no desplegado** en Render (el cambio vive sin commitear en el
árbol de trabajo). Checklist a ejecutar tras el merge y el deploy:

- [ ] Abrir `https://biopet-frontend.onrender.com` en ventana privada.
- [ ] Iniciar sesión con usuario demo y navegar `/login` → `/mascotas` (y
      demás rutas principales).
- [ ] Revisar la consola del navegador: confirmar que ninguna llamada al
      backend (`https://biopet-backend-dh5e.onrender.com/api/*` — nota:
      en producción esto pasa por `/api/*` del mismo origen, no
      cross-origin) sea bloqueada por CSP.
- [ ] `curl -I https://biopet-frontend.onrender.com/login` y confirmar
      presencia de las cinco cabeceras con los valores de la tabla.
- [ ] Si algo se rompe: **no parchear a ciegas** — identificar la
      directiva CSP exacta que bloquea el recurso (consola del navegador
      lo dice literalmente) y documentarlo aquí antes de ajustar.

## Criterio de aceptación — estado actual

| Condición | Estado |
|---|---|
| Build local pasa (antes y después) | ✅ Verificado (secciones 1 y 3) |
| Frontend público carga | ✅ hoy sin cabeceras; verificado localmente CON cabeceras (sección 3) |
| Login funciona | Verificado localmente que la SPA sirve/enruta correctamente con las cabeceras; **falta confirmación visual real en navegador** y contra producción |
| Las cinco cabeceras presentes | ✅ Verificado localmente (sección 3); ❌ aún no en producción — pendiente merge + deploy |
| Sin errores CSP funcionales | Alta confianza por análisis del build real; sin confirmar en navegador real (limitación de esta sesión) |
