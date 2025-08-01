package ai4mbse.subsystems;

import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Comment;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import ai4mbse.model.AllocationCandidate;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * User Interface Management Subsystem.
 * Verwaltet alle Benutzerinteraktionen und Dialoge.
 */
public class UserInterfaceManager {

    /**
     * Zeigt eine Nachricht in einem Dialog an.
     * 
     * Stellt sicher, dass der Dialog im UI-Thread angezeigt wird.
     * 
     * @param msg Die anzuzeigende Nachricht
     * @param type Der Nachrichtentyp (ERROR, WARNING, INFORMATION)
     */
    public void showMessage(String msg, int type) {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(() -> showMessage(msg, type));
            return;
        }
        JOptionPane.showMessageDialog(getMainFrame(), msg, "AI4MBSE", type);
    }

    /**
     * Gibt das Hauptfenster von MagicDraw zurück.
     * 
     * @return Das Hauptfenster oder null, wenn nicht verfügbar
     */
    public Frame getMainFrame() {
        Application app = Application.getInstance();
        return (app != null) ? app.getMainFrame() : null;
    }

    /**
     * Zeigt einen Dialog zur Eingabe des Google Gemini API Keys.
     * 
     * @return Der eingegebene API Key oder null bei Abbruch
     */
    public String showApiKeyDialog() {
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
                Preferences prefs = Preferences.userNodeForPackage(UserInterfaceManager.class);
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
     * Startet den Hauptworkflow für Requirement-Allokation.
     * Diese Methode ist ein Platzhalter und sollte bei Bedarf implementiert werden.
     */
    public void startRequirementAllocationWorkflow(Project project) {
        // TODO: Implement the full workflow
        showMessage("Workflow nicht vollständig implementiert. Verwenden Sie die Main-Klasse.", JOptionPane.INFORMATION_MESSAGE);
    }
    
    public Package showPackageSelectionDialog(String title, String instruction, Project project) {
        // TODO: Implement package selection dialog
        showMessage("Package selection not implemented yet.", JOptionPane.INFORMATION_MESSAGE);
        return null;
    }
    
    public Element showRequirementSelectionDialog(List<Element> requirements) {
        // TODO: Implement requirement selection dialog  
        showMessage("Requirement selection not implemented yet.", JOptionPane.INFORMATION_MESSAGE);
        return null;
    }
    
    public List<AllocationCandidate> showAllocationCandidatesDialog(List<AllocationCandidate> candidates) {
        // TODO: Implement allocation candidates dialog
        showMessage("Allocation candidates dialog not implemented yet.", JOptionPane.INFORMATION_MESSAGE);
        return new ArrayList<>();
    }
    
    public void showProgress(String message, int percentage) {
        // TODO: Implement progress dialog
        System.out.println("Progress: " + message + " (" + percentage + "%)");
    }

    /**
     * Zeigt den Hauptdialog für die Requirement-Auswahl und Subsystem-Allokation.
     * 
     * @param project Das aktuelle MagicDraw-Projekt
     * @param operationInProgress Flag zur Kontrolle laufender Operationen
     * @param callback Callback-Funktion für die weitere Verarbeitung
     */
    public void showRequirementAndSubsystemDialog(Project project, AtomicBoolean operationInProgress, RequirementsPackageCallback callback) {
        // Schritt 1: Requirement-Ordner auswählen
        showPackageSelectionDialog(
            "Requirements-Ordner wählen",
            "Wählen Sie den Ordner mit den Requirements:",
            project,
            (selectedPackage) -> {
                if (selectedPackage != null) {
                    callback.onRequirementsPackageSelected(selectedPackage, project);
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
     * @param callback Callback für die weitere Verarbeitung
     */
    public void showNonModalRequirementDialog(List<Element> elems, DefaultListModel<String> model, Project project, RequirementSelectionCallback callback) {
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
            
            // Fortsetzung mit Callback
            callback.onRequirementSelected(selectedRequirement, project);
        });
        
        cancelButton.addActionListener(e -> {
            reqDialog.dispose();
            callback.onRequirementSelectionCancelled();
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
     * @param callback Callback für die weitere Verarbeitung
     */
    public void showNonModalSubsystemDialog(Element selectedRequirement, Project project, SubsystemSelectionCallback callback) {
        showPackageSelectionDialog(
            "Subsystem-Ordner wählen",
            "Wählen Sie den Ordner mit den Subsystemen:",
            project,
            (selectedPackage) -> {
                if (selectedPackage != null) {
                    callback.onSubsystemSelected(selectedRequirement, selectedPackage, project);
                } else {
                    callback.onSubsystemSelectionCancelled();
                }
            }
        );
    }
    
    /**
     * Zeigt einen allgemeinen Package-Auswahl-Dialog.
     * 
     * @param title Der Dialog-Titel
     * @param instruction Die Anweisung für den Benutzer
     * @param project Das aktuelle Projekt
     * @param callback Callback-Funktion mit dem ausgewählten Package
     */
    public void showPackageSelectionDialog(String title, String instruction, Project project, PackageSelectionCallback callback) {
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
     * @param project Das aktuelle MagicDraw-Projekt
     * @return Root-Node des Package-Trees mit vollständiger Hierarchie
     */
    public DefaultMutableTreeNode createPackageTree(Project project) {
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
     * @param parentPackage Das Parent-Package, dessen Kinder durchsucht werden
     * @param parentNode Der Parent-Tree-Node, zu dem die Kinder hinzugefügt werden
     */
    public void addPackageChildren(Package parentPackage, DefaultMutableTreeNode parentNode) {
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

    // Temporäre Log-Methode - genau wie im Original
    private void log(String message) {
        try {
            if (Application.getInstance() != null && Application.getInstance().getGUILog() != null)
                Application.getInstance().getGUILog().log("[AI4MBSE] " + message);
            else System.out.println("[AI4MBSE] " + message);
        } catch (Exception e) {
            System.err.println("Logging failed: " + e.getMessage());
        }
    }
}

/**
 * Callback-Interface für Requirements-Package-Auswahl.
 */
interface RequirementsPackageCallback {
    void onRequirementsPackageSelected(Package selectedPackage, Project project);
}

/**
 * Spezialisierte Tree-Node-Klasse für Package-Darstellung im Tree.
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
     * @return Benutzerfreundlicher Package-Name für die Tree-Anzeige
     */
    @Override
    public String toString() {
        return pkg.getHumanName() != null ? pkg.getHumanName() : pkg.getName();
    }
}