package ai4mbse.subsystems;

import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.openapi.uml.ModelElementsManager;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.uml2.ext.magicdraw.metadata.UMLFactory;
import com.nomagic.uml2.ext.magicdraw.classes.mddependencies.Dependency;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.awt.EventQueue;

import ai4mbse.model.AllocationCandidate;

/**
 * Model Relationship Creation Subsystem.
 * Erstellt Satisfy-Dependencies zwischen Requirements und Subsystem-Blöcken.
 */
public class ModelRelationshipCreator {
    
    /** Executor Service für asynchrone Operationen */
    private static final ExecutorService backgroundExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "AI4MBSE-ModelRelationshipCreator");
        t.setDaemon(true);
        return t;
    });

    /**
     * Startet die asynchrone Erstellung von Satisfy-Dependencies.
     * 
     * @param project Das aktuelle MagicDraw-Projekt
     * @param accepted Liste der vom Benutzer akzeptierten Allokationen
     * @return CompletableFuture für asynchrone Ausführung
     */
    public CompletableFuture<Void> createRelationshipsAsync(Project project, List<AllocationCandidate> accepted) {
        return CompletableFuture.runAsync(() -> {
            createRelationships(project, accepted);
        }, backgroundExecutor).exceptionally(ex -> {
            EventQueue.invokeLater(() -> {
                log("Fehler beim Erstellen der Relationships: " + ex.getMessage());
            });
            return null;
        });
    }
    
    /**
     * Erstellt Satisfy-Dependencies zwischen Requirements und Subsystem-Blöcken (synchron).
     * 
     * Diese Methode sucht für jedes akzeptierte Allokationspaar das entsprechende
     * Block-Element im Subsystem-Package und erstellt eine Dependency mit dem
     * Satisfy-Stereotyp aus dem SysML-Profil.
     * 
     * @param project Das aktuelle MagicDraw-Projekt
     * @param accepted Liste der vom Benutzer akzeptierten Allokationen
     */
    public void createRelationships(Project project, List<AllocationCandidate> accepted) {
        if (accepted == null || accepted.isEmpty()) {
            return;
        }

        // Neue Modellsession für die Änderungen starten
        SessionManager.getInstance().createSession(project, "Allocate Requirements to Subsystems");
        try {
            ModelElementsManager manager = ModelElementsManager.getInstance();
            for (AllocationCandidate candidate : accepted) {
                Element requirement = candidate.getRequirementElement();
                Element subsystemPackage = findSubsystemElementById(project, candidate.getSubsystemId());
                Element block = null;

                // Suche nach dem entsprechenden Block im Subsystem-Package
                if (subsystemPackage instanceof Package) {
                    String packageName = null;
                    if (subsystemPackage instanceof NamedElement) {
                        packageName = ((NamedElement) subsystemPackage).getName();
                    }
                    log("Suche Block mit Namen '" + packageName + "' im Package '" + subsystemPackage.getHumanName() + "'");

                    // Debug-Ausgabe aller Elemente im Package (mit Unterpackages)
                    for (Element el : ((Package) subsystemPackage).getOwnedElement()) {
                        List<Stereotype> stereotypesList = StereotypesHelper.getStereotypes(el);
                        List<String> names = new ArrayList<>();
                        for (Stereotype st : stereotypesList) {
                            names.add(st.getName());
                        }
                        String stereotypeNames = String.join(", ", names);
                        String elementType = el instanceof Package ? "📁 Package" : "📄 Element";
                        log(elementType + ": '" + el.getHumanName() + "' mit Stereotyp(en): " + stereotypeNames);
                    }

                    // Suche nach dem ersten Block im Package (unabhängig vom Namen)
                    block = findFirstBlockInPackage((Package) subsystemPackage);
                    if (block == null) {
                        log("Kein Block im Package '" + subsystemPackage.getHumanName() + "' gefunden.");
                    } else {
                        log("Block '" + block.getHumanName() + "' gefunden.");
                    }
                }

                // Erstellung der Satisfy-Dependency, falls Block gefunden
                if (block != null && requirement instanceof NamedElement) {
                    // Prüfung auf bereits existierende Relationships
                    boolean alreadyExists = false;
                    for (Element dep : block.get_directedRelationshipOfSource()) {
                        if (dep instanceof Dependency) {
                            Dependency existingDep = (Dependency) dep;
                            if (existingDep.getSupplier().contains(requirement)) {
                                alreadyExists = true;
                                log("Relationship already exists between Block '" + block.getHumanName() +
                                    "' and Requirement '" + requirement.getHumanName() + "'.");
                                break;
                            }
                        }
                    }

                    // Erstellung einer neuen Satisfy-Dependency
                    if (!alreadyExists) {
                        Element owner = block.getOwner();
                        if (owner == null) {
                            owner = project.getPrimaryModel();
                        }

                        // Dependency-Element erstellen
                        Dependency dependency = UMLFactory.eINSTANCE.createDependency();
                        dependency.getClient().add((NamedElement) block);   // Block als Client
                        dependency.getSupplier().add((NamedElement) requirement); // Requirement als Supplier

                        // Satisfy-Stereotyp aus SysML-Profil anwenden (deutsch/englisch)
                        Stereotype satisfyStereotype = getSatisfyStereotype(project);
                        if (satisfyStereotype != null) {
                            StereotypesHelper.addStereotype(dependency, satisfyStereotype);
                            log("Applied '" + satisfyStereotype.getName() + "' stereotype to dependency.");
                        } else {
                            log("WARNING: 'Satisfy' stereotype not found in SysML profile (tried: Satisfy, Erfüllen).");
                        }

                        // Dependency zum Modell hinzufügen
                        try {
                            manager.addElement(dependency, owner);
                            log("Created Satisfy dependency from Block '" + block.getHumanName()
                                + "' to Requirement '" + requirement.getHumanName() + "'.");
                        } catch (Exception e) {
                            log("ERROR: Failed to create Satisfy dependency: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                } else {
                    log("WARNING: Block not found or requirement is not a NamedElement. Cannot create relationship.");
                }
            }
        } catch (Exception ex) {
            log("ERROR creating relationships: " + ex.getMessage());
            ex.printStackTrace();
            Application.getInstance().getGUILog().showError("Error creating relationships: " + ex.getMessage());
        } finally {
            // Session immer schließen, auch bei Fehlern
            SessionManager.getInstance().closeSession(project);
        }
    }

    /**
     * Findet den Satisfy-Stereotyp in verschiedenen Sprachen.
     * 
     * @param project Das aktuelle Projekt
     * @return Der gefundene Satisfy-Stereotyp oder null
     */
    public Stereotype getSatisfyStereotype(Project project) {
        String[] satisfyNames = {"Satisfy", "Erfüllen", "Erfüllt"};
        String[] profileNames = {"SysML", "SysML_DE", "SysML_Deutsch"};
        
        for (String profileName : profileNames) {
            for (String satisfyName : satisfyNames) {
                Stereotype stereotype = StereotypesHelper.getStereotype(project, satisfyName, profileName);
                if (stereotype != null) {
                    return stereotype;
                }
            }
        }
        return null;
    }

    /**
     * Prüft, ob ein Element den Block-Stereotyp hat (deutsch/englisch).
     * 
     * @param element Das zu prüfende Element
     * @return true, wenn das Element einen Block-Stereotyp hat
     */
    public boolean hasBlockStereotype(Element element) {
        String[] blockStereotypeNames = {"Block", "Baustein"};
        for (String stereotypeName : blockStereotypeNames) {
            if (StereotypesHelper.hasStereotype(element, stereotypeName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Findet den ersten Block in einem Package (unabhängig vom Namen).
     * Sucht rekursiv in allen Unterpackages (z.B. "Logical Structure").
     * 
     * @param pkg Das zu durchsuchende Package
     * @return Das erste gefundene Block-Element oder null
     */
    public Element findFirstBlockInPackage(Package pkg) {
        log("Durchsuche Package: '" + pkg.getHumanName() + "'");
        
        // Erst direkt im Package suchen
        for (Element el : pkg.getOwnedElement()) {
            if (el instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class &&
                hasBlockStereotype(el)) {
                log("✅ Block gefunden direkt im Package: '" + el.getHumanName() + "'");
                return el;
            }
        }
        
        // Dann rekursiv in allen Unterpackages suchen (z.B. "Logical Structure")
        for (Element el : pkg.getOwnedElement()) {
            if (el instanceof Package) {
                Package subPkg = (Package) el;
                log("🔍 Durchsuche Unterpackage: '" + subPkg.getHumanName() + "'");
                Element found = findFirstBlockInPackage(subPkg);
                if (found != null) {
                    log("✅ Block gefunden in Unterpackage '" + subPkg.getHumanName() + "': '" + found.getHumanName() + "'");
                    return found;
                }
            }
        }
        
        log("❌ Kein Block gefunden in Package: '" + pkg.getHumanName() + "'");
        return null;
    }

    /**
     * Sucht rekursiv nach einem Block mit dem angegebenen Namen in einem Package.
     * 
     * Die Methode durchsucht das Package und alle Unterpackages nach einem 
     * Class-Element mit dem "Block"-Stereotyp und dem passenden Namen.
     * 
     * @param pkg Das zu durchsuchende Package
     * @param blockName Der Name des gesuchten Blocks
     * @return Das gefundene Block-Element oder null, wenn nicht gefunden
     */
    public Element findBlockRecursive(Package pkg, String blockName) {
        for (Element el : pkg.getOwnedElement()) {
            // Prüfung auf Block-Element mit passendem Namen
            if (el instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class &&
                ((com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class) el).getName().equals(blockName) &&
                hasBlockStereotype(el)) {
                return el;
            }
            // Rekursive Suche in Unterpackages
            if (el instanceof Package) {
                Element found = findBlockRecursive((Package) el, blockName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Findet ein Subsystem-Element anhand seiner eindeutigen MagicDraw-ID.
     * 
     * @param project Das aktuelle Projekt
     * @param subsystemId Die eindeutige ID des gesuchten Elements
     * @return Das gefundene Element oder null, wenn nicht gefunden
     */
    public Element findSubsystemElementById(Project project, String subsystemId) {
        if (subsystemId == null) return null;
        Element root = project.getPrimaryModel();
        return findElementByIdRecursive(root, subsystemId);
    }

    /**
     * Rekursive Hilfsmethode zur Suche eines Elements anhand seiner ID.
     * 
     * @param parent Das Parent-Element, in dem gesucht werden soll
     * @param id Die gesuchte Element-ID
     * @return Das gefundene Element oder null
     */
    public Element findElementByIdRecursive(Element parent, String id) {
        if (parent == null) return null;
        if (id.equals(parent.getID())) return parent;
        for (Element child : parent.getOwnedElement()) {
            Element found = findElementByIdRecursive(child, id);
            if (found != null) return found;
        }
        return null;
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