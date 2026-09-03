# Z4 — Validación de cabeceras de seguridad (Fred F2) sin romper el frontend

Registro de la validación del hallazgo de recalificación "Validar cabeceras
de seguridad y no romper el frontend" (Zaida, validación; Fred implementa
las cuatro cabeceras en la capa de hosting). Este documento se actualiza en
dos momentos: **antes** del merge de Fred (baseline) y **después** de su
despliegue (validación funcional).

## 1. Baseline pre-merge (paso 27)

Ejecutado **antes** de que Fred integre las cabeceras, sobre el frontend
actual, para tener una referencia de que el build ya pasaba independiente
del cambio de Fred.

- **Fecha/hora (UTC-05:00):** 2026-09-03 16:16 (commit `9e03208`, rama
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

## 3. Validación post-deploy (pasos 28–32) — **PENDIENTE**

Bloqueada por la dependencia declarada en el plan ("Fred F2"). Esta
sección se completa en cuanto el despliegue con las cuatro cabeceras esté
en producción; no se simula ni se anticipa con datos de otro entorno,
siguiendo el mismo criterio aplicado en Z2/Z3 (no reportar como evidencia
final algo que todavía no ocurrió contra el sistema real).

Checklist a ejecutar en cuanto Fred notifique el despliegue:

- [ ] Abrir `https://biopet-frontend.onrender.com` en ventana privada.
- [ ] Iniciar sesión con usuario demo y navegar `/login` → `/mascotas` (y
      demás rutas principales).
- [ ] Revisar la consola del navegador: confirmar que ninguna llamada al
      backend (`https://biopet-backend-dh5e.onrender.com/api/*`) sea
      bloqueada por CSP (sin errores `Refused to connect`/`Refused to
      load`).
- [ ] `curl -I https://biopet-frontend.onrender.com/login` y confirmar
      presencia de `Strict-Transport-Security`, `Content-Security-Policy`,
      `X-Frame-Options`, `X-Content-Type-Options`.
- [ ] Si algo se rompe: **no parchear el header por cuenta propia** —
      reportar a Fred la directiva CSP exacta (o cabecera) que bloquea el
      recurso, con la URL bloqueada y el mensaje literal de consola.

## Criterio de aceptación — estado actual

| Condición | Estado |
|---|---|
| Build local pasa | ✅ Verificado (sección 1) |
| Frontend público carga | ✅ (ya lo hace hoy, sin las cabeceras nuevas) |
| Login funciona | Pendiente de reverificar tras el deploy de Fred |
| Las cuatro cabeceras presentes | ❌ Todavía no desplegadas |
| Sin errores CSP funcionales | No aplica todavía (CSP no desplegada) |

**Conclusión parcial:** la mitad de Z4 que no depende de Fred (baseline
pre-merge) está cerrada y en verde. La otra mitad (validación post-deploy)
queda explícitamente pendiente hasta que Fred despliegue F2; este
documento es el punto de retorno para completarla sin repetir el baseline.
