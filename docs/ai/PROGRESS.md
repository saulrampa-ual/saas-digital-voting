# PROGRESS · VeriVoto

Última actualización: 2026-11-20 · Rama: `dev` (HEAD `4d0ed29`; cambios locales sin commit) · sin push

## Hitos

| Hito | Fechas | Objetivo | Estado |
| --- | --- | --- | --- |
| P0 | 29/10 – 02/11 | Línea base y memoria de la IA | Hecho (`da2e6db`) |
| P1 | 02/11 – 08/11 | Java 25, monorepo Maven, migración de `tenant-service`, esqueletos, infraestructura, CI de compilación | Hecho (CI en verde, `efe8afc`) |
| P2 | 05/11 – 10/11 | Modelo de datos, separación de secreto, contratos OpenAPI de la rebanada 1, ADRs | Hecho (`ce6cb37`) |
| P3 | 09/11 – 17/11 | Organizaciones y roles, configuración de Keycloak, censo, base del frontend y login | Hecho (`b7aea1c`) |
| P4 | 16/11 – 30/11 | Rebanada vertical del voto (RF-06–10, RF-20) | Hecho (todo en `dev`, `cbecfd2`) |
| P5 | 30/11 – 14/12 | Auditoría, escrutinio y resultados (RF-12–16), modelo de amenazas | Hecho (todo en `dev`, `4dd72fc`) |
| P6 | 14/12 – 31/12 | Consola y transversales (RF-05b, RF-17–19, RF-21) | Hecho (todo en `dev`, `ffc4f7f`) |
| P7 | 01/01 – 06/01/2027 | Pruebas y medición (H4) | Cerrado el 15/11/2026: 151 pruebas backend y 92 frontend, 0 fallos; H1a sin decisión (parte manual no realizada); H1b aceptada (09/11); H1c rechazada (11/11); RNF-4 aceptado (13/11). Limitaciones en `docs/evaluation/resultados-p7/cierre.md` |
| P8 | 01/12 – 27/01/2027 | Memoria del TFM | En curso: criterios y resultados de P7 incorporados; PDF recompilado con 93 páginas y presentación de defensa con 20 diapositivas; revisión editorial y visual completa aún pendiente |
| P9 | Tras la entrega | Post-MVP: RF-11, RF-22, RF-23 | Fuera de alcance |

## Hecho

- 20/11: RNF-18 actualizado en la memoria con la prueba de reinstalación del icono PWA en Android y sus límites;
  se añadieron dos capturas recortadas en `Plantilla-TFM-Informatica-UAL/Figuras/capturas/`. El acceso usó una
  ruta sintética sin invitación real y mostró «Página no encontrada»; no se verificó el voto. La memoria se compiló
  con pdfLaTeX en 96 páginas y se revisó visualmente la tabla y la figura.
- 20/11: README principal y frontend ampliados con requisitos, preparación de `infra/.env`, arranque local,
  direcciones y acceso a manuales; creado `backend/README.md` con comandos de inicio de los cinco servicios y
  comprobaciones de salud. No se ejecutaron builds ni pruebas (cambio documental).
- 19/11, noche (sin commit): **serie C de H1c, rechazada; H1c cerrada.** `mvnw verify` de ballot-box: 33 pruebas, 0
  fallos. Humo correcto (20 de 20 en menos de 2 s). Ejecución 30: 231 votos aceptados, 269 conexiones rechazadas y solo
  17 de 500 respondidas en menos de 2 s; cadena íntegra. No procede la serie D (los votos aceptados también superan los
  2 s). Memoria (tablas 13.1 y 13.2, secciones de H1c, conclusiones; 93 páginas), diapositivas 11 y de reserva,
  `guion.md` y `preguntas.md` actualizados; compilan.
- 19/11, tarde (sin commit): **IMP-64 implementada (D-65)**, a petición del autor, para intentar H1c en una serie C.
  `GroupCommitVoteWriter` (confirmación por grupos con un escritor por proceso, grupo barajado, recibo solo tras el
  commit) y arreglo del commit ambiguo (si el recibo del grupo está en la urna, no se revierte el token).
  `verivoto.voting.max-group-size` (100). Pruebas: `GroupCommitVoteWriterTest` (5), un caso nuevo en
  `VotingServiceTest` y uno de 40 votos simultáneos con PostgreSQL en `BallotBoxScenariosIntegrationTest`. En el
  entorno de la IA (javac 21, sin Maven): dominio y application compilan sin avisos nuevos y `GroupCommitVoteWriterTest`
  pasa 100 de 100 (20 repeticiones de los 5 casos) con un arnés propio; falta el `mvnw verify` del autor. k6: métrica
  `answered_under_2s` y regla 4 de la serie C en `carga.md` (una conexión rechazada no responde). Revisión del secreto:
  sin datos, tiempos, registros ni contratos nuevos; el orden dentro del grupo se baraja. Por error, una prueba de
  `check-load-run.mjs` con `K6_ONLY=1` sobrescribió `.local/load/check-run-26.json` sin los datos de la urna (ya
  anotados en `carga.md`); se regenera ejecutando `check-load-run.mjs` con `LOAD_RUN=26`.
- 19/11 (sin commit): **H1c, serie B, cerrada como rechazada.** Entorno local arreglado (administrador de Keycloak,
  clientes que faltaban, audiencia y secretos; `scripts/dev/keycloak-restore-local-admin.ps1`,
  `keycloak-missing-clients.mjs`, `keycloak-sync-secrets.mjs`) y arranque reproducible (`start-services.ps1`, que ahora
  comprueba los puertos, y `stop-services.ps1`). Ejecución válida 26: 339 de 500 aceptados, p95 7211 ms, cadena
  íntegra. Las colas ampliadas dejan entrar más votos, pero el ritmo sigue en unos 43 por segundo (un voto por
  transacción). Memoria (tablas 13.1 y 13.2, sección de H1c, conclusiones), diapositivas 11 y de reserva, `guion.md`
  y `preguntas.md` actualizados; compilan. IMP-64 (confirmación por grupos) registrada como propuesta. No afecta a
  OE1–OE5.
- 18/11, tarde (sin commit): **IMP-63** hecha. En Firefox y WebKit las POST al mismo origen se saltaban `page.route`
  (trazas de la segunda ejecución de M2); ahora llegan a un servidor simulado de respaldo (`tests/e2e/support/api-mock.ts`
  y `mock-router.mjs`, proxy `proxy.e2e.conf.json`, servidor de pruebas en el puerto 4300) con la misma respuesta
  sintética. Entorno de la IA: Chromium 212/212 y, forzando el salto de las POST, las 56 afectadas pasan. Equipo del
  autor: Firefox **212/212**; WebKit **211/212** (una prueba agotó el tiempo esperando la casilla del código; repetida sola el 19/11, pasa). RNF-2 aceptado en la tabla 13.2. **M1:** guion y hojas adaptados al Narrador de Windows y a la versión actual; nueva fila
  recomendada TM-01 (IMP-50). **M5 / IMP-57:** `SO_BACKLOG` 1024 en el *gateway* (`ConnectionBacklogConfiguration`) y
  `server.tomcat.accept-count` 1000 en ballot-box y voter-access; serie B de H1c definida en `carga.md` antes de
  medir. **No compilado en el entorno de la IA** (sin Java 25). Revisión del secreto del voto: solo cambian colas de
  conexión; no hay datos, registros, orden ni tiempos nuevos.
- 17/11: eliminada `.local/sheet-15min.png` a petición del autor. Memoria compilada dos veces con pdfLaTeX (93 páginas) y presentación compilada dos veces (20 páginas: 14 principales y 6 de reserva); ambos PDFs sin referencias sin resolver.

