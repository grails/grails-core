package org.codehaus.groovy.grails.commons

import groovy.transform.CompileStatic

/**
 * @author Graeme Rocher
 * @deprecated Use {@link org.grails.core.DefaultGrailsServiceClass} instead
 */
@Deprecated
@CompileStatic
class DefaultGrailsServiceClass extends org.grails.core.DefaultGrailsServiceClass {
    DefaultGrailsServiceClass(Class<?> clazz) {
        super(clazz)
    }
}
