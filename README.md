# VeriVoto · Plataforma SaaS de votación digital

Trabajo Fin de Máster (Máster en Ingeniería Informática, Universidad de Almería, 2026/2027).
Autor: Saul David Ramos Pacheco · Tutor: Javier Criado Rodríguez.

Plataforma SaaS multi-tenant para votaciones internas de universidades, asociaciones y
organizaciones: segura, auditable y con API abierta.

## Estado

Fase P1 (plataforma). Existe `identity-service` (creación de organizaciones) y el entorno local de
Keycloak. El seguimiento detallado está en [`docs/ai/PROGRESS.md`](docs/ai/PROGRESS.md).

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

## Flujo de trabajo

1. Ramas `feat/*`, `fix/*` o `chore/*` desde `dev`; `dev` → `main` en cada hito.
2. Commits en inglés con Conventional Commits.
3. Nunca commitear secretos (`.env`, credenciales). Usar `.env.example`.

Las reglas para trabajar con asistentes de IA están en [`CLAUDE.md`](CLAUDE.md).
