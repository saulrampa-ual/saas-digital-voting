# Entorno de pruebas P7

Fecha de ejecución: 31/10/2026. Rama: `test/p7-pending-suite`. Commit base: `cc0229b`; no se han creado commits.

| Elemento | Valor verificado |
| --- | --- |
| Sistema | Windows 11, amd64 |
| CPU | AMD Ryzen 7 5825U with Radeon Graphics; 8 núcleos / 16 hilos |
| RAM física | 15,4 GiB |
| Docker Desktop | 29.4.0; daemon con 7 614 MiB disponibles para contenedores |
| Java | Eclipse Temurin 25.0.5 (seleccionado por proceso; el `PATH` global apunta a Java 17.0.18) |
| Maven | Wrapper del repositorio; Maven 3.9.16 |
| Node.js / npm | 24.15.0 / 11.12.1 |
| PostgreSQL | 18.6 en los cuatro contenedores Testcontainers ejecutados por `mvnw verify` |
| Playwright / navegador | `@playwright/test` 1.64.1; Chromium for Testing 156.0.8078.4 |
| axe | `@axe-core/playwright` 4.14.0 |
| Keycloak / Mailpit | Contenedores locales estaban sanos, pero no se consultaron sus versiones de imagen en esta ejecución |
| k6 | No instalado (`k6` no se reconoce como comando) |

Los cuatro contenedores locales de infraestructura estaban activos (PostgreSQL principal, PostgreSQL de urna, Keycloak y Mailpit). Los cinco servicios VeriVoto y el gateway no estaban activos; por eso no se ejecutó el E2E de integración contra el stack real ni se usó Mailpit.

Dependencias de Playwright/axe fijadas en `frontend/package.json` y `frontend/package-lock.json`. Chromium está instalado en la caché local de Playwright, fuera del repositorio.

## Fase 2 · Entorno Windows (31/10/2026)

Rama: `test/p7-phase2`; base: `acda120` (incluye `b1b68e6`). SO: Windows 11 Home 10.0.26300, amd64. CPU: AMD Ryzen 7 5825U, 8 núcleos / 16 hilos. RAM física: 15,4 GiB; Docker Desktop asigna 7,4 GiB al daemon.

| Elemento | Versión comprobada en esta fase |
| --- | --- |
| Java | Eclipse Temurin 25.0.5 (usado por proceso) |
| Maven | Wrapper, Apache Maven 3.9.16 |
| Node.js / npm | 24.15.0 / 11.12.1 |
| Docker / Compose | 29.4.0 / 5.1.1 |
| PostgreSQL | 18.6 (ambas imágenes de Compose) |
| Keycloak | 26.8.1 (etiqueta de imagen e inicio del servidor) |
| Mailpit | v1.31.5 (binario dentro del contenedor) |
| Playwright / Chromium | 1.64.1 / 156.0.8078.4 |
| axe | `@axe-core/playwright` 4.14.0 |
| k6 | 2.3.0 (instalado con WinGet; `GrafanaLabs.k6`) |
| Navegador | Chromium for Testing, caché local de Playwright |

Comandos de versión: `backend/mvnw.cmd --version` con `JAVA_HOME` de Temurin 25; `node --version`; `npm --version`; `docker --version`; `docker compose version`; `docker exec verivoto-p7-phase2-mailpit-1 /mailpit version`; `docker exec verivoto-p7-phase2-keycloak-1 /opt/keycloak/bin/kc.sh --version`; `k6 version`; versión de Chromium consultada con `browser.version()` de Playwright.

La infraestructura de la fase se ejecuta como proyecto Compose `verivoto-p7-phase2`, con volúmenes nuevos. Los volúmenes preexistentes del proyecto `verivoto` quedaron detenidos y preservados; no se ejecutó `down -v`. `infra/.env` contiene credenciales aleatorias locales y está ignorado por Git. Los cinco servicios de aplicación y el gateway se iniciaron en procesos locales; su health y los flujos integrados se registran en `informe-ejecucion.md`.

**Nota del autor (01/11/2026):** al preparar esta fase se cambiaron las contraseñas de los roles de PostgreSQL de los volúmenes del proyecto original `verivoto` para que coincidieran con un `infra/.env` nuevo. Ese entorno ya no es coherente con el `.env` actual (Keycloak conserva el administrador y los secretos de cliente de su importación); queda parado y sus datos se conservan. Las pruebas siguientes usan `verivoto-p7-phase2`.
