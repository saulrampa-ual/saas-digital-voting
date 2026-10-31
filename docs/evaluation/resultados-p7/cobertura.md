# Cobertura P7

JaCoCo 0.8.16 se generó en la misma pasada final de `mvnw verify` (Java 25.0.5, 31/10/2026). Los porcentajes se recalcularon sumando `LINE_COVERED / (LINE_COVERED + LINE_MISSED)` y `INSTRUCTION_COVERED / (INSTRUCTION_COVERED + INSTRUCTION_MISSED)` de cada CSV, con una fila por clase y agregado por servicio. Esto permite comparar los servicios con la misma métrica y regla de agregación.

| Servicio | Líneas cubiertas | Instrucciones cubiertas | CSV |
| --- | ---: | ---: | --- |
| api-gateway | 0,0 % (0/3) | 0,0 % (0/8) | `jacoco-api-gateway.csv` |
| identity-service | 13,8 % (106/768) | 14,3 % (600/4206) | `jacoco-identity.csv` |
| election-service | 37,2 % (568/1528) | 34,0 % (2809/8260) | `jacoco-election.csv` |
| voter-access-service | 7,7 % (115/1499) | 8,7 % (746/8590) | `jacoco-voter-access.csv` |
| ballot-box-service | 13,2 % (213/1611) | 12,4 % (1328/10674) | `jacoco-ballot-box.csv` |

La referencia previa de `docs/evaluation/informe.md` consignaba cobertura de líneas de 0,0 %, 13,6 %, 10,3 %, 7,7 % y 13,2 % respectivamente. En esta pasada, gateway, voter-access y ballot-box mantienen el mismo porcentaje a un decimal; election pasa a 37,2 % por las pruebas nuevas de CSV, importación y HTTP. identity pasa de 13,6 % (107/785) a 13,8 % (106/768) sin cambios en su código ni en sus pruebas: la diferencia de denominador procede de la medición inicial y queda PENDIENTE de aclarar; esta tabla usa la ejecución final homogénea. No se interpreta la cobertura global de election como cobertura de cada flujo: aún faltan muchas áreas funcionales señaladas en el encargo.
