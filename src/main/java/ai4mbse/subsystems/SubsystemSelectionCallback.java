package ai4mbse.subsystems;

import com.nomagic.magicdraw.core.Project;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;

/**
 * Callback-Interface für Subsystem-Auswahl.
 */
public interface SubsystemSelectionCallback {
    void onSubsystemSelected(Element selectedRequirement, Package selectedSubsystemPackage, Project project);
    void onSubsystemSelectionCancelled();
}