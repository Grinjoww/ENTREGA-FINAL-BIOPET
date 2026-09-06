# Evidencia OWASP — un archivo curl por control

La revisión del 5 de septiembre de 2026 señaló, sobre P3 (Seguridad OWASP +
ZAP + auditoría de acceso a datos): "la evidencia OWASP por control está
resumida en un README y no como salidas curl separadas por control." Este
directorio responde exactamente a esa observación: un archivo de texto por
control, con el comando `curl` (u `openssl` cuando el control es TLS) y su
salida real, capturado contra el despliegue académico público — no contra
`localhost`.

## Qué es esto y qué no reemplaza

- **No sustituye** la evidencia ya existente en `../raw/A0X-*.txt` ni los
  documentos detallados `../A0X-*.md`, ni el README consolidado
  `../owasp/README.md`. Esa evidencia sigue vigente y cubre casos que un
  curl aislado no puede cubrir (flujo autenticado completo con dos
  usuarios, rate-limiting hasta el bloqueo, logs de contenedor).
- **Sí añade** exactamente lo que faltaba: un archivo por control, con
  comando reproducible y salida real, contra
  `https://biopet-backend-dh5e.onrender.com` y
  `https://biopet-frontend.onrender.com`.

## Controles cubiertos

| Control | Archivo | Qué demuestra con curl | Qué NO demuestra con curl (ver evidencia complementaria) |
|---|---|---|---|
| A01 — Broken Access Control | [`A01-curl.txt`](A01-curl.txt) | 401 sin sesión sobre dos endpoints protegidos | Control de propiedad entre dos usuarios autenticados (IDOR) → `../raw/A01-access-control.txt` |
| A02 — Cryptographic Failures | [`A02-curl.txt`](A02-curl.txt) | TLS 1.3 real y certificado vigente | Atributos de cookie de sesión → `../raw/A07-auth-rate-limit.txt` |
| A03 — Injection | [`A03-curl.txt`](A03-curl.txt) | 422 ante payload de inyección en `email`, sin fuga de stack trace | Parámetros no validados como email, conteo de filas antes/después → `../raw/A03-injection.txt` |
| A05 — Security Misconfiguration | [`A05-curl.txt`](A05-curl.txt) | Las 4 cabeceras presentes en backend Y frontend públicos | Manejo de errores verboso (evidenciado por prueba automatizada) |
| A07 — Identification and Authentication Failures | [`A07-curl.txt`](A07-curl.txt) | 401 genérico ante credenciales inválidas, sin cookie | Secuencia completa de rate-limit hasta 429 → `../raw/A07-auth-rate-limit.txt` (no reproducida en producción para no bloquear el acceso real al demo) |
| A09 — Security Logging and Monitoring Failures | [`A09-curl.txt`](A09-curl.txt) | El lado cliente del evento (`LOGIN_FAILURE` real, con `X-Request-ID` de correlación) y su fundamento en el código (`AuthService`/`AuthenticationAuditService`) | Que el servidor efectivamente escriba y persista esa línea de log → `../raw/A09-audit-logs.txt` y `AuthenticationAuditServiceTest` |

Se seleccionaron los mismos seis controles que ya usa
`../owasp/README.md` (A01, A02, A03, A05, A07, A09), la lista oficial ya
adoptada por el equipo para esta entrega; A04 y A06 siguen documentados por
separado como antes.

## Reproducción

```bash
bash scripts/capture-owasp-curl.sh
```

El script no requiere ninguna credencial: los cinco controles con
evidencia curl real (A01, A02, A03, A05, A07) solo hacen peticiones sin
autenticar contra el despliegue público. No crea cuentas, no modifica
datos, y no imprime ningún secreto (no hay ninguno involucrado: A03 y A07
usan credenciales deliberadamente inválidas).

## Sobre secretos y datos sensibles

Ningún archivo de este directorio contiene un JWT real, una cookie de
sesión real, una contraseña real ni ninguna credencial de producción. A03
y A07 usan, a propósito, credenciales inválidas o inexistentes: eso es lo
que el control necesita demostrar (que el sistema las rechaza), no una
omisión de sanitizado.
