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
| Pruebas y medición (P7) | En curso: 119 pruebas backend y 28 frontend en la ejecución del 11/11 (cobertura de líneas 14–70 %); RNF-4 aceptado (13/11, 5 de 5 sin formación previa, máximo 4 min); H1a sin decisión (parte automática favorable; parte manual no realizada); H1b aceptada (09/11, 5 de 5 y 5 de 5); H1c rechazada (11/11, p95 2,4–4,9 s) |
| Memoria (P8) | En curso: C1 aplicado para H1a, H1b y H1c; C2 en curso (13/11: figura de arquitectura implementada, RF-03 con Excel, cifras de pruebas del 11/11); faltan capturas y C4 |
| Publicación | `dev` va por delante de `origin/dev`: falta `git push origin dev` (lo hace el autor) |

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

---

## 2. Se puede empezar ya (sin esperar respuestas)

Orden recomendado: de arriba abajo. Semana del 03/11 al 16/11/2026.

- [x] **A1 · Publicar lo que hay** (`Autor`): `git push origin dev`. **Hecho:** `origin/dev` coincide con `dev` (`06f39be`, 08/11/2026). Opcional: borrar ramas locales ya fusionadas
  (`feat/login-and-home-polish`, `feat/login-language-flags`, `feat/login-responsive-redesign`,
  `fix/login-mobile-spacing`, `feat/console-rail-navigation`) y la remota `origin/feat/role-home-dashboards`.
- [x] **A2 · Poner al día `PROGRESS.md`** (`IA`, 08/11/2026): entrada de la consola del 03/11 (barra de iconos, logo, banderas,
  gráfica por periodo) y quitar la marca suelta `PENDIENTE_PANEL`.
- [ ] **A3 · Demo UAL y E2E** (`Ambos`): arrancar el stack, `node scripts/demo/seed-ual.mjs` (corregido, sin repetir)
  y `npm run test:e2e` contra el frontend de la rama actual (no otro servidor en el puerto 4200, ver DEF-03).
- [ ] **A4 · axe en todas las pantallas** (`Ambos`, parte automática de H1a): login, inicio de cada rol, censos,
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
- [ ] **A5 · Pruebas unitarias P7.1** (`IA`): censo/CSV, claims institucionales (RF-05b), privacidad (RF-18),
  recordatorios (RF-17), errores y reglas de dominio sin prueba; frontend: gráfica por día (`app-day-chart`),
  agregados del inicio y flujo visual del votante. **Parcial (08/11/2026, sin commit):** frontend, gráfica por día
  (`day-chart.component.spec.ts`, 9 pruebas; Vitest 28 de 28). Backend escrito (08/11/2026, sin commit): `CensusServiceTest`,
  `PrivacyServiceTest` (election) y `JwtVoterIdentityVerifierTest`, `VoterAccessServiceSsoTest`, `NotificationAdminServiceTest`,
  `PersonalDataServiceTest` (voter-access), 38 pruebas que pasan. **Verificadas por el autor (08/11/2026):** `./mvnw verify` en Windows con Java 25.0.5 y Testcontainers (PostgreSQL 18.6): BUILD SUCCESS en 5:27 min; election 36, voter-access 38 y ballot-box 22 pruebas, 0 fallos.
- [ ] **A6 · Integración P7.2/P7.3** (`IA`; **escrita 08/11/2026, sin commit**: `ElectionOrganizationIsolationIntegrationTest`, `ProcessSyncOutboxIntegrationTest`, `VoterAccessScenariosIntegrationTest`, `BallotBoxScenariosIntegrationTest`, 13 pruebas que pasan; alcance acordado: solo el cruce entre organizaciones, no la matriz de roles; se añaden `spring-boot-starter-webmvc-test` y `spring-security-test` como dependencias de prueba en voter-access y ballot-box. **Verificadas por el autor (08/11/2026):** `./mvnw verify` en Windows con Java 25.0.5 y Testcontainers (PostgreSQL 18.6): BUILD SUCCESS en 5:27 min; election 36, voter-access 38 y ballot-box 22 pruebas, 0 fallos.): matriz completa de autorización HTTP entre organizaciones y roles,
  concurrencia del token de voto (un solo voto con N envíos), reintentos e idempotencia del *outbox*, inspección del
  esquema y de los logs para el secreto del voto (RNF-11), todas las manipulaciones de la cadena.
