package org.codehaus.groovy.grails.commons.cfg

import grails.core.GrailsApplication
import groovy.transform.CompileStatic

/**
 * @author Graeme Rocher
 * @deprecated Use {@link grails.config.GrailsConfig} instead
 */
@CompileStatic
@Deprecated
class GrailsConfig extends grails.config.GrailsConfig{
    GrailsConfig(GrailsApplication grailsApplication) {
        super(grailsApplication)
    }

}
