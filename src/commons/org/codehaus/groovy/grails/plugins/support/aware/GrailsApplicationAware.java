package org.codehaus.groovy.grails.plugins.support.aware;

import org.codehaus.groovy.grails.commons.GrailsApplication;

/**
 * <p>Convenience interface that can be implemented by classes that are
 * registered by plugins.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 * @see GrailsApplication
 */
public interface GrailsApplicationAware {
    /**
     * <p>This method is called by the {@link org.springframework.context.ApplicationContext} that
     * loads the Grails application. The {@link GrailsApplication} instance that represents
     * the loaded Grails application is injected.</p>
     *
     * @param grailsApplication the {@link GrailsApplication} object that represents this Grails application
     */
    void setGrailsApplication(GrailsApplication grailsApplication);
}
