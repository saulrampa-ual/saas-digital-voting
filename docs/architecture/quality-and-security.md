# Calidad y seguridad del código

Herramientas gratuitas que sustituyen a un analizador comercial como Fortify (tareas F1–F4 de `docs/ai/PLAN.md`,
decisión D-51). Se ejecutan antes de cada paso de `dev` a `main` (los sábados, Q-09) y en cada push o pull request.

| Tarea | Herramienta | Versión | Dónde | Qué comprueba | Bloquea |
| --- | --- | --- | --- | --- | --- |
| F1 | SonarQube Community Build | 26.10.0.133078 | Local (`infra/compose.quality.yaml`) | Fiabilidad, mantenibilidad, duplicados, *security hotspots*, cobertura (JaCoCo) | *Quality gate* «Sonar way» (revisión manual) |
| F2 | Semgrep Community Edition | 1.183.0 | CI (`security.yml`, job `sast`) | SAST de Java, TypeScript, secretos, OWASP Top 10, GitHub Actions y Dockerfile | Sí, hallazgos `ERROR` |
| F3 | Trivy | 0.76.1 | CI (job `dependencies`) | Vulnerabilidades de dependencias Maven y npm, configuración (Dockerfile) y secretos | Sí, `HIGH`/`CRITICAL` con versión corregida |
| F3 | npm audit | (npm de Node 24) | CI (job `dependencies`) | Dependencias de producción del frontend | Sí, `high` o superior |
| F4 | gitleaks | 8.30.1 | CI (job `secrets`) y *pre-commit* local | Secretos en todo el historial de git (todas las ramas) | Sí |

Diferencias con Fortify que conviene declarar en la memoria: Semgrep CE no hace análisis de flujo entre ficheros
(*taint* interprocedimental), que en Semgrep es de pago; SonarQube Community tampoco hace análisis de *taint* (solo en
ediciones comerciales). CodeQL y SonarQube Cloud son gratuitos solo para repositorios públicos.

## Cómo se ejecuta

### En GitHub Actions (F2–F4)

`.github/workflows/security.yml` se lanza en cada push y pull request a `dev` y `main`, los sábados a las 06:17 UTC
(antes del paso semanal a `main`) y a mano (*Run workflow*). Cada job sube su informe completo como artefacto
(`semgrep-report`, `trivy-report`) y luego aplica su umbral.