- [ ] **A7 · Carga H1c** (`Ambos`): instalar k6, completar escenarios de acceso y escrutinio, credenciales sintéticas
  fuera del repo, tres ejecuciones de 500 VU con p95, errores y recursos; comprobar la cadena y la participación después.
  **En curso (05/11):** `scripts/load/prepare-voting-tokens.mjs` prepara cada ejecución (sin probar con el stack real).
  Solo después de las sesiones H1b (Mailpit guarda 500 mensajes). Falta instalar k6 y ejecutar. Variables y
  definición de ejecución válida en `docs/evaluation/carga.md`. El autor ejecuta; la IA analiza y cierra H1c.
- [x] **A8 · Gantt de la memoria** (`IA`, IMP-02): la iteración 3 aún dice «re-voto». **Hecho:** `plan_gantt.tex` ya dice «Iteración 3. Emisión y recibo (RF-09--RF-10)» (comprobado el 08/11/2026).
- [ ] **A9 · RF-05a** (`IA`, según Q-07): IdP externo de prueba y prueba de inicio de sesión de gestión federado.
- [ ] **A10 · Job E2E en CI** (`IA`): Playwright + axe en GitHub Actions (al menos con servicios simulados).
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
- [ ] **A12 · Registrar la sesión RNF-4** (`IA`, tras Q-11): hoja de resultados de administradores en
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
- [ ] **F5 · Corregir lo que salga** (`IA`): mirar el primer resultado del workflow `security` en GitHub (las
  vulnerabilidades de Maven solo se ven allí) y el *quality gate* de SonarQube; pendientes de la línea base: IMP-46
  (Actions de `build.yml` por SHA), IMP-47, IMP-48 e IMP-49 (necesitan aprobación). Anotar los resultados en la memoria.
  **En curso (05/11):** SonarQube, según la captura del autor: los dos *quality gates* en «Passed»; backend con seguridad
  D (6 incidencias), fiabilidad C (5), mantenibilidad A (6) y cobertura 18,8 %; frontend con seguridad A (0), fiabilidad D
  (12), mantenibilidad A (1) y cobertura 0,0 %. Clasificación pendiente hasta exportar la lista de incidencias. Workflow
  `build`: todo en verde y Vitest 19 de 19 (captura). Workflow `security`: sin verificar (solo comunicación del autor);
  faltan `semgrep-report` y `trivy-report`. Detalle y pasos de exportación en `docs/architecture/quality-and-security.md`.
  **13/11/2026:** workflow `security` de `dev` en verde (run `43066068128`); `npm audit` 0; Semgrep local 0 ERROR y sin
  hallazgos nuevos (IMP-46 e IMP-48 ya registradas; resto mitigado o falso positivo). Falta SonarQube, tras las sesiones RNF-4.
- [ ] **F6 · Primer `dev` → `main`** (`Autor`, sábado tras F1–F5).

## 3. Necesitan al autor

- [ ] **B1 · Responder las preguntas Q-01 a Q-10** (`Autor`).
- [x] **B2 · Sesión H1b con cinco votantes** (`Autor`, prioritaria): **Hecho (09/11/2026): H1b aceptada**, véase `hoja-registro.md`. montar el escenario `USAB`
  (`docs/evaluation/usabilidad/preparacion-sesion.md`, `censo-sesion.csv`), hacer un ensayo y las cinco sesiones;
  anotar T0/T1/T2 y resultados como cantidades (x de 5) en la hoja de registro (preparadas el 05/11:
  `hoja-registro-h1b.csv` y `scripts/evaluation/h1b-summary.mjs` para el resumen y la decisión del mismo día 09). Criterio: 5 de 5 en ≤ 3 min y al
  menos 4 de 5 interpretan la verificación.
- [x] **B3 · Revisión manual de accesibilidad** (`Autor`, parte manual de H1a): teclado y lector de pantalla
  (NVDA o Narrador) en acceso, voto y recibo siguiendo `docs/evaluation/accesibilidad-manual.md`.
  **Cerrada sin ejecutar (08/11/2026):** NVDA no leía la página en el equipo del autor; se aplica el anexo del guion.
  H1a queda sin decisión y las hojas CSV se conservan sin rellenar.
