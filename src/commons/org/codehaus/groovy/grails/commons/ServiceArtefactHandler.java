package org.codehaus.groovy.grails.commons;

/**
 * @author Marc Palmer (marc@anyware.co.uk)
*/
public class ServiceArtefactHandler extends ArtefactHandlerAdapter {

    public static final String TYPE = "Service";

    public ServiceArtefactHandler() {
        super(TYPE, GrailsServiceClass.class, DefaultGrailsServiceClass.class, DefaultGrailsServiceClass.SERVICE,
            false);
    }
}
