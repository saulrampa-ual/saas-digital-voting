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
