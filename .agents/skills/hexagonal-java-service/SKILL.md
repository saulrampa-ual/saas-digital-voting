---
name: hexagonal-java-service
description: Estructura y reglas de la arquitectura hexagonal de los servicios Java/Spring Boot de VeriVoto. Usar al crear un servicio, un caso de uso, un puerto, un adaptador, una entidad JPA o una migración Flyway en backend/services/.
---

# Servicio hexagonal (Java 25 + Spring Boot 4.1)

## Estructura de paquetes

Paquete base: `com.saasvoting.<servicio>` (p. ej. `com.saasvoting.identity`).

```
src/main/java/com/saasvoting/<servicio>/
├── <Servicio>Application.java
├── domain/
│   └── model/                  # agregados, value objects, enums, excepciones de dominio
├── application/
│   ├── port/in/                # casos de uso (interfaces)
│   ├── port/out/               # repositorios, clientes, relojes (interfaces)
│   └── service/                # implementación de casos de uso
└── infrastructure/
    ├── adapter/in/rest/        # controllers, DTOs request/response, exception handler
    ├── adapter/out/persistence/# entidades JPA, repositorios Spring Data, adaptador del puerto
    ├── adapter/out/client/     # clientes HTTP hacia otros servicios
    └── config/                 # @Configuration: beans de casos de uso, seguridad, Clock
src/main/resources/
├── application.yml
├── db/migration/V<n>__<descripcion>.sql
└── openapi/<servicio>.yaml
```

## Reglas de dependencia

1. `domain` no importa nada de Spring, JPA, Jakarta ni de `application`/`infrastructure`.
2. `application` solo importa `domain` y JDK. Sin anotaciones de Spring (`@Service`, `@Transactional`
   incluidas): los servicios se registran como beans en `infrastructure/config`.
3. `infrastructure` puede importar todo. Los DTOs REST y las entidades JPA nunca salen de su adaptador.
4. Un adaptador de entrada llama a un puerto `in`; nunca a un repositorio directamente.
5. Toda entidad de negocio lleva `organization_id` y todo puerto de consulta lo recibe (RNF-15).

## Estilo (ya usado en el código)

- `record` para modelos inmutables, DTOs y respuestas. Validación de invariantes en el constructor
  compacto del `record`.
- Inyección por constructor con `Objects.requireNonNull(x, "x must not be null")`.
- Mappers manuales: `TenantResponse.fromDomain(...)`, `TenantJpaEntity.fromDomain(...)`, `toDomain()`.
  Sin Lombok ni MapStruct.
- El tiempo se obtiene de un `java.time.Clock` inyectado, nunca de `Instant.now()` directo.
- Errores REST con `ProblemDetail` y mensajes en español claro (RNF-8). Excepciones de dominio
  propias, no `IllegalStateException` genérica.
- Transacciones: `@Transactional` en el adaptador de persistencia o en un decorador de
  `infrastructure`, no en `application`.
- Flyway: `V1__create_<tabla>.sql`, nombres de tablas y columnas en `snake_case`, restricciones `CHECK`
  para invariantes simples. `spring.jpa.hibernate.ddl-auto=validate`.

## Pasos para un caso de uso nuevo

1. Modelo o cambio en `domain/model`.
2. Interfaz en `application/port/in` y, si hace falta, en `application/port/out`.
3. Implementación en `application/service` (clase `final`, sin Spring).
4. Adaptador de salida en `infrastructure/adapter/out/...` que implementa el puerto.
5. Bean del caso de uso en `infrastructure/config`.
6. Controller + DTOs en `infrastructure/adapter/in/rest`, alineados con el contrato OpenAPI
   (skill `openapi-contract-first`).
7. Migración Flyway si cambia el esquema.
8. Compilar: `./mvnw -pl services/<servicio> -am -DskipTests compile`. Sin tests (ver `AGENTS.md`).

Si una API de Spring o Jakarta no es segura en la versión del proyecto, consultar la documentación
oficial o preguntar; no suponer.
