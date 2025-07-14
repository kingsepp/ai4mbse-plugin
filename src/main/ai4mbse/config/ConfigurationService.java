package ai4mbse.config;

import java.util.prefs.Preferences;

/**
 * Einfacher Configuration Service - nur API Key Management wie im Original.
 */
public class ConfigurationService {
    
    /**
     * Holt den API Key aus der Umgebungsvariable oder fordert ihn vom Benutzer an - ORIGINAL.
     * 
     * @return Der API Key oder null, wenn der Benutzer die Eingabe abbricht
     */
    public String getOrRequestApiKey() {
        // Zuerst Umgebungsvariable prüfen
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return apiKey.trim();
        }
        
        // Falls nicht gesetzt, in gespeicherten Preferences suchen
        Preferences prefs = Preferences.userNodeForPackage(ConfigurationService.class);
        apiKey = prefs.get("GEMINI_API_KEY", null);
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return apiKey.trim();
        }
        
        return null; // Wird später von UserInterfaceManager abgefragt
    }
}