# ADR-0007: Límites de la verificabilidad y visibilidad del resultado

- Estado: Aceptada
- Fecha: 2026-10-29
- Decisiones relacionadas: D-16

## Contexto

El TFM (sección 3.4) prohíbe llamar verificabilidad criptográfica completa al recibo y la huella. RF-13 dejaba abierto qué es «público».

## Decisión

El recibo demuestra que un voto consta en la urna, no que sea el emitido. La huella final y la del resultado son siempre públicas. Cada organización elige por proceso quién ve el resultado: `CENSUS_ONLY` (por defecto) o `PUBLIC_LINK`.

## Consecuencias

Se cumple RF-13 sin sobreprometer. La verificación del resultado consiste en recalcular el recuento a partir de la lista publicada de votos contabilizados.

## Alternativas descartadas

Verificabilidad de extremo a extremo con cifrado homomórfico (tipo Helios o Belenios): trabajo futuro (RF-23).
