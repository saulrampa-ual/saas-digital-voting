# Informe de evaluación P7

> Estado: **parcial; resultados pendientes**. Este informe recoge solo evidencias obtenidas en ejecuciones reales.
> No sustituye los informes de accesibilidad, carga ni las sesiones moderadas pendientes.

## Entorno y reproducibilidad

| Elemento | Configuración / evidencia |
| --- | --- |
| Commit evaluado | Rama `chore/p7-p8-tfm-work`, base `5fa854f`; cambios sin commit |
| Sistema operativo y hardware | Windows; hardware exacto PENDIENTE |
| Java / Maven / PostgreSQL / Docker | Temurin 25.0.5; Maven Wrapper del proyecto; PostgreSQL y Docker no usados en esta ejecución |
| Configuración de servicios | PENDIENTE |
| Ejecución unitaria | `backend`: `mvnw --batch-mode --no-transfer-progress verify`; BUILD SUCCESS |
| Datos | Sintéticos; no registrar ni publicar identidades, códigos, tokens o recibos completos |

## Hipótesis

| Hipótesis | Método y criterio | Resultado real | Decisión |
| --- | --- | --- | --- |
| H1a: accesibilidad del flujo | Playwright + axe en pantallas del votante y gestión; cero incidencias críticas o serias; completar checklist manual. axe no certifica WCAG. | PENDIENTE: flujo E2E y barrido axe no ejecutados. | Pendiente |
| H1b: usabilidad | Al menos 5 personas no técnicas; ≥90 % completa sin ayuda en ≤3 min y ≥80 % interpreta la verificación. Materiales en `docs/evaluation/usabilidad/`. | PENDIENTE: las sesiones las realiza el autor. | Pendiente |
| H1c: rendimiento | 500 emisores simultáneos; p95 de emisión <2 s; tres ejecuciones reproducibles y errores/recursos documentados. | PENDIENTE: carga no ejecutada. | Pendiente |

## Evidencia técnica por servicio

| Servicio | Evidencia | Resultado |
| --- | --- | --- |
| api-gateway | 1 prueba ArchUnit | 1 pasada; cobertura de líneas 0,0 % (0/3) |
| identity-service | `CallerTest`, `ManagementUserServiceTest`, `ApiKeyServiceTest`, `ArchitectureTest` | 11 pasadas; cobertura de líneas 13,6 % (107/785) |
| election-service | `CallerTest`, `ProcessStatusTest`, `VotingProcessTest`, `ArchitectureTest` | 9 pasadas; cobertura de líneas 10,3 % (157/1528) |
| voter-access-service | `AccessCredentialTest`, `VotingTokenServiceTest`, `ArchitectureTest` | 8 pasadas; cobertura de líneas 7,7 % (115/1499) |
| ballot-box-service | `VotingServiceTest`, `ChainVerifierTest`, `TallyResultTest`, `ArchitectureTest` | 16 pasadas; cobertura de líneas 13,2 % (212/1611) |
| frontend | Vitest: `role.guard.spec.ts`, `voter-api.service.spec.ts`, `voter-sso.service.spec.ts` | 10 pasadas; no se ha medido cobertura |

Backend: 45 pruebas, 0 fallos y 0 errores en la última ejecución completa `verify`. Frontend: 10 pruebas, 0 fallos;
`npm run test:ci` y `npm run build` completaron correctamente. Total conjunto: 55 pruebas. Informes JaCoCo:
`backend/services/<servicio>/target/site/jacoco/index.html`.
Las cifras reflejan solo esta batería unitaria inicial; el gateway no contiene lógica ejercitada en estas pruebas.
No se asigna umbral de cobertura en esta fase.

## RNF e ISO/IEC 25010:2023

| RNF | Característica / subcaracterística (mapeo de `rnf_iso25010.tex`) | Evidencia prevista | Estado |
| --- | --- | --- | --- |
| RNF-1 | Capacidad de interacción; Inclusivity, User assistance | axe, revisión manual con teclado, foco, lector de pantalla, zoom y contraste | Pendiente |
| RNF-3 | Capacidad de interacción; Learnability, Operability | Sesiones moderadas H1b | Pendiente |
| RNF-6 | Capacidad de interacción; Self-descriptiveness | Pregunta de comprensión del recibo | Pendiente |
| RNF-11 | Seguridad; Confidentiality | Revisión de esquema, logs, interfaces y separación de bases | Parcial: `VotingServiceTest` comprueba token consumido por separado, evento sin hora y hash del recibo; esquema, logs y persistencia siguen pendientes |
| RNF-12 | Seguridad y fiabilidad; Integrity, Faultlessness | Envíos concurrentes con token único en PostgreSQL | Pendiente |
| RNF-13 | Seguridad; Integrity, Accountability, Non-repudiation | Alteraciones de eventos, verificación de cadena y registros solo añadibles | Parcial: pruebas de dominio en curso |
| RNF-14 | Seguridad; Authenticity, Resistance, Confidentiality | Límites de código, bloqueo, cuenta institucional, permisos y claves API | Pendiente |
| RNF-15 | Seguridad; Confidentiality | Dos organizaciones en pruebas de repositorio y autorización | Parcial: reglas de Caller, acceso a persistencia pendiente |
| RNF-17 | Eficiencia de desempeño; Time behaviour, Capacity | k6: emisión, acceso y escrutinio; tres ejecuciones | Pendiente |
| RNF-18 | Flexibilidad; Installability, Adaptability | Instalación PWA y comprobación en tamaños/navegadores | Pendiente |

La correspondencia resume el mapeo del TFM; no constituye certificación de conformidad con la norma.

## Limitaciones y amenazas a la validez

Las pruebas unitarias cubren reglas aisladas y no demuestran por sí solas integración, aislamiento de persistencia,
secreto del voto ni comportamiento bajo carga. La accesibilidad automatizada no sustituye la evaluación manual.
La usabilidad depende de una muestra pequeña y autoseleccionada. Los resultados de carga dependen del hardware,
contenedores, configuración y preparación de datos; se deben informar sin generalizarlos a producción.

## Pendiente de completar

- Ejecuciones completas de integración con PostgreSQL 18.6 y verificación de aislamiento entre organizaciones.
- E2E con Keycloak y Mailpit reales en 1280 px y 360 px, idiomas ES/EN y axe en flujos acordados.
- Tres mediciones k6 con datos sintéticos, percentiles, errores, CPU/RAM y comprobación posterior de cadena y participación.
- Sesiones moderadas, resultados agregados y cobertura JaCoCo por servicio.
- Actualizar decisiones de H1a/H1b/H1c y trazabilidad solo cuando se disponga de dichas evidencias.
