Report seed and loading
=======================

Files in this folder:

- `report_seed.sql` — minimal sample data used by the report endpoints (users, company, videogames, purchases, comments, wallet, commissions).
- `load_report_seed.ps1` — PowerShell helper to import the SQL into MySQL or show H2 import guidance.

How to use (MySQL):

1. Edit `load_report_seed.ps1` parameters or run it with explicit parameters:

```powershell
.\load_report_seed.ps1 -dbType mysql -host localhost -port 3306 -user root -password yourpass -database tienda
```

2. The script invokes the `mysql` client. Ensure `mysql` is on your PATH.

How to use (H2):

1. Use the H2 RunScript tool:

```powershell
java -cp path\to\h2.jar org.h2.tools.RunScript -url "jdbc:h2:~/tienda" -user sa -script "report_seed.sql"
```

Notes and troubleshooting:
- The included SQL assumes table and column names used in the project (Usuario, Empresa, Videojuego, Compra, Comentario, Cartera, Comision_Empresa, Comision_Global). If your schema differs, adapt `report_seed.sql` accordingly before running.
- If you re-generate the DB from `schema.sql`, run the seed afterwards to populate data used by the reports.
