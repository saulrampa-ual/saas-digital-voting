# ADR-0005: Separación entre participación y urna, y emisión del voto

- Estado: Aceptada
- Fecha: 2026-10-29
- Decisiones relacionadas: D-08, D-20

## Contexto

RNF-11 exige que nada enlace voto e identidad; RNF-12, un único voto contabilizable por votante incluso con envíos simultáneos.

## Decisión

Participación en `voter_access_db` (pg-main) y urna en `ballot_box_db` (pg-ballot), sin roles comunes. Tras el OTP, voter-access emite un token de voto de un solo uso. Ballot-box pide a voter-access consumirlo (transacción atómica que marca al votante) y después guarda voto, recibo y evento; si falla, revierte el consumo. Ballot-box no guarda nada derivado del token. Detalle en `data-model.md`, sección 6.2.

## Consecuencias

Ningún voto duplicado. Riesgo aceptado: si fallan el guardado y la reversión, el votante queda marcado sin voto en la urna, y se registra una incidencia.

## Alternativas descartadas

Firma ciega o credenciales anónimas: más secreto, pero exigen criptografía fuera del alcance del MVP (RF-23).
