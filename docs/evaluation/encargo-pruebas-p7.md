# Encargo de pruebas P7 · VeriVoto

Documento para entregar a otra IA (o persona) que ejecutará las pruebas pendientes de P7 en el equipo del autor.
Recoge qué hay hecho, qué falta, cómo hacerlo y **qué debe entregar** para que el autor pueda continuar con P7.7
(informe) y P8 (memoria).

Fecha del encargo: 31/10/2026 · Rama base: `dev` (`cc0229b`).

---

## 0. Antes de empezar (obligatorio)

1. Leer completos `CLAUDE.md`, `docs/ai/CONTEXT.md`, `docs/ai/PROGRESS.md`, `docs/ai/DECISIONS.md` (D-01..D-49),
   `docs/ai/TRACEABILITY.md`, `docs/architecture/threat-model.md`, `docs/architecture/api-testing.md` y
   `docs/evaluation/informe.md`.
2. Reglas que no se pueden saltar:
   - **Datos sintéticos** siempre (dominio `@verivoto.test`). Nunca correos, nombres ni votos reales.
   - **Secreto del voto (RNF-11):** ninguna prueba, log, fixture ni informe puede enlazar identidad y voto. Usar la
     skill `voting-secrecy-guard` al terminar cualquier cambio que toque `voter-access` o `ballot-box`.
   - **Sin secretos en el repositorio:** tokens, enlaces, códigos OTP, recibos y contraseñas solo en ficheros locales
     fuera del repo o en variables de entorno.
   - **No modificar código de producción.** Si una prueba descubre un fallo, se deja la prueba fallando o marcada como
     `@Disabled("DEF-xx: …")` con referencia al defecto y se documenta (apartado 4). Las correcciones las aprueba el autor.
   - **No inventar resultados.** Solo cifras de ejecuciones reales; lo no ejecutado se marca `PENDIENTE`.
   - Código, nombres de rama y commits en inglés (Conventional Commits); documentación en español.
   - No hacer `push`, `merge`, `rebase` ni `reset --hard`. No tocar `Plantilla-TFM-Informatica-UAL/` ni
     `prototiposPantallas/`.
   - Cada respuesta al autor incluye la sección «Asunciones y cambios de comportamiento».
3. Trabajar en una rama nueva desde `dev`: `test/p7-pending-suite`.

## 1. Entorno necesario

| Elemento | Versión / requisito |
| --- | --- |
| Sistema | Windows del autor (PowerShell) con Docker Desktop en marcha |
| Java | 25 LTS (última ejecución registrada: Temurin 25.0.5) |
| Maven | Maven Wrapper del repo (`backend/mvnw.cmd`) |
| Node.js / npm | 24 LTS (registrado 24.15.0) |
| PostgreSQL | 18.6 (Testcontainers y `infra/compose.yaml`) |
| Keycloak / Mailpit | 26.8.1 / v1.31.5 (`infra/compose.yaml`) |
| Playwright | Añadirlo como dependencia de desarrollo del frontend con versión fija |
| axe | `@axe-core/playwright`, versión fija |
| k6 | Instalado por el autor; anotar versión |

Arranque del stack: `docs/architecture/local-infrastructure.md` y `docs/architecture/api-testing.md`.

## 2. Estado de partida (verificado el 31/10/2026)

**Backend: 49 pruebas `@Test`**, todas en verde en la última ejecución registrada de `mvnw verify`:

| Servicio | Pruebas existentes |
| --- | --- |
| api-gateway | `ArchitectureTest` (1) |
| identity | `ApiKeyServiceTest` (4), `ManagementUserServiceTest` (2), `CallerTest` (2), `ArchitectureTest` (2), `IdentityPostgresIntegrationTest` (2) |
| election | `VotingProcessTest` (3), `ProcessStatusTest` (2), `CallerTest` (3), `ArchitectureTest` (2), `ElectionPostgresIntegrationTest` (2) |
| voter-access | `VotingTokenServiceTest` (3), `AccessCredentialTest` (3), `ArchitectureTest` (2), `VoterAccessPostgresIntegrationTest` (2) |
| ballot-box | `VotingServiceTest` (4), `ChainVerifierTest` (5), `TallyResultTest` (5), `ArchitectureTest` (2), `BallotBoxPostgresIntegrationTest` (2) |

