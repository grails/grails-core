/*
* Copyright 2004-2005 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.codehaus.groovy.grails.commons;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * <p>Mutable holder of artefact info</p>
 *
 * @author Marc Palmer (marc@anyware.co.uk)
 * @author Graeme Rocher
 */
public class DefaultArtefactInfo implements ArtefactInfo {

    private GrailsClass[] grailsClasses;

    private Class[] classes;

    private Map grailsClassesByName = new HashMap();

    private Map classesByName = new HashMap();

    private Map logicalPropertyNameToClassMap = new HashMap();

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
        logicalPropertyNameToClassMap.put( artefactClass.getLogicalPropertyName(), artefactClass);
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

    public synchronized GrailsClass getGrailsClassByLogicalPropertyName(String logicalName) {
        return (GrailsClass)logicalPropertyNameToClassMap.get(logicalName);
    }
}
