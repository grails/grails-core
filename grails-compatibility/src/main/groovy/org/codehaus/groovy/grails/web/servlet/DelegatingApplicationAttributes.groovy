package org.codehaus.groovy.grails.web.servlet

import groovy.transform.CompileStatic

/**
 * @author Graeme Rocher
 * @deprecated Do not use, will be removed in a future version of Grails
 */
@Deprecated
@CompileStatic
class DelegatingApplicationAttributes implements GrailsApplicationAttributes, grails.web.util.GrailsApplicationAttributes {

    @Delegate grails.web.util.GrailsApplicationAttributes applicationAttributes

    DelegatingApplicationAttributes(grails.web.util.GrailsApplicationAttributes applicationAttributes) {
        this.applicationAttributes = applicationAttributes
    }
}
