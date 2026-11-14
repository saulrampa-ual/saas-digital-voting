# VeriVoto · Guía breve de la consola de gestión

Esta guía resume cómo se crean y se programan los procesos de votación en la consola de VeriVoto. Está pensada para
personas con el rol de **administración de la organización**.

## 1. Moverse por la consola

- Menú lateral, a la izquierda. Con el botón del menú se muestran u ocultan los nombres de las secciones; plegado,
  solo se ven los iconos.
- Secciones del rol de administración: **Inicio**, **Censos**, **Procesos**, **Participación**, **Resultados**,
  **Trazabilidad**, **Privacidad (RGPD)** y **Claves de API**.
- Los procesos de votación se gestionan en **Procesos**. Los censos (las listas de personas que pueden votar) los
  prepara la persona gestora del censo, en **Censos**. Para crear un proceso solo hace falta elegir un censo que ya
  exista.

## 2. Ciclo de vida de un proceso

| Estado | Qué significa | Qué se puede hacer |
| --- | --- | --- |
| Borrador | Recién creado | Editar todo y programar el proceso |
| Programado | Listo para abrirse | Nada: se abre solo en la fecha; censo y papeleta quedan congelados |
| Abierto | Votación en curso | Consultar la participación |
| Cerrado | Ha terminado el plazo | Escrutinio automático en curso |
| Escrutado | Hay resultados | Ver resultados; archivar |
| Archivado | Proceso terminado | Solo consulta |

La apertura y el cierre son **automáticos** en las fechas fijadas: no se pueden abrir ni cerrar a mano.

## 3. Crear un proceso

En **Procesos**, botón **Crear proceso**. El formulario tiene **cuatro pasos**; arriba se ve en qué paso estás
(«Paso n de 4») y cuál viene después. Con **Siguiente** y **Atrás** se pasa de un paso a otro sin perder lo escrito.

### Paso 1 · Datos básicos

- **Idioma por defecto**: español o inglés. Los campos obligatorios del idioma por defecto llevan `*`.
- **Título** y **Descripción**, cada uno en español y en inglés. Para guardar el borrador basta el idioma por
  defecto; **para programar hacen falta los dos idiomas**. En pantallas estrechas los dos idiomas aparecen en
  pestañas; la marca «pendiente» indica que falta un texto en ese idioma.
- **¿Quién podrá ver el resultado?**: solo las personas del censo, o cualquiera con el enlace público.

### Paso 2 · Calendario

- **Apertura** y **Cierre**, con fecha y hora. Se interpretan en la **hora de tu navegador**.
- El cierre debe ser posterior a la apertura, y para programar la apertura tiene que estar en el futuro.

### Paso 3 · Censo

- Elige uno de los censos de la organización. Junto a cada uno se ve cuántas personas tiene; un censo vacío no se
  podrá programar.
- **Cómo entran los votantes**: código de 6 cifras por correo, o cuenta institucional (SSO).

### Paso 4 · Papeleta

- **Tipo de papeleta**:
  - **Opción única**: cada persona elige una candidatura u opción. Escribes tú las opciones.
  - **Sí / No / Abstención**: para consultas. Las tres opciones se crean solas en los dos idiomas.
- **Pregunta**, en español y en inglés.
- **Opciones de la papeleta** (opción única): cada fila tiene el texto en español y en inglés. Hacen falta al menos 2
  (máximo 50). **Añadir opción** crea una fila nueva; el botón con la ✕ quita una fila.
- **Habilitar voto en blanco**: casilla que permite votar en blanco.

Al final, **Crear proceso**. El proceso se guarda como **Borrador** y aparece un aviso con dos botones: **Ver
proceso** e **Ir a procesos**.

Si falta algo, los campos afectados se marcan en rojo con un mensaje («Revisa los campos marcados»).

## 4. Programar un proceso

1. Abre el proceso: desde el aviso **Ver proceso** o desde la lista de **Procesos**, pulsando sobre él.
2. En la ficha del proceso:
   - **Estado del proceso** muestra en qué estado está (al principio, Borrador).
   - **Validación previa a programar** comprueba cuatro cosas: textos en los dos idiomas, calendario coherente,
     censo con personas y papeleta en los dos idiomas. Cada una aparece como «Correcto» o «Falta completar».
   - **Datos del proceso** resume fechas, censo, opciones, voto en blanco, visibilidad y acceso de los votantes.
3. Si algo falta, **Editar proceso** vuelve al formulario de cuatro pasos (solo mientras esté en Borrador).
4. **Programar** pide confirmación: al programar se **congela el censo** y se **envían las invitaciones**, y ya no se
   puede editar. Pulsa **Programar** en el aviso para confirmar o **Cancelar** para volver.
5. El estado pasa a **Programado** y el proceso se abrirá solo en la fecha fijada. El **Historial de estados**
   registra el cambio.

## 5. Preguntas frecuentes

- **¿Puedo dejarlo a medias?** Sí: el proceso queda en Borrador hasta que lo programes y se puede editar.
- **No aparece el botón Programar.** Solo aparece en Borrador y con el rol de administración.
- **Me dice que faltan traducciones.** Revisa en los dos idiomas el título, la pregunta y cada opción; la descripción
  solo si la has escrito en algún idioma.
- **Me dice que el calendario no es coherente.** La apertura debe ser futura y anterior al cierre.
