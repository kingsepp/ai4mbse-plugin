package ai4mbse.subsystems;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;

/**
 * Callback-Interface für Package-Auswahl.
 */
public interface PackageSelectionCallback {
    void onPackageSelected(Package selectedPackage);
}