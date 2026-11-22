# VeriVoto · Plataforma SaaS de votación digital

Trabajo Fin de Máster (Máster en Ingeniería Informática, Universidad de Almería, 2026/2027).
Autor: Saul David Ramos Pacheco · Tutor: Javier Criado Rodríguez.

Plataforma SaaS multi-tenant para votaciones internas de universidades, asociaciones y
organizaciones: segura, auditable y con API abierta.

## Estado

El MVP está implementado y las fases P1–P7 están cerradas. La fase P8 (memoria del TFM) está en curso.
El seguimiento detallado está en [`docs/ai/PROGRESS.md`](docs/ai/PROGRESS.md).

## Stack

| Capa | Tecnología |
| --- | --- |
| Backend | Java 25 LTS, Spring Boot 4.1.2, Spring Cloud 2025.1.3, arquitectura hexagonal por servicio |
| Identidad (gestión) | Keycloak 26.8.1 con Email OTP |
| Datos | PostgreSQL 18.6, Flyway |
| Frontend | Angular 22 + Ionic 9 (PWA) |
| Infraestructura | Docker Compose |
| CI | GitHub Actions (solo compilación por ahora) |

## Arquitectura del MVP

| Servicio | Responsabilidad |
| --- | --- |
| `api-gateway` | Entrada única, CORS, límites, claves de API |
| `identity-service` | Organizaciones, roles, RGPD, trazabilidad administrativa |
| `election-service` | Censo, procesos, papeleta, ciclo de vida |
| `voter-access-service` | Acceso del votante, OTP, token de voto, participación, notificaciones |
| `ballot-box-service` | Urna, recibos, registro encadenado, escrutinio, actas |

La participación (`voter-access`) y la urna (`ballot-box`) nunca comparten base de datos.

## Estructura del repositorio

- `backend/services/` — servicios Spring Boot.
- `frontend/` — aplicación Angular + Ionic (pendiente).
- `infra/` — Docker Compose y Keycloak.
- `docs/` — arquitectura, ADR, seguridad, requisitos y memoria de trabajo con IA (`docs/ai/`).
- `prototiposPantallas/` — prototipos de pantallas (referencia del frontend; no modificar).
- `Plantilla-TFM-Informatica-UAL/` — memoria del TFM en LaTeX.
- `scripts/` — utilidades de desarrollo.

## Requisitos para desarrollo local

- Docker Desktop con Docker Compose v2.
- Java 25 (el backend usa Maven Wrapper incluido en `backend/`).
- Node.js `^22.22.3 || ^24.15.0 || ^26.0.0` y npm 10.9.9 (frontend).
- Conexión a Internet en el primer arranque para descargar imágenes, dependencias y, para LibreTranslate,
  modelos de traducción.

## Inicio rápido

1. Desde la raíz del repositorio, copia la plantilla local y completa **todos** sus valores vacíos con
   contraseñas aleatorias distintas. No compartas ni publiques `infra/.env`:

   ```powershell
   Copy-Item infra/.env.example infra/.env
   ```

2. Arranca PostgreSQL, Keycloak, Mailpit y LibreTranslate:

   ```powershell
   docker compose -f infra/compose.yaml --env-file infra/.env up -d --build
   docker compose -f infra/compose.yaml --env-file infra/.env ps
   ```

   Keycloak importa el realm `verivoto` en el primer arranque. El administrador inicial de VeriVoto
   utiliza `VERIVOTO_SUPERADMIN_EMAIL` y `VERIVOTO_SUPERADMIN_INITIAL_PASSWORD`; Keycloak pedirá cambiar
   la contraseña inicial. Para ver los correos locales, abre `http://localhost:8025`.

3. En cinco terminales PowerShell separadas, inicia los servicios backend siguiendo
   [`backend/README.md`](backend/README.md).
4. En otra terminal, inicia el frontend siguiendo [`frontend/README.md`](frontend/README.md).
5. Abre `http://localhost:4200/admin`. El acceso de gestión se autentica en Keycloak. Para crear cuentas
   sintéticas y conocer los permisos de cada rol, consulta el
   [Manual de roles y simulación](docs/evaluation/resultados-p7/manual-simulacion-roles.md).

### Direcciones locales

| Componente | Dirección |
| --- | --- |
| Consola web | `http://localhost:4200/admin` |
| API gateway | `http://localhost:8000` |
| Keycloak | `http://localhost:8080` |
| Mailpit (buzón de pruebas) | `http://localhost:8025` |
| LibreTranslate | `http://localhost:5000` |
| PostgreSQL principal | `localhost:5432` |
| PostgreSQL de la urna | `localhost:5433` |

La guía de [infraestructura local](docs/architecture/local-infrastructure.md) explica los puertos,
volúmenes y comprobaciones. Para probar contratos y recorridos completos, sigue
[`docs/architecture/api-testing.md`](docs/architecture/api-testing.md). Los volúmenes de PostgreSQL
conservan los datos locales al detener Compose; `docker compose down -v` los elimina.

## Flujo de trabajo

1. Ramas `feat/*`, `fix/*` o `chore/*` desde `dev`; `dev` → `main` en cada hito.
2. Commits en inglés con Conventional Commits.
3. Nunca commitear secretos (`.env`, credenciales). Usar `.env.example`.

Las reglas para trabajar con asistentes de IA están en [`CLAUDE.md`](CLAUDE.md).
