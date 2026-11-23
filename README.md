# Freelance Quotation Builder

A Java Swing desktop application for managing client contacts, maintaining a service catalog, and preparing quotations that can be previewed and shared as text.

## Compile and run on Windows

Use **JDK 11 or newer** with `java` and `javac` on your PATH. Run these commands from PowerShell in the project folder.

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac --release 11 -encoding UTF-8 -d out Main.java src/model/*.java src/storage/*.java src/ui/*.java
java -cp out Main
```

Compiled classes go into the ignored `out` directory. Swing and the other Java APIs are supplied by the JDK.

## Use the application

1. In **Clients**, enter a name and optional email and phone, then click **Add Client**. A nonempty email must contain `@`. Select a result to edit or delete that client. Use **Clear Form** to return to the add form. Duplicate names are allowed, and the directory displays email and phone so you can identify each record.
2. In **Offerings**, add a product or service with a name, optional description, and nonnegative unit price. Enter decimals with a period, such as `75.50`. The price represents one unit or one hour. Catalog price edits apply to newly added quotation lines. Existing lines keep their agreed prices.
3. In **Quotations**, select a client and check the date and status. The date starts at today and accepts `YYYY-MM-DD`. Enter your business or freelancer name in **Prepared by** if you want it on the quotation. Choose an offering, enter a positive quantity or number of hours, and click **Add Item**. Select a line to **Remove Item**. Enter a discount from 0% to 100% and click **Calculate** to refresh the summary.
4. Click **Preview Quote** to review the itemized document. **Save Quote** assigns a permanent number such as `Q-0001` and requires at least one item. To share the numbered document, open the preview after saving and click **Export as Text**. An unsaved draft shows **Pending save** in the preview.
5. Select a quotation in **Saved quotations** and click **Open** to edit it. Use **Save Quote** to apply changes or **Delete Quote** to remove the selected quotation. The editor identifies new quotes, open quotes, and unsaved changes. Starting a new quote, opening another record, or closing the window prompts you before discarding quote edits.

Search by part of a name in any tab. The quotation search also accepts a quote number, such as `Q-0001`. **Show All** clears the search. Delete actions ask for confirmation, and a saved quotation protects its client and offerings from deletion. If an offering used only in an unsaved draft is deleted, remove the marked line before saving the draft.

## Local data

The application automatically saves successful adds, updates, and deletions to `data/quotations.txt` relative to the folder where you run it. Run from the same folder to reopen the same records. Back up that file to keep a copy of your clients, catalog, quotations, and next quotation number. The folder is ignored by Git. The snapshot is designed for one running application at a time.

If the file is missing, the application starts with an empty store. If it cannot read the file, it opens a clearly labeled temporary session and preserves the original file for recovery. Records entered in that session last until you close the window.

## Application structure

| Location | Responsibility |
| --- | --- |
| `Main.java` | Starts Swing, creates the shared `DataStore`, and opens the window |
| `src/model` | Defines `Client`, `Offering`, `QuoteItem`, and `Quote`; validates model data and calculates totals |
| `src/storage/DataStore.java` | Manages the three record collections, partial searches, quote numbers, and relationship rules |
| `src/storage/DataFile.java` | Reads and writes the UTF-8 snapshot using buffered text I/O |
| `src/ui` | Hosts the window, forms, lists, workflow, and quotation preview and export |
| `test` | Plain Java tests for the model, storage, and Swing integration |

## Run the tests

After compiling the application, run these commands in the same PowerShell window.

```powershell
javac --release 11 -encoding UTF-8 -cp out -d out test/*.java
java -cp out ModelTest
java -cp out SwingTest
```

`SwingTest` requires a graphical session. To capture the three populated tabs and preview, use `java -cp out SwingTest screenshots`. Tests use sample records and temporary files.

## Limitations

All amounts use the same currency chosen by the freelancer. The catalog and preview display numeric amounts without a currency symbol. Calculations use `double` and amounts are rounded for display. Quotation statuses are set manually.

See [the short project report](PROJECT_REPORT.md) and [the presentation guide](PRESENTATION_GUIDE.md).
