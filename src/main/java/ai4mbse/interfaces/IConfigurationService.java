package ai4mbse.interfaces;

import java.util.Properties;

/**
 * Interface für Configuration and Persistence Subsystem.
 * 
 * Definiert die Schnittstelle für sichere Verwaltung von Plugin-Konfigurationen, 
 * API-Keys und persistenten Einstellungen.
 */
public interface IConfigurationService {
    
    /**
     * Holt den API-Key aus verschiedenen Quellen oder fordert ihn vom Benutzer an.
     * 
     * @return Der API-Key oder null, wenn nicht verfügbar
     */
    String getApiKey();
    
    /**
     * Setzt einen neuen API-Key und speichert ihn sicher.
     * 
     * @param apiKey Der zu speichernde API-Key
     * @return true, wenn erfolgreich gespeichert
     */
    boolean setApiKey(String apiKey);
    
    /**
     * Validiert einen API-Key auf Korrektheit.
     * 
     * @param apiKey Der zu validierende API-Key
     * @return true, wenn der API-Key gültig erscheint
     */
    boolean validateApiKey(String apiKey);
    
    /**
     * Speichert Plugin-Konfiguration.
     * 
     * @param config Die zu speichernde Konfiguration
     * @return true, wenn erfolgreich gespeichert
     */
    boolean saveConfiguration(Properties config);
    
    /**
     * Lädt Plugin-Konfiguration.
     * 
     * @return Die geladene Konfiguration
     */
    Properties loadConfiguration();
    
    /**
     * Holt einen Konfigurationswert.
     * 
     * @param key Der Schlüssel des Konfigurationswerts
     * @param defaultValue Standardwert, falls Schlüssel nicht existiert
     * @return Der Konfigurationswert
     */
    String getConfigValue(String key, String defaultValue);
    
    /**
     * Setzt einen Konfigurationswert.
     * 
     * @param key Der Schlüssel des Konfigurationswerts
     * @param value Der zu setzende Wert
     */
    void setConfigValue(String key, String value);
    
    /**
     * Validiert die aktuelle Konfiguration.
     * 
     * @return true wenn die Konfiguration gültig ist
     */
    boolean validateConfiguration();
    
    /**
     * Gibt den Pfad zur Modell-JSON-Datei zurück.
     * 
     * @return Pfad zur JSON-Datei
     */
    String getModelJsonPath();
    
    /**
     * Speichert einen API-Key.
     * 
     * @param apiKey Der zu speichernde API-Key
     */
    void storeApiKey(String apiKey);
}