- [x] **B4 · RNF-4 con administradores** (`Autor`): sesión realizada (comunicado el 03/11/2026). Falta registrar
  los datos (Q-11 → A12; hecho el 05/11) y fijar la definición de «sin ayuda» (Q-02; hecho, D-55).

## 4. Mejoras opcionales (solo si se aprueban en Q-08)

**Aprobado por el autor el 04/11/2026 (Q-08, D-52).** Propuesta de la IA (03/11): hacer ya D1, D2 y D5 (bajo coste y mejoran accesibilidad y seguridad), D3 al preparar
el despliegue y D4 si sobra tiempo. Las que más aportan antes de entregar, de más a menos:

- [ ] **D1 · IMP-15**: aviso 2 minutos antes de que caduque la sesión de gestión (WCAG 2.2.1).
- [ ] **D2 · IMP-21**: límite de peticiones por IP en el *gateway* para `/api/v1/voter-access/**` (RNF-14). **En curso
  (05/11):** commit `4ec0efc` en la rama `feat/voter-access-limit-jwt-audience`, sin fusionar; faltan las pruebas del autor.
- [ ] **D3 · IMP-32**: cabeceras CSP, HSTS y `Referrer-Policy` en el despliegue del frontal. Solo si hay despliegue: el
  TFM se ejecuta y evalúa en local y no está previsto desplegarlo (05/11, D-53).
- [ ] **D4 · IMP-11 / IMP-40**: al suspender una organización, deshabilitar sus usuarios y claves de API en Keycloak.
- [ ] **D5 · IMP-27**: comprobar la audiencia del JWT en cada servicio y quitar `verivoto-dev-swagger` en producción
  (esta segunda parte solo aplica si hay despliegue, D-53). **En curso (05/11):** commit `72bdfba` en la misma rama que D2, sin fusionar;
  faltan las pruebas del autor y actualizar el realm local (`scripts/dev/keycloak-api-audience.mjs`).

El resto de `IMPROVEMENTS.md` queda para P9 salvo que se decida otra cosa.

## 5. Memoria (P8)

- [ ] **C1 · Resultados de la evaluación** (`IA`, tras A4, A7, B2, B3): completar `docs/evaluation/informe.md`, la tabla
  13.1 (`evaluacion_resultados.tex`), la lista `[PENDIENTE]` de `contenido1.tex` y las conclusiones. Lo que no se
  mida se deja como PENDIENTE y limitación, sin inventar resultados. **En curso (05/11):** parte de RNF-4 hecha (no
  demostrado); H1a: parte automática favorable tras corregir DEF-05 y DEF-06, parte manual no realizada y H1a sin decisión (08/11, aplicado en la memoria); H1b aceptada y aplicada en la memoria (10/11); H1c rechazada y aplicada en la memoria (11/11); antes: H1c sin
  datos. C2 y C3 no se adelantan hasta tener H1a, H1b y H1c medidas o declaradas limitación.
- [ ] **C2 · Alinear con lo implementado** (`IA`): figuras de arquitectura (objetivo frente a implementado),
  diagramas conceptuales y texto heredado; capturas seguras de las pantallas (datos sintéticos de la demo UAL). **En curso (13/11/2026):** figura de arquitectura implementada, texto heredado, RF-03 (Excel), figura de secuencia y cifras de pruebas hechos, sin commit; faltan las capturas.
- [x] **C3 · Aplicar las decisiones Q-01 a Q-04** (`IA`) en la especificación y las tablas 1.1 y 4.3. **Hecho (08/11/2026, D-56)**, adelantado a petición del autor (el plan decía esperar a H1a, H1b y H1c): `contenido1.tex` (tablas 1.1 y 4.3), `especificacion_detallada.tex` (nota de alcance, RF-11 y RI-6) y `conclusiones.tex` (P9).
- [ ] **C4 · Revisión editorial y visual final** (`Ambos`, P8.4): compilar el PDF, revisar página a página,
  avisos de maquetación de portada y contraportada; el autor da el visto bueno.

## 6. Cierre

- [ ] **E1 · Cerrar P7** (`IA`): cuando A3–A7 y B2–B4 estén hechos o declarados pendientes, actualizar `PROGRESS.md`,
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
