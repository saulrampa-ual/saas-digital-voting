# Preguntas para la reunión con el tutor (Javier Criado)

Preparado el 20/11/2026. Base: `docs/ai/PLAN.md` (preguntas Q-01 a Q-13 y sección 9), `docs/ai/PROGRESS.md`,
`docs/ai/IMPROVEMENTS.md`, la memoria y el correo enviado al tutor el 13/11/2026.

## Cómo usar este documento

- **No mandar más correos.** El tutor tiene pendiente el correo del 13/11/2026 (estado del prototipo y de las
  evaluaciones, propuesta de casos de uso en tabla, diagramas de casos de uso simplificados y petición de reunión).
  Si se le escriben correos nuevos encima, el hilo se pierde. Se espera a que responda a ese y en la reunión se hacen
  estas preguntas.
- Cada pregunta lleva un **contexto** (qué se hizo y por qué se pregunta), un **ejemplo** (qué cambia en la memoria
  según la respuesta) y la **propuesta** del autor, para que el tutor pueda contestar rápido aunque no recuerde el
  detalle.
- Durante la reunión, anotar la respuesta en la columna «Respuesta» de la tabla de resumen y, después, pasarla a
  `PLAN.md` (y a `DECISIONS.md` si cierra una decisión).

## Resumen (por orden de prioridad)

| N.º | Tema | Qué se le pide | Bloquea | Respuesta |
| --- | --- | --- | --- | --- |
| 1 | Formato de los casos de uso en tabla | Aprobar el formato (RF-01 y RF-09 ya convertidos) | Convertir el resto de CU en la memoria | |
| 2 | Diagramas de casos de uso simplificados | Aprobar los de gestión y votación | Incluirlos en la memoria | |
| 3 | Prioridades MoSCoW de los RNF (Q-13) | Confirmar o cambiar la tabla 7.1 | Quitar los asteriscos de la tabla 7.1 | |
| 4 | SSO con cuentas `@inlumine.ual.es` (M6/A9) | Aceptar que quede como trabajo futuro | Texto de limitaciones y tabla de RF | |
| 5 | Identificador institucional oculto (M6b) | Confirmar que es correcto no mostrarlo | Ninguno (ya aplicado) | |
| 6 | H1c rechazada (prueba de carga) | Aceptar el resultado documentado | Conclusiones y defensa | |
| 7 | H1a (accesibilidad manual) | Confirmar la aceptación con el Narrador y la corrección previa | Tabla 13.1 y conclusiones | |
| 8 | Alcance final del MVP | Confirmar lo que queda fuera | Tablas 1.1 y 4.3 | |
| 9 | Limitaciones de las sesiones de usabilidad | Aceptar cómo están descritas | Capítulo 13 | |
| 10 | Revisión heurística (M3) | Si es útil y quién puede ser el segundo evaluador | RNF-8, 9 y 10 | |
| 11 | Entrega al director y entrega oficial | Fechas y qué se entrega | Calendario final | |
| 12 | Defensa | Plantilla, demo y formato | Presentación | |
| 13 | Próximas revisiones (Q-10) | Cuándo y qué quiere ver | Calendario de C4 | |

---

## 1. Formato de los casos de uso en tabla

**Contexto.** El tutor pidió (13/11/2026) los casos de uso en formato tabla. Se preparó una plantilla
(`preambulo_cu.tex`) y un script (`scripts/memoria/cu-a-tabla.mjs`) que convierte las fichas de la especificación.
Como ejemplo, solo están convertidos **RF-01** y **RF-09**; el resto se convierte cuando el formato esté aprobado,
para no rehacer todos los casos dos veces.

**Ejemplo.** Una tabla por caso de uso con las filas: identificador, nombre, actor, precondición, flujo principal
(pasos numerados), flujos alternativos, postcondición y requisitos relacionados. En RF-09 («Emitir voto») el flujo
principal va desde «el votante abre su enlace» hasta «recibe el recibo».

**Pregunta.** ¿Le vale este formato para todos los casos de uso? ¿Quiere añadir o quitar alguna fila (por ejemplo,
«frecuencia» o «requisitos no funcionales»)?

**Propuesta.** Aprobar el formato actual y convertir el resto con el script.

## 2. Diagramas de casos de uso simplificados

**Contexto.** En la reunión del 21/10/2026 pidió un actor genérico del que hereden los roles. Además de la figura que
ya está en la memoria, se prepararon dos diagramas más sencillos, uno de **gestión** y otro de **votación**
(`Figuras/diagramas/dcu_gestion.tex`, `dcu_votacion.tex`, con un estilo común en `dcu_estilo.tex`). **No están
incluidos en la memoria** hasta que los vea.

