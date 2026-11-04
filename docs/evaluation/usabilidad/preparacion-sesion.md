# Preparación de la sesión de usabilidad (H1b)

Guía para montar y repetir la sesión con cinco personas votantes no técnicas. Complementa el
[protocolo](protocolo.md), el [guion](guion-tareas.md), la [hoja de registro](hoja-registro.md) y el
[consentimiento](consentimiento.md). Todo el escenario es sintético: no se usan personas, correos ni votaciones reales.

Criterio de H1b (validado por el tutor): las 5 personas completan acceso, emisión y obtención del recibo en ≤ 3 minutos
y al menos 4 de 5 interpretan correctamente el resultado de la verificación. Con n = 5 los resultados son evidencia
descriptiva, sin inferencia estadística.

## 1. Escenario sintético

| Elemento | Valor |
| --- | --- |
| Organización | Identificador `USAB`, nombre «Organización de prueba (usabilidad)» |
| Administración | `admin.usab@verivoto.test` (ADMIN) |
| Gestión del censo | `censo.usab@verivoto.test` (CENSUS_MANAGER) |
| Censo | «Sesión de usabilidad», importado desde [`censo-sesion.csv`](censo-sesion.csv): P01–P05 y dos reservas (R1, R2) |
| Título del proceso | ES «Votación de prueba: actividad del club» · EN «Test vote: club activity» |
| Descripción | ES «Votación ficticia para una prueba de usabilidad. No tiene ningún efecto.» · EN «Fictitious vote for a usability test. It has no effect.» |
| Papeleta | Opción única. Pregunta ES «¿Qué actividad debería organizar el club el próximo mes?» · EN «Which activity should the club organise next month?» |
| Opciones | «Ruta de senderismo» / «Hiking trip», «Taller de fotografía» / «Photography workshop», «Visita a un museo» / «Museum visit»; voto en blanco activado |
| Acceso del votante | Código por correo (`EMAIL_CODE`) |
| Resultados | Solo personas del censo (`CENSUS_ONLY`) |
| Apertura | Al menos 5 minutos después de programar y antes de la primera sesión |
| Cierre | Después de la última sesión prevista (por ejemplo, 7 días más tarde) |
| Opción de la tarea | «Taller de fotografía», igual para todas las personas; se entrega en una tarjeta impresa |

Las opciones son neutras a propósito: la persona vota lo que indica la tarjeta, nunca su preferencia. Una vez programado,
el proceso no se puede editar y cada enlace personal admite un solo voto; por eso hay dos entradas de reserva.

## 2. Arranque del entorno (cada día de sesiones)

1. Infraestructura: `docker compose -f infra/compose.yaml --env-file infra/.env up -d` y comprobar `pg-main`,
   `pg-ballot` y Keycloak en `healthy` y Mailpit en `http://localhost:8025`
   ([`local-infrastructure.md`](../../architecture/local-infrastructure.md)).
2. Servicios: gateway, identity, election, voter-access y ballot-box, como en
   [`api-testing.md`](../../architecture/api-testing.md) (apartados 2, 4, 7 y 8). Comprobar `/actuator/health` de cada uno.
3. Frontend: `cd frontend; npm start` y abrir `http://localhost:4200`.
4. Ensayo: hacer una votación completa con una persona del equipo antes de la primera sesión real (puede usarse un
   proceso aparte para no gastar las entradas del censo). Ese ensayo no se registra en la hoja.

## 3. Montaje del escenario (una sola vez)

**Opción automática (recomendada):** con todo el entorno arrancado, desde la raíz del repositorio:

```
set DEMO_PASSWORD=<contraseña de las cuentas de gestión de USAB, 12 caracteres o más>
node scripts/demo/seed-usab.mjs
```

El script hace los pasos 1 a 5 de abajo y, además, crea un proceso de **ensayo** con su propio censo de dos entradas
(`e1@verivoto.test`, `e2@verivoto.test`), para que el ensayo no gaste entradas del censo real. Espera las 9 invitaciones
y la apertura (5 minutos por defecto; `USAB_OPEN_MINUTES`) y deja la hoja de la persona moderadora en
`.local/sesion-usab.md` (fuera del repositorio): por cada entrada, su buzón filtrado en Mailpit y, solo como respaldo, su
enlace personal. No abre enlaces, no pide códigos ni vota. Los procesos quedan abiertos 7 días (`USAB_DAYS`). Se puede
volver a ejecutar para regenerar la hoja: reutiliza la organización, las cuentas, los censos y los procesos.

