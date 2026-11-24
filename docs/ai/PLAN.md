# PLAN · Lo que falta hasta la entrega

Creado: 03/11/2026 · Entrega de la memoria: 27/01/2027 · Base: `PROGRESS.md`, `TRACEABILITY.md`, `IMPROVEMENTS.md`
y las marcas `[PENDIENTE]` de la memoria.

**Cómo usarlo.** Marca `[x]` al terminar una tarea y anota la fecha o el commit. Para retomar, pide a la IA
«revisa `docs/ai/PLAN.md` y dime qué falta»: debe contrastar cada tarea con el repositorio y actualizar este fichero.
Cuando una pregunta de la sección 1 tenga respuesta, se escribe la respuesta y, si cierra una decisión, se pasa a
`DECISIONS.md`.

**Quién:** `IA` la puede hacer sola · `Autor` solo la puedes hacer tú · `Ambos` la IA prepara y tú ejecutas o validas.

## Estado general

| Bloque | Estado |
| --- | --- |
| Desarrollo del MVP (P0–P6, RF-01 a RF-21) | Hecho. Única excepción: RF-05a parcial (falta un IdP externo de prueba) |
| Pruebas y medición (P7) | Cerrado el 15/11/2026. Cifras actuales: 175 pruebas backend (22/11), 259 unitarias frontend y 100 E2E de accesibilidad (22/11); cobertura de líneas 45–90 % por servicio en backend (59,7 % en conjunto) y 87,8 % en frontend; RNF-4 aceptado (13/11, 5 de 5 sin formación previa, máximo 4 min); H1a aceptada (22/11, revisión manual con Narrador); H1b aceptada (09/11, 5 de 5 y 5 de 5); H1c rechazada en las series A (11/11), B y C (19/11) |
| Memoria (P8) | En curso: C1 actualizado para H1a, H1b y H1c el 22/11; memoria recompilada (97 páginas) y presentación recompilada (20 diapositivas); falta revisión editorial y visual C4 |
| Publicación | PR #4 fusionado en `main` el 15/11/2026 (`7ee3a57`), con `build` y `security` en verde. Anteproyecto firmado por el director (comunicado por el autor el 15/11/2026) |

---

## 1. Preguntas para el autor (responder primero: desbloquean tareas)

| ID | Pregunta | Desbloquea | Respuesta |
| --- | --- | --- | --- |
| Q-01 | ¿RF-11 (revoto) queda como «Could» o pasa a «Won't» en la tabla 4.3? | C3 | **Respondida (08/11, D-56):** pasa a *Won't* (fuera de esta versión), con nota de que el identificador se conserva por trazabilidad con la especificación v1.0. Aplicado en la tabla 4.3 y en RF-11 de la especificación |
| Q-02 | ¿«Sin ayuda» en RNF-4 excluye también consultar la documentación? (marcador `[PENDIENTE DEL AUTOR]` en `especificacion_detallada.tex`) | C3, B4 | **Respondida (05/11, D-55):** «sin ayuda» = sin intervención de personas; consultar la documentación sí está permitido. Marcador de `especificacion_detallada.tex` resuelto |
| Q-03 | ¿Se anotan RF-22 y RF-23 como fuera del MVP en la tabla 1.1? | C3 | **Respondida (08/11, D-56):** sí; anotados en sus filas (OE3 y OE4) y con una nota bajo la tabla: fuera del MVP, trabajo futuro (P9), sin evaluar en esta entrega |
| Q-04 | ¿En P9 se depura el modelo de datos de sustitución del voto (`votes.status` `SUBSTITUTED`, RI-6/RI-8/RI-9) o se deja descrito? | C3 | **Respondida (08/11, D-56):** se deja descrito, no se depura; se marca como no operativo en el MVP (la urna registra un único voto por token y nunca genera «Sustituido»). Aplicado en la especificación (nota de alcance y RI-6) y en las conclusiones |
| Q-05 | ¿Se hace la sesión RNF-4 con cinco administradores o se declara pendiente en la memoria? | B4 | **Respondida (03/11):** la sesión con administradores ya se hizo. Falta pasar los datos: ver Q-11 |
| Q-06 | ¿Fecha para el ensayo y para las cinco sesiones con votantes (H1b)? ¿Quiénes participan (sin datos reales)? | B2 | **Respondida (05/11):** ensayo el domingo 08/11/2026 y cinco sesiones el lunes 09/11/2026, con el escenario sintético `USAB`, solo si el sábado 07/11 se cumplen tres condiciones: `seed-usab.mjs` ha funcionado con el stack real y existe `.local/sesion-usab.md`; el consentimiento tiene los datos institucionales y el contacto; hay cinco participantes y dos reservas confirmados. Si falla alguna, el autor propone ensayo el 15/11 y sesiones el 16/11/2026 (fechas propuestas, no definitivas). Estado el 05/11: **primera condición cumplida** (A11); faltan las otras dos. Los procesos #4 y #5 cierran el 12/11/2026 a las 12:04, así que con las fechas alternativas haría falta preparar un escenario nuevo |
| Q-07 | ¿Se prueba RF-05a con un IdP externo (p. ej. un segundo realm de Keycloak como «universidad») o se deja como limitación? | A9 | **Respondida (03/11):** sí, con las cuentas `@inlumine.ual.es` de 4 compañeros (Google Workspace). Plan en la sección 2b; falta Q-12 |
| Q-08 | ¿Qué mejoras de `IMPROVEMENTS.md` se aprueban antes de la entrega? Propuesta en la sección 4 | D1–D5 | **Respondida (04/11, D-52):** se aprueban D1, D2 y D5 ya, D3 con el despliegue y D4 si queda tiempo |
| Q-09 | ¿Cuándo se fusiona `dev` en `main` (al cerrar P7, o antes para tener una versión estable)? | E2 | **Respondida (03/11):** en fin de semana (sábado), después de implantar SonarQube y un análisis de seguridad gratuito similar a Fortify (sección 2c) |
| Q-10 | ¿Qué fecha tiene la próxima revisión con el tutor y qué quiere ver? (marca el orden de C1–C4) | C | **En espera (03/11):** se le envió la memoria actualizada y aún no ha respondido. Las indicaciones de la reunión del 21/10/2026 ya están aplicadas (sección 7) |
| Q-11 | Resultados de la sesión RNF-4: para cada administrador (A1–A5), ¿terminó crear, configurar y programar un proceso de opción única?, ¿cuánto tardó?, ¿necesitó ayuda (y de qué tipo)? Fecha de la sesión y si firmaron consentimiento | C1 | **Respondida (05/11):** sesión el 03/11/2026, 5 participantes (P01–P05, anónimos), consentimiento firmado, 5 de 5 sin ayuda de personas (D-55). Tiempos **estimados** tras la sesión, no medidos: 5, 5, 11, 3,4 y 3,4 min (uno corregido de 6 a 11 min), sin asignar a nadie. **Solo 2 de 5 sin formación previa.** RNF-4 **no demostrado**; evidencia indicativa (`docs/evaluation/usabilidad/resultados-rnf4-administradores.md`) |
| Q-12 | SSO con cuentas de la UAL: ¿los 4 compañeros aceptan por escrito que se usen sus correos solo para la prueba, en local y borrándolos después? ¿Tienes una cuenta de Google personal para crear el proyecto de Google Cloud? | A9 | **Respondida (05/11, D-54):** cuenta de Google Cloud creada. Los 4 compañeros dieron su aceptación **verbal** (uso de sus correos solo en la prueba, en local y borrado después). **No hay constancia escrita** y el autor no la pedirá por ahora: se registra como limitación, no como consentimiento documentado |
| Q-13 | Prioridades MoSCoW de los RNF (tabla 7.1): la memoria propone Must para RNF-1, 3, 6, 7, 11–16 y Should para RNF-2, 4, 5, 8–10 y 17 (RNF-18 sigue Could), marcadas con asterisco. ¿Las confirma el tutor? | Tabla 7.1 sin asteriscos | **Pendiente (17/11/2026):** propuesta aplicada en la memoria; se pregunta al tutor en el próximo correo o reunión |

