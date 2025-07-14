package ai4mbse.logging;

import com.nomagic.magicdraw.core.Application;

/**
 * Einfaches Logging Subsystem.
 * Bietet grundlegendes Logging für das AI4MBSE Plugin.
 */
public class LoggingService {
    
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
    public void logError(String message) {
        log("ERROR: " + message);
    }
    
    /**
     * Loggt eine Exception.
     * 
     * @param ex Die Exception
     * @param context Der Kontext, in dem die Exception auftrat
     */
    public void logError(Exception ex, String context) {
        log("ERROR in " + context + ": " + ex.getMessage());
        ex.printStackTrace();
    }
}