**Ejemplo.** En el de gestión, «Persona de gestión» es el actor genérico (iniciar sesión, consultar procesos) y de él
heredan Superadministrador, Administrador, Gestor de censo y Auditor, cada uno con sus casos propios. En el de
votación solo aparecen el Votante y sus casos (acceder, votar, comprobar el recibo, ver el resultado).

**Pregunta.** ¿Sustituyen a la figura actual, se añaden junto a ella o prefiere dejar solo la actual?

**Propuesta.** Sustituir la figura actual por los dos diagramas simplificados, que se leen mejor en una página.

## 3. Prioridades MoSCoW de los requisitos no funcionales (Q-13)

**Contexto.** La especificación no daba prioridad a los RNF. Para poder decir en la memoria cuáles son
imprescindibles, se propuso una prioridad MoSCoW en la **tabla 7.1**, marcada con asterisco hasta que la confirme.

**Ejemplo.** La propuesta es:

- **Must:** RNF-1 (accesibilidad), RNF-3 (uso del votante), RNF-6 (claridad de la verificación), RNF-7 (prevención de
  errores) y RNF-11 a RNF-16 (secreto, unicidad, integridad, seguridad de acceso, aislamiento y protección de datos).
- **Should:** RNF-2 (adaptable y navegadores), RNF-4 (uso del administrador), RNF-5 (idiomas), RNF-8 a RNF-10
  (mensajes, orientación y consistencia) y RNF-17 (rendimiento).
- **Could:** RNF-18 (PWA instalable).

Si confirma, se quitan los asteriscos. Si cambia alguno (por ejemplo, RNF-17 a Must), cambia también la lectura de la
tabla 13.2: H1c rechazada pasaría a ser un requisito imprescindible no cumplido.

**Pregunta.** ¿Confirma estas prioridades o quiere cambiar alguna?

**Propuesta.** Confirmar la propuesta tal como está.

## 4. SSO con cuentas `@inlumine.ual.es` (M6/A9)

**Contexto.** El inicio de sesión con cuenta institucional está **implementado**: para el votante (RF-05b) existe el
flujo completo con Keycloak, y para gestión (RF-05a) está preparado el paso del flujo de Keycloak. Pero **no se ha
podido probar con un proveedor real**, porque hacen falta cuentas `@inlumine.ual.es` y la configuración institucional
del proveedor de identidad de la UAL, a las que no hay acceso en esta fase. Por eso:

- la opción «Cuenta institucional» está **oculta en la consola** (se puede volver a activar con un solo valor de
  configuración);
- RF-05a sigue como «parcial» en la trazabilidad;
- queda como **trabajo futuro (P9)** para cuando haya acceso.

**Ejemplo.** En el capítulo de limitaciones se añadiría: «El inicio de sesión con cuenta institucional (RF-05a y
RF-05b) está implementado, pero no se ha podido probar con un proveedor real: requiere cuentas @inlumine.ual.es y una
configuración institucional no disponibles en este trabajo. Por ello la opción no se ofrece en la consola de esta
versión y su prueba queda como trabajo futuro».

**Pregunta.** ¿Le parece bien dejarlo así, como trabajo futuro? ¿Sabe si la universidad facilita un proveedor de
prueba o cuentas para TFM, o lo dejamos fuera?

**Propuesta.** Dejarlo como trabajo futuro con el texto anterior.

## 5. Identificador institucional oculto en la consola (M6b)

**Contexto.** El censo guarda un «identificador institucional» opcional (por ejemplo, el código de estudiante). Se
decidió que **la persona que usa la consola no lo vea**: ya no aparece en la tabla del censo, en el formulario, en la
plantilla Excel ni en la búsqueda de privacidad. El dato **se conserva** en el censo y en el backend, porque lo usan
la importación (detección de duplicados) y la exportación de datos personales del RGPD (derecho de acceso), que lo
sigue incluyendo.

**Ejemplo.** Si una persona pide sus datos (RF-18), el fichero exportado sí lleva su identificador institucional,
porque es un dato suyo; pero el gestor del censo no lo ve en pantalla.

**Pregunta.** ¿Está de acuerdo con este criterio, sobre todo con mantenerlo en la exportación del RGPD?

**Propuesta.** Mantenerlo así (ya aplicado).

## 6. H1c rechazada: prueba de carga

**Contexto.** H1c pide que, con 500 usuarios simultáneos, el p95 de la emisión del voto esté por debajo de 2 segundos.
Se midió en **tres versiones** del sistema, en local:

| Serie | Fecha | Cambio | Resultado |
| --- | --- | --- | --- |
| A | 11/11/2026 | Versión inicial | p95 entre 2,4 y 4,9 s |
| B | 19/11/2026 | Colas de conexión ampliadas | p95 7,2 s; 339 de 500 votos aceptados |
| C | 19/11/2026 | Confirmación de votos por grupos (IMP-64) | Unos 55 votos por segundo; 17 de 500 por debajo de 2 s |

