package org.codehaus.groovy.grails.commons

import groovy.transform.CompileStatic

/**
 * @author Graeme Rocher
 * @deprecated Use {@link DefaultGrailsServiceClass} instead
 */
@Deprecated
@CompileStatic
class DefaultGrailsServiceClass extends DefaultGrailsServiceClass {
    DefaultGrailsServiceClass(Class<?> clazz) {
        super(clazz)
    }
}
