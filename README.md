# Freelance Quotation Builder

A Java Swing desktop app for building and keeping track of freelance quotes.

## Run on Windows

Open PowerShell in this folder with JDK 11 or newer installed.

```powershell
$build = Join-Path ([System.IO.Path]::GetTempPath()) 'quotation-builder-classes'
New-Item -ItemType Directory -Force -Path $build | Out-Null
$sources = (Get-ChildItem -Path src -Recurse -Filter '*.java').FullName
javac -d $build Main.java $sources
java -cp $build Main
```

## Build a quote

1. In **Clients**, add a client. Their email and phone are optional.
2. In **Services**, add a service and set its USD rate per project, hour, or item.
3. In **Quotes**, choose **New quote**, enter a project name, and add services with quantities. Enter a whole-number discount from 0 to 100 and choose **Calculate** to check the total.
4. Save the quote. Use **Copy quote** to paste its text into an email. Use **Edit** to record when the quote is sent, accepted, or declined.

The app saves changes to `freelance-quotations.txt` in your home folder. Each quote keeps the contact details and rates used when it was prepared.

## Code

- `Main.java` starts the Swing window.
- `src/model` contains the client, service, quote, and line-item classes. Each quote uses an `ArrayList` of items to calculate its total.
- `src/ui` contains the three tabs and their event handlers.
- `src/storage/DataStore.java` reads and writes the saved records as a sequential text file.
