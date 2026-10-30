# Probar las APIs en local

Cada servicio publica Swagger UI en `http://localhost:<puerto>/swagger-ui.html`. El botón **Authorize** inicia
sesión en Keycloak (contraseña + código por correo) con el cliente de desarrollo `verivoto-dev-swagger`
(Authorization Code + PKCE, D-26) y envía el token en cada petición.

## 1. Preparar Keycloak (una sola vez)

Si el realm `verivoto` ya estaba importado antes de existir el cliente de desarrollo:

1. `http://localhost:8080` → usuario `local-admin` → realm `verivoto`.
2. *Realm settings* → *Action* → *Partial import* → fichero `infra/keycloak/dev/verivoto-dev-swagger-client.json`
   → *If a resource exists: Skip* → *Import*.

En una instalación nueva no hace falta: el cliente ya está en `verivoto-realm.json`.

## 2. Arrancar identity-service (PowerShell, desde la raíz del repo)

```powershell
. .\scripts\Import-DotEnv.ps1                     # carga infra/.env en esta sesión
$env:IDENTITY_DB_USERNAME = "identity"
cd backend
.\mvnw.cmd -pl services/identity-service spring-boot:run
```

Usa `IDENTITY_DB_PASSWORD` e `IDENTITY_SERVICE_CLIENT_SECRET` de `infra/.env`. Al arrancar, Flyway aplica V1–V3
en `identity_db`. Health: `http://localhost:8081/actuator/health`.

En IntelliJ, la configuración de ejecución equivalente necesita esas tres variables de entorno.

## 3. Recorrido de prueba (RF-01, RF-02)

1. `http://localhost:8081/swagger-ui.html` → **Authorize** → *client_id* ya relleno → marcar `openid` →
   **Authorize** → login de `superadmin` → código desde Mailpit (`http://localhost:8025`).
2. `POST /api/v1/organizations`:
   ```json
   { "id": "UAL", "name": "Universidad de Almería",
     "initialAdministrator": { "fullName": "Ana Pérez", "email": "ana@ual.test" } }
   ```
   Esperado: **201**. En Mailpit llega a `ana@ual.test` el correo para fijar la contraseña.
3. Repetir el mismo POST → **409** «Ya existe una organización con ese identificador.»
4. `GET /api/v1/organizations` → la lista contiene `UAL`.
5. `GET /api/v1/organizations/UAL/users` → `Ana Pérez` con rol `ADMIN`.
6. `PUT /api/v1/organizations/UAL/users/{id de Ana}/role` con `{ "role": "AUDITOR" }` → **409** «La organización
   debe tener al menos un administrador.»
7. `POST /api/v1/organizations/UAL/users` con `{ "fullName": "Luis Gil", "email": "luis@ual.test", "role": "CENSUS_MANAGER" }`
   → **201**.
8. Aislamiento (RNF-15): fijar la contraseña de Ana desde el correo, **Logout** en Swagger, **Authorize** como
   `ana@ual.test` y pedir `GET /api/v1/organizations` → **403**; `GET /api/v1/organizations/OTRA` → **404**.

En la consola de Keycloak, los usuarios creados aparecen con el atributo `organization_id = UAL` y su rol.

## 4. Censo (RF-03) en election-service

El censo solo lo modifica un **Gestor de censo** de la organización (el Administrador puede consultarlo). El
superadministrador no pertenece a ninguna organización, así que no ve censos.

1. Fijar la contraseña de `luis@ual.test` (creado en el paso 3.7 con rol `CENSUS_MANAGER`) desde su correo en Mailpit.
2. Arrancar election-service:
   ```powershell
   . .\scripts\Import-DotEnv.ps1
   $env:ELECTION_DB_USERNAME = "election"
   cd backend
   .\mvnw.cmd -pl services/election-service spring-boot:run
   ```
3. `http://localhost:8082/swagger-ui.html` → **Authorize** como `luis@ual.test`.
4. `POST /api/v1/censuses` con `{ "name": "Claustro 2026" }` → **201**.
5. `POST /api/v1/censuses/{id}/imports` con el fichero `scripts/samples/voters-sample.csv` (sin cambiar
   `dryRun`) → resumen: `toCreate = 4`, 1 duplicado (línea 6, `EMAIL`) y 3 líneas inválidas (7, 8 y 9).
   No se guarda nada (`applied = false`).
