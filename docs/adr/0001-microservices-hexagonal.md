# ADR-0001: Microservicios hexagonales: 5 servicios propios + Keycloak

- Estado: Aceptada
- Fecha: 2026-10-29
- Decisiones relacionadas: D-04, D-05

## Contexto

El TFM describe un diseño conceptual; el autor quiere microservicios con arquitectura hexagonal. Es un TFM individual con unas 9 semanas de desarrollo.

## Decisión

Cinco servicios propios (`api-gateway`, `identity`, `election` con censo, `voter-access` con notificaciones, `ballot-box` con escrutinio) y Keycloak. Cada servicio de negocio sigue la plantilla hexagonal (`docs/architecture/hexagonal-template.md`). El `tenant-service` existente se conserva como `identity-service`.

## Consecuencias

Menos bases de datos, contratos y llamadas que con 8 servicios. Se conserva la separación física entre participación y urna, que es el argumento central del secreto del voto.

## Alternativas descartadas

Monolito modular (más simple, pero no responde a lo pedido); 8 servicios (census, tally y notification separados: más operación sin beneficio para el MVP).
