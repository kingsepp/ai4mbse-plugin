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
- Asynchrone Verarbeitung mit nicht-blockierender UI
- Automatische Erstellung von SysML Satisfy-Abhängigkeiten
- Konfidenz-Bewertung der KI-Empfehlungen

## Voraussetzungen

- MagicDraw/Cameo Systems Modeler 2024x
- Java 17+
- Google Gemini API Key

## Installation

1. **Build:**
   ```powershell
   powershell -ExecutionPolicy Bypass -File "scripts/build-jar.ps1"
   ```

2. **Installation:**
   - JAR nach `C:\Users\UserName\AppData\Local\.magic.systems.of.systems.architect\2024x\plugins\AI4MBSE\AI4MBSE.jar` kopieren
   - `plugin.xml` nach `C:\Users\UserName\AppData\Local\.magic.systems.of.systems.architect\2024x\plugins\AI4MBSE\plugin.xml` kopieren
   - MagicDraw neustarten

## Nutzung

1. SysML-Projekt in MagicDraw öffnen
2. Tools-Menü → "Find Subsystem for Requirement (AI4MBSE)"
3. API Key eingeben (beim ersten Start)
4. Requirements- und Subsystem-Pakete auswählen
5. KI-Empfehlungen prüfen und bestätigen

## Architektur

### Komponenten-Übersicht

```
Main.java (Plugin-Lifecycle)
├── PluginCoreManager.java (Core-Orchestrierung)
├── UserInterfaceManager.java (UI-Management)
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
├── PluginStub.java                # Test-Stub
├── config/
│   └── ConfigurationService.java  # Konfiguration
├── core/
│   └── PluginCoreManager.java     # Core-Manager
├── interfaces/                    # Service-Interfaces
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
│   └── UserInterfaceManager.java
├── ui/
│   └── AllocationDialog.java      # UI-Dialog
└── utils/
    ├── MagicDrawAPI.java          # MagicDraw-Utils
    └── SimpleJSON.java            # JSON-Utils
```

### Kern-Interfaces

| Interface | Zweck |
|-----------|-------|
| `IUserInterface` | UI-Management und Dialoge |
| `IAIService` | KI-Integration (Google Gemini) |
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

**Test-Build** (ohne MagicDraw, nur für Entwicklung):
```cmd
# Kompiliert nur den PluginStub für Tests
javac -d build src/main/java/ai4mbse/PluginStub.java
```

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
| API-Authentifizierungsfehler | API-Key im Plugin-Dialog eingeben |
| Build-Fehler | Java 17+ installiert? |
| Berechtigungsfehler (Linux/macOS) | `chmod +x scripts/*.sh` |

### Debug-Informationen

Plugin-Logging aktivieren durch MagicDraw-Konsole. Alle Plugin-Nachrichten beginnen mit `[AI4MBSE]`.

## Wichtige Hinweise

- **Lizenz**: MIT License (siehe [LICENSE](LICENSE))
- **Nicht offiziell**: Unabhängiges Open-Source-Projekt, nicht von Dassault Systèmes
- **Abhängigkeiten**: Erfordert gültige MagicDraw 2024x-Lizenz
- **Datenschutz**: Requirement-Daten werden an Google's Gemini API gesendet