- 15/11: memoria recompilada dos veces con pdfLaTeX (95 páginas; sin referencias sin resolver). Revisión visual solicitada completada: tabla 13.1 legible en una página y su explicación continúa correctamente en la siguiente; figuras 8.1 y 8.2 legibles, sin solapamientos. Script `node scripts/dev/keycloak-api-audience.mjs` ejecutado contra Keycloak local ya activo; añadió el audience mapper `verivoto-api` a seis clientes (admin, cuatro servicios y Swagger). No se modificó el comportamiento del voto.
- 15/11 (C4, lectura del PDF de 95 páginas): sin texto ni figuras cortados. Corregidos el ancho de la tabla 1.1, la ruta larga del apéndice A.1 (`\path`) y las filas de P7 y P8 de la tabla 9.2, que aún daban RNF-4 como pendiente. La contraportada queda al límite de espacio: el resumen no admite más texto. Pendiente de la compilación del autor.
- 16/11 (sin commit): **IMP-50** hecha (mensaje propio del 429 del *gateway* en el flujo del votante, con la espera de `Retry-After` cuando se puede leer). **A5, flujo del votante:** `voter-flow.page.spec.ts` (51 pruebas de la máquina de estados: enlace, código, papeleta, voto, recibo, resultado, cuenta institucional) y un caso más en `voter-api.service.spec.ts`. En el entorno de la IA (Node 22, Linux): Vitest 144 de 144 en 13 ficheros, `ng build` correcto, `tsc` de las pruebas sin errores y cobertura de líneas del frontend 43,2 % (989/2287); Ejecución del autor (16/11, `npm run test:coverage`): 144 de 144 y cobertura de líneas 42,0 % (960/2287), ya en la memoria. **Barrido axe:** tres estados de IMP-50 añadidos a `voter-flow.accessibility.spec.ts`; 100 de 100 sin críticos ni serios (Chrome en el entorno de la IA). Capturas de la memoria agrupadas de dos en dos (figuras 11.1–11.6, tres páginas en lugar de seis). Revisión del secreto del voto: el cambio es solo de interfaz; no añade registros, almacenamiento ni datos nuevos.
- 16/11, tarde (sin commit): **D4** (IMP-11, IMP-40): suspender una organización desactiva en Keycloak sus cuentas de gestión y los clientes de sus claves activas, y bloquea crear cuentas nuevas; reactivarla las activa (GET y PUT de la representación completa, idempotente; Keycloak primero y estado después). **Autorización HTTP de identity:** `IdentityAuthorizationIntegrationTest` (7 casos con PostgreSQL real y Keycloak simulado) más `OrganizationServiceTest` (5), un caso en `ManagementUserServiceTest` y 4 en `KeycloakAdminClientTest`; `spring-boot-starter-webmvc-test` y `spring-security-test` añadidos a identity. **No compilado en el entorno de la IA** (sin Java 25 ni Maven Central): falta el `mvnw verify` del autor, que debería dar 168 pruebas de backend. **IMP-46** aplicado en `build.yml`; **IMP-48** a la espera de los *digest*. **C4:** revisión de coherencia y estilo de la memoria (objetivos, alcance, plan de evaluación, RQ1, limitaciones, RF-13, SMS, comillas, «revotación», «en línea»). **Defensa:** `Presentacion-TFM/` con `presentacion.tex` (17 diapositivas y 4 de reserva), `estilo-ual.sty`, `guion.md` y `preguntas.md`.
- 16/11: eliminados de `.local/` `old-fe-src.tgz`, `voter-flow.accessibility.spec.ts` y la carpeta `figs-preview` a petición del autor. Memoria recompilada en dos pasadas con pdfLaTeX: 92 páginas, sin referencias sin resolver.
- 16/11: eliminados a petición del autor los ficheros de vista previa `.local/s-08.png`, `s-13.png`, `sheet0.png`, `sheet1.png`, `t.png` y `tex.tgz`; verificada su ausencia.
- 17/11: verificados D4, IMP-46 y las pruebas de identity. `mvnw -pl services/identity-service -am test`: 30 pruebas, 0 fallos (incluye 7 HTTP con PostgreSQL 18.6 y 10 unitarias asociadas a D4). Consultados los digest OCI actuales: Keycloak `sha256:3f3c5f8e03c614774b4bd9173b88391fefa4153bfc010d8012d59ad825377332`, Gitleaks `sha256:c00b6bd0aeb3071cbcb79009cb16a60dd9e0a7c60e2be9ab65d25e6bc8abbb7f`, Trivy `sha256:790fd66d1697395d8693d1acaa1ea702406a20f109b6fc7bac8db623eca6c06c`; aún no se fijan en los manifiestos.
- 17/11 (sin commit): **IMP-48** aplicado con los *digest* del autor (Dockerfile de Keycloak, gitleaks y Trivy en `security.yml`). Memoria, `informe.md`, `cierre.md`, `PLAN.md` y `TRACEABILITY.md` con 168 pruebas de backend (identity 30), 312 en total, 23 de integración sobre PostgreSQL en 9 clases y cobertura de líneas del backend 59,1 % (3479/5890; identity 75,2 %); la autorización HTTP de identity deja de figurar como pendiente. **Defensa: 15 minutos** (confirmado por el director): `presentacion.tex` pasa a 14 diapositivas y 6 de reserva; `guion.md` rehecho (≈ 13 min). Pendiente de decisión: prioridades MoSCoW de los RNF (columna «Por validar» de la tabla 7.1).
- 18/11 (sin commit): tabla 13.2 «Evidencia por requisito no funcional» en `evaluacion_resultados.tex` (Must: 2 aceptados y 8 con evidencia parcial; Should: RNF-4 aceptado, RNF-17 no se cumple, RNF-10 sin evaluar). Plan de mejoras M1–M6 en `PLAN.md` (8.4). Preparadas M2 (`E2E_BROWSERS=all` añade Firefox y WebKit; 636 pruebas listadas, por defecto 212) y M3 (`docs/evaluation/revision-heuristica/`).
- 18/11, mediodía: **M2, primera ejecución del autor** (636 pruebas a la vez): Chromium 212/212, Firefox 115/212, WebKit 173/212; los 136 fallos son tiempos agotados de 30 s o cierres del navegador (`Protocol error`), ninguno por axe. No concluyente. Ajustes sin commit: 90 s por prueba en Firefox y WebKit, nombre de fichero de axe con el navegador (antes se sobrescribían) y ejecución por motor con 2 *workers*. Comprobado en el entorno de la IA que Chromium sigue igual (16 de 16 en una muestra; 636 pruebas listadas con `E2E_BROWSERS=all`).
- 18/11, tarde (sin commit): **M2, segunda ejecución** (un motor cada vez): Firefox y WebKit 156/212 cada uno, sin incumplimientos críticos ni serios; las 56 restantes no llegan a su pantalla porque esos motores no pasan las POST por `page.route` (502 del proxy) → IMP-63. **M4** hecha por el autor (PWA instalada en iOS y Android). **D-64, identidad de la PWA:** iconos de marca generados con `scripts/brand/pwa-icons.py` (rsvg-convert) en `frontend/public/icons/` (`any` 72–512, `maskable` 192 y 512, `apple-touch-icon` 180), manifiesto con iconos `any` y `maskable` separados e `id: "./"`, `apple-touch-icon` en `index.html`, banderas del selector visibles en móvil (18×12 px). Verificado en el entorno de la IA (Chrome, Node 22): tamaños de todos los PNG iguales a los declarados, `ng build` correcto con los iconos y el `apple-touch-icon` en `dist/` y en `ngsw.json`; selector a 360 y 390 px, es y en, en voto y recibo: banderas visibles, botones de 44 px de alto (52–54 px de ancho), sin solaparse con el título ni desbordar la página; consola a 360 px comprobada en capturas; barrido axe completo en Chromium 212/212 sin críticos ni serios. **No verificado:** el icono nuevo en un teléfono (hay que reinstalar la PWA) y Vitest (`npm run test:ci`) tras el cambio de estilos. Tabla 13.2: RNF-2 con Firefox, WebKit y dispositivos reales; RNF-18 aceptado.

- H1c: completados en `docs/evaluation/carga.md` los datos del equipo usados en la medición: AMD Ryzen 7 5825U,
  16 GB RAM (15,4 GiB visibles para Windows) y Docker 29.4.0 (build 9d7ad9f). Datos consultados el 11/11/2026.

- P7.0: pruebas activadas el 2026-10-31 según autorización `ACTIVAR TESTS` del prompt de continuación. Añadidos
  `spring-boot-starter-test`, Testcontainers 2.0.5 (gestionado por Spring Boot), ArchUnit 1.5.1 y JaCoCo 0.8.16;
  CI backend ejecuta `verify` con tests. Último `mvnw verify` completo con Java 25.0.5: BUILD SUCCESS,
  49 pruebas backend, 0 fallos/errores; Testcontainers usó cuatro contenedores PostgreSQL 18.6.
- P7.2 (parcial): cuatro pruebas de integración ejecutan todas las migraciones de identity (5), election (6),
  voter-access (4) y ballot-box (6). Comprueban append-only para acciones administrativas, columnas/scoping
  de organización mediante el adaptador/JPA real de censos en election, participación por fecha sin hora y ausencia
  de datos de identidad/tiempo en la urna; también prueban el rechazo de un evento `VOTE_RECEIVED` con hora.
  La autorización HTTP y la cobertura de otros repositorios/entidades siguen pendientes.
- P7.1/P7.3 (parcial): tests de permisos/último administrador/API keys, ciclo de vida y programación, código/OTP,
  consumo del token, compensación si falla el guardado, evento de voto sin tiempo/hash del recibo, participación por
  día, cadena, escrutinio, y reglas de arquitectura por servicio. Cobertura de líneas de la batería inicial: gateway 0,0 %,
  identity 13,6 %, election 10,3 %, voter-access 7,7 %, ballot-box 13,2 %; véase `docs/evaluation/informe.md`.
  Es parcial y no acredita esquema/logs ni cobertura suficiente; P7 sigue abierto.
- Frontend P7.1: runner Angular `@angular/build:unit-test` con Vitest 5.0.5 + jsdom 30.1.3; 10 pruebas de
  guards de rol, flujo API papeleta→voto→recibo y PKCE/state/nonce; `npm run test:ci` y `npm run build` pasan.
  CI ejecuta unit tests y build; `npm install` con Node 24.15.0 terminó sin vulnerabilidades reportadas.
- P7.4/P7.5/P7.6/P7.7 (material inicial): lista manual de accesibilidad, guion k6 para 500 VU con tokens
  sintéticos externos, protocolo, consentimiento, guion y hoja de registro de usabilidad; informe de evaluación
  con H1a/H1b/H1c y RNF pendientes marcados, sin inventar resultados. Carga no ejecutada.
- P8.0: compilación base de 69 páginas y diagnóstico inicial en `docs/evaluation/diagnostico-memoria-p8.md`.
  El autor aprobó el enfoque el 2026-10-31; las ediciones autorizadas comenzaron después de esa confirmación.
