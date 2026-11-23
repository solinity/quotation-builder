# Presentation Guide

## Explain the project in one minute

Freelancers need to prepare consistent estimates and look up earlier quotes. Our application keeps client contacts, service prices, and quotations in one Swing window. A quotation is linked to a client and has one or more service lines. Each line remembers its agreed price, so a later catalog update leaves previous totals intact.

```text
Main → MainFrame → ClientsPanel / OfferingsPanel / QuotesPanel
                     All three use the same DataStore

DataStore → clients, offerings, quotations
Quote     → one Client and a list of QuoteItems
QuoteItem → one Offering, a quantity, and an agreed unit price
```

## Demonstration sequence

1. In **Clients**, add two clients named **Alex Rivera** with different emails. Search for `alex rivera` and show that both are returned. Select one and update its phone.
2. In **Offerings**, add **Design** at 75, another **Design** at 120, and **Consultation** at 30. Give the two Design offerings different descriptions. Search for `design`, then click **Show All**.
3. In **Quotations**, select the intended Alex Rivera by email. Add two units of the Design offering priced at 120 and two units of Consultation. Demonstrate **Remove Item** by adding and removing an extra line. Enter a 10% discount and click **Calculate**. The subtotal should be **300.00**, and the total should be **270.00**.
4. Set the status to **SENT** and save. Start a new quote, search for the saved one by client name, and click **Open**. Change the status to **ACCEPTED** and save again.
5. Change the 120 Design offering to 160 in **Offerings**. Return to the existing quote and point out that its line still shows 120.00. Attempt to delete the referenced offering and the referenced client to show the protective messages.
6. Show an error using a quantity of zero or a discount above 100. Finish by deleting the quote. The former references are now available for deletion if no other saved quote uses them.

## Walk through the code

**Start and navigation.** `Main.main()` creates one `DataStore` and one `MainFrame` through `SwingUtilities.invokeLater()`, as in the registration lab. The frame passes the same store to the three panel constructors. Selecting Quotations refreshes its client and offering choices from that store.

**Add, display, and search.** A button listener reads the form and calls a constructor or model method. For a client or offering, the panel adds the object to the store and rebuilds its list. For a search, the store loops over its list and returns a separate list of matches. Searching leaves the stored records intact.

**Update and delete.** Selecting a client or offering fills the form. Update first constructs a validated replacement from all the fields. The panel then copies those values through the original object's setters. Delete checks saved quotes for references to the selected object. A quote checks its client directly and its offerings through its items.

**Create, open, and save quotations.** The editor holds a draft with its own item list. Opening an existing quote copies its data into the draft. Save checks the date, client, status, discount, and item references. It then adds the new quote or updates the opened quote. The saved quote is unaffected by rejected or unsaved edits.

**Calculation.** `QuoteItem.getLineTotal()` multiplies quantity by the item's stored unit price. `Quote.getSubtotal()` loops over the lines and adds their totals. `Quote.getTotal()` applies the discount as a fraction of 100. A discount of zero keeps the subtotal and a discount of 100 makes the total zero.

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
| Why use one DataStore? | The same object is passed into all three tabs, so a client created in one tab becomes a choice in another. Its collections last for the current session. |
| How did you test your app? | `ModelTest` checks formulas, bounds, and stored relationships. `SwingTest` opens the window and exercises the real buttons, selections, dialogs, and tab changes with sample inputs. |