6. Repetir con `dryRun=false` → `applied = false` (hay líneas inválidas).
7. Repetir con `dryRun=false&discardInvalidLines=true` → `applied = true`; `GET .../voters` devuelve 4 personas.
8. `POST .../voters` con el correo de una persona ya existente (en mayúsculas) → **409**.
9. Como `ana@ual.test` (ADMIN): `GET /api/v1/censuses` → **200**; `POST /api/v1/censuses` → **403**.

## 5. Procesos y papeletas (RF-06) en election-service

Solo el **Administrador** crea y edita procesos; el **Auditor** los consulta. Con election-service arrancado
(apartado 4) y autorizado en Swagger como `ana@ual.test` (ADMIN de `ual`):

1. `POST /api/v1/processes` → **201** en `DRAFT` (usa el `id` del censo del apartado 4):
   ```json
   {
     "censusId": 1,
     "title": { "es": "Elección de representantes 2026", "en": "Representatives election 2026" },
     "description": { "es": "Claustro de estudiantes" },
     "opensAt": "2026-12-01T09:00:00+01:00",
     "closesAt": "2026-12-03T21:00:00+01:00",
     "defaultLanguage": "es",
     "resultsVisibility": "CENSUS_ONLY",
     "ballot": {
       "voteType": "SINGLE_CHOICE",
       "question": { "es": "¿Qué candidatura eliges?" },
       "blankVoteEnabled": true,
       "options": [
         { "position": 1, "text": { "es": "Candidatura A" } },
         { "position": 2, "text": { "es": "Candidatura B" } }
       ]
     }
   }
   ```
2. Repetir con `"voteType": "YES_NO_ABSTAIN"` y sin `options` → las opciones «Sí», «No» y «Abstención» (es/en) las
   genera el sistema.
3. Errores esperados (**400** con `errors[].field`): una sola opción (`ballot.options`), cierre antes de la apertura
   (`closesAt`), título sin el idioma por defecto (`title`), idioma `fr` (`title`).
4. `PUT /api/v1/processes/{id}` con otros datos → **200**; los identificadores de las opciones cambian (normal en un
   borrador).
5. `GET /api/v1/processes` y `GET /api/v1/processes?status=DRAFT` → el proceso; `GET .../status-history` → `[]`
   (las transiciones llegan con P4.2).
6. Como `luis@ual.test` (CENSUS_MANAGER): `GET /api/v1/processes` → **403**. Con un `censusId` de otra organización
   → **404**.
7. El censo sigue editable: solo se congela cuando el proceso pase a `SCHEDULED` (P4.2).

## 6. Ciclo de vida (RF-07) en election-service

Programar valida el proceso, congela el censo y deja en el *outbox* (`process_sync`) la instantánea para
voter-access y la papeleta para ballot-box. Mientras esos servicios no existan (P4.3, P4.4), los mensajes se
reintentan cada vez más espaciados (máx. 5 min) y aparece un aviso en el log; para silenciarlo:
`$env:ELECTION_SYNC_ENABLED = "false"`.

1. Crear un proceso (apartado 5) con todos los textos en `es` y `en`, apertura dentro de 2 minutos y cierre 2
   minutos después.
2. Crear otro sin inglés y con apertura pasada → `POST /api/v1/processes/{id}/schedule` → **422** con `errors`
   (cada campo que falta: `opensAt`, `censusId` si el censo está vacío, `title`, `ballot.options[n].text`…).
3. `POST /api/v1/processes/{id}/schedule` del completo → **200** `SCHEDULED`. Repetir → **409**
   (`currentStatus: SCHEDULED`). El censo aparece `frozen: true` y no admite altas (**409**).
4. Esperar: en ≤ 30 s tras la apertura pasa a `OPEN` y tras el cierre a `CLOSED`.
   `GET .../status-history` → `DRAFT→SCHEDULED` (tu usuario), `SCHEDULED→OPEN` y `OPEN→CLOSED` (`SYSTEM`).
5. `POST .../archive` → **409** «Solo se puede archivar un proceso escrutado» (el escrutinio llega en P5).
6. En la BD: `SELECT target, message_type, state, attempts FROM process_sync;` → 2 mensajes por transición.

## 7. Acceso del votante (RF-08) en voter-access-service

El votante no usa Keycloak: su enlace personal y el código de 6 cifras lo identifican. Los correos llegan a
Mailpit (`http://localhost:8025`).

