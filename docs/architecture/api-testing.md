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
