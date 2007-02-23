package org.codehaus.groovy.grails.commons;

import java.util.Map;

/**
 * <p>Holder for all the class-related info and structures relating to an Artefact</p>
 * @author Marc Palmer (marc@anyware.co.uk)
 */
public interface ArtefactInfo {

    /**
     * <p>Gets you the array of all artefact Class(es), the original classes loaded
     * that represent the artefacts.</p>
     * @return The array, which may be empty but never null.
     */
    Class[] getClasses();

    /**
     * <p>Gets you the array of all artefact GrailsClass(es), the classes used to track
     * information about the artefact.</p>
     * @return The array, which may be empty but never null.
     */
    GrailsClass[] getGrailsClasses();

    /**
     * <p>Gets you the unmodifiable Map of all artefact Class(es), the actual classes implementing the
     * artefact.</p>
     * <p>The Map is keyed by class name, i.e. "BookController"</p>
     * @return The Map, which may be empty but never null.
     */
    Map getClassesByName();

    /**
     * <p>Gets you the unmodifiable Map of all artefact GrailsClass(es), the classes that store extra
     * state relating to the artefact.</p>
     * <p>The Map is keyed by class name, i.e. "BookController"</p>
     * @return The Map, which may be empty but never null.
     */
    Map getGrailsClassesByName();

    /**
     * <p>Retrieves the GrailsClass representing a named artefact.</p>
     * <p>From the GrailsClass you can get to the original artefact Class using getClazz().</p>
     * @param name The name of the artefact, i.e. "BookController"
     * @return The Map, which may be empty but never null.
     */
    GrailsClass getGrailsClass(String name);
}