1. Arrancar voter-access (con infraestructura, election y gateway en marcha):
   ```powershell
   . .\scripts\Import-DotEnv.ps1
   $env:VOTER_ACCESS_DB_USERNAME = "voter_access"
   cd backend
   .\mvnw.cmd -pl services/voter-access-service spring-boot:run
   ```
   election necesita `ELECTION_SERVICE_CLIENT_SECRET` (en `.env`) para enviarle el proceso.
2. Programar un proceso (apartado 6) con apertura dentro de 2 minutos. En ≤ 10 s llegan a Mailpit las
   invitaciones, en el idioma de cada persona, con el enlace `http://localhost:4200/votar/{enlace}`.
3. Con el enlace (64 caracteres), en `http://localhost:8000` (gateway) o Swagger `http://localhost:8083/swagger-ui.html`:
   - `GET /api/v1/voter-access/links/{enlace}` → **200** `SCHEDULED`; `POST …/otp` → **422** (aún no abierta).
   - Tras la apertura: `POST …/otp` → **202** (`a•••@ual.es`) y el código llega a Mailpit; repetir enseguida →
     **429**.
   - `POST …/otp/verify` con un código erróneo → **400** `remainingAttempts`; con el correcto → **200** con
     `votingToken` (válido 15 min); repetir con el mismo código → **410**.
   - 5 códigos erróneos seguidos → **423** durante 15 minutos.
4. Los endpoints `/internal/v1/**` exigen un token de servicio (rol `SERVICE`) y no pasan por el gateway. Se
   prueban con un token *client credentials* de `election-service` (PowerShell):
   ```powershell
   $t = (Invoke-RestMethod -Method Post http://localhost:8080/realms/verivoto/protocol/openid-connect/token `
     -Body @{ grant_type = "client_credentials"; client_id = "election-service"; client_secret = $env:ELECTION_SERVICE_CLIENT_SECRET }).access_token
   Invoke-RestMethod -Method Post http://localhost:8083/internal/v1/voting-tokens/consume `
     -Headers @{ Authorization = "Bearer $t" } -ContentType application/json `
     -Body (@{ votingToken = "<token>"; processId = 1 } | ConvertTo-Json)
   ```
   La primera vez → 204; la segunda → **409**. Tras consumirlo, pedir otro código y verificarlo →
   `hasVoted: true` con la fecha, sin token.

## 8. Urna (RF-09, RF-10) en ballot-box-service

Con election, voter-access y ballot-box en marcha (`BALLOT_BOX_SERVICE_CLIENT_SECRET` en `.env`; ballot-box usa
`pg-ballot`, puerto 5433):

```powershell
. .\scripts\Import-DotEnv.ps1
$env:BALLOT_BOX_DB_USERNAME = "ballot_box"
cd backend
.\mvnw.cmd -pl services/ballot-box-service spring-boot:run
```

1. Programar un proceso (apartado 6). Tras la apertura, obtener un `votingToken` (apartado 7).
   `GET /api/v1/ballot-box/processes/{id}/ballot` con la cabecera `X-Voting-Token: {votingToken}` → la papeleta con los
   mismos `id` de opción que en election. Sin cabecera, o con un token inventado o de otro proceso → **404**; con el
   token ya usado → **409**; caducado → **410** (D-43).
2. Votar:
   `POST /api/v1/ballot-box/processes/{id}/votes` con `{ "votingToken": "…", "selection": { "optionId": 5 } }`
   (o `{ "blank": true }`) → **201** con `receiptCode` (64 hex). Repetir con el mismo token → **409**.
3. `GET …/receipts/{receiptCode}` → `{ "present": true }`; con otro código → `false`.
4. Con una opción que no es de la papeleta → **400** (el token sigue sirviendo). Con voter-access parado → **503**;
   al volver a arrancarlo, el mismo token funciona.
5. Tras el cierre → **422**. En `ballot_box_db`: `audit_events` tiene `OPENED`, un `VOTE_RECEIVED` por voto (sin
   hora) y `CLOSED`; `ballot_snapshots.final_hash` es el `event_hash` de `CLOSED`. `votes` y `receipts` no tienen
   ninguna columna de tiempo ni de identidad.

### Escrutinio y resultados (RF-13, RF-15, P5.1)

