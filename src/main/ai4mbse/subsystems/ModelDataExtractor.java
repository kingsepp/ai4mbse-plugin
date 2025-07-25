package ai4mbse.subsystems;

import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Model Data Extraction Subsystem.
 * Extrahiert Requirements und Package-Strukturen aus MagicDraw-Modellen.
 */
public class ModelDataExtractor {
    
    /** Namen aller unterstützten Requirement-Stereotypen im SysML-Profil */
    private static final String[] REQUIREMENT_STEREOTYPE_NAMES = {
        // Standard SysML Stereotypes (uppercase)
        "Requirement",           // Standard SysML Requirement
        "AbstractRequirement",   // SysML Abstract Requirement
        "BusinessRequirement",   // SysML Business Requirement
        "ExtendedRequirement",   // SysML Extended Requirement
        "FunctionalRequirement", // SysML Functional Requirement
        "InterfaceRequirement",  // SysML Interface Requirement
        "PerformanceRequirement",// SysML Performance Requirement
        "PhysicalRequirement",   // SysML Physical Requirement
        "UsabilityRequirement",  // SysML Usability Requirement
        // Lowercase variants (often used in custom profiles)
        "requirement",           // lowercase requirement
        "abstractRequirement",   // lowercase abstractRequirement
        "businessRequirement",   // lowercase businessRequirement
        "extendedRequirement",   // lowercase extendedRequirement
        "functionalRequirement", // lowercase functionalRequirement
        "interfaceRequirement",  // lowercase interfaceRequirement
        "performanceRequirement",// lowercase performanceRequirement
        "physicalRequirement",   // lowercase physicalRequirement
        "usabilityRequirement",  // lowercase usabilityRequirement
        // German SysML Stereotypes (potential localizations)
        "Anforderung",           // German: Requirement
        "AbstrakteAnforderung",  // German: AbstractRequirement
        "Geschäftsanforderung",  // German: BusinessRequirement
        "ErweiterteAnforderung", // German: ExtendedRequirement
        "FunktionaleAnforderung",// German: FunctionalRequirement
        "SchnittstellenAnforderung", // German: InterfaceRequirement
        "LeistungsAnforderung",  // German: PerformanceRequirement
        "PhysischeAnforderung",  // German: PhysicalRequirement
        "BenutzbarkeitAnforderung" // German: UsabilityRequirement
    };

    /**
     * Sammelt alle Requirements aus einem Package und dessen Unter-Packages.
     * 
     * @param pkg Das Package, aus dem Requirements gesammelt werden sollen
     * @return Liste aller gefundenen Requirement-Elemente mit SysML Requirement-Stereotyp
     */
    public List<Element> extractRequirementsFromPackage(Package pkg) {
        List<Element> requirements = new ArrayList<>();
        
        // Alle unterstützten Requirement-Stereotypen sammeln
        List<Stereotype> requirementStereotypes = new ArrayList<>();
        Project project = Application.getInstance().getProject();
        
        for (String stereotypeName : REQUIREMENT_STEREOTYPE_NAMES) {
            @SuppressWarnings("deprecation")
            Stereotype st = StereotypesHelper.getStereotype(project, stereotypeName);
            if (st != null) {
                requirementStereotypes.add(st);
                log("Found stereotype: " + stereotypeName);
            } else {
                log("Stereotype not available: " + stereotypeName);
            }
        }
        
        // Validierung: Wurden überhaupt Requirement-Stereotypen gefunden?
        if (requirementStereotypes.isEmpty()) {
            log("No requirement stereotypes found in project");
            return requirements; // Leere Liste zurückgeben
        }
        
        // Rekursive Sammlung aller Requirements im Package-Bereich
        collectRequirementsRecursive(pkg, requirements, requirementStereotypes);
        return requirements;
    }
    
    /**
     * Rekursive Hilfsmethode zum Sammeln von Requirements.
     * 
     * @param pkg Das aktuelle Package, das durchsucht wird
     * @param requirements Die Liste, zu der gefundene Requirements hinzugefügt werden
     * @param requirementStereotypes Liste aller SysML Requirement-Stereotypen zum Filtern
     */
    public void collectRequirementsRecursive(Package pkg, List<Element> requirements, List<Stereotype> requirementStereotypes) {
        log("Durchsuche Package: " + pkg.getName() + " mit " + pkg.getOwnedElement().size() + " Elementen");
        
        // Alle Elemente des aktuellen Packages durchgehen
        for (Element element : pkg.getOwnedElement()) {
            String elementName = (element instanceof NamedElement) ? ((NamedElement) element).getName() : element.toString();
            log("Prüfe Element: " + elementName + " (Typ: " + element.getClass().getSimpleName() + ")");
            
            // Prüfen, ob das Element ein Requirement ist (hat einen der entsprechenden Stereotypen)
            boolean foundRequirement = false;
            for (Stereotype requirementStereotype : requirementStereotypes) {
                if (StereotypesHelper.hasStereotypeOrDerived(element, requirementStereotype)) {
                    log("✓ Requirement gefunden: " + elementName + " mit Stereotyp: " + requirementStereotype.getName());
                    requirements.add(element);
                    foundRequirement = true;
                    break; // Element gefunden, nicht weiter prüfen
                }
            }
            
            if (!foundRequirement) {
                // Zeige angewandte Stereotypen zur Diagnose
                var appliedStereotypes = StereotypesHelper.getStereotypes(element);
                if (!appliedStereotypes.isEmpty()) {
                    log("Element " + elementName + " hat andere Stereotypen: " + 
                        appliedStereotypes.stream().map(s -> s.getName()).collect(Collectors.joining(", ")));
                }
            }
            
            // Wenn das Element selbst ein Package ist, rekursiv durchsuchen
            if (element instanceof Package) {
                collectRequirementsRecursive((Package) element, requirements, requirementStereotypes);
            }
        }
        
        log("Package " + pkg.getName() + " abgeschlossen. Gefundene Requirements: " + requirements.size());
    }

