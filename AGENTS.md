# AGENTS.md · VeriVoto (TFM)

Reglas para cualquier asistente de IA que trabaje en este repositorio. Léelo entero antes de cada tarea,
junto con `docs/ai/PROGRESS.md`.

## 1. Proyecto

Plataforma SaaS multi-tenant de votación digital segura y auditable, con API abierta, para votaciones
internas de universidades, asociaciones y organizaciones. Es el TFM de Saul David Ramos Pacheco
(Máster en Ingeniería Informática, UAL, 2026/2027; tutor Javier Criado Rodríguez).

**Fuentes de verdad, por orden de prioridad:**
1. `Plantilla-TFM-Informatica-UAL/especificacion_detallada.tex` y `main.pdf` (memoria del TFM).
2. `docs/ai/DECISIONS.md`.
3. `prototiposPantallas/` (referencia funcional y visual; cede ante el TFM si hay conflicto).

**Alcance del MVP** (numeración RF-01..RF-23 del TFM): RF-01 a RF-10 (incluye RF-05a y RF-05b),
RF-12 a RF-21. **Fuera del MVP:** RF-11 (revoto), RF-22, RF-23, webhooks, SMS.

## 2. Arquitectura y stack

- 5 servicios propios + Keycloak (decisión D-05): `api-gateway`, `identity-service`, `election-service`,
  `voter-access-service`, `ballot-box-service`. Detalle en `docs/ai/CONTEXT.md`.
- Hexagonal por servicio (skill `hexagonal-java-service`). El dominio no depende de Spring.
- Comunicación REST síncrona entre servicios. Sin Kafka en el MVP.
- Cada servicio tiene su propia base de datos. `ballot-box` usa una instancia PostgreSQL aparte.
- Keycloak 26.8.1 autentica a los usuarios de gestión (RF-04, RF-05a); los servicios son resource
  servers JWT. El acceso del votante (enlace + OTP) lo implementa `voter-access-service`, no Keycloak.

| Tecnología | Versión |
| --- | --- |
| Java | 25 LTS |
| Spring Boot | 4.1.2 |
| Spring Cloud | 2025.1.3 |
| PostgreSQL | 18.6 |
| Keycloak | 26.8.1 |
| Angular / Ionic | 22.x / 9.x |
| Node.js / TypeScript | 24 LTS / 6.0.x |

Las dependencias gestionadas por los BOM de Spring Boot o Spring Cloud no llevan versión en el pom.
No se usan Lombok ni MapStruct: `record` y mappers manuales.

## 3. Reglas duras

1. **Pruebas activadas para P7.** El autor autorizó `ACTIVAR TESTS` en el prompt de continuación de VeriVoto
   confirmado el 2026-10-31. Se permiten pruebas unitarias, de integración, e2e y carga dentro del alcance
   de P7. Mantener datos sintéticos, respetar el secreto del voto y no presentar resultados que no procedan
   de ejecuciones reales. Las tareas fuera de P7 siguen necesitando autorización específica.
2. **Secreto del voto (RNF-11).** Nada enlaza voto e identidad: ni tablas, ni claves, ni logs, ni
   identificadores, ni marcas de tiempo u orden que permitan correlacionar participación y urna.
   Revisar con la skill `voting-secrecy-guard` cualquier cambio en `voter-access` o `ballot-box`.
3. **Multi-tenant.** Toda entidad de negocio lleva `organization_id` y toda consulta filtra por él.
4. **Honestidad técnica.** No inventar clases, anotaciones, APIs ni sintaxis. Si falta contexto o hay
   duda sobre una API o versión, preguntar o consultar la documentación oficial.
5. **Transparencia.** Cada respuesta incluye una sección «Asunciones y cambios de comportamiento».
6. **Verificabilidad.** El recibo y la huella encadenada demuestran presencia e integridad; no se
   llaman «verificabilidad criptográfica completa» (TFM, sección 3.4).
7. **Alcance.** Trabajar solo en lo que pide la tarea. Lo que quede fuera se anota en
   `docs/ai/PROGRESS.md` (sección «Pendiente») sin implementarlo.
8. **Mejoras.** Las mejoras detectadas (arquitectura, seguridad, UX, accesibilidad, rendimiento,
   alcance) se registran en `docs/ai/IMPROVEMENTS.md`; nunca se implementan sin aprobación.
9. **Prototipos.** Antes de cualquier trabajo de UI, leer `docs/ai/PROTOTYPES.md` y
   `prototiposPantallas/`. No modificar esa carpeta. Si un prototipo contradice un RF/RNF, la
   accesibilidad o la arquitectura, avisar en vez de resolverlo.
10. **Secretos.** Nunca escribir secretos reales; usar `.env.example`.

## 4. Git

- Ramas `feat/*`, `fix/*`, `chore/*`, `docs/*` desde `dev`; `dev` → `main` al cerrar cada hito.
- Código, ramas y commits en inglés. Commits con Conventional Commits (`feat:`, `fix:`, `chore:`,
  `docs:`, `refactor:`, `build:`, `ci:`).
- No ejecutar `git push`, `reset --hard`, `rebase` ni otros comandos destructivos sin petición expresa.
- La documentación de proyecto y la memoria se escriben en español.

## 5. Memoria del proyecto (`docs/ai/`)

| Fichero | Uso |
| --- | --- |
| `CONTEXT.md` | Qué es el proyecto; autocontenido |
| `PROGRESS.md` | Estado por hito y siguiente paso exacto |
| `PLAN.md` | Lo que falta hasta la entrega: tareas, preguntas al autor y calendario |
| `DECISIONS.md` | Decisiones cerradas (D-xx) |
| `TRACEABILITY.md` | RF → servicio → endpoint → pantalla → estado |
| `IMPROVEMENTS.md` | Mejoras propuestas pendientes de aprobación |
| `PROTOTYPES.md` | Inventario de `prototiposPantallas/` |

Al cerrar una sesión de trabajo, actualizar estos ficheros con la skill `project-progress-log`.

## 6. Al terminar cada tarea

Resumir: qué se hizo, asunciones y cambios de comportamiento, qué queda pendiente, mejoras
registradas y comandos que debe ejecutar el autor.