---

## 2. Se puede empezar ya (sin esperar respuestas)

Orden recomendado: de arriba abajo. Semana del 03/11 al 16/11/2026.

- [x] **A1 · Publicar lo que hay** (`Autor`): `git push origin dev`. **Hecho:** `origin/dev` coincide con `dev` (`06f39be`, 08/11/2026). Opcional: borrar ramas locales ya fusionadas
  (`feat/login-and-home-polish`, `feat/login-language-flags`, `feat/login-responsive-redesign`,
  `fix/login-mobile-spacing`, `feat/console-rail-navigation`) y la remota `origin/feat/role-home-dashboards`.
- [x] **A2 · Poner al día `PROGRESS.md`** (`IA`, 08/11/2026): entrada de la consola del 03/11 (barra de iconos, logo, banderas,
  gráfica por periodo) y quitar la marca suelta `PENDIENTE_PANEL`.
- [x] **A3 · Demo UAL y E2E** (`Ambos`): arrancar el stack, `node scripts/demo/seed-ual.mjs` (corregido, sin repetir) **Hecho (15/11/2026: demo UAL cargada y usada para las capturas de la memoria; E2E del 13/11, 200 de 200 tras IMP-62)**
  y `npm run test:e2e` contra el frontend de la rama actual (no otro servidor en el puerto 4200, ver DEF-03).
- [x] **A4 · axe en todas las pantallas** (`Ambos`, parte automática de H1a): login, inicio de cada rol, censos, **Hecho (08/11/2026; repetido el 13/11: 200 comprobaciones sin críticos ni serios)**
  procesos (A3/A4), participación, resultados, auditoría, trazabilidad, privacidad, claves de API, flujo del votante
  (V1–V8, B1–B5) en 1280 y 360 px, ES y EN. Criterio: cero incumplimientos críticos ni serios. El selector de idioma
  con banderas es nuevo (sustituye al que dio DEF-01): repetir también `/recibo/:id`. **En curso (05/11):** flujo del
  votante medido (`voter-flow.accessibility.spec.ts`, 88 auditorías): 8 críticos de DEF-05 en la papeleta; DEF-05
  corregido (aprobado por el autor) y nuevo barrido con 0 críticos y 0 serios
  (`docs/evaluation/accesibilidad-axe-2026-11-05.md`). DEF-06 (error de formato del recibo no visible) corregido y
  tercer barrido con 0 críticos y 0 serios; su prueba pasa en verde. La prueba
  `receipt-check.accessibility.spec.ts` se ha adaptado al selector de idioma con banderas y pasa. Faltan la consola de
  gestión y la página pública de resultados. **08/11/2026:** consola de gestión (todas las páginas y roles) y
  resultados públicos medidos con `admin-console.accessibility.spec.ts` (sin commit), con Keycloak y API simulados:
  27 pantallas o estados, 108 auditorías, 0 críticos y 3 serios, todos de **DEF-07** (tablas que desbordan a 360 px
  sin acceso con teclado; sin corregir, necesita aprobación). Resultados públicos (V8) y 404: 0. Informe:
  `docs/evaluation/accesibilidad-axe-consola-2026-11-08.md`. **DEF-07 corregido el mismo día** (opción A, aprobada por
  el autor): barrido 2 con 0 críticos y 0 serios, 108 de 108 sin marcas. A4 completa; no cambia H1a (flujo del votante).
- [x] **A5 · Pruebas unitarias P7.1** (`IA`): censo/CSV, claims institucionales (RF-05b), privacidad (RF-18), **Hecho (16/11/2026: flujo del votante cubierto con `voter-flow.page.spec.ts`; lo que quede sin prueba figura como limitación en `resultados-p7/cierre.md`)**
  recordatorios (RF-17), errores y reglas de dominio sin prueba; frontend: gráfica por día (`app-day-chart`),
  agregados del inicio y flujo visual del votante. **Parcial (08/11/2026, sin commit):** frontend, gráfica por día
  (`day-chart.component.spec.ts`, 9 pruebas; Vitest 28 de 28). Backend escrito (08/11/2026, sin commit): `CensusServiceTest`,
  `PrivacyServiceTest` (election) y `JwtVoterIdentityVerifierTest`, `VoterAccessServiceSsoTest`, `NotificationAdminServiceTest`,
  `PersonalDataServiceTest` (voter-access), 38 pruebas que pasan. **Verificadas por el autor (08/11/2026):** `./mvnw verify` en Windows con Java 25.0.5 y Testcontainers (PostgreSQL 18.6): BUILD SUCCESS en 5:27 min; election 36, voter-access 38 y ballot-box 22 pruebas, 0 fallos.
- [x] **A6 · Integración P7.2/P7.3** (`IA`; **escrita 08/11/2026, sin commit**: `ElectionOrganizationIsolationIntegrationTest`, `ProcessSyncOutboxIntegrationTest`, `VoterAccessScenariosIntegrationTest`, `BallotBoxScenariosIntegrationTest`, 13 pruebas que pasan; alcance acordado: solo el cruce entre organizaciones, no la matriz de roles; se añaden `spring-boot-starter-webmvc-test` y `spring-security-test` como dependencias de prueba en voter-access y ballot-box. **Verificadas por el autor (08/11/2026):** `./mvnw verify` en Windows con Java 25.0.5 y Testcontainers (PostgreSQL 18.6): BUILD SUCCESS en 5:27 min; election 36, voter-access 38 y ballot-box 22 pruebas, 0 fallos.): matriz completa de autorización HTTP entre organizaciones y roles, **Hecho (08/11/2026; en `dev` y en las 135 pruebas del 14/11)**
  concurrencia del token de voto (un solo voto con N envíos), reintentos e idempotencia del *outbox*, inspección del
  esquema y de los logs para el secreto del voto (RNF-11), todas las manipulaciones de la cadena.
- [x] **A7 · Carga H1c** (`Ambos`): instalar k6, completar escenarios de acceso y escrutinio, credenciales sintéticas **Hecho (11/11/2026: tres ejecuciones; H1c rechazada, véase `carga.md`)**
  fuera del repo, tres ejecuciones de 500 VU con p95, errores y recursos; comprobar la cadena y la participación después.
  **En curso (05/11):** `scripts/load/prepare-voting-tokens.mjs` prepara cada ejecución (sin probar con el stack real).
  Solo después de las sesiones H1b (Mailpit guarda 500 mensajes). Falta instalar k6 y ejecutar. Variables y
  definición de ejecución válida en `docs/evaluation/carga.md`. El autor ejecuta; la IA analiza y cierra H1c.
