# PROGRESS · VeriVoto

Última actualización: 2026-10-30 · Rama de trabajo: `feat/traceability` (P6.1 sin commitear, sobre `dev` `4dd72fc`) · Último commit en `dev`: `4dd72fc`

## Hitos

| Hito | Fechas | Objetivo | Estado |
| --- | --- | --- | --- |
| P0 | 29/10 – 02/11 | Línea base y memoria de la IA | Hecho (`da2e6db`) |
| P1 | 02/11 – 08/11 | Java 25, monorepo Maven, migración de `tenant-service`, esqueletos, infraestructura, CI de compilación | Hecho (CI en verde, `efe8afc`) |
| P2 | 05/11 – 10/11 | Modelo de datos, separación de secreto, contratos OpenAPI de la rebanada 1, ADRs | Hecho (`ce6cb37`) |
| P3 | 09/11 – 17/11 | Organizaciones y roles, configuración de Keycloak, censo, base del frontend y login | Hecho (`b7aea1c`) |
| P4 | 16/11 – 30/11 | Rebanada vertical del voto (RF-06–10, RF-20) | Hecho (todo en `dev`, `cbecfd2`) |
| P5 | 30/11 – 14/12 | Auditoría, escrutinio y resultados (RF-12–16), modelo de amenazas | Hecho (todo en `dev`, `4dd72fc`) |
| P6 | 14/12 – 31/12 | Consola y transversales (RF-05b, RF-17–19, RF-21) | En curso (P6.1 trazabilidad, RF-19, en `feat/traceability`; siguen RF-17, RF-18, RF-21) |
| P7 | 01/01 – 06/01/2027 | Pruebas y medición (H4) | Bloqueada (`ACTIVAR TESTS`) |
| P8 | 01/12 – 27/01/2027 | Memoria del TFM | Pendiente |
| P9 | Tras la entrega | Post-MVP: RF-11, RF-22, RF-23 | Fuera de alcance |

## Hecho

- Antes de P0 (commits hasta `09f21e1`): estructura de carpetas; `tenant-service` con el caso de uso
  «crear organización» (dominio, puertos, servicio, JPA, Flyway V1, REST, resource server JWT con
  `ROLE_SUPERADMIN`); entorno local de Keycloak 26.8.1 + Email OTP v1.8.1 + Mailpit v1.31.5 con su
  documentación; prototipos en `prototiposPantallas/`. No se ha compilado ni arrancado nada.
- P0.1: `README.md` y `.gitignore` sin el envoltorio de PowerShell; el script del antiguo `.gitkeep`
  movido a `scripts/create-folders.ps1`.
- P0.2: `CLAUDE.md`.
- P0.3: `docs/ai/` (CONTEXT, PROGRESS, DECISIONS, TRACEABILITY, IMPROVEMENTS, PROTOTYPES).
- P0.4: skills en `.claude/skills/`.
- P1.2: `backend/pom.xml` padre (`verivoto-backend`): Spring Boot 4.1.2, `java.version=25`, BOM Spring
  Cloud 2025.1.3, módulo `services/identity-service`. Maven Wrapper 3.3.4 (*only-script*, Maven 3.9.16)
  en `backend/` y `backend/.gitattributes` para los finales de línea de `mvnw` y `mvnw.cmd`.
- P1.3: `tenant-service` → `identity-service`. Paquete `com.saasvoting.tenant` → `com.saasvoting.identity`;
  puertos en `application/port`, adaptadores en `infrastructure/adapter`, configuración en
  `infrastructure/config`. Renombradas `IdentityServiceApplication` e `IdentityServiceConfiguration`.
  Sin cambios de lógica. `domain` y `application` compilados con `javac` de forma aislada; el módulo
  completo no se ha compilado con Maven (sin acceso a Maven Central desde el entorno de la IA).
  Cambios de configuración: `application.properties` → `application.yml`; variables `TENANT_*` →
  `IDENTITY_*`; BD por defecto `tenant_service` → `identity_db` (D-08); puerto 8081 (D-09).
  Compilado por el autor con `mvnw` y Java 25 (commit `5ed5949`).
