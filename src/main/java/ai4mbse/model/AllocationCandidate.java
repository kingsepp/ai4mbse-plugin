package ai4mbse.model;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.google.gson.annotations.SerializedName;

/**
 * Repräsentiert einen Allokationskandidaten für die Zuordnung von Requirements zu Subsystemen.
 * Diese Klasse wird sowohl für die JSON-Serialisierung (Gson) als auch für die Darstellung 
 * in der Benutzeroberfläche verwendet.
 * 
 * Die Klasse unterstützt die Verarbeitung von KI-generierten Allokationsvorschlägen aus der 
 * Gemini API und die anschließende Erstellung von Satisfy-Dependencies im Cameo Systems Modeler.
 * 
 * @author AI4MBSE Plugin
 * @version 1.0
 */
public class AllocationCandidate {
    /** Der Name des Subsystems, dem das Requirement zugeordnet werden soll */
    private String subsystemName;
    
    /** 
     * Konfidenzwert für die Allokation (0.0 - 1.0)
     * Wird aus JSON-Feldern "score" oder "confidence" gemappt 
     */
    @SerializedName(value = "score", alternate = { "confidence" })
    private double confidence;
    
    /** Gibt an, ob dieser Kandidat vom Benutzer für die Allokation ausgewählt wurde */
    private boolean selectedForAllocation;

    /** 
     * Referenz auf das ursprüngliche Requirement-Element aus dem Cameo Model
     * Wird nicht in JSON serialisiert, aber für die Relationship-Erstellung benötigt 
     */
    private transient Element requirementElement;
    
    /** 
     * Die eindeutige MagicDraw ID des Subsystem-Elements
     * Wird nicht in JSON serialisiert, aber für die Suche im Model benötigt 
     */
    private transient String subsystemId;

    /** Begründung der KI für diese Allokationsempfehlung */
    @SerializedName("justification")
    private String justification;

    /**
     * Standardkonstruktor für Gson-Deserialisierung.
     * Wird automatisch von Gson beim Parsen der JSON-Antwort der KI aufgerufen.
     */
    public AllocationCandidate() {
        this.selectedForAllocation = false;
    }

    /**
     * Konstruktor für manuell erstellte Allokationskandidaten.
     * 
     * @param subsystemName Name des Subsystems
     * @param confidence Konfidenzwert (0.0 bis 1.0)
     * @param justification Begründung der KI
     */
    public AllocationCandidate(String subsystemName, double confidence, String justification) {
        this.subsystemName = subsystemName;
        this.confidence = confidence;
        this.justification = justification;
        this.selectedForAllocation = false;
    }

    // Getter-Methoden

    /**
     * @return Der Name des empfohlenen Subsystems
     */
    public String getSubsystemName() {
        return subsystemName;
    }

    /**
     * @return Der Konfidenzwert als Dezimalzahl zwischen 0.0 und 1.0
     */
    public double getConfidence() {
        return confidence;
    }

    /**
     * @return Der Konfidenzwert als Prozentzahl (0-100)
     */
    public int getConfidencePercent() {
        return (int) Math.round(confidence * 100);
    }

    /**
     * @return Die Begründung der KI für diese Empfehlung
     */
    public String getJustification() {
        return justification;
    }

    /**
     * @return true, wenn dieser Kandidat vom Benutzer ausgewählt wurde
     */
    public boolean isSelectedForAllocation() {
        return selectedForAllocation;
    }

    /**
     * @return Das referenzierte Requirement-Element aus dem Cameo Model
     */
    public Element getRequirementElement() {
        return requirementElement;
    }

    /**
     * @return Die eindeutige MagicDraw ID des Subsystem-Elements
     */
    public String getSubsystemId() {
        return subsystemId;
    }

    // Setter-Methoden

    /**
     * Setzt den Namen des empfohlenen Subsystems.
     * 
     * @param subsystemName Der Subsystemname
     */
    public void setSubsystemName(String subsystemName) {
        this.subsystemName = subsystemName;
    }

    /**
     * Setzt den Konfidenzwert für diese Allokationsempfehlung.
     * 
     * @param confidence Konfidenzwert zwischen 0.0 und 1.0
     */
    public void setConfidence(double confidence) {
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    /**
     * Setzt die Begründung der KI für diese Empfehlung.
     * 
     * @param justification Die Begründung als Text
     */
    public void setJustification(String justification) {
        this.justification = justification;
    }

    /**
     * Markiert diesen Kandidaten als vom Benutzer ausgewählt oder nicht ausgewählt.
     * 
     * @param selected true für Auswahl, false für Abwahl
     */
    public void setSelectedForAllocation(boolean selected) {
        this.selectedForAllocation = selected;
    }

    /**
     * Setzt die Referenz auf das ursprüngliche Requirement-Element.
     * 
     * @param requirementElement Das Requirement-Element aus dem Cameo Model
     */
    public void setRequirementElement(Element requirementElement) {
        this.requirementElement = requirementElement;
    }

    /**
     * Setzt die eindeutige MagicDraw ID des referenzierten Subsystem-Elements.
     * 
     * @param subsystemId Die MagicDraw Element-ID
     */
    public void setSubsystemId(String subsystemId) {
        this.subsystemId = subsystemId;
    }

    /**
     * Erstellt eine String-Repräsentation dieses Allokationskandidaten.
     * Nützlich für Debugging und Logging.
     * 
     * @return String-Darstellung mit allen wichtigen Feldern
     */
    @Override
    public String toString() {
        return String.format(
            "AllocationCandidate{subsystem='%s', confidence=%.2f, selected=%b, justification='%s'}",
            subsystemName, confidence, selectedForAllocation, 
            justification != null ? justification.substring(0, Math.min(50, justification.length())) + "..." : "null"
        );
    }

    /**
     * Vergleicht zwei AllocationCandidate-Objekte auf Gleichheit.
     * Zwei Kandidaten gelten als gleich, wenn sie denselben Subsystem-Namen haben.
     * 
     * @param obj Das zu vergleichende Objekt
     * @return true, wenn die Objekte gleich sind
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AllocationCandidate that = (AllocationCandidate) obj;
        return subsystemName != null ? subsystemName.equals(that.subsystemName) : that.subsystemName == null;
    }

    /**
     * Berechnet den Hash-Code für dieses Objekt.
     * 
     * @return Hash-Code basierend auf dem Subsystem-Namen
     */
    @Override
    public int hashCode() {
        return subsystemName != null ? subsystemName.hashCode() : 0;
    }
}