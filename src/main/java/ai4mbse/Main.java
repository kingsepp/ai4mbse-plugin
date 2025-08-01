package ai4mbse;

import com.nomagic.magicdraw.core.Project;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;

import java.awt.EventQueue;
import java.awt.Frame;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CancellationException;
import javax.swing.SwingWorker;
import javax.swing.ProgressMonitor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.DefaultListModel;

import com.nomagic.actions.ActionsCategory;
import com.nomagic.actions.AMConfigurator;
import com.nomagic.actions.ActionsManager;
import com.nomagic.magicdraw.actions.ActionsConfiguratorsManager;
import com.nomagic.magicdraw.actions.ActionsID;
import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.project.ProjectEventListenerAdapter;
import com.nomagic.magicdraw.plugins.Plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import ai4mbse.model.AllocationCandidate;
import ai4mbse.ui.AllocationDialog;
import ai4mbse.subsystems.ModelDataExtractor;
import ai4mbse.subsystems.JsonModelExporter;
import ai4mbse.subsystems.AIIntegrationService;
import ai4mbse.subsystems.ModelRelationshipCreator;
import ai4mbse.subsystems.UserInterfaceManager;
import ai4mbse.config.ConfigurationService;
import ai4mbse.logging.LoggingService;
import ai4mbse.subsystems.RequirementSelectionCallback;
import ai4mbse.subsystems.SubsystemSelectionCallback;
import ai4mbse.subsystems.PackageSelectionCallback;
// Jackson für JSON-Verarbeitung
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai4mbse.utils.SimpleJSON;
import javax.swing.JOptionPane;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Hauptklasse des AI4MBSE-Plugins für Cameo Systems Modeler/MagicDraw.
 * 
 * ⚠️ REFACTORING ABGESCHLOSSEN: Diese Klasse dient jetzt ausschließlich der 
 * Orchestrierung und Steuerung. Jegliche Fachlogik und Hilfslogik wurde in 
 * die jeweiligen Service-Files der 10-Subsystem-Architektur ausgelagert:
 * 
 * 📋 SERVICE-SUBSYSTEME (10 Module):
 * 1. ModelDataExtractor: Requirement- und Package-Extraktion
 * 2. JsonModelExporter: JSON-Export der Modellstruktur  
 * 3. AIIntegrationService: KI-API Integration und Response-Verarbeitung
 * 4. ModelRelationshipCreator: Satisfy-Dependency Erstellung
 * 5. UserInterfaceManager: UI-Dialoge und Benutzerinteraktion
 * 6. ConfigurationService: API-Key Management
 * 7. LoggingService: Logging-Funktionalität
 * 8. AsynchronousProcessingService: Async-Task-Management
 * 9. PluginCoreManager: Plugin-Lifecycle-Management
 * 10. Utility-Services: JSON-Utils, MagicDraw-API-Wrapper
 * 
 * 🔧 EXTRAHIERTE FUNKTIONEN:
 * - findPackage() → JsonPackageUtils.findPackage()
 * - exportModelAsJsonAsync() → JsonModelExporter.exportModelAsJsonAsync()
 * - processAIResponse() → AIIntegrationService.processAIResponse()
 * - showApiKeyDialog() → UserInterfaceManager.showApiKeyDialog()
 * 
 * 🎯 ORCHESTRIERUNG-WORKFLOW:
 * 1. Export des Modells in JSON-Struktur (JsonModelExporter)
 * 2. Auswahl eines Requirements durch Benutzer (UserInterfaceManager + ModelDataExtractor)
 * 3. Abfrage der Gemini API mit Requirement-Text (AIIntegrationService)
 * 4. Anzeige der KI-Empfehlungen zur Benutzerauswahl (UserInterfaceManager)
 * 5. Automatische Erstellung von Satisfy-Dependencies (ModelRelationshipCreator)
 * 
 * @author AI4MBSE Plugin Team
 * @version 1.8.0 (Refactored - No Duplication)
 * @see com.nomagic.magicdraw.plugins.Plugin
 */
public class Main extends Plugin {
    /** Eindeutige ID für die Plugin-Aktion */
    private static final String ACTION_ID = "AI4MBSE_FindSubsystemAction";
    
    /** Anzeigename der Plugin-Aktion im Menü */
    private static final String ACTION_NAME = "Find Subsystem for Requirement (AI4MBSE)";
    
    /** Menügruppe, in der die Aktion angezeigt wird */
    private static final String ACTION_GROUP = "Tools";
    
