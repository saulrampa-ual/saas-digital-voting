import pathlib, sys

root = pathlib.Path(sys.argv[1]) / "docs"


def edit(rel, pairs, append=None):
    p = root / rel
    raw = p.read_bytes().decode("utf-8")
    crlf = "\r\n" in raw
    text = raw.replace("\r\n", "\n")
    for old, new in pairs:
        if text.count(old) != 1:
            raise SystemExit(f"{rel}: expected 1 match, got {text.count(old)}: {old[:60]!r}")
        text = text.replace(old, new)
    if append:
        text = text.rstrip("\n") + "\n" + append
    if crlf:
        text = text.replace("\n", "\r\n")
    p.write_bytes(text.encode("utf-8"))
    print("ok", rel)


D35 = ("| D-35 | 2026-10-30 | Pantallas de procesos (P4.6): lista `/admin/procesos` (ADMIN y AUDITOR), A3 en 4 pasos "
       "(datos básicos, calendario, censo, papeleta) reutilizada para editar un borrador (`/admin/procesos/:id/editar`), "
       "con cada texto en ES y EN uno al lado del otro y fechas `datetime-local` en hora local enviadas en ISO; validación "
       "en cliente con los mismos límites que el servicio (solo el idioma por defecto obligatorio al guardar, D-29) y "
       "errores del servidor (`ProblemDetail` con campos) arriba del formulario; F3 como tarjeta de éxito al crear; A4 "
       "(`/admin/procesos/:id`) con línea de estados, acciones según estado (Editar y Programar en `DRAFT`, Archivar en "
       "`TALLIED`; abrir y cerrar no tienen botón porque son automáticos, D-30), comprobación previa a programar calculada "
       "en cliente (el 422 del servidor sigue mandando), historial con «Sistema» o «Persona administradora» (no se muestra "
       "quién) y vista de solo lectura para el auditor; confirmaciones con `ion-alert` declarativo; errores de campo con "
       "un párrafo propio con `role=\"alert\"` (el `errorText` de Ionic no se mostraba en nuestro uso); presupuesto inicial "
       "a 1,2 MB aviso / 1,5 MB error (≈ 934 kB, ≈ 210 kB transferidos) | Cerrar la rebanada P4 sin Swagger con el "
       "flujo de A3/A4; RF-20 en la edición; sin revelar identidades en el historial | Un formulario por idioma; "
       "`AlertController` (aumentaba el bundle igual); botón «volver a borrador» (no existe en el TFM) |\n")

edit("ai/DECISIONS.md", [], append=D35)

edit("ai/TRACEABILITY.md", [
    ("| A3, F3, Gestión de candidatos | Backend hecho (P4.1, D-29); pantallas A3/F3 pendientes |",
     "| A3, F3, Gestión de candidatos | Hecho: backend (P4.1, D-29) y pantallas A3/F3 en `frontend/src/app/features/admin/processes` (P4.6, D-35); las opciones de la papeleta cubren la gestión de candidaturas |"),
    ("`TALLIED` llega con el escrutinio (P5); pantalla A4 pendiente |",
     "`TALLIED` llega con el escrutinio (P5); pantalla A4 hecha (P4.6, D-35) |"),
    ("textos de papeleta en A3 por confirmar | En curso (interfaz es/en y papeleta del votante con todas las traducciones; falta la edición bilingüe en A3) |",
     "edición ES/EN de títulos, pregunta y opciones en A3 | Hecho (interfaz es/en, papeleta del votante con todas las traducciones y edición bilingüe en A3, D-35) |"),
])

edit("ai/PROTOTYPES.md", [
    ("| RF-20 | Selector ES/EN en la cabecera | Parcial: no consta la edición bilingüe de la papeleta en A3 |",
     "| RF-20 | Selector ES/EN en la cabecera; A3 | Sí: la edición bilingüe se añade en A3 (P4.6, D-35) |"),
])