**Frontend: 10 pruebas** (Vitest): `role.guard.spec.ts`, `voter-api.service.spec.ts`, `voter-sso.service.spec.ts`.

**Cobertura de líneas JaCoCo** (batería inicial): gateway 0,0 %, identity 13,6 %, election 10,3 %,
voter-access 7,7 %, ballot-box 13,2 %.

**Material preparado y no ejecutado:** `tests/load/voting.js` (k6, solo emisión) y `docs/evaluation/carga.md`;
`docs/evaluation/accesibilidad-manual.md`. No existen pruebas E2E ni Playwright.

**Hipótesis:** H1a, H1b y H1c PENDIENTE. RNF-4 PENDIENTE (sin hipótesis).

## 3. Pruebas que faltan

Prioridad: **A** = imprescindible para cerrar P7 · **B** = muy recomendable · **C** = si da tiempo.
Cada bloque indica qué demostrar, con qué y qué evidencia guardar.

### Bloque 1 · Autorización HTTP y aislamiento entre organizaciones (P7.2) · Prioridad A

RNF-14, RNF-15, D-17, D-22, D-43. Hoy no hay ninguna prueba HTTP de autorización.

- Por servicio: `@SpringBootTest` + MockMvc o WebTestClient + Testcontainers, con JWT de prueba
  (`spring-security-test`, `jwt()` con los claims `organization_id`, roles y `azp`). Nada de Keycloak real aquí.
- Casos mínimos:
  1. **Entre organizaciones:** un ADMIN/AUDITOR/CENSUS_MANAGER de la organización A pide recursos de B (censo, proceso,
     estado, participación, resultados, CSV, acta, eventos de auditoría, acciones administrativas, claves de API,
     privacidad). Debe recibir 404 o 403 según el contrato, nunca 200, y no recibir datos de B.
  2. **Matriz de roles** por endpoint público: SUPERADMIN, ADMIN, AUDITOR, CENSUS_MANAGER, INTEGRATION y sin token
     (401). Contrastar con `TRACEABILITY.md` y los OpenAPI de `backend/services/*/src/main/resources/openapi/`.
  3. **INTEGRATION** solo lee: cualquier `POST`/`PUT`/`DELETE` responde 403, y solo ve su `organization_id`.
  4. **Rutas `/internal/**`** (D-43): sin token → 401; rol `SERVICE` con un `azp` distinto del permitido → 403; con el
     `azp` correcto → 2xx. Cubrir todas las rutas internas de voter-access, ballot-box, election e identity.
  5. **Papeleta** (D-43, IMP-31): `GET …/ballot` sin `X-Voting-Token`, con un token inventado o de otro proceso → 404.
- Evidencia: una tabla «endpoint × rol × código esperado × código obtenido».

### Bloque 2 · Concurrencia, outbox y repositorios restantes (P7.2) · Prioridad A

RNF-12, RNF-13, D-20, D-21, D-30, D-32.

- **Voto único bajo concurrencia** (Testcontainers con las dos bases): N ≥ 20 peticiones paralelas con el mismo
  token de voto → exactamente 1 éxito (201) y el resto 409; en `pg-ballot` 1 voto, 1 recibo y 1 evento
  `VOTE_RECEIVED`; en voter-access, la persona marcada una sola vez.
- **Compensación:** si falla el guardado en la urna, el token se revierte y un reintento registra un solo voto.
  Si la reversión falla, se crea la incidencia `REVERT_FAILED` solo con el día.
- **Outbox `process_sync`:** orden por proceso y destino; reintentos exponenciales; 400/409/422 → `FAILED` y destino
  bloqueado; receptores idempotentes (el mismo mensaje dos veces no duplica nada).
- **Ciclo de vida:** el cambio condicional `UPDATE … WHERE status = :from` no permite dos transiciones simultáneas;
  el censo queda congelado desde `SCHEDULED`.
