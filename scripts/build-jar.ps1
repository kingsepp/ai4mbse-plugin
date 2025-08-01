# === Konfiguration ===
# Automatische Erkennung des Projektverzeichnisses basierend auf Skript-Location
$projectRoot    = (Get-Item $PSScriptRoot).Parent.FullName
$srcFolder      = "$projectRoot\src\main\java"
$binFolder      = "$projectRoot\target\classes"
$libFolder      = "$projectRoot\libs"
$jarTool        = if ($env:JAVA_HOME) { "$env:JAVA_HOME\bin\jar.exe" } else { "jar.exe" }
$javacTool      = if ($env:JAVA_HOME) { "$env:JAVA_HOME\bin\javac.exe" } else { "javac.exe" }
$mainClass      = "ai4mbse.Main"
$pluginJar      = "AI4MBSE.jar"
$manifestPath   = "$projectRoot\manifest.mf"
$outputJarPath  = "$projectRoot\target\$pluginJar"
$pluginDestDir  = "$env:LOCALAPPDATA\.magic.systems.of.systems.architect\2024x\plugins\AI4MBSE"
# MSOSA Executable automatisch erkennen
$msosaExe = if (Test-Path "C:\Program Files\Magic Systems of Systems Architect\bin\msosa.exe") {
    "C:\Program Files\Magic Systems of Systems Architect\bin\msosa.exe"
} elseif (Test-Path "${env:ProgramFiles}\Magic Systems of Systems Architect\bin\msosa.exe") {
    "${env:ProgramFiles}\Magic Systems of Systems Architect\bin\msosa.exe"
} else {
    $null
}
$projektPfad    = $null  # Optional: Pfad zu Testprojekt für automatischen Start

# Variablen initialisieren
$stubModeActive = $false

# === Bereinigen ===
if (Test-Path $binFolder) {
    Remove-Item -Recurse -Force $binFolder
}
New-Item -ItemType Directory -Path $binFolder | Out-Null

# === Kompilieren ===
# MSOSA/MagicDraw Installation erkennen
if (Test-Path "C:\Program Files\Magic Systems of Systems Architect\lib\md.jar") {
    $msosaLibPath = "C:\Program Files\Magic Systems of Systems Architect\lib\*"
    Write-Host "✅ CATIA/MagicDraw gefunden: C:\Program Files\Magic Systems of Systems Architect\lib"
    $magicDrawFound = $true
} elseif (Test-Path "${env:ProgramFiles}\Magic Systems of Systems Architect\lib\md.jar") {
    $msosaLibPath = "${env:ProgramFiles}\Magic Systems of Systems Architect\lib\*"
    Write-Host "✅ CATIA/MagicDraw gefunden: ${env:ProgramFiles}\Magic Systems of Systems Architect\lib"
    $magicDrawFound = $true
} elseif ($env:MAGICDRAW_HOME -and (Test-Path "$env:MAGICDRAW_HOME\lib\md.jar")) {
    $msosaLibPath = "$env:MAGICDRAW_HOME\lib\*"
    Write-Host "✅ CATIA/MagicDraw gefunden: $env:MAGICDRAW_HOME\lib"
    $magicDrawFound = $true
} else {
    $magicDrawFound = $false
}

# Debug: Zeige erkannte MagicDraw Installation
if ($magicDrawFound) {
    Write-Host "MagicDraw Installation erkannt - verwende vollständige Compilation"
    Write-Host "Für Open Source Demo verwenden Sie: .\test\compile-stub.cmd"
} else {
    Write-Host "Keine MagicDraw Installation erkannt - verwende Stub-Modus"
}

if ($magicDrawFound) {
    # Vollständige Compilation mit MagicDraw APIs
    $classpath = "$libFolder\*;$msosaLibPath"
    $javaFiles = Get-ChildItem -Recurse "$srcFolder\*.java"
    if ($javaFiles.Count -eq 0) {
        Write-Host "Keine Java-Dateien gefunden im Pfad: $srcFolder"
        exit 1
    }
    Write-Host "Kompiliere $($javaFiles.Count) Java-Datei(en)..."
    Write-Host "Classpath: $classpath"
    & $javacTool -encoding UTF-8 -Xlint:deprecation -cp $classpath -d $binFolder $javaFiles.FullName
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "❌ Vollständige Kompilierung fehlgeschlagen!"
        Write-Host "Fallback auf Stub-Modus..."
        $stubModeActive = $true
    }
} else {
    Write-Host "Warnung: CATIA/MagicDraw Installation nicht gefunden."
    Write-Host "   Fallback: Kompiliere nur Plugin-Stub fuer Open Source Demonstration"
    Write-Host "   Fuer vollstaendige Funktionalitaet: MagicDraw installieren oder MAGICDRAW_HOME setzen"
    Write-Host ""
    $stubModeActive = $true
}