6. Al cerrarse el proceso, en unos 10 s el log de ballot-box dice `Tally: 1 process(es) tallied, 1 acknowledged` y el
   proceso aparece `TALLIED` en election (historial «Cerrado → Escrutado · Sistema»). Antes, `…/results` → **409**.
7. Como `ana@ual.test` (ADMIN) o la persona auditora: `GET /api/v1/ballot-box/processes/{id}/results` → votos y
   porcentajes por opción, en blanco, participación, `finalHash` y `resultHash`. Gestor de censo → **403**.
8. `GET …/results/export` (ADMIN) → CSV con `;` y coma decimal; `?lang=en` en inglés.
9. `GET …/results/counted-votes` → votos con identificador aleatorio y opción. Recalcular: SHA-256 de las líneas
   `voteId:optionId` (o `voteId:blank`) unidas por `\n` = `votesHash`; SHA-256 de
   `result|id|finalHash|censo|votos|en blanco|opción:votos|…|votesHash` = `resultHash`.
10. `GET …/fingerprints` sin token → huellas. Con `PUBLIC_LINK`, el proceso trae `resultsPublicId` y
    `GET /api/v1/ballot-box/public/results/{resultsPublicId}` devuelve el resultado sin token.

### Registro de la urna y participación (RF-12, RF-14, P5.2)

11. Como la persona auditora: `GET /api/v1/ballot-box/processes/{id}/audit-events?page=0&size=50` → eventos en orden;
    los de voto con `occurredAt: null`. ADMIN → **403**.
12. `POST …/audit-events/verification` → `{ "intact": true, "eventsChecked": n, "problems": [] }`; después
    `GET …/audit-events/verification` devuelve esa misma verificación. Antes de verificar nunca → **404**.
13. (Solo en local) cambiar a mano el `payload_hash` de un evento en `ballot_box_db.audit_events` y verificar →
    `intact: false` con `HASH_MISMATCH` en esa secuencia y una incidencia `CHAIN_BROKEN`; si el proceso se cierra así,
    el escrutinio no se publica hasta restaurar el valor.
14. Como `ana@ual.test` (ADMIN): `GET /api/v1/voter-access/processes/{id}/participation` → censo, `votesCast`,
    `pending`, `participation` y `byDay` (solo días). Auditor o gestor de censo → **403**.

## 9. Flujo completo del votante en la PWA (P4.5)

Con infraestructura, gateway, election, voter-access, ballot-box y el frontend (`cd frontend; npm start`):

1. Programar un proceso (apartado 6) con apertura dentro de 2 minutos.
2. Abrir en el móvil o a 360 px el enlace del correo de Mailpit (`http://localhost:4200/votar/…`): antes de la
   apertura aparece «La votación todavía no está abierta» (C3); después, la bienvenida (V1).
3. «Empezar» envía el código (B1). Un código erróneo muestra los intentos que quedan (V2); con el correcto aparece la
   papeleta (B2). Cambiar a EN mantiene la opción elegida.
4. «Revisar mi voto» (B3) → «Confirmar y votar» → recibo (B4) → «Comprobar mi recibo» (B5): «Tu voto está en la urna».
5. Volver a abrir el enlace y repetir el código → «Ya has votado» con la fecha, sin hora (V3).
6. Con ballot-box parado, «Confirmar y votar» → «Tu voto NO se ha registrado» (V7); al arrancarlo, «Reintentar»
   registra un solo voto.

## 10. Procesos desde la consola (P4.6, A3/A4)

Con la misma infraestructura del apartado 9 y un censo con al menos una persona (apartado 4):

1. Entrar en `http://localhost:4200/admin` como administrador de la organización → «Procesos» → «Crear proceso».
2. Paso 1: título en ES y EN. Dejar el título vacío y pulsar «Siguiente» muestra el error debajo del campo.
3. Paso 2: apertura dentro de 2–3 minutos y cierre posterior; con el cierre antes de la apertura aparece el error.
4. Paso 3: elegir el censo. Paso 4: tipo «Opción única», pregunta y al menos 2 opciones; «Crear proceso» → F3.
5. En A4 (borrador): la comprobación previa marca lo que falta (p. ej. traducciones EN). «Editar proceso» abre el
   mismo formulario con los datos; al completar el EN, todo queda en verde.