- **Repositorios sin integración** (adaptador JPA real, filtrando por organización): procesos, papeletas, censo y
  personas, participación, invitaciones/recordatorios, tokens de resultados, resultados, actas, acciones
  administrativas y claves de API.

### Bloque 3 · Unitarias de reglas sin cubrir (P7.1) · Prioridad A

| Área | RF / decisión | Qué probar |
| --- | --- | --- |
| Importación CSV del censo | RF-03, D-27 | Cabecera obligatoria, separador `,` y `;`, UTF-8, límites de 5 MB y 50 000 filas, duplicados por correo (sin mayúsculas) o identificador dentro del censo y del fichero, `dryRun` y `discardInvalidLines`, transacción única. Usar `scripts/samples/voters-sample.csv` (4 altas, 1 duplicado, 3 inválidas) |
| Privacidad | RF-18, D-46 | Búsqueda, exportación y borrado/anonimización; bloqueo con procesos programados o abiertos; el dato personal nunca va en la URL |
| Recordatorios y avisos | RF-17, D-45 | Máximo 5 por proceso, solo con el proceso abierto, solo a quien no ha votado, se guarda solo el número de destinatarios, cancelación, comprobación justo antes de enviar |
| Entrada con cuenta institucional | RF-05b, D-48 | Correo verificado distinto del censo = intento fallido; `nonce`/`state`; un ID token caducado o de otro cliente se rechaza |
| Tokens de resultados | RF-15, D-41 | 30 min, reutilizable, solo hash, sin la persona; no sirve para votar |
| Acta PDF | RF-16, D-40 | Mismos bytes en dos descargas, `X-Content-SHA256` correcto, sin datos personales ni recibos |
| Trazabilidad | RF-19, D-44 | Los 16 tipos de acción; solo añadir; de las personas del censo solo el número |
| Escrutinio | RF-13, D-37, D-43 | `TALLY_FAILED` si no cuadra con `CLOSED`; `PARTICIPATION_MISMATCH` si no cuadra con voter-access |
| Gateway | RF-21 | Enrutado, CORS y filtros (cobertura actual 0 %) |
| Frontend (Vitest) | RF-08–RF-10, RF-20 | Máquina de estados del votante; nada sensible en la URL ni en `localStorage`; formato del recibo (64 hex); cambiar de idioma no pierde la selección; validación de A3 y del CSV en cliente |

### Bloque 4 · Secreto del voto e integridad (P7.3) · Prioridad A

RNF-11, RNF-13, D-18, ADR-0005, ADR-0008.

- **Esquema:** prueba que recorra **todas** las tablas de `ballot_box_db` y falle si aparece una columna de
  hora/fecha (salvo las permitidas y documentadas) o de identidad (correo, nombre, `roll_entry_id`, token).
  En `voter_access_db`, participación solo con fecha.
- **Logs:** ejecutar el flujo completo (bloque 5) con el nivel de log por defecto y buscar en los logs de **todos** los
  servicios enlaces, códigos OTP, tokens de voto o de resultados, recibos y correos. En los logs de ballot-box no puede
  haber ningún dato de identidad. Entregar el comando de búsqueda y el número de coincidencias (debe ser 0).
- **Cadena:** una prueba por cada tipo de problema de `ChainVerifier` (hash, enlace, secuencia, cabeza, papeleta de
  `OPENED`, nº de eventos = votos = recibos, `CLOSED`, huella final) y verificación de que el escrutinio no publica
  con la cadena rota.
- Pasar la checklist de `.claude/skills/voting-secrecy-guard` y adjuntarla rellena.

### Bloque 5 · Extremo a extremo y accesibilidad automática (P7.4, H1a) · Prioridad A

- Playwright contra el stack real (gateway, cinco servicios, Keycloak y Mailpit). El código OTP se lee con la API de
  Mailpit (`http://localhost:8025/api/v1/…`), nunca impreso en los logs del test.
- **Flujos del votante**, en 1280×800 y 360×800, ES y EN:
  - camino feliz (acceso → papeleta → revisión → voto → recibo → comprobar recibo);
  - voto en blanco y código erróneo con intentos restantes;
  - votación todavía no abierta, «Ya has votado», votación cerrada y enlace no válido;
  - cambio de idioma sin perder la selección;
  - V7 con la urna caída y reintento que registra un solo voto.
- **Flujos de gestión:**
  - login con contraseña y código;
  - crear e importar un censo;
  - crear y programar un proceso;
  - participación, resultados con CSV y acta;
  - auditoría con verificación;
  - trazabilidad, privacidad y claves de API.
- **axe** (`@axe-core/playwright`, etiquetas `wcag2a`, `wcag2aa`, `wcag21a`, `wcag21aa`, `wcag22aa`) en **cada
  pantalla y estado** recorrido. Por pantalla: número de incumplimientos críticos, serios, moderados y menores, con el
  identificador de la regla.
- **Teclado:** recorrido automático con Tab/Shift+Tab/Enter/Espacio del flujo del votante completo (sin ratón) y
  comprobación de que el foco es visible.
- **Comprobaciones de seguridad** en el navegador: sin enlace, código, token ni recibo en la URL, `localStorage` o
  `sessionStorage`; sin errores de consola.
- Proponer (sin activarlo) un job de CI para E2E.

> **Límite:** la revisión con lector de pantalla (NVDA) del criterio de H1a la hace el autor siguiendo
> `docs/evaluation/accesibilidad-manual.md`. La IA solo prepara el entorno y deja anotadas las pantallas y estados
> que hay que revisar.

**Criterio H1a (D-49):** cero incumplimientos críticos ni serios en las pantallas evaluadas, más revisión manual con
teclado y lector de pantalla sin bloqueos en acceso, voto y recibo. axe no certifica WCAG. Un incumplimiento serio
puntual se documenta como incidencia; no se relaja el criterio.

### Bloque 6 · Carga (P7.5, H1c) · Prioridad A

- **Script de preparación** (fuera del repo para los datos y dentro del repo para el código):
  - organización y censo sintéticos de al menos 500 personas;
  - proceso abierto;
  - obtención de ≥ 500 tokens de voto distintos (enlace por invitación, código leído de la API de Mailpit) en un JSON
    local fuera del repo.
- **Atención:**
  - el token de voto caduca a los **15 minutos** (D-31): generar los tokens justo antes de cada ejecución, o ajustar
    la preparación y documentarlo;
  - cada token vale para un solo voto;
  - cada ejecución necesita un proceso y tokens nuevos.
- **Ejecuciones:** 3 de `tests/load/voting.js` con 500 VU × 1, con la configuración idéntica en las tres.
- **Por ejecución:**
  - p50, p90, p95 y p99 de emisión;
  - errores HTTP y checks fallidos;
  - duración;
  - `docker stats` pico por contenedor;
  - después de la carga, verificación de la cadena (`intact: true`) y votos en urna = personas marcadas como votantes.
- **Escenarios adicionales (prioridad B):** acceso (petición y verificación de código) y escrutinio de un proceso
  con ≥ 500 votos (tiempo hasta `TALLIED`).
- **Criterio H1c:** con 500 usuarios simultáneos, p95 de emisión < 2 s en las tres ejecuciones. Un fallo se informa tal
  cual, sin cambiar el criterio.

### Bloque 7 · Cobertura e informe técnico (P7.7) · Prioridad B

- `mvnw verify` completo al final, con JaCoCo por servicio (líneas e instrucciones) extraído de forma homogénea del
  CSV de JaCoCo y comparado con la batería inicial.
- `npm run test:ci` con cobertura y `npm run build`.

### Fuera de este encargo

- **Sesiones de usabilidad con 5 votantes (H1b):** las hace el autor con `docs/evaluation/usabilidad/`.
- **RNF-4 (administradores):** requiere sesión con personas y un protocolo que aún no existe.
- **Revisión con lector de pantalla:** la hace el autor.
- **RF-05a con un IdP externo real:** no hay IdP de prueba.
- RF-11, RF-22 y RF-23 (fuera del MVP).

## 4. Qué debe entregar al autor

Todo en la rama `test/p7-pending-suite` (sin push ni merge). Ninguna evidencia puede contener tokens, enlaces,
códigos, recibos, correos ni contraseñas.

| # | Entregable | Ruta / formato |
| --- | --- | --- |
| 1 | Código de las pruebas nuevas (backend, frontend, Playwright, scripts de carga y de preparación) | En sus módulos; `tests/e2e/` y `tests/load/` |
| 2 | Entorno exacto | `docs/evaluation/resultados-p7/entorno.md`: fecha, commit, SO, CPU, RAM, versiones (Java, Maven, Node, Docker, PostgreSQL, Keycloak, Mailpit, Playwright, axe, k6, navegadores) |
| 3 | Informe de ejecución | `docs/evaluation/resultados-p7/informe-ejecucion.md`: por bloque, pruebas añadidas, RF/RNF que cubren, comandos exactos y resultado (pasa/falla/omitida) con cifras reales |
| 4 | Recuento de pruebas | Total backend y frontend por servicio, a partir de Surefire/Vitest (no estimado), y diferencia frente a 49/10 |
| 5 | Cobertura | `docs/evaluation/resultados-p7/cobertura.md` + los `jacoco.csv` por servicio, comparando con la batería inicial |
| 6 | Matriz de autorización | `docs/evaluation/resultados-p7/autorizacion.md` (bloque 1) |
| 7 | Secreto del voto | `docs/evaluation/resultados-p7/secreto.md`: esquema, búsqueda en logs (comando y coincidencias), checklist `voting-secrecy-guard` |
| 8 | E2E y axe | Resumen en `docs/evaluation/resultados-p7/e2e-axe.md` (tabla pantalla × estado × críticos/serios/moderados/menores, con las reglas) + JSON de axe; informe HTML de Playwright **fuera** del repo (indicar ruta) |
| 9 | Carga | `docs/evaluation/resultados-p7/carga.md`: tabla por ejecución (p50/p90/p95/p99, errores, duración, CPU/RAM), cadena íntegra y votos = participación; resúmenes JSON de k6 sin tokens |
| 10 | Defectos | `docs/evaluation/resultados-p7/defectos.md`: `DEF-xx`, severidad, RF/RNF, pasos de reproducción, prueba que lo detecta, propuesta (no aplicada) |
| 11 | Propuesta de actualización | Texto propuesto (sin aplicar) para la columna «Evidencia P7» de `docs/ai/TRACEABILITY.md`, `docs/evaluation/informe.md` y `docs/ai/PROGRESS.md` |
| 12 | Mejoras | Nuevas `IMP-xx` en `docs/ai/IMPROVEMENTS.md` con estado «Propuesta» |
| 13 | Lista de commits | Hash y mensaje de cada commit de la rama |
| 14 | Resumen para el autor | ≤ 1 página: qué pasa, qué falla, estado de H1a y H1c según los criterios, qué queda pendiente y qué decisiones necesita |

**Decisiones sobre hipótesis.** Solo se escriben si hay evidencia completa:

- **H1a:** solo la parte automática; queda PENDIENTE hasta la revisión con lector de pantalla del autor.
- **H1c:** se acepta o se rechaza solo con las 3 ejecuciones completas; si no, PENDIENTE.

## 5. Checklist de cierre del encargo

- [ ] `mvnw verify` completo ejecutado al final, con el resultado anotado.
- [ ] `npm run test:ci` y `npm run build` ejecutados, con el resultado anotado.
- [ ] Playwright + axe ejecutados en las pantallas listadas.
- [ ] Tres ejecuciones de k6 y comprobación posterior de la cadena y de la participación.
- [ ] Búsqueda de datos sensibles en logs y en las evidencias = 0 coincidencias.
- [ ] Nada fuera de `test/p7-pending-suite`; sin push; sin cambios en código de producción, en la memoria LaTeX ni en
      `prototiposPantallas/`.
- [ ] Entregables 1–14 completos o marcados `PENDIENTE` con el motivo.