- [x] **A8 · Gantt de la memoria** (`IA`, IMP-02): la iteración 3 aún dice «re-voto». **Hecho:** `plan_gantt.tex` ya dice «Iteración 3. Emisión y recibo (RF-09--RF-10)» (comprobado el 08/11/2026).
- [ ] **A9 · RF-05a** (`IA`, según Q-07): **Bloqueada (20/11/2026), trabajo futuro (P9).** No es posible probarla en esta fase por falta de acceso a las cuentas `@inlumine.ual.es` y a la configuración institucional. Véase la sección 9.
- [x] **A10 · Job E2E en CI** (`IA`): Playwright + axe en GitHub Actions (al menos con servicios simulados). **Hecho (15/11/2026: el job de accesibilidad E2E pasa en GitHub en el PR #4 y en `main`)**
  **Borrador (08/11/2026, sin commit, pendiente de revisión del autor):** job `e2e-a11y` en `build.yml` (`ubuntu-latest`,
  Node 24, `npx playwright install --with-deps chromium`, `npm run test:e2e`, artefacto `e2e-a11y-results`). Sin
  secretos. Probado en el entorno de la IA con `CI=1`: 200 de 200 en 10,3 min; falta verlo en GitHub tras el push.
- [x] **A11 · Elección y votantes para la sesión H1b** (`IA` prepara, `Autor` ejecuta): hecho el 04/11/2026
  (`d5608f9`): `node scripts/demo/seed-usab.mjs` monta el escenario `USAB` de `preparacion-sesion.md` (censo de 7
  entradas `p01…p05@verivoto.test` + `r1`, `r2`, papeleta neutra, proceso abierto 7 días) y un proceso de ensayo
  aparte; deja la hoja de la persona moderadora en `.local/sesion-usab.md` con el buzón de Mailpit de cada entrada.
  Probado contra un entorno simulado; **falta que el autor lo ejecute con el stack real** antes del ensayo (B2).
  **05/11: la primera ejecución real falló** (503 al crear la organización: Keycloak rechaza los paréntesis en el nombre
  de las cuentas de gestión). Nombres corregidos en el script. **Segundo intento (05/11): falló al restablecer la contraseña
  de `superadmin`** (`invalidPasswordHistoryMessage`: la política del realm no admite ninguna de las 3 últimas
  contraseñas y el script vuelve a fijar `DEMO_PASSWORD` en cada ejecución). Hay que repetir con una contraseña nueva
  (IMP-54). **Tercer intento (05/11, 12:04): terminó bien** con una contraseña nueva: organización `USAB`, dos cuentas
  de gestión, censo de 7 entradas, censo de ensayo de 2, procesos #4 (sesión) y #5 (ensayo) abiertos hasta el 12/11/2026
  a las 12:04, 9 invitaciones y `.local/sesion-usab.md` generado. **Escenario `USAB` preparado.**
- [x] **A12 · Registrar la sesión RNF-4** (`IA`, tras Q-11): hoja de resultados de administradores en **Hecho (13/11/2026: sesiones online P01–P05; RNF-4 aceptado)**
  `docs/evaluation/usabilidad/`, resultado como cantidades (x de 5) y su paso a la memoria (C1). **En curso (05/11):**
  `resultados-rnf4-administradores.md`: 5 de 5 sin ayuda de personas, 4 de 5 con tiempo estimado ≤ 10 min y solo 2 de 5
  sin formación previa. RNF-4 **no demostrado**, evidencia indicativa; ya pasado a la memoria (parte RNF-4 de C1). Solo
  se cerraría con una nueva sesión con administradores sin formación previa y tiempos cronometrados.

## 2b. SSO con cuentas de la universidad (A9, RF-05a y RF-05b)

Las cuentas `@inlumine.ual.es` de estudiantes son de Google Workspace (página del STIC de la UAL), así que se puede
federar con Google sin pedir nada a informática:

1. Crear un proyecto en Google Cloud con una cuenta personal y un cliente OAuth 2.0 «Aplicación web» con la URI de
   redirección de Keycloak (`http://localhost:8080/realms/verivoto/broker/google/endpoint`). Pantalla de consentimiento
   en modo «Prueba» con los 4 correos como usuarios de prueba (solo ellos pueden entrar).
2. En el realm `verivoto`, añadir Google como proveedor de identidad con «Hosted domain» `inlumine.ual.es` (solo se
   aceptan cuentas de ese dominio) y sin crear cuentas automáticamente para gestión.
3. **RF-05a (gestión):** dar de alta a un compañero como usuario de gestión de la organización UAL con su correo y
   asociar el proveedor (`identity-provider-redirector` por organización); entra con su cuenta de la UAL.
4. **RF-05b (votante):** proceso con «cuenta institucional» y un censo con los 4 correos; cada uno abre su enlace,
   entra con Google y vota. Hoy RF-05b solo está probado con Keycloak simulado: esto da la prueba con un IdP real.
5. Riesgos: el administrador de Google Workspace de la UAL puede bloquear aplicaciones de terceros (probar primero
   con tu propia cuenta `@inlumine.ual.es`); los correos son datos personales reales: consentimiento, solo en local,
   nunca en el repositorio ni en capturas, y borrado al terminar (dejarlo anotado como limitación en la memoria).
   Situación (05/11, D-54): aceptación verbal de los 4 compañeros, sin constancia escrita; se anota como limitación.

## 2c. Calidad y seguridad del código antes de pasar a `main` (Q-09)

Herramientas gratuitas como alternativa a Fortify:

Guía, versiones y línea base: `docs/architecture/quality-and-security.md` (D-51).

- [x] **F1 · SonarQube Community Build** (`IA`, 04/11/2026): `infra/compose.quality.yaml` (aparte del stack),
  `sonar-maven-plugin` fijado en `backend/pom.xml`, `frontend/sonar-project.properties` y
  `scripts/quality/sonar.ps1`. **Falta la primera ejecución del autor** (token de SonarQube) y revisar su *quality
  gate*. Sin cobertura del frontend hasta IMP-49.
- [x] **F2 · SAST** (`IA`, 04/11/2026): Semgrep CE 1.183.0 en `.github/workflows/security.yml` (bloquea con `ERROR`).
- [x] **F3 · Dependencias y configuración** (`IA`, 04/11/2026): Trivy 0.76.1 (Maven, npm, Dockerfile; bloquea
  `HIGH`/`CRITICAL` corregibles) y `npm audit --omit=dev`. Trivy sustituye a OWASP Dependency-Check, que necesita una
  clave de la NVD.
- [x] **F4 · Secretos** (`IA`, IMP-09, 04/11/2026): gitleaks 8.30.1 sobre todo el historial en el CI y
  `.pre-commit-config.yaml`. Línea base: 0 secretos.
- [x] **F5 · Corregir lo que salga** (`IA`): mirar el primer resultado del workflow `security` en GitHub (las **Hecho (14/11/2026: los dos *quality gates* en «Passed», 0 incidencias abiertas, `security` en verde el 13/11; IMP-46 e IMP-48 siguen pendientes de aprobación)**
  vulnerabilidades de Maven solo se ven allí) y el *quality gate* de SonarQube; pendientes de la línea base: IMP-46
  (Actions de `build.yml` por SHA), IMP-47, IMP-48 e IMP-49 (necesitan aprobación). Anotar los resultados en la memoria.
  **En curso (05/11):** SonarQube, según la captura del autor: los dos *quality gates* en «Passed»; backend con seguridad
  D (6 incidencias), fiabilidad C (5), mantenibilidad A (6) y cobertura 18,8 %; frontend con seguridad A (0), fiabilidad D
  (12), mantenibilidad A (1) y cobertura 0,0 %. Clasificación pendiente hasta exportar la lista de incidencias. Workflow
  `build`: todo en verde y Vitest 19 de 19 (captura). Workflow `security`: sin verificar (solo comunicación del autor);
  faltan `semgrep-report` y `trivy-report`. Detalle y pasos de exportación en `docs/architecture/quality-and-security.md`.
  **13/11/2026:** workflow `security` de `dev` en verde (run `43066068128`); `npm audit` 0; Semgrep local 0 ERROR y sin
  hallazgos nuevos (IMP-46 e IMP-48 ya registradas; resto mitigado o falso positivo). Falta SonarQube, tras las sesiones RNF-4.
- [x] **F6 · Primer `dev` → `main`** (`Autor`, sábado tras F1–F5). **Hecho (15/11/2026: PR #4 fusionado en `main`, `7ee3a57`; `build` y `security` en verde)**

## 3. Necesitan al autor

- [ ] **B1 · Responder las preguntas Q-01 a Q-13** (`Autor`). Respondidas Q-01 a Q-09, Q-11 y Q-12; abiertas Q-10 (próxima reunión con el tutor) y Q-13 (prioridades de los RNF), comprobado el 20/11/2026.
- [x] **B2 · Sesión H1b con cinco votantes** (`Autor`, prioritaria): **Hecho (09/11/2026): H1b aceptada**, véase `hoja-registro.md`. montar el escenario `USAB`
  (`docs/evaluation/usabilidad/preparacion-sesion.md`, `censo-sesion.csv`), hacer un ensayo y las cinco sesiones;
  anotar T0/T1/T2 y resultados como cantidades (x de 5) en la hoja de registro (preparadas el 05/11:
  `hoja-registro-h1b.csv` y `scripts/evaluation/h1b-summary.mjs` para el resumen y la decisión del mismo día 09). Criterio: 5 de 5 en ≤ 3 min y al
  menos 4 de 5 interpretan la verificación.
- [x] **B3 · Revisión manual de accesibilidad** (`Autor`, parte manual de H1a): teclado y lector de pantalla
  (NVDA o Narrador) en acceso, voto y recibo siguiendo `docs/evaluation/accesibilidad-manual.md`.
  **Hecho (22/11/2026):** revisión con el Narrador de Windows tras adaptar el guion; 39 comprobaciones obligatorias,
  0 bloqueos y 2 incidencias documentadas. H1a aceptada tras corregir y volver a medir el contraste de foco.
- [x] **B4 · RNF-4 con administradores** (`Autor`): sesión realizada (comunicado el 03/11/2026). Falta registrar
  los datos (Q-11 → A12; hecho el 05/11) y fijar la definición de «sin ayuda» (Q-02; hecho, D-55).

## 4. Mejoras opcionales (solo si se aprueban en Q-08)

**Aprobado por el autor el 04/11/2026 (Q-08, D-52).** Propuesta de la IA (03/11): hacer ya D1, D2 y D5 (bajo coste y mejoran accesibilidad y seguridad), D3 al preparar
el despliegue y D4 si sobra tiempo. Las que más aportan antes de entregar, de más a menos:

- [x] **D1 · IMP-15**: aviso 2 minutos antes de que caduque la sesión de gestión (WCAG 2.2.1). **Hecho (13/11/2026: aviso de caducidad de sesión aplicado y con pruebas)**
- [x] **D2 · IMP-21**: límite de peticiones por IP en el *gateway* para `/api/v1/voter-access/**` (RNF-14). **En curso **Hecho (15/11/2026: fusionada en `dev`, `c48dbab`; `mvnw verify` del autor con 151 pruebas y 0 fallos)**
  (05/11):** commit `4ec0efc` en la rama `feat/voter-access-limit-jwt-audience`, sin fusionar; faltan las pruebas del autor.
- [ ] **D3 · IMP-32**: cabeceras CSP, HSTS y `Referrer-Policy` en el despliegue del frontal. Solo si hay despliegue: el
  TFM se ejecuta y evalúa en local y no está previsto desplegarlo (05/11, D-53).
- [x] **D4 · IMP-11 / IMP-40**: al suspender una organización, deshabilitar sus usuarios y claves de API en Keycloak. **Hecho (16/11/2026, sin commit): IMP-11 e IMP-40 (cuentas y claves); pendiente del `mvnw verify` del autor**
- [x] **D5 · IMP-27**: comprobar la audiencia del JWT en cada servicio y quitar `verivoto-dev-swagger` en producción **Hecho (15/11/2026: fusionada en `dev`, `c48dbab`; `mvnw verify` del autor con 151 pruebas y 0 fallos)**
  (esta segunda parte solo aplica si hay despliegue, D-53). **En curso (05/11):** commit `72bdfba` en la misma rama que D2, sin fusionar;
  faltan las pruebas del autor y actualizar el realm local (`scripts/dev/keycloak-api-audience.mjs`).

El resto de `IMPROVEMENTS.md` queda para P9 salvo que se decida otra cosa.

## 5. Memoria (P8)

- [x] **C1 · Resultados de la evaluación** (`IA`, tras A4, A7, B2, B3): completar `docs/evaluation/informe.md`, la tabla **Hecho (13/11/2026: H1a sin decisión, H1b aceptada, H1c rechazada y RNF-4 aceptado, aplicados en la memoria)**
  13.1 (`evaluacion_resultados.tex`), la lista `[PENDIENTE]` de `contenido1.tex` y las conclusiones. Lo que no se
  mida se deja como PENDIENTE y limitación, sin inventar resultados. **En curso (05/11):** parte de RNF-4 hecha (no
  demostrado); H1a: parte automática favorable tras corregir DEF-05 y DEF-06, parte manual no realizada y H1a sin decisión (08/11, aplicado en la memoria); H1b aceptada y aplicada en la memoria (10/11); H1c rechazada y aplicada en la memoria (11/11); antes: H1c sin
  datos. C2 y C3 no se adelantan hasta tener H1a, H1b y H1c medidas o declaradas limitación.
- [x] **C2 · Alinear con lo implementado** (`IA`): figuras de arquitectura (objetivo frente a implementado), **Hecho (15/11/2026: capturas de la consola añadidas en `implementacion.tex`; figuras 5.1 y 5.2 redistribuidas; cifras de pruebas y cobertura del 14–15/11)**
  diagramas conceptuales y texto heredado; capturas seguras de las pantallas (datos sintéticos de la demo UAL). **En curso (13/11/2026):** figura de arquitectura implementada, texto heredado, RF-03 (Excel), figura de secuencia y cifras de pruebas hechos, sin commit; faltan las capturas.
- [x] **C3 · Aplicar las decisiones Q-01 a Q-04** (`IA`) en la especificación y las tablas 1.1 y 4.3. **Hecho (08/11/2026, D-56)**, adelantado a petición del autor (el plan decía esperar a H1a, H1b y H1c): `contenido1.tex` (tablas 1.1 y 4.3), `especificacion_detallada.tex` (nota de alcance, RF-11 y RI-6) y `conclusiones.tex` (P9).
- [ ] **C4 · Revisión editorial y visual final** (`Ambos`, P8.4): compilar el PDF, revisar página a página,
  avisos de maquetación de portada y contraportada; el autor da el visto bueno. **Primera pasada (23/11/2026):**
  `Plantilla-TFM-Informatica-UAL/revision-c4-2026-11-23.md` (3 correcciones hechas; contraportada y tablas de CU partidas, pendientes del autor).

## 6. Cierre

- [x] **E1 · Cerrar P7** (`IA`): cuando A3–A7 y B2–B4 estén hechos o declarados pendientes, actualizar `PROGRESS.md`, **Hecho (15/11/2026: `TRACEABILITY.md`, `PROGRESS.md` y `resultados-p7/cierre.md`)**
  `TRACEABILITY.md` (columna de evidencia) y `docs/evaluation/resultados-p7/`.
- [ ] **E2 · `dev` → `main`** (`Autor`, según Q-09) y etiqueta de la versión entregada.
- [ ] **E3 · Entrega** (`Autor`): PDF final y anexos.

## 7. Indicaciones del tutor (reunión del 21/10/2026)

Todas aplicadas en la memoria (ver `Plantilla-TFM-Informatica-UAL/cambios_para_javi.md`):

- [x] Casos de uso: actor genérico del que heredan los roles; la funcionalidad común va en el actor genérico (varios
  actores apuntando a un caso de uso significaría que deben estar todos a la vez).
- [x] Inicio de sesión genérico en los casos de uso; el SSO institucional es una decisión de diseño, no un requisito.
- [x] Requisitos no funcionales encajados en ISO/IEC 25010 (categoría y subcategoría); autenticación en seguridad
  (autenticidad).
- [x] RGPD como requisito no funcional.
- [x] Fortalezas y debilidades de la evaluación como amenazas a la validez y limitaciones.
- [x] Plan de trabajo con diagrama de Gantt, tareas y fechas (`plan_gantt.tex`) y seguimiento previsto frente a real.
- [x] Empezar la implementación sin esperar validación de los módulos.
- [ ] Próxima reunión: pendiente de que el tutor responda (Q-10).

## 8. Objetivos del anteproyecto y RNF (17/11/2026)

Criterio acordado con el autor: **primero se cumplen los objetivos del anteproyecto firmado** (OE1–OE5, cada uno con
su evidencia); lo que no exige el anteproyecto se trata como mejora y se hace solo si queda tiempo.

### 8.1 Objetivos del anteproyecto

| OE | Evidencia que pide el anteproyecto | Estado |
| --- | --- | --- |
| OE1 | Pruebas de autorización por rol y de aislamiento entre organizaciones | Cumplido: `IdentityAuthorizationIntegrationTest` (17/11) y pruebas de aislamiento de election, voter-access y ballot-box |
| OE2 | Pruebas de las transiciones de estado y contrato OpenAPI publicado | Cumplido: `ProcessStatusTest`, `VotingProcessTest`; contratos en `openapi/` de cada servicio y Swagger UI |
| OE3 | Unicidad del token de emisión y ausencia de vínculos entre identidad y voto | Cumplido: 20 envíos simultáneos dan 1 voto; esquema y registros sin vínculo (voter-access y ballot-box) |
| OE4 | Recálculo independiente del resultado y verificación de la cadena de huellas | Cumplido: recálculo en el navegador (D-37), `ChainVerifierTest`, `TallyResultTest` |
| OE5 | Informe de evaluación por hipótesis, con los criterios no alcanzados | Cumplido: capítulo 13 e `informe.md` (H1a, H1b y RNF-4 aceptados; H1c rechazada) |

La memoria solo tenía OE1–OE4; el 17/11 se añadió OE5 (objetivos, tabla 1.1 y conclusiones) para que coincida con el
anteproyecto.

### 8.2 RNF: evidencia actual y qué falta

| RNF | Prioridad propuesta | Evidencia actual | ¿Lo exige el anteproyecto? | Acción |
| --- | --- | --- | --- | --- |
| RNF-1 Accesibilidad | Must | axe: 212 comprobaciones sin críticos ni serios; revisión manual no hecha | Sí (auditoría WCAG): cumplido con la parte automática | Mejora: revisión manual con Narrador de Windows (autor, 1–2 h). Si se hace, H1a puede decidirse |
| RNF-2 Adaptable y navegadores | Should | Barridos a 360 y 1280 px; solo Chrome | No | Mejora: E2E en Firefox y WebKit (IA prepara, autor ejecuta) |
| RNF-3 Uso del votante | Must | H1b aceptada | Sí | — |
| RNF-4 Uso del administrador | Should | Aceptado (13/11) | No (se hizo igualmente) | — |
| RNF-5 Idiomas | Should | Barridos en es y en; textos es/en | No | Citar como evidencia en el capítulo 13 |
| RNF-6 Claridad de la verificación | Must | H1b: 5 de 5 | Sí | — |
| RNF-7 Prevención de errores | Must | Pantalla de revisión; pruebas «no vota sin selección» y error de papeleta | Indirectamente (OE3) | Citar como evidencia en el capítulo 13 |
| RNF-8, 9, 10 Mensajes, orientación, consistencia | Should | Indicador de pasos y estados probados; revisión heurística (2 evaluadores) no hecha | No | Mejora: revisión heurística de Nielsen |
| RNF-11 Secreto | Must | Esquema y registros revisados; pruebas de integración | Sí (OE3) | — |
| RNF-12 Unicidad | Must | 20 envíos simultáneos dan 1 voto | Sí (OE3) | — |
| RNF-13 Integridad y trazabilidad | Must | Cadena de huellas, trigger solo añadir, escrutinio bloqueado si se rompe | Sí (OE4) | — |
| RNF-14 Seguridad de acceso | Must | Límites de código y bloqueo, límite por IP, audiencia JWT, rutas internas, suspensión | Indirectamente (OE1) | Citar como evidencia en el capítulo 13 |
| RNF-15 Aislamiento | Must | Pruebas HTTP con dos organizaciones en los cuatro servicios | Sí (OE1) | — |
| RNF-16 Protección de datos | Must | `PrivacyServiceTest`, `PersonalDataServiceTest` (búsqueda, exportación, borrado o anonimización) | No | Citar como evidencia en el capítulo 13 |
| RNF-17 Rendimiento | Should | H1c rechazada y documentada | Sí (prueba de carga): cumplido al documentarla | Mejora (P9): perfilar, optimizar y repetir |
| RNF-18 PWA instalable | Could | Manifiesto y *service worker*; instalación no probada | No | Mejora: instalar en Android e iOS |

### 8.3 Orden de trabajo hasta la entrega (27/01/2027)

1. [x] Tabla 7.1 con prioridades propuestas y nota; OE5 en la memoria (IA, 17/11).
2. [ ] Preguntar Q-13 al tutor (autor): pregunta 3 de `docs/tutor/preguntas-reunion.md`, para la reunión.
3. [x] (18/11, tabla 13.2) Tabla «RNF → evidencia» en el capítulo 13 con lo de la columna «Evidencia actual» (IA, una sesión). Cierra la duda
   del tribunal sobre qué RNF tienen evidencia.
4. [ ] Revisión C4 final y ensayos de la defensa (ambos).
5. Mejoras, solo si sobra tiempo y por este orden: revisión manual de accesibilidad (RNF-1), E2E en Firefox y WebKit
   (RNF-2), revisión heurística (RNF-8–10), instalación de la PWA (RNF-18), optimización de H1c (RNF-17), A9 (RF-05a).

### 8.4 Plan de las mejoras (punto 5)

Cada mejora sube el «Estado» de uno o varios RNF en la tabla 13.2. Ninguna es necesaria para los objetivos del
anteproyecto. Orden: de más valor y menos coste a menos valor y más coste.

| Id | RNF | Qué se hace | Quién y cuánto | Preparado | Resultado en la memoria |
| --- | --- | --- | --- | --- | --- |
| M1 | RNF-1 | Revisión manual con teclado y lector de pantalla (`accesibilidad-manual-guion.md`). NVDA no funcionó en el equipo del autor: guion adaptado al Narrador de Windows el 18/11 | Autor, 1–2 h | Guion y hojas CSV | **Hecho (22/11/2026)**: 39/39 obligatorias, 0 bloqueos, 2 incidencias (IMP-67, IMP-68); contraste con foco de los botones verdes 3,32:1, corregido (IMP-69) y re-medido 5,26:1. H1a **aceptada**; `test:ci` 259/259, axe 100/100; memoria y presentación recompiladas. Aplicado en tablas 13.1 y 13.2, conclusiones, resumen, especificación, diapositivas 11 y 13, guion y pregunta 7 del tutor. Pendiente: confirmación del tutor sobre el Narrador |
| M2 | RNF-2 | Las 212 comprobaciones E2E también en Firefox y WebKit (motor de Safari) | Autor, 30 min de ejecución. IA analiza y propone correcciones | Hecho: `E2E_BROWSERS=all` en `playwright.config.ts` (636 pruebas listadas; por defecto y en CI sigue solo Chromium) | RNF-2 pasa a aceptado o se registran defectos |
| M3 | RNF-8, 9, 10 | Revisión heurística de Nielsen por dos personas y cronometraje de la realimentación | Autor y otra persona, 60–90 min cada uno. IA junta las hojas y redacta | Hecho: `docs/evaluation/revision-heuristica/` (protocolo, hoja y, desde el 23/11, `guion-evaluador.md` con el recorrido y una hoja por evaluador). Sin ejecutar | Estado de RNF-8, 9 y 10 en la tabla 13.2 |
| M4 ✔ (18/11, autor: instalada en iOS y Android; iconos de marca después, D-64) | RNF-18 | Instalar la PWA en Android (Chrome, «Instalar aplicación») e iOS (Safari, «Añadir a pantalla de inicio») a través de un túnel HTTPS, como en las sesiones RNF-4 | Autor, 30 min. IA prepara la lista de comprobación | Túnel ya usado el 13/11 | RNF-18 aceptado o no |
| M5 | RNF-17 | Perfilar la emisión, ajustar colas del *gateway* y de los servicios (el token ya se consume fuera del bloqueo) y repetir las tres ejecuciones de H1c | IA, 1–2 sesiones de código con revisión del secreto del voto. Autor, ≈ 1 h de k6 | Escenarios k6 y `carga.md` | **Hecha (19/11/2026): serie B, H1c rechazada de nuevo** (p95 7211 ms, 339 de 500 aceptados; unos 43 votos por segundo). Después, IMP-64 (confirmación por grupos, D-65) implementada y medida en la **serie C (19/11/2026): H1c rechazada otra vez** (unos 55 votos por segundo; 17 de 500 en menos de 2 s). H1c cerrada como rechazada; el perfilado del resto de la cadena queda como trabajo futuro (IMP-57). No afecta a los objetivos del anteproyecto: OE5 pide hacer la prueba de carga y documentar también los criterios no alcanzados |
| M6 | RF-05a | SSO de gestión con las cuentas de la UAL (A9) | Ambos | Sección 2b | Bloqueada (20/11/2026): sin acceso a las cuentas ni a la configuración institucional; trabajo futuro (P9). Véase la sección 9 |

**Comandos del autor para M2** (en `frontend`, con el frontend sin backend, igual que el E2E habitual):

```powershell
npx playwright install firefox webkit
$env:E2E_BROWSERS = 'all'
npx playwright test --project=firefox --workers=2
npx playwright test --project=webkit --workers=2
Remove-Item Env:E2E_BROWSERS
```

**Primera ejecución (18/11/2026, los tres navegadores a la vez):** Chromium 212 de 212; Firefox 115 de 212 y WebKit 173
de 212. Los 136 fallos son tiempos agotados (30 s) o cierres del navegador por carga, ninguno es un incumplimiento de
axe: ejecución **no concluyente**. Ajustado el mismo día: 90 s por prueba en Firefox y WebKit, un fichero de resultados
por navegador y ejecución de cada motor por separado con 2 *workers*.

**Segunda ejecución (18/11/2026, un motor cada vez):** Firefox y WebKit, 156 de 212 cada uno, todas sin
incumplimientos críticos ni serios. Las 56 restantes fallan siempre igual en los dos motores: sus peticiones POST
(código, verificación, búsqueda de privacidad) no pasan por el simulacro de Playwright y llegan al proxy (502). Es una
limitación del arnés (IMP-63), no de la aplicación.

**Tercera ejecución (18/11/2026, tras IMP-63):** con el servidor simulado de respaldo, Firefox **212 de 212** (informe
de Playwright: 212 esperadas, 0 inesperadas) y sin incumplimientos críticos ni serios en sus 208 resultados de axe.
WebKit: 211 de 212 y 0 incumplimientos en sus 207 resultados; la restante (V8-results, 1280 px, EN) agotó los 90 s esperando a que la casilla del código dejara de moverse, sin relación con la accesibilidad ni con las POST (la petición del código sí se simuló). Repetida sola el 19/11/2026: pasa. **M2 cerrada: RNF-2 aceptado** (tabla 13.2).

**Calendario propuesto**

| Semana | Mejoras |
| --- | --- |
| 18–24/11/2026 | M2 y M1 (autor); M4 si hay túnel |
| 25/11–6/12/2026 | M3 con la segunda persona |
| 7–20/12/2026 | M6 solo si el tutor lo pide (M5 adelantada al 18–24/11) |
| 21/12/2026–15/01/2027 | Pasar resultados a la memoria y a la presentación; C4 final |
| Desde el 15/01/2027 | Congelar el código; ensayos de la defensa |

### 8.5 Cambios de interfaz decididos por el autor

| Id | Qué | Motivo | Estado |
| --- | --- | --- | --- |
| M6b | Ocultar la columna «Identificador institucional» en la interfaz de gestión | Decisión de que el usuario no vea ese dato | Aplicado con commit `e87aed8` (20/11/2026) (IMP-66). El campo se conserva en el censo y en el backend porque lo usan otras funciones (importación, detección de duplicados, búsqueda y exportación de privacidad), pero desaparece de la vista del frontend: tabla del censo, formulario de alta y edición, vista previa y plantillas Excel descargables, textos de ayuda de la importación, motivo de duplicado y búsqueda de privacidad. La exportación de privacidad (derecho de acceso del RGPD, generada por el backend) lo sigue incluyendo |

## 9. Bloqueado o dependiente de terceros

| Id | Qué | Depende de | Estado |
| --- | --- | --- | --- |
| M6 / A9 | SSO con cuentas `@inlumine.ual.es` (RF-05a gestión; RF-05b con un IdP real) | Cuentas `@inlumine.ual.es` y configuración institucional (proveedor de identidad de la UAL) que no están disponibles; confirmación del tutor | Bloqueado (20/11/2026): no es posible probarlo en esta fase por falta de acceso a las cuentas y a la configuración institucional. Opción oculta en la consola (`features.institutionalSso = false`). Se deja como trabajo futuro (P9) para cuando se disponga de acceso. |
| Q-10 | Próxima reunión con el tutor | Respuesta del tutor | Sin respuesta |
| Q-13 | Prioridades MoSCoW de los RNF (tabla 7.1 con asteriscos) | Confirmación del tutor | Pendiente |
| D3 | Cabeceras CSP, HSTS y `Referrer-Policy` | Un despliegue (D-53) | Fuera de alcance: el TFM solo se ejecuta en local |

## Calendario orientativo

| Semanas | Foco |
| --- | --- |
| 03/11 – 16/11/2026 | A1–A6, A11, A12, B1 (preguntas), F1–F4 |
| 17/11 – 30/11/2026 | A7, A8, A9 (SSO UAL), A10, B3, F5–F6; D1, D2 y D5 (aprobadas, D-52) |
| 01/12 – 21/12/2026 | B2 (sesiones con votantes) |
| 22/12/2026 – 10/01/2027 | C1, C2, C3, E1 |
| 11/01 – 27/01/2027 | C4, E2, E3 |

## Registro de avance

| Fecha | Tarea | Nota / commit |
| --- | --- | --- |
| 03/11/2026 | — | Plan creado |
| 03/11/2026 | Q-05, Q-07, Q-09, Q-10 | Respuestas del autor; añadidas A11, A12, secciones 2b, 2c y 7; B4 hecha |
| 04/11/2026 | A11, F1–F4 | Script de la sesión H1b (`seed-usab.mjs`); SonarQube local, workflow `security` (Semgrep, Trivy, npm audit, gitleaks) y línea base |
| 04/11/2026 | Q-06, Q-08, Q-11, Q-12; F5 | Respuestas del autor (D-52). Primer `security`: gitleaks y Semgrep pasan; Trivy falla por 429 de Maven Central (arreglado: `~/.m2` resuelto y montado). SonarQube: 401, token no válido |
| 05/11/2026 | F5 | SonarQube: *quality gates* «Passed» con seguridad D en backend y fiabilidad D en frontend (captura); clasificación pendiente. `build` en verde; `security` sin verificar |
| 05/11/2026 | Q-06, Q-11, Q-12, A12, F5, D2, D5 | Condiciones del calendario H1b; RNF-4 registrado como agregado (PENDIENTE); aceptación verbal en Q-12; resultado comunicado de F5; D2 y D5 implementados sin commit |
| 05/11/2026 | Q-06, Q-11, Q-12 (D-54), A11, F5 | Segunda tanda de respuestas: fechas alternativas 15–16/11 como propuesta; RNF-4 con 2 tiempos declarados (PENDIENTE); aceptación verbal como limitación; `seed-usab.mjs` falla con el stack real (nombres con paréntesis), corregido sin repetir; métricas de SonarQube de la captura |
| 05/11/2026 | Q-11, A11 | Tercera tanda: RNF-4 con 3 tiempos confirmados (5, 5 y 6 min; PENDIENTE, 3 de 5); `seed-usab.mjs` falla por el historial de contraseñas (IMP-54); F5 en la rama `docs/f5-sonarqube-ci` |
| 05/11/2026 | A11, Q-06, Q-11, F5, D2, D5 | `seed-usab.mjs` termina con el stack real (escenario `USAB` preparado); RNF-4 con los 5 tiempos del autor (5 de 5 ≤ 10 min); F5 fusionada en `dev`; D2 y D5 con commit en su rama, sin fusionar |
| 05/11/2026 | Q-02, Q-11, IMP-52, B2 | D-55 («sin ayuda» admite documentación); tiempos RNF-4 estimados y alguno con formación previa: RNF-4 no demostrado; consentimiento completado con los datos institucionales; IMP-52 hecha |
| 05/11/2026 | Q-11 | Los 5 participantes de RNF-4 tenían formación previa: RNF-4 no demostrado (0 de 5 cumplen la condición), evidencia indicativa |
| 05/11/2026 | Q-11, C1 (RNF-4) | Corrección del autor: 2 de 5 sin formación previa y un tiempo de 6 a 11 min. RNF-4 no demostrado, pasado a `evaluacion_resultados.tex`, `contenido1.tex`, `conclusiones.tex` y `especificacion_detallada.tex` |
| 05/11/2026 | A4, A7, C1 (H1a) | Barrido axe del flujo del votante: 8 críticos (DEF-05), 0 serios; H1a rechazada en la versión medida. Script de preparación de la carga (H1c) |
| 05/11/2026 | A4, C1 (H1a), B2, A7 | DEF-05 corregido y nuevo barrido axe: 0 críticos, 0 serios (H1a: parte automática cumple; manual pendiente). DEF-06 abierto. Prueba del recibo adaptada. Hoja CSV y resumen de H1b. Definición de ejecución válida de k6 |
| 05/11/2026 | A4, C1 (H1a) | DEF-05 cerrado por el autor. DEF-06 corregido (aprobado): mensaje propio en región `role="alert"`; quitado `test.fail`, la prueba pasa. Tercer barrido axe con las mismas versiones: 88 auditorías, 0 críticos, 0 serios. H1a: parte automática favorable, parte manual pendiente |
| 08/11/2026 | A5, A6 | A5 backend (6 clases, 38 pruebas) y A6 (4 clases, 13 pruebas) escritas; pasan en el entorno de la IA y en el `./mvnw verify` del autor (Java 25, PostgreSQL 18.6; BUILD SUCCESS). Dos `pom.xml` con dependencias de prueba. Sin commit |
| 08/11/2026 | A10, A5, RNF-4 | A10: job `e2e-a11y` en `build.yml` (borrador, 200 de 200 con `CI=1`). A5: pruebas de la gráfica por día (Vitest 28 de 28); backend y A6 bloqueados por falta de Maven Central y Java 25. RNF-4: protocolo y hoja de cronometraje para repetir la sesión (`rnf4-protocolo-administradores.md`, `hoja-registro-rnf4.csv`), sin ejecutar. Sin commit |
| 08/11/2026 | A4, C3 | DEF-07 corregido (opción A, aprobada): tablas de la consola como regiones enfocables con nombre; barrido 2 con 0 críticos y 0 serios, 108 de 108 sin `test.fail`; Vitest 19 de 19 y build. Tabla de alcance (`dafo_justificacion.tex`) alineada: RF-11 en «Trabajo futuro». Sin commit |
| 08/11/2026 | A4 | Barrido axe de la consola de gestión y de los resultados públicos (108 auditorías): 0 críticos, 3 serios de DEF-07 (registrado, sin corregir). Prueba nueva `admin-console.accessibility.spec.ts`, sin commit |
| 08/11/2026 | A1, A2, A8, Q-01, Q-03, Q-04, C3 | A1 y A8 ya estaban hechas (comprobado). A2: entrada de la consola del 03/11 y quitada `PENDIENTE_PANEL`. Respuestas Q-01, Q-03 y Q-04 (D-56) aplicadas en la memoria (C3). Sin commit |
| 08/11/2026 | B3, C1 (H1a) | Revisión manual de H1a no realizada (NVDA no leía la página en el equipo del autor). Anexo del guion aplicado a la tabla 13.1, la sección de validez, `conclusiones.tex` y `contenido1.tex`, con el barrido 4 (`06f39be`): parte automática favorable; parte manual no realizada; sin decisión. CSV sin rellenar. Sin commit |
| 10/11/2026 | B2, C1 (H1b) | Sesiones H1b del 09/11: 5 válidas (P01 invalidada por el entorno, R1 la sustituye, R2 fuera del cómputo); 5 de 5 sin ayuda en ≤ 180 s, 5 de 5 comprensión; mediana 51 s. H1b aceptada y aplicada en `evaluacion_resultados.tex`, `contenido1.tex`, `conclusiones.tex`, `resumentexto.tex`, `especificacion_detallada.tex` e `informe.md`. IMP-56 (logo y margen). `feature/censo-importacion-ux` fusionada en `dev` y borrada. Rama `docs/h1b-resultados` |
| 11/11/2026 | P7.5, C1 (H1c) | Prueba de humo (ejecución 11) y tres ejecuciones de 500 VU (procesos 10–12): p95 4852, 2402 y 4849 ms; 222/240/281 votos aceptados, resto conexiones rechazadas; cadena íntegra. **H1c rechazada**, aplicada en `evaluacion_resultados.tex`, `contenido1.tex`, `conclusiones.tex`, `resumentexto.tex`, `especificacion_detallada.tex`, `informe.md` y `carga.md`. `check-load-run.mjs`, auditor sintético y recuento por código; IMP-57; IMP-55 actualizada. Rama `test/h1c-load-results` |
| 13/11/2026 | RNF-4 online, C2, D-63 | Sesiones online en curso (túnel nuevo, `login-code-fixed` reparado, comando `passwords`). C2: figura 10.1 de arquitectura implementada, RF-03 con Excel, texto heredado y cifras de pruebas del 11/11 (119 backend, 28 frontend, cobertura 14–70 %). Observaciones de participantes aplicadas en la consola (D-63; 53 Vitest en el entorno de la IA). IMP-59 a IMP-61. Sin commit |
| 13/11/2026 | B4, A12, C1 (RNF-4) | Sesiones RNF-4 online P01–P05: 5 de 5 válidas, sin formación previa, sin ayuda y en ≤ 4 min (mediana 3 min); **RNF-4 aceptado** y aplicado en `evaluacion_resultados.tex`, `conclusiones.tex`, `contenido1.tex`, `especificacion_detallada.tex` y `resumentexto.tex`. Tiempos redondeados al minuto (limitación anotada). D1 aplicado. Sin commit |
| 15/11/2026 | Revisión del plan; C2, F5 | Contrastado con el repositorio: A3, A4, A6, A7, A12, F5, D1, C1 y C2 marcados como hechos. Cobertura y recuentos del 14–15/11 en la memoria (135 backend, 92 frontend), capturas y figuras 5.1/5.2; `573fd7e` subido. Abiertos: A5 (resto), A9, A10 (verlo en GitHub), D2/D5 (rama sin fusionar), D4, F6, C4, E1–E3, Q-10 |
| 15/11/2026 | F6, A10, E1, anteproyecto | PR #4 en `main` con todos los checks en verde; P7 cerrado (evidencia por RF en `TRACEABILITY.md`, `resultados-p7/cierre.md`); anteproyecto firmado. D2 y D5: la rama se fusiona sin conflictos con `dev` (comprobado con `git apply --3way --check`); falta `mvnw verify` del autor |
| 15/11/2026 | D2, D5, C4 (parcial) | Rama de IMP-21 e IMP-27 fusionada por el autor (`c48dbab`) y verificada: 151 pruebas de backend. Memoria actualizada (151 y 92 pruebas; cobertura 52,4 % en backend). C4 parcial: figuras 8.1 y 8.2 redistribuidas y tabla 13.1 con resultados breves y el detalle en el texto |
| 16/11/2026 | IMP-50, A5, C4 | Mensaje del 429 del *gateway* en el flujo del votante (aprobado por el autor); 52 pruebas nuevas de frontend (144 en el entorno de la IA, cobertura 43,2 %; pendiente de la ejecución del autor para pasar las cifras a la memoria); capturas de dos en dos. Sin commit |
| 16/11/2026 | D4, IMP-46, IMP-48, C4, defensa | D4 implementado con pruebas (incluida la autorización HTTP de identity, `IdentityAuthorizationIntegrationTest`); IMP-46 aplicado; IMP-48 a la espera de los *digest*. Revisión de coherencia y estilo de la memoria (textos que seguían dando por pendientes evaluaciones ya hechas). Presentación de la defensa en `Presentacion-TFM/` (beamer, guion y preguntas). Sin commit |
| 17/11/2026 | Q-13, sección 8, IMP-48 | Prioridades MoSCoW propuestas en la tabla 7.1; OE5 añadido a la memoria; plan de objetivos del anteproyecto y RNF; IMP-48 aplicado; defensa ajustada a 15 min. Sin commit |
| 18/11/2026 | 8.3 (punto 3), 8.4 (punto 5) | Tabla 13.2 «Evidencia por requisito no funcional» en la memoria; plan de mejoras M1–M6; preparadas M2 (`E2E_BROWSERS=all`) y M3 (protocolo de revisión heurística). Sin commit |
| 18/11/2026 | M2, M4, D-64 | M2: Firefox y WebKit 156/212 sin incumplimientos, resto limitado por el arnés (IMP-63). M4 hecha por el autor. Iconos de la PWA, `apple-touch-icon`, `id` del manifiesto y banderas en móvil (D-64). Tabla 13.2: RNF-18 aceptado. Sin commit |
| 18/11/2026 | IMP-63, M1, M5 | IMP-63 hecha: Firefox 212/212. Guion de M1 adaptado al Narrador (y fila TM-01, IMP-50). M5: colas de conexión ajustadas y serie B de H1c definida. Sin commit |
| 19/11/2026 | IMP-64 implementada | Confirmación por grupos (D-65) y arreglo del commit ambiguo; serie C preparada en `carga.md` (reglas fijadas antes de medir). Pendiente: `mvnw verify` y serie C del autor. Sin commit |
| 19/11/2026 | M5, IMP-64 | Keycloak local recuperado (scripts `scripts/dev/keycloak-*`, `start-services.ps1`). Serie B de H1c: humo correcto; 20–22 descartadas (variables sin cargar); 23–25 contra un *gateway* antiguo; 26 válida y rechazada (p95 7,2 s). H1c cerrada como rechazada; memoria y presentación actualizadas. Sin commit |
| 20/11/2026 | Revisión del plan | Contrastado con el repositorio: `dev` va 3 commits por delante de `origin/dev` (`f0f4482`, `56746fa`, `39d939d`) y hay 4 ficheros modificados sin commit. M5 actualizada con la serie C. Abiertos: Q-10, Q-13, M1, M3, M6/A9, recuento final de pruebas y cobertura, C4, E2, E3 |
| 22/11/2026 | M1, C1, C4 | Revisión manual H1a completada con Narrador (39 obligatorias, 0 bloqueos); contraste corregido y re-medido. `test:ci` 259/259 y E2E axe 100/100; memoria (97 páginas) y presentación (20 diapositivas) recompiladas. H1a aceptada; pendiente confirmación del tutor y revisión editorial/visual C4 |
| 23/11/2026 | IMP-67, IMP-68, M3, C4, M6/A9 | IMP-67 e IMP-68 en el paso del código (261 pruebas en copia Linux); guion de M3 por evaluador; primera pasada de C4; párrafo del SSO comentado en `conclusiones.tex`. Sin commit |
| 22/11/2026 | Revisión del plan y preguntas al tutor | Contrastado con el repositorio y los chats: `dev` = `origin/dev` (`47f0134`), árbol limpio. `preguntas-reunion.md` actualizado (M1 hecha, cifras finales 175/259, M6b con commit, limitación de H1a en la pregunta 9). Abiertos: Q-10, Q-13, M3, IMP-67/68 (propuestas), texto de limitaciones del SSO, C4, E2, E3 |
| 20/11/2026 | M6/A9, M6b | SSO bloqueado y trabajo futuro (sección 9; opción oculta en la consola, IMP-65). M6b aplicado sin commit: «Identificador institucional» fuera de la interfaz de gestión y plantillas Excel regeneradas (IMP-66) |
