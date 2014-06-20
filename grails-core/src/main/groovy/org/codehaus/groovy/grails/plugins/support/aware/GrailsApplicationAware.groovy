package org.codehaus.groovy.grails.plugins.support.aware

import org.codehaus.groovy.grails.commons.GrailsApplication

/**
 *
 * @author Graeme Rocher
 * @deprecated Use {@link grails.core.support.GrailsApplicationAware} instead
 */
@Deprecated
public interface GrailsApplicationAware {
    /**
     * <p>This method is called by the {@link org.springframework.context.ApplicationContext} that
     * loads the Grails application. The {@link grails.core.GrailsApplication} instance that represents
     * the loaded Grails application is injected.</p>
     *
     * @param grailsApplication the {@link grails.core.GrailsApplication} object that represents this Grails application
     */
    void setGrailsApplication(GrailsApplication grailsApplication);
}