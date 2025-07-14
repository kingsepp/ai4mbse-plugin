package ai4mbse.subsystems;

import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
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