- Frontend: `npm run build` terminó correctamente (bundle inicial 1,07 MB; 215,26 kB de transferencia estimada).
- P7.6 (2026-10-31, rama `docs/p7-usability-session`, sin commit): preparación de la sesión con cinco votantes:
  escenario sintético `USAB` (censo de 7 entradas `@verivoto.test` con 2 reservas, papeleta neutra, opción fijada por
  tarjeta), arranque y montaje, lista previa por participante, marcas T0/T1/T2 (H1b = T0→T1, acceso + emisión + recibo),
  definición de ayuda, reglas de sesiones válidas/reservas y hoja con resumen en cantidades. CSV comprobado contra las
  reglas de importación del censo; el escenario no se ha ejecutado.
- Integración P7/P8 en `dev` (2026-10-31): commits `912dc7b`, `9c87bd0`, `2a6a39e` y merge `1208892`; rama
  `chore/p7-p8-tfm-work` borrada en local tras comprobar que estaba integrada. Sin push.
- P8.5 (2026-10-31, revisado y aprobado por el autor): aplicados en la memoria los criterios confirmados por el
  tutor (D-49): H1a/RNF-1 unificado (cero incumplimientos críticos ni serios + revisión manual sin bloqueos), H1b/RNF-3/RNF-6
  en cantidades (5 de 5 en ≤3 min; al menos 4 de 5 interpretan la verificación), RNF-4 (al menos 4 de 5 administradores en
  ≤10 min, valor propuesto) pendiente y sin hipótesis; nota de n = 5; tabla 13.1 con «criterio del protocolo» y «resultado
  medido» (todo PENDIENTE); D-03/D-05 confirmadas por el tutor por correo el 30/10/2026 en 1.5, 10.1, 10.5, apéndice A,
  6.1.11 y 14.3; nota de alcance de revoto al inicio del capítulo 6; RF-11 retirado de la figura 8.2 y del listado A.2;
  listado A.3 alineado con la figura 5.2 (el voto no se cifra en cliente); 14.2 remite a 14.3. Actualizados también
  `docs/evaluation/informe.md` y `docs/evaluation/usabilidad/protocolo.md`. PDF compilado con pdfLaTeX (TeX Live 2023)
  en el entorno de la IA: 82 páginas, sin errores ni referencias indefinidas; inspeccionadas las páginas modificadas.
  No se regeneraron los PDF de PlantUML (no hay PlantUML 1.2026.8 disponible; las figuras de la memoria son TikZ).

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

- P6.5: inicio de sesión del votante con cuenta institucional (RF-05b, D-48): cada proceso elige código por correo o cuenta institucional (election V6, voter-access V4); con SSO el votante abre su enlace y entra con Keycloak (cliente público `verivoto-voter`, PKCE), y voter-access acepta el ID token si su correo verificado es el del censo (otra cuenta cuenta como intento fallido); pantallas B1 (entrada con la cuenta), `/votar/sso` (vuelta) y opción en A3; invitación adaptada. Prueba de extremo a extremo de la IA (24/24) y en navegador (19/19); regresión completa en verde.
- P6.4: API documentada con claves por organización (RF-21, D-47): cada clave es un cliente confidencial de Keycloak creado por identity (V5 `api_keys`, sin secreto) con rol `INTEGRATION` de solo lectura y `organization_id` fijo; lectura de procesos, participación y resultados; pantalla «Claves de API» (secreto mostrado una vez, revocación); realm con el rol nuevo y permisos de clientes para identity; gateway con `/api/v1/api-keys`. Prueba de extremo a extremo de la IA (26/26) y en navegador (20/20); regresión completa en verde.
- P6.3: derechos sobre datos personales (RF-18, D-46) en election: búsqueda por correo o teléfono, exportación JSON (datos del censo, participación y correos, preguntados a voter-access) y borrado: se borran las entradas de censos no usados y se anonimizan las de censos usados, con la anonimización del censo congelado de voter-access por la *outbox* (`ANONYMIZE`, Flyway V5); bloqueado con procesos programados o abiertos. Pantalla F2 «Privacidad (RGPD)» para ADMIN; gateway con `/api/v1/privacy/**`. Prueba de extremo a extremo de la IA (34/34) y en navegador (22/22); regresión completa en verde.
- P6.2: notificaciones a votantes (RF-17, D-45): recordatorios del ADMIN a quien no ha votado (solo el número de destinatarios), aviso de cierre a todo el censo, estado `CANCELLED`, vista de correos con fallidos y reintento en A5, y su trazabilidad (nuevo cliente de Keycloak `voter-access-service`). Prueba de extremo a extremo (40/40) y en navegador (26/26); regresión completa en verde. En `dev` (`ab1f55c`).
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


- Recuento final: `mvnw verify` completo del backend y `npm run test:coverage` del frontend, para actualizar en la
  memoria, la presentación e `informe.md` el número de pruebas (ballot-box pasa de 26 a 33; integración, de 23 a 24) y
  la cobertura. Hoy las cifras publicadas son las del 17/11.
- M1 (20/11): revisión con el Narrador según `docs/evaluation/accesibilidad-manual-guion.md`.
- M3: revisión heurística con una segunda persona (`docs/evaluation/revision-heuristica/`).
- Q-13 y correo de seguimiento al tutor con los resultados del 18–19/11.
- M6/A9 (SSO @inlumine.ual.es): bloqueado y trabajo futuro (P9). No es posible probarlo en esta fase por falta de acceso a las cuentas y a la configuración institucional; validar con el tutor. Opción oculta en la consola (features.institutionalSso = false, 20/11/2026).
- M6b (IMP-66): «Identificador institucional» oculto en la interfaz de gestión y plantillas Excel regeneradas (20/11/2026, sin commit). El campo se conserva en el censo y en el backend; la exportación de privacidad lo sigue incluyendo. Falta la revisión del autor, abrir las plantillas y el commit.

> P7 se cerró el 15/11/2026. Los puntos P7.x de esta lista que no se completaron quedan como limitaciones declaradas (`docs/evaluation/resultados-p7/cierre.md`), no como trabajo en curso. D2 y D5 fusionadas por el autor el 15/11/2026 (`c48dbab`; `mvnw verify`: 151 pruebas, 0 fallos); memoria, `informe.md`, `threat-model.md` e `IMPROVEMENTS.md` actualizados. C4: figuras 8.1 y 8.2 y tabla 13.1 revisadas visualmente el 15/11; memoria recompilada (92 páginas) el 16/11 y `keycloak-api-audience.mjs` ejecutado contra Keycloak local. Siguiente paso: el resto de C4 (A9 bloqueada).

- P7.1: ampliar cobertura unitaria de censo/CSV, validación de claims institucionales, privacidad, recordatorios,
  errores, pruebas del flujo visual del votante y reglas de dominio que aún no tienen pruebas.
- P7.2: ampliar integración PostgreSQL a otros repositorios, autorización HTTP entre organizaciones,
  concurrencia de tokens y reintentos/idempotencia de outbox.
- P7.3: inspección del esquema y logs para secreto del voto; pruebas de todas las manipulaciones de cadena y límites
  de datos temporales.
- P7.4: Playwright con Keycloak y Mailpit reales, flujos de votación y gestión en 1280/360 px y ES/EN, axe
  (la revisión manual de `docs/evaluation/accesibilidad-manual.md` no se realizó; H1a sin decisión); añadir job E2E a CI. El runner unitario está configurado;
  el navegador real y E2E siguen sin ejecutarse.
- P7.5: completar escenarios k6 de acceso y escrutinio; preparar en entorno local las credenciales sintéticas
  fuera del repo; ejecutar tres mediciones de voto con percentiles, errores y recursos y comprobar la cadena/participación.
- P7.6: materiales listos (`preparacion-sesion.md`, `censo-sesion.csv`, guion, hoja y protocolo alineados con D-49).
  El autor debe montar el escenario `USAB`, hacer un ensayo y realizar las cinco sesiones con votantes; resultados en
  la hoja como cantidades (x de 5).
- P7.7: completar `docs/evaluation/informe.md` con evidencias posteriores; H1a sin decisión (parte manual no realizada); H1b aceptada (09/11); H1c rechazada (11/11).
- P8: enfoque aprobado por el autor. P8.1 corrige el alcance de RF-11 y añade la comparación de planificación;
  los capítulos de diseño técnico, implementación, amenazas, evaluación y conclusiones son borradores basados en
  las evidencias actuales. Se recompiló el PDF de 79 páginas y se inspeccionaron la introducción, el flujo de voto,
  la tabla de hipótesis y capítulos añadidos. P8.4 aún requiere revisión visual completa y editorial; algunas
  secciones originales de análisis y los diagramas conceptuales deben alinearse con la implementación.
- Decisiones del autor: si en P9 se depura el modelo de datos de sustitución (`votes.status` `SUBSTITUTED`, RI-6/RI-8/RI-9);
  si RF-11 sigue como «Could» o pasa a «Won't» en la tabla 4.3; si «sin ayuda» (RNF-4) excluye consultar documentación
  (marcador `[PENDIENTE DEL AUTOR]` en RNF-4); si se anota RF-23/RF-22 como fuera del MVP en la tabla 1.1.
- P7.6: sesión con votantes hecha (H1b aceptada, 09/11). Queda repetir RNF-4 con administradores sin formación previa (`rnf4-protocolo-administradores.md`); modalidad online preparada el 11/11 (D-57), sin ejecutar ni probar con el stack: falta el ensayo remoto.

## Bloqueos / preguntas abiertas

- P8: pendientes la revisión visual completa del PDF, alineación integral de figuras y texto heredado. La evaluación empírica P7 sigue abierta.
- P7.3+: queda pendiente ampliar las comprobaciones de aislamiento de persistencia, autorización HTTP, concurrencia
  de token único y contratos HTTP/outbox.

