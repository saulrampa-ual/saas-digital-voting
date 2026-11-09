# Census workbook templates

Regenerate both downloadable workbooks from the repository root with:

```powershell
python -m pip install openpyxl
python tools/generate_census_templates.py
```

The script writes `census-template.xlsx` and `plantilla-censo.xlsx` to
`frontend/public/templates/`. Phone and institutional ID columns are stored as
text so leading zeroes are preserved.
