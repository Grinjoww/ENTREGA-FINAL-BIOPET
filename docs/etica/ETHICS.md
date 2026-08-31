# Declaración ética y de gestión de datos — BIOPET

Conforme al bloque F de la Guía de la Tercera Entrega, este documento declara
los aspectos éticos del uso de datos y de participantes en el proyecto BIOPET,
aunque el sistema no manipule datos de salud, financieros ni personales
sensibles en el sentido regulatorio del término.

## i. Fuentes de datos y su licencia

El proyecto utiliza dos categorías de datos, ambas generadas por el propio
equipo, sin datos de terceros ni conjuntos de datos externos:

1. **Datos sintéticos de desarrollo y prueba.** El usuario administrador
   creado automáticamente al arrancar el sistema (`admin@biopet.ec`, según
   `README.md` del proyecto v0.9.0-rc) y cualquier dato semilla (`db/seed.sql`)
   son ficticios, generados por el equipo para fines de desarrollo. No
   provienen de una clínica veterinaria real ni de datos de mascotas o dueños
   reales.
2. **Datos empíricos de evaluación (bloque C de la Guía).** Los datos crudos
   de rendimiento (k6), seguridad (curl/OWASP) y cobertura (JaCoCo) son
   generados por herramientas automatizadas contra el propio sistema, sin
   intervención de personas ni datos personales. Los datos de usabilidad
   (SUS, bloque C.3) sí involucran participantes humanos y se tratan en la
   sección "ii" de este documento.

El código fuente del proyecto se distribuye bajo licencia MIT (ver `LICENSE`
en la raíz del repositorio). No se identificó en los repositorios provistos
ningún conjunto de datos de terceros con licencia propia (por ejemplo,
datasets públicos de mascotas o clínicas) que deba declararse aquí.

## ii. Tratamiento de datos personales

El sistema BIOPET, en su alcance implementado hasta esta versión (v0.9.0-rc),
almacena los siguientes datos personales de sus propios usuarios finales,
conforme al diccionario de datos del SRS (sección 5.1):

