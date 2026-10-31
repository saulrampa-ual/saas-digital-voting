# Preparación y registro de pruebas de carga

Estado: **parcial; no ejecutado**. `tests/load/voting.js` mide solo la emisión de votos con 500 VU y un token
único por iteración. El escenario de acceso por código y el de escrutinio se deben preparar y medir aparte antes de
cerrar P7.5. No se han iniciado servicios ni se han generado resultados de rendimiento.

## Preparación sintética

1. Levantar el entorno local de evaluación conforme a `docs/architecture/local-infrastructure.md`, usando únicamente
   cuentas, organizaciones y un censo de prueba. No usar direcciones ni identidades reales.
2. Crear y abrir un proceso con al menos 500 electores sintéticos y una opción válida. Acordar una instancia aislada;
   no enviar la carga a producción ni a entornos compartidos.
3. Completar la fase de autenticación antes de medir: emitir al menos 500 tokens de voto distintos para ese proceso.
   Guardarlos en un JSON local **fuera del repositorio**, como un array de strings. El archivo contiene credenciales
   de prueba y se elimina al finalizar. No incluir tokens, enlaces, códigos ni recibos en logs o informes.
4. Confirmar que todos los tokens corresponden al mismo proceso abierto, que son distintos y que las opciones están
   en la papeleta. La comprobación de unicidad también se ejecuta al iniciar el script.

## Ejecución

Desde PowerShell, con k6 instalado por el autor, configurar las variables localmente (no guardarlas en el repo):

```powershell
$env:BASE_URL = "http://localhost:8000"
$env:PROCESS_ID = "<id-del-proceso-sintetico>"
$env:OPTION_ID = "<id-de-opcion-valida>"
$env:TOKENS_FILE = "<ruta-local-fuera-del-repo>\tokens.json"
$env:VUS = "500"
$env:ITERATIONS = "1"
k6 run tests/load/voting.js
```

Repetir tres veces con procesos y tokens sintéticos nuevos, equivalentes y con configuración idéntica; dejar que
k6 devuelva su resumen final. El criterio fijado es p95 de emisión < 2 s; conservar el p50 (mediana), p90, p95, p99,
total de peticiones, errores HTTP y comprobaciones fallidas. Un fallo del umbral se informa sin cambiar el criterio.
Cada token se usa una vez para evitar medir el rechazo de doble voto.

## Datos que debe registrar el autor

| Campo | Valor |
| --- | --- |
| Fecha, sistema operativo, CPU y RAM | PENDIENTE |
| Versiones de k6, Java, PostgreSQL y Docker | PENDIENTE |
| Límites de CPU/RAM y configuración del stack | PENDIENTE |
| Número de VU, iteraciones y duración | 500 VU × 1; ejecución pendiente |
| Ejecuciones 1, 2 y 3: p50/p90/p95/p99 y errores | PENDIENTE |
| CPU/RAM pico por contenedor (`docker stats`) | PENDIENTE |
| Cadena íntegra tras la carga | PENDIENTE |
| Votos en urna = personas marcadas como votantes | PENDIENTE |
| Escenarios independientes de acceso y escrutinio | PENDIENTE |

La medición cubre el tiempo HTTP de la petición de emisión a través del gateway; no equivale a una prueba de
capacidad de producción ni mide la experiencia completa del votante.
