package org.codehaus.groovy.grails.commons;

/**
 * <p>Static singleton holder for the GrailsApplication instance</p>
 * @author Marc Palmer (marc@anyware.co.uk)
 */
public abstract class ApplicationHolder {
    private static GrailsApplication application;

    public static GrailsApplication getApplication() {
        return application;
    }

    public static void setApplication(GrailsApplication application) {
        ApplicationHolder.application = application;
    }
}
