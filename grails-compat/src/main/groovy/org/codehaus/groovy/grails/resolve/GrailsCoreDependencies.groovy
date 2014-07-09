package org.codehaus.groovy.grails.resolve

import groovy.transform.CompileStatic

/**
 * @author Graeme Rocher
 * @deprecated Use {@link org.grails.dependency.resolution.GrailsCoreDependencies} instead
 */
@CompileStatic
@Deprecated
class GrailsCoreDependencies extends org.grails.dependency.resolution.GrailsCoreDependencies {
    GrailsCoreDependencies(String grailsVersion) {
        super(grailsVersion)
    }

    GrailsCoreDependencies(String grailsVersion, String servletVersion) {
        super(grailsVersion, servletVersion)
    }

    GrailsCoreDependencies(String grailsVersion, String servletVersion, boolean java5compatible, boolean isGrailsProject) {
        super(grailsVersion, servletVersion, java5compatible, isGrailsProject)
    }
}
