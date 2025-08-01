package ai4mbse.interfaces;

import java.util.List;

/**
 * Interface für Logging and Diagnostics Subsystem.
 * 
 * Definiert die Schnittstelle für umfassendes Logging, Debugging und 
 * System-Health-Monitoring.
 */
public interface ILoggingService {
    
    /**
     * Log-Level Enumeration.
     */
    public enum LogLevel {
        DEBUG, INFO, WARNING, ERROR, CRITICAL
    }
    
    /**
     * Log-Entry Datenstruktur.
     */
    public static class LogEntry {
        public final long timestamp;
        public final LogLevel level;
        public final String message;
        public final String subsystem;
        public final Exception exception;
        
        public LogEntry(LogLevel level, String message, String subsystem, Exception exception) {
            this.timestamp = System.currentTimeMillis();
            this.level = level;
            this.message = message;
            this.subsystem = subsystem;
            this.exception = exception;
        }
    }
    
    /**
     * Loggt eine Nachricht mit dem angegebenen Level.
     * 
     * @param level Das Log-Level
     * @param message Die Log-Nachricht
     * @param subsystem Das Subsystem, das die Nachricht sendet
     */
    void log(LogLevel level, String message, String subsystem);
    
    /**
     * Loggt eine Exception mit Kontext.
     * 
     * @param exception Die zu loggende Exception
     * @param subsystem Das Subsystem, in dem die Exception auftrat
     */
    void logError(Exception exception, String subsystem);
    
    /**
     * Holt die Log-Historie.
     * 
     * @return Liste aller Log-Einträge als Strings
     */
    List<String> getLogHistory();
    
    /**
     * Exportiert Logs in eine Datei.
     * 
     * @param filePath Der Pfad zur Zieldatei
     * @return true wenn erfolgreich, false sonst
     */
    boolean exportLogs(String filePath);
    
    /**
     * Sammelt Diagnose-Informationen über das System.
     * 
     * @return Diagnose-Bericht als String
     */
    String getDiagnostics();
    
    /**
     * Setzt das minimale Log-Level.
     * 
     * @param level Das neue minimale Log-Level
     */
    void setLogLevel(LogLevel level);
    
    // Convenience methods für häufig verwendete Log-Level
    void debug(String message, String component);
    void info(String message, String component);  
    void warning(String message, String component);
    void error(String message, String component);
    
    // Additional convenience methods used by services
    void log(String message);
    void logInfo(String message);
    void logDebug(String message);
    void logWarning(String message);
    void logError(String message);
    void logErrorWithContext(Exception e, String context);
}