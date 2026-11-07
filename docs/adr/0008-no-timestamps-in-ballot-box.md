# ADR-0008: Sin marcas de tiempo en la urna

- Estado: Aceptada
- Fecha: 2026-10-29
- Decisiones relacionadas: D-18

## Contexto

RI-6, RI-8 y RI-9 piden hora exacta en votos, recibos y eventos, y RI-7 la hora del voto en la participación. Cruzar ambas permitiría emparejar voto y votante (RNF-11).

## Decisión

Votos, recibos y eventos de voto no guardan hora; la cadena conserva el orden por secuencia. La participación guarda solo el día. Los identificadores de voto y participación son UUID aleatorios.

## Consecuencias

Se elimina la correlación por tiempo en los datos guardados. La pantalla «Ya has votado» muestra la fecha sin hora. Queda un riesgo residual por el orden físico de escritura, declarado como límite.

## Alternativas descartadas

Horas exactas como en el TFM (correlación trivial para un insider); lotes barajados (más secreto, más complejidad y recibos que tardan en constar).