edit("ai/PROGRESS.md", [
    ("Rama de trabajo: `feat/voter-screens` (P4.5 sin commitear) · Último commit: `ce22e93`",
     "Rama de trabajo: `feat/process-screens` (P4.6 sin commitear, sobre `feat/voter-screens` `817ca82`) · Último commit en `dev`: `ce22e93`"),
    ("En curso (P4.1–P4.4 en `dev`, `ce22e93`; P4.5 probado por la IA, sin commitear; faltan A3/A4) |",
     "En curso (P4.1–P4.4 en `dev`; P4.5 en `feat/voter-screens` sin integrar; P4.6 probado por la IA, sin commitear) |"),
    ("## En curso\n\n",
     "## En curso\n\n"
     "- P4.6: pantallas de procesos (RF-06, RF-07, RF-20; D-35): `ProcessApiService`, lista de procesos, A3/F3 "
     "(crear y editar borrador, 4 pasos, ES/EN), A4 (ciclo de vida, acciones, comprobación previa, historial, auditor "
     "en solo lectura), `StatusBadgeComponent`, menú «Procesos» y textos es/en. Prueba en navegador (Playwright, 1280 y "
     "360 px) con gateway, election, voter-access, ballot-box reales y Keycloak simulado: 22/22 (una comprobación "
     "repetida aparte por la transición de página), incluida la apertura automática vista en A4 "
     "(«Programado → Abierto · Sistema»). `ng build` limpio (inicial ≈ 934 kB / 210 kB transferidos).\n"),
    ("""1. `git push origin dev` (hay 2 commits sin publicar).
2. Compilar (`.\\mvnw.cmd -DskipTests compile`) y seguir la sección 4 de `docs/architecture/api-testing.md`.
3. Commit en `feat/election-census` e integración en `dev`.
4. Probar el flujo completo (`api-testing.md` §9) y commitear P4.5 en `feat/voter-screens`.
5. Siguiente: **P4.6**, pantallas de gestión A3 (crear proceso y papeleta bilingüe) y A4 (ciclo de vida), para
   cerrar la rebanada sin Swagger.""",
     """1. Probar A3/A4 con el Keycloak real (`api-testing.md` §10) y commitear P4.6 en `feat/process-screens`.
2. Integrar `feat/voter-screens` y `feat/process-screens` en `dev`.
3. Siguiente: **A2** en el frontend (censos: crear, importar CSV con resumen previo, D-27) y después **P5**
   (auditoría, escrutinio y resultados)."""),
])

edit("architecture/api-testing.md", [], append="""
## 10. Procesos desde la consola (P4.6, A3/A4)

Con la misma infraestructura del apartado 9 y un censo con al menos una persona (apartado 4):

1. Entrar en `http://localhost:4200/admin` como administrador de la organización → «Procesos» → «Crear proceso».
2. Paso 1: título en ES y EN. Dejar el título vacío y pulsar «Siguiente» muestra el error debajo del campo.
3. Paso 2: apertura dentro de 2–3 minutos y cierre posterior; con el cierre antes de la apertura aparece el error.
4. Paso 3: elegir el censo. Paso 4: tipo «Opción única», pregunta y al menos 2 opciones; «Crear proceso» → F3.
5. En A4 (borrador): la comprobación previa marca lo que falta (p. ej. traducciones EN). «Editar proceso» abre el
   mismo formulario con los datos; al completar el EN, todo queda en verde.
6. «Programar» → confirmar: estado «Programado», historial «Borrador → Programado · Persona administradora» y
   correos de invitación en Mailpit. «Editar» ya no aparece.
7. Esperar la apertura (scheduler cada 30 s) y recargar A4: «Abierto», historial «Programado → Abierto · Sistema».
8. Entrar como auditor: ve la lista y A4 sin botones («Tu rol permite consultar el proceso, no cambiarlo»).
""")
