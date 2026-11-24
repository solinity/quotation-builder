# Freelance Quotation Builder

Run the app from PowerShell with JDK 11 or newer on your PATH.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1
```

The first run downloads the pinned SQLite JDBC driver into `lib`. Later runs use the local copy.

Use **File → Save Session** to write your clients, offerings, and saved quotations to a `.sqlite` database. **File → Load Session** opens either a `.sqlite` database or a legacy `.qbs` session. After opening a `.qbs` file, **Save Session** creates a separate SQLite database. **Help → Show Session File Location** shows the active file’s path.

In **Settings**, enter how many guaraníes equal 1 US dollar under **Guaraníes per 1 USD**, then click **Apply**. Choose **Currency** to view and enter prices in USD or PYG. **Save Session** stores the rate and currency with the session.

On the **Quotations** tab, select a saved quotation and click **Open** to see its details, **Edit** to make changes, or **Export PDF** to save a PDF in the selected currency.

## System design

A freelancer enters client details, offerings, quantities, and discounts through the Swing tabs. Clients and Offerings share a `CatalogPanel`. The `DataStore` keeps records in `ArrayList` collections.

A `Quote` links a client to its items and applies the quotation discount after the item discounts. Each item keeps its agreed unit price. `SessionDatabase` saves records with JDBC. The preview and PDF share the same content and currency.

Run the model and Swing tests with `powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 -Test`.