- P1.4: esqueletos de `api-gateway` (Gateway Server WebFlux, ruta `/api/v1/tenants/**` → identity),
  `election-service`, `voter-access-service` y `ballot-box-service` (main, paquetes hexagonales con
  `package-info.java`, `application.yml`, `/actuator/health`, Flyway sin migraciones, springdoc 3.1.1).
  `identity-service` recibe Actuator, springdoc y acceso público a `/actuator/health` y a la
  documentación OpenAPI. Plantilla documentada en `docs/architecture/hexagonal-template.md`.
  Compilado por el autor: los 5 módulos con `BUILD SUCCESS` (commit `d6f6bc6`).
- P1.5: `infra/compose.yaml` (proyecto `verivoto`) con `pg-main` y `pg-ballot` (`postgres:18.6`),
  Keycloak 26.8.1 con BD en `keycloak_db` y health en el puerto 9000, Mailpit. Scripts de inicialización
  con un rol por base de datos (probados con PostgreSQL 16 en el entorno de la IA), `infra/.env.example`,
  `.gitattributes` para los `.sh` y `docs/architecture/local-infrastructure.md`. Commit `14a2b33`
  (borra `infra/keycloak/compose.yaml`). El autor corrigió el Dockerfile de Keycloak: `USER root` y
  permisos 644 del jar del OTP, que `ADD` desde URL deja ilegible para el usuario `keycloak`.
  Incidencia: `14a2b33` incluía contraseñas locales en `infra/.env.example`; se restaura la plantilla
  vacía y se corrige el commit antes de publicarlo (no llegó a GitHub).
- P1.6: `.github/workflows/build.yml` con dos jobs: compilación del backend con Temurin 25
  (`actions/checkout@v7`, `actions/setup-java@v6`, caché Maven, `mvnw -DskipTests verify`) y
  validación de `infra/compose.yaml` con `docker compose config` y valores de prueba. Validado en el
  entorno de la IA: YAML del workflow y `docker compose config` (Compose v5.6.1). Primera ejecución en
  GitHub en verde (confirmado por el autor). El commit con contraseñas se rehízo sin ellas (`9ebbf84`).
- P2: `docs/architecture/data-model.md` (modelo por servicio con diagramas ER, separación de secreto,
  flujo del voto con token de un solo uso, riesgos residuales); contratos OpenAPI 3.1 de identity,
  election, voter-access y ballot-box en `src/main/resources/openapi/` (validados con
  `openapi-spec-validator`); ADR-0001 a ADR-0009 en `docs/adr/`; decisiones D-16 a D-23.
- P3.2: realm `verivoto` en `infra/keycloak/realm/verivoto-realm.json` (roles, flujo contraseña + Email OTP,
  bloqueo, política de contraseñas, clientes `verivoto-admin`, `identity-service`, `election-service`,
  `ballot-box-service`, claims `roles` y `organization_id`, perfil de usuario, superadministrador inicial).
  Importación en el compose con secretos por variables (D-24). Validado: JSON, referencias internas y
  `docker compose config`; los nombres de autenticadores, mappers y campos se comprobaron en el código fuente
  de Keycloak 26.8.1 y de Email OTP v1.8.1. Importado por el autor (`947ad90`); el primer login de
  `superadmin` falló por credenciales (4 intentos), probablemente al teclear la contraseña inicial.
- P3.1: `identity-service` con organizaciones (RF-01) y usuarios de gestión (RF-02) según el contrato:
  `Tenant` → `Organization` (ficheros renombrados con `mv`), `Caller` con las reglas de acceso por
  organización (RNF-15), puertos `IdentityProvider`, `OrganizationRepository` y `ManagementUserRepository`,
  adaptador `KeycloakAdminClient` (Admin API con *client credentials*), controladores, `ApiExceptionHandler`
  (IMP-01), migraciones V2 (renombrado) y V3 (`management_users`), ruta del gateway `/api/v1/organizations`.
  Realm: `firstName` y `lastName` dejan de ser obligatorios (requiere reimportar). Verificado en el entorno de
  la IA: `domain` y `application` compilan con `javac`; V1–V3 se aplican sobre PostgreSQL 16. Falta compilar
  con Maven y probar contra Keycloak. Compilado por el autor e integrado (`f1a9683`).
