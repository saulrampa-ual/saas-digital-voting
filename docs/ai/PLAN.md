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
| Pruebas y medición (P7) | En curso: 60 pruebas backend, 19 frontend, cobertura 7–14 %; H1a, H1b y H1c sin medir |
| Memoria (P8) | En curso: borrador de 82 páginas; tabla de hipótesis en «PENDIENTE» |
| Publicación | `dev` va por delante de `origin/dev`: falta `git push origin dev` (lo hace el autor) |

---

## 1. Preguntas para el autor (responder primero: desbloquean tareas)

| ID | Pregunta | Desbloquea | Respuesta |
| --- | --- | --- | --- |
| Q-01 | ¿RF-11 (revoto) queda como «Could» o pasa a «Won't» en la tabla 4.3? | C3 | ¿? |
| Q-02 | ¿«Sin ayuda» en RNF-4 excluye también consultar la documentación? (marcador `[PENDIENTE DEL AUTOR]` en `especificacion_detallada.tex`) | C3, B4 | ¿? |
| Q-03 | ¿Se anotan RF-22 y RF-23 como fuera del MVP en la tabla 1.1? | C3 | ¿? |
| Q-04 | ¿En P9 se depura el modelo de datos de sustitución del voto (`votes.status` `SUBSTITUTED`, RI-6/RI-8/RI-9) o se deja descrito? | C3 | ¿? |
| Q-05 | ¿Se hace la sesión RNF-4 con cinco administradores o se declara pendiente en la memoria? | B4 | **Respondida (03/11):** la sesión con administradores ya se hizo. Falta pasar los datos: ver Q-11 |
| Q-06 | ¿Fecha para el ensayo y para las cinco sesiones con votantes (H1b)? ¿Quiénes participan (sin datos reales)? | B2 | **Respondida (04/11):** ensayo el 08/11/2026 y sesiones H1b el 09/11/2026. Antes del ensayo, `seed-usab.mjs` tiene que funcionar con el stack real (A11) |
| Q-07 | ¿Se prueba RF-05a con un IdP externo (p. ej. un segundo realm de Keycloak como «universidad») o se deja como limitación? | A9 | **Respondida (03/11):** sí, con las cuentas `@inlumine.ual.es` de 4 compañeros (Google Workspace). Plan en la sección 2b; falta Q-12 |
| Q-08 | ¿Qué mejoras de `IMPROVEMENTS.md` se aprueban antes de la entrega? Propuesta en la sección 4 | D1–D5 | **Respondida (04/11, D-52):** se aprueban D1, D2 y D5 ya, D3 con el despliegue y D4 si queda tiempo |
| Q-09 | ¿Cuándo se fusiona `dev` en `main` (al cerrar P7, o antes para tener una versión estable)? | E2 | **Respondida (03/11):** en fin de semana (sábado), después de implantar SonarQube y un análisis de seguridad gratuito similar a Fortify (sección 2c) |
| Q-10 | ¿Qué fecha tiene la próxima revisión con el tutor y qué quiere ver? (marca el orden de C1–C4) | C | **En espera (03/11):** se le envió la memoria actualizada y aún no ha respondido. Las indicaciones de la reunión del 21/10/2026 ya están aplicadas (sección 7) |
| Q-11 | Resultados de la sesión RNF-4: para cada administrador (A1–A5), ¿terminó crear, configurar y programar un proceso de opción única?, ¿cuánto tardó?, ¿necesitó ayuda (y de qué tipo)? Fecha de la sesión y si firmaron consentimiento | C1 | **Parcial (04/11):** sesión el 03/11/2026, consentimiento firmado, según el registro del autor todos terminaron sin ayuda. Falta, para A1–A5, si terminó y cuánto tardó cada uno, y aclarar si los 30 minutos son por administrador o de toda la sesión |
| Q-12 | SSO con cuentas de la UAL: ¿los 4 compañeros aceptan por escrito que se usen sus correos solo para la prueba, en local y borrándolos después? ¿Tienes una cuenta de Google personal para crear el proyecto de Google Cloud? | A9 | **Parcial (04/11):** hay cuenta de Google para el proyecto de Google Cloud. Falta confirmar que los 4 compañeros aceptan por escrito |

---

## 2. Se puede empezar ya (sin esperar respuestas)

Orden recomendado: de arriba abajo. Semana del 03/11 al 16/11/2026.

- [ ] **A1 · Publicar lo que hay** (`Autor`): `git push origin dev`. Opcional: borrar ramas locales ya fusionadas
  (`feat/login-and-home-polish`, `feat/login-language-flags`, `feat/login-responsive-redesign`,
  `fix/login-mobile-spacing`, `feat/console-rail-navigation`) y la remota `origin/feat/role-home-dashboards`.
- [ ] **A2 · Poner al día `PROGRESS.md`** (`IA`): entrada de la consola del 03/11 (barra de iconos, logo, banderas,
  gráfica por periodo) y quitar la marca suelta `PENDIENTE_PANEL`.
- [ ] **A3 · Demo UAL y E2E** (`Ambos`): arrancar el stack, `node scripts/demo/seed-ual.mjs` (corregido, sin repetir)
  y `npm run test:e2e` contra el frontend de la rama actual (no otro servidor en el puerto 4200, ver DEF-03).
- [ ] **A4 · axe en todas las pantallas** (`Ambos`, parte automática de H1a): login, inicio de cada rol, censos,
  procesos (A3/A4), participación, resultados, auditoría, trazabilidad, privacidad, claves de API, flujo del votante
  (V1–V8, B1–B5) en 1280 y 360 px, ES y EN. Criterio: cero incumplimientos críticos ni serios. El selector de idioma
  con banderas es nuevo (sustituye al que dio DEF-01): repetir también `/recibo/:id`.
