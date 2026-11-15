---
name: openapi-contract-first
description: Flujo contract-first para las APIs REST de VeriVoto. Usar antes de crear o cambiar cualquier endpoint, controller o DTO REST, y al actualizar la matriz RF → endpoint.
---

# OpenAPI contract-first

## Dónde

`backend/services/<servicio>/src/main/resources/openapi/<servicio>.yaml`, en OpenAPI 3.1.

## Pasos

1. Leer el RF en `especificacion_detallada.tex` y su fila en `docs/ai/TRACEABILITY.md`.
2. Escribir o modificar el contrato **antes** que el código:
   - Rutas bajo `/api/v1/...`, recursos en plural y en inglés.
   - Esquemas de request y response separados; ningún campo que enlace identidad y voto.
   - Errores con `application/problem+json` (RFC 9457) y un esquema `Problem` común: `type`, `title`,
     `status`, `detail`. El `detail` va en español claro, sin jerga técnica (RNF-8).
   - Seguridad: esquema `bearerAuth` (JWT de Keycloak) para gestión. La API key por organización de
     RF-21 se define en P2; el nombre de la cabecera está por decidir: no inventarlo.
   - Cada operación declara el rol requerido en su `description` y el RF en `tags` o `x-rf`.
3. Actualizar `docs/ai/TRACEABILITY.md` (columna Endpoint).
4. Implementar controller y DTOs ajustados al contrato (skill `hexagonal-java-service`).
5. Comparar el contrato con el que genera springdoc en `/v3/api-docs` y corregir diferencias.

## Reglas

- Cambiar un endpoint ya publicado es un cambio de comportamiento: se indica en la respuesta.
- Los endpoints de votante (`voter-access`, `ballot-box`) no devuelven nunca datos de identidad junto
  a datos de voto. Revisar con `voting-secrecy-guard`.
