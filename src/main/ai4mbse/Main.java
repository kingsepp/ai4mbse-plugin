package ai4mbse;

import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.openapi.uml.ModelElementsManager;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.uml2.ext.magicdraw.metadata.UMLFactory;
import com.nomagic.uml2.ext.magicdraw.classes.mddependencies.Dependency;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Comment;
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
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.JEditorPane;
import java.awt.Dimension;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.JPasswordField;
import java.util.prefs.Preferences;

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
// Jackson für JSON-Verarbeitung
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Hauptklasse des AI4MBSE-Plugins für Cameo Systems Modeler/MagicDraw.
 * 
 * Dieses Plugin ermöglicht es, Requirements automatisch mithilfe von Künstlicher Intelligenz 
 * (Google Gemini API) zu Subsystemen zu allokieren. Es exportiert das aktuelle Modell als JSON,
 * sendet Requirement-Beschreibungen an die KI und erstellt basierend auf den Empfehlungen
 * Satisfy-Dependencies zwischen Requirements und Subsystem-Blöcken.
 * 
 * Funktionsweise:
 * 1. Export des Modells in eine JSON-Struktur für die KI-Analyse
 * 2. Auswahl eines Requirements durch den Benutzer
 * 3. Abfrage der Gemini API mit Requirement-Text und verfügbaren Subsystemen
 * 4. Anzeige der KI-Empfehlungen in einem Dialog zur Benutzerauswahl
 * 5. Automatische Erstellung von Satisfy-Dependencies im Modell
 * 
 * @author AI4MBSE Plugin Team
 * @version 2.0.0
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
                
                // Asynchroner Export mit Progress-Feedback
                exportModelAsJsonAsync(project)
                    .thenRun(() -> {
                        // Nach erfolgreichem Export: Dialog öffnen
                        EventQueue.invokeLater(() -> showRequirementAndSubsystemDialog(project));
                    })
                    .exceptionally(ex -> {
                        // Fehlerbehandlung
                        EventQueue.invokeLater(() -> {
                            log("Fehler beim Modellexport: " + ex.getMessage());
                            showMessage("Fehler beim Modellexport: " + ex.getMessage(), JOptionPane.ERROR_MESSAGE);
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
        showPackageSelectionDialog(
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
    
    /**
     * Zeigt einen nicht-modalen Dialog zur Requirement-Auswahl.
     * 
     * @param elems Liste der Requirement-Elemente
     * @param model ListModel für die UI
     * @param project Das aktuelle Projekt
     */
    private void showNonModalRequirementDialog(List<Element> elems, DefaultListModel<String> model, Project project) {
        JDialog reqDialog = new JDialog(getMainFrame(), "Select Requirement for Subsystem Analysis", false);
        reqDialog.setLayout(new BorderLayout());
        
        JList<String> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(Math.min(12, model.size()));
        JScrollPane pane = new JScrollPane(list);
        pane.setPreferredSize(new Dimension(400, 300));
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        
        okButton.addActionListener(e -> {
            int idx = list.getSelectedIndex();
            if (idx < 0) {
                showMessage("⚠️ Bitte ein Requirement auswählen.", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Element selectedRequirement = elems.get(idx);
            reqDialog.dispose();
            
            // Fortsetzung mit Subsystem-Dialog
            showNonModalSubsystemDialog(selectedRequirement, project);
        });
        
        cancelButton.addActionListener(e -> {
            reqDialog.dispose();
            operationInProgress.set(false);
        });
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        reqDialog.add(pane, BorderLayout.CENTER);
        reqDialog.add(buttonPanel, BorderLayout.SOUTH);
        reqDialog.pack();
        reqDialog.setLocationRelativeTo(getMainFrame());
        reqDialog.setVisible(true);
    }
    
    /**
     * Zeigt einen nicht-modalen Dialog zur Subsystem-Package-Auswahl.
     * 
     * @param selectedRequirement Das ausgewählte Requirement
     * @param project Das aktuelle Projekt
     */
    private void showNonModalSubsystemDialog(Element selectedRequirement, Project project) {
        showPackageSelectionDialog(
            "Subsystem-Ordner wählen",
            "Wählen Sie den Ordner mit den Subsystemen:",
            project,
            (selectedPackage) -> {
                if (selectedPackage != null) {
                    continueWithAIAnalysis(selectedRequirement, selectedPackage.getHumanName(), project);
                } else {
                    operationInProgress.set(false);
                }
            }
        );
    }
    
    /**
     * Zeigt einen allgemeinen Package-Auswahl-Dialog.
     * 
     * Dieser Dialog ermöglicht es dem Benutzer, interaktiv ein Package aus der
     * Projekt-Hierarchie auszuwählen, anstatt Ordnernamen manuell eingeben zu müssen.
     * Besonders nützlich für große Projekte mit 1000+ Requirements, da eine 
     * hierarchische Tree-Ansicht die Navigation erheblich vereinfacht.
     * 
     * Features:
     * - Hierarchische Darstellung aller Packages im Projekt
     * - Automatische Tree-Expansion für bessere Übersicht
     * - Einzelauswahl-Modus zur Vermeidung von Mehrfachselektionen
     * - Nicht-modaler Dialog für bessere UI-Integration
     * 
     * @param title Der Dialog-Titel
     * @param instruction Die Anweisung für den Benutzer
     * @param project Das aktuelle Projekt
     * @param callback Callback-Funktion mit dem ausgewählten Package
     */
    private void showPackageSelectionDialog(String title, String instruction, Project project, PackageSelectionCallback callback) {
        // Nicht-modaler Dialog für bessere UI-Integration
        JDialog packageDialog = new JDialog(getMainFrame(), title, false);
        packageDialog.setLayout(new BorderLayout());
        
        // Benutzeranweisung oben anzeigen
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel(instruction));
        
        // Package-Hierarchie als Tree-Struktur erstellen
        DefaultMutableTreeNode rootNode = createPackageTree(project);
        JTree packageTree = new JTree(rootNode);
        
        // Tree-Konfiguration für optimale Bedienbarkeit
        packageTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        packageTree.setRootVisible(true); // Root-Package sichtbar machen
        
        // Tree standardmäßig eingeklappt lassen für bessere Navigation bei großen Projekten
        // Nur das Root-Element sichtbar, Benutzer kann bei Bedarf einzelne Zweige aufklappen
        packageTree.collapseRow(0);
        
        // Scrollbare Tree-Ansicht mit angemessener Größe
        JScrollPane treeScrollPane = new JScrollPane(packageTree);
        treeScrollPane.setPreferredSize(new Dimension(400, 300));
        
        // Button-Panel für Benutzer-Aktionen
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        
        // OK-Button: Ausgewähltes Package validieren und zurückgeben
        okButton.addActionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) packageTree.getLastSelectedPathComponent();
            
            // Validierung: Ist ein gültiger PackageTreeNode ausgewählt?
            if (selectedNode == null || !(selectedNode instanceof PackageTreeNode)) {
                showMessage("⚠️ Bitte wählen Sie ein Package aus.", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Package extrahieren und Dialog schließen
            PackageTreeNode packageNode = (PackageTreeNode) selectedNode;
            Package selectedPackage = packageNode.getPackage();
            packageDialog.dispose();
            
            // Callback mit ausgewähltem Package aufrufen
            callback.onPackageSelected(selectedPackage);
        });
        
        // Cancel-Button: Dialog abbrechen ohne Auswahl
        cancelButton.addActionListener(e -> {
            packageDialog.dispose();
            callback.onPackageSelected(null); // null = Abbruch
        });
        
        // UI-Layout zusammenbauen
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        packageDialog.add(topPanel, BorderLayout.NORTH);
        packageDialog.add(treeScrollPane, BorderLayout.CENTER);
        packageDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // Dialog konfigurieren und anzeigen
        packageDialog.pack();
        packageDialog.setLocationRelativeTo(getMainFrame());
        packageDialog.setVisible(true);
    }
    
    /**
     * Erstellt einen hierarchischen Baum aller Packages im Projekt.
     * 
     * Diese Methode traversiert die gesamte Package-Hierarchie des MagicDraw-Projekts
     * und erstellt eine Tree-Struktur, die für die interaktive Package-Auswahl
     * verwendet wird. Ersetzt die alte Lösung mit hardcodierten Ordnernamen.
     * 
     * @param project Das aktuelle MagicDraw-Projekt
     * @return Root-Node des Package-Trees mit vollständiger Hierarchie
     */
    private DefaultMutableTreeNode createPackageTree(Project project) {
        Package rootPackage = project.getPrimaryModel();
        
        // Spezieller PackageTreeNode für bessere Tree-Darstellung
        PackageTreeNode rootNode = new PackageTreeNode(rootPackage);
        
        // Rekursiv alle Unter-Packages hinzufügen
        addPackageChildren(rootPackage, rootNode);
        
        return rootNode;
    }
    
    /**
     * Fügt rekursiv alle Unter-Packages zu einem Tree-Node hinzu.
     * 
     * Diese rekursive Methode durchläuft die gesamte Package-Hierarchie und
     * erstellt für jedes gefundene Package einen entsprechenden Tree-Node.
     * Ermöglicht die vollständige Navigation durch komplexe Projektstrukturen.
     * 
     * @param parentPackage Das Parent-Package, dessen Kinder durchsucht werden
     * @param parentNode Der Parent-Tree-Node, zu dem die Kinder hinzugefügt werden
     */
    private void addPackageChildren(Package parentPackage, DefaultMutableTreeNode parentNode) {
        // Alle Elemente des Parent-Packages durchgehen
        for (Element element : parentPackage.getOwnedElement()) {
            // Nur Package-Elemente berücksichtigen (keine Klassen, etc.)
            if (element instanceof Package) {
                Package childPackage = (Package) element;
                
                // Neuen Tree-Node für das Child-Package erstellen
                PackageTreeNode childNode = new PackageTreeNode(childPackage);
                parentNode.add(childNode);
                
                // Rekursiver Aufruf für die nächste Hierarchie-Ebene
                addPackageChildren(childPackage, childNode);
            }
        }
    }
    
    /**
     * Zeigt Requirements aus einem ausgewählten Package.
     * 
     * Diese Methode wird nach der Package-Auswahl aufgerufen und sammelt alle
     * Requirements aus dem ausgewählten Package und dessen Unter-Packages.
     * Ermöglicht eine gezielte Requirement-Auswahl ohne manuelles Durchsuchen
     * großer Requirement-Listen.
     * 
     * @param selectedPackage Das vom Benutzer ausgewählte Package
     * @param project Das aktuelle MagicDraw-Projekt
     */
    private void showRequirementsFromPackage(Package selectedPackage, Project project) {
        // Sammle alle Requirements aus dem ausgewählten Package und Unter-Packages
        // Dies ermöglicht eine gezielte Suche ohne das gesamte Projekt zu durchsuchen
        List<Element> requirements = collectRequirementsFromPackage(selectedPackage);
        
        // Validierung: Wurden Requirements im ausgewählten Package gefunden?
        if (requirements.isEmpty()) {
            showMessage("❗️ Keine Requirement-Elemente im ausgewählten Package gefunden.", JOptionPane.INFORMATION_MESSAGE);
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
        
        // Nicht-modaler Requirement-Auswahl-Dialog anzeigen
        showNonModalRequirementDialog(elems, model, project);
    }
    
    /**
     * Sammelt alle Requirements aus einem Package und dessen Unter-Packages.
     * 
     * Diese Methode ermöglicht eine gezielte Requirement-Sammlung aus einem
     * spezifischen Package-Bereich, anstatt das gesamte Projekt zu durchsuchen.
     * Besonders effizient für große Projekte mit strukturierter Package-Organisation.
     * 
     * @param pkg Das Package, aus dem Requirements gesammelt werden sollen
     * @return Liste aller gefundenen Requirement-Elemente mit SysML Requirement-Stereotyp
     */
    private List<Element> collectRequirementsFromPackage(Package pkg) {
        List<Element> requirements = new ArrayList<>();
        
        // Alle unterstützten Requirement-Stereotypen sammeln
        List<Stereotype> requirementStereotypes = new ArrayList<>();
        Project project = Application.getInstance().getProject();
        
        for (String stereotypeName : REQUIREMENT_STEREOTYPE_NAMES) {
            @SuppressWarnings("deprecation")
            Stereotype st = StereotypesHelper.getStereotype(project, stereotypeName);
            if (st != null) {
                requirementStereotypes.add(st);
                log("Found stereotype: " + stereotypeName);
            } else {
                log("Stereotype not available: " + stereotypeName);
            }
        }
        
        // Validierung: Wurden überhaupt Requirement-Stereotypen gefunden?
        if (requirementStereotypes.isEmpty()) {
            log("No requirement stereotypes found in project");
            return requirements; // Leere Liste zurückgeben
        }
        
        // Rekursive Sammlung aller Requirements im Package-Bereich
        collectRequirementsRecursive(pkg, requirements, requirementStereotypes);
        return requirements;
    }
    
    /**
     * Rekursive Hilfsmethode zum Sammeln von Requirements.
     * 
     * Durchsucht ein Package und alle seine Unter-Packages nach Elementen
     * mit SysML Requirement-Stereotypen. Diese Methode ermöglicht eine
     * vollständige Erfassung aller Requirements in einem Package-Bereich.
     * 
     * @param pkg Das aktuelle Package, das durchsucht wird
     * @param requirements Die Liste, zu der gefundene Requirements hinzugefügt werden
     * @param requirementStereotypes Liste aller SysML Requirement-Stereotypen zum Filtern
     */
    private void collectRequirementsRecursive(Package pkg, List<Element> requirements, List<Stereotype> requirementStereotypes) {
        log("Durchsuche Package: " + pkg.getName() + " mit " + pkg.getOwnedElement().size() + " Elementen");
        
        // Alle Elemente des aktuellen Packages durchgehen
        for (Element element : pkg.getOwnedElement()) {
            String elementName = (element instanceof NamedElement) ? ((NamedElement) element).getName() : element.toString();
            log("Prüfe Element: " + elementName + " (Typ: " + element.getClass().getSimpleName() + ")");
            
            // Prüfen, ob das Element ein Requirement ist (hat einen der entsprechenden Stereotypen)
            boolean foundRequirement = false;
            for (Stereotype requirementStereotype : requirementStereotypes) {
                if (StereotypesHelper.hasStereotypeOrDerived(element, requirementStereotype)) {
                    log("✓ Requirement gefunden: " + elementName + " mit Stereotyp: " + requirementStereotype.getName());
                    requirements.add(element);
                    foundRequirement = true;
                    break; // Element gefunden, nicht weiter prüfen
                }
            }
            
            if (!foundRequirement) {
                // Zeige angewandte Stereotypen zur Diagnose
                var appliedStereotypes = StereotypesHelper.getStereotypes(element);
                if (!appliedStereotypes.isEmpty()) {
                    log("Element " + elementName + " hat andere Stereotypen: " + 
                        appliedStereotypes.stream().map(s -> s.getName()).collect(Collectors.joining(", ")));
                }
            }
            
            // Wenn das Element selbst ein Package ist, rekursiv durchsuchen
            if (element instanceof Package) {
                collectRequirementsRecursive((Package) element, requirements, requirementStereotypes);
            }
        }
        
        log("Package " + pkg.getName() + " abgeschlossen. Gefundene Requirements: " + requirements.size());
    }
    
    /**
     * Setzt die KI-Analyse mit den ausgewählten Parametern fort.
     * 
     * @param selectedRequirement Das ausgewählte Requirement
     * @param folderName Der Subsystem-Package-Name
     * @param project Das aktuelle Projekt
     */
    private void continueWithAIAnalysis(Element selectedRequirement, String folderName, Project project) {

        // JSON-Modelldatei laden und Subsystem-Informationen extrahieren
        String userHome = System.getProperty("user.home");
        Path jsonPath = Paths.get(
            userHome,
            "AppData",
            "Local",
            ".magic.systems.of.systems.architect",
            "2024x",
            "plugins",
            "AI4MBSE",
            "model_structure.json"
        );
        if (!Files.exists(jsonPath)) {
            showMessage("❌ model_structure.json nicht gefunden unter: " + jsonPath, JOptionPane.ERROR_MESSAGE);
            operationInProgress.set(false);
            return;
        }
        JsonNode subsystemNode = null;
        List<String> subsystemNames = new ArrayList<>();
        /** Zuordnung von Subsystem-Namen zu deren MagicDraw-IDs */
        Map<String, String> subsystemNameToIdMap = new HashMap<>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new File(jsonPath.toString()));
            JsonNode packages = root.path("packages");
            // Debug-Logging für Package-Suche
            log("SUCHE Package mit Name: '" + folderName + "'");
            log("Verfügbare Packages im JSON:");
            if (packages.isArray()) {
                for (JsonNode pkg : packages) {
                    String pkgName = pkg.path("name").asText();
                    log("  - JSON Package: '" + pkgName + "'");
                    subsystemNode = findPackage(pkg, folderName);
                    if (subsystemNode != null) {
                        log("  ✅ GEFUNDEN: '" + pkgName + "' matched '" + folderName + "'");
                        break;
                    }
                }
            }
            if (subsystemNode == null) {
                int count = 0;
                if (packages.isArray()) {
                    for (JsonNode pkg : packages) count++;
                }
                String packageCount = String.valueOf(count);
                log("❌ FEHLER: Package '" + folderName + "' in keinem der " + packageCount + " JSON-Packages gefunden");
                showMessage("❌ Paket '" + folderName + "' nicht gefunden. Verfügbare Packages wurden ins Log geschrieben.", JOptionPane.ERROR_MESSAGE);
                operationInProgress.set(false);
                return;
            } else {
                log("✅ Package erfolgreich gefunden: '" + folderName + "'");
            }
            // Extraktion aller Subsystem-Namen und deren eindeutigen IDs
            JsonNode subPackagesNode = subsystemNode.path("subPackages");
            if (subPackagesNode.isArray()) {
                for (JsonNode sub : subPackagesNode) {
                    String n = sub.path("name").asText();
                    String id = sub.path("id").asText();
                    if (!n.isEmpty() && !id.isEmpty()) {
                        subsystemNames.add(n);
                        subsystemNameToIdMap.put(n, id);
                    }
                }
            } else {
                log("WARNING: 'subPackages' node is not an array or does not exist under the selected subsystem folder.");
            }
        } catch (Exception ex) {
            log("Fehler beim Lesen von model_structure.json: " + ex.getMessage());
            showMessage("Fehler beim Lesen von model_structure.json: " + ex.getMessage(), JOptionPane.ERROR_MESSAGE);
            operationInProgress.set(false);
            return;
        }

        // Schritt 4: KI-Prompt mit Requirement-Text und Subsystem-Liste erstellen
        String reqText = selectedRequirement.getOwnedComment().stream()
            .map(Comment::getBody)
            .filter(b -> b != null && !b.trim().isEmpty())
            .findFirst().orElse(selectedRequirement.getHumanName());

        StringBuilder subsystemListBuilder = new StringBuilder();       
        for (String n : subsystemNames) {
            subsystemListBuilder.append("- ").append(n).append("\n");
        }

        // Improved expert prompt using advanced prompting techniques for better accuracy
        String prompt = "Hello Gemini,\n\n" +
                        "Please act as an **expert Systems Engineer specializing in Model-Based Systems Engineering (MBSE)** with extensive experience in complex system architectures, particularly those modeled using tools like CATIA Magic / No Magic products (or similar SysML-compliant tools). Your analytical skills allow you to infer logical relationships even when they are not explicitly stated.\n\n" +
                        "I will provide you with a requirement from a systems engineering project. Your primary task is to meticulously analyze this requirement and provide well-founded, actionable suggestions for requirement allocation to appropriate subsystems.\n\n" +
                        "**CRITICAL INSTRUCTIONS – PLEASE ADHERE STRICTLY:**\n\n" +
                        "1. **Single-Pass Task:** This is a **single-pass analysis**. You will not receive follow-up clarifications or opportunities for correction. It is paramount that your first response is as accurate, complete, and helpful as possible.\n" +
                        "2. **Precision and Justification:** Every suggested allocation must be logically justified based *only* on the information present in the requirement description and subsystem names. Do not invent information or make assumptions beyond the data.\n" +
                        "3. **Semantic Fit:** Only suggest allocations where subsystems semantically fit the requirement's intent and technical domain.\n" +
                        "4. **Confidence Scoring:** Provide realistic confidence scores (0.0-1.0) based on how well each subsystem matches the requirement's purpose.\n\n" +
                        "**Your Detailed Task:**\n\n" +
                        "1. **Parse:** Thoroughly analyze the provided requirement description.\n" +
                        "2. **Evaluate:** For each available subsystem, assess its suitability for implementing or satisfying the requirement.\n" +
                        "3. **Score:** Assign realistic confidence scores where:\n" +
                        "   - 0.9-1.0: Perfect semantic match, primary responsibility\n" +
                        "   - 0.7-0.8: Strong match, likely implementation candidate\n" +
                        "   - 0.4-0.6: Moderate match, supporting role possible\n" +
                        "   - 0.1-0.3: Weak match, indirect contribution only\n" +
                        "   - 0.0: No logical connection\n" +
                        "4. **Justify:** Provide concise (1-2 sentences) justification explaining *why* each allocation is logical.\n\n" +
                        "**Requirement to Analyze:**\n" +
                        "\"" + reqText + "\"\n\n" +
                        "**Available Subsystems:**\n" +
                        subsystemListBuilder.toString() +
                        "\n" +
                        "**Output Format:** Provide your analysis as a JSON array of objects. Each object must have exactly three keys:\n" +
                        "- 'subsystemName' (string, exact name from Available Subsystems list)\n" +
                        "- 'score' (float, confidence value 0.0-1.0)\n" +
                        "- 'justification' (string, concise reasoning for the score)\n\n" +
                        "**Example JSON Structure:**\n" +
                        "[\n" +
                        "  { \"subsystemName\": \"Power Management System\", \"score\": 0.95, \"justification\": \"Direct responsibility for power-related requirements based on domain expertise.\" },\n" +
                        "  { \"subsystemName\": \"Control System\", \"score\": 0.65, \"justification\": \"May interface with power systems for monitoring and control functions.\" },\n" +
                        "  { \"subsystemName\": \"User Interface\", \"score\": 0.15, \"justification\": \"Minimal relevance, only potential status display capabilities.\" }\n" +
                        "]\n\n" +
                        "**IMPORTANT:** Return only valid JSON and nothing else. Ensure all subsystem names exactly match those provided in the Available Subsystems list.";

        // Schritt 5: API Key prüfen oder vom Benutzer anfordern
        String apiKey = getOrRequestApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            // Kein API Key verfügbar (nicht gesetzt oder Benutzer hat Eingabe abgebrochen)
            operationInProgress.set(false);
            return;
        }
        
        final String fPrompt = prompt;
        final Map<String, String> fSubsystemNameToIdMap = subsystemNameToIdMap;
        
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
                
                String suggestion = GeminiClient.callGeminiAPI(fPrompt, apiKey);
                
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
                            processAIResponse(suggestion, selectedRequirement, fSubsystemNameToIdMap, project);
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

    /**
     * Verarbeitet die Antwort der KI und zeigt den Allokationsdialog an.
     * 
     * Diese Methode:
     * 1. Bereinigt die KI-Antwort von Markdown-Formatierung
     * 2. Parst die JSON-Antwort zu AllocationCandidate-Objekten
     * 3. Validiert die Subsystem-Namen gegen das Modell
     * 4. Zeigt den Auswahldialog an
     * 5. Erstellt die bestätigten Relationships
     * 
     * @param aiResponse Die rohe Antwort der Gemini API
     * @param requirementElement Das ausgewählte Requirement-Element
     * @param subsystemNameToIdMap Zuordnung von Subsystem-Namen zu IDs
     * @param project Das aktuelle MagicDraw-Projekt
     */
    private void processAIResponse(
            String aiResponse,
            Element requirementElement,
            Map<String, String> subsystemNameToIdMap,
            Project project
    ) {
        log("[AI4MBSE] Raw AI Response: " + aiResponse);

        // Bereinigung der KI-Antwort: Entfernung von Markdown-Formatierung
        String cleanedAiResponse = aiResponse.trim();

        // Entfernung von führenden Markdown Code-Block-Markierungen
        if (cleanedAiResponse.startsWith("```json")) {
            cleanedAiResponse = cleanedAiResponse.substring("```json".length());
        } else if (cleanedAiResponse.startsWith("```")) {
            cleanedAiResponse = cleanedAiResponse.substring("```".length());
        }

        // Entfernung von abschließenden Markdown Code-Block-Markierungen
        if (cleanedAiResponse.endsWith("```")) {
            cleanedAiResponse = cleanedAiResponse.substring(0, cleanedAiResponse.length() - "```".length());
        }
        
        cleanedAiResponse = cleanedAiResponse.trim();
        log("[AI4MBSE] Cleaned AI Response: " + cleanedAiResponse);

        try {
            // Deserialisierung der JSON-Antwort zu AllocationCandidate-Objekten
            Gson gson = new Gson();
            Type listType = new TypeToken<List<AllocationCandidate>>() {}.getType();
            List<AllocationCandidate> candidates = gson.fromJson(cleanedAiResponse, listType);


            // Validierung der KI-Antwort
            if (candidates == null || candidates.isEmpty()) {
                log("Keine gültigen Kandidaten aus der KI-Antwort erhalten!");
                showMessage("Keine gültigen Kandidaten aus der KI-Antwort erhalten oder KI-Antwort ist leer.", JOptionPane.INFORMATION_MESSAGE);
                operationInProgress.set(false);
                return;
            }

            // Filterung und Validierung der Kandidaten gegen das Modell
            List<AllocationCandidate> validCandidates = new ArrayList<>();
            for (AllocationCandidate candidate : candidates) {
                String subsystemId = subsystemNameToIdMap.get(candidate.getSubsystemName());
                if (subsystemId != null) {
                    candidate.setRequirementElement(requirementElement);
                    candidate.setSubsystemId(subsystemId);
                    validCandidates.add(candidate);
                } else {
                    log("WARNING: Subsystem '" + candidate.getSubsystemName() + "' from AI response not found in model's subsystem list. Skipping.");
                }
            }

            if (validCandidates.isEmpty()) {
                showMessage("Die KI hat keine Subsysteme vorgeschlagen, die im Modell gefunden werden konnten.", JOptionPane.INFORMATION_MESSAGE);
                operationInProgress.set(false);
                return;
            }

            // Anzeige des nicht-modalen Auswahldialogs für den Benutzer
            AllocationDialog dialog = new AllocationDialog(getMainFrame(), validCandidates);
            
            // Event-Listener für Dialog-Ergebnisse (nicht-blockierend)
            dialog.setDialogListener(new AllocationDialog.AllocationDialogListener() {
                @Override
                public void onAllocationsAccepted(List<AllocationCandidate> accepted) {
                    if (accepted.isEmpty()) {
                        showMessage("Keine Allokationen ausgewählt.", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    // Logging der bestätigten Allokationen
                    log("User confirmed the following allocations:");
                    for (AllocationCandidate acceptedCandidate : accepted) {
                        log("- Requirement: " + acceptedCandidate.getRequirementElement().getHumanName() +
                            " -> Subsystem: " + acceptedCandidate.getSubsystemName() +
                            " (ID: " + acceptedCandidate.getSubsystemId() + ")");
                    }

                    // Erstellung der Satisfy-Dependencies im Modell (asynchron)
                    createRelationshipsAsync(project, accepted);
                }
                
                @Override
                public void onDialogCancelled() {
                    log("User cancelled allocation dialog.");
                    showMessage("Allokation abgebrochen.", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            
            // Dialog nicht-modal anzeigen (blockiert UI nicht!)
            dialog.setVisible(true);

        } catch (JsonSyntaxException e) {
            log("Fehler beim Parsen der KI-Antwort: " + e.getMessage() + "\nAI Response was: " + aiResponse);
            showMessage("Fehler beim Parsen der KI-Antwort. Möglicherweise kein gültiges JSON. Details im Log. " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
            operationInProgress.set(false);
        } catch (Exception e) {
            log("Unerwarteter Fehler in processAIResponse: " + e.getMessage());
            e.printStackTrace();
            showMessage("Ein unerwarteter Fehler ist aufgetreten: " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
            operationInProgress.set(false);
        }
    }

    /**
     * Startet die asynchrone Erstellung von Satisfy-Dependencies.
     * 
     * @param project Das aktuelle MagicDraw-Projekt
     * @param accepted Liste der vom Benutzer akzeptierten Allokationen
     * @return CompletableFuture für asynchrone Ausführung
     */
    private CompletableFuture<Void> createRelationshipsAsync(Project project, List<AllocationCandidate> accepted) {
        return CompletableFuture.runAsync(() -> {
            createRelationships(project, accepted);
        }, backgroundExecutor).exceptionally(ex -> {
            EventQueue.invokeLater(() -> {
                log("Fehler beim Erstellen der Relationships: " + ex.getMessage());
                showMessage("Fehler beim Erstellen der Relationships: " + ex.getMessage(), JOptionPane.ERROR_MESSAGE);
            });
            return null;
        });
    }
    
    /**
     * Erstellt Satisfy-Dependencies zwischen Requirements und Subsystem-Blöcken (synchron).
     * 
     * Diese Methode sucht für jedes akzeptierte Allokationspaar das entsprechende
     * Block-Element im Subsystem-Package und erstellt eine Dependency mit dem
     * Satisfy-Stereotyp aus dem SysML-Profil.
     * 
     * @param project Das aktuelle MagicDraw-Projekt
     * @param accepted Liste der vom Benutzer akzeptierten Allokationen
     */
    private void createRelationships(Project project, List<AllocationCandidate> accepted) {
        if (accepted == null || accepted.isEmpty()) {
            return;
        }

        // Neue Modellsession für die Änderungen starten
        SessionManager.getInstance().createSession(project, "Allocate Requirements to Subsystems");
        try {
            ModelElementsManager manager = ModelElementsManager.getInstance();
            for (AllocationCandidate candidate : accepted) {
                Element requirement = candidate.getRequirementElement();
                Element subsystemPackage = findSubsystemElementById(project, candidate.getSubsystemId());
                Element block = null;

                // Suche nach dem entsprechenden Block im Subsystem-Package
                if (subsystemPackage instanceof Package) {
                    String packageName = null;
                    if (subsystemPackage instanceof NamedElement) {
                        packageName = ((NamedElement) subsystemPackage).getName();
                    }
                    log("Suche Block mit Namen '" + packageName + "' im Package '" + subsystemPackage.getHumanName() + "'");

                    // Debug-Ausgabe aller Elemente im Package (mit Unterpackages)
                    for (Element el : ((Package) subsystemPackage).getOwnedElement()) {
                        List<Stereotype> stereotypesList = StereotypesHelper.getStereotypes(el);
                        List<String> names = new ArrayList<>();
                        for (Stereotype st : stereotypesList) {
                            names.add(st.getName());
                        }
                        String stereotypeNames = String.join(", ", names);
                        String elementType = el instanceof Package ? "📁 Package" : "📄 Element";
                        log(elementType + ": '" + el.getHumanName() + "' mit Stereotyp(en): " + stereotypeNames);
                    }

                    // Suche nach dem ersten Block im Package (unabhängig vom Namen)
                    block = findFirstBlockInPackage((Package) subsystemPackage);
                    if (block == null) {
                        log("Kein Block im Package '" + subsystemPackage.getHumanName() + "' gefunden.");
                    } else {
                        log("Block '" + block.getHumanName() + "' gefunden.");
                    }
                }

                // Erstellung der Satisfy-Dependency, falls Block gefunden
                if (block != null && requirement instanceof NamedElement) {
                    // Prüfung auf bereits existierende Relationships
                    boolean alreadyExists = false;
                    for (Element dep : block.get_directedRelationshipOfSource()) {
                        if (dep instanceof Dependency) {
                            Dependency existingDep = (Dependency) dep;
                            if (existingDep.getSupplier().contains(requirement)) {
                                alreadyExists = true;
                                log("Relationship already exists between Block '" + block.getHumanName() +
                                    "' and Requirement '" + requirement.getHumanName() + "'.");
                                break;
                            }
                        }
                    }

                    // Erstellung einer neuen Satisfy-Dependency
                    if (!alreadyExists) {
                        Element owner = block.getOwner();
                        if (owner == null) {
                            owner = project.getPrimaryModel();
                        }

                        // Dependency-Element erstellen
                        Dependency dependency = UMLFactory.eINSTANCE.createDependency();
                        dependency.getClient().add((NamedElement) block);   // Block als Client
                        dependency.getSupplier().add((NamedElement) requirement); // Requirement als Supplier

                        // Satisfy-Stereotyp aus SysML-Profil anwenden (deutsch/englisch)
                        Stereotype satisfyStereotype = getSatisfyStereotype(project);
                        if (satisfyStereotype != null) {
                            StereotypesHelper.addStereotype(dependency, satisfyStereotype);
                            log("Applied '" + satisfyStereotype.getName() + "' stereotype to dependency.");
                        } else {
                            log("WARNING: 'Satisfy' stereotype not found in SysML profile (tried: Satisfy, Erfüllen).");
                        }

                        // Dependency zum Modell hinzufügen
                        try {
                            manager.addElement(dependency, owner);
                            log("Created Satisfy dependency from Block '" + block.getHumanName()
                                + "' to Requirement '" + requirement.getHumanName() + "'.");
                        } catch (Exception e) {
                            log("ERROR: Failed to create Satisfy dependency: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                } else {
                    log("WARNING: Block not found or requirement is not a NamedElement. Cannot create relationship.");
                }
            }
        } catch (Exception ex) {
            log("ERROR creating relationships: " + ex.getMessage());
            ex.printStackTrace();
            Application.getInstance().getGUILog().showError("Error creating relationships: " + ex.getMessage());
        } finally {
            // Session immer schließen, auch bei Fehlern
            SessionManager.getInstance().closeSession(project);
        }
    }

    /**
     * Findet den Satisfy-Stereotyp in verschiedenen Sprachen.
     * 
     * @param project Das aktuelle Projekt
     * @return Der gefundene Satisfy-Stereotyp oder null
     */
    private Stereotype getSatisfyStereotype(Project project) {
        String[] satisfyNames = {"Satisfy", "Erfüllen", "Erfüllt"};
        String[] profileNames = {"SysML", "SysML_DE", "SysML_Deutsch"};
        
        for (String profileName : profileNames) {
            for (String satisfyName : satisfyNames) {
                Stereotype stereotype = StereotypesHelper.getStereotype(project, satisfyName, profileName);
                if (stereotype != null) {
                    return stereotype;
                }
            }
        }
        return null;
    }

    /**
     * Prüft, ob ein Element den Block-Stereotyp hat (deutsch/englisch).
     * 
     * @param element Das zu prüfende Element
     * @return true, wenn das Element einen Block-Stereotyp hat
     */
    private boolean hasBlockStereotype(Element element) {
        String[] blockStereotypeNames = {"Block", "Baustein"};
        for (String stereotypeName : blockStereotypeNames) {
            if (StereotypesHelper.hasStereotype(element, stereotypeName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Findet den ersten Block in einem Package (unabhängig vom Namen).
     * Sucht rekursiv in allen Unterpackages (z.B. "Logical Structure").
     * 
     * @param pkg Das zu durchsuchende Package
     * @return Das erste gefundene Block-Element oder null
     */
    private Element findFirstBlockInPackage(Package pkg) {
        log("Durchsuche Package: '" + pkg.getHumanName() + "'");
        
        // Erst direkt im Package suchen
        for (Element el : pkg.getOwnedElement()) {
            if (el instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class &&
                hasBlockStereotype(el)) {
                log("✅ Block gefunden direkt im Package: '" + el.getHumanName() + "'");
                return el;
            }
        }
        
        // Dann rekursiv in allen Unterpackages suchen (z.B. "Logical Structure")
        for (Element el : pkg.getOwnedElement()) {
            if (el instanceof Package) {
                Package subPkg = (Package) el;
                log("🔍 Durchsuche Unterpackage: '" + subPkg.getHumanName() + "'");
                Element found = findFirstBlockInPackage(subPkg);
                if (found != null) {
                    log("✅ Block gefunden in Unterpackage '" + subPkg.getHumanName() + "': '" + found.getHumanName() + "'");
                    return found;
                }
            }
        }
        
        log("❌ Kein Block gefunden in Package: '" + pkg.getHumanName() + "'");
        return null;
    }

    /**
     * Sucht rekursiv nach einem Block mit dem angegebenen Namen in einem Package.
     * 
     * Die Methode durchsucht das Package und alle Unterpackages nach einem 
     * Class-Element mit dem "Block"-Stereotyp und dem passenden Namen.
     * 
     * @param pkg Das zu durchsuchende Package
     * @param blockName Der Name des gesuchten Blocks
     * @return Das gefundene Block-Element oder null, wenn nicht gefunden
     */
    private Element findBlockRecursive(Package pkg, String blockName) {
        for (Element el : pkg.getOwnedElement()) {
            // Prüfung auf Block-Element mit passendem Namen
            if (el instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class &&
                ((com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class) el).getName().equals(blockName) &&
                hasBlockStereotype(el)) {
                return el;
            }
            // Rekursive Suche in Unterpackages
            if (el instanceof Package) {
                Element found = findBlockRecursive((Package) el, blockName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }


    /**
     * Findet ein Subsystem-Element anhand seiner eindeutigen MagicDraw-ID.
     * 
     * @param project Das aktuelle Projekt
     * @param subsystemId Die eindeutige ID des gesuchten Elements
     * @return Das gefundene Element oder null, wenn nicht gefunden
     */
    private Element findSubsystemElementById(Project project, String subsystemId) {
        if (subsystemId == null) return null;
        Element root = project.getPrimaryModel();
        return findElementByIdRecursive(root, subsystemId);
    }

    /**
     * Rekursive Hilfsmethode zur Suche eines Elements anhand seiner ID.
     * 
     * @param parent Das Parent-Element, in dem gesucht werden soll
     * @param id Die gesuchte Element-ID
     * @return Das gefundene Element oder null
     */
    private Element findElementByIdRecursive(Element parent, String id) {
        if (parent == null) return null;
        if (id.equals(parent.getID())) return parent;
        for (Element child : parent.getOwnedElement()) {
            Element found = findElementByIdRecursive(child, id);
            if (found != null) return found;
        }
        return null;
    }


    /**
     * Sammelt rekursiv alle Elemente aus dem Modellbaum.
     * 
     * @param root Das Wurzelelement
     * @param into Die Liste, in die alle gefundenen Elemente eingefügt werden
     */
    private void collectAll(Element root, List<Element> into) {
        if (root == null) return;
        into.add(root);
        if (root instanceof Package) {
            for (Element child : ((Package) root).getOwnedElement()) {
                collectAll(child, into);
            }
        } else if (root instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier) {
            for (Element member : ((com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier) root).getOwnedMember()) {
                if (member != root) {
                    collectAll(member, into);
                }
            }
        }
    }

    /**
     * Sammelt alle Requirements aus dem Projekt.
     * 
     * Durchsucht das gesamte Modell nach Elementen mit SysML Requirement-Stereotypen.
     * Unterstützt alle Arten von Requirements (Functional, Performance, etc.).
     * 
     * @param project Das aktuelle Projekt
     * @return Liste aller gefundenen Requirement-Elemente
     */
    private List<Element> collectRequirements(Project project) {
        List<Element> all = new ArrayList<>();
        if (project.getPrimaryModel() != null) {
            collectAll(project.getPrimaryModel(), all);
        }
        
        List<Element> reqs = new ArrayList<>();
        
        // Alle unterstützten Requirement-Stereotypen sammeln
        List<Stereotype> requirementStereotypes = new ArrayList<>();
        for (String stereotypeName : REQUIREMENT_STEREOTYPE_NAMES) {
            @SuppressWarnings("deprecation")
            Stereotype st = StereotypesHelper.getStereotype(project, stereotypeName);
            if (st != null) {
                requirementStereotypes.add(st);
                log("Found stereotype: " + stereotypeName);
            } else {
                log("Stereotype not available: " + stereotypeName);
            }
        }
        
        if (requirementStereotypes.isEmpty()) {
            log("No requirement stereotypes found in project");
            return reqs;
        }
        
        // Filterung nach Requirement-Stereotypen
        for (Element e : all) {
            for (Stereotype st : requirementStereotypes) {
                if (StereotypesHelper.hasStereotypeOrDerived(e, st)) {
                    reqs.add(e);
                    break; // Element gefunden, nicht weiter prüfen
                }
            }
        }
        return reqs;
    }

    /**
     * Sucht ein Package in der JSON-Struktur anhand des Namens.
     * 
     * Unterstützt verschiedene Namensvarianten (z.B. mit "Paket "-Prefix).
     * 
     * @param node Der JSON-Knoten, in dem gesucht werden soll
     * @param name Der gesuchte Package-Name
     * @return Der gefundene JSON-Knoten oder null
     */
    private JsonNode findPackage(JsonNode node, String name) {
        String pkgName = node.path("name").asText();
        
        // 1. Exakte Übereinstimmung (höchste Priorität)
        String[] exactCandidates = { name, "Paket " + name, "Package " + name };
        if (Arrays.asList(exactCandidates).contains(pkgName)) {
            return node;
        }
        
        // 2. Normalisierte Suche (Leerzeichen vs Unterstriche)
        String normalizedSearch = name.toLowerCase().replace("_", " ").trim();
        String normalizedPkg = pkgName.toLowerCase().replace("_", " ").trim();
        if (normalizedSearch.equals(normalizedPkg)) {
            return node;
        }
        
        // 3. Teilstring-Suche (enthält)
        if (pkgName.toLowerCase().contains(name.toLowerCase()) || 
            name.toLowerCase().contains(pkgName.toLowerCase())) {
            return node;
        }
        
        // 4. Suche ohne Zahlen-Präfix (03_Subsystem → Subsystem)
        String nameWithoutPrefix = name.replaceFirst("^\\d+_?", "");
        String pkgWithoutPrefix = pkgName.replaceFirst("^\\d+_?", "");
        if (!nameWithoutPrefix.isEmpty() && !pkgWithoutPrefix.isEmpty()) {
            if (nameWithoutPrefix.equalsIgnoreCase(pkgWithoutPrefix)) {
                return node;
            }
        }
        
        // Rekursive Suche in Unterpackages
        for (JsonNode child : node.path("subPackages")) {
            JsonNode found = findPackage(child, name);
            if (found != null) return found;
        }
        return null;
    }


    /**
     * Startet den asynchronen Export der Modellstruktur als JSON-Datei.
     * 
     * Diese Methode erstellt einen CompletableFuture, der den Export im Hintergrund
     * ausführt, ohne die UI zu blockieren.
     * 
     * @param project Das zu exportierende Projekt
     * @return CompletableFuture<Void> für asynchrone Ausführung
     */
    private CompletableFuture<Void> exportModelAsJsonAsync(Project project) {
        return CompletableFuture.runAsync(() -> {
            exportModelAsJson(project);
        }, backgroundExecutor);
    }
    
    /**
     * Exportiert die aktuelle Modellstruktur als JSON-Datei (synchron).
     * 
     * Diese Methode:
     * 1. Extrahiert die Package-Hierarchie des Projekts
     * 2. Konvertiert sie in eine JSON-Struktur
     * 3. Speichert die Datei im Plugin-Verzeichnis für spätere Verwendung
     * 
     * Die JSON-Datei wird von der KI-Abfrage-Funktion verwendet, um die
     * verfügbaren Subsysteme zu identifizieren.
     * 
     * @param project Das zu exportierende Projekt
     */
    private void exportModelAsJson(Project project) {
        try {
            log("DEBUG exportModelAsJson(): gestartet");
            Package root = project.getPrimaryModel();
            if (root == null) {
                log("WARN exportModelAsJson(): primaryModel ist null – Abbruch");
                return;
            }
            log("DEBUG exportModelAsJson(): primaryModel = " + root.getHumanName());

            // Konvertierung der Modellstruktur in Export-Objekte
            ExportedPackage rootExport = new ExportedPackage(root);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Map<String,Object> model = new HashMap<>();
            model.put("project", project.getName());
            model.put("packages", Collections.singletonList(rootExport));
            String json = gson.toJson(model);
            log("DEBUG exportModelAsJson(): JSON-Länge = " + json.length());

            // Bestimmung des Plugin-Verzeichnisses
            String userHome = System.getProperty("user.home");
            Path pluginBase = Paths.get(
                userHome,
                "AppData",
                "Local",
                ".magic.systems.of.systems.architect",
                "2024x",
                "plugins",
                "AI4MBSE"
            );
            Path jsonPath = pluginBase.resolve("model_structure.json");

            // Verzeichniserstellung falls nicht vorhanden
            Files.createDirectories(pluginBase);
            log("DEBUG exportModelAsJson(): schreibe nach: " + jsonPath.toAbsolutePath());

            // JSON-Datei schreiben
            Files.write(
                jsonPath,
                json.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
            log("INFO exportModelAsJson(): Modell-JSON erfolgreich geschrieben nach " 
                + jsonPath.toAbsolutePath());

        } catch (Exception e) {
            log("ERROR exportModelAsJson(): " 
                + e.getClass().getSimpleName() + " – " + e.getMessage());
            for (StackTraceElement ste : e.getStackTrace()) {
                log("    at " + ste.toString());
            }
        }
    }


    /**
     * Utility-Methode für Logging.
     * 
     * Schreibt Nachrichten in das MagicDraw-Log oder auf die Konsole.
     * 
     * @param message Die zu loggende Nachricht
     */
    private void log(String message) {
        try {
            if (Application.getInstance() != null && Application.getInstance().getGUILog() != null)
                Application.getInstance().getGUILog().log("[AI4MBSE] " + message);
            else System.out.println("[AI4MBSE] " + message);
        } catch (Exception e) {
            System.err.println("Logging failed: " + e.getMessage());
        }
    }

    /**
     * Zeigt eine Nachricht in einem Dialog an.
     * 
     * Stellt sicher, dass der Dialog im UI-Thread angezeigt wird.
     * 
     * @param msg Die anzuzeigende Nachricht
     * @param type Der Nachrichtentyp (ERROR, WARNING, INFORMATION)
     */
    private void showMessage(String msg, int type) {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(() -> showMessage(msg, type));
            return;
        }
        JOptionPane.showMessageDialog(getMainFrame(), msg, ACTION_NAME, type);
    }

    /**
     * Gibt das Hauptfenster von MagicDraw zurück.
     * 
     * @return Das Hauptfenster oder null, wenn nicht verfügbar
     */
    private Frame getMainFrame() {
        Application app = Application.getInstance();
        return (app != null) ? app.getMainFrame() : null;
    }

    /**
     * Holt den API Key aus der Umgebungsvariable oder fordert ihn vom Benutzer an.
     * 
     * Diese Methode prüft zuerst die Umgebungsvariable GEMINI_API_KEY.
     * Falls nicht gesetzt, öffnet sie einen Dialog zur Eingabe des API Keys.
     * Der eingegebene API Key wird sicher in den Benutzer-Preferences gespeichert.
     * 
     * @return Der API Key oder null, wenn der Benutzer die Eingabe abbricht
     */
    private String getOrRequestApiKey() {
        // Zuerst Umgebungsvariable prüfen
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return apiKey.trim();
        }
        
        // Falls nicht gesetzt, in gespeicherten Preferences suchen
        Preferences prefs = Preferences.userNodeForPackage(Main.class);
        apiKey = prefs.get("GEMINI_API_KEY", null);
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return apiKey.trim();
        }
        
        // API Key von Benutzer anfordern
        return showApiKeyDialog();
    }
    
    /**
     * Zeigt einen Dialog zur Eingabe des Google Gemini API Keys.
     * 
     * Dieser Dialog ermöglicht es dem Benutzer, den API Key direkt im Plugin
     * einzugeben, anstatt Umgebungsvariablen setzen zu müssen. Der Key wird
     * sicher in den Benutzer-Preferences gespeichert für zukünftige Verwendung.
     * 
     * Features:
     * - Sichere Eingabe mit Passwort-Feld
     * - Automatische Speicherung in Benutzer-Preferences
     * - Link zur Google AI Studio für API Key-Erstellung
     * - Validierung der Eingabe
     * 
     * @return Der eingegebene API Key oder null bei Abbruch
     */
    private String showApiKeyDialog() {
        JDialog apiDialog = new JDialog(getMainFrame(), "Google Gemini API Key", true);
        apiDialog.setLayout(new BorderLayout());
        
        // Informations-Panel
        JPanel infoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(10, 10, 5, 10); gbc.anchor = GridBagConstraints.WEST;
        infoPanel.add(new JLabel("🔑 Google Gemini API Key benötigt"), gbc);
        
        gbc.gridy = 1; gbc.insets = new Insets(5, 10, 10, 10);
        infoPanel.add(new JLabel("Für die KI-Funktionalität wird ein Google Gemini API Key benötigt."), gbc);
        
        gbc.gridy = 2;
        JLabel linkLabel = new JLabel("<html><a href='https://ai.google.dev/'>Kostenlosen API Key bei Google AI Studio erhalten</a></html>");
        linkLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        linkLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                try {
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://ai.google.dev/"));
                } catch (Exception ex) {
                    // Fallback: URL als Text anzeigen
                    JOptionPane.showMessageDialog(apiDialog, 
                        "Bitte öffnen Sie diesen Link manuell:\nhttps://ai.google.dev/", 
                        "Link öffnen", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        infoPanel.add(linkLabel, gbc);
        
        gbc.gridy = 3;
        JLabel urlLabel = new JLabel("<html><small><b>URL zum Kopieren:</b> https://ai.google.dev/</small></html>");
        infoPanel.add(urlLabel, gbc);
        
        // Eingabe-Panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(10, 10, 5, 10);
        inputPanel.add(new JLabel("API Key:"), gbc);
        
        JPasswordField apiKeyField = new JPasswordField(40);
        apiKeyField.setEchoChar('*');
        gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        inputPanel.add(apiKeyField, gbc);
        
        JLabel hintLabel = new JLabel("<html><small>Der API Key wird sicher gespeichert und kann jederzeit geändert werden.</small></html>");
        gbc.gridy = 2; gbc.insets = new Insets(5, 10, 10, 10);
        inputPanel.add(hintLabel, gbc);
        
        // Button-Panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton("Speichern & Fortfahren");
        JButton cancelButton = new JButton("Abbrechen");
        
        // Container für Rückgabewert
        final String[] result = {null};
        
        okButton.addActionListener(e -> {
            char[] keyChars = apiKeyField.getPassword();
            String enteredKey = new String(keyChars).trim();
            
            // Einfache Validierung
            if (enteredKey.isEmpty()) {
                showMessage("⚠️ Bitte geben Sie einen API Key ein.", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (enteredKey.length() < 10) {
                showMessage("⚠️ Der API Key scheint zu kurz zu sein. Bitte überprüfen Sie die Eingabe.", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // API Key in Preferences speichern
            try {
                Preferences prefs = Preferences.userNodeForPackage(Main.class);
                prefs.put("GEMINI_API_KEY", enteredKey);
                prefs.flush();
                
                result[0] = enteredKey;
                apiDialog.dispose();
                
                showMessage("✅ API Key erfolgreich gespeichert! Die KI-Analyse kann jetzt gestartet werden.", JOptionPane.INFORMATION_MESSAGE);
                
            } catch (Exception ex) {
                log("Fehler beim Speichern des API Keys: " + ex.getMessage());
                showMessage("❌ Fehler beim Speichern des API Keys. Bitte versuchen Sie es erneut.", JOptionPane.ERROR_MESSAGE);
            }
            
            // Passwort-Array löschen (Sicherheit)
            java.util.Arrays.fill(keyChars, ' ');
        });
        
        cancelButton.addActionListener(e -> {
            result[0] = null;
            apiDialog.dispose();
        });
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        // Dialog zusammenbauen
        apiDialog.add(infoPanel, BorderLayout.NORTH);
        apiDialog.add(inputPanel, BorderLayout.CENTER);
        apiDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // Dialog konfigurieren und anzeigen
        apiDialog.pack();
        apiDialog.setLocationRelativeTo(getMainFrame());
        apiDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        apiDialog.setVisible(true);
        
        return result[0];
    }
    
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
}

/**
 * Callback-Interface für Package-Auswahl.
 * 
 * Dieses Interface ermöglicht die asynchrone Rückgabe der Benutzer-Auswahl
 * aus dem Package-Auswahl-Dialog. Unterstützt sowohl erfolgreiche Auswahl
 * als auch Abbruch-Szenarien.
 */
interface PackageSelectionCallback {
    /**
     * Wird aufgerufen, wenn der Benutzer ein Package ausgewählt hat oder den Dialog abbricht.
     * 
     * @param selectedPackage Das ausgewählte Package oder null bei Abbruch
     */
    void onPackageSelected(Package selectedPackage);
}

/**
 * Spezialisierte Tree-Node-Klasse für Package-Darstellung im Tree.
 * 
 * Diese Klasse erweitert DefaultMutableTreeNode um Package-spezifische
 * Funktionalität und ermöglicht eine benutzerfreundliche Darstellung
 * der Package-Namen im Tree-Widget.
 * 
 * Features:
 * - Bessere toString()-Implementierung für Package-Namen
 * - Direkte Package-Referenz für einfachen Zugriff
 * - Type-safe Package-Handling
 */
class PackageTreeNode extends DefaultMutableTreeNode {
    /** Das MagicDraw-Package, das dieser Tree-Node repräsentiert */
    private Package pkg;
    
    /**
     * Konstruktor für Package-Tree-Node.
     * 
     * @param pkg Das MagicDraw-Package, das repräsentiert werden soll
     */
    public PackageTreeNode(Package pkg) {
        super(pkg);
        this.pkg = pkg;
    }
    
    /**
     * Gibt das gekapselte MagicDraw-Package zurück.
     * 
     * @return Das MagicDraw-Package dieses Tree-Nodes
     */
    public Package getPackage() {
        return pkg;
    }
    
    /**
     * Optimierte String-Repräsentation für Tree-Anzeige.
     * 
     * Verwendet bevorzugt den Package-Namen, fällt aber auf den
     * Human-Namen zurück, falls der Name nicht verfügbar ist.
     * 
     * @return Benutzerfreundlicher Package-Name für die Tree-Anzeige
     */
    @Override
    public String toString() {
        return pkg.getHumanName() != null ? pkg.getHumanName() : pkg.getName();
    }
}

/**
 * Hilfsklasse für den JSON-Export von Modellelementen.
 * 
 * Kapselt die relevanten Eigenschaften eines MagicDraw-Elements
 * für die Serialisierung in die JSON-Struktur.
 */
class ExportedElement {
    String id;
    String name;
    String type;
    List<String> stereotypes;

    /**
     * Konstruktor für Export-Element.
     * 
     * @param e Das zu exportierende MagicDraw-Element
     */
    ExportedElement(Element e) {
        this.id = e.getID();
        this.name = e.getHumanName();
        this.type = e.getClass().getSimpleName();
        this.stereotypes = StereotypesHelper.getStereotypes(e).stream()
            .map(Stereotype::getName)
            .collect(Collectors.toList());
    }
}

/**
 * Hilfsklasse für den JSON-Export von Packages.
 * 
 * Repräsentiert die hierarchische Struktur von MagicDraw-Packages
 * inklusive aller enthaltenen Elemente und Unterpackages.
 */
class ExportedPackage {
    String name;
    String id;
    List<ExportedElement> elements = new ArrayList<>();
    List<ExportedPackage> subPackages = new ArrayList<>();

    /**
     * Konstruktor für Export-Package.
     * 
     * Konvertiert ein MagicDraw-Package rekursiv in die Export-Struktur.
     * 
     * @param pkg Das zu exportierende Package
     */
    ExportedPackage(Package pkg) {
        this.name = pkg.getHumanName();
        this.id = pkg.getID();
        for (Element e : pkg.getOwnedElement()) {
            if (e instanceof Package) {
                subPackages.add(new ExportedPackage((Package) e));
            } else {
                elements.add(new ExportedElement(e));
            }
        }
    }
}

/**
 * Client für die Kommunikation mit der Google Gemini API.
 * 
 * Diese Klasse kämmt die HTTP-Kommunikation mit der Gemini API
 * und stellt Methoden für das Senden von Prompts und das Empfangen
 * von KI-generierten Antworten bereit.
 * 
 * @author AI4MBSE Plugin Team
 * @version 1.0
 */
class GeminiClient {
    /** Standard-URL für die Gemini API (kann über Umgebungsvariable überschrieben werden) */
    private static final String API_URL = System.getenv().getOrDefault("GEMINI_API_URL",
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent");
    
    /** Timeout für HTTP-Verbindungen in Millisekunden */
    private static final int TIMEOUT = 60000;
    
    /** GSON-Instanz für JSON-Serialisierung */
    private static final Gson gson = new Gson();

    /** Speichert den letzten Request-Payload für Debugging-Zwecke */
    public static String lastRequestPayload;

    /**
     * Führt einen API-Aufruf an Google Gemini durch.
     * 
     * Diese Methode:
     * 1. Baut das korrekte JSON-Payload für die Gemini API
     * 2. Sendet den HTTP-Request mit dem Prompt
     * 3. Parst die Antwort und extrahiert den generierten Text
     * 
     * @param prompt Der Text-Prompt für die KI
     * @param apiKey Der API-Schlüssel für die Authentifizierung
     * @return Die von der KI generierte Antwort als String
     * @throws IOException Bei Netzwerk- oder API-Fehlern
     * @throws JsonSyntaxException Bei fehlerhaften JSON-Antworten
     */
    public static String callGeminiAPI(String prompt, String apiKey) throws IOException, JsonSyntaxException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(API_URL + "?key=" + apiKey);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);

            // Aufbau des korrekten Gemini API JSON-Payloads
            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", prompt);
            JsonArray partsArray = new JsonArray();
            partsArray.add(textPart);
            JsonObject content = new JsonObject();
            content.add("parts", partsArray);
            JsonArray contentsArray = new JsonArray();
            contentsArray.add(content);
            JsonObject req = new JsonObject();
            req.add("contents", contentsArray);

            // Serialisierung und Speicherung für Debugging
            lastRequestPayload = gson.toJson(req);
            Application.getInstance()
                .getGUILog()
                .log("[AI4MBSE][DEBUG] Gemini-Payload: " + lastRequestPayload);

            // HTTP-Request senden
            conn.setDoOutput(true);
            try (OutputStreamWriter w = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
                w.write(lastRequestPayload);
            }

            // Antwort lesen und verarbeiten
            int code = conn.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                code < 400 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            if (code >= 400) throw new IOException("API Error: " + sb.toString());
            
            // JSON-Antwort parsen und Text extrahieren
            JsonObject resp = JsonParser.parseString(sb.toString()).getAsJsonObject();
            JsonArray cands = resp.getAsJsonArray("candidates");
            return cands.get(0).getAsJsonObject()
                .getAsJsonObject("content").getAsJsonArray("parts")
                .get(0).getAsJsonObject().get("text").getAsString();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }   
}