- Pruebas manuales de API (D-26): cliente `verivoto-dev-swagger` (en el realm y como importación parcial en
  `infra/keycloak/dev/`), Swagger UI de identity con OAuth2 + PKCE, `scripts/Import-DotEnv.ps1` y la guía
  `docs/architecture/api-testing.md`. Integrado (`3e03b8b`).
- P3.4: base del frontend en `frontend/` (D-28): Angular 22 + Ionic 9 PWA, i18n es/en con selector en cabecera,
  login de gestión con Keycloak (PKCE) y menú filtrado por rol (`roleGuard` → `/admin/sin-permiso`),
  listados de organizaciones (SUPERADMIN) y censos (ADMIN, CENSUS_MANAGER), página de votante `/votar/:linkToken`
  (provisional hasta P4.5) y 404. `ng build` limpio (inicial 802 kB / 183 kB transferidos); smoke test a 360 px con
  Playwright (idioma, 404, redirección de `/admin` al login de Keycloak, sin errores de consola). CI: job `frontend`.
- P3.3: censo en `election-service` (RF-03): dominio (`Census`, `Voter`, `VoterData`, `ImportSummary`, `Caller`),
  `CensusService` (alta y edición manual, duplicados, importación CSV con `dryRun` y descarte de líneas
  inválidas), JPA y Flyway V1 (`censuses`, `voters` con índices únicos por censo), REST según contrato con
  `VoterCsvReader`, seguridad JWT, Swagger con Keycloak, ruta del gateway (`/api/v1/censuses`,
  `/api/v1/processes`), CSV de ejemplo en `scripts/samples/`. La congelación usa una política provisional que
  nunca congela (`NoProcessesCensusFreezePolicy`) hasta que existan procesos (P4.1). Verificado en el entorno
  de la IA: `domain`, `application` y el lector CSV compilan con `javac`; comprobación manual del CSV de ejemplo
  (resumen, rechazo, descarte, aislamiento por organización, rol y censo congelado); V1 en PostgreSQL 16.

## En curso

