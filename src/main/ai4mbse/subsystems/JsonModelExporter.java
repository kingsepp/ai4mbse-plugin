package ai4mbse.subsystems;

import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * JSON Model Export Subsystem.
 * Exportiert MagicDraw-Modellstrukturen als JSON für KI-Verarbeitung.
 */
public class JsonModelExporter {
    
    /** Executor Service für asynchrone Operationen */
    private static final ExecutorService backgroundExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "AI4MBSE-Background");
        t.setDaemon(true);
        return t;
    });

    /**
     * Startet den asynchronen Export der Modellstruktur als JSON-Datei.
     * 
     * @param project Das zu exportierende Projekt
     * @return CompletableFuture<Void> für asynchrone Ausführung
     */
    public CompletableFuture<Void> exportModelAsJsonAsync(Project project) {
        return CompletableFuture.runAsync(() -> {
            exportModelAsJson(project);
        }, backgroundExecutor);
    }
    
    /**
     * Exportiert die aktuelle Modellstruktur als JSON-Datei (synchron).
     * 
     * @param project Das zu exportierende Projekt
     */
    public void exportModelAsJson(Project project) {
        try {
            log("DEBUG exportModelAsJson(): gestartet");
            Package root = project.getPrimaryModel();
            if (root == null) {
                log("WARN exportModelAsJson(): primaryModel ist null – Abbruch");
                return;
            }
            log("DEBUG exportModelAsJson(): primaryModel = " + root.getHumanName());

            // Konvertierung der Modellstruktur in Export-Objekte
            ExportedPackage rootExport = new ExportedPackage(root);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Map<String,Object> model = new HashMap<>();
            model.put("project", project.getName());
            model.put("packages", Collections.singletonList(rootExport));
            String json = gson.toJson(model);
            log("DEBUG exportModelAsJson(): JSON-Länge = " + json.length());

            // Bestimmung des Plugin-Verzeichnisses
            String userHome = System.getProperty("user.home");
            Path pluginBase = Paths.get(
                userHome,
                "AppData",
                "Local",
                ".magic.systems.of.systems.architect",
                "2024x",
                "plugins",
                "AI4MBSE"
            );
            Path jsonPath = pluginBase.resolve("model_structure.json");

            // Verzeichniserstellung falls nicht vorhanden
            Files.createDirectories(pluginBase);
            log("DEBUG exportModelAsJson(): schreibe nach: " + jsonPath.toAbsolutePath());

            // JSON-Datei schreiben
            Files.write(
                jsonPath,
                json.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
            log("INFO exportModelAsJson(): Modell-JSON erfolgreich geschrieben nach " 
                + jsonPath.toAbsolutePath());

        } catch (Exception e) {
            log("ERROR exportModelAsJson(): " 
                + e.getClass().getSimpleName() + " – " + e.getMessage());
            for (StackTraceElement ste : e.getStackTrace()) {
                log("    at " + ste.toString());
            }
        }
    }

    // Temporäre Log-Methode - wird später durch LoggingService ersetzt
    private void log(String message) {
        try {
            if (Application.getInstance() != null && Application.getInstance().getGUILog() != null)
                Application.getInstance().getGUILog().log("[AI4MBSE] " + message);
            else System.out.println("[AI4MBSE] " + message);
        } catch (Exception e) {
            System.err.println("Logging failed: " + e.getMessage());
        }
    }
}

/**
 * Hilfsklasse für den JSON-Export von Modellelementen.
 */
class ExportedElement {
    String id;
    String name;
    String type;
    List<String> stereotypes;

    /**
     * Konstruktor für Export-Element.
     * 
     * @param e Das zu exportierende MagicDraw-Element
     */
    ExportedElement(Element e) {
        this.id = e.getID();
        this.name = e.getHumanName();
        this.type = e.getClass().getSimpleName();
        this.stereotypes = StereotypesHelper.getStereotypes(e).stream()
            .map(Stereotype::getName)
            .collect(Collectors.toList());
    }
}

/**
 * Hilfsklasse für den JSON-Export von Packages.
 */
class ExportedPackage {
    String name;
    String id;
    List<ExportedElement> elements = new ArrayList<>();
    List<ExportedPackage> subPackages = new ArrayList<>();

    /**
     * Konstruktor für Export-Package.
     * 
     * @param pkg Das zu exportierende Package
     */
    ExportedPackage(Package pkg) {
        this.name = pkg.getHumanName();
        this.id = pkg.getID();
        for (Element e : pkg.getOwnedElement()) {
            if (e instanceof Package) {
                subPackages.add(new ExportedPackage((Package) e));
            } else {
                elements.add(new ExportedElement(e));
            }
        }
    }
}