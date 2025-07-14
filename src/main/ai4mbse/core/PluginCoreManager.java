package ai4mbse.core;

import com.nomagic.actions.ActionsCategory;
import com.nomagic.actions.AMConfigurator;
import com.nomagic.actions.ActionsManager;
import com.nomagic.magicdraw.actions.ActionsConfiguratorsManager;
import com.nomagic.magicdraw.actions.ActionsID;
import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.plugins.Plugin;

import ai4mbse.subsystems.UserInterfaceManager;
import ai4mbse.subsystems.ModelDataExtractor;
import ai4mbse.subsystems.JsonModelExporter;
import ai4mbse.subsystems.AIIntegrationService;
import ai4mbse.subsystems.ModelRelationshipCreator;
import ai4mbse.config.ConfigurationService;
import ai4mbse.services.AsynchronousProcessingService;
import ai4mbse.logging.LoggingService;


import javax.swing.JOptionPane;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Plugin Core Management Subsystem - VEREINFACHT.
 * Genau wie im Original Main.java aber mit separaten Subsystem-Klassen.
 */
public class PluginCoreManager extends Plugin {
    
    /** Eindeutige ID für die Plugin-Aktion */
    private static final String ACTION_ID = "AI4MBSE_FindSubsystemAction";
    
    /** Anzeigename der Plugin-Aktion im Menü */
    private static final String ACTION_NAME = "Find Subsystem for Requirement (AI4MBSE)";
    
    /** Menügruppe, in der die Aktion angezeigt wird */
    private static final String ACTION_GROUP = "Tools";
    
    /** Flag zur Kontrolle laufender Operationen */
    private final AtomicBoolean operationInProgress = new AtomicBoolean(false);
    
    // Einfache Subsystem-Instanzen
    private LoggingService loggingService;
    private ConfigurationService configService;
    private UserInterfaceManager userInterface;
    private ModelDataExtractor modelExtractor;
    private JsonModelExporter jsonExporter;
    private AIIntegrationService aiService;
    private ModelRelationshipCreator relationshipCreator;
    private AsynchronousProcessingService asyncProcessor;
    
    /** 
     * Konfigurator für das Hauptmenü zur Registrierung der Plugin-Aktionen.
     */
    private final AMConfigurator mainMenuConfigurator = new AMConfigurator() {
        @Override
        public void configure(ActionsManager manager) {
            ActionsCategory tools = (ActionsCategory) manager.getActionFor(ActionsID.TOOLS);
            if (tools == null) {
                tools = new ActionsCategory(ActionsID.TOOLS, ActionsID.TOOLS);
                tools.setNested(false);
                manager.addCategory(0, tools);
            }
            
            if (tools.getAction(ACTION_ID) == null) {
                tools.addAction(createFindSubsystemAction());
            }
        }
        
        @Override 
        public int getPriority() { 
            return AMConfigurator.MEDIUM_PRIORITY; 
        }
    };
    
    /**
     * Initialisiert das Plugin beim Start von MagicDraw.
     */
    @Override
    public void init() {
        try {
            // Einfache Subsystem-Initialisierung
            loggingService = new LoggingService();
            configService = new ConfigurationService();
            asyncProcessor = new AsynchronousProcessingService();
            modelExtractor = new ModelDataExtractor();
            jsonExporter = new JsonModelExporter();
            aiService = new AIIntegrationService();
            relationshipCreator = new ModelRelationshipCreator();
            userInterface = new UserInterfaceManager();
            
            ActionsConfiguratorsManager.getInstance().addMainMenuConfigurator(mainMenuConfigurator);
            loggingService.log("Plugin initialized successfully.");
                             
        } catch (Exception e) {
            if (loggingService != null) {
                loggingService.log("Failed to initialize AI4MBSE Plugin: " + e.getMessage());
            }
            System.err.println("Failed to initialize AI4MBSE Plugin: " + e.getMessage());
        }
    }
    
    /**
     * Erstellt die Hauptaktion des Plugins für die Subsystem-Suche.
     */
    private MDAction createFindSubsystemAction() {
        return new MDAction(ACTION_ID, ACTION_NAME, null, ACTION_GROUP) {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                Project project = Application.getInstance().getProject();
                if (project == null) {
                    userInterface.showMessage("❌ Kein Projekt aktiv.", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                if (!operationInProgress.compareAndSet(false, true)) {
                    userInterface.showMessage(
                        "⚠️ AI4MBSE ist bereits aktiv. Bitte warten Sie, bis die Operation abgeschlossen ist.", 
                        JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
                
                try {
                    // Hier würde normalerweise der Main-Workflow aufgerufen
                    // Für jetzt einfach eine Nachricht zeigen
                    userInterface.showMessage("Plugin erfolgreich geladen! Subsystem-Klassen verfügbar.", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    loggingService.log("Fehler: " + ex.getMessage());
                    userInterface.showMessage(
                        "Fehler beim Starten des Workflows: " + ex.getMessage(), 
                        JOptionPane.ERROR_MESSAGE
                    );
                } finally {
                    operationInProgress.set(false);
                }
            }
            
            @Override 
            public void updateState() {
                boolean projectAvailable = Application.getInstance().getProject() != null;
                boolean notBusy = !operationInProgress.get();
                setEnabled(projectAvailable && notBusy);
                
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
     * Plugin-Lifecycle: Wird beim Schließen aufgerufen.
     */
    @Override 
    public boolean close() { 
        try {
            if (asyncProcessor != null) {
                asyncProcessor.shutdown();
            }
            
            if (loggingService != null) {
                loggingService.log("Plugin shutdown completed");
            }
            
            return true;
        } catch (Exception e) {
            if (loggingService != null) {
                loggingService.log("Fehler beim Schließen: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Plugin-Lifecycle: Prüft, ob das Plugin unterstützt wird.
     */
    @Override 
    public boolean isSupported() { 
        return true; 
    }
    
    // Getter methods with direct types
    public UserInterfaceManager getUserInterface() { return userInterface; }
    public ModelDataExtractor getModelExtractor() { return modelExtractor; }
    public AIIntegrationService getAiService() { return aiService; }
}