## Cómo continuar (siguiente paso exacto)

Ver también `docs/ai/PLAN.md` (tareas, preguntas al autor y calendario). **Novedad del 13/11:** ver «Sesiones RNF-4 online, C2 y observaciones de participantes (13/11/2026)»: terminar las sesiones, registrar resultados de RNF-4 (C1), `cleanup`, repetir `.local\pruebas.ps1`, capturas de C2 y revisión del autor antes del commit. Novedad del 04/11: ejecutar `seed-usab.mjs` con el stack real, la primera ejecución de SonarQube y revisar el primer resultado del workflow `security` (F5). Novedad del 05/11: escenario `USAB` preparado; antes del sábado 07/11 completar el consentimiento y confirmar participantes; ejecutar las pruebas de la rama `feat/voter-access-limit-jwt-audience` (D2, D5) y no fusionarla antes del 09/11; exportar las incidencias de SonarQube y los artefactos de `security` (F5).

0. Autor: resolver las decisiones pendientes (modelo de sustitución en P9, RF-11 Could/Won't, «sin ayuda» en RNF-4, RF-22/RF-23 en la tabla 1.1).
1. Cargar la demo UAL con `scripts/demo/seed-ual.mjs` (primer intento fallido al capturar el código de autorización;
   corregido, pendiente de repetir) y repetir `npm run test:e2e` contra el frontend de la rama (DEF-03 descartado).
2. Entregar el segundo encargo (`docs/evaluation/encargo-pruebas-p7-fase2.md`) a la IA de pruebas: k6, E2E con stack real
   y axe en todas las pantallas, logs, matriz de autorización completa, concurrencia y outbox.
3. RNF-4 online (D-57): rellenar los huecos de `rnf4-participantes-online.md`, `node scripts/demo/seed-rnf4.mjs`,
   `cloudflared tunnel --url http://localhost:4200 --http-host-header localhost:4200`, `.local\rnf4-remoto.ps1 -Url <URL>` y
   ensayo completo con R2 desde otra red antes de la primera sesión.
3. H1b aceptada (10/11) y H1c rechazada (11/11). Siguiente: parte manual de H1a (NVDA), repetir RNF-4 con
   administradores sin formación previa y, si el autor lo aprueba, IMP-57 (perfilar la emisión) en P9.
4. Revisar P8.1–P8.3, corregir la arquitectura objetivo/implementada, completar capturas seguras y ejecutar la
   revisión editorial y visual final de P8.4.

## Sesiones RNF-4 online, C2 y observaciones de participantes (13/11/2026)

Todo en `dev`, **sin commit**, pendiente de revisión del autor.

- **Scripts de sesión.** `scripts/demo/lib/login-code.mjs` no tenía `fixed` ni `FIXED_CODE` (`login-code-fixed`
  fallaba): añadidos según D-61 y probados por el autor con R1 (código 111111). Nuevo comando
  `seed-rnf4.mjs passwords`: regenera contraseñas, quita el bloqueo por intentos y reescribe
  `.local/sesion-rnf4.md` solo con la API de administración; el seed completo no termina con el modo remoto activo
  (IMP-59). El túnel rápido del 12/11 había caducado («Tunnel not found»); se levantó uno nuevo con
  `.local\rnf4-remoto.ps1`.
- **C2 (memoria).** Figura nueva `Figuras/diagramas/arquitectura_implementada.tex` (figura 10.1) y su texto en
  `diseno_tecnico.tex`; LibreTranslate en `implementacion.tex`; RF-03 pasa a importar Excel (.xlsx); texto heredado en
  futuro de `contenido1.tex` pasado a presente; participante «Voter-access API» en la figura de secuencia.
- **Cifras de pruebas de la memoria** actualizadas con la ejecución del 11/11/2026 (`.local/pruebas.log`, `jacoco.csv`):
  119 backend, 28 frontend, 16 de integración en 8 clases, 200 Playwright + axe; cobertura de líneas gateway 33,3 %,
  identity 14,4 %, election 70,3 %, voter-access 50,2 %, ballot-box 41,1 %. Ficheros: `evaluacion_resultados.tex`,
  `resumentexto.tex`, `conclusiones.tex`, `plan_gantt.tex`, `docs/evaluation/informe.md`.
- **Observaciones de participantes (D-63)**, aplicadas con las sesiones en curso por decisión del autor: A3 con un solo
  campo por texto y «Revisar la traducción» plegado; aviso de recuperación con «Seguir donde lo dejé»; idioma del login
  conservado en la consola; botones de idioma compactos en móvil; «Ver detalle» en la lista de procesos; indicador de
  desplegable en Claves API; pregunta de visibilidad más clara. Comprobado en el entorno de la IA: `ngc`, `ng build`
  y 53 pruebas Vitest (2 nuevas). El servidor de desarrollo del autor recompiló sin errores a las 13:55.
- **Mejoras registradas:** IMP-59 (seed en modo remoto), IMP-60 (autorización HTTP de identity), IMP-61 (una
  organización por participante).

- **Tarde del 13/11.** El túnel de las 11:07 cayó a las 17:16 por un corte de red del equipo (~1 h sin conexión: Cloudflare
  borró el túnel rápido); nuevo túnel a las 18:22 (`belts-six-sought-girls`). **Revisión del tutor (correo del 13/11):**
  invitado al repositorio (`javicriado`); guía de arranque `docs/GUIA-TUTOR.md`; prototipos visuales estáticos en
  `docs/prototipos/` (login, creación de proceso, votación, resultados, participación); resumen de cambios enviado en PDF.
  **CU en tabla:** `preambulo_cu.tex` (cargado en `main.tex`) y `scripts/memoria/cu-a-tabla.mjs`; convertidos RF-01 y RF-09
  como ejemplo (compilan); el resto, cuando el tutor apruebe el formato (`node scripts/memoria/cu-a-tabla.mjs
  Plantilla-TFM-Informatica-UAL/especificacion_detallada.tex --write`). **DCU:** propuesta en
  `Figuras/diagramas/dcu_{estilo,gestion,votacion}.tex` (`scripts/memoria/dcu.py`), **sin incluir** en la memoria hasta la
  reunión con el tutor. **D1 (IMP-15):** aviso de caducidad de sesión preparado en `.local/pendiente-d1/` (fuera del árbol;
  59 Vitest); **aplicado al repositorio el 13/11 tras la última sesión RNF-4**. F5 sin avanzar: la sesión de la IA no tiene acceso a GitHub ni al
  SonarQube local.

- **Cierre del bloque RNF-4 (13/11, noche).** Hoja `hoja-registro-rnf4.csv` completa (P01–P05 válidas, sin formación
  previa, sin ayuda, 2–4 min, tiempos redondeados al minuto); **RNF-4 aceptado** y aplicado en la memoria.
  `seed-rnf4.mjs cleanup` y `rnf4-remoto.ps1 -Off` ejecutados (cuentas deshabilitadas, código de acceso aleatorio para
  todos otra vez; los correos con «111111» de Mailpit son de las sesiones, todos anteriores al `cleanup`). Batería
  `.local\pruebas.ps1`: backend 119/119, Vitest 59/59, build correcto, E2E 199/200 (falla
  `A4-process-edit-ballot · 360px · en`, `target-size`; reproducido en el entorno de la IA y ajeno a D-63; IMP-62).
  Cifras de la memoria actualizadas (178 pruebas). IMP-62 aprobada y hecha: E2E 200/200 (entorno de la IA). **14/11:** SonarQube relanzado (gates en ERROR por cobertura e
  incidencias del código nuevo); 31 incidencias clasificadas (10 reales, 8 falsos positivos, 13 aceptadas) en
  `quality-and-security.md`; las 10 reales corregidas e IMP-49 hecha (frontend 22,7 % de líneas). Segundo análisis: incidencias nuevas del backend 0; quedaban `S1845`/`S8786` en `VoterData` (corregidas con `EmailRule`, común con `PrivacyQuery`) y el `input` de censos (ahora `aria-labelledby`; axe censos 24/24). Falta marcar 21 incidencias FP/aceptadas en SonarQube. Siguiente: el autor
  marca las aceptadas, relanza `sonar.ps1`, carga la demo para las capturas, commit y push. **14/11 (tarde):** commit `aa33912` en `dev` (subido por el autor); las 21 incidencias marcadas en SonarQube (0 abiertas, notas A; los gates solo fallan por cobertura del código nuevo, 65,3 %). A petición del autor se añaden pruebas para llegar al 80 %: backend `VoterXlsxReaderTest` (+12) y `MinutesTest` (sin compilar en el entorno de la IA), frontend 92/92 con `auth.service`, `session-timeout.component` y `census-detail.page` (cobertura del código nuevo estimada 87 %); detalle en `quality-and-security.md`. Siguiente: el autor relanza `sonar.ps1` (comprueba las pruebas del backend y la cobertura), carga la demo con un `DEMO_PASSWORD` nuevo para las capturas, commit y push, y limpieza de `.local` y del token. **14/11 (noche):** análisis relanzado por el autor: los dos gates en «Passed» (código nuevo: backend 85,8 %, frontend 87,3 %; 0 incidencias abiertas); pruebas subidas en `f370bc8`. `seed-ual.mjs` no ha terminado hoy (no existe `.local/demo-ual.md`). Siguiente: cargar la demo, capturas, actualizar en la memoria el texto de la cobertura, limpieza de `.local` y del token. **15/11:** texto de la cobertura actualizado en `evaluacion_resultados.tex`, `resumentexto.tex`, `conclusiones.tex` y `docs/evaluation/informe.md` con los informes de la ejecución del 14/11 (JaCoCo: 14,4–73,0 % por servicio, 49,9 % en conjunto; frontend 33,7 %; código nuevo 85,8 % y 87,3 % según SonarQube); sin commit. Recuentos de pruebas actualizados en la memoria, `informe.md` y `PLAN.md`: 135 de backend (Surefire del 14/11) y 92 de frontend en 12 ficheros (`npm run test:ci` del autor, 15/11); total 227. Figuras 5.1 (arquitectura objetivo) y 5.2 (secuencia de emisión) redistribuidas con más separación, sin cambiar componentes, mensajes ni numeración (`Figuras/diagramas/`); compiladas aparte en el entorno de la IA, pendiente de la compilación del autor. **15/11:** capturas de C2 hechas con el script nuevo `scripts/demo/capturas-memoria.mjs` (consola como ADMIN y AUDITOR, datos de la demo UAL; `seed-ual.mjs` no terminó por faltar las invitaciones en Mailpit, pero los datos ya existían) en `Figuras/capturas/`; seis incluidas en `implementacion.tex` (sección «Material gráfico», RF-06, RF-07, RF-14, RF-15/16, RF-19), sin compilar en el entorno de la IA. **15/11 (cierre):** memoria compilada con pdfLaTeX en dos pasadas (96 páginas; sin referencias sin resolver). Revisadas visualmente las figuras 5.1 y 5.2 y el capítulo 13; el Abstract sigue desbordando a la página XI casi vacía. Confirmado en `seed-ual.mjs` que Laura Moreno y Sergio Cano son cuentas de prueba `@ual.test`; «zc<» y «UAL PERMISO NAVIDAD» no aparecen en el seed y requieren una captura limpia antes de la entrega. Se eliminó `.local/` (incluidos JAR y logs; stack local detenido); `SONAR_TOKEN` no aparece en los ámbitos de proceso, usuario ni equipo. Pendiente: capturas del flujo del votante cuando haya un proceso abierto con alguien sin votar; commit y push cuando lo decida el autor.

Pendiente de esta sesión: anotar en `observacion` qué versión de la interfaz usó cada participante; capturas seguras
(C2) cuando acaben las sesiones; repetir `.local\pruebas.ps1` (backend, Vitest, build y E2E) con el stack libre y
actualizar las cifras si cambian; revisar a mano en móvil los cambios de D-63; `cleanup` y `rnf4-remoto.ps1 -Off` al
terminar el bloque.

## RNF-4: adaptación a sesiones online (11/11/2026)

- D-57: participantes a distancia por videollamada, acceso por un único túnel HTTPS (Cloudflare Tunnel) al puerto 4200.
- `rnf4-protocolo-administradores.md` reescrito para online (acceso remoto, guion del moderador, fuentes de tiempo,
  incidencias, latencia, limpieza). Nuevo `rnf4-participantes-online.md` (parte A antes, parte B en la llamada).
- `hoja-registro-rnf4.csv`: añade `fecha`, `modalidad`, `navegador_so`, `t0`, `t1`, `incidencias_tecnicas`,
  `referencia_grabacion`; `rnf4-summary.mjs` lee por nombre de columna y sigue funcionando (comprobado con la hoja vacía).
- Nuevo `scripts/demo/seed-rnf4.mjs`: organización `RNF4`, 7 cuentas ADMIN (`admin.rnf4.p01`…`r2`) con contraseña
  aleatoria no temporal, deshabilitadas hasta `enable Pxx`; censo «Censo de prueba RNF-4»; `remote-on/off`,
  `disable`, `cleanup`. Credenciales en `.local/sesion-rnf4.md`. `node --check` correcto; **no ejecutado**.
- Front: `proxy.conf.json` reenvía `/realms/verivoto` y `/resources` a Keycloak; `app-config.ts` usa el mismo origen
  fuera de `localhost` (`tsc -p tsconfig.app.json --noEmit` correcto). `infra/compose.rnf4-remote.yaml`
  (`KC_PROXY_HEADERS=xforwarded`). Ayuda local `.local/rnf4-remoto.ps1 -Url … | -Off`.
- Sin probar con el stack: login por el túnel, emisor nuevo en los cuatro servicios, llamadas entre servicios con
  *client credentials* y eventos de auditoría al crear/programar. Sin commit.

## H1c: resultado de la prueba de carga (11/11/2026)

- Ejecuciones 1, 2 y 3 (procesos 10, 11 y 12), 500 VU × 1 sin rampa: p95 de la emisión 4852, 2402 y 4849 ms; 201 =
  222, 240 y 281; conexiones rechazadas (`0`) 273, 260 y 219; 5 × 500 en la 1. Cadena íntegra y urna = 201 =
  participación en las tres. **H1c RECHAZADA (H0c).** Detalle y observaciones en `docs/evaluation/carga.md`.
- Ejecución 2: `docker stats` sin muestra dentro de los 2,6 s de k6 (condición 6 no cumplida estrictamente); no
  cambia la decisión. Los servicios Java corren fuera de Docker: sin CPU/RAM de los servicios.
- Versión medida: jars de `.local/run/` del 31/10 y 03/11 (procesos en marcha desde el 03/11); el código de
  producción de *gateway*, ballot-box y voter-access es igual al de `dev` `879cb42`.
- Prueba de humo: ejecución 11 (proceso 9) correcta. Ejecuciones 9 y 10 (procesos 7 y 8) abiertas sin votos.
- Cambios del autor (otra herramienta) revisados: `prepare-voting-tokens.mjs` espera a que voter-access acepte el OTP,
  conserva los correos hasta generar los tokens y reanuda un proceso ya programado/abierto; ambos scripts toleran el
  rechazo de Keycloak por historial de contraseñas. Sin secretos en el código.
- Memoria: tabla 13.1, procedimiento, conclusiones, resumen/abstract y RNF-17. IMP-57 (perfilar la emisión).

## H1c: preparación de la comprobación y validación del backend (11/11/2026)

- Autor: `./mvnw verify` en Windows, BUILD SUCCESS en 5:36 min (election 46, voter-access 38, ballot-box 22 pruebas);
  backend de `feature/censo-importacion-ux` validado. «Connection refused» y «Surefire is going to kill self fork JVM»
  en election-service: anotado en IMP-55 (no se corrige ahora). k6 v2.3.0 instalado por el autor.
- Sin commit: cuenta `auditor.load@verivoto.test` y `.local/load/run-N.json` en `prepare-voting-tokens.mjs`; recuento por
  código en `tests/load/voting.js`; nuevo `scripts/load/check-load-run.mjs` (puntos 7 y 8 de `carga.md`). Probado solo
  el análisis del resumen de k6 v2.3.0 con un servidor simulado; sin ejecutar contra el stack.
- Siguiente paso: prueba de humo (`LOAD_RUN=9`, `LOAD_VOTERS=20`) y después las ejecuciones 1, 2 y 3.
- Ramas: `feat/voter-access-limit-jwt-audience` se queda sin fusionar hasta después de H1c; `test/p7-phase2-role-captures`
  la revisa el autor; la rama remota del censo la borra el autor al hacer push.

## H1b: resultado de las sesiones con votantes (10/11/2026)

- Sesiones del 09/11/2026 (escenario `USAB`). Hoja `docs/evaluation/usabilidad/hoja-registro-h1b.csv` corregida: valores
  `si`/`no`, segundos enteros (cronómetro min:s,cc redondeado; P02 «1,08,97» = 69 s) y sin los códigos de recibo que se
  habían anotado (la hoja no debe contenerlos).
- `node scripts/evaluation/h1b-summary.mjs`: 5 válidas de 5 (reservas usadas: 1; invalidadas por el entorno: 1);
  éxito sin ayuda en ≤ 180 s 5 de 5; comprensión 5 de 5; mediana 51 s, mínimo 36 s, máximo 79 s; 0 errores.
  **H1b ACEPTADA** (evidencia descriptiva, n = 5).
- P01 inválida (su entrada del censo ya tenía voto del 06/11; incidencia de entorno) y sustituida por R1. R2 se realizó
  pero no era necesaria: fuera del cómputo, mismo resultado.
- Aplicado en la memoria: tabla 13.1 (`evaluacion_resultados.tex`), procedimiento de `contenido1.tex`, `conclusiones.tex`,
  resumen y abstract (`resumentexto.tex`) y comentarios de RNF-3/RNF-6 (`especificacion_detallada.tex`); `informe.md` y
  `hoja-registro.md`. La frase del bloque G de `conclusiones.tex` (cambio del autor aún sin commit) también se ajustó en el
  árbol de trabajo. IMP-56: logo y margen de la pantalla del votante (P05, R2).
- Ramas: `feature/censo-importacion-ux` fusionada en `dev` (`1078a48`; Vitest 28/28 y build sin errores en copia Linux;
  backend sin ejecutar aquí por falta de Maven Central y Java 25) y borrada en local. Sin fusionar:
  `feat/voter-access-limit-jwt-audience` (exige actualizar el realm y pruebas Maven del autor) y
  `test/p7-phase2-role-captures` (conflicto en `scripts/demo/seed-ual.mjs` con la refactorización a `lib/common.mjs`).

## Evaluación H1a, H1b y H1c (05/11/2026, tarde)

- **H1a (parte automática)**: `frontend/tests/e2e/voter-flow.accessibility.spec.ts` recorre 22 pantallas y estados del
  votante con respuestas de API sintéticas y ejecuta axe 4.14.0 (WCAG 2.0–2.2 A/AA) a 1280 y 360 px en ES y EN: 88
  auditorías, 8 incumplimientos críticos (todos DEF-05: `aria-required-children` en el `ion-list` de la papeleta), 0
  serios. Ejecutado en el entorno de la IA sobre `dev` `4a5e593`. Informe `docs/evaluation/accesibilidad-axe-2026-11-05.md`;
  tabla 13.1, conclusiones, informe y defectos actualizados. Primer barrido: H1a rechazada en la versión medida.
- **DEF-05 corregido** (aprobado por el autor; sin commit): `ballot-step.component.ts` usa `div.options` en lugar de
  `ion-list`. Nuevo barrido con las mismas herramientas: 88 auditorías, 0 críticos, 0 serios, 0 moderados, 0 menores;
  Vitest 19 de 19 y compilación correctos. **H1a: parte automática cumple; revisión manual pendiente** (la hace el autor
  con teclado y NVDA). **DEF-06** abierto: el error de formato del código de recibo no se muestra (Ionic quita
  `ion-invalid`; WCAG 3.3.1); la prueba lo marca con `test.fail`. `receipt-check.accessibility.spec.ts` adaptada al
  selector de idioma con banderas (pasa). Lighthouse no ejecutado (no añade cobertura).
- **DEF-06 corregido** (05/11/2026, noche; aprobado por el autor; sin commit): `receipt-check.page.ts` muestra el
  error de formato en un elemento propio dentro de una región `role="alert"` en lugar de `errorText` (Ionic quitaba
  `ion-invalid`). Quitado el `test.fail` del estado «formato erróneo» en `voter-flow.accessibility.spec.ts`; la
  comprobación se conserva, se añade la del `role="alert"` y pasa en verde. Tercer barrido axe con las mismas
  versiones: 88 auditorías, 0 críticos, 0 serios, 0 moderados, 0 menores; `receipt-check.accessibility.spec.ts` 4 de 4;
  Vitest 19 de 19 y compilación correctos. DEF-05 cerrado por el autor. **H1a: parte automática medida y favorable;
  parte manual pendiente** (NVDA, autor, sobre esta misma versión).
- **H1a, parte manual (06/11/2026)**: preparado `docs/evaluation/accesibilidad-manual-guion.md` (paso a paso, sin
  resultados) y las hojas vacías `accesibilidad-manual-obligatorias.csv` (39 filas) y `accesibilidad-manual-recomendadas.csv`
  (10). Datos: demo UAL (`seed-ual.mjs`), no `USAB`. La revisión la hace el autor; sin datos todavía. El anexo del
  guion tiene la redacción por si no se realiza («parte automática favorable; parte manual no realizada»), sin aplicar.
- **Versión de H1a (08/11/2026)**: `dev` está en `06f39be` (merge de `fix/auditoria-visual`); las correcciones de DEF-05
  y DEF-06 entraron en `9c948cf`. Los ficheros del votante son idénticos a los medidos el 05/11 salvo por los saltos de
  línea (CRLF en Windows), por eso cambiaron dos SHA-256. Barrido 4 sobre `06f39be`: 88 auditorías, 0 críticos, 0 serios.
  Guion actualizado con el commit y los SHA-256 completos de `06f39be`. Copia temporal en `.local/claude-tmp/` (ignorada).
- **H1a, parte manual no realizada (08/11/2026)**: el autor no pudo hacer la revisión (NVDA no leía la página en su
  equipo) y decidió cerrarla sin ejecutar. Aplicado el anexo del guion en la tabla 13.1 y la sección de validez
  (`evaluacion_resultados.tex`), `conclusiones.tex` y `contenido1.tex` (lista de hipótesis y validez de constructo),
  con el barrido 4 sobre `06f39be`. **H1a: parte automática favorable; parte manual no realizada; sin decisión**
  (no rechazada; mismo tratamiento que RNF-4). Las hojas CSV se conservan sin rellenar. Sin commit.
- **A4, consola de gestión (08/11/2026)**: `frontend/tests/e2e/admin-console.accessibility.spec.ts` (nuevo, sin commit)
  simula Keycloak (código de autorización y token sintético por rol) y la API, y pasa axe por 27 pantallas o estados de
  la consola y los resultados públicos, a 1280 y 360 px y en ES y EN: 108 auditorías, 0 críticos, 3 serios, todos de
  **DEF-07** (`.table-wrap` desborda a 360 px y no recibe el foco: detalle de censo en ES y correos no entregados de
  Participación en ES y EN). DEF-07 registrado en `defectos.md` con dos opciones de corrección, **sin aplicar**; las
  3 combinaciones van como `test.fail`. 108 de 108 pruebas en verde. No cambia H1a. Informe:
  `docs/evaluation/accesibilidad-axe-consola-2026-11-08.md`.
- **DEF-07 corregido (08/11/2026)**, opción A aprobada por el autor: cada `.table-wrap` de la consola (detalle de
  censo, panel de correos, claves de API, privacidad, auditoría y trazabilidad) tiene `tabindex="0"`, `role="region"` y
  nombre. Barrido 2: 108 auditorías, 0 críticos, 0 serios; `test.fail` quitado, 108 de 108; Vitest 19 de 19 y build
  correctos. Avisos de axe sin decidir listados en el informe como «pendientes de decisión». Sin commit.
- **A10, A5 y RNF-4 (08/11/2026, noche; sin commit, pendiente de revisión)**: job `e2e-a11y` en `build.yml` con las
  tres pruebas axe (votante, recibo y consola), sin secretos; con `CI=1` en el entorno de la IA, 200 de 200 en 10,3 min.
  A5: `day-chart.component.spec.ts` (9 pruebas; Vitest 28 de 28). A5 backend y A6 bloqueados: el entorno de la IA no
  accede a Maven Central ni a Docker Hub y no tiene Java 25. RNF-4: `rnf4-protocolo-administradores.md` y
  `hoja-registro-rnf4.csv` para repetir la sesión con 5 administradores sin formación previa y tiempos cronometrados;
  no ejecutado. `scripts/evaluation/rnf4-summary.mjs` calcula la decisión desde la hoja (probado solo con datos
  inventados fuera del repositorio, igual que `h1b-summary.mjs`, que se volvió a comprobar el 08/11).
- **A5 backend y A6 (08/11/2026, noche; sin commit)**: pruebas unitarias nuevas en election (`CensusServiceTest`,
  `PrivacyServiceTest`) y voter-access (`JwtVoterIdentityVerifierTest`, `VoterAccessServiceSsoTest`,
  `NotificationAdminServiceTest`, `PersonalDataServiceTest`): 38. Integración con Testcontainers (A6, alcance acordado):
  aislamiento entre organizaciones por HTTP en los tres servicios (404 según OpenAPI), outbox (reintento, entrega única,
  rechazo permanente que bloquea), receptores idempotentes, 20 envíos simultáneos del mismo token = 1 voto, y secreto
  del voto en esquema y logs: 13 pruebas en 4 clases. `voter-access` y `ballot-box` ganan `spring-boot-starter-webmvc-test`
  y `spring-security-test` (scope test). Ejecutadas por la IA con las librerías de `~/.m2` del autor, Java 21 y, las de
  integración, contra PostgreSQL 16 local (sin Docker): todo en verde. **Verificado por el autor** el 08/11/2026:
  `./mvnw verify` en Windows (Java 25.0.5, Testcontainers con PostgreSQL 18.6): BUILD SUCCESS en 5:27 min; election
  36, voter-access 38 y ballot-box 22 pruebas, 0 fallos ni errores. En election aparecen errores de conexión en el log
  (no fallos) y Surefire espera 30 s al cerrar: ver IMP-55.
- **Tabla de alcance (08/11/2026)**: `dafo_justificacion.tex`, fila «Trabajo futuro» con RF-11 (informe RF-17, revoto,
  D-03); RF-17 del informe sale de la lista «sin ID local» y la correspondencia RF-11 → RF-17 se añade al párrafo de
  la tabla. Sin commit.
- **C3 y Q-01, Q-03, Q-04 (08/11/2026)**: respuestas del autor registradas (D-56) y aplicadas en `contenido1.tex`
  (tablas 1.1 y 4.3), `especificacion_detallada.tex` (nota de alcance, RF-11 prioridad W, RI-6) y `conclusiones.tex`
  (P9: modelo de sustitución descrito y no operativo). Sin commit.
- **H1b**: sin datos; sesiones el 09/11 (escenario `USAB` listo; hoja `hoja-registro-h1b.csv` y
  `scripts/evaluation/h1b-summary.mjs` preparados, probados solo con datos inventados fuera del repositorio). Con 5 participantes, el criterio de 90 % y 80 % que
  cita el autor equivale al validado por el tutor (5 de 5 en ≤ 3 min; 4 de 5 interpretan el recibo).
- **H1c**: sin datos. `scripts/load/prepare-voting-tokens.mjs` (sin probar) prepara cada ejecución; k6 sin instalar;
  hay que ejecutarlo después de H1b (Mailpit guarda 500 mensajes). Variables y ejecución válida definidas en `carga.md`.

## D2, D5, calendario H1b y respuestas del autor (05/11/2026)

- **D2 · IMP-21 (límite por IP en el *gateway*)**, commit `4ec0efc` en `feat/voter-access-limit-jwt-audience` (sin fusionar): filtro estándar `RequestRateLimiter` de Spring Cloud
  Gateway 5.0.3 en la ruta `voter-access`, con un `RateLimiter` propio en memoria (cubo de fichas por dirección IP; el
  *gateway* WebFlux solo trae el de Redis) y un `KeyResolver` por dirección de la conexión (`X-Forwarded-For` solo con
  `GATEWAY_TRUSTED_PROXIES` > 0). Paquete `com.saasvoting.gateway.ratelimit`; valores propuestos: ráfaga 120 y 120
  peticiones/min (`GATEWAY_VOTER_ACCESS_RATE_LIMIT_CAPACITY`, `…_PER_MINUTE`); `…_ENABLED=false` lo desactiva (carga
  H1c desde una sola máquina). Responde 429 con `Retry-After` y `X-RateLimit-Remaining`. En local todo el navegador
  entra por el *proxy* de Angular (127.0.0.1): el límite es común a todos los usuarios locales. No guarda enlaces,
  códigos ni identidades y no escribe logs (RNF-11). Comprobado en el entorno de la IA arrancando el *gateway* con las
  bibliotecas reales del build (compilado con Java 21): 5 peticiones permitidas y 429 a la sexta con ráfaga 5, otras
  rutas sin límite, `X-Forwarded-For` falso ignorado, recuperación tras `Retry-After` y desactivado sin límite.
  Pruebas nuevas (no ejecutadas: sin Maven Central ni JUnit en el entorno de la IA): `TokenBucketRateLimiterTest`,
  `ClientAddressKeyResolverTest`, `VoterAccessRateLimitIntegrationTest`.
- **D5 · IMP-27 (audiencia del JWT)**, commit `72bdfba` en la misma rama (sin fusionar): los cuatro servicios exigen `aud` = `verivoto-api`
  (`spring.security.oauth2.resourceserver.jwt.audiences`). Mapeador `oidc-audience-mapper` en `verivoto-admin`, las
  cuatro cuentas de servicio y `verivoto-dev-swagger` (realm y fichero `dev`), y en los clientes de claves de API que
  crea `KeycloakAdminClient` (RF-21). `verivoto-voter` no cambia: voter-access comprueba sus ID tokens (RF-05b) con un
  decodificador propio (mismo emisor, sin la audiencia de la API) porque el del servidor de recursos ya la exige.
  Prueba nueva `KeycloakAdminClientTest` (no ejecutada). Para un realm local ya importado:
  `node scripts/dev/keycloak-api-audience.mjs` (idempotente; probado contra un Keycloak simulado). Quitar
  `verivoto-dev-swagger` «en producción» no aplica sin despliegue (D-53); en local conserva la audiencia para Swagger.
  Main de los cinco servicios compilado con Java 21 contra las bibliotecas reales; **sin compilar con Java 25 ni
  ejecutar con Maven**.
- **No mezclar D2/D5 con las sesiones H1b**: con D5 activo, un realm sin el mapeador rechaza todas las llamadas (401).
  Propuesta: no fusionar la rama en `dev` hasta después de las sesiones del 09/11, o actualizar el realm antes.
- **Q-06**: ensayo 08/11 y sesiones 09/11, viables solo con las tres condiciones de `PLAN.md` el sábado 07/11. Si
  fallan, el autor propone 15/11 (ensayo) y 16/11 (sesiones), como fechas propuestas, no definitivas.
- **Q-11 / A12 / C1 (RNF-4)**: `docs/evaluation/usabilidad/resultados-rnf4-administradores.md`; 5 participantes (P01–P05,
  anónimos), 5 de 5 sin ayuda de personas (D-55); tiempos **estimados** 5, 5, 11, 3,4 y 3,4 min (4 de 5 ≤ 10 min); solo
  2 de 5 sin formación previa. RNF-4 no demostrado, evidencia indicativa; aplicado en la memoria (tabla de hipótesis,
  procedimiento, conclusiones y comentario de la especificación).
- **Q-12 (D-54)**: aceptación **verbal** de los 4 compañeros, sin constancia escrita; el autor no la pedirá por ahora.
  Se registra como limitación de A9 (RNF-16), nunca como consentimiento documentado.

- **Stack local**: los servicios arrancados el 03/11 siguen vivos tras suspender el equipo. El 05/11 a las 10:38
  `arrancar.ps1` los encontró sanos y no los reinició, así que siguen siendo los JAR del 31/10 y el 03/11 (sin D2/D5).
- **A11 · `seed-usab.mjs` con el stack real (05/11, 10:41)**: falla al crear la organización `USAB` con 503 «No se ha
  podido contactar con el servicio de identidad». Causa (log de identity-service): Keycloak responde 400
  `error-person-name-invalid-character` en `firstName`, porque el nombre de las cuentas de gestión llevaba paréntesis
  («Administración USAB (prueba)») y el validador `person-name-prohibited-characters` del realm los prohíbe. Keycloak
  rechazó el alta antes de crear nada y la organización no se guardó: no quedan datos a medias. Nombres corregidos en el
  script («Administración de prueba USAB», «Gestión del censo de prueba USAB»). El 503 en vez de un error de validación
  se registra como IMP-53.
- **A11 · segundo intento (05/11)**: falla antes de crear nada, en `PUT …/users/{id}/reset-password` de `superadmin`
  (400 `invalidPasswordHistoryMessage`). La política del realm (`length(12)`, `notUsername`, `notEmail`,
  `passwordHistory(3)`) no admite ninguna de las 3 últimas contraseñas, y el script fija `DEMO_PASSWORD` en
  `superadmin` en cada ejecución: repetirlo con la misma contraseña siempre falla (IMP-54). **La preparación de `USAB`
  no está hecha** hasta que el script termine y exista `.local/sesion-usab.md`.
- **A11 · tercer intento (05/11, 12:04)**: termina bien con una contraseña nueva: organización `USAB`, cuentas de
  administración y censo, censo de 7 entradas, censo de ensayo de 2, procesos #4 (sesión) y #5 (ensayo) abiertos hasta
  el 12/11/2026 a las 12:04, 9 invitaciones y `.local/sesion-usab.md`. **Escenario `USAB` preparado** (condición 1 de
  Q-06). Si las sesiones pasan al 15–16/11, los procesos ya estarán cerrados y habrá que preparar otros.
- D2 y D5 ya no están en el árbol de trabajo de `dev`: tienen commit en su rama y no deben fusionarse antes de las
  sesiones del 09/11 ni sin las pruebas del autor; al fusionar, ejecutar `scripts/dev/keycloak-api-audience.mjs`.
- Mejoras nuevas: IMP-50 (mensaje para el 429 del *gateway*), IMP-51 (el log de errores del *gateway* incluye el enlace
  personal en la ruta), IMP-52 (`.scannerwork/` en `.gitignore`; aprobada y hecha el 05/11, `333c27e`).
- Consentimiento de la sesión H1b completado con los datos institucionales y el contacto del autor (condición 2 de Q-06).

## F5 · SonarQube y CI (05/11/2026)

- Rama `docs/f5-sonarqube-ci` (desde `dev` en `6c30035`), solo documentación de F5: commit `a2e0fa9`, fusionada en
  `dev` (`2fb5f02`).
- SonarQube (captura del autor, análisis del 04/11): los dos *quality gates* en «Passed». Backend: seguridad D (6),
  fiabilidad C (5), mantenibilidad A (6), cobertura 18,8 %, duplicación 3,5 %. Frontend: seguridad A (0), fiabilidad D
  (12), mantenibilidad A (1), cobertura 0,0 %, duplicación 0,3 %. Pasar el *gate* no significa que no haya hallazgos.
- Workflow `build` (ejecución 41829391258): los tres jobs en verde y Vitest 19 de 19 (captura). Workflow `security`:
  sin verificar; solo consta la comunicación del autor.
- Pendiente: exportar la lista de incidencias de SonarQube y los artefactos `semgrep-report` y `trivy-report` a `.local/`,
  clasificar cada hallazgo (real / falso positivo / aceptado) y corregir los reales antes de F6.

## Sesión H1b y calidad del código (04/11/2026)

- A11 (`feat/usability-session-seed`, `d5608f9`, en `dev`): `scripts/demo/seed-usab.mjs` monta el escenario `USAB` de
  `docs/evaluation/usabilidad/preparacion-sesion.md` (organización, cuentas ADMIN y CENSUS_MANAGER, censo importado de
  `censo-sesion.csv` en dos pasos, proceso programado) y un proceso de ensayo con su propio censo; espera invitaciones y
  apertura y escribe `.local/sesion-usab.md` (buzón de Mailpit por entrada). No abre enlaces, no pide códigos ni vota.
  Ayudas comunes en `scripts/demo/lib/common.mjs`; `seed-ual.mjs` las usa sin cambiar su comportamiento. `.local/` pasa a
  `.gitignore` (antes solo en `.git/info/exclude`). Probado en el entorno de la IA contra un Keycloak, *gateway* y
  Mailpit simulados con navegador real: ejecución completa, segunda ejecución idempotente y demo UAL con votos. **Sin
  probar contra el stack real.**
- F1–F4 (D-51, `docs/architecture/quality-and-security.md`): SonarQube Community 26.10 en `infra/compose.quality.yaml`,
  `sonar-maven-plugin` 5.8.0.7211 en `backend/pom.xml`, `frontend/sonar-project.properties`, `scripts/quality/sonar.ps1`;
  `.github/workflows/security.yml` (gitleaks, Semgrep, Trivy, `npm audit`), `.semgrepignore`, `.pre-commit-config.yaml`.
  `actionlint` 1.7.12 sin errores y `docker compose config` del fichero de calidad válido. Línea base: 0 secretos en todo
  el historial, 0 vulnerabilidades npm, 1 aviso LOW de Trivy, Semgrep sin hallazgos `ERROR` tras marcar un falso
  positivo en `verivoto.js` (`nosemgrep`). No se ejecutó: SonarQube (sin acceso a Docker Hub ni a Maven Central desde la
  IA), las vulnerabilidades Maven de Trivy y los paquetes `p/…` de Semgrep (sin acceso a sus registros). Registradas
  IMP-46–IMP-49.

## Consola de gestión: barra de iconos, logo, banderas y gráfica por periodo (03/11/2026)

Rama `feat/console-rail-navigation` (`8f1bbf5`), fusionada en `dev` en `c571021`.

- Barra de iconos siempre visible para todos los roles; la hamburguesa de su parte superior la despliega con los
  nombres de las secciones y solo ese botón la vuelve a plegar (el contenido se desplaza desde 992 px; por debajo se
  superpone). Se quita el nombre del usuario del pie de la barra (ya está en la cabecera); cerrar sesión queda como icono.
- Cabecera: logo de VeriVoto (escudo del prototipo) centrado; la hamburguesa pasa a la barra.
- Selector de idioma: botones ES/EN con las banderas de España y Reino Unido, con el mismo aspecto que el login de
  Keycloak (`aria-pressed`, objetivos de 44 px).
- Participación por día (inicio y página de participación): componente compartido `app-day-chart` con escala fija de
  0 a 100 %, todos los días del periodo de votación (los futuros, vacíos) y los meses bajo el eje. Solo días, nunca
  horas (D-18).
- La medición axe de la consola de gestión con este diseño es parte de A4 (ver `docs/ai/PLAN.md`).

## Login y panel de inicio (02/11/2026)

Rama `feat/login-and-home-polish` creada desde `feat/role-home-dashboards` (sin fusionar entonces) y fusionada en `dev` con autorización del autor.

- Login (IMP-14): el patrón de líneas de `keycloak.v2` (`keycloak-bg-darken.svg`), el borde azul de la cabecera de la tarjeta y los degradados se sustituyen por un panel azul liso a la izquierda y una tarjeta blanca sobre `#f3f5f8` a la derecha (una columna por debajo de 900 px). Botón verde de 48 px, campos con un único borde de 1,5 px (contraste 3,66:1), foco de 3 px, enlaces de 44 px, sin los sellos con emojis. Textos del panel y títulos en `messages_es/en.properties`; subtítulo de la tarjeta con `js/verivoto.js`.
- Contraseña: `login-update-password.ftl` (primera contraseña, cambio y final de «He olvidado mi contraseña») lista los requisitos de `passwordPolicies` (hoy: 12 caracteres, distinta del usuario y del correo, distinta de las 3 últimas), asociados con `aria-describedby`; en vivo solo longitud y coincidencia; el resto «se comprueba al guardar».
- Panel de inicio (IMP-44): línea de rol con lo que puede hacer, nombre de la organización (`GET /organizations/{id}`), tarjetas de cifras y bloques por rol. ADMIN: procesos abiertos, votos y participación de los procesos abiertos (personas sobre el censo), avisos (correos no entregados de `/notifications`, censos vacíos, borradores, censos congelados), procesos activos con barra y gráfico de votos acumulados **por día** con tabla oculta para lectores de pantalla. Gestor de censo: cifras y avisos de censos. Auditor: última verificación de la urna, estados por proceso con huella y acciones recientes (`/admin-actions`). Superadministrador: organizaciones, cuentas y organizaciones con un solo administrador.
- No implementado por no existir en el backend: gráfico por horas (contradice D-18) e «intentos de acceso fallidos» (IMP-45). «Registro de auditoría verificado» solo lo ve el AUDITOR (el ADMIN no tiene permiso de lectura de la cadena).
- Ajuste del 03/11/2026 (rama `feat/login-language-flags`): logo del panel más grande (32 px de texto, escudo de 46 px); en móvil la banda azul solo lleva el logo y la tarjeta queda justo debajo; se quita el hueco antes del botón (campo oculto de Keycloak que sumaba dos huecos de 24 px); el selector de idioma pasa a dos botones pequeños con bandera (España y Reino Unido) y código ES/EN, enlazados a las mismas URL de Keycloak (el `select` queda como alternativa sin JS); aviso «¿Vas a votar? Usa el enlace personal…» bajo la tarjeta. axe sin incumplimientos a 1280 y 360 px en ES y EN. `feat/role-home-dashboards` borrada en local tras comprobar que está en `dev` (la remota `origin/feat/role-home-dashboards` sigue: borrarla requiere push).
- Rediseño responsive del 03/11/2026 (rama `feat/login-responsive-redesign`, a partir de «VeriVoto – Logo y prototipos»): logo del prototipo (escudo con ranura de urna, check menta; «Veri» blanco y «Voto» `#5FD3A1`), paleta `#16365C`/`#4B5563`. Escritorio (≥ 768 px, 50/50): logo, mensaje y puntos de confianza en un único bloque centrado (antes el logo quedaba separado del mensaje y el pie se montaba sobre el tercer punto), pie abajo, marca de agua al 6 % en la esquina; escalones para poca altura (≤ 760 px), 768–1023 px y ≥ 1600 px. Móvil (< 768 px, `100dvh`): banda con logo y eslogan que crece con la altura, tarjeta superpuesta 24 px, aviso al votante y franja inferior con los tres puntos en versión corta (mismo `<ul>` reordenado con `display: contents`, sin duplicar texto); safe areas con `env()`. Se anulan dos huecos de keycloak.v2 (pie vacío dentro de la tarjeta, 28 px). Revisado en el navegador con Keycloak real: sin scroll en 360×640, 375×667, 390×844, 430×932, 768×1024, 1280×650, 1366×768, 1440×900 y 1920×1080; en 844×390 (horizontal) el formulario completo cabe y solo el aviso queda bajo el pliegue. axe 4.10.2 sin incumplimientos en ES (360) y EN (1440).
- Presupuesto de estilos por componente de Angular: aviso a 8 kB y error a 12 kB (el SCSS del inicio ocupa 7,8 kB).
- Comprobaciones: `npm run test:ci` 19/19 (3 pruebas nuevas de agregados) y `npm run build` sin avisos, ejecutados en una copia Linux del frontend con Node 22.23. Login real en Keycloak a 1280 y 360 px y pantalla de contraseña nueva en ES y EN (flujo «He olvidado mi contraseña» con la cuenta de auditoría, sin guardar la contraseña): axe 4.10.2 sin incumplimientos.

## Consolidación de ramas (01/11/2026)

- DEF-04 corregido (`0733eef`): `view-realm` para la cuenta de servicio de identity-service en `verivoto-realm.json`.
- Resultados de la fase 2 de la otra IA confirmados con notas de revisión: DEF-03 descartado (se probó el frontend antiguo
  que seguía en el puerto 4200), repetición de axe no válida, entorno original `verivoto` incoherente con el `.env` actual.
- `scripts/demo/seed-ual.mjs`: organización UAL, cuentas de gestión y elecciones pasada, en curso y futura (solo local).
- `fix/keycloak-identity-view-realm`, `test/p7-phase2-results`, `test/p7-phase2` y `test/p7-pending-suite` integradas en `dev`
  y borradas; comprobado que `dev` contiene sus commits y no pierde ningún fichero respecto a ellas.

## P7.7 · Encargo de pruebas (31/10/2026)

Trabajo realizado en worktree separada `saas-digital-voting-p7`, rama `test/p7-pending-suite`, desde `cc0229b`. Revisado por el autor y confirmado en `b8c9038`, `597a15d` y `fbdd458`; integrado en `dev` (`5acf2d8`). Se preservaron los cambios locales de la worktree original `docs/p7-test-handoff`.

- Verificación Maven final con Java 25.0.5: BUILD SUCCESS, 60 pruebas backend (gateway 1, identity 12, election 19, voter-access 9, ballot-box 19), 0 fallos/errores/omitidas. CSV JaCoCo comparables y resumen P7 en `docs/evaluation/resultados-p7/`.
- Añadidas pruebas sintéticas de lectura/importación CSV, autorización HTTP parcial, inspección de columnas de todas las tablas públicas de ballot-box y dos casos de integridad de cadena. No se modificó código de producción.
- Frontend: 10 Vitest pasan y build compila. Cuatro ejecuciones Playwright/axe de la pantalla inicial de recibo detectan incumplimientos críticos/serios; se registra la incidencia DEF-01 y H1a queda PENDIENTE. No equivale a auditar todos los estados/pantallas ni el stack real.
- Pendiente: matriz completa de autorización, concurrencia/outbox/repositorios y unidades enumeradas en el encargo; recorrido E2E completo y logs con servicios activos; k6 no instalado, por lo que no se hicieron las tres cargas ni se evalúa H1c. Solo datos sintéticos; ningún token/OTP real.
- DEF-02 reclasificado por el autor: el enlace de la invitación va en la ruta por diseño (D-33); código, tokens y recibo nunca en URL ni almacenamiento. IMP-43 queda como mejora P9.

Evidencias y limitaciones: `docs/evaluation/resultados-p7/`. No cerrar P7 hasta completar los criterios pendientes y resolver las decisiones registradas.
