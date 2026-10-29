# ADR-0004: Multi-tenancy por `organization_id`

- Estado: Aceptada
- Fecha: 2026-10-29
- Decisiones relacionadas: D-17

## Contexto

RNF-15 exige que ningún usuario acceda a datos de otra organización.

## Decisión

Base de datos compartida por servicio con columna `organization_id` en todas las tablas de negocio. Su valor es el identificador de RI-1 (`VARCHAR(20)`). El JWT de Keycloak lo publica en el claim `organization_id` (nulo para el Superadministrador). Toda consulta filtra por él en el adaptador de persistencia.

## Consecuencias

Simple y suficiente para el MVP. El aislamiento depende del filtro en el código: se verificará con pruebas de acceso cruzado (P7).

## Alternativas descartadas

Esquema o base de datos por organización: más aislamiento, mucha más operación.