    /** Namen aller unterstützten Requirement-Stereotypen im SysML-Profil */
    private static final String[] REQUIREMENT_STEREOTYPE_NAMES = {
        // Standard SysML Stereotypes (uppercase)
        "Requirement",           // Standard SysML Requirement
        "AbstractRequirement",   // SysML Abstract Requirement
        "BusinessRequirement",   // SysML Business Requirement
        "ExtendedRequirement",   // SysML Extended Requirement
        "FunctionalRequirement", // SysML Functional Requirement
        "InterfaceRequirement",  // SysML Interface Requirement
        "PerformanceRequirement",// SysML Performance Requirement
        "PhysicalRequirement",   // SysML Physical Requirement
        "UsabilityRequirement",  // SysML Usability Requirement
        // Lowercase variants (often used in custom profiles)
        "requirement",           // lowercase requirement
        "abstractRequirement",   // lowercase abstractRequirement
        "businessRequirement",   // lowercase businessRequirement
        "extendedRequirement",   // lowercase extendedRequirement
        "functionalRequirement", // lowercase functionalRequirement
        "interfaceRequirement",  // lowercase interfaceRequirement
        "performanceRequirement",// lowercase performanceRequirement
        "physicalRequirement",   // lowercase physicalRequirement
        "usabilityRequirement",  // lowercase usabilityRequirement
        // German SysML Stereotypes (potential localizations)
        "Anforderung",           // German: Requirement
        "AbstrakteAnforderung",  // German: AbstractRequirement
        "Geschäftsanforderung",  // German: BusinessRequirement
        "ErweiterteAnforderung", // German: ExtendedRequirement
        "FunktionaleAnforderung",// German: FunctionalRequirement
        "SchnittstellenAnforderung", // German: InterfaceRequirement
        "LeistungsAnforderung",  // German: PerformanceRequirement
        "PhysischeAnforderung",  // German: PhysicalRequirement
        "BenutzbarkeitAnforderung" // German: UsabilityRequirement
    };
    
