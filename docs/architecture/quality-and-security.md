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