- P6.1: trazabilidad de acciones administrativas (RF-19, D-44): registro central en identity (`admin_actions`, solo añadir con disparadores), 16 tipos de acción registrados en identity, election y la urna (estas dos con *outbox* propia y entrega idempotente a `POST /internal/v1/admin-actions`), consulta con filtros y pantalla «Trazabilidad» para ADMIN y AUDITOR; gateway con `/api/v1/admin-actions`; planificador de election y urna con 4 hilos. Prueba de extremo a extremo de la IA (42/42, incluida la entrega con identity caído) y en navegador (20/20); regresión completa en verde (votación, escrutinio, auditoría, acta, resultado del votante, endurecimiento y todas las pantallas).
- P5.7: endurecimiento del modelo de amenazas (D-43): rutas internas solo para el cliente que las usa (IMP-26), papeleta solo con token de voto válido comprobado sin consumirlo (IMP-31) y cruce del total de la urna con el número de personas que votaron al escrutar, con incidencia `PARTICIPATION_MISMATCH` (IMP-28, Flyway V5). `threat-model.md` actualizado: T-04 y T-17 pasan a Bajo, T-08 a Bajo-Medio. Prueba de extremo a extremo de la IA (39/39) y regresión completa: votación 29/29, escrutinio 54/54, auditoría 37/37, acta 21/21, resultado del votante 38/38, resultados y V8 39/39, votante con resultado 20/20, consola, votante, procesos y censos.
- P5.6: modelo de amenazas (D-42) en `docs/architecture/threat-model.md`: alcance y supuestos, activos, 8 adversarios, diagrama de flujo con 5 fronteras de confianza, 27 amenazas STRIDE con defensa, evidencia y riesgo residual, sondas contra los servicios reales y acciones priorizadas. Hallazgos medios convertidos en IMP-26–IMP-33 (rutas internas abiertas a cualquier servicio, papeleta pública por identificador correlativo, urna sin cruce con la participación, enlace y código por el mismo correo, censos pequeños).
- P5.5: resultado para las personas del censo (RF-15 con `CENSUS_ONLY`, D-41). voter-access: con el proceso `TALLIED`/`ARCHIVED` el enlace personal admite código (correo «para ver el resultado») y su verificación da un token de resultados de 30 min (solo hash en `results_tokens`, Flyway V2, sin la persona) y si votó con el día; interno `POST /internal/v1/results-tokens/check`. ballot-box: `GET /api/v1/ballot-box/voter/processes/{id}/results` y `…/counted-votes` con `X-Results-Token`, comprobado en cada petición (404/410/503). PWA: «Resultado de la votación» → código → resumen compartido con V8 (`app-result-summary`) con verificación en el navegador; C2 avisa de que el resultado saldrá en el mismo enlace. Prueba de extremo a extremo de la IA (38/38) y en navegador a 360 px (20/20); regresión: votación 28/28, escrutinio 54/54, auditoría 37/37, resultados y V8 39/39, consola, votante, procesos y censos.
- P5.4: acta de resultados en PDF (RF-16, D-40): escritor PDF propio sin dependencias detrás de `MinutesRenderer`, acta por idioma guardada una vez con su SHA-256 (`minutes`, Flyway V4) y servida con `X-Content-SHA256`, título del proceso en la papeleta interna (`ballot_titles`), solo ADMIN (409 antes del escrutinio, 503 si falla); F1 con «Generar acta (PDF)» y la huella del fichero. Prueba de extremo a extremo de la IA (21/21: PDF válido con `qpdf`, texto con tildes y ñ, inglés, acta de 3 páginas con 30 opciones, mismos bytes en la segunda descarga, permisos, gateway) y en navegador (44/44); regresión: escrutinio 54/54, auditoría 37/37, procesos 28/28.
- P5.3: pantallas de resultados, participación y auditoría (RF-12–RF-15, D-39): A5 (participación con refresco y por día), F4/F1 (escrutinio en curso y resultados, CSV, enlace público, verificación en el navegador), H1 (registro de la urna y verificación), V8 (resultados públicos sin sesión), selector de proceso, menú y enlaces desde A4, textos es/en. Prueba en navegador de la IA (39/39, 1280 y 360 px) con todos los servicios reales y una manipulación real del registro; regresión: consola 14/14, votante 25/25, procesos 28/28, censos 37/37. `ng build` limpio (inicial ≈ 966 kB / 186 kB transferidos).
- P5.2: registro de la urna y participación (RF-12, RF-14, D-38). ballot-box: `ChainVerifier` (hash, enlaces, secuencia, cabeza, papeleta de `OPENED`, votos/recibos/eventos, `CLOSED`, huella final), `AuditService` y `AuditController` (eventos por páginas y verificación para AUDITOR), Flyway V3 `chain_checks`, incidencia `CHAIN_BROKEN` única y escrutinio que verifica la cadena antes de publicar. voter-access: `ParticipationService` y `ParticipationController` (ADMIN; censo, votos, pendientes, % y por día). Prueba de extremo a extremo de la IA (37/37): seis manipulaciones reales de la base de datos detectadas y deshechas, escrutinio bloqueado y reanudado, permisos y organizaciones, participación antes, durante y después; regresión P5.1 54/54 y urna 28/28.
- P5.1: escrutinio y resultados (RF-13, RF-15, D-37). ballot-box: Flyway V2 (`results`, `result_options`, incidencia `TALLY_FAILED`), `TallyResult` (recuento, porcentajes, huella de la lista de votos y del resultado), `TallyService` + `TallyScheduler` (comprobación con el evento `CLOSED`, incidencia única, reintento, aviso a election hasta que responde), `ResultQueryService` y `ResultsController` (huellas públicas, resultado ADMIN/AUDITOR, votos contabilizados, CSV ADMIN, enlace público), `ServiceTokenProvider` compartido y `HttpElectionClient`. election: `POST /internal/v1/processes/{id}/tallied` (SERVICE, idempotente) y `resultsPublicId` en la respuesta del proceso con `PUBLIC_LINK`. Prueba de extremo a extremo de la IA (54/54) con todos los servicios reales: recuento manipulado no publicado y reintento, election caído durante el escrutinio, proceso sin votos, porcentajes y participación, recálculo independiente de la huella, permisos y organizaciones, CSV es/en, enlace público, secreto del voto en datos y logs; regresión de la urna 28/28.
- Correcciones de A4 tras la prueba del usuario con Keycloak real (D-35): la validación previa solo aparece en borrador (antes marcaba «Calendario coherente» como incompleto con el proceso abierto); en los demás estados, «Datos del proceso»; los botones que no aplican no se muestran («Editar» seguía visible en `OPEN`); el auditor ya no consulta censos (veía «— no tiene personas» por el 403).
- A2/G1: censos (RF-03, D-36): `CensusApiService`, lista con alta de censo, detalle con importación CSV en dos pasos (validación cancelable → resumen → aplicar), alta, edición y baja manual, búsqueda y páginas, vista móvil, solo lectura para el administrador y censo congelado. Prueba en navegador con el stack completo: A2 37/37 (incluido un CSV de 49 000 filas) y regresión de consola 14/14, votante 25/25 y A3/A4 28/28 (con apertura automática y A4 sin acciones en `OPEN`). `ng build` limpio (inicial ≈ 962 kB / 214 kB transferidos).
- P4.6: pantallas de procesos (RF-06, RF-07, RF-20; D-35): `ProcessApiService`, lista de procesos, A3/F3 (crear y editar borrador, 4 pasos, ES/EN), A4 (ciclo de vida, acciones, comprobación previa, historial, auditor en solo lectura), `StatusBadgeComponent`, menú «Procesos» y textos es/en. Prueba en navegador (Playwright, 1280 y 360 px) con gateway, election, voter-access, ballot-box reales y Keycloak simulado: 22/22 (una comprobación repetida aparte por la transición de página), incluida la apertura automática vista en A4 («Programado → Abierto · Sistema»). `ng build` limpio (inicial ≈ 934 kB / 210 kB transferidos).
- P4.1: procesos y papeletas en `election-service` (RF-06, RF-20; D-29): dominio (`VotingProcess`, `ProcessData`,
  `Ballot`, `BallotOption`, `LocalizedText`, `ProcessStatus`, `VoteType`, `ResultsVisibility`, `StatusChange`),
  `ProcessService` (listar con filtro de estado, crear en `DRAFT`, consultar, editar solo en `DRAFT`, historial),
  Flyway V2 (`processes`, `process_texts`, `ballots`, `ballot_texts`, `ballot_options`, `ballot_option_texts`,
  `process_status_changes`), JPA, REST según contrato y errores con campo y mensaje. La congelación del censo ya
  consulta los procesos `SCHEDULED`/`OPEN` (`ProcessCensusFreezePolicy` sustituye a la provisional). Verificado en
  el entorno de la IA: `domain` y `application` compilan con `javac` y una comprobación manual de las reglas; V1 + V2
  en PostgreSQL 16 (restricción diferida y `CHECK` de fechas). Prueba de extremo a extremo (29/29) con el servicio
  real contra PostgreSQL y tokens firmados de prueba.
