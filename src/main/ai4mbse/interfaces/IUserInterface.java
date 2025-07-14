package ai4mbse.interfaces;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.magicdraw.core.Project;
import ai4mbse.model.AllocationCandidate;
import java.util.List;

/**
 * Interface für User Interface Management Subsystem.
 * 
 * Definiert die Schnittstelle für alle Benutzerinteraktionen durch ein System 
 * von modalen und nicht-modalen Dialogen.
 */
public interface IUserInterface {
    
    /**
     * Startet den Haupt-Workflow für Requirement-Allokation.
     * 
     * @param project Das aktuelle Projekt
     */
    void startRequirementAllocationWorkflow(Project project);
    
    /**
     * Zeigt einen Package-Auswahl-Dialog.
     * 
     * @param title Dialog-Titel
     * @param instruction Benutzeranweisung
     * @param project Das aktuelle Projekt
     * @return Das ausgewählte Package oder null bei Abbruch
     */
    Package showPackageSelectionDialog(String title, String instruction, Project project);
    
    /**
     * Zeigt einen Requirement-Auswahl-Dialog.
     * 
     * @param requirements Liste der verfügbaren Requirements
     * @return Das ausgewählte Requirement oder null bei Abbruch
     */
    Element showRequirementSelectionDialog(List<Element> requirements);
    
    /**
     * Zeigt API-Key-Eingabe-Dialog.
     * 
     * @return Der eingegebene API-Key oder null bei Abbruch
     */
    String showApiKeyDialog();
    
    /**
     * Zeigt Allokations-Kandidaten zur Benutzerauswahl.
     * 
     * @param candidates Liste der KI-generierten Kandidaten
     * @return Liste der vom Benutzer bestätigten Kandidaten
     */
    List<AllocationCandidate> showAllocationCandidatesDialog(List<AllocationCandidate> candidates);
    
    /**
     * Zeigt eine Nachricht oder Fehler an.
     * 
     * @param message Die anzuzeigende Nachricht
     * @param messageType Der Nachrichtentyp (INFO, WARNING, ERROR)
     */
    void showMessage(String message, int messageType);
    
    /**
     * Zeigt Progress-Feedback für längere Operationen.
     * 
     * @param message Progress-Beschreibung
     * @param percentage Fortschritt in Prozent (0-100)
     */
    void showProgress(String message, int percentage);
}