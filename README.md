@'
# SaaS de votación digital

Plataforma SaaS de votación digital. Estado: **estructura inicial**, sin código de aplicación todavía.

## Stack objetivo

- **Backend**: Java + Spring Boot (versión por confirmar: JDK 17 ya instalado, JDK 25 según plan).
- **Frontend**: Angular (versión por confirmar; se aborda al final).
- **Base de datos**: PostgreSQL 18 en Docker.
- **Infraestructura**: Docker / Docker Compose.
- **CI**: GitHub Actions.

## Estructura del repositorio

- `backend/` — aplicación Spring Boot (pendiente).
- `frontend/` — aplicación Angular (pendiente).
- `infra/` — Docker Compose y configuración de infraestructura (pendiente).
- `scripts/` — utilidades de desarrollo (pendiente).
- `docs/` — documentación del proyecto.
    - `architecture/` — decisiones de arquitectura.
    - `requirements/` — requisitos y especificación.
    - `security/` — políticas y análisis de seguridad.
    - `adr/` — Architecture Decision Records.

## Documentos de referencia

- `Informe plataforma SaaS de votación digital.md`
- `Proyecto_plataforma_SaaS_de_votacion_digital.pdf.md`

## Estado actual

Repositorio inicializado. Ramas `main` y `dev` creadas. Carpetas de alto nivel creadas. Sin archivos de aplicación todavía.

## Cómo contribuir (provisional)

1. Trabajar sobre `dev`.
2. Commits pequeños y descriptivos.
3. No commitear secretos (`.env`, credenciales). Usar `.env.example` cuando exista.
   '@ | Out-File -FilePath README.md -Encoding utf8