param([switch]$Test)

$ErrorActionPreference = "Stop"
$driverName = "sqlite-jdbc-3.54.1.0.jar"
$driverFolder = Join-Path $PSScriptRoot "lib"
$driverPath = Join-Path $driverFolder $driverName
$driverHash = "F8DCE420A6016278497F4F0560F65602B9DB858DE141B541CF91A4644EBE4639"

if (-not (Test-Path -LiteralPath $driverPath)) {
    New-Item -ItemType Directory -Path $driverFolder -Force | Out-Null
    $download = "$driverPath.download"
    $driverUrl = "https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.54.1.0/$driverName"
    try {
        Invoke-WebRequest -UseBasicParsing -Uri $driverUrl -OutFile $download
        if ((Get-FileHash -Algorithm SHA256 -LiteralPath $download).Hash -ne $driverHash) {
            throw "The SQLite driver download failed its checksum verification."
        }
        Move-Item -LiteralPath $download -Destination $driverPath
    } finally {
        if (Test-Path -LiteralPath $download) {
            Remove-Item -LiteralPath $download
        }
    }
}

$classes = Join-Path $PSScriptRoot "out"
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$sourceFolders = @((Join-Path $PSScriptRoot "src"))
if ($Test) { $sourceFolders += Join-Path $PSScriptRoot "test" }
$sources = @(Join-Path $PSScriptRoot "Main.java") +
    @(Get-ChildItem -LiteralPath $sourceFolders -Recurse -File -Filter "*.java").FullName
& javac --release 11 -encoding UTF-8 -d $classes @sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$classpath = $classes + [IO.Path]::PathSeparator + $driverPath
$runtimeArgs = @()
$javaVersion = @(& java --version)[0]
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if ($javaVersion -match '^(?:openjdk|java) (\d+)' -and [int]$Matches[1] -ge 24) {
    $runtimeArgs += "--enable-native-access=ALL-UNNAMED"
}

if ($Test) { $entryPoints = "ModelTest", "SwingTest" } else { $entryPoints = "Main" }
foreach ($entryPoint in $entryPoints) {
    & java @runtimeArgs -cp $classpath $entryPoint
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
