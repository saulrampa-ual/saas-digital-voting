# Encargo de pruebas P7 · Fase 2

Continuación de [`encargo-pruebas-p7.md`](encargo-pruebas-p7.md). Las reglas del apartado 0 de ese encargo siguen
vigentes: datos sintéticos, secreto del voto, sin secretos en el repo, sin cambios en código de producción, sin inventar
resultados, sin push ni merge y sin tocar `Plantilla-TFM-Informatica-UAL/` ni `prototiposPantallas/`.

- **Fecha:** 31/10/2026.
- **Base:** `dev` en `b1b68e6` o posterior.
- **Rama nueva desde `dev`:** `test/p7-phase2`. Esta vez **sí** se pueden hacer commits en esa rama (Conventional
  Commits, en inglés).
- **Worktree anterior:** la de `test/p7-pending-suite` ya está integrada en `dev` y no se reutiliza.

## 1. Qué cambió desde la fase 1

- **Integrado en `dev`:** las pruebas de la fase 1 (60 de backend, 10 de Vitest y 4 casos Playwright/axe) y los
  resultados de `docs/evaluation/resultados-p7/`.
- **DEF-01 corregido:**
  - **causa:** el selector de idioma compartido;
  - **arreglo:** el nombre accesible es ahora el código visible más el nombre oculto del idioma;
  - **verificación en el entorno de la IA:** axe 0 en `/recibo/7` y `/votar/…` (1280/360, ES/EN), Vitest 10/10 y build
    correctos.
- **DEF-02 reclasificado (D-50):** el enlace de la invitación va en la ruta `/votar/:linkToken` por diseño. En la URL y
  en `localStorage`/`sessionStorage` nunca deben aparecer el código, el token de voto o de resultados ni el recibo. Las
  pruebas comprueban exactamente eso.
- **Usuarios y roles ficticios:** la guía para crearlos (`TESTA`/`TESTB`) está en
  `docs/evaluation/resultados-p7/manual-simulacion-roles.md`.

## 2. Tareas, en este orden

### 2.1 Confirmar DEF-01 en Windows · A

`npm run test:e2e`, con los 4 casos existentes. Esperado: 0 críticos y 0 serios. Anotar el resultado real en
`e2e-axe.md`.

### 2.2 Stack completo y escenario de prueba · A

1. Levantar la infraestructura, los cinco servicios, el gateway y el frontend (`local-infrastructure.md`,
   `api-testing.md`).
2. Crear las cuentas de `manual-simulacion-roles.md`.
3. Anotar en `entorno.md` las versiones reales de las imágenes de Keycloak y Mailpit.

### 2.3 E2E real y axe en todas las pantallas (H1a, parte automática) · A

**Lectura de códigos y credenciales**
- Los códigos de acceso se leen con la API de Mailpit (`/api/v1/messages`, `/api/v1/message/{id}`) dentro del test.
- Nunca se imprimen ni se guardan.
- Las credenciales de las cuentas de gestión van por variables de entorno.

**Votante**, en 1280×800 y 360×800, ES y EN:
- camino feliz hasta «Comprobar mi recibo»;
- voto en blanco;
- código erróneo con intentos restantes;
- «La votación todavía no está abierta», «Ya has votado», votación cerrada y enlace no válido;
- cambio de idioma sin perder la selección;
- V7 con ballot-box parado y reintento: un solo voto;
- resultado para el censo (RF-15) cuando el proceso esté escrutado.

**Gestión:** login (contraseña + código) y cada menú visible por rol:
- organizaciones, censos con importación, procesos (A3/A4), participación;
- resultados con CSV y acta, auditoría con verificación;
- trazabilidad, privacidad (F2), claves de API;
- resultados públicos `/resultados/…`.

**axe** (`wcag2a`, `wcag2aa`, `wcag21a`, `wcag21aa`, `wcag22aa`) en **cada pantalla y estado** recorrido. Guardar el
JSON con las reglas y los selectores (no solo los recuentos) **fuera del repo**, y en `e2e-axe.md` una tabla:

| pantalla | estado | ancho | idioma | críticos | serios | moderados | menores | reglas |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |

**Comprobaciones en cada prueba:**
- ni código, ni token, ni recibo en la URL ni en el almacenamiento;
- sin errores de consola;
- foco visible al tabular.

**Si aparecen incumplimientos críticos o serios:** abrir un `DEF-xx` por cada causa distinta, con su selector. No
corregir: lo decide el autor.

### 2.4 Secreto del voto en logs y en el flujo · A

- **Logs:** tras el E2E, buscar en los logs de los **cinco servicios y del gateway** enlaces (64 caracteres base64url),
  códigos de 6 cifras asociados a correos, tokens de voto o de resultados, recibos (64 hex) y direcciones de correo.
  En ballot-box no puede haber ninguna dirección de correo ni identificador del censo.
- **Qué entregar de los logs:** el comando, el número de coincidencias por servicio y, si las hay, el contexto
  **anonimizado**.
- **Checklist:** completar los puntos 3, 4, 6 y 8–11 de la checklist `voting-secrecy-guard` en `secreto.md`.

### 2.5 Carga (H1c) · A

1. Instalar k6 (`winget install k6`) y anotar su versión.
2. Escribir en `tests/load/` el script de preparación. Debe crear:
   - una organización y un censo sintéticos de 500 personas;
   - un proceso abierto;
   - 500 tokens de voto (enlace de la invitación, código desde la API de Mailpit) en un JSON **fuera del repo**.
3. **El token de voto caduca a los 15 minutos:** generar los tokens justo antes de cada ejecución. Cada ejecución usa
   un proceso y tokens nuevos.
4. Hacer **3 ejecuciones** de `tests/load/voting.js` con 500 VU × 1 y la misma configuración. Por ejecución, anotar:
   - p50, p90, p95 y p99;
   - errores y checks fallidos;
   - duración;
   - CPU/RAM pico (`docker stats` y los procesos Java);
   - después, `POST …/audit-events/verification` con `intact: true` y votos en urna = personas que votaron.
5. **Prioridad B:** escenario de acceso (pedir y verificar código) y tiempo de escrutinio con ≥ 500 votos.
6. **Criterio H1c:** p95 de emisión < 2 s en las 3 ejecuciones. Si no se cumple, se informa tal cual.

### 2.6 Matriz de autorización completa (RNF-14, RNF-15, D-43) · A

- Una prueba HTTP por servicio, con todos los endpoints públicos del contrato y los roles SUPERADMIN, ADMIN, AUDITOR,
  CENSUS_MANAGER, INTEGRATION y sin token, incluidos los cruces entre organizaciones.
- **Rutas `/internal/**`:** dependen de Spring Security (rol `SERVICE` y claim `azp`), no del dominio. Usar
  `jwt().authorities(...)` con el conversor real del servicio (o con las autoridades que genera) y comprobar 401, 403
  con otro `azp` y 2xx con el `azp` correcto.
- **Papeleta:** `GET …/ballot` sin `X-Voting-Token`, con un token inventado o con uno de otro proceso → 404.
- Añadir `spring-security-test` (scope test, sin versión) en los servicios que lo necesiten.

### 2.7 Concurrencia y outbox (RNF-12, D-20, D-21, D-30, D-32) · A

- **Voto único:** N ≥ 20 votos en paralelo con el mismo token → 1 voto, 1 recibo y 1 evento; el resto 409; la persona
  marcada una sola vez.
- **Compensación:** reversión del consumo si falla el guardado en la urna; si la reversión falla, incidencia
  `REVERT_FAILED` solo con el día.
- **Outbox `process_sync`:** orden, reintentos, `FAILED` con 400/409/422, receptores idempotentes.
- **Transiciones:** dos transiciones simultáneas del mismo proceso → solo una gana.

### 2.8 Unitarias restantes · B

Las áreas del bloque 3 del primer encargo que siguen sin pruebas:
- privacidad (RF-18) y recordatorios (RF-17);
- entrada con cuenta institucional (RF-05b) y tokens de resultados (RF-15);
- acta (RF-16) y trazabilidad (RF-19);
- cruces del escrutinio (`TALLY_FAILED`, `PARTICIPATION_MISMATCH`) y gateway;
- en el frontend, la máquina de estados del votante y las validaciones de A3 y CSV.

### 2.9 Cierre · A

- `mvnw verify` completo y JaCoCo homogéneo (actualizar `cobertura.md` y los CSV).
- `npm run test:ci` y `npm run build`.
- `npm run test:e2e`.

## 3. Qué entregar

Todo en `test/p7-phase2`, con commits y **sin** push ni merge. Ninguna evidencia puede contener credenciales,
enlaces, códigos, tokens, recibos ni correos reales.

| # | Entregable |
| --- | --- |
| 1 | Pruebas nuevas (backend, Vitest, Playwright, scripts de carga y de preparación) |
| 2 | `docs/evaluation/resultados-p7/` actualizado. En cada fichero, una sección «Fase 2» al final; no borrar lo de la fase 1: `entorno`, `informe-ejecucion`, `recuento-pruebas`, `cobertura` + CSV, `autorizacion`, `secreto`, `e2e-axe`, `carga`, `defectos`, `propuesta-evidencia-p7` |
| 3 | Resúmenes JSON de k6 (sin tokens) y JSON de axe por pantalla, fuera del repo, con su ruta anotada |
| 4 | `commits.md` con el hash y el mensaje de cada commit |
| 5 | `resumen.md`, sección «Fase 2» (≤ 1 página): qué pasa, qué falla, defectos nuevos y estado de H1a (parte automática) y de H1c |

**Cómo decidir las hipótesis:**
- **H1a:** solo se informa la parte automática (cero críticos y cero serios en todas las pantallas evaluadas). La
  hipótesis completa queda PENDIENTE hasta la revisión manual con NVDA del autor.
- **H1c:** se acepta o rechaza solo con las 3 ejecuciones completas; si no, PENDIENTE.

## 4. Fuera de este encargo

- Las sesiones de usabilidad con 5 votantes (H1b) y la revisión con lector de pantalla: las hace el autor.
- RNF-4 con administradores.
- RF-05a con un proveedor de identidad externo real.
- RF-11, RF-22 y RF-23.
