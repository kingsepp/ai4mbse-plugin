package ai4mbse.interfaces;

import java.util.concurrent.*;

/**
 * Interface für Asynchronous Processing und Task Management.
 * 
 * Definiert die Schnittstelle für asynchrone Operationen, Thread-Pool-Management
 * und Task-Scheduling im AI4MBSE Plugin.
 */
public interface IAsynchronousProcessingService {
    
    /**
     * Führt eine Aufgabe asynchron aus und gibt ein CompletableFuture zurück.
     * 
     * @param <T> Der Rückgabetyp der Aufgabe
     * @param task Die auszuführende Aufgabe
     * @return CompletableFuture mit dem Ergebnis
     */
    <T> CompletableFuture<T> executeAsync(Callable<T> task);
    
    /**
     * Führt eine Runnable-Aufgabe asynchron aus.
     * 
     * @param task Die auszuführende Aufgabe
     * @return CompletableFuture für die Vollendung
     */
    CompletableFuture<Void> executeAsync(Runnable task);
    
    /**
     * Plant eine Aufgabe für die verzögerte Ausführung.
     * 
     * @param <T> Der Rückgabetyp der Aufgabe
     * @param task Die auszuführende Aufgabe
     * @param delay Die Verzögerung vor der Ausführung
     * @param unit Die Zeiteinheit für die Verzögerung
     * @return ScheduledFuture mit dem Ergebnis
     */
    <T> ScheduledFuture<T> scheduleTask(Callable<T> task, long delay, TimeUnit unit);
    
    /**
     * Plant eine wiederkehrende Aufgabe.
     * 
     * @param task Die auszuführende Aufgabe
     * @param initialDelay Initiale Verzögerung
     * @param period Wiederholungsintervall
     * @param unit Zeiteinheit
     * @return ScheduledFuture für die Kontrolle der Aufgabe
     */
    ScheduledFuture<?> scheduleRepeating(Runnable task, long initialDelay, long period, TimeUnit unit);
    
    /**
     * Prüft, ob der Service heruntergefahren wurde.
     * 
     * @return true, wenn der Service heruntergefahren ist
     */
    boolean isShutdown();
    
    /**
     * Fährt den Service ordnungsgemäß herunter.
     */
    void shutdown();
}