- Corrección: con Spring Boot 4 la autoconfiguración de Flyway está en `spring-boot-starter-flyway`; election,
  voter-access y ballot-box solo tenían `flyway-core`, así que sus migraciones nunca se ejecutaban (identity ya lo
  tenía). Corregido en los cuatro `pom.xml`.
- Prueba completa de la IA (2026-10-30) con gateway, identity, election, voter-access, ballot-box, PWA y un
  Keycloak simulado: consola de gestión 14/14 (superadministrador, administrador, gestor de censo; 1280 y 360 px),
  flujo del votante por el gateway 25/25 y urna de extremo a extremo 28/28. Corregido (D-34): contenido de las
  páginas de gestión encogido y menú de censos visible para el superadministrador sin organización.
- P4.5: pantallas del votante (RF-08–RF-10, D-33): `VoterFlowPage` (`/votar/:linkToken`) con V1, B1/V2 (`ion-input-otp`,
  intentos, cuenta atrás de reenvío), B2/C1 (opciones de 64 px, voto en blanco), B3, B4 (copiar recibo), estados
  C2–C4 y V3–V7, barra de estado e indicador de 4 pasos; `ReceiptCheckPage` (B5, `/recibo/:processId`); textos
  es/en. Prueba en navegador a 360 px (Playwright) contra election, voter-access y ballot-box reales (25/25):
  C3, V1, V2, C1, cambio de idioma sin perder la selección, B3, B4, B5, V3 con fecha sin hora, enlace no válido,
  V7 con ballot-box caído y reintento, nada sensible en el almacenamiento ni en la URL, sin errores de consola.
