# AI4MBSE Plugin für MagicDraw/Cameo Systems Modeler

AI-gestützte Requirement-Allocation für Cameo Systems Modeler/MagicDraw mit Google Gemini API.

## Hinweis

**Dieses Repository wird "as is" und ohne Wartung, Support oder Gewährleistung veröffentlicht.**

- Jeder kann es frei verwenden, ändern oder weitergeben.
- Es sind keine weiteren Updates oder Bugfixes geplant.
- Es besteht keinerlei Anspruch auf Support, Wartung oder Fehlerbehebung.

## Was ist das?

Ein Plugin für MagicDraw/Cameo Systems Modeler, das KI nutzt, um Systemanforderungen automatisch den passenden Subsystemen zuzuordnen.

**Hauptfunktionen:**
- KI-gestützte Requirement-zu-Subsystem-Zuordnung
- Asynchrone Verarbeitung mit nicht-blockierender Benutzeroberfläche (UI)
- Automatische Erstellung von SysML Satisfy-Abhängigkeiten
- Konfidenz-Bewertung der KI-Empfehlungen

## Voraussetzungen

- MagicDraw/Cameo Systems Modeler 2024x
- Java 17+
- Google Gemini API-Schlüssel

## Installation

1. **Build:**
   ```powershell
   powershell -ExecutionPolicy Bypass -File "scripts/build-jar.ps1"
   ```

2. **Installation:**
   - Das Build-Script installiert automatisch ins MagicDraw Plugin-Verzeichnis
   - Bei manueller Installation: JAR-Datei und plugin.xml nach `%LOCALAPPDATA%\.magic.systems.of.systems.architect\2024x\plugins\AI4MBSE\` kopieren
   - MagicDraw neustarten

## Nutzung

1. SysML-Projekt in MagicDraw öffnen
2. Tools-Menü → "Find Subsystem for Requirement (AI4MBSE)"
3. API-Schlüssel eingeben (beim ersten Start)
4. Requirements- und Subsystem-Pakete auswählen
5. KI-Empfehlungen prüfen und bestätigen

## Architektur

### Komponenten-Übersicht

```
Main.java (Plugin-Lifecycle)
├── PluginCoreManager.java (Core-Orchestrierung)
├── UserInterfaceManager.java (Benutzeroberflächen-Management)
├── AIIntegrationService.java (KI-Integration)
├── JsonModelExporter.java (Modell-Export)
├── ModelDataExtractor.java (Daten-Extraktion)
├── ModelRelationshipCreator.java (Beziehungs-Erstellung)
└── AsynchronousProcessingService.java (Async-Verarbeitung)
```

### Projektstruktur

```
src/main/java/ai4mbse/
├── Main.java                      # Plugin-Hauptklasse
├── config/
│   └── ConfigurationService.java  # Konfiguration
├── core/
│   └── PluginCoreManager.java     # Core-Manager
├── interfaces/                    # Service-Interfaces
│   ├── IAIService.java
│   ├── IAsynchronousProcessingService.java
│   ├── IConfigurationService.java
│   ├── ILoggingService.java
│   ├── IModelExtraction.java
│   └── IUserInterface.java
├── logging/
│   └── LoggingService.java        # Logging
├── model/
│   └── AllocationCandidate.java   # Datenmodell
├── services/
│   └── AsynchronousProcessingService.java
├── subsystems/                    # Modulare Subsysteme
│   ├── AIIntegrationService.java
│   ├── JsonModelExporter.java
│   ├── ModelDataExtractor.java
│   ├── ModelRelationshipCreator.java
│   ├── PackageSelectionCallback.java
│   ├── RequirementSelectionCallback.java
│   ├── SubsystemSelectionCallback.java
│   └── UserInterfaceManager.java
├── ui/
│   └── AllocationDialog.java      # UI-Dialog
├── utils/
│   ├── MagicDrawAPI.java          # MagicDraw-Utils
│   ├── PluginStub.java            # Test-Stub
│   └── SimpleJSON.java            # JSON-Hilfsfunktionen
└── resources/
    └── plugin.xml                 # Plugin-Konfiguration
```

### Kern-Interfaces

| Interface | Zweck |
|-----------|-------|
| `IUserInterface` | Benutzeroberflächen-Management und Dialoge |
| `IAIService` | Künstliche Intelligenz-Integration (Google Gemini) |
| `IModelExtraction` | Modell-Datenextraktion |
| `IAsynchronousProcessingService` | Async-Operationen |
| `IConfigurationService` | Konfigurationsmanagement |
| `ILoggingService` | Logging und Debugging |

## Entwicklung

### Build-Optionen

**Vollständiger Build** (erfordert MagicDraw-Installation):
```powershell
powershell -ExecutionPolicy Bypass -File "scripts/build-jar.ps1"
```

**Test-Build** (ohne MagicDraw, für Open-Source-Entwicklung):
```cmd
# Kompiliert nur den PluginStub für Tests
javac -d target/classes src/main/java/ai4mbse/utils/PluginStub.java
java -cp target/classes ai4mbse.utils.PluginStub
```

> **Hinweis:** Der PluginStub ermöglicht Entwicklung und Tests ohne MagicDraw-Lizenz zum Beispiel unter Windows Subsystem for Linux (WSL). Das Build-Script fällt automatisch auf den Stub zurück, wenn MagicDraw nicht installiert ist.

### Code-Stil

- **Java 17** Target-Kompatibilität
- **4 Spaces** Einrückung, max. 100 Zeichen/Zeile
- **K&R-Stil** Klammern
- **lowerCamelCase** für Methoden/Variablen
- **UpperCamelCase** für Klassen

## Troubleshooting

### Häufige Probleme

| Problem | Lösung |
|---------|--------|
| Plugin nicht im Tools-Menü sichtbar | MagicDraw-Log prüfen: `Help → System Info → Log` |
| API-Authentifizierungsfehler | API-Schlüssel im Plugin-Dialog eingeben |
| Build-Fehler | Java 17+ installiert? |
| PowerShell Execution Policy | `Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser` |

### Debug-Informationen

Plugin-Logging aktivieren durch MagicDraw-Konsole. Alle Plugin-Nachrichten beginnen mit `[AI4MBSE]`.

## Wichtige Hinweise

- **Lizenz**: MIT License (siehe [LICENSE](LICENSE))
- **Nicht offiziell**: Unabhängiges Open-Source-Projekt, nicht von Dassault Systèmes
- **Abhängigkeiten**: Erfordert gültige MagicDraw 2024x-Lizenz
- **Datenschutz**: Anforderungsdaten werden an Google's Gemini API gesendet