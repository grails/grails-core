package org.codehaus.groovy.grails.commons;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * <p>Mutable holder of artefact info</p>
 * @author Marc Palmer (marc@anyware.co.uk)
 */
public class DefaultArtefactInfo implements ArtefactInfo {

    private GrailsClass[] grailsClasses;

    private Class[] classes;

    private Map grailsClassesByName = new HashMap();

    private Map classesByName = new HashMap();

    public Map handlerData = new HashMap();

    /**
     * <p>Call to add a new class to this info object.</p>
     * <p>You <b>must</b> call refresh() later to update the arrays</p>
     * @param artefactClass
     */
    public synchronized void addGrailsClass(GrailsClass artefactClass) {
        grailsClassesByName = new HashMap(grailsClassesByName);
        classesByName = new HashMap(classesByName);

        Class actualClass = artefactClass.getClazz();
        boolean addToGrailsClasses = true;
        if (artefactClass instanceof InjectableGrailsClass) {
            addToGrailsClasses = ((InjectableGrailsClass)artefactClass).getAvailable();        
        }
        if (addToGrailsClasses) {
            grailsClassesByName.put( actualClass.getName(), artefactClass);
        }
        classesByName.put( actualClass.getName(), actualClass);
    }

    /**
     * Refresh the arrays generated from the maps
     */
    public synchronized void updateComplete() {
        grailsClassesByName = Collections.unmodifiableMap(grailsClassesByName);
        classesByName = Collections.unmodifiableMap(classesByName);

        // Make GrailsClass(es) array
        grailsClasses = (GrailsClass[]) grailsClassesByName.values().toArray(
            new GrailsClass[grailsClassesByName.size()]);

        // Make classes array
        classes = (Class[]) classesByName.values().toArray(
            new Class[classesByName.size()]);
    }

    public Class[] getClasses() {
        return this.classes;
    }

    public GrailsClass[] getGrailsClasses() {
        return this.grailsClasses;
    }

    public Map getClassesByName() {
        return this.classesByName;
    }

    public Map getGrailsClassesByName() {
        return this.grailsClassesByName;
    }

    public synchronized GrailsClass getGrailsClass(String name) {
        return (GrailsClass) this.grailsClassesByName.get(name);
    }
}
