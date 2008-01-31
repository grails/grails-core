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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import groovy.lang.Closure;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * @author Marc Palmer (marc@anyware.co.uk)
 */
public class ArtefactHandlerAdapter implements ArtefactHandler {
    private String type;
    private Class grailsClassType;
    private Class grailsClassImpl;
    private boolean allowAbstract;

    protected Log log = LogFactory.getLog(ArtefactHandlerAdapter.class);
    private String artefactSuffix;


    public ArtefactHandlerAdapter(String type, Class grailsClassType, Class grailsClassImpl, String artefactSuffix) {
        this.artefactSuffix = artefactSuffix;
        this.type = type;
        this.grailsClassType = grailsClassType;
        this.grailsClassImpl = grailsClassImpl;
    }

    public ArtefactHandlerAdapter(String type, Class grailsClassType, Class grailsClassImpl, String artefactSuffix,
        boolean allowAbstract) {
        this.artefactSuffix = artefactSuffix;
        this.type = type;
        this.grailsClassType = grailsClassType;
        this.grailsClassImpl = grailsClassImpl;
        this.allowAbstract = allowAbstract;
    }

    public String getType() {
        return type;
    }

    public final boolean isArtefact(Class aClass) {
        if (isArtefactClass(aClass)) {
            if (log.isDebugEnabled()) {
                log.debug("[" + aClass.getName() + "] is a " + type + " class.");
            }
            return true;
        }
        else {
            if (log.isDebugEnabled()) {
                log.debug("[" + aClass.getName() + "] is not a " + type + " class.");
            }
            return false;
        }
    }

    /**
     * <p>Checks that class's name ends in the suffix specified for this handler.</p>
     * <p>Override for more complex criteria</p> 
     * @param clazz The class to check
     * @return True if it is an artefact of this type
     */
    public boolean isArtefactClass(Class clazz) {
        if(clazz == null) return false;
        boolean ok = clazz.getName().endsWith(artefactSuffix) && !Closure.class.isAssignableFrom(clazz);
        if (ok && !allowAbstract) {
            ok &= !Modifier.isAbstract(clazz.getModifiers());
        }
        return ok;
    }

    /**
     * <p>Creates new GrailsClass derived object using the type supplied in constructor. May not perform
     * optimally but is a convenience.</p>
     * @param artefactClass Creates a new artefact for the given class
     * @return An instance of the GrailsClass interface representing the artefact
     */
    public GrailsClass newArtefactClass(Class artefactClass) {

        try {
            Constructor c = grailsClassImpl.getDeclaredConstructor(new Class[] { Class.class } );
            // TODO GRAILS-720 plugin class instance created here first
            return (GrailsClass) c.newInstance(new Object[] { artefactClass});
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException( "Unable to locate constructor with Class parameter for "+grailsClassImpl, e);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException( "Unable to locate constructor with Class parameter for "+grailsClassImpl, e);
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException( "Unable to locate constructor with Class parameter for "+grailsClassImpl, e);
        }
        catch (InstantiationException e) {
            throw new RuntimeException( "Unable to locate constructor with Class parameter for "+grailsClassImpl, e);
        }
    }

    /**
     * Sets up the relationships between the domain classes, this has to be done after
     * the intial creation to avoid looping
     */
    public void initialize(ArtefactInfo artefacts) {
    }

    public GrailsClass getArtefactForFeature(Object feature) {
        return null;
    }

    public boolean isArtefactGrailsClass(GrailsClass artefactGrailsClass) {
        return grailsClassType.isAssignableFrom(artefactGrailsClass.getClass());
    }
}
