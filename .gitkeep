$dirs = @(
  "backend\src\main\java",
  "backend\src\main\resources",
  "backend\src\test",
  "frontend\src",
  "frontend\e2e",
  "docs\requirements",
  "docs\architecture",
  "docs\security",
  "docs\adr",
  "infra",
  "scripts",
  ".github\workflows"
)
foreach ($d in $dirs) { New-Item -ItemType File -Force -Path "$d\.gitkeep" | Out-Null }