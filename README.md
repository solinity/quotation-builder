# Freelance Quotation Builder

A Java Swing desktop application for freelancers who need to manage client contacts, maintain a service catalog, and prepare cost estimates with quantities, discounts, and quotation statuses.

## Compile and run on Windows

Use **JDK 11 or newer** with `java` and `javac` on your PATH. Run these commands from PowerShell in the project folder.

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac --release 11 -d out Main.java src/model/*.java src/storage/*.java src/ui/*.java
java -cp out Main
```

Compiled classes go into the ignored `out` directory. Swing and the other Java APIs are supplied by the JDK.

## Use the application

- **Clients.** Enter a name and optional email and phone, then click **Add Client**. A nonempty email must contain `@`. Select a row to load it into the form. Use **Update** to apply your edits or **Delete** to remove that row. **Clear Form** clears the fields and selection.
- **Offerings.** Enter a name, optional description, and nonnegative unit price. Click **Add Offering**. Select a row to view, update, or delete it. Enter decimal numbers with a period, for example `75.50`.
- **Quotations.** Click **New Quote**, select a client, and check the date and status. The date starts at today and accepts `YYYY-MM-DD`. Select an offering and enter a positive quantity or number of hours for each **Add Item** action. Select a line and click **Remove Item** to remove it. Enter a discount from 0 to 100 and click **Calculate** to apply it to the preview. **Save Quote** validates the form, calculates the amounts, and saves a quotation with at least one item.
- **Find and manage saved records.** Each tab supports full-name search with case ignored. Quotations are searched by client name. Use **Show All** to reset the filter. For a saved quotation, select its row and click **Open**. **Save Quote** applies changes to the opened quotation. **Delete Quote** removes the highlighted quotation. **New Quote** or **Open** replaces the current editor contents, so save any edits first.

Duplicate names can appear in any search result. The displayed numbers identify positions in the current session's record lists and may change after deletion. A saved quotation protects its associated client and offerings from deletion. If an offering used only in an unsaved draft is deleted, remove the marked line before saving the draft. Existing lines retain their agreed unit prices when catalog prices change.

## Application structure

| Location | Responsibility |
| --- | --- |
| `Main.java` | Starts Swing, creates the shared `DataStore`, and opens the window |
| `src/model` | Defines `Client`, `Offering`, `QuoteItem`, and `Quote`; validates model data and calculates totals |
| `src/storage/DataStore.java` | Holds the three record collections, searches them, and checks quotation references |
| `src/ui` | Hosts the tabbed window, module panels, forms, and list renderers |
| `test` | Plain Java tests for the model, storage, and Swing integration |

## Run the tests

After compiling the application, run these commands in the same PowerShell window.

```powershell
javac --release 11 -cp out -d out test/*.java
java -cp out ModelTest
java -cp out SwingTest
```

`SwingTest` requires a graphical session. To capture its three populated tabs, use `java -cp out SwingTest screenshots`.

## Limitations

Records are kept in memory and cleared when the application closes. Saved quotations use one consistent currency chosen by the user outside the application. Amounts use `double` arithmetic and are rounded for display. The status is a label that users can update directly.

See [the short project report](PROJECT_REPORT.md) and [the presentation guide](PRESENTATION_GUIDE.md).