En las tres la cadena de huellas quedó íntegra y no hubo votos duplicados: el sistema es correcto, pero no rápido
con esa carga en un portátil. H1c queda **rechazada** y documentada como limitación y trabajo futuro. El anteproyecto
(OE5) pide hacer la prueba y documentar también los criterios no alcanzados, así que el objetivo se cumple.

**Ejemplo.** En la defensa se diría: «H1c se rechazó en las tres versiones evaluadas; la integridad se mantuvo; el
límite está repartido por la cadena de servicios y perfilarlo es trabajo futuro».

**Pregunta.** ¿Le parece suficiente documentarlo así, o quiere alguna prueba más (por ejemplo, con 200 usuarios para
ver hasta dónde aguanta)? IMP-64 (confirmación por grupos) estaba anotada como «requiere aprobación del tutor»: ¿la
mantenemos en el código? No mejoró H1c, pero corrige un caso raro en el que un fallo de red podía permitir un segundo
voto.

**Propuesta.** Cerrar H1c como rechazada y mantener IMP-64 por la corrección del caso del commit ambiguo.

## 7. H1a: revisión manual de accesibilidad

**Contexto.** El criterio que él validó (D-49) tiene dos partes: (1) cero incumplimientos críticos o serios con axe,
que **se cumple** (212 comprobaciones en Chrome, Firefox y WebKit), y (2) una revisión manual con teclado y lector de
pantalla sin bloqueos. La parte manual no se pudo hacer el 08/11 porque NVDA no leía la página en el equipo del autor.
El guion se adaptó al **Narrador de Windows** y la revisión (M1) se hizo el **22/11/2026** en Chrome con la demo
sintética: 39 de 39 comprobaciones obligatorias, **0 bloqueos** y 2 incidencias menores (las casillas del código se
anuncian como «Input 1 of 6», en inglés, y la cuenta atrás para pedir otro código se lee cada segundo). Los botones
verdes bajaban de 5,26:1 a **3,32:1** de contraste al recibir el foco (mínimo 4,5:1). Según el guion, se corrigió antes de
decidir (hoja de estilos global, sin la capa blanca de Ionic y con contorno de 3 px) y, medido de nuevo, quedan en 5,26:1.
Con eso, H1a figura como **aceptada** en la memoria.

**Ejemplo.** Si no le vale el Narrador, H1a volvería a «sin decisión» hasta repetir la revisión con NVDA. Si no le vale
corregir el contraste antes de decidir, H1a quedaría **rechazada** en la versión inicial y aceptada en la corregida.

**Pregunta.** ¿Le vale el Narrador como lector de pantalla para cumplir el criterio, en lugar de NVDA? ¿Y le parece
correcto corregir el contraste y volver a medir antes de decidir, como prevé el guion?

**Propuesta.** Aceptar el Narrador, que es el lector incluido en Windows 11, y la corrección previa, porque el guion lo
prevé y el criterio no se ha relajado.

## 8. Alcance final del MVP

**Contexto.** En el correo del 13/11 se le pidió una reunión para revisar el alcance final. Lo que queda **fuera de
esta versión** y cómo está anotado:

- **RF-11 (cambiar el voto):** *Won't* en la tabla 4.3 (D-03, confirmado por correo el 30/10/2026); se conserva el
  identificador por trazabilidad.
- **RF-22 y RF-23:** fuera del MVP, trabajo futuro, sin evaluar (notas en la tabla 1.1).
- **Webhooks y SMS:** fuera del MVP.
- **Despliegue:** no hay; el TFM se ejecuta y se evalúa en local con datos sintéticos (D-53).
- **OE5:** se añadió a la memoria para que coincida con el anteproyecto firmado.

**Ejemplo.** En la tabla 4.3, RF-11 aparece como «Won't (fuera de esta versión)» y en las conclusiones como trabajo
futuro.

**Pregunta.** ¿Está de acuerdo con este alcance final? ¿Falta algo del anteproyecto que crea que se debe cubrir?

**Propuesta.** Cerrar el alcance tal como está.

## 9. Limitaciones de las sesiones de usabilidad

**Contexto.** H1b (votantes, 09/11) y RNF-4 (administradores, 13/11) se aceptaron con 5 participantes cada una. Hay
limitaciones que la memoria ya declara:

- con 5 personas la evidencia es **descriptiva**, sin estadística;
- en RNF-4 los tiempos se anotaron **redondeados al minuto** (todos entre 2 y 4 minutos, con el límite en 10);
- las sesiones RNF-4 fueron **online**, por túnel a un entorno local, con documentación permitida (D-55);
- los participantes son conocidos del autor.

