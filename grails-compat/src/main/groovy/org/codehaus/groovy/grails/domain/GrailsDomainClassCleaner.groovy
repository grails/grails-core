package org.codehaus.groovy.grails.domain

import grails.core.GrailsApplication

/**
 * @deprecated Use {@link org.grails.plugins.domain.support.GrailsDomainClassCleaner} instead
 * @author Graeme Rocher
 */
@Deprecated
class GrailsDomainClassCleaner extends org.grails.plugins.domain.support.GrailsDomainClassCleaner {

    GrailsDomainClassCleaner(GrailsApplication grailsApplication) {
        super(grailsApplication)
    }
}