6. «Programar» → confirmar: estado «Programado», historial «Borrador → Programado · Persona administradora» y
   correos de invitación en Mailpit. «Editar» ya no aparece.
7. Esperar la apertura (scheduler cada 30 s) y recargar A4: «Abierto», historial «Programado → Abierto · Sistema».
8. Con el proceso abierto, A4 no muestra «Editar» ni la validación previa, sino «Datos del proceso» y «En este estado no hay
   acciones manuales».
9. Entrar como auditor: ve la lista y A4 sin botones («Tu rol permite consultar el proceso, no cambiarlo») ni censo.

## 11. Censos desde la consola (A2, G1)

1. Entrar en `http://localhost:4200/admin` como gestor de censo → «Censos». Crear «Socios 2026» (sin nombre aparece el error).
2. En el censo, «Seleccionar fichero» con `scripts/samples/voters-sample.csv` → resumen: 4 altas, 1 duplicado y 3 líneas con
   error (7, 8 y 9). Todavía no se ha guardado nada (0 personas). Solo se ofrece «Descartar líneas con error e importar 4».
3. Tras importar: 4 personas en la tabla y en la cabecera. Volver a elegir el mismo fichero → 0 altas y 5 duplicados, sin
   botón de aplicar.
4. Un fichero `.txt` o de más de 5 MB se rechaza sin subirlo. Con un CSV grande se ve «Validando …» y «Cancelar».
5. «Alta manual»: sin nombre ni correo, errores en cada campo; teléfono `+34 600` → «solo admite cifras». Un correo ya
   existente (en mayúsculas) → error del servicio (409).
6. «Editar» y «Quitar» (con confirmación) actualizan la tabla y el número de personas. La búsqueda filtra por nombre, correo
   o identificador.
7. Como administrador: lista y detalle en solo lectura, sin crear ni importar. Con el censo usado por un proceso programado:
   «Censo congelado» y sin acciones. El auditor no ve «Censos».

## 12. Resultados, participación y auditoría desde la consola (P5.3)

Con todo en marcha y un proceso `PUBLIC_LINK` con votos (sección 9):

1. Como `ana@ual.test` (ADMIN) → «Participación»: con el proceso abierto, «Se actualiza automáticamente», censo, votos,
   pendientes, porcentaje y una barra por día. Nunca aparece qué se ha votado.
2. Al cerrarse, «Resultados»: unos segundos «Escrutinio en curso…» y después el resultado por opción, «Resultado
   verificado» (el navegador lo recalcula), «Exportar resultados (CSV)» y el enlace público para copiar.
3. Abrir ese enlace en una ventana privada (`/resultados/…`): resultado, participación y «Resultado verificado» sin
   iniciar sesión.
4. Como la persona auditora → «Auditoría»: «Registro íntegro», eventos con los votos «sin hora» y «Verificar integridad
   ahora». No ve «Participación» ni el botón de CSV.
5. Como ADMIN, en «Resultados» de un proceso escrutado → «Generar acta (PDF)»: se descarga `acta-proceso-{id}.pdf` y la
   tarjeta «Acta del proceso» muestra su huella. Una segunda descarga es el mismo fichero (misma huella). La persona
   auditora no ve el botón.

### Acta de resultados (RF-16, P5.4)

`GET /api/v1/ballot-box/processes/{id}/results/minutes?lang=es` (o `en`) con token de ADMIN:

- **200**: `application/pdf`, `Content-Disposition: attachment; filename=acta-proceso-{id}.pdf` (`minutes-process-{id}.pdf`
  en inglés) y `X-Content-SHA256` = SHA-256 del cuerpo (`sha256sum` del fichero). Repetir la petición → mismos bytes.
- **409** antes del escrutinio; **403** con AUDITOR o gestor de censo; **401** sin token; **404** con un proceso de otra
  organización; **503** si no se ha podido generar.
- El PDF contiene título, pregunta, fechas, censo, votos, participación, resultado por opción y las huellas `finalHash`
  y `resultHash` de `…/results`; ningún nombre, correo ni recibo.

### Resultado para las personas del censo (RF-15, P5.5)

Con un proceso escrutado (`TALLIED`, también `ARCHIVED`) y el enlace personal de su invitación:

1. Abrir `/votar/{enlace}` → «Resultado de la votación» con «Ver el resultado». Pedir el código: el correo dice «Tu código
   para ver el resultado de …». Al escribirlo se ve el resultado, «Resultado verificado» y, si votó, «Tu voto quedó
   registrado el …» (solo el día).
2. Por API: `POST /api/v1/voter-access/links/{enlace}/otp` y `…/otp/verify` → `resultsToken` (sin `votingToken`).
   `GET /api/v1/ballot-box/voter/processes/{id}/results` con la cabecera `X-Results-Token: {resultsToken}` → el mismo
   resultado que ve ADMIN; `…/results/counted-votes` → los votos para recalcular la huella.
3. Sin cabecera, con un token inventado o de otro proceso → **404**; pasados 30 min → **410** «Tu acceso a los resultados
   ha caducado»; con voter-access parado → **503**. Entre el cierre y el escrutinio no se envía código (**422**).
4. El token no sirve para votar y en `voter_access_db.results_tokens` solo está su hash, sin la persona.

### Rutas internas por cliente y cruce con la participación (P5.7, D-43)

1. Cada ruta `/internal/**` solo acepta el cliente que la usa: con un token de `identity-service` (o de cualquier otro
   cliente), `PUT /internal/v1/processes/{id}/status` de la urna → **403**; con el de `election-service` → 204. Sin
   token → **401**.
2. Tras un escrutinio normal, `ballot_box_db.incidents` no tiene filas del proceso. Si en `voter_access_db.participation`
   se marca como no votante a alguien que sí votó antes del escrutinio, la urna no publica el resultado
   (`TALLY_FAILED` y `PARTICIPATION_MISMATCH`, election sigue en `CLOSED`); al corregirlo, el siguiente ciclo escruta.

### Trazabilidad de acciones administrativas (RF-19, P6.1, D-44)

1. Hacer algunas acciones (crear un censo, importar, crear y programar un proceso; como auditora, verificar la urna).
2. Como ADMIN o AUDITOR: «Trazabilidad» o `GET /api/v1/admin-actions` → acciones de la organización, la más reciente
   primero, con quién, qué, sobre qué y cuándo. Filtros `from`, `to` (ISO 8601, `to` excluido), `actorId`, `action`,
   `processId`; `GET /api/v1/admin-actions/actors` para la lista de personas. Gestor de censo → **403**.
3. Las acciones de election y de la urna tardan unos segundos (se entregan desde su *outbox*). Con identity parado, la
   acción se hace igual y aparece cuando identity vuelve.
4. `UPDATE` o `DELETE` sobre `identity_db.admin_actions` → error de la base de datos (solo añadir).

### Recordatorios y correos a las personas votantes (RF-17, P6.2, D-45)

Requiere `VOTER_ACCESS_SERVICE_CLIENT_SECRET` en `infra/.env` y el cliente `voter-access-service` en Keycloak (reimportar
el realm, ver `keycloak-realm.md`); sin él todo funciona, pero sus acciones esperan en la *outbox* y no salen en la trazabilidad.

1. Con un proceso abierto y alguien que ya ha votado: «Participación» → «Enviar recordatorio ahora» (o
   `POST /api/v1/voter-access/processes/{id}/reminders` sin cuerpo) → **201** `SCHEDULED`; a los pocos segundos pasa a
   `SENT` con el número de destinatarios. En Mailpit, el recordatorio solo llega a quien no ha votado, con un enlace nuevo;
   el de la invitación da **404**.
2. `{"sendAt": "<ISO 8601>"}` lo programa. Antes de abrir o tras cerrar → **409** `NOT_OPEN`; en el pasado o tras el
   cierre → **409** `OUT_OF_PERIOD`; el sexto → **409** `TOO_MANY`; `DELETE …/reminders/{rid}` lo cancela (ya enviado o
   cancelado → **409** `ALREADY_SENT`).
3. `GET …/notifications` → recuentos por tipo y estado, invitaciones y avisos de cierre fallidos con la persona y el
   motivo, y los recordatorios. Los recordatorios fallidos solo se cuentan. `POST …/notifications/retry` → `{"retried": n}`.
4. Al cerrarse la votación, todo el censo recibe «Votación cerrada» (sin enlace) y los recordatorios pendientes se cancelan.
5. AUDITOR o gestor de censo → **403**; ADMIN de otra organización → **404**. En «Trazabilidad» aparecen
   «Recordatorio programado», «Recordatorio cancelado» y «Reintento de correos fallidos».