- P4.4: ballot-box (RF-09, RF-10, RF-12 parcial, D-32): recepción de la papeleta y de los estados, cadena de
  eventos con hash (OPENED, VOTE_RECEIVED sin hora, CLOSED) y huella final, emisión del voto según D-20 (consumo en
  voter-access, voto + hash del recibo + evento en una transacción con la papeleta bloqueada, reversión si falla e
  incidencia solo con el día), verificación del recibo, seguridad (`/internal/**` con rol `SERVICE`), Flyway V1,
  ruta del gateway `/api/v1/ballot-box/**`. Prueba de extremo a extremo (28/28) con election, voter-access (con el
  adaptador SMTP real) y ballot-box reales: voto, voto en blanco, recibo, doble voto, caída de voter-access y
  reintento, 5 envíos simultáneos con el mismo token (un solo voto), cierre y recálculo independiente de la cadena.
- P4.3: voter-access (RF-08, D-31): recepción idempotente de la instantánea y de los estados, censo congelado
  con credencial, participación e invitación por persona, invitación por correo con enlace personal (solo hash),
  código de 6 cifras por correo con límites y bloqueo (RNF-14), token de voto de un solo uso, `consume`/`revert`
  para ballot-box con bloqueo de fila (RNF-12) y día sin hora (D-18), seguridad (`/internal/**` con rol `SERVICE`,
  voter público), Flyway V1, ruta del gateway `/api/v1/voter-access/**`. Prueba de extremo a extremo (36/36) con
  election real, voter-access real y ballot-box simulado: invitaciones, idiomas, hashes, 404/410/422/423/429/503,
  consumo doble, reversión y «ya has votado».
- P4.2: ciclo de vida (RF-07, D-30): `schedule` (validación completa con 422, congelación, instantáneas en el
  *outbox*), apertura y cierre automáticos (`LifecycleScheduler`), `archive` (`TALLIED → ARCHIVED`), historial con
  usuario o `SYSTEM`, Flyway V3 (`process_sync`) y entrega del *outbox* (`ProcessSyncDispatcher`, token
  *client credentials*). Prueba de extremo a extremo (22/22) con receptores simulados: orden, reintentos tras 503,
  contenido de las instantáneas (ballot-box sin datos de votantes) y borrado del `payload`.


## Pendiente

- Validar con el tutor la reducción a 5 servicios (D-05) y la exclusión del revoto (D-03).
- Corregir el Gantt del TFM (`plan_gantt.tex`): la iteración 3 debe quedar en RF-09–RF-10.
- Verificar el último parche de Spring Cloud 2025.1.x, springdoc 3.1.x y Maven (P1.1); de Ionic 9 (P3.4).
- Prototipos: pantallas que faltan para RF-17 y RF-21 (ver `PROTOTYPES.md`).
- Seguridad de `election`, `voter-access` y `ballot-box`: los esqueletos no incluyen Spring Security;
  se añade en el hito de cada servicio (P3.3, P4.3, P4.4).
- CORS y API keys en el gateway (P6); rutas de los demás servicios cuando existan sus contratos (P2).

## Bloqueos / preguntas abiertas

- ¿Se activan las pruebas de propiedades críticas al terminar P4? (IMP-03)
- Retraso respecto al Gantt: la iteración 1 (RF-01–RF-03) acababa el 31/10.

## Cómo continuar (siguiente paso exacto)

1. Commitear P6.1 en `feat/traceability` e integrarlo en `dev`.
2. **P6.2**: notificaciones (RF-17): recordatorios a quien no ha votado, aviso de cierre y vista de envíos fallidos.
3. Después P6.3 (RF-18, RGPD) y P6.4 (RF-21, API con claves por organización).
