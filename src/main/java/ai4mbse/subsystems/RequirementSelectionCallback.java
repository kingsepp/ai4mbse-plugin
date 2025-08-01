package ai4mbse.subsystems;

import com.nomagic.magicdraw.core.Project;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;

/**
 * Callback-Interface für Requirement-Auswahl.
 */
public interface RequirementSelectionCallback {
    void onRequirementSelected(Element selectedRequirement, Project project);
    void onRequirementSelectionCancelled();
}