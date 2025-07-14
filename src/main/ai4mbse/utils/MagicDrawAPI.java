/*
 * Copyright (c) 2024 AI4MBSE Development Team
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * This file was generated with assistance from artificial intelligence tools.
 */

package ai4mbse.utils;

/**
 * Dummy interfaces to replace MagicDraw APIs for open source compilation.
 * 
 * These interfaces allow the code to compile without MagicDraw dependencies.
 * At runtime, the actual MagicDraw classes will be loaded from the installation.
 * 
 * This is a common pattern for plugin development where compile-time and runtime
 * dependencies differ.
 */

// MagicDraw Core Types
interface Project {
    String getName();
    Package getPrimaryModel();
}

interface Element {
    String getID();
    String getHumanName();
    Element getOwner();
    java.util.Collection<Element> getOwnedElement();
    java.util.Collection<Element> get_directedRelationshipOfSource();
    java.util.Collection<Comment> getOwnedComment();
}

interface NamedElement extends Element {
    String getName();
}

interface Package extends NamedElement {
    // Inherits getOwnedElement() from Element interface
}

interface Comment extends Element {
    String getBody();
}

// MagicDraw UML Types (stubs for compilation)
interface UMLClass extends NamedElement {
    // UML Class element
}

interface Classifier extends NamedElement {
    java.util.Collection<Element> getOwnedMember();
}

interface Dependency extends Element {
    java.util.Collection<NamedElement> getClient();
    java.util.Collection<NamedElement> getSupplier();
}

interface Stereotype {
    String getName();
}

// MagicDraw Plugin Framework
abstract class Plugin {
    public abstract void init();
    public abstract boolean close();
    public abstract boolean isSupported();
}

abstract class MDAction {
    protected String id;
    protected String name;
    
    public MDAction(String id, String name, Object icon, String group) {
        this.id = id;
        this.name = name;
    }
    
    public abstract void actionPerformed(java.awt.event.ActionEvent e);
    public abstract void updateState();
    
    protected void setEnabled(boolean enabled) {}
    protected void setDescription(String description) {}
}

// MagicDraw Utility Classes
class Application {
    private static Application instance = new Application();
    
    public static Application getInstance() {
        return instance;
    }
    
    public Project getProject() {
        return null; // Will be overridden at runtime
    }
    
    public java.awt.Frame getMainFrame() {
        return null; // Will be overridden at runtime
    }
    
    public GUILog getGUILog() {
        return new GUILog();
    }
}

class GUILog {
    public void log(String message) {
        System.out.println(message);
    }
    public void showError(String message) {
        System.err.println(message);
    }
}

interface AMConfigurator {
    int MEDIUM_PRIORITY = 100;
    void configure(ActionsManager manager);
    int getPriority();
}

class ActionsCategory {
    public ActionsCategory(String id, String name) {}
    public void setNested(boolean nested) {}
    public void addAction(MDAction action) {}
    public MDAction getAction(String id) { return null; }
}

class ActionsManager {
    public Object getActionFor(String id) { return null; }
    public void addCategory(int index, ActionsCategory category) {}
}

class ActionsID {
    public static final String TOOLS = "TOOLS";
}

class ActionsConfiguratorsManager {
    private static ActionsConfiguratorsManager instance = new ActionsConfiguratorsManager();
    
    public static ActionsConfiguratorsManager getInstance() {
        return instance;
    }
    
    public void addMainMenuConfigurator(AMConfigurator configurator) {}
}

class ModelElementsManager {
    private static ModelElementsManager instance = new ModelElementsManager();
    
    public static ModelElementsManager getInstance() {
        return instance;
    }
    
    public void addElement(Element element, Element owner) {}
}

class SessionManager {
    private static SessionManager instance = new SessionManager();
    
    public static SessionManager getInstance() {
        return instance;
    }
    
    public void createSession(Project project, String name) {}
    public void closeSession(Project project) {}
}

class UMLFactory {
    public static UMLFactory eINSTANCE = new UMLFactory();
    
    public Dependency createDependency() {
        return new Dependency() {
            private java.util.List<NamedElement> clients = new java.util.ArrayList<>();
            private java.util.List<NamedElement> suppliers = new java.util.ArrayList<>();
            
            public java.util.Collection<NamedElement> getClient() { return clients; }
            public java.util.Collection<NamedElement> getSupplier() { return suppliers; }
            
            // Dummy implementations for Element interface
            public String getID() { return "dummy"; }
            public String getHumanName() { return "dummy"; }
            public Element getOwner() { return null; }
            public java.util.Collection<Element> getOwnedElement() { return new java.util.ArrayList<>(); }
            public java.util.Collection<Element> get_directedRelationshipOfSource() { return new java.util.ArrayList<>(); }
            public java.util.Collection<Comment> getOwnedComment() { return new java.util.ArrayList<>(); }
        };
    }
}

class StereotypesHelper {
    public static java.util.List<Stereotype> getStereotypes(Element element) {
        return new java.util.ArrayList<>();
    }
    
    public static boolean hasStereotype(Element element, String stereotypeName) {
        return false;
    }
    
    public static boolean hasStereotypeOrDerived(Element element, Stereotype stereotype) {
        return false;
    }
    
    public static Stereotype getStereotype(Project project, String name) {
        return null;
    }
    
    public static Stereotype getStereotype(Project project, String name, String profile) {
        return null;
    }
    
    public static void addStereotype(Element element, Stereotype stereotype) {}
}