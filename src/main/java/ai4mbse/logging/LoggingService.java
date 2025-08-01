package ai4mbse.logging;

import com.nomagic.magicdraw.core.Application;
import ai4mbse.interfaces.ILoggingService;
import java.util.List;
import java.util.ArrayList;

/**
 * Einfaches Logging Subsystem.
 * Bietet grundlegendes Logging für das AI4MBSE Plugin.
 */
public class LoggingService implements ILoggingService {
    
    /**
     * Loggt eine einfache Nachricht.
     * 
     * @param message Die Log-Nachricht
     */
    public void log(String message) {
        try {
            if (Application.getInstance() != null && Application.getInstance().getGUILog() != null) {
                Application.getInstance().getGUILog().log("[AI4MBSE] " + message);
            } else {
                System.out.println("[AI4MBSE] " + message);
            }
        } catch (Exception e) {
            System.err.println("Logging failed: " + e.getMessage());
        }
    }
    
    /**
     * Loggt eine Fehlermeldung.
     * 
     * @param message Die Fehlermeldung
     */
    
    /**
     * Loggt eine Exception.
     * 
     * @param ex Die Exception
     * @param context Der Kontext, in dem die Exception auftrat
     */
    public void logErrorWithContext(Exception ex, String context) {
        log("ERROR in " + context + ": " + ex.getMessage());
        ex.printStackTrace();
    }
    
    public void logError(String message) {
        log("ERROR: " + message);
    }
    
    // Additional interface methods
    public void logInfo(String message) {
        log("INFO: " + message);
    }
    
    public void logDebug(String message) {
        log("DEBUG: " + message);
    }
    
    public void logWarning(String message) {
        log("WARNING: " + message);
    }
    
    // Interface implementation stubs
    @Override
    public void log(LogLevel level, String message, String subsystem) {
        log(level.name() + " [" + subsystem + "]: " + message);
    }
    
    @Override
    public void logError(Exception exception, String subsystem) {
        log("ERROR [" + subsystem + "]: " + exception.getMessage());
    }
    
    @Override
    public List<String> getLogHistory() {
        return new ArrayList<>();
    }
    
    @Override
    public boolean exportLogs(String filePath) {
        return false;
    }
    
    @Override
    public String getDiagnostics() {
        return "Basic diagnostics";
    }
    
    @Override
    public void setLogLevel(LogLevel level) {
        // Implementation not needed for basic service
    }
    
    @Override
    public void debug(String message, String component) {
        log("DEBUG [" + component + "]: " + message);
    }
    
    @Override
    public void info(String message, String component) {
        log("INFO [" + component + "]: " + message);
    }
    
    @Override
    public void warning(String message, String component) {
        log("WARNING [" + component + "]: " + message);
    }
    
    @Override
    public void error(String message, String component) {
        log("ERROR [" + component + "]: " + message);
    }
}