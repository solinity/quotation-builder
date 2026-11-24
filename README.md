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

A freelancer enters client details, offerings, quantities, and discounts through the Swing tabs. Each button calls a method that reads the form, validates the input, and updates the display. The Clients and Offerings tabs inherit their common actions from `CatalogPanel`. The shared `DataStore` keeps clients, offerings, and quotations in `ArrayList` collections.

A `Quote` links one client to a list of `QuoteItem` objects. Each item holds its offering and agreed unit price. The model calculates the item discounts, adds the discounted line totals, and applies the quotation discount. `SessionDatabase` saves those records with JDBC. `QuoteDialog` and `QuotePdf` display or export the result in the selected currency.

Run the model and Swing tests with `powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 -Test`.