**Opción manual:**


1. Como superadministración, crear la organización `USAB` con `admin.usab@verivoto.test` como administración inicial
   (`api-testing.md`, apartado 3) y dar de alta `censo.usab@verivoto.test` con rol de gestión del censo. Fijar las
   contraseñas desde los correos de Mailpit.
2. Como gestión del censo: «Censos» → crear «Sesión de usabilidad» → importar `censo-sesion.csv` (apartado 11). El
   resumen debe indicar 7 altas, 0 duplicados y 0 líneas con error.
3. Como administración: «Procesos» → «Crear proceso» con los datos de la tabla anterior, con todos los textos en ES y EN
   (apartado 10). Comprobar en A4 que la comprobación previa está en verde.
4. «Programar». En Mailpit deben llegar 7 invitaciones, una por dirección, con un enlace
   `http://localhost:4200/votar/…`. No abrir esos enlaces antes de cada sesión.
5. Esperar a que A4 muestre «Abierto».

## 4. Antes de cada participante

- [ ] A4 muestra el proceso «Abierto» y la fecha de cierre es posterior a la sesión.
- [ ] Equipo de la persona moderadora, navegador actualizado, zoom al 100 % y notificaciones silenciadas. Anotar
      navegador y ancho de ventana en la hoja.
- [ ] Ventana privada nueva con una sola pestaña: Mailpit filtrado por la dirección de la persona participante
      (escribir `to:p01@verivoto.test` en la búsqueda de Mailpit). Esa pestaña hace de «buzón de correo».
- [ ] La invitación de esa dirección está visible y sin abrir el enlace.
- [ ] Tarjeta impresa con la opción «Taller de fotografía».
- [ ] Consentimiento impreso, hoja de registro con la fila preparada y cronómetro a cero.
- [ ] La persona moderadora no mira ni anota códigos de acceso, recibos ni la pantalla del voto más allá de lo necesario
      para marcar los tiempos.

## 5. Durante la sesión

Seguir el [guion](guion-tareas.md) literalmente. Puntos de tiempo:

| Marca | Momento | Uso |
| --- | --- | --- |
| T0 | Aparece la bienvenida de la votación (botón «Empezar») | Inicio del tiempo de H1b |
| T1 | Aparece la pantalla «Voto registrado» con el código de recibo | Fin del tiempo de H1b (acceso + emisión + recibo) |
| T2 | Aparece el resultado de «Comprobar mi recibo» | Observación de la verificación; no entra en los 3 minutos |

## 6. Después de cada participante

1. Cerrar la ventana privada (el enlace, el código y el token solo viven en memoria).
2. Completar la fila de la hoja en el momento, sin datos identificativos ni la opción votada.
3. Opcional: borrar en Mailpit los correos de esa dirección.
4. Comprobar en «Participación» que el número de votos ha aumentado en uno (solo el total, sin identidad).

## 7. Incidencias y reservas

- **Incidencia de entorno** (servicio caído, correo que no llega en 60 s, fallo del equipo o de la red): anotarla y
  marcar la sesión como no válida. No se repite con la misma persona (habría aprendizaje); se usa una entrada de
  reserva (R1, R2) con otra persona nueva.
- **No son incidencias de entorno**: errores de la persona, códigos mal escritos, bloqueo tras 5 códigos erróneos o
  abandono. Se registran como errores o como tarea no completada y la sesión sigue siendo válida.
- Si no se llega a cinco sesiones válidas, H1b queda PENDIENTE (no se acepta ni se rechaza) y se informa como límite.

## 8. Cierre

1. Completar el resumen agregado de la hoja con cantidades (x de 5).
2. Trasladar solo los agregados a `docs/evaluation/informe.md` y, cuando existan, a la tabla 13.1 de la memoria.
3. El proceso se cierra y se escruta solo al llegar su fecha de cierre; el resultado de la votación no se analiza ni se
   publica en el TFM.
4. Conservar los consentimientos según indique la universidad y no digitalizarlos junto con la hoja de resultados.
