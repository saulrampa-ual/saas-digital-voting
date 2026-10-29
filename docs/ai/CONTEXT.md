# CONTEXT · VeriVoto

Documento autocontenido: quien lo lea sin más contexto debe entender el proyecto.

## Qué es

VeriVoto es una plataforma SaaS multi-tenant de votación digital segura y auditable, con API abierta,
para votaciones internas de universidades, asociaciones y organizaciones. Es el Trabajo Fin de Máster
de Saul David Ramos Pacheco (Máster en Ingeniería Informática, Universidad de Almería, curso
2026/2027), tutorizado por Javier Criado Rodríguez.

Fuentes de verdad: `Plantilla-TFM-Informatica-UAL/especificacion_detallada.tex` y `main.pdf`.
Los prototipos de `prototiposPantallas/` son referencia funcional y ceden ante el TFM.

## Actores (TFM, tabla de actores)

| ID | Actor | Responsabilidad |
| --- | --- | --- |
| ACT-1 | Superadministrador | Organizaciones y configuración global |
| ACT-2 | Administrador de organización | Usuarios, procesos y configuración de su organización |
| ACT-3 | Auditor | Registros, información y resultados electorales |
| ACT-4 | Votante | Accede a la papeleta, vota y consulta las evidencias permitidas |
| ACT-5 | Gestor de censo | Participantes, grupos y censos |
| ACT-6 | Proveedor de identidad (externo) | Autenticación institucional OIDC |
| ACT-7 | Servicio de notificaciones (externo) | Envío de correos |

## Alcance del MVP

Incluidos: RF-01 a RF-10 (con RF-05a y RF-05b) y RF-12 a RF-21.
Fuera: RF-11 (revoto, se retomará tras el MVP), RF-22 (métodos avanzados), RF-23 (cifrado en
cliente), webhooks y SMS. El recibo y la huella encadenada no se presentan como verificabilidad
criptográfica completa.

## Arquitectura

| Servicio | RF | Base de datos | Puerto local |
| --- | --- | --- | --- |
| `api-gateway` | RF-21 (enrutado, CORS, límites, validación de API key) | — | 8000 |
| `identity-service` (antes `tenant-service`) | RF-01, RF-02, RF-18, RF-19, RF-21 (emisión de claves) | `identity_db` en `pg-main` | 8081 |
| Keycloak 26.8.1 | RF-04, RF-05a por configuración | `keycloak_db` en `pg-main` | 8080 |
| `election-service` | RF-03, RF-06, RF-07, RF-20 | `election_db` en `pg-main` | 8082 |
| `voter-access-service` | RF-05b, RF-08, RF-14, RF-17 | `voter_access_db` en `pg-main` | 8083 |
| `ballot-box-service` | RF-09, RF-10, RF-12, RF-13, RF-15, RF-16 | `ballot_box_db` en `pg-ballot` (puerto 5433) | 8084 |
| `frontend` (Angular 22 + Ionic 9, PWA) | Pantallas | — | 4200 |

Reglas estructurales:
- Hexagonal por servicio; el dominio no depende de Spring.
- REST síncrono entre servicios. Llamadas previstas: election → voter-access (instantánea del censo
  congelado al abrir el proceso); ballot-box → voter-access (consumo del token de voto de un solo uso).
- `voter-access` (participación, RI-7) y `ballot-box` (urna, RI-6) no comparten proceso, base de
  datos ni instancia PostgreSQL (RNF-11).
- Toda entidad de negocio lleva `organization_id` (RNF-15).
- Los usuarios de gestión se autentican en Keycloak; el votante entra con enlace personal + OTP
  gestionados por `voter-access` (no es usuario de Keycloak).

## Stack

Java 25 LTS · Spring Boot 4.1.2 · Spring Cloud 2025.1.3 (Gateway 5.0.3) · PostgreSQL 18.6 · Flyway ·
springdoc-openapi 3.1.x · Keycloak 26.8.1 + Email OTP v1.8.1 · Mailpit v1.31.5 (local) ·
Angular 22.x · Ionic 9.x · Node.js 24 LTS · TypeScript 6.0.x · Docker Compose · GitHub Actions.

## Restricciones

- Pruebas bloqueadas hasta `ACTIVAR TESTS` (ver `CLAUDE.md`).
- Calendario del Gantt del TFM: desarrollo hasta el 31/12/2026; pruebas cerradas el 06/01/2027 (H4);
  entrega al director el 18/01/2027 (H5); entrega oficial el 27/01/2027.
- Entorno de desarrollo: Windows, IntelliJ IDEA, VS Code, Docker Desktop.
- Repositorio: `github.com/saulrampa-ual/saas-digital-voting` (ramas `main` y `dev`).
