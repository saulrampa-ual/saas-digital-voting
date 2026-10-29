# Plantilla hexagonal de los servicios

Todos los servicios de negocio (`identity`, `election`, `voter-access`, `ballot-box`) siguen la misma
estructura. `api-gateway` no la sigue: es solo infraestructura (enrutado, CORS, límites, API keys) y
no tiene dominio.

## Paquetes

| Paquete | Contenido | Puede depender de |
| --- | --- | --- |
| `domain.model` | Agregados, value objects, reglas, excepciones de dominio | Solo JDK |
| `application.port.in` | Interfaces de casos de uso | `domain` |
| `application.port.out` | Interfaces de persistencia, clientes de otros servicios, reloj | `domain` |
| `application.service` | Implementación de los casos de uso, sin anotaciones de Spring | `domain`, `application.port` |
| `infrastructure.adapter.in.rest` | Controllers, DTOs, manejo de errores (`ProblemDetail`) | Todo |
| `infrastructure.adapter.out.persistence` | Entidades JPA, repositorios Spring Data, implementación de puertos | Todo |
| `infrastructure.adapter.out.client` | Clientes HTTP hacia otros servicios (election, ballot-box) | Todo |
| `infrastructure.config` | Beans de casos de uso, seguridad, OpenAPI, `Clock` | Todo |

Cada paquete vacío lleva un `package-info.java` que documenta su responsabilidad.

## Reglas

1. Las dependencias apuntan hacia dentro: `infrastructure` → `application` → `domain`.
2. Los DTOs REST y las entidades JPA no salen de su adaptador; se convierten con mappers manuales.
3. Los casos de uso se registran como beans en `infrastructure.config`, no con `@Service`.
4. Toda entidad de negocio lleva `organization_id`.

## Elementos comunes de cada servicio

| Elemento | Valor |
| --- | --- |
| Paquete base | `com.saasvoting.<servicio>` (`identity`, `election`, `voteraccess`, `ballotbox`) |
| Health | `/actuator/health` (Spring Boot Actuator, solo `health` expuesto) |
| OpenAPI generado | `/v3/api-docs` y `/swagger-ui.html` (springdoc 3.1.1) |
| Contrato fuente | `src/main/resources/openapi/<servicio>.yaml` (desde P2) |
| Migraciones | `src/main/resources/db/migration/V<n>__<descripcion>.sql` (Flyway) |
| Configuración | `application.yml` con variables `<SERVICIO>_SERVER_PORT`, `<SERVICIO>_DB_URL`, `<SERVICIO>_DB_USERNAME`, `<SERVICIO>_DB_PASSWORD` |

| Servicio | Puerto | BD por defecto |
| --- | --- | --- |
| `api-gateway` | 8000 | — |
| `identity-service` | 8081 | `localhost:5432/identity_db` |
| `election-service` | 8082 | `localhost:5432/election_db` |
| `voter-access-service` | 8083 | `localhost:5432/voter_access_db` |
| `ballot-box-service` | 8084 | `localhost:5433/ballot_box_db` (`pg-ballot`) |

La regla de dependencias se comprobará con ArchUnit cuando se activen las pruebas (IMP-08).
