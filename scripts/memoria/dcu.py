# Propuesta de DCU (13/11/2026, pendiente de comentar con el tutor). python scripts/memoria/dcu.py dentro de Plantilla-TFM-Informatica-UAL/Figuras/diagramas.
# Generates the two use-case diagrams (TikZ) of the TFM: one column of use cases inside the system boundary,
# primary actors on the left, secondary actors on the right, and the abstract «Usuario de gestión» further left.
#   python dcu.py  → writes dcu_gestion.tex and dcu_votacion.tex (style in dcu_estilo.tex)
def diagram(title, ucs, left, right, abstract=None, width=3.9, step=1.05):
    L = [f"% {title}. Generado por dcu.py: casos de uso en una columna, actores primarios a la izquierda y secundarios a la derecha.",
         "\\begin{tikzpicture}"]
    ys = {}
    for i, (k, code, name) in enumerate(ucs):
        ys[k] = -i * step
        L.append(f"\\node[uc,minimum width={width}cm] ({k}) at (1.5,{ys[k]:.2f}) {{{code}\\\\{name}}};")
    top, bot = 0.95, -(len(ucs) - 1) * step - 0.75
    half = width / 2 + 0.45
    L.append(f"\\draw[boundary] ({1.5-half:.2f},{top}) rectangle ({1.5+half:.2f},{bot:.2f});")
    L.append(f"\\node[anchor=south west,font=\\sffamily\\small\\bfseries] at ({1.5-half:.2f},{top}) {{{title}}};")
    ax_l, ax_r = 1.5 - half - 2.6, 1.5 + half + 2.6
    pos = {}
    for side, actors, x in (("west", left, ax_l), ("east", right, ax_r)):
        for name, label, cases, y in actors:
            y = y if y is not None else sum(ys[c] for c in cases) / len(cases)
            pos[name] = y
            L.append(f"\\actor{{{name}}}{{{x:.2f},{y:.2f}}}{{{label}}}")
            for c in cases:
                L.append(f"\\draw[assoc] ({name}) -- ({c}.{side});")
    if abstract:
        aname, alabel, acase, children = abstract
        cy = [pos[n] for n in children]
        L.append(f"\\actor{{{aname}}}{{{ax_l-3.0:.2f},{(max(cy)+min(cy))/2:.2f}}}{{{alabel}}}")
        for n in children:
            L.append(f"\\draw[gen] ($({n})+(-.3,.2)$) -- ({aname});")
        L.append(f"\\draw[assoc] ({aname}) ++(0,.6) |- ({acase}.west);")
    L.append("\\end{tikzpicture}")
    return "\n".join(L) + "\n"

gestion = diagram("VeriVoto · consola de gestión",
    [("rf04", "RF-04 / RF-05a", "Iniciar sesión"), ("rf01", "RF-01", "Dar de alta organización"),
     ("rf03", "RF-03", "Gestionar el censo"), ("rf02", "RF-02", "Asignar roles"),
     ("rf06", "RF-06", "Crear y configurar proceso"), ("rf07", "RF-07", "Programar proceso"),
     ("rf14", "RF-14", "Consultar participación"), ("rf15", "RF-15", "Consultar y exportar resultados"),
     ("rf16", "RF-16", "Generar acta"), ("rf18", "RF-18", "Exportar o borrar\\\\datos personales"),
     ("rf19", "RF-19", "Consultar trazabilidad"), ("rf12", "RF-12", "Verificar el registro de la urna"),
     ("rf21", "RF-21", "Consultar datos por la API")],
    [("super", "Superadmin.", ["rf01"], None), ("censo", "Gestor de censo", ["rf03"], None),
     ("admin", "Administrador de organización", ["rf02", "rf06", "rf07", "rf14", "rf15", "rf16", "rf18", "rf19"], None),
     ("audit", "Auditor", ["rf19", "rf12"], None), ("integ", "Sistema de integración", ["rf21"], None)],
    [("idp", "Proveedor de identidad", ["rf04"], None)],
    abstract=("gest", "Usuario de gestión", "rf04", ["super", "censo", "admin", "audit"]), width=4.6, step=1.4)
votacion = diagram("VeriVoto · votación",
    [("rf05b", "RF-05b", "Acceder con enlace y código\\\\o cuenta institucional"), ("rf08", "RF-08", "Acceder a la papeleta"),
     ("rf09", "RF-09", "Emitir el voto"), ("rf10", "RF-10", "Verificar el recibo"), ("rf15", "RF-15", "Consultar el resultado"),
     ("rf17", "RF-17", "Enviar avisos"), ("rf13", "RF-13", "Escrutinio automático")],
    [("votante", "Votante", ["rf05b", "rf08", "rf09", "rf10", "rf15"], None),
     ("reloj", "Temporizador del sistema", ["rf17", "rf13"], None)],
    [("idp", "Proveedor de identidad institucional", ["rf05b"], 0.0), ("correo", "Servicio de correo", ["rf05b", "rf17"], -3.3)],
    step=1.1)
open("dcu_gestion.tex", "w", encoding="utf-8").write(gestion)
open("dcu_votacion.tex", "w", encoding="utf-8").write(votacion)