- [ ] **A5 · Pruebas unitarias P7.1** (`IA`): censo/CSV, claims institucionales (RF-05b), privacidad (RF-18),
  recordatorios (RF-17), errores y reglas de dominio sin prueba; frontend: gráfica por día (`app-day-chart`),
  agregados del inicio y flujo visual del votante.
- [ ] **A6 · Integración P7.2/P7.3** (`IA`): matriz completa de autorización HTTP entre organizaciones y roles,
  concurrencia del token de voto (un solo voto con N envíos), reintentos e idempotencia del *outbox*, inspección del
  esquema y de los logs para el secreto del voto (RNF-11), todas las manipulaciones de la cadena.
- [ ] **A7 · Carga H1c** (`Ambos`): instalar k6, completar escenarios de acceso y escrutinio, credenciales sintéticas
  fuera del repo, tres ejecuciones de 500 VU con p95, errores y recursos; comprobar la cadena y la participación después.
- [ ] **A8 · Gantt de la memoria** (`IA`, IMP-02): la iteración 3 aún dice «re-voto».
- [ ] **A9 · RF-05a** (`IA`, según Q-07): IdP externo de prueba y prueba de inicio de sesión de gestión federado.
- [ ] **A10 · Job E2E en CI** (`IA`): Playwright + axe en GitHub Actions (al menos con servicios simulados).
- [x] **A11 · Elección y votantes para la sesión H1b** (`IA` prepara, `Autor` ejecuta): hecho el 04/11/2026
  (`d5608f9`): `node scripts/demo/seed-usab.mjs` monta el escenario `USAB` de `preparacion-sesion.md` (censo de 7
  entradas `p01…p05@verivoto.test` + `r1`, `r2`, papeleta neutra, proceso abierto 7 días) y un proceso de ensayo
  aparte; deja la hoja de la persona moderadora en `.local/sesion-usab.md` con el buzón de Mailpit de cada entrada.
  Probado contra un entorno simulado; **falta que el autor lo ejecute con el stack real** antes del ensayo (B2).
- [ ] **A12 · Registrar la sesión RNF-4** (`IA`, tras Q-11): hoja de resultados de administradores en
  `docs/evaluation/usabilidad/`, resultado como cantidades (x de 5) y su paso a la memoria (C1).

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
- [ ] **F6 · Primer `dev` → `main`** (`Autor`, sábado tras F1–F5).

## 3. Necesitan al autor

- [ ] **B1 · Responder las preguntas Q-01 a Q-10** (`Autor`).
- [ ] **B2 · Sesión H1b con cinco votantes** (`Autor`, prioritaria): montar el escenario `USAB`
  (`docs/evaluation/usabilidad/preparacion-sesion.md`, `censo-sesion.csv`), hacer un ensayo y las cinco sesiones;
  anotar T0/T1/T2 y resultados como cantidades (x de 5) en la hoja de registro. Criterio: 5 de 5 en ≤ 3 min y al
  menos 4 de 5 interpretan la verificación.
- [ ] **B3 · Revisión manual de accesibilidad** (`Autor`, parte manual de H1a): teclado y lector de pantalla
  (NVDA o Narrador) en acceso, voto y recibo siguiendo `docs/evaluation/accesibilidad-manual.md`.
- [x] **B4 · RNF-4 con administradores** (`Autor`): sesión realizada (comunicado el 03/11/2026). Falta registrar
  los datos (Q-11 → A12) y fijar la definición de «sin ayuda» (Q-02).

## 4. Mejoras opcionales (solo si se aprueban en Q-08)

**Aprobado por el autor el 04/11/2026 (Q-08, D-52).** Propuesta de la IA (03/11): hacer ya D1, D2 y D5 (bajo coste y mejoran accesibilidad y seguridad), D3 al preparar
el despliegue y D4 si sobra tiempo. Las que más aportan antes de entregar, de más a menos:

- [ ] **D1 · IMP-15**: aviso 2 minutos antes de que caduque la sesión de gestión (WCAG 2.2.1).
- [ ] **D2 · IMP-21**: límite de peticiones por IP en el *gateway* para `/api/v1/voter-access/**` (RNF-14).
- [ ] **D3 · IMP-32**: cabeceras CSP, HSTS y `Referrer-Policy` en el despliegue del frontal.
- [ ] **D4 · IMP-11 / IMP-40**: al suspender una organización, deshabilitar sus usuarios y claves de API en Keycloak.
- [ ] **D5 · IMP-27**: comprobar la audiencia del JWT en cada servicio y quitar `verivoto-dev-swagger` en producción.

El resto de `IMPROVEMENTS.md` queda para P9 salvo que se decida otra cosa.

## 5. Memoria (P8)

- [ ] **C1 · Resultados de la evaluación** (`IA`, tras A4, A7, B2, B3): completar `docs/evaluation/informe.md`, la tabla
  13.1 (`evaluacion_resultados.tex`), la lista `[PENDIENTE]` de `contenido1.tex` y las conclusiones. Lo que no se
  mida se deja como PENDIENTE y limitación, sin inventar resultados.
- [ ] **C2 · Alinear con lo implementado** (`IA`): figuras de arquitectura (objetivo frente a implementado),
  diagramas conceptuales y texto heredado; capturas seguras de las pantallas (datos sintéticos de la demo UAL).
- [ ] **C3 · Aplicar las decisiones Q-01 a Q-04** (`IA`) en la especificación y las tablas 1.1 y 4.3.
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
| 17/11 – 30/11/2026 | A7, A8, A9 (SSO UAL), A10, B3, ensayo de B2, F5–F6; D1–D5 si se aprueban |
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