    /**
     * Erstellt einen hierarchischen Baum aller Packages im Projekt.
     * 
     * @param project Das aktuelle MagicDraw-Projekt
     * @return Root-Node des Package-Trees mit vollständiger Hierarchie
     */
    public DefaultMutableTreeNode createPackageTree(Project project) {
        Package rootPackage = project.getPrimaryModel();
        
        // Spezieller PackageTreeNode für bessere Tree-Darstellung
        PackageTreeNode rootNode = new PackageTreeNode(rootPackage);
        
        // Rekursiv alle Unter-Packages hinzufügen
        addPackageChildren(rootPackage, rootNode);
        
        return rootNode;
    }
    
    /**
     * Fügt rekursiv alle Unter-Packages zu einem Tree-Node hinzu.
     * 
     * @param parentPackage Das Parent-Package, dessen Kinder durchsucht werden
     * @param parentNode Der Parent-Tree-Node, zu dem die Kinder hinzugefügt werden
     */
    public void addPackageChildren(Package parentPackage, DefaultMutableTreeNode parentNode) {
        // Alle Elemente des Parent-Packages durchgehen
        for (Element element : parentPackage.getOwnedElement()) {
            // Nur Package-Elemente berücksichtigen (keine Klassen, etc.)
            if (element instanceof Package) {
                Package childPackage = (Package) element;
                
                // Neuen Tree-Node für das Child-Package erstellen
                PackageTreeNode childNode = new PackageTreeNode(childPackage);
                parentNode.add(childNode);
                
                // Rekursiver Aufruf für die nächste Hierarchie-Ebene
                addPackageChildren(childPackage, childNode);
            }
        }
    }

    /**
     * Sammelt alle Requirements aus dem Projekt.
     * 
     * @param project Das aktuelle Projekt
     * @return Liste aller gefundenen Requirement-Elemente
     */
    public List<Element> extractRequirements(Project project) {
        List<Element> all = new ArrayList<>();
        if (project.getPrimaryModel() != null) {
            collectAll(project.getPrimaryModel(), all);
        }
        
        List<Element> reqs = new ArrayList<>();
        
        // Alle unterstützten Requirement-Stereotypen sammeln
        List<Stereotype> requirementStereotypes = new ArrayList<>();
        for (String stereotypeName : REQUIREMENT_STEREOTYPE_NAMES) {
            @SuppressWarnings("deprecation")
            Stereotype st = StereotypesHelper.getStereotype(project, stereotypeName);
            if (st != null) {
                requirementStereotypes.add(st);
                log("Found stereotype: " + stereotypeName);
            } else {
                log("Stereotype not available: " + stereotypeName);
            }
        }
        
        if (requirementStereotypes.isEmpty()) {
            log("No requirement stereotypes found in project");
            return reqs;
        }
        
        // Filterung nach Requirement-Stereotypen
        for (Element e : all) {
            for (Stereotype st : requirementStereotypes) {
                if (StereotypesHelper.hasStereotypeOrDerived(e, st)) {
                    reqs.add(e);
                    break; // Element gefunden, nicht weiter prüfen
                }
            }
        }
        return reqs;
    }

    /**
     * Sammelt rekursiv alle Elemente aus dem Modellbaum.
     * 
     * @param root Das Wurzelelement
     * @param into Die Liste, in die alle gefundenen Elemente eingefügt werden
     */
    public void collectAll(Element root, List<Element> into) {
        if (root == null) return;
        into.add(root);
        if (root instanceof Package) {
            for (Element child : ((Package) root).getOwnedElement()) {
                collectAll(child, into);
            }
        } else if (root instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier) {
            for (Element member : ((com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier) root).getOwnedMember()) {
                if (member != root) {
                    collectAll(member, into);
                }
            }
        }
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
    
    public boolean hasRequirementStereotype(Element element) {
        if (element == null) return false;
        
        // Check for requirement stereotypes using the existing logic
        for (String stereotypeName : REQUIREMENT_STEREOTYPE_NAMES) {
            if (StereotypesHelper.hasStereotype(element, stereotypeName)) {
                return true;
            }
        }
        return false;
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
 * Spezialisierte Tree-Node-Klasse für Package-Darstellung im Tree.
 */
class PackageTreeNode extends DefaultMutableTreeNode {
    /** Das MagicDraw-Package, das dieser Tree-Node repräsentiert */
    private Package pkg;
    
    /**
     * Konstruktor für Package-Tree-Node.
     * 
     * @param pkg Das MagicDraw-Package, das repräsentiert werden soll
     */
    public PackageTreeNode(Package pkg) {
        super(pkg);
        this.pkg = pkg;
    }
    
    /**
     * Gibt das gekapselte MagicDraw-Package zurück.
     * 
     * @return Das MagicDraw-Package dieses Tree-Nodes
     */
    public Package getPackage() {
        return pkg;
    }
    
    /**
     * Optimierte String-Repräsentation für Tree-Anzeige.
     * 
     * @return Benutzerfreundlicher Package-Name für die Tree-Anzeige
     */
    @Override
    public String toString() {
        return pkg.getHumanName() != null ? pkg.getHumanName() : pkg.getName();
    }
}