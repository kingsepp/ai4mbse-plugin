package ai4mbse.interfaces;

import ai4mbse.model.AllocationCandidate;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import java.util.List;

/**
 * Interface für AI Integration Subsystem.
 * 
 * Definiert die Schnittstelle für die Kommunikation mit Google Gemini API 
 * durch erweiterte Prompt-Engineering-Techniken.
 */
public interface IAIService {
    
    /**
     * Analysiert ein Requirement und gibt Allokations-Kandidaten zurück.
     * 
     * @param requirement Das zu analysierende Requirement
     * @param availableSubsystems Liste der verfügbaren Subsysteme
     * @param apiKey Der API-Schlüssel für die KI-Abfrage
     * @return Liste der KI-generierten Allokations-Kandidaten
     * @throws Exception Bei API-Fehlern oder Parsing-Problemen
     */
    List<AllocationCandidate> analyzeRequirement(Element requirement, List<String> availableSubsystems, String apiKey) throws Exception;
    
    /**
     * Validiert eine KI-Antwort auf korrekte JSON-Struktur.
     * 
     * @param response Die rohe KI-Antwort
     * @return true, wenn die Antwort gültiges JSON enthält
     */
    boolean validateResponse(String response);
    
    /**
     * Erstellt einen erweiterten Prompt für die KI-Analyse.
     * 
     * @param requirementText Der Text des Requirements
     * @param subsystemList Liste der verfügbaren Subsysteme
     * @return Der optimierte Prompt-String
     */
    String buildAdvancedPrompt(String requirementText, List<String> subsystemList);
    
    /**
     * Bereinigt KI-Antworten von Markdown-Formatierung.
     * 
     * @param rawResponse Die rohe KI-Antwort
     * @return Der bereinigte JSON-String
     */
    String cleanResponse(String rawResponse);
}