# Stub-Compilation wenn nötig
if ($stubModeActive) {
    & $javacTool -encoding UTF-8 -d $binFolder "$srcFolder\ai4mbse\PluginStub.java"
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Plugin-Stub erfolgreich kompiliert (Open Source Modus)"
    } else {
        Write-Host "FEHLER: Auch Stub-Kompilierung fehlgeschlagen!"
        exit 1
    }
}

# === Manifest erstellen ===
$manifestContent = if ($stubModeActive) {
@"
Manifest-Version: 1.0
Main-Class: ai4mbse.PluginStub
"@
} else {
@"
Manifest-Version: 1.0
Main-Class: $mainClass
"@
}
Set-Content -Path $manifestPath -Value $manifestContent -Encoding ASCII

# === JAR bauen ===
if (Test-Path $outputJarPath) {
    Remove-Item $outputJarPath -Force
}

Write-Host "Erstelle JAR-Datei..."
Write-Host "Verwende JAR-Tool: $jarTool"

# JAR-Tool existiert pruefen
if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\jar.exe")) {
    $jarTool = "$env:JAVA_HOME\bin\jar.exe"
} elseif (Get-Command "jar.exe" -ErrorAction SilentlyContinue) {
    $jarTool = "jar.exe"
} elseif (Get-Command "jar" -ErrorAction SilentlyContinue) {
    $jarTool = "jar"
} else {
    Write-Host "FEHLER: JAR-Tool nicht gefunden!"
    Write-Host "Bitte installieren Sie Java JDK oder setzen Sie JAVA_HOME"
    exit 1
}

& $jarTool cfm $outputJarPath $manifestPath -C $binFolder .

if ($LASTEXITCODE -ne 0) {
    Write-Host "FEHLER: JAR-Erstellung fehlgeschlagen!"
    Write-Host "Ueberpruefe ob 'jar' Tool verfuegbar ist (Java JDK erforderlich)"
    exit 1
}

# === Manifest bereinigen ===
Remove-Item $manifestPath -Force

# === Plugin kopieren ===
if (Test-Path $outputJarPath) {
    if (-Not (Test-Path $pluginDestDir)) {
        New-Item -ItemType Directory -Path $pluginDestDir | Out-Null
    }
    Copy-Item $outputJarPath -Destination $pluginDestDir -Force
    
    # WICHTIG: plugin.xml auch kopieren (für MagicDraw Plugin-Erkennung)
    $pluginXmlSource = "$projectRoot\plugin.xml"
    if (Test-Path $pluginXmlSource) {
        Copy-Item $pluginXmlSource -Destination $pluginDestDir -Force
        Write-Host "Plugin-Dateien kopiert:"
        Write-Host "   $pluginDestDir\$pluginJar"
        Write-Host "   $pluginDestDir\plugin.xml"
    } else {
        Write-Host "WARNUNG: plugin.xml nicht gefunden!"
    }
    
    Write-Host ""
    Write-Host "===== BUILD ERFOLGREICH ====="
    if ($stubModeActive) {
        Write-Host "Plugin-Stub gebaut und kopiert nach:"
        Write-Host "   $outputJarPath"
        Write-Host ""
        Write-Host "HINWEIS: Dies ist ein Plugin-Stub für Open Source Demonstration."
        Write-Host "Für vollständige Funktionalität benötigen Sie:"
        Write-Host "1. MagicDraw/Cameo Systems Modeler 2024x Installation"
        Write-Host "2. Google Gemini API Key"
    } else {
        Write-Host "Vollständiges Plugin erfolgreich gebaut und installiert!"
        Write-Host "Installation-Verzeichnis: $pluginDestDir"
        Write-Host ""
        Write-Host "Nächste Schritte:"
        Write-Host "1. MagicDraw/MSOSA neu starten"
        Write-Host "2. Plugin sollte im Tools-Menü verfügbar sein"
        Write-Host "3. Google Gemini API Key konfigurieren (siehe README.md)"
    }
} else {
    Write-Host "FEHLER: JAR-Datei wurde nicht erstellt!"
    exit 1
}

# === MSOSA neustarten & Projekt öffnen ===
if ($msosaExe -and (Test-Path $msosaExe)) {
    Write-Host "Starte MSOSA neu..."
    Get-Process | Where-Object { $_.Path -eq $msosaExe } | ForEach-Object { $_.Kill(); Start-Sleep -Seconds 2 }

    if (Test-Path $projektPfad) {
        Start-Process $msosaExe -ArgumentList "`"$projektPfad`""
    } else {
        Write-Host "⚠️ Projektdatei nicht gefunden: $projektPfad"
    }
} else {
    Write-Host "⚠️ MSOSA.exe nicht gefunden unter: $msosaExe"
}