    /** Executor Service für asynchrone Operationen */
    private static final ExecutorService backgroundExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "AI4MBSE-Background");
        t.setDaemon(true);
        return t;
    });
    
    /** Flag zur Kontrolle laufender Operationen */
    private final AtomicBoolean operationInProgress = new AtomicBoolean(false);
    
    /** Service-Instanzen für ausgelagerte Funktionalitäten */
    private final ModelDataExtractor modelDataExtractor = new ModelDataExtractor();
    private final JsonModelExporter jsonModelExporter = new JsonModelExporter();
    private final AIIntegrationService aiIntegrationService = new AIIntegrationService();
    private final ModelRelationshipCreator modelRelationshipCreator = new ModelRelationshipCreator();
    private final UserInterfaceManager userInterfaceManager = new UserInterfaceManager();
    private final ConfigurationService configurationService = new ConfigurationService();
    private final LoggingService loggingService = new LoggingService();

    /** 
     * Konfigurator für das Hauptmenü zur Registrierung der Plugin-Aktionen.
     * Fügt die AI4MBSE-Aktion zum Tools-Menü hinzu.
     */
    private final AMConfigurator mainMenuConfigurator = new AMConfigurator() {
        /**
         * Konfiguriert das Actions-Menü und fügt die AI4MBSE-Aktion hinzu.
         * 
         * @param manager Der ActionsManager von MagicDraw
         */
        @Override
        public void configure(ActionsManager manager) {
            ActionsCategory tools = (ActionsCategory) manager.getActionFor(ActionsID.TOOLS);
            if (tools == null) {
                tools = new ActionsCategory(ActionsID.TOOLS, ActionsID.TOOLS);
                tools.setNested(false);
                manager.addCategory(0, tools);
            }
            // Plugin-Aktion zum Tools-Menü hinzufügen
            if (tools.getAction(ACTION_ID) == null) {
                tools.addAction(createFindSubsystemAction());
            }
        }
        
        @Override public int getPriority() { return AMConfigurator.MEDIUM_PRIORITY; }
    };

    /**
     * Initialisiert das Plugin beim Start von MagicDraw.
     * Registriert den Menü-Konfigurator für die Plugin-Aktionen.
     */
    @Override
    public void init() {
        ActionsConfiguratorsManager.getInstance().addMainMenuConfigurator(mainMenuConfigurator);
        log("Plugin initialized successfully.");
    }

    /**
     * Erstellt die Hauptaktion des Plugins für die Subsystem-Suche.
     * 
     * @return MDAction für das MagicDraw-Menü
     */
    private MDAction createFindSubsystemAction() {
        return new MDAction(ACTION_ID, ACTION_NAME, null, ACTION_GROUP) {
            /**
             * Führt die Hauptfunktion des Plugins asynchron aus:
             * 1. Modellexport als JSON (asynchron)
             * 2. Öffnung des Requirement-Auswahl-Dialogs (nach Export)
             * 
             * @param e Das ActionEvent
             */
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                Project project = Application.getInstance().getProject();
                if (project == null) {
                    showMessage("❌ Kein Projekt aktiv.", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // Prüfung auf bereits laufende Operation
                if (!operationInProgress.compareAndSet(false, true)) {
                    showMessage("⚠️ AI4MBSE ist bereits aktiv. Bitte warten Sie, bis die Operation abgeschlossen ist.", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // Asynchroner Export mit Progress-Feedback über JsonModelExporter Service
                jsonModelExporter.exportModelAsJsonAsync(project)
                    .thenRun(() -> {
                        // Nach erfolgreichem Export: Dialog öffnen
                        EventQueue.invokeLater(() -> showRequirementAndSubsystemDialog(project));
                    })
                    .exceptionally(ex -> {
                        // Fehlerbehandlung
                        EventQueue.invokeLater(() -> {
                            loggingService.log("Fehler beim Modellexport: " + ex.getMessage());
                            userInterfaceManager.showMessage("Fehler beim Modellexport: " + ex.getMessage(), JOptionPane.ERROR_MESSAGE);
                            operationInProgress.set(false);
                        });
                        return null;
                    });
            }
            
            /**
             * Aktualisiert den Status der Aktion basierend auf der Verfügbarkeit eines Projekts
             * und ob bereits eine Operation läuft.
             */
            @Override public void updateState() {
                boolean projectAvailable = Application.getInstance().getProject() != null;
                boolean notBusy = !operationInProgress.get();
                setEnabled(projectAvailable && notBusy);
                
                // Beschreibung mit Status-Information
                if (!projectAvailable) {
                    setDescription("Kein Projekt aktiv");
                } else if (!notBusy) {
                    setDescription("AI4MBSE-Operation läuft bereits...");
                } else {
                    setDescription("KI-gestützte Requirement-Allokation starten");
                }
            }
        };
    }

    /**
     * Zeigt den Hauptdialog für die Requirement-Auswahl und Subsystem-Allokation.
     * 
     * Ablauf:
     * 1. Auswahl des Requirement-Ordners
     * 2. Sammlung aller Requirements aus dem ausgewählten Ordner
     * 3. Benutzerauswahl eines Requirements
     * 4. Auswahl des Subsystem-Ordners
     * 5. Extraktion der Subsystem-Informationen
     * 6. Erstellung des KI-Prompts und API-Aufruf
     * 
     * @param project Das aktuelle MagicDraw-Projekt
     */
    private void showRequirementAndSubsystemDialog(Project project) {
        // Schritt 1: Requirement-Ordner auswählen
        userInterfaceManager.showPackageSelectionDialog(
            "Requirements-Ordner wählen",
            "Wählen Sie den Ordner mit den Requirements:",
            project,
            (selectedPackage) -> {
                if (selectedPackage != null) {
                    showRequirementsFromPackage(selectedPackage, project);
                } else {
                    operationInProgress.set(false);
                }
            }
        );
    }
    
    // Diese Methode wurde nach UserInterfaceManager ausgelagert - keine lokale Implementierung mehr erforderlich
    
    /**
     * Zeigt einen nicht-modalen Dialog zur Subsystem-Package-Auswahl.
     * Verwendet UserInterfaceManager für die UI-Logik.
     * 
     * @param selectedRequirement Das ausgewählte Requirement
     * @param project Das aktuelle Projekt
     */
    private void showNonModalSubsystemDialog(Element selectedRequirement, Project project) {
        userInterfaceManager.showNonModalSubsystemDialog(selectedRequirement, project,
            new SubsystemSelectionCallback() {
                @Override
                public void onSubsystemSelected(Element selectedRequirement, Package selectedSubsystemPackage, Project project) {
                    continueWithAIAnalysis(selectedRequirement, selectedSubsystemPackage, project);
                }
                
                @Override
                public void onSubsystemSelectionCancelled() {
                    operationInProgress.set(false);
                }
            });
    }
    
    /**
     * Sammelt alle Block-Elemente aus einem Package und seinen Subpackages.
     * 
     * @param pkg Das Package, aus dem Blöcke gesammelt werden sollen
     * @param blockNames Liste der Block-Namen (wird befüllt)
     * @param blockNameToIdMap Map von Block-Namen zu IDs (wird befüllt)
     */
    private void collectBlocksFromPackage(Package pkg, List<String> blockNames, Map<String, String> blockNameToIdMap) {
        // Durchsuche alle Elemente im Package
        for (Element element : pkg.getOwnedElement()) {
            // Prüfe, ob das Element ein Block-Stereotyp hat
            if (hasBlockStereotype(element)) {
                String blockName = element.getHumanName();
                String blockId = element.getID();
                if (blockName != null && !blockName.isEmpty() && blockId != null && !blockId.isEmpty()) {
                    blockNames.add(blockName);
                    blockNameToIdMap.put(blockName, blockId);
                }
            }
            
            // Rekursiv in Subpackages suchen
            if (element instanceof Package) {
                collectBlocksFromPackage((Package) element, blockNames, blockNameToIdMap);
            }
        }
    }
    
    /**
     * Prüft, ob ein Element einen Block-Stereotyp hat.
     * 
     * @param element Das zu prüfende Element
     * @return true, wenn das Element ein Block ist
     */
    private boolean hasBlockStereotype(Element element) {
        List<Stereotype> stereotypes = StereotypesHelper.getStereotypes(element);
        for (Stereotype stereotype : stereotypes) {
            String stereotypeName = stereotype.getName();
            if ("Block".equals(stereotypeName) || "block".equals(stereotypeName)) {
                return true;
            }
        }
        return false;
    }
    
    // Diese Methoden wurden in die jeweiligen Service-Klassen ausgelagert
    
    /**
     * Zeigt Requirements aus einem ausgewählten Package.
     * Verwendet ModelDataExtractor für die Requirement-Sammlung.
     * 
     * @param selectedPackage Das vom Benutzer ausgewählte Package
     * @param project Das aktuelle MagicDraw-Projekt
     */
    private void showRequirementsFromPackage(Package selectedPackage, Project project) {
        // Sammle alle Requirements über ModelDataExtractor Service
        List<Element> requirements = modelDataExtractor.extractRequirementsFromPackage(selectedPackage);
        
        // Validierung: Wurden Requirements im ausgewählten Package gefunden?
        if (requirements.isEmpty()) {
            userInterfaceManager.showMessage("❗️ Keine Requirement-Elemente im ausgewählten Package gefunden.", JOptionPane.INFORMATION_MESSAGE);
            operationInProgress.set(false);
            return;
        }
        
        // List-Model für die Requirement-Auswahl vorbereiten
        DefaultListModel<String> model = new DefaultListModel<>();
        List<Element> elems = new ArrayList<>();
        
        // Alle gefundenen Requirements zur Liste hinzufügen
        for (Element e : requirements) {
            model.addElement(e.getHumanName());
            elems.add(e);
        }
        
        // Nicht-modaler Requirement-Auswahl-Dialog über UserInterfaceManager anzeigen
        userInterfaceManager.showNonModalRequirementDialog(elems, model, project, 
            new RequirementSelectionCallback() {
                @Override
                public void onRequirementSelected(Element selectedRequirement, Project project) {
                    showNonModalSubsystemDialog(selectedRequirement, project);
                }
                
                @Override
                public void onRequirementSelectionCancelled() {
                    operationInProgress.set(false);
                }
            });
    }
    
    /**
     * Sammelt alle Requirements aus einem Package - delegiert an ModelDataExtractor.
     * 
     * @param pkg Das Package, aus dem Requirements gesammelt werden sollen
     * @return Liste aller gefundenen Requirement-Elemente mit SysML Requirement-Stereotyp
     */
    private List<Element> collectRequirementsFromPackage(Package pkg) {
        return modelDataExtractor.extractRequirementsFromPackage(pkg);
    }
    
    // Diese Methode wurde nach ModelDataExtractor ausgelagert - keine lokale Implementierung mehr erforderlich
    
    /**
     * Setzt die KI-Analyse mit den ausgewählten Parametern fort.
     * 
     * @param selectedRequirement Das ausgewählte Requirement
     * @param selectedSubsystemPackage Das ausgewählte Subsystem-Package
     * @param project Das aktuelle Projekt
     */
    private void continueWithAIAnalysis(Element selectedRequirement, Package selectedSubsystemPackage, Project project) {

        // Direkt aus dem ausgewählten Package alle Block-Elemente extrahieren
        List<String> blockNames = new ArrayList<>();
        /** Zuordnung von Block-Namen zu deren MagicDraw-IDs */
        Map<String, String> blockNameToIdMap = new HashMap<>();

        try {
            // Sammle alle Block-Elemente aus dem ausgewählten Package und seinen Subpackages
            collectBlocksFromPackage(selectedSubsystemPackage, blockNames, blockNameToIdMap);
            
            if (blockNames.isEmpty()) {
                log("❌ Keine Block-Elemente im ausgewählten Package gefunden: " + selectedSubsystemPackage.getHumanName());
                showMessage("❌ Keine Block-Elemente im ausgewählten Package gefunden.", JOptionPane.ERROR_MESSAGE);
                operationInProgress.set(false);
                return;
            }
            
            
        } catch (Exception ex) {
            log("Fehler beim Extrahieren der Block-Elemente: " + ex.getMessage());
            showMessage("Fehler beim Extrahieren der Block-Elemente: " + ex.getMessage(), JOptionPane.ERROR_MESSAGE);
            operationInProgress.set(false);
            return;
        }

        // Schritt 4: KI-Prompt mit Requirement-Text und Block-Liste über AIIntegrationService erstellen
        String reqText = aiIntegrationService.extractRequirementText(selectedRequirement);
        String prompt = aiIntegrationService.buildAdvancedPrompt(reqText, blockNames);

        // Schritt 5: API Key über ConfigurationService prüfen oder vom Benutzer anfordern
        String apiKey = configurationService.getOrRequestApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            // Falls kein API Key verfügbar, über UserInterfaceManager anfordern
            apiKey = userInterfaceManager.showApiKeyDialog();
            if (apiKey == null || apiKey.isEmpty()) {
                operationInProgress.set(false);
                return;
            }
            // API Key speichern für zukünftige Verwendung
            if (!configurationService.setApiKey(apiKey)) {
                log("WARNING: API Key konnte nicht gespeichert werden.");
            }
        }
        
        final String fPrompt = prompt;
        final String fApiKey = apiKey;
        final Map<String, String> fBlockNameToIdMap = blockNameToIdMap;
        
        // Asynchroner API-Aufruf mit SwingWorker für bessere UI-Integration
        SwingWorker<String, Void> apiWorker = new SwingWorker<String, Void>() {
            private ProgressMonitor progressMonitor;
            
            @Override
            protected String doInBackground() throws Exception {
                // Progress Monitor für Benutzer-Feedback
                EventQueue.invokeLater(() -> {
                    progressMonitor = new ProgressMonitor(
                        getMainFrame(),
                        "KI-Analyse wird durchgeführt...",
                        "Verbindung zur Gemini API",
                        0, 100
                    );
                    progressMonitor.setProgress(25);
                });
                
                if (isCancelled()) {
                    return null;
                }
                
                EventQueue.invokeLater(() -> {
                    if (progressMonitor != null) {
                        progressMonitor.setNote("Anfrage wird gesendet...");
                        progressMonitor.setProgress(50);
                    }
                });
                
                String suggestion = aiIntegrationService.callGeminiAPI(fPrompt, fApiKey);
                
                EventQueue.invokeLater(() -> {
                    if (progressMonitor != null) {
                        progressMonitor.setNote("Antwort wird verarbeitet...");
                        progressMonitor.setProgress(75);
                    }
                });
                
                return suggestion;
            }
            
            @Override
            protected void done() {
                // Progress Monitor schließen
                if (progressMonitor != null) {
                    progressMonitor.close();
                }
                operationInProgress.set(false);
                
                try {
                    if (!isCancelled()) {
                        String suggestion = get();
                        if (suggestion != null) {
                            // Verarbeitung der AI-Antwort über AIIntegrationService
                            List<AllocationCandidate> validCandidates = aiIntegrationService.processAIResponse(
                                suggestion, selectedRequirement, fBlockNameToIdMap);
                            
                            if (validCandidates.isEmpty()) {
                                userInterfaceManager.showMessage("Die KI hat keine Subsysteme vorgeschlagen, die im Modell gefunden werden konnten.", JOptionPane.INFORMATION_MESSAGE);
                                return;
                            }
                            
                            // Anzeige des Allokationsdialogs
                            AllocationDialog dialog = new AllocationDialog(userInterfaceManager.getMainFrame(), validCandidates);
                            
                            dialog.setDialogListener(new AllocationDialog.AllocationDialogListener() {
                                @Override
                                public void onAllocationsAccepted(List<AllocationCandidate> accepted) {
                                    if (accepted.isEmpty()) {
                                        userInterfaceManager.showMessage("Keine Allokationen ausgewählt.", JOptionPane.INFORMATION_MESSAGE);
                                        return;
                                    }
                                    
                                    // Logging der bestätigten Allokationen
                                    loggingService.log("User confirmed the following allocations:");
                                    for (AllocationCandidate acceptedCandidate : accepted) {
                                        loggingService.log("- Requirement: " + acceptedCandidate.getRequirementElement().getHumanName() +
                                            " -> Subsystem: " + acceptedCandidate.getSubsystemName() +
                                            " (ID: " + acceptedCandidate.getSubsystemId() + ")");
                                    }
                                    
                                    // Erstellung der Satisfy-Dependencies über ModelRelationshipCreator
                                    modelRelationshipCreator.createRelationshipsAsync(project, accepted);
                                }
                                
                                @Override
                                public void onDialogCancelled() {
                                    loggingService.log("User cancelled allocation dialog.");
                                    userInterfaceManager.showMessage("Allokation abgebrochen.", JOptionPane.INFORMATION_MESSAGE);
                                }
                            });
                            
                            dialog.setVisible(true);
                        }
                    }
                } catch (CancellationException e) {
                    log("KI-Abfrage wurde abgebrochen.");
                    showMessage("KI-Abfrage wurde abgebrochen.", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    log("Error querying Gemini: " + e.getMessage());
                    showMessage("Fehler bei der KI-Abfrage: " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        apiWorker.execute();
    }

    // Diese Methode wurde nach AIIntegrationService ausgelagert

    // Diese Methode wurde nach ModelRelationshipCreator ausgelagert
    
    // Diese Methode wurde nach ModelRelationshipCreator ausgelagert

    // Diese Methode wurde nach ModelRelationshipCreator ausgelagert

    // Diese Methode wurde nach ModelRelationshipCreator ausgelagert

    // Diese Methode wurde nach ModelRelationshipCreator ausgelagert

    // Diese Methode wurde nach ModelRelationshipCreator ausgelagert


    // Diese Methode wurde nach ModelRelationshipCreator ausgelagert

    // Diese Methode wurde nach ModelRelationshipCreator ausgelagert


    // Diese Methode wurde nach ModelDataExtractor ausgelagert

    // Diese Methode wurde nach ModelDataExtractor ausgelagert

    // Diese Methode wurde nach JsonPackageUtils ausgelagert


    // Diese Methode wurde nach JsonModelExporter ausgelagert
    
    // Diese Methode wurde nach JsonModelExporter ausgelagert


    // Diese Methode wurde nach LoggingService ausgelagert

    // Diese Methode wurde nach UserInterfaceManager ausgelagert

    // Diese Methode wurde nach UserInterfaceManager ausgelagert


    // Diese Methode wurde nach ConfigurationService ausgelagert
    
    // Diese Methode wurde nach UserInterfaceManager ausgelagert
    
    /**
     * Plugin-Lifecycle: Wird beim Schließen aufgerufen.
     * Beendet alle laufenden Hintergrund-Operationen.
     * 
     * @return true, wenn das Plugin erfolgreich geschlossen werden kann
     */
    @Override 
    public boolean close() { 
        try {
            backgroundExecutor.shutdown();
            return true;
        } catch (Exception e) {
            log("Fehler beim Schließen des Plugins: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Plugin-Lifecycle: Prüft, ob das Plugin unterstützt wird.
     * 
     * @return true, da das Plugin in allen MagicDraw-Umgebungen funktioniert
     */
    @Override public boolean isSupported() { return true; }
    
    // Utility-Methoden für vereinfachten Service-Zugriff
    private void log(String message) {
        loggingService.log(message);
    }
    
    private void showMessage(String msg, int type) {
        userInterfaceManager.showMessage(msg, type);
    }
    
    private Frame getMainFrame() {
        return userInterfaceManager.getMainFrame();
    }
}

// Alle Hilfsklassen und Interfaces wurden in die jeweiligen Service-Files ausgelagert:
// - PackageSelectionCallback, RequirementSelectionCallback, SubsystemSelectionCallback -> UserInterfaceManager
// - PackageTreeNode -> ModelDataExtractor  
// - ExportedElement, ExportedPackage -> JsonModelExporter
// - GeminiClient -> AIIntegrationService
