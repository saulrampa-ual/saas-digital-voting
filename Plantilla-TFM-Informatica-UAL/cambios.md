# Validación de los ajustes UML y de la memoria

Revisión preparada conforme a las indicaciones del tutor y a la estructura de
la plantilla TFM de la Universidad de Almería.

- (a) Las asociaciones actor--caso de uso de las fuentes PlantUML usan líneas sin punta de flecha. Las generalizaciones y dependencias `include`/`extend` conservan su notación UML.
- (b) Existe el actor genérico `Usuario de gestión`; Superadministrador y Gestor de censo heredan directamente de él. `Usuario con auditoría` hereda de `Usuario de gestión`, y Administrador y Auditor heredan de `Usuario con auditoría`.
- (c) RF-13a está incluido desde RF-07 y se documenta como comportamiento obligatorio al cierre. RF-13b es independiente.
- (d) El RF-05 de la especificación se desglosa en RF-05a (SSO de gestión) y RF-05b (SSO de votante). El proveedor de identidad participa en ambos; RF-05b no se incorpora a RF-08.
- (e) RF-10 y RF-11 extienden RF-09 y no tienen asociación directa con Votante. El punto de extensión queda tras confirmar la emisión.
- (f) RF-19 solo tiene asociación con `Usuario con auditoría`, especialización de gestión cuyos subtipos son Administrador y Auditor.
- (g) RNF-8 y RNF-10 se declaran transversales a RF-01--RF-19 y a todas las pantallas, sin asociarlos a un caso de uso concreto.
- (h) La tabla `tab:rnf-iso25010` incluye los 18 RNF y asigna a cada uno características y subcaracterísticas del modelo ISO/IEC 25010:2023.

Las fuentes editables están en `Figuras/diagramas/casos_uso_gestion.puml`,
`Figuras/diagramas/casos_uso_votacion.puml`,
`Figuras/diagramas/secuencia_emitir_voto_recibo.puml` y
`Figuras/diagramas/escrutinio_verificacion_actividad.puml`. Los diagramas de
secuencia y actividad se incluyen en el capítulo 3; los de casos de uso, en el
capítulo 6.

La introducción incorpora preguntas de investigación, objetivos específicos,
trazabilidad, metodología DSR, plan de evaluación, delimitación y amenazas a la
validez. Los catálogos de RF y RNF muestran prioridad MoSCoW explícita o
«Por validar». El Gantt amplía la altura entre filas y deja separación entre
fases; los hitos y las fechas se conservan.


## Actualización de requisitos gráficos y trazabilidad (2026-10-29)

- `plan_gantt.tex`: remaqueta las figuras 9.1/9.2 en orientación vertical con escala y tipografía ajustadas a página A4.
- `contenido1.tex`: actualiza la trazabilidad de RF-20 a RF-23, divide el catálogo en RF-05a/RF-05b, declara RNF-8/RNF-10 transversales, corrige la frase de §5.1, actualiza §5.2/§5.3 y sus figuras; añade la tabla de transversalidad del capítulo 5.
- `especificacion_detallada.tex`: cambia el título de RF-05 a «Iniciar sesión», manteniendo SSO/OIDC como decisión de diseño.
- `cambios_diagrama.tex`, `Figuras/diagramas/casos_uso_gestion.puml`, `Figuras/diagramas/casos_uso_votacion.puml`: actualiza etiquetas RF-05 y evita asociaciones actor-caso de uso duplicadas en RF-12/RF-13b/RF-15.
- `Figuras/diagramas/arquitectura_componentes_saas_votacion.tex`: nueva fuente vectorial TikZ de la arquitectura 5.1 con asignaciones RF y conexiones OIDC/IdP solicitadas.
- `Figuras/diagramas/casos_uso_gestion.tex` y `Figuras/diagramas/casos_uso_votacion.tex`: figuras 8.1/8.2 vectoriales con actores genéricos, generalización abierta y asociaciones directas no duplicadas.
- `Figuras/diagramas/secuencia_emitir_voto_recibo.tex`: nueva figura vectorial TikZ 5.2 con seis módulos, emisión 1--7, verificación 8--10 y nota de anonimato del token.
- `Figuras/diagramas/secuencia_emitir_voto_recibo.puml`: actualiza el listado A.3 como fuente PlantUML equivalente del flujo de la figura.
- `apendices.tex`: actualiza la descripción/caption del listado A.3.
- `main.tex`: carga `arrows.meta` para las puntas UML abiertas de generalización y extensión.
- `resumentexto.tex`: ajusta el resumen y el abstract a las fuentes de diagramas actuales.
- `cambios.md`: registra esta actualización.


### Verificación de entrega

- Compilación de `main.tex` con pdfLaTeX y BibTeX: correcta; 69 páginas A4.
- Comprobación de la pasada final: sin referencias o citas indefinidas y sin errores LaTeX.
- Inspección visual: figuras 5.1, 5.2, 8.1, 8.2, 9.1 y 9.2; índices y orden de capítulos.
- PDF final generado en `main.pdf`.
