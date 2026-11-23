# Presentation Guide

## Explain the project in one minute

Freelancers need to prepare consistent estimates and find them again later. Our application keeps client contacts, service prices, and quotations in one Swing window. Each quote has a permanent number and one or more service lines. The freelancer can preview the quotation, export it for a client, and find it again after restarting the app. Each line remembers its agreed price, so later catalog updates leave its total intact.

```text
Main → MainFrame → ClientsPanel / OfferingsPanel / QuotesPanel
                     All three use the same DataStore

DataStore → clients, offerings, quotations
DataStore → DataFile → data/quotations.txt
Quote     → one Client and a list of QuoteItems
QuoteItem → one Offering, a quantity, and an agreed unit price
QuotesPanel → QuotePreview → optional text export
```

## Demonstration sequence

1. In **Clients**, add two clients named **Alex Rivera** with different emails. Search for `river` to show both results. Select one and update its phone.
2. In **Offerings**, add **Design** at 75, another **Design** at 120, and **Consultation** at 30. Give the two Design offerings different descriptions. Search for `sign`, then click **Show All**.
3. In **Quotations**, select the intended Alex Rivera by email. Enter a sample name in **Prepared by**. Add two units of the Design offering priced at 120 and two units of Consultation. Add and remove an extra line to demonstrate **Remove Item**. Enter a 10% discount and click **Calculate**. Point out the **300.00** subtotal, **−30.00** discount, and **270.00** total.
4. Set the status to **SENT** and select **Save Quote**. Note the assigned number. Open **Preview Quote** to show the document, including the sender, client, and line prices. Export it as text and open the exported file.
5. Start a new quote, search for the saved one by its number, and click **Open**. Change the status to **ACCEPTED**. Click **New Quote** and decline the prompt to show that unsaved edits are retained. Save the status change.
6. Change the 120 Design offering to 160 in **Offerings**. Return to the quote and show its agreed price of 120.00. Attempt to delete the referenced offering and client to show the protective messages.
7. Close and reopen the application from the same folder. Open the same quote by its number and verify its status and prices. Show an error using a quantity of zero or a discount above 100.

## Walk through the code

**Start and navigation.** `Main.main()` opens the local store and creates a `MainFrame` through `SwingUtilities.invokeLater()`. The frame passes the store to the three panel constructors. Selecting Quotations refreshes its choices from that store. `SwingStyle` shares the visual rules, and the layout managers position and resize the controls.

**Add, display, and search.** A button listener reads the form and calls a constructor or model method. For a client or offering, the panel adds the object to the store and rebuilds its list. For a search, the store loops over its list and returns a separate list of matches. Searching leaves the stored records intact.

**Update and delete.** Selecting a client or offering fills the form. Update constructs a validated replacement and passes it to `DataStore`. The store copies those values into the selected object so existing quotations keep their references. Delete asks for confirmation. The store checks each saved quote's client and the offerings used in its items.

**Create, open, and save quotations.** The editor holds a draft with its own item list. Opening a saved quote copies its details into the draft. Button and field listeners update the editor status. Save checks the date, client, status, discount, and references. `DataStore` gives a new quote the next number and preserves that number on subsequent edits.

**Calculation and preview.** `QuoteItem.getLineTotal()` multiplies quantity by the item's stored unit price. `Quote.getSubtotal()` loops over the lines and adds their totals. `Quote.getDiscountAmount()` gives the deduction and `Quote.getTotal()` gives the final amount. The editor and `QuotePreview` call these same methods. The preview puts the formatted document into a read-only text area and writes that text to the file selected by the user.

**File I/O.** `DataFile` uses a `BufferedWriter` for saving and a `BufferedReader` for loading. It writes a new file beside the current one and replaces the current file after the write finishes. When reading, it creates clients and offerings before attaching them to quotations. The separate next-number field gives a new quotation a fresh number after deletion or restart.

**Object-oriented programming.** Private fields and validated methods demonstrate encapsulation. Constructors initialize usable objects. A quotation has a client and a list of items. Each item knows its offering. Inheritance is visible where the frame extends `JFrame` and each tab panel extends `JPanel`.

## Possible questions

| Professor's question | Suggested answer |
| --- | --- |
| How do you know which Alex Rivera was selected? | Swing lists and combos hold the actual `Client` objects. The rendered record number and contact details help the user select a row. The code uses the selected object directly. |
| What happens if you edit the client's name? | The existing quotation references the same client, so its display and search use the updated name. |
| Why store an offering and also store the unit price? | The reference identifies the service. The saved price represents what was agreed for that particular line. New lines use the current catalog price. |
| Where do you check bad input? | The panels parse text and catch errors to show a dialog. The models validate the numbers and required values. The store checks that saved references exist and that a referenced record survives deletion attempts. |
| Why use ArrayList? | The number of records can grow or shrink at runtime. We add and remove objects and use loops for searching, checking references, and summing items. |
| Can a quotation be saved with zero items or a free item? | An empty draft displays zero and cannot be saved. A real item can have a zero unit price if its quantity is greater than zero. |
| What if an item total overflows? | The item constructor checks that the multiplication is finite. `Quote.addItem()` also checks the proposed subtotal before adding the line. |
| Why use one DataStore? | The same object is passed into all three tabs, so a client created in one tab becomes a choice in another. It also saves changes through one path. |
| What if the data file is damaged? | The app opens a labeled temporary session and leaves the file available for recovery. |
| How do you handle currency? | The freelancer uses one currency throughout. The display shows numeric amounts, so the same catalog prices stay meaningful across quotations. |
| How did you test your app? | `ModelTest` checks formulas, bounds, stored relationships, restart, and write failures. `SwingTest` uses real controls to check the workflow, preview, export, and resizing. |