**Ejemplo.** En el capítulo 13: «Los tiempos de RNF-4 están redondeados al minuto; como todos están muy por debajo
del límite, el redondeo no cambia la decisión».

**Pregunta.** ¿Le parecen suficientes estas limitaciones o quiere que se añada alguna más?

**Propuesta.** Mantenerlas como están.

## 10. Revisión heurística (M3, opcional)

**Contexto.** RNF-8, RNF-9 y RNF-10 (mensajes, orientación y consistencia) no tienen evaluación propia. Está preparada
una revisión heurística de Nielsen (`docs/evaluation/revision-heuristica/`) con **dos evaluadores** que revisan por
separado (60–90 minutos cada uno). El segundo no puede haber participado en el desarrollo.

**Ejemplo.** Si tras juntar las dos hojas no queda ningún problema de gravedad 3 o 4, RNF-10 se cumple; si además toda
la realimentación tarda 1 segundo o menos, RNF-8 también.

**Pregunta.** ¿Merece la pena hacerla antes de la entrega? ¿Vale un compañero del máster como segundo evaluador, o
prefiere que sea otra persona (por ejemplo, él mismo)?

**Propuesta.** Hacerla entre el 25/11 y el 06/12 con un compañero del máster.

## 11. Entrega al director y entrega oficial

**Contexto.** El calendario de la memoria (Gantt) prevé la **entrega al director el 18/01/2027** y la **entrega
oficial el 27/01/2027**, con el código congelado desde el 15/01/2027.

**Ejemplo.** El 18/01 se le enviaría el PDF de la memoria y la etiqueta del repositorio con la versión entregada;
del 18 al 27/01 se aplicarían sus correcciones.

**Preguntas.**

- ¿Le valen esas fechas o necesita la memoria antes para revisarla con calma?
- ¿Qué se entrega además del PDF: anexos, enlace al repositorio con una etiqueta, un vídeo de demostración?
- ¿El repositorio debe seguir público o privado después de la entrega?

**Propuesta.** Mantener las fechas y entregar PDF, anexos y enlace a la etiqueta del repositorio.

## 12. Defensa

**Contexto.** La exposición dura 15 minutos (confirmado por él el 17/11). La presentación está preparada en el
repositorio (`Presentacion-TFM/`, con guion y posibles preguntas), en una carpeta propia por si cambia la plantilla de
la universidad.

**Ejemplo.** Una demostración en vivo puede fallar por el entorno local; un vídeo de 1–2 minutos del flujo del votante
y de la verificación del resultado evita ese riesgo.

**Preguntas.**

- ¿Hay que usar una plantilla oficial de la UAL para las diapositivas?
- ¿Es mejor una demo en vivo, un vídeo grabado o solo capturas?
- ¿Cuándo se sabrán la fecha y el tribunal? ¿Le gustaría ver un ensayo de la defensa antes?

**Propuesta.** Plantilla actual salvo que exista una obligatoria; vídeo corto en lugar de demo en vivo; un ensayo con
él a mediados de enero.

## 13. Próximas revisiones (Q-10)

**Contexto.** Desde el 03/11 se espera su indicación sobre la siguiente revisión. Lo que queda por hacer es sobre todo
la revisión editorial y visual de la memoria (C4) y la defensa.

**Pregunta.** ¿Con qué frecuencia quiere revisar la memoria hasta enero y en qué formato (PDF por correo, revisión en
el repositorio, reunión)?

**Propuesta.** Una revisión del PDF completo a mediados de diciembre y otra antes del 18/01/2027.

---

## Para llevar a la reunión

- [ ] El PDF de la memoria actualizado (con la tabla 13.2 y OE5).
- [ ] El resultado de M1 (Narrador), si ya está hecho.
- [ ] Los ejemplos de casos de uso en tabla (RF-01 y RF-09) y los dos diagramas simplificados, compilados.
- [ ] La demo UAL arrancada en local, por si quiere ver la consola o el flujo del votante (`docs/evaluation/simulacion-tutor.md`).
- [ ] Cifras actuales de pruebas (las de backend tras el `mvnw verify` completo; frontend: 259 pruebas y unos 91 % de
      líneas cubiertas).
- [ ] Este documento, para anotar las respuestas.

## Después de la reunión

1. Anotar cada respuesta en la tabla de resumen.
2. Pasarlas a `docs/ai/PLAN.md` (Q-10, Q-13 y sección 9) y, si cierran una decisión, a `docs/ai/DECISIONS.md`.
3. Aplicar los cambios en la memoria (tabla 7.1, casos de uso, diagramas, limitaciones).
