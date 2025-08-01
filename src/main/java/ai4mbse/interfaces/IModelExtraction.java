package ai4mbse.interfaces;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.magicdraw.core.Project;
import java.util.List;
import javax.swing.tree.TreeNode;

/**
 * Interface für Model Data Extraction Subsystem.
 * 
 * Definiert die Schnittstelle für die intelligente Extraktion und Verarbeitung 
 * von SysML-Modelldaten mit Multi-Sprach- und Multi-Stereotyp-Unterstützung.
 */
public interface IModelExtraction {
    
    /**
     * Sammelt alle Requirements aus dem gesamten Projekt.
     * 
     * @param project Das aktuelle MagicDraw-Projekt
     * @return Liste aller gefundenen Requirement-Elemente
     */
    List<Element> extractRequirements(Project project);
    
    /**
     * Sammelt Requirements aus einem spezifischen Package.
     * 
     * @param pkg Das Package, aus dem Requirements extrahiert werden sollen
     * @return Liste der gefundenen Requirements
     */
    List<Element> extractRequirementsFromPackage(Package pkg);
    
    /**
     * Erstellt eine hierarchische Tree-Struktur aller Packages.
     * 
     * @param project Das aktuelle Projekt
     * @return Root-Node des Package-Trees
     */
    TreeNode createPackageTree(Project project);
    
    /**
     * Validiert, ob ein Element einen Block-Stereotyp hat.
     * 
     * @param element Das zu prüfende Element
     * @return true, wenn das Element einen Block-Stereotyp hat
     */
    boolean hasBlockStereotype(Element element);
    
    /**
     * Prüft, ob ein Element Requirement-Stereotypen hat.
     * 
     * @param element Das zu prüfende Element
     * @return true, wenn das Element Requirement-Stereotypen hat
     */
    boolean hasRequirementStereotype(Element element);
}