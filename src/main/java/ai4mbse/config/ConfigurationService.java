package ai4mbse.config;

import java.util.prefs.Preferences;
import java.util.Properties;
import java.nio.file.Paths;
import ai4mbse.interfaces.IConfigurationService;

/**
 * Einfacher Configuration Service - nur API Key Management wie im Original.
 */
public class ConfigurationService implements IConfigurationService {
    
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
    
    // Interface implementation methods
    @Override
    public String getApiKey() {
        return getOrRequestApiKey();
    }
    
    @Override
    public boolean setApiKey(String apiKey) {
        try {
            Preferences prefs = Preferences.userNodeForPackage(ConfigurationService.class);
            prefs.put("GEMINI_API_KEY", apiKey);
            prefs.flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean validateApiKey(String apiKey) {
        return apiKey != null && apiKey.length() > 10;
    }
    
    @Override
    public boolean saveConfiguration(Properties config) {
        return true; // Stub implementation
    }
    
    @Override
    public Properties loadConfiguration() {
        return new Properties(); // Stub implementation
    }
    
    @Override
    public String getConfigValue(String key, String defaultValue) {
        Preferences prefs = Preferences.userNodeForPackage(ConfigurationService.class);
        return prefs.get(key, defaultValue);
    }
    
    @Override
    public void setConfigValue(String key, String value) {
        try {
            Preferences prefs = Preferences.userNodeForPackage(ConfigurationService.class);
            prefs.put(key, value);
            prefs.flush();
        } catch (Exception e) {
            // Log error
        }
    }
    
    @Override
    public boolean validateConfiguration() {
        String apiKey = getApiKey();
        return apiKey != null && !apiKey.trim().isEmpty();
    }
    
    @Override
    public String getModelJsonPath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(
            userHome,
            "AppData",
            "Local",
            ".magic.systems.of.systems.architect",
            "2024x",
            "plugins",
            "AI4MBSE",
            "model_structure.json"
        ).toString();
    }
    
    @Override
    public void storeApiKey(String apiKey) {
        setApiKey(apiKey);
    }
}