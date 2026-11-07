# Simulación de una votación para la revisión con el tutor

Guion para crear en local una votación con datos ficticios y recorrer las pantallas de cada rol, de principio a fin, en
unos 30 minutos. Todos los datos son sintéticos: no se usan nombres ni correos reales.

## 0. Antes de empezar

1. Arrancar el entorno (`.local\0-todo.cmd` o `arrancar.ps1`) y comprobar que abren:
   - la consola, en <http://localhost:4200/admin>;
   - Mailpit, en <http://localhost:8025>; ahí llegan todos los correos (códigos e invitaciones).
2. Crear las cuentas locales una sola vez con `.local\5-cuentas-locales.cmd`. El resultado queda en
   `.local\cuentas-locales.md` (fuera del repositorio).
3. **Entrar en la consola:** usuario y contraseña, y después el código de 6 cifras que llega a Mailpit.
4. **Conviene usar una ventana privada por rol:** así no hay que cerrar sesión entre pasos.

| Rol | Usuario |
| --- | --- |
| Superadministrador | `daniel.ortiz@ual.test` |
| Administradora (UAL) | `laura.moreno@ual.test` |
| Gestor de censo (UAL) | `sergio.cano@ual.test` |
| Auditora (UAL) | `irene.vega@ual.test` |

La contraseña de las cuatro es la de `DEMO_PASSWORD` en `.local\cuentas.ps1`.

## 1. Gestor de censo: crear el censo

1. Entrar como Sergio y abrir **Censos**.
2. Crear el censo «Claustro de estudiantes 2026 (simulación)» con **Crear censo**.
3. Abrir el censo e importar `scripts/demo/censo-simulacion.csv` (12 personas ficticias, dominio `simulacion.test`).
4. Revisar el resumen de la importación (12 válidas, sin duplicados) y confirmar.
5. **Para enseñar al tutor:** el **Alta manual** de una persona más y el buscador.

## 2. Administradora: crear y programar el proceso

1. Entrar como Laura, abrir **Procesos** y pulsar **Crear proceso**.
2. Rellenar los cuatro pasos del asistente:
   - **Datos básicos:** título y descripción en español **y** en inglés (sin los dos idiomas no se puede programar).
     Ejemplo: «Elección de representante de estudiantes» / «Student representative election».
   - **Calendario:**
     - apertura dentro de **5 minutos** (debe ser futura en el momento de programar);
     - cierre **15–20 minutos** después.
   - **Censo:** el creado en el paso 1.
   - **Papeleta:**
     - opción única, con tres candidaturas ficticias (por ejemplo «Lista Azul», «Lista Verde» y «Lista Naranja») en
       los dos idiomas;
     - voto en blanco habilitado.
3. Elegir la visibilidad del resultado: **Solo las personas del censo** o **Cualquiera con el enlace público**.
4. Abrir el proceso, revisar la **Validación previa a programar** (cuatro comprobaciones en verde) y pulsar
   **Programar**.
   - Al programar, el censo queda congelado y salen las invitaciones a Mailpit.
   - Se puede enseñar en **Censos** que el censo aparece como «Congelado».
5. El proceso pasa solo a **Abierto** a la hora de apertura. No hay botón para abrirlo a mano: es una decisión de
   diseño.

## 3. Votantes: votar

Se repite con 4–6 personas del censo para que la participación tenga sentido.

1. En Mailpit, abrir la invitación de una persona (por ejemplo `alba.castillo@simulacion.test`) y abrir su enlace.
2. Pedir el código, leerlo en Mailpit e introducirlo.
3. Elegir una opción (en alguna persona, voto en blanco) y confirmar.
4. Guardar el recibo y pulsar **Comprobar mi recibo**.

**Casos que merece la pena enseñar:**
- abrir el enlace antes de la apertura: «La votación todavía no está abierta»;
- volver a usar el mismo enlace después de votar: «Ya has votado»;
- introducir un código erróneo: indica los intentos restantes;
- cambiar a inglés a mitad del flujo: no se pierde la selección.

## 4. Administradora: seguir la participación

**Participación** muestra el porcentaje del censo y los votos emitidos.

- **Recordatorios:** desde el mismo panel se puede mandar uno a quien no ha votado. Solo se ve a cuántas personas
  llega, nunca a quiénes.
- **Punto clave para el tutor (secreto del voto):** la participación se cuenta **por día, sin horas**, y no se ve qué
  ha votado nadie hasta el escrutinio.

## 5. Cierre y escrutinio

1. A la hora de cierre, el proceso pasa solo a **Cerrado** y, en uno o dos minutos, a **Escrutado**.
2. En **Resultados**, la administradora ve:
   - el recuento por opción y los votos en blanco;
   - la descarga del CSV y del acta.
3. Si la visibilidad es pública, el enlace `/resultados/…` se puede abrir sin sesión.

## 6. Auditora: verificar

Entrar como Irene:

- **Auditoría:** lanzar la verificación de la cadena de eventos. Esperado: íntegra, y votos en urna = personas que
  votaron.
- **Trazabilidad:** quién hizo cada cambio de estado del proceso, sin datos del voto.

## 7. Superadministrador

Entrar como Daniel y abrir **Organizaciones**: alta y consulta de organizaciones y de sus personas gestoras. No ve
censos, votos ni resultados de ninguna organización.

## Notas

- **Elecciones de demostración ya cargadas:** si hace falta enseñar los tres estados sin esperar, la organización UAL
  tiene una pasada (escrutada), una presente (abierta) y una futura (programada). Las cuentas locales pertenecen a esa
  misma organización y también las ven.
- **Entorno local de desarrollo:** nada de esto vale como evidencia de la memoria. Para las sesiones con votantes reales
  se sigue `docs/evaluation/usabilidad/`.