Las Actions de terceros van fijadas por SHA de commit (con la etiqueta en un comentario) y los analizadores se ejecutan
desde versiones fijas, no desde sus propias Actions: en marzo de 2026 se comprometieron `trivy-action`, `setup-trivy` y
las imágenes 0.69.4–0.69.6 de Trivy ([GHSA-69fq-xp46-6x23](https://osv.dev/vulnerability/GHSA-69fq-xp46-6x23)).
Las imágenes Docker van por etiqueta de versión, no por *digest* (IMP-48).

### SonarQube en local (F1)

1. Docker Desktop en marcha. `docker compose -f infra/compose.quality.yaml up -d` y abrir `http://localhost:9000`.
   Primer acceso `admin` / `admin`; SonarQube obliga a cambiar la contraseña.
2. *My Account → Security → Generate token*, tipo «Global Analysis Token». No guardarlo en ficheros.
3. Desde la raíz del repositorio, en PowerShell:

   ```powershell
   $env:SONAR_TOKEN = "<token>"
   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\quality\sonar.ps1
   ```

   El script arranca SonarQube si no está, espera a que esté `UP`, ejecuta `mvnw verify` (pruebas e informes JaCoCo)
   con `sonar-maven-plugin` 5.8.0.7211 (fijado en `backend/pom.xml`), analiza el frontend con `@sonar/scan` 5.0.2
   (`frontend/sonar-project.properties`) y muestra el *quality gate* de `verivoto-backend` y `verivoto-frontend`.
   `-SkipBackend` o `-SkipFrontend` analizan solo una parte. Las pruebas del backend usan Testcontainers: Docker debe
   estar disponible.
4. `docker compose -f infra/compose.quality.yaml down` para pararlo; los volúmenes conservan los análisis.

La base de datos es la integrada de SonarQube (modo de evaluación): no vale para producción ni se actualiza entre
versiones; al cambiar la versión de la imagen hay que borrar los volúmenes (`down -v`). Sin cobertura en el frontend
hasta instalar el proveedor de cobertura de Vitest (IMP-49).

### Mismas comprobaciones en local, sin esperar al CI

```bash
pip install semgrep==1.183.0
semgrep scan --metrics=off --config p/java --config p/typescript --config p/secrets --config p/owasp-top-ten \
  --config p/github-actions --config p/dockerfile
docker run --rm -v "$PWD:/repo" zricethezav/gitleaks:v8.30.1 git --redact --log-opts="--all" /repo
docker run --rm -v "$PWD:/src" -w /src aquasec/trivy:0.76.1 fs --scanners vuln,misconfig,secret \
  --skip-dirs Plantilla-TFM-Informatica-UAL --skip-dirs prototiposPantallas /src
cd frontend && npm audit --omit=dev
```

*Pre-commit* (F4): `pip install pre-commit` y `pre-commit install` una vez; desde entonces gitleaks revisa cada commit
(`.pre-commit-config.yaml`).

`.semgrepignore` excluye la memoria del TFM, los prototipos, la documentación y los directorios generados. Un falso
positivo se marca con `// nosemgrep` en la línea anterior, con el motivo y la referencia a este documento, y se anota
en la tabla de abajo.

## Línea base (04/11/2026, rama `dev`, commit `72609e6`)

Ejecutada en el entorno de la IA con los binarios oficiales comprobados con sus sumas SHA-256. Las reglas de Semgrep
se tomaron del repositorio `semgrep/semgrep-rules` (carpetas `java`, `typescript`, `javascript`, `dockerfile`, `yaml`,
`generic/secrets` y `html`), no del registro (`semgrep.dev` no era accesible), así que el primer análisis del CI con los
paquetes `p/…` puede diferir. La base de vulnerabilidades de Trivy tampoco era accesible: las vulnerabilidades de las
dependencias Maven solo se verán en el CI. SonarQube no se ha ejecutado todavía.

| Herramienta | Resultado |
| --- | --- |
| gitleaks (historial completo, todas las ramas, y ficheros actuales) | 0 secretos |
| npm audit (`frontend/package-lock.json`, todas las dependencias) | 0 vulnerabilidades |
| Trivy, configuración y secretos | 1 aviso `LOW` (DS-0026: el Dockerfile de Keycloak no define `HEALTHCHECK`; la salud se comprueba en `compose.yaml`). Aceptado |
| Trivy, vulnerabilidades | No ejecutado (sin acceso a la base de datos) |
| Semgrep | 138 hallazgos con las reglas del repositorio: revisión abajo |

Revisión de los hallazgos de Semgrep:

| Hallazgo | Fichero | Nivel | Decisión |
| --- | --- | --- | --- |
| `insecure-innerhtml`, `insecure-document-method` | `infra/keycloak/themes/verivoto/login/resources/js/verivoto.js:49` | ERROR | Falso positivo: solo inserta constantes de `LANGS` (bandera SVG y código), buscadas con una clave que debe existir en `LANGS`. Marcado con `nosemgrep`. Sustituir por `createElementNS` queda como IMP-47 |
| `github-actions-mutable-action-tag` (×5) | `.github/workflows/build.yml` | WARNING | Real: las Actions de `build.yml` van por etiqueta. Fijarlas por SHA como en `security.yml` (IMP-46) |
| `dockerfile-source-not-pinned` | `infra/keycloak/Dockerfile:1` | INFO | Real (bajo): imagen base por etiqueta, no por *digest* (IMP-48) |
| `prefer-copy-over-add` | `infra/keycloak/Dockerfile:6` | INFO | Aceptado: `ADD` de una URL con `--checksum`, comprobada en la construcción |
| `html-in-template-string` (×2) | `frontend/src/app/features/admin/api-keys/api-keys.page.ts:242` | WARNING | Falso positivo: es el texto de un ejemplo `curl` que Angular muestra con interpolación (escapado) |
| `lazy-load-module` | `scripts/demo/lib/common.mjs:38` | WARNING | Aceptado: Playwright se carga desde el frontend solo en los scripts de demo locales |
| `missing-template-string-indicator` (×108), `i18next-key-format` (×18) | Componentes Angular | INFO/WARNING | Ruido: reglas de JavaScript genérico e i18next que no aplican a plantillas Angular ni a `ngx-translate`. No están en los paquetes `p/…` del CI |
| Error de análisis | `frontend/src/app/features/admin/processes/process-form.page.html:193` | — | El analizador HTML de Semgrep no entiende el *control flow* de Angular (`@if`). Sin efecto en el resultado |

## Primera ejecución (F5, 04/11/2026)

| Herramienta | Resultado | Acción |
| --- | --- | --- |
| gitleaks (CI) | Pasa | — |
| Semgrep CE (CI, paquetes `p/…`) | Pasa el umbral `ERROR` | Falta revisar el informe completo (`semgrep-report`) |
| Trivy (CI) | Error `FATAL`: Maven Central respondió `429 Too Many Requests` al descargar POM padre (p. ej. `spring-cloud-dependencies-parent`) desde la IP compartida del *runner*; no se generó `trivy-report.txt` y no se ejecutaron el umbral ni `npm audit` | El job resuelve antes las dependencias (`mvnw dependency:resolve`, caché de `~/.m2` con `setup-java`) y monta `~/.m2` en Trivy; el informe ya no corta el job y `npm audit` se ejecuta siempre. Pendiente de repetir |
| SonarQube (local) | El escáner no arranca: `GET /api/v2/analysis/version` devuelve `401 Unauthorized` | Token no válido: generar uno nuevo («Global Analysis Token») y repetir. Sin cambios en el proyecto |

### Segunda ejecución (commit `6c30035`)

Qué está comprobado y qué no. La IA no tiene acceso a GitHub Actions ni al SonarQube local: separa lo que ha podido
verificar (ficheros del repositorio y capturas del autor) de lo que el autor solo ha comunicado.

| Herramienta | Resultado | Fuente |
| --- | --- | --- |
| Workflow `security` (gitleaks, Semgrep CE, Trivy y `npm audit`) | Sin verificar: no hay captura, registros ni artefactos de este workflow. Solo consta lo que comunicó el autor el 05/11/2026 (que aparecía en verde) | Comunicación del autor |
| Workflow `build` (ejecución 41829391258) | Tres jobs en verde (backend con Java 25 y cobertura, compose, frontend); Vitest: 4 de 4 ficheros y 19 de 19 pruebas | Captura del autor (05/11/2026) |
| Semgrep CE, hallazgos por debajo de `ERROR` | Sin revisar | Falta el artefacto `semgrep-report` (`semgrep.sarif`) |
| Trivy, hallazgos por debajo de `HIGH`/`CRITICAL` o sin corrección | Sin revisar | Falta el artefacto `trivy-report` (`trivy-report.txt`) |
| SonarQube, análisis enviados | `verivoto-backend` y `verivoto-frontend` analizados el 04/11/2026 contra SonarQube 26.10.0.133078 | Comprobado: `backend/target/sonar/report-task.txt` y `frontend/.scannerwork/report-task.txt` |
| SonarQube, *quality gate* | «Passed» en los dos proyectos | Captura del autor (05/11/2026) |
| SonarQube, métricas | Ver la tabla siguiente | Captura del autor (05/11/2026) |

Métricas de SonarQube según la captura (análisis del 04/11/2026; número de incidencias abiertas y calificación):

| Proyecto | Líneas | Seguridad | Fiabilidad | Mantenibilidad | *Hotspots* revisados | Cobertura | Duplicación |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `verivoto-backend` (Java, XML) | 14k | D · 6 | C · 5 | A · 6 | A · «—» | 18,8 % | 3,5 % |
| `verivoto-frontend` (TypeScript, HTML…) | 7,1k | A · 0 | D · 12 | A · 1 | A · «—» | 0,0 % | 0,3 % |

En la escala de SonarQube, una D significa que hay al menos una incidencia de severidad alta: la D de seguridad del
backend (el filtro lateral de la captura muestra un proyecto con «≥ 1 high issue») y la D de fiabilidad del frontend. La
cobertura del frontend es 0 % porque todavía no se publica `lcov` (F1, IMP-49).

**Clasificación de las incidencias: PENDIENTE.** Sin la lista exportada no se conocen sus reglas, tipos, severidades
exactas ni causas, y no se anotan. Cuando esté exportada (pasos de abajo), cada incidencia se clasifica como real, falso
positivo o aceptada en una tabla como la de Semgrep de la línea base, y las reales se corrigen en ramas `fix/*`.

Pasar el *quality gate* no quiere decir que no haya hallazgos. El *gate* por defecto («Sonar way») mira sobre todo el
código nuevo, y en el primer análisis todavía no hay código nuevo con el que comparar. Por eso SonarQube puede dar «Passed» y a la vez
mostrar problemas, *hotspots* sin revisar o una cobertura baja. Esos datos se anotan aparte, cuando se exporten.

Para completar F5 hay que llevar los datos a `.local/` (carpeta ignorada por git) y que la IA los clasifique como
real, falso positivo o aceptado:

1. Artefactos del CI: GitHub → *Actions* → ejecución `security` de `6c30035` → *Artifacts* → descargar
   `semgrep-report` y `trivy-report` y descomprimirlos en `.local/ci/`.
2. *Quality gate*, lista de incidencias y *hotspots* de SonarQube (PowerShell, desde la raíz del repositorio y con
   `$env:SONAR_TOKEN` definido):

   ```powershell
   $h = @{ Authorization = "Bearer $env:SONAR_TOKEN" }; New-Item -ItemType Directory -Force .local\sonar | Out-Null
   foreach ($k in 'verivoto-backend', 'verivoto-frontend') {
     Invoke-RestMethod "http://localhost:9000/api/qualitygates/project_status?projectKey=$k" -Headers $h |
       ConvertTo-Json -Depth 10 | Out-File ".local\sonar\$k-gate.json" -Encoding utf8
     Invoke-RestMethod "http://localhost:9000/api/issues/search?components=$k&resolved=false&ps=500" -Headers $h |
       ConvertTo-Json -Depth 10 | Out-File ".local\sonar\$k-issues.json" -Encoding utf8
     Invoke-RestMethod "http://localhost:9000/api/hotspots/search?project=$k&ps=500" -Headers $h |
       ConvertTo-Json -Depth 10 | Out-File ".local\sonar\$k-hotspots.json" -Encoding utf8
   }
   ```

## Revisión F5 del 13/11/2026 (workflow `security` y análisis local)

**Workflow `security` en GitHub** (rama `dev`, consultado con la API de GitHub): las ejecuciones de `dev` del 11/11
terminan en verde salvo una cancelada por `concurrency` (la sustituyó la siguiente). Última completa: run `43066068128`
(11/11/2026 17:13 UTC): gitleaks, Semgrep (puerta de severidad ERROR) y Trivy + `npm audit` (puerta HIGH/CRITICAL
corregibles), los tres pasos en `success`. Los artefactos `semgrep-report` y `trivy-report` existen, pero la red de la
sesión de la IA no permite descargarlos (almacenamiento de artefactos de GitHub); siguen disponibles 30 días desde la
página del run.

**Comprobación local equivalente** (mismo commit de `dev`, `4d6c0d5`; no incluye los cambios sin commit del 12–13/11):

| Herramienta | Resultado | Clasificación |
| --- | --- | --- |
| `npm audit` (dependencias de producción y todas) | 0 vulnerabilidades | — |
| Semgrep 1.183.0 con las reglas comunitarias de `semgrep-rules` (java, typescript, generic/secrets, dockerfile, github-actions; el registro `p/…` no es accesible desde la sesión, así que **es una aproximación**, no el mismo conjunto) | 37 hallazgos, 0 de severidad ERROR | ver abajo |

- `github-actions-mutable-action-tag` ×7 (WARNING), `.github/workflows/build.yml`: Actions por etiqueta y no por SHA.
  **Real, ya registrado** como IMP-46 (pendiente de aprobación).
- `dockerfile-source-not-pinned` ×1 (INFO), `infra/keycloak/Dockerfile`: `FROM` sin *digest*. **Real, ya registrado** como IMP-48.
- `prefer-copy-over-add` ×1 (INFO), mismo Dockerfile: `ADD` de una URL. **Mitigado**: lleva `--checksum=sha256:…`, que
  Docker comprueba antes de usar el fichero.
- `i18next-key-format` ×28 (WARNING), consola y pruebas e2e: **falso positivo**; la regla es de i18next y el proyecto
  usa `@ngx-translate/core` con otro formato de claves (D-28).

Sin hallazgos nuevos respecto a la línea base. **Trivy** no se ha podido repetir en local (su base de datos se descarga de
`ghcr.io`, no accesible desde la sesión); el CI lo pasó. **SonarQube**: pendiente de exportar la lista de incidencias
cuando termine el bloque de sesiones RNF-4 (arrancarlo consume memoria del equipo que sirve las sesiones).

## SonarQube · análisis del 14/11/2026 (commit `e079eb1` + cambios sin commit del 12–14/11)

**Quality gates: ERROR en los dos proyectos**, por dos condiciones sobre el **código nuevo** (desde el 04/11): cobertura
< 80 % (backend 67,6 %; frontend 0,0 %, porque no se envía ningún informe `lcov`: IMP-49) e incidencias nuevas > 0
(backend 1, frontend 3). Duplicación del código nuevo: 0,0 % en ambos. Sin *security hotspots* pendientes. Datos leídos
en la interfaz de SonarQube por el autor; la clasificación se ha hecho revisando cada fichero y línea citados.

Clasificación: **R** = real, **FP** = falso positivo, **A** = aceptado (decisión de diseño documentada).

| Proyecto | Regla (nº) | Dónde | Clase | Motivo |
| --- | --- | --- | --- | --- |
| backend | `java:S4502` CSRF desactivado (4) | `SecurityConfiguration` de los cuatro servicios | A | API REST sin estado con token Bearer en cabecera; no hay cookies de sesión que un tercero pueda reutilizar, así que CSRF no aplica (recomendación de Spring Security para *resource servers*) |
| backend | `java:S2077` SQL dinámico (2) | `AdminActionRepositoryAdapter.java:82, 84` | FP | Solo se concatenan fragmentos JPQL fijos; todos los valores van como parámetros (`setParameter`) |
| backend | `java:S3655` `Optional.get()` (2) | `ApiKeyService.java:93`, `identity/AdminActionService.java:46` | FP | `isEmpty()` se comprueba justo antes en el mismo método; se puede reescribir con `orElseThrow` sin cambiar el comportamiento |
| backend | `java:S6218` record con array (1) | `Minutes.java:7` (`byte[] pdf`) | R (bajo) | `equals`/`hashCode`/`toString` compararían el array por referencia; hoy no se comparan actas, pero conviene sobrescribirlos |
| backend | `java:S1845` nombres que solo difieren en mayúsculas (5) | `PrivacyQuery` (`EMAIL`/`email`, `PHONE`/`phone`), `VoterImportColumns` (`LANGUAGE`/`language()`, **nueva**) | R (legibilidad) | Renombrar las constantes (`EMAIL_PATTERN`, `LANGUAGE_HEADER`…) |
| backend | `java:S8786` expresión regular costosa (2) | `PrivacyQuery.java:12` (correo) | R (mitigado) | El patrón admite retroceso polinómico, pero la entrada se limita antes a 100 caracteres |
| frontend | `typescript:S2871` `sort()` sin comparador (3) | `day-chart.component.ts:119-120`, `home-dashboard.ts:100` | FP | Ordena fechas ISO `AAAA-MM-DD`, para las que el orden de texto por defecto es el correcto |
| frontend | `typescript:S6544` promesa donde se espera `void` (4) | `ngOnInit` asíncronos y similares | A | Patrón habitual en Angular; cada método captura sus errores dentro |
| frontend | `typescript:S6959` `reduce()` sin valor inicial (1) | `home-dashboard.ts:114` | FP | La lista vacía se descarta en la línea anterior |
| frontend | `Web:InputWithoutLabelCheck` (1, **nueva**) | `census-detail.page.html:80` | R | El `input type=file` oculto no tiene nombre accesible; el botón visible es el control real: ocultarlo a la tecnología de apoyo o darle nombre |
| frontend | `Web:S6845` `tabindex` en elemento no interactivo (2, **nuevas**) | `census-detail.page.html:103, 243` | A | Regiones desplazables de tablas que deben recibir el foco con teclado (DEF-07, WCAG 2.1.1; regla `scrollable-region-focusable` de axe) |
| frontend | `typescript:S7651` (1) | `review-step.component.ts:61` | R | Salida llamada `change`, igual que un evento nativo del DOM: renombrarla |
| frontend | `typescript:S8786` expresión regular costosa (3) | `voter-sso.service.ts:116`, `app.config.ts:24`, `census-detail.page.ts:34` | A | Se aplican a datos propios (base64 aleatorio, URL de la propia API) o a un campo de longitud limitada en el cliente |

**Resumen:** de 31 incidencias, 10 reales (ninguna explotable: legibilidad, accesibilidad y un `equals` sin uso), 8 falsos
positivos y 13 aceptadas por diseño. Las 4 nuevas que rompen la condición «incidencias nuevas = 0» son 2 reales
(`S1845` en `VoterImportColumns`, `InputWithoutLabelCheck`) y 2 aceptadas (`S6845`); las aceptadas se marcan en
SonarQube como «Accepted» con el motivo de esta tabla. La cobertura del código nuevo no se alcanzará en el plazo del
TFM con el umbral por defecto (80 %): se documenta como limitación y se propone medir la del frontend (IMP-49).

### Correcciones del 14/11/2026 (las 10 reales de la tabla anterior; aprobadas por el autor)

- `VoterImportColumns`: `LANGUAGE` → `PREFERRED_LANGUAGE` (`S1845`).
- `PrivacyQuery`: `EMAIL`/`PHONE` → `EMAIL_PARTS`/`PHONE_DIGITS` (`S1845`) y comprobación del correo sin el patrón con
  retroceso (`S8786`): misma regla (dominio con un punto que no es ni el primer ni el último carácter), verificada con
  300 000 cadenas aleatorias frente al patrón anterior, sin diferencias.
- `Minutes`: `equals`, `hashCode` y `toString` propios que comparan el PDF por contenido y no lo imprimen (`S6218`).
- `census-detail.page.html`: el `input type=file` oculto, que abre el botón visible, queda fuera del teclado y de la
  tecnología de apoyo (`tabindex="-1"`, `aria-hidden="true"`) (`InputWithoutLabelCheck`).
- `review-step.component.ts`: salida `change` → `changeSelection` (`S7651`) y su uso en `voter-flow.page.html`.
- IMP-49: cobertura del frontend enviada a SonarQube (`npm run test:coverage`, `lcov`).

Comprobado en el entorno de la IA: Vitest 59/59, `ng build` y E2E 200/200. El backend (Java 25) no se puede compilar ahí:
lo comprueba `sonar.ps1`, que ejecuta `mvnw verify`. Quedan las 13 aceptadas, que el autor marca como «Accepted» en
SonarQube con el motivo de la tabla, y la condición de cobertura del código nuevo (80 %), que se documenta como limitación.

### Segundo análisis del 14/11/2026 (tras las correcciones) y cierre

Datos exportados por el autor a `.local/sonar/` (API de SonarQube). **Incidencias nuevas: backend 0 (antes 1),
frontend 3 (antes 3)**; cobertura del código nuevo: backend 64,7 %, frontend 65,3 % (antes 0,0 %, sin informe). Las
correcciones eliminaron `S6218`, `S7651`, `S8786` de `PrivacyQuery` y `S1845` de `PrivacyQuery` y `VoterImportColumns`.

Pendientes que aparecieron con la lista exportada (la tabla anterior atribuía a `PrivacyQuery` dos incidencias que en
realidad estaban también en `VoterData`) y se corrigen el mismo día:

- `VoterData.java:10-11`: `S1845` (×2) y `S8786` (×1), mismo caso que `PrivacyQuery`. La regla del correo pasa a una clase
  común del dominio, `EmailRule` (misma regla, sin retroceso), usada por `VoterData` y `PrivacyQuery`.
- `census-detail.page.html:81`: `InputWithoutLabelCheck` seguía abierta (la regla exige un nombre explícito): el `input`
  se nombra con `aria-labelledby` al título de la zona de arrastre y sigue fuera del orden de tabulación. Barridos axe de
  censos y procesos: 24/24.

Quedan **21 incidencias que el autor marca en SonarQube** (*Issues* → incidencia → cambiar estado), con el motivo de la
tabla anterior: **«False positive»** para `S2077` (2), `S3655` (2), `S2871` (3) y `S6959` (1); **«Accept»** para `S4502`
(4), `S6845` (2), `S6544` (4) y `S8786` del frontend (3). Con eso, la única condición que falla en los dos *quality
gates* es la cobertura del código nuevo (< 80 %), que se documenta como limitación del TFM sin cambiar el umbral.
