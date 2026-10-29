---
name: ionic-angular-screen
description: Convenciones para crear o modificar pantallas del frontend de VeriVoto (Angular 22 + Ionic 9, PWA), del votante o de la consola de gestión. Usar en cualquier trabajo de UI en frontend/.
---

# Pantalla Ionic + Angular

## Antes de empezar

1. Leer `docs/ai/PROTOTYPES.md` y la pantalla correspondiente en `prototiposPantallas/` (no modificarla).
2. Comprobar el RF en `docs/ai/TRACEABILITY.md`.
3. Revoto fuera del MVP (D-03): no implementar «Cambiar mi voto», recibos sustituidos ni avisos de
   cambio de voto aunque aparezcan en el prototipo.
4. Si el prototipo contradice un RF/RNF, la accesibilidad o la arquitectura: avisar, no resolverlo.

## Versiones y API (Ionic 9 / Angular 22)

- Componentes standalone. En Ionic 9 los componentes se importan desde `@ionic/angular`
  (en Ionic 8 era `@ionic/angular/standalone`; no copiar ejemplos antiguos sin adaptarlos).
- Ionic 9 usa detección de cambios zoneless por defecto y los componentes de Angular 22 son `OnPush`
  por defecto: el estado de la vista va en `signal`/`computed`. Tras un `await` (p. ej. al cerrar un
  `ion-modal`) actualizar una signal, no una propiedad plana.
- Si una API o un componente no está confirmado en la documentación de Ionic 9 o Angular 22,
  consultarla o preguntar; no suponer.

## Estructura

```
frontend/src/app/
├── core/          # auth (Keycloak), http, interceptors, guards
├── shared/        # componentes reutilizables (ErrorState, StepIndicator, cabeceras)
└── features/
    ├── voter/     # flujo de voto (PWA)
    └── admin/     # consola: /global, /org, /censo, /auditoria (lazy, guard por rol)
```

## Checklist de cada pantalla

- **Estados:** carga (`ion-spinner`/`ion-progress-bar` con `role="status"`), error (`role="alert"`,
  texto claro y acción de salida), vacío y éxito.
- **Accesibilidad (WCAG 2.2 AA):** etiqueta en cada control; foco al `h1` al cambiar de pantalla;
  orden de tabulación igual al visual; objetivos táctiles de 44 px o más (48 px en papeleta y
  confirmación); color nunca como único portador de significado; texto mínimo 14 px.
- **Móvil primero:** usable desde 360 px; en gestión, tablas → tarjetas en móvil.
- **Idiomas:** todo texto visible pasa por el mecanismo i18n (es/en). Cambiar de idioma no pierde
  datos del formulario (RF-20). El mecanismo concreto se decide en P3.4.
- **Votante:** indicador de 4 pasos (RNF-9), estado del proceso y hora de cierre visibles, confirmación
  explícita en pantalla propia (RNF-7), botón de votar desactivado al pulsarlo, lenguaje no técnico
  (RNF-6). Nunca mostrar recuentos por opción antes del cierre.
- **Tema:** colores solo mediante variables Ionic de `src/theme/variables.scss` (paleta en `PROTOTYPES.md`).
- **Compilar:** `ng build`. Sin tests (ver `CLAUDE.md`).
