#!/usr/bin/env python3
"""Generate the Spanish and English voter census workbooks in frontend/public/templates."""

from pathlib import Path

from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.utils import get_column_letter


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "frontend" / "public" / "templates"

TEMPLATES = {
    "census-template.xlsx": {
        "headers": ["Full name *", "Email *", "Phone", "Institutional ID", "Language for messages"],
        "examples": [
            ["María García López", "maria.garcia@example.com", "600123456", "12345678A", "Spanish"],
            ["John Smith", "john.smith@example.com", "600654321", "87654321B", "English"],
        ],
        "instructions": ["Instructions", "Delete the two grey example rows before uploading.",
                         "Name and email are required. Language may be Spanish or English."],
        "widths": [30, 34, 16, 28, 30],
    },
    "plantilla-censo.xlsx": {
        "headers": ["Nombre completo *", "Correo electrónico *", "Teléfono", "Identificador institucional",
                    "Idioma de las comunicaciones"],
        "examples": [
            ["María García López", "maria.garcia@example.com", "600123456", "12345678A", "Español"],
            ["John Smith", "john.smith@example.com", "600654321", "87654321B", "Inglés"],
        ],
        "instructions": ["Instrucciones", "Borra las dos filas grises de ejemplo antes de subir el fichero.",
                         "El nombre y el correo son obligatorios. El idioma puede ser español o inglés."],
        "widths": [30, 34, 16, 28, 30],
    },
}


def make_template(template: dict, destination: Path) -> None:
    workbook = Workbook()
    voters = workbook.active
    voters.title = "Censo" if "plantilla" in destination.name else "Census"
    voters.append(template["headers"])

    header_fill = PatternFill("solid", fgColor="D7EFE4")
    example_fill = PatternFill("solid", fgColor="E8EEF6")
    muted_font = Font(color="6B7785", italic=True)
    for cell in voters[1]:
        cell.font = Font(bold=True)
        cell.fill = header_fill
        cell.alignment = Alignment(vertical="center")

    for row in template["examples"]:
        voters.append(row)
        for cell in voters[voters.max_row]:
            cell.fill = example_fill
            cell.font = muted_font

    # Store phone and institutional ID as text for the entire columns, preserving leading zeroes.
    for column in ("C", "D"):
        voters.column_dimensions[column].number_format = "@"
    for row_number in range(4, 1001):
        for column in (3, 4):
            voters.cell(row=row_number, column=column).number_format = "@"
    for index, width in enumerate(template["widths"], start=1):
        voters.column_dimensions[get_column_letter(index)].width = width
    voters.freeze_panes = "A2"

    instructions = workbook.create_sheet("Instrucciones" if "plantilla" in destination.name else "Instructions")
    for line in template["instructions"]:
        instructions.append([line])
    instructions.column_dimensions["A"].width = 95
    instructions["A1"].font = Font(bold=True, size=13)

    destination.parent.mkdir(parents=True, exist_ok=True)
    workbook.save(destination)


def main() -> None:
    for name, content in TEMPLATES.items():
        make_template(content, OUTPUT / name)
        print(f"Generated {OUTPUT / name}")


if __name__ == "__main__":
    main()
