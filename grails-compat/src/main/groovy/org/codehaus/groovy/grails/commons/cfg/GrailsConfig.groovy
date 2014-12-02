package org.codehaus.groovy.grails.commons.cfg

import grails.core.GrailsApplication
import groovy.transform.CompileStatic
import org.grails.core.cfg.DeprecatedGrailsConfig

/**
 * @author Graeme Rocher
 * @deprecated Use {@link org.grails.config.CodeGenConfig} instead
 */
@CompileStatic
@Deprecated
class GrailsConfig extends DeprecatedGrailsConfig{
    GrailsConfig(GrailsApplication grailsApplication) {
        super(grailsApplication)
    }

}
