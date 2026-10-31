# Diagnóstico P8.0 de la memoria

Fecha: 2026-10-31. Documento de revisión; **las propuestas siguientes no se han aplicado**. El texto actual presenta
el TFM como especificación y diseño conceptual, mientras que el repositorio ya contiene un MVP implementado. El
cambio de enfoque debe aprobarlo el autor antes de reescribir esos pasajes.

## Pasajes que requieren revisión

| Ubicación | Enfoque actual | Reescritura propuesta (sin aplicar) |
| --- | --- | --- |
| `Plantilla-TFM-Informatica-UAL/contenido1.tex`, introducción y OBJ/OE | La especificación es la base de un análisis y diseño futuros; OBJ/OE describen sobre todo tareas de diseño. | Presentar como artefacto evaluado el MVP implementado (cinco servicios y PWA), conservar los objetivos aprobados y añadir cómo se materializaron; no afirmar resultados aún no medidos. |
| `contenido1.tex`, contribución académica | OIDC/SSO queda como opción por evaluar y la contribución se formula como propuesta. | Describir RF-05b tal como se implementó: cada proceso usa código por correo o cuenta institucional, según D-48. Mantener explícitos los límites de verificabilidad. |
| `contenido1.tex`, «Alcance y supuestos» | El alcance es «propuesto», el revoto queda provisional y las mediciones se describen como futuras. | Delimitar el MVP real (RF-01–RF-10 y RF-12–RF-21; RF-11, RF-22 y RF-23 fuera de alcance) y distinguir requisitos implementados de evaluaciones todavía pendientes. |
| `contenido1.tex`, «Delimitación» | La contribución se limita a especificación y diseño conceptual; el prototipo se plantea como futuro. | Indicar que existe una implementación local funcional y acotar con precisión lo que no demuestra: coacción, anonimato frente al operador y verificabilidad criptográfica extremo a extremo. |
| `contenido1.tex`, metodología DSR | El artefacto se describe como especificación y diseño; la demostración depende de prototipos futuros. | Describir el artefacto como plataforma implementada, los escenarios como demostración y P7 como evaluación empírica, separando resultados reales de trabajo aún pendiente. |
| `contenido1.tex`, plan de evaluación | Evaluación pendiente de prototipo; hipótesis y umbrales se llaman iniciales y no obtenidos. | Mantener H1a/H1b/H1c y sus criterios sin alterarlos; incorporar para cada una el método ejecutado, configuración, resultados reales, decisión y limitaciones. Mientras falte evidencia, dejar «pendiente». |
| `contenido1.tex`, amenazas a la validez | Se promete mitigar amenazas en futuro. | Redactar en pasado solo las medidas realmente aplicadas y mantener como limitaciones aquellas que sigan sin resolverse. |
| `contenido1.tex`, «Organización de la memoria» | Enumera capítulos y contenidos de la versión previa. | Actualizar el índice narrativo tras añadir los capítulos de arquitectura implementada, implementación, seguridad, evaluación, conclusiones y apéndices. |
| `resumentexto.tex` | El resumen anuncia evaluación pendiente y resultados para incorporar después. | Reescribir resumen y abstract al cerrar P7 con resultados reales; indicar resultados ausentes como pendientes, sin estimarlos. |
| `plan_gantt.tex` | La iteración 3 incluye RF-11 y el cronograma solo muestra el plan previsto. | Corregir iteración 3 a RF-09–RF-10 y añadir comparación entre hitos previstos y reales P0–P8 usando `docs/ai/PROGRESS.md`. |
| `Figuras/diagramas/arquitectura_componentes_saas_votacion.tex` | Un único diagrama mezcla CDN/WAF, KMS, cola y workers de arquitectura objetivo con los servicios implementados. | Separar arquitectura objetivo e implementada. La implementada debe mostrar api-gateway, identity, election, voter-access, ballot-box, Keycloak, REST/outbox y las dos bases físicas, sin broker. |
| `especificacion_detallada.tex`, RF-05/RF-05b | Describe autenticación genérica con SSO como alternativa sin concretar el modo por proceso. | Hacer explícita la decisión D-48: el proceso habilita código por correo o cuenta institucional; reflejar el flujo implementado y conservar la distinción entre autenticación de gestión y votantes. |
| Capítulos posteriores y anexos | Aún no documentan con detalle la arquitectura y la implementación reales. | Añadir los capítulos indicados en P8.2; enlazar endpoints, pantallas, ADR, modelos de datos y evidencias desde trazabilidad, sin inventar cobertura ni resultados. |

## Decisiones que siguen abiertas

La reducción a cinco servicios (D-05) y la exclusión del revoto (D-03) constan como preguntas para el tutor en
`docs/ai/PROGRESS.md`. Este diagnóstico no las resuelve ni propone cambiar esas decisiones.

## Compilación de diagnóstico

- `latexmk -pdf -interaction=nonstopmode -halt-on-error main.tex`: no ejecutable en este equipo; MiKTeX indica que
  falta el motor Perl requerido por `latexmk`.
- Alternativa ejecutada: `pdflatex` → `bibtex` → `pdflatex` ×2, desde `Plantilla-TFM-Informatica-UAL/`.
- Resultado: PDF de 69 páginas generado; BibTeX terminó correctamente y el log no contiene referencias o citas
  indefinidas. La compilación informa avisos de cajas sobreanchas/subanchas, sustituciones de tamaño de fuente,
  PDFs incluidos con versiones superiores a PDF 1.5 y el mensaje de MiKTeX «Infinite glue shrinkage found in box
  being split» en tablas largas.
- El PDF generado es un archivo ignorado por Git. No se modificaron los fuentes LaTeX en esta fase.
