package org.codehaus.groovy.grails.exceptions

import groovy.transform.CompileStatic

/**
 * @author Graeme Rocher
 * @deprecated Use {@link org.grails.core.exceptions.GrailsDomainException} instead
 */
@Deprecated
@CompileStatic
class GrailsDomainException extends org.grails.core.exceptions.GrailsDomainException{

    GrailsDomainException() {
    }

    GrailsDomainException(String message, Throwable cause) {
        super(message, cause)
    }

    GrailsDomainException(String message) {
        super(message)
    }

    GrailsDomainException(Throwable cause) {
        super(cause)
    }
}