- **Tabla `usuarios`**: nombre, correo electrónico y contraseña (con hash
  BCrypt; el SRS declara explícitamente que "ninguna credencial se guarda en
  texto plano", sección 2.5).
- **Tabla `mascotas`**: nombre, especie, raza, fecha de nacimiento y el
  identificador del dueño (`duenio_id`), que no son datos personales por sí
  mismos, pero quedan asociados a un usuario identificable.

Estos datos corresponden a los propios integrantes del equipo y a datos de
prueba durante el desarrollo; el proyecto no ha sido desplegado con datos de
clientes reales de una clínica veterinaria a la fecha de esta entrega. No se
recolectan datos de salud humana, datos financieros sensibles ni categorías
especiales de datos personales.

**Medidas de protección aplicadas, verificadas en el código:**

- Contraseñas con hash BCrypt (`UserDetailsServiceImpl`, REQ-NF-005).
- Autenticación JWT con cookie `HttpOnly + Secure + SameSite=Strict`
  (restricción de la sección 2.5 del SRS).
- Revocación de tokens mediante lista negra en Redis (ADR-003).
- Comunicación cifrada exigida mediante HTTPS/TLS 1.3 (REQ-NF-003, pendiente
  de evidencia empírica según el SRS, sección 7).

Si en fases posteriores (Entrega Final o producción real) el sistema procesa
datos de clientes reales de clínicas veterinarias, este documento deberá
actualizarse para incorporar una base legal de tratamiento y, si aplica en
Ecuador, referencia a la Ley Orgánica de Protección de Datos Personales
(LOPDP). Esa actualización no se realiza en esta entrega por no existir
todavía dicho procesamiento en el alcance implementado.

## iii. Mecanismo de consentimiento informado para las pruebas de usabilidad

El bloque C.3 de la Guía exige aplicar el instrumento System Usability Scale
(SUS) de Brooke con un mínimo de diez participantes externos al equipo del
PFC. Para esa actividad, el equipo aplica el siguiente mecanismo:

1. Cada participante recibe y firma (física o digitalmente) el formulario de
   consentimiento informado disponible en
   `docs/etica/consentimientos/plantilla.md`, antes de ejecutar la tarea de
   onboarding (login, alta de un registro, edición, eliminación lógica,
   logout) sobre la que se mide la usabilidad.
2. El consentimiento informa el propósito de la prueba, la tarea a realizar,
   el carácter voluntario de la participación, el tratamiento anonimizado de
   los datos y el derecho a retirarse en cualquier momento sin consecuencias.
3. Los formularios firmados debían archivarse fuera del repositorio
   público y referenciarse únicamente por código de participante (P01,
   P02, ...), conforme al bloque C.3 de la Guía — ver la rectificación
   sobre este punto más abajo.
4. Los resultados agregados (matriz cruda `docs/mediciones/sus/sus-raw.csv` y
   reporte con media, desviación típica e IC 95 %) se vinculan al código de
   participante, nunca a su nombre, correo o cualquier otro dato identificable.

**Estado a la fecha de esta entrega:** la prueba SUS se ejecutó inicialmente
con diez participantes externos al equipo (P01–P10, Tercera Entrega) y se
amplió para la Entrega Final con ocho participantes adicionales (P11–P18),
alcanzando n=18 (por encima del mínimo de 15 exigido). Los datos crudos y
el reporte agregado están disponibles en `docs/mediciones/sus/sus-raw.csv`
y `docs/mediciones/sus/REPORT.md`, respectivamente; el puntaje SUS de cada
registro es matemáticamente reproducible desde sus respuestas Q1–Q10
(`scripts/analisis-sus.py`).

**Rectificación (2026-08-31):** según declaración del equipo, los 18
registros P01–P18 corresponden a participantes reales. Durante la
auditoría documental posterior de la Entrega Final se constató que
actualmente no se dispone de evidencia verificable de los formularios
individuales de consentimiento mencionados en el punto 3 de arriba y en
la versión original de esta sección, que afirmaba su existencia y
custodia fuera del repositorio. Esta situación se documentó mediante una
constancia de regularización firmada por los tres integrantes del
proyecto —
[`docs/etica/regularizacion-sus/CONSTANCIA-REGULARIZACION-SUS-BIOPET-2026-08-31.pdf`](regularizacion-sus/CONSTANCIA-REGULARIZACION-SUS-BIOPET-2026-08-31.pdf),
ver también [`docs/etica/regularizacion-sus/README.md`](regularizacion-sus/README.md) —,
**la cual no sustituye consentimientos individuales, no incorpora firmas
de participantes y no fue generada como evidencia retrospectiva de que
esos consentimientos existieron.** Su único propósito es dejar constancia
transparente de la situación documental encontrada.

## iv. Ausencia de datos identificables en el repositorio público

- Los formularios de consentimiento firmados individuales, si existen,
  no se suben al repositorio público bajo ninguna circunstancia; en el
  repositorio solo está la plantilla vacía
  (`docs/etica/consentimientos/plantilla.md`). Ver la rectificación de
  la sección iii: actualmente no se dispone de esos formularios firmados
  como evidencia verificable — no se trata solo de que no se publiquen,
  sino de que el equipo no puede exhibirlos hoy.
- Los datos crudos de usabilidad que se archiven en
  `docs/mediciones/sus/sus-raw.csv` deben identificar a cada participante
  únicamente por su código (P01, P02, ...), nunca por nombre, correo u otro
  dato personal.
- Las credenciales de prueba documentadas en `README.md`
  (`admin@biopet.ec` / `Admin123*`) son datos ficticios de desarrollo, no
  corresponden a una persona real y se mantienen únicamente para facilitar la
  reproducibilidad exigida por el bloque B de la Guía.
- El equipo no identificó, en los repositorios provistos, ningún archivo de
  datos crudo, log o captura que contenga nombres, correos reales o
  contraseñas en texto plano de personas reales.


