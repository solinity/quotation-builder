# Freelance Quotation Builder

Universidad Politécnica Taiwán-Paraguay · Java Programming · Semester 2026.2

## Problem and objectives

Freelancers often rebuild price estimates from separate contact lists, price lists, and spreadsheets. This can cause inconsistent totals and make previous terms hard to reconstruct after rates change. The application gives a freelancer one place to keep client details and a service catalog, prepare a quotation, calculate its cost, and track its status.

## System design

The freelancer enters contacts, offerings, quantities, dates, and discounts in three Swing tabs. Each panel reads from the same `DataStore`. The store has private `ArrayList` collections for the three types of records. The model calculates totals and the panels display them. The main output is a saved quotation with its client, date, status, itemized charges, subtotal, and final total.

| Class | Responsibility |
| --- | --- |
| `Main`, `MainFrame` | Start Swing, create one shared store, and host the navigation tabs |
| `Client`, `Offering` | Validate and store contact details and service catalog entries |
| `QuoteItem` | Reference an offering and keep the agreed price, quantity, and line calculation |
| `Quote` | Reference a client, hold a list of items, and calculate subtotal and discounted total |
| `DataStore` | Store, find, save, and remove records while protecting their relationships |
| `ClientsPanel`, `OfferingsPanel`, `QuotesPanel` | Read forms, respond to Swing events, show validation messages, and refresh the display |
| `FormPanel`, `RecordRenderer` | Lay out labeled fields and show records with distinguishing details |

The source demonstrates primitive variables, strings, conditions, loops, methods, constructors, encapsulation, `ArrayList`, and object relationships. The panels extend Swing classes and use button and list-selection listeners. The design follows the project handout and the course examples for encapsulation, object relationships, GUI events, and exceptions. The references included the course plan, grading scheme, Units 7–10 and 11–14, Lab 6, the Swing event-handling lab, and Lab 9's `StudentRegistration.java`.

## Operations and validation

The Clients and Offerings tabs add, display, search, update, and delete records. Name searches use a loop with `equalsIgnoreCase()`. Selecting a row loads that object's fields. Updates validate all replacement values before calling setters on the original object. Duplicate names are allowed, and deletion uses the selected record.

The Quotations tab creates, opens, searches, updates, and deletes quotations. Each line is quantity multiplied by the unit price at the time the line was added. The subtotal is the sum of all line totals. The final total is the subtotal multiplied by `1 - discountPercent / 100`. For example, two units at 120 and two at 30 produce a subtotal of 300 and a total of 270 with a 10% discount. Changes in the quote editor reach the saved record only after a successful save.

The program uses validation and Swing dialogs for missing selections, blank required names, an email without `@`, invalid dates, missing status, invalid numerical text, non-finite numbers, out-of-range discounts, and overflow. Empty quotations display zero and require at least one line to be saved. The store prevents deleting clients and offerings still used in saved quotations.

## Testing and actual results

The application and tests compiled on Windows with Temurin JDK 25.0.4.1 using `javac --release 11`. A separate launch check ran the `Main` entry point and confirmed that Windows reported the **Freelance Quotation Builder** window.

| Executed test | Result |
| --- | --- |
| `ModelTest` client and offering groups | Passed. Verified add, retrieve, exact-name search, duplicates, deletion, and validation including zero price, negative values, NaN, and infinity. |
| `ModelTest` calculation and storage groups | Passed. Verified empty, single-item, and multi-item totals, discounts of 0%, 25%, and 100%, overflow, reference protection, saved updates, and invalid references. |
| `SwingTest` client and offering groups | Passed. Exercised the actual forms, buttons, list selections, search and reset, atomic updates, and error dialogs. |
| `SwingTest` quotation and integration groups | Passed. Exercised all tabs, item add and removal, calculated labels, save, search, open, update, status and date changes, deletion, and validation. Confirmed shared records, preserved prices, and rejected stale draft references. |

All eight groups passed on the final build. The Swing test operates a real window through its components. The images below were captured from that window with sample records and visually reviewed. A separate mouse-and-keyboard pass and the live classroom demonstration were not performed.

## GUI screenshots

**Clients** · Duplicate names with distinct contact details.

![Clients tab with the second Alex Rivera record selected](screenshots/clients.png)

**Offerings** · Two Design offerings with distinct unit prices and descriptions.

![Offerings tab with the Brand package offering selected](screenshots/offerings.png)

**Quotations** · Reopened quotation with two line items, a subtotal of 300.00, and a discounted total of 270.00.

![Quotations tab showing the reopened sent quotation and its totals](screenshots/quotations.png)

## Limitations

The store is session-only. Closing the window clears the records. The quotation uses ordinary `double` arithmetic with formatted display amounts and assumes a single currency. Catalog names and contact details remain linked to their current records while agreed unit prices are retained in each quotation item.
