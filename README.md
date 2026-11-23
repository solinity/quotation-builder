# Freelance Quotation Builder

Run the app from PowerShell with JDK 11 or newer on your PATH.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1
```

The first run downloads the pinned SQLite JDBC driver into `lib`. Later runs use the local copy.

Use **File → Save Session** to write your clients, offerings, and saved quotations to a `.sqlite` database. **File → Load Session** opens either a `.sqlite` database or a legacy `.qbs` session. After opening a `.qbs` file, **Save Session** creates a separate SQLite database. **Help → Show Session File Location** shows the active file’s path.

Run the model and Swing tests with `powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 -Test`.
