# Presentación de la defensa del TFM

| Fichero | Contenido |
| --- | --- |
| `presentacion.tex` | Diapositivas en LaTeX (beamer, 16:9). 14 diapositivas para 15 minutos y 6 de reserva |
| `estilo-ual.sty` | Colores, cabecera, pie y logotipos. Lo único que hay que cambiar si la universidad da otra plantilla |
| `guion.md` | Qué decir en cada diapositiva y tiempos (≈ 13 min de los 15) |
| `preguntas.md` | Preguntas previsibles del tribunal con respuestas orientativas |

## Compilar

Desde esta carpeta, con la misma instalación de LaTeX que la memoria:

```
pdflatex presentacion.tex
pdflatex presentacion.tex
```

Las figuras se leen de `../Plantilla-TFM-Informatica-UAL/Figuras/` (diagramas TikZ, capturas y escudos). Si cambias
un diagrama o una captura de la memoria, basta con recompilar. Los ficheros auxiliares (`.aux`, `.log`, `.nav`,
`.snm`, `.toc`, `.out`) no hace falta subirlos.

## Cambiar de plantilla

- **Otra plantilla beamer de la UAL:** sustituye `estilo-ual.sty` por la de la universidad (o cambia
  `\usepackage{estilo-ual}` por su paquete o tema). `presentacion.tex` solo usa entornos estándar más dos comandos,
  `\cifra` y `\ualbloque`; si la nueva plantilla no los tiene, cópialos de `estilo-ual.sty`.
- **Plantilla de PowerPoint:** compila el PDF y pasa el contenido diapositiva a diapositiva; el texto está en
  `presentacion.tex` y las figuras en la carpeta de la memoria.

## Antes de la defensa

- Ensaya con la tabla de tiempos de `guion.md` (15 minutos confirmados).
- Revisa que las cifras de las diapositivas 10 y 11 siguen coincidiendo con la memoria.
- Lleva el PDF en un USB además del portátil.
