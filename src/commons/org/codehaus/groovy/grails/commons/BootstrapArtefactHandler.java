package org.codehaus.groovy.grails.commons;

/**
 * @author Marc Palmer (marc@anyware.co.uk)
*/
public class BootstrapArtefactHandler extends ArtefactHandlerAdapter {

    public static final String TYPE = "Bootstrap";


    public BootstrapArtefactHandler() {
        super(TYPE, GrailsBootstrapClass.class, DefaultGrailsBootstrapClass.class, DefaultGrailsBootstrapClass.BOOT_STRAP);
    }
}
