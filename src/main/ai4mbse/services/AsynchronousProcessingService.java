package ai4mbse.services;

import java.util.concurrent.*;

/**
 * Einfacher Asynchronous Processing Service - wie im Original Main.java.
 */
public class AsynchronousProcessingService {
    
    /** Executor Service für asynchrone Operationen - ORIGINAL aus Main.java */
    private static final ExecutorService backgroundExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "AI4MBSE-Background");
        t.setDaemon(true);
        return t;
    });
    
    /**
     * Startet den asynchronen Export der Modellstruktur als JSON-Datei - ORIGINAL.
     */
    public CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, backgroundExecutor);
    }
    
    /**
     * Plugin-Lifecycle: Wird beim Schließen aufgerufen - ORIGINAL.
     */
    public void shutdown() {
        try {
            backgroundExecutor.shutdown();
        } catch (Exception e) {
            System.err.println("Fehler beim Schließen: " + e.getMessage());
